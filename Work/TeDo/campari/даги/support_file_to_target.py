from airflow.decorators import task, dag
from airflow.hooks.postgres_hook import PostgresHook
from datetime import datetime
from airflow.models import Variable

pg_hook = PostgresHook(postgres_conn_id='my_db')
conn = pg_hook.get_conn()
cursor = conn.cursor()

bf_settings = Variable.get("support_file", deserialize_json=True)['config']

default_args = {
    'owner': "TeDo",
    'depends_on_past': False
}

@dag(
    start_date=datetime(2024, 10, 10),
    schedule_interval=None,
    catchup=False,
    max_active_runs=1,
    default_args=default_args,
    tags=['support_file', 'business_files', 'target']
)
def support_file_to_target():

    @task
    def t_1_support_file_to_target(settings_list: list):
        target = settings_list['target']
        source = settings_list['source']

        # Очистка целевой таблицы
        sql3 = f'truncate table {target} cascade;'
        cursor.execute(sql3)

        # Копирование данных
        sql4 = f'with res as (insert into {target} select * from {source} returning *) select count(*) from res;'
        cursor.execute(sql4)
        cursor.fetchone()
        conn.commit()

    bf_to_target = t_1_support_file_to_target.expand(settings_list=bf_settings)
    bf_to_target

support_file_to_target()
