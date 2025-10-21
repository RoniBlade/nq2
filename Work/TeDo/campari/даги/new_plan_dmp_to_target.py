from airflow.decorators import task, dag
from airflow.hooks.postgres_hook import PostgresHook
from datetime import datetime

# Подключение к базе данных
pg_hook = PostgresHook(postgres_conn_id='my_db')
conn = pg_hook.get_conn()
cursor = conn.cursor()

# Аргументы по умолчанию
default_args = {
    'owner': 'dag_owner',
    'depends_on_past': False
}

@dag(
    start_date=datetime(2024, 10, 10),
    schedule_interval=None,  # Отключаем шедулинг
    catchup=False,
    max_active_runs=1,
    default_args=default_args,
    tags=['new_plan_dmp', 'business_files', 'target']
)
def new_plan_dmp_to_target():
    """
    DAG для перекладки данных из временной схемы в target_edw.Plan_DMP_noncontract_sheet
    """

    @task
    def load_new_plan_dmp_to_target():
        source_table = "loading_businessfiles.Plan_DMP_noncontract_sheet"
        target_table = "target_edw.Plan_DMP_noncontract_sheet"

        # Получение количества строк в целевой таблице перед загрузкой
        cursor.execute(f"SELECT count(*) FROM {target_table};")
        cnt_tgt_before = cursor.fetchone()[0]

        # Очистка целевой таблицы
        cursor.execute(f"TRUNCATE TABLE {target_table} CASCADE;")
        print(f"Таблица {target_table} очищена.")

        # Копирование данных из временной таблицы в целевую
        cursor.execute(f"""
            INSERT INTO {target_table}
            SELECT * FROM {source_table};
        """)
        cnt_inserted = cursor.rowcount
        conn.commit()

        # Логирование информации о загрузке
        print(f"Скопировано {cnt_inserted} строк из {source_table} в {target_table}.")
        print(f"Количество строк в {target_table} до загрузки: {cnt_tgt_before}. После загрузки: {cnt_inserted}.")

    # Выполнение задачи перекладки данных
    load_new_plan_dmp_to_target()

new_plan_dmp_to_target()
