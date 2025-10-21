from airflow.decorators import task, dag
from airflow.hooks.postgres_hook import PostgresHook
from datetime import datetime

pg_hook = PostgresHook(postgres_conn_id='my_db')
conn = pg_hook.get_conn()
cursor = conn.cursor()

default_args = {
    'owner': 'dag_owner',
    'depends_on_past': False
}

@dag(
    start_date=datetime(2024, 10, 10),
    schedule_interval=None,
    catchup=False,
    max_active_runs=1,
    default_args=default_args,
    tags=['fias', 'target']
)
def fias_to_target():
    """
    DAG для перекладки данных из временной схемы в target_edw.fias_region
    """

    @task
    def load_fias_to_target():
        source_table = "loading_fias.dictionary_fias_region"
        target_table = "target_edw.dictionary_fias_region"

        # Получение количества строк в целевой таблице перед загрузкой
        cursor.execute(f"SELECT count(*) FROM {target_table};")
        cnt_tgt_before = cursor.fetchone()[0]

        # Очистка целевой таблицы
        cursor.execute(f"TRUNCATE TABLE {target_table};")
        print(f"Таблица {target_table} очищена.")

        # Копирование данных из staging в target
        cursor.execute(f"""
            INSERT INTO {target_table}
            SELECT * FROM {source_table};
        """)
        cnt_inserted = cursor.rowcount
        conn.commit()

        print(f"Скопировано {cnt_inserted} строк из {source_table} в {target_table}.")
        print(f"Количество строк в {target_table} до загрузки: {cnt_tgt_before}. После загрузки: {cnt_inserted}.")

    load_fias_to_target()

fias_to_target()
