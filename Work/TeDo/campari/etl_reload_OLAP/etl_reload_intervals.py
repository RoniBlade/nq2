from datetime import datetime, timedelta
import logging
import pymssql
import psycopg2
import pandas as pd
import time
from psycopg2.extras import execute_values
from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.operators.empty import EmptyOperator
from airflow.operators.trigger_dagrun import TriggerDagRunOperator

# Параметры подключения к базам данных
sql_server_conn_params = {
    'server': '213.79.122.235',
    'user': 'Cube_Campari_SQL',
    'password': 'kCzFuO9vZo1c',
    'database': 'Campari',
    'port': 1433
}

pg_conn_params = {
    'dbname': 'postgres',
    'user': 'superuser',
    'password': 'rOGvBCBRDpsWIzyg4epLP7Ow',
    'host': 'qas.PGsilver01.corp.camparirus.ru',
    'port': '5432'
}


# SQL-запрос для извлечения данных из SQL Server
sql_server_query = """
    SELECT 
    КодПериод AS code_period,
    ROUND(SUM(ROUND("Продажи в руб.", 2)), 2) AS total_gross_sales,
    ROUND(SUM(ROUND("Продажи, шт.", 0)), 0) AS total_sales_qty
FROM 
    "Campari_K1_OLAP_"
GROUP BY 
    "КодПериод"
order by 1;
"""

# Настройка логирования
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Функции для подключения к базам данных
def connect_sql_server():
    return pymssql.connect(
        server=sql_server_conn_params['server'],
        user=sql_server_conn_params['user'],
        password=sql_server_conn_params['password'],
        database=sql_server_conn_params['database'],
        port=sql_server_conn_params['port']
    )

def connect_postgres():
    return psycopg2.connect(
        dbname=pg_conn_params['dbname'],
        user=pg_conn_params['user'],
        password=pg_conn_params['password'],
        host=pg_conn_params['host'],
        port=pg_conn_params['port']
    )

# Получение данных из SQL Server
def get_sql_server_data():
    sql_query = """
    SELECT code_period, total_gross_sales, total_sales_qty
    FROM info_etl."monthly_aggregations_campari_k1_olap_";
    """
    
    conn = connect_postgres()
    df = pd.read_sql(sql_query, conn)
    conn.close()
    
    return df

# Получение данных из PostgreSQL
def get_postgres_data():
    sql_query = """
    SELECT code_period, total_gross_sales, total_sales_qty
    FROM info_etl."monthly_aggregations_campari_k1_olap";
    """
    
    conn = connect_postgres()
    df = pd.read_sql(sql_query, conn)
    conn.close()
    return df

# Функция перезагрузки данных по периодам
def reload_periods():

    sql_server_data = get_sql_server_data()
    postgres_data = get_postgres_data()

    merged_df = pd.merge(
        postgres_data,
        sql_server_data,
        on='code_period',
        how='outer',
        suffixes=('_pg', '_sql')
    )

    # Определяем разницу между данными из PostgreSQL и SQL Server
    def check_difference(row):
        # Сравниваем общее количество продаж и общую сумму
        return (
            (row['total_sales_qty_pg'] != row['total_sales_qty_sql']) or
            abs(row['total_gross_sales_pg'] - row['total_gross_sales_sql']) > 1
        )

    differences = merged_df[merged_df.apply(check_difference, axis=1)]
    periods_to_reload = differences['code_period'].tolist()
    
    logger.info(f"Интервалы для перезагрузки: {periods_to_reload}")

    if not periods_to_reload:
        logger.info("Нет интервалов для перезагрузки.")
        return

    max_retries = 3
    retry_count = 0
    sql_cursor = None  # Инициализация переменной

    while retry_count < max_retries:
        try:
            pg_conn = connect_postgres()
            pg_conn.autocommit = False

            sql_conn = connect_sql_server()
            sql_cursor = sql_conn.cursor(as_dict=True)

            disable_indexes(pg_conn)
            delete_data_from_olap(pg_conn, periods_to_reload)

            for period in periods_to_reload:
                logger.info(f'Начало перезагрузки данных за период {period}')
                offset = 0
                chunk_size = 500000

                while True:
                    sql_query = f"""
                        WITH OrderedData AS (
                            SELECT 
                                "КодПериод", "Дата", "КодАдресВх", "КодНоменклатураВх", "Продажи, шт.",
                                "Продажи в руб.", "Сумма без НДС", "Себестоимость в руб.", 
                                "КодПоставщикРЦ", "КодПроизводитель", "InsertDate",
                                ROW_NUMBER() OVER (ORDER BY "КодПериод", "Дата", "КодАдресВх", "КодНоменклатураВх") AS RowNum
                            FROM Campari_K1_OLAP_
                            WHERE "КодПериод" = '{period}'
                        )
                        SELECT * FROM OrderedData
                        WHERE RowNum > {offset}
                        ORDER BY RowNum
                        OFFSET 0 ROWS
                        FETCH NEXT {chunk_size} ROWS ONLY;
                    """
                    
                    sql_cursor.execute(sql_query)
                    rows = sql_cursor.fetchall()

                    if not rows:
                        break

                    pg_cur = pg_conn.cursor()
                    pg_insert_query = """
                    INSERT INTO loading_retailexpert."Campari_K1_OLAP" ("КодПериод", "Дата", "КодАдресВх", "КодНоменклатураВх", 
                                                                        "Продажи, шт.", "Продажи в руб.", "Сумма без НДС", 
                                                                        "Себестоимость в руб.", "КодПоставщикРЦ", 
                                                                        "КодПроизводитель", "InsertDate")
                    VALUES %s;
                    """
                    execute_values(pg_cur, pg_insert_query, [tuple(row.values())[:-1] for row in rows])
                    pg_cur.close()

                    offset += chunk_size

                pg_conn.commit()
                logger.info(f'Данные за период {period} успешно сохранены в базе данных PostgreSQL')

            enable_indexes(pg_conn)
            break

        except (Exception, psycopg2.DatabaseError, pymssql.DatabaseError) as error:
            logger.error(f'Ошибка во время перезагрузки данных: {error}')
            if pg_conn:
                pg_conn.rollback()
            retry_count += 1
            if retry_count < max_retries:
                time.sleep(5)

        finally:
            if sql_cursor:
                sql_cursor.close()
            if sql_conn:
                sql_conn.close()
            if pg_conn:
                pg_conn.close()


# Агрегация данных в PostgreSQL
def insert_data_into_postgres_aggregation():
    conn = connect_postgres()
    try:
        cur = conn.cursor()
        cur.execute("CALL insert_data_into_postgres_aggregation_proc();")
        conn.commit()
        logger.info("Процедура успешно выполнена в PostgreSQL.")
    except Exception as e:
        logger.error(f"Ошибка при вызове процедуры: {e}")
        if conn:
            conn.rollback()
    finally:
        if cur:
            cur.close()
        if conn:
            conn.close()

def fetch_data_from_sql_server(sql_server_cursor):
    try:
        sql_server_cursor.execute(sql_server_query)
        rows = sql_server_cursor.fetchall()
        logging.info("Данные успешно извлечены из SQL Server.")
        
        # Применение округления и использование индексов
        return [(row[0], round(row[1], 2), round(row[2], 0)) for row in rows]
    except Exception as e:
        logging.error(f"Ошибка выполнения запроса в SQL Server: {e}")
        raise

def insert_data_to_postgresql(pg_conn, pg_cursor, data):
    insert_query = """
        INSERT INTO info_etl.monthly_aggregations_campari_k1_olap_ (code_period, total_gross_sales, total_sales_qty)
        VALUES (%s, %s, %s)
        ON CONFLICT (code_period)
        DO UPDATE 
        SET total_gross_sales = EXCLUDED.total_gross_sales,
            total_sales_qty = EXCLUDED.total_sales_qty;
    """
    try:
        pg_cursor.executemany(insert_query, data)
        pg_conn.commit()
        logging.info("Данные успешно вставлены или обновлены в PostgreSQL.")
    except Exception as e:
        logging.error(f"Ошибка вставки данных в PostgreSQL: {e}")
        pg_conn.rollback()
        raise

# Агрегация данных в SQL Server
def insert_data_into_sql_aggregation():
    try:
        sql_server_conn = connect_sql_server()
        sql_server_cursor = sql_server_conn.cursor()

        pg_conn = connect_postgres()
        pg_cursor = pg_conn.cursor()
        
        truncate_query = "TRUNCATE TABLE info_etl.monthly_aggregations_campari_k1_olap_;"
        pg_cursor.execute(truncate_query)

        data = fetch_data_from_sql_server(sql_server_cursor)

        # Вставляем данные в PostgreSQL
        if data:
            insert_data_to_postgresql(pg_conn, pg_cursor, data)
        else:
            logging.info("Нет данных для вставки.")

    except Exception as e:
        logging.error(f"Произошла ошибка: {e}")
    finally:
        # Закрываем подключения к базам данных
        if sql_server_cursor:
            sql_server_cursor.close()
        if sql_server_conn:
            sql_server_conn.close()
        if pg_cursor:
            pg_cursor.close()
        if pg_conn:
            pg_conn.close()
        logging.info("Подключения к базам данных закрыты.")


# Функции управления индексами
def disable_indexes(pg_conn):
    logger.info('Отключение индексов на таблице OLAP...')
    pg_cur = pg_conn.cursor()
    try:
        pg_cur.execute('ALTER TABLE loading_retailexpert."Campari_K1_OLAP" DISABLE TRIGGER ALL;')
        logger.info('Индексы и триггеры успешно отключены')
    except Exception as e:
        logger.error(f'Ошибка при отключении индексов: {e}')
        raise
    finally:
        pg_cur.close()

def enable_indexes(pg_conn):
    logger.info('Включение индексов на таблице OLAP...')
    pg_cur = pg_conn.cursor()
    try:
        pg_cur.execute('ALTER TABLE loading_retailexpert."Campari_K1_OLAP" ENABLE TRIGGER ALL;')
        logger.info('Индексы и триггеры успешно включены')
    except Exception as e:
        logger.error(f'Ошибка при включении индексов: {e}')
        raise
    finally:
        pg_cur.close()

# Удаление данных перед загрузкой
def delete_data_from_olap(pg_conn, periods):
    pg_cur = pg_conn.cursor()
    try:
        for period in periods:
            logger.info(f'Удаление данных за период {period} из OLAP...')
            delete_query = f"""
                DELETE FROM loading_retailexpert."Campari_K1_OLAP" 
                WHERE "КодПериод" = '{period}';
            """
            pg_cur.execute(delete_query)
        pg_conn.commit()
        logger.info(f'Данные за периоды {periods} успешно удалены из таблицы OLAP')
    except Exception as e:
        logger.error(f'Ошибка при удалении данных за периоды: {e}')
        raise
    finally:
        pg_cur.close()

# DAG
default_args = {
    'owner': 'airflow',
    'start_date': datetime(2024, 7, 24),
    'retries': 1,
    'retry_delay': timedelta(minutes=5)
}

with DAG(
    'etl_campari_k1',
    default_args=default_args,
    description='ETL процесс из SQL Server в PostgreSQL',
    schedule_interval='0 0 * * 0',
    catchup=False,
    tags=['loading', 'retailexpert']
) as dag:

    start = EmptyOperator(task_id='start')

    reload_data = PythonOperator(
        task_id='reload_periods',
        python_callable=reload_periods
    )

    insert_postgres_aggregation = PythonOperator(
        task_id='insert_data_into_postgres_aggregation',
        python_callable=insert_data_into_postgres_aggregation
    )

    insert_sql_aggregation = PythonOperator(
        task_id='insert_data_into_sql_aggregation',
        python_callable=insert_data_into_sql_aggregation
    )

    trigger_another_dag = TriggerDagRunOperator(
        task_id='trigger_loading_retailexpert',
        trigger_dag_id='loading_retailexpert'
    )

    end = EmptyOperator(task_id='end')

    start >> insert_sql_aggregation >> reload_data >> trigger_another_dag >> insert_postgres_aggregation >> end
