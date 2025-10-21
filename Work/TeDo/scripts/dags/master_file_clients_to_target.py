from airflow.decorators import task, dag
from airflow.hooks.postgres_hook import PostgresHook
from datetime import datetime
from airflow.models import Variable

pg_hook = PostgresHook(postgres_conn_id='my_db')
conn = pg_hook.get_conn()
cursor = conn.cursor()

bf_settings = Variable.get("master_file_clients", deserialize_json=True)['config']

default_args = {
    'owner': "TeDo",
    'depends_on_past': False
}

@dag(
    start_date=datetime(2024, 10, 10),
    schedule_interval=None,  # Отключаем шедулинг
    catchup=False,
    max_active_runs=1,
    default_args=default_args,
    tags=['master_file_clients', 'business_files', 'target']
)
def master_file_clients_to_target():
    @task
    def t_1_master_file_clients_to_target(settings_list: list):
        target = settings_list['target']
        source = settings_list['source']
        process_id = settings_list['process_id']
        conf_id_1 = settings_list['config_id']['id']
        conf_id_2 = settings_list['config_id']['update']

        sql = f'select count(*) from {target};'
        cursor.execute(sql)
        cnt_tgt = cursor.fetchone()[0]

        sql2 = f'select info_etl.write_start_info_etl_log({process_id});'
        cursor.execute(sql2)
        id_ = cursor.fetchone()[0]
        conn.commit()

        sql3 = f'truncate table {target} cascade;'
        cursor.execute(sql3)

        sql4 = f'with res as (insert into {target} select * from {source} returning *) select count(*) from res;'
        cursor.execute(sql4)
        cnt_src = cursor.fetchone()[0]
        conn.commit()

        sql5 = f'select max(id) from {target};'
        cursor.execute(sql5)
        max_id = cursor.fetchone()[0] or 0

        sql6 = f'''
            call info_etl.write_end_info_etl_log({id_}, {cnt_src}, 0, {cnt_tgt}, {max_id}, now()::timestamp);
            update info_etl.config set value = {max_id}, lastupdatedate = now() where id = {conf_id_1};
            update info_etl.config set value = now(), lastupdatedate = now() where id = {conf_id_2};
        '''
        cursor.execute(sql6)
        conn.commit()

    bf_to_target = t_1_master_file_clients_to_target.expand(settings_list=bf_settings)

    bf_to_target

master_file_clients_to_target()
