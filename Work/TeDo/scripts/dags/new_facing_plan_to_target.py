from airflow.decorators import task, dag
from airflow.hooks.postgres_hook import PostgresHook
from datetime import datetime
from airflow.models import Variable

pg_hook = PostgresHook(postgres_conn_id='my_db')
conn = pg_hook.get_conn()
cursor = conn.cursor()

bf_settings = Variable.get("new_facing_plan", deserialize_json=True)['config']

default_args = {'owner': "TeDo", 'depends_on_past': False}

@dag(
    start_date=datetime(2024, 10, 10),
    schedule_interval=None,
    catchup=False,
    max_active_runs=1,
    default_args=default_args,
    tags=['new_facing_plan', 'business_files', 'target']
)
def new_facing_plan_to_target():
    """
    DAG для обработки данных, запускается вручную или триггером из другого DAG.
    """

    @task
    def t_1_new_facing_plan_to_target(settings_list: list):
        target = settings_list['target']
        source = settings_list['source']
        process_id = settings_list['process_id']
        conf_id_1 = settings_list['config_id']['id']
        conf_id_2 = settings_list['config_id']['update']

        print('Получение данных из ', target)
        cursor.execute(f'SELECT count(*) FROM {target};')
        cnt_tgt = cursor.fetchone()[0]

        cursor.execute(f'SELECT info_etl.write_start_info_etl_log({process_id});')
        id_ = cursor.fetchone()[0]
        conn.commit()

        cursor.execute(f'TRUNCATE TABLE {target} CASCADE;')
        print(f'Очищена таблица {target}')

        cursor.execute(
            f'WITH res AS (INSERT INTO {target} SELECT * FROM {source} RETURNING *) SELECT count(*) FROM res;'
        )
        cnt_src = cursor.fetchone()[0]
        conn.commit()

        cursor.execute(f'SELECT max(id) FROM {target};')
        max_id = cursor.fetchone()[0] or 0

        print(f'Скопировано из {source} в {target}: {cnt_src} строк, max_id={max_id}')

        sql6 = f"""
            CALL info_etl.write_end_info_etl_log({id_}, {cnt_src}, 0, {cnt_tgt}, {max_id}, now()::timestamp);
            UPDATE info_etl.config SET value = {max_id}, lastupdatedate = now() WHERE id = {conf_id_1};
            UPDATE info_etl.config SET value = now(), lastupdatedate = now() WHERE id = {conf_id_2};
        """
        cursor.execute(sql6)
        conn.commit()
        print('Обработка завершена')

    bf_to_target = t_1_new_facing_plan_to_target.expand(settings_list=bf_settings)

    bf_to_target

new_facing_plan_to_target()
