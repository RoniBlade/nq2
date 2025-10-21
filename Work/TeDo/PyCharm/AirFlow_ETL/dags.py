from datetime import datetime, timedelta
from airflow import DAG
from airflow.operators.python_operator import PythonOperator
from airflow.hooks.base_hook import BaseHook
import pandas as pd
import sqlalchemy

default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'start_date': datetime(2023, 1, 1),
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 1,
    'retry_delay': timedelta(minutes=5),
}

dag = DAG(
    'etl_dag_per_table',
    default_args=default_args,
    description='ETL DAG for loading each table from Automatica to my DB',
    schedule_interval=timedelta(days=1),
)


def get_schemas():
    src_conn = BaseHook.get_connection('automatica_db')
    engine = sqlalchemy.create_engine(
        f'mysql+pymysql://{src_conn.login}:{src_conn.password}@{src_conn.host}:{src_conn.port}/{src_conn.schema}')
    schemas_query = "SELECT schema_name FROM information_schema.schemata WHERE schema_name NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')"
    schemas = pd.read_sql(schemas_query, engine)
    return schemas['schema_name'].tolist()


def get_tables(schema_name):
    src_conn = BaseHook.get_connection('automatica_db')
    engine = sqlalchemy.create_engine(
        f'mysql+pymysql://{src_conn.login}:{src_conn.password}@{src_conn.host}:{src_conn.port}/{src_conn.schema}')
    tables_query = f"SELECT table_name FROM information_schema.tables WHERE table_schema = '{schema_name}'"
    tables = pd.read_sql(tables_query, engine)
    return tables['table_name'].tolist()


def create_schema(schema_name):
    dest_conn = BaseHook.get_connection('my_db')
    engine = sqlalchemy.create_engine(
        f'postgresql://{dest_conn.login}:{dest_conn.password}@{dest_conn.host}:{dest_conn.port}/{dest_conn.schema}')
    with engine.connect() as conn:
        conn.execute(f"CREATE SCHEMA IF NOT EXISTS {schema_name}")


def create_table(schema_name, table_name):
    src_conn = BaseHook.get_connection('automatica_db')
    dest_conn = BaseHook.get_connection('my_db')

    src_engine = sqlalchemy.create_engine(
        f'mysql+pymysql://{src_conn.login}:{src_conn.password}@{src_conn.host}:{src_conn.port}/{src_conn.schema}')
    dest_engine = sqlalchemy.create_engine(
        f'postgresql://{dest_conn.login}:{dest_conn.password}@{dest_conn.host}:{dest_conn.port}/{dest_conn.schema}')

    table_schema_query = f"""
    SELECT column_name, data_type, character_maximum_length
    FROM information_schema.columns
    WHERE table_schema = '{schema_name}' AND table_name = '{table_name}'
    """
    table_schema = pd.read_sql(table_schema_query, src_engine)

    create_table_query = f"CREATE TABLE IF NOT EXISTS {schema_name}.{table_name} ("
    columns = []
    for index, row in table_schema.iterrows():
        column_definition = f"{row['column_name']} {row['data_type']}"
        if row['data_type'] == 'varchar' and row['character_maximum_length'] is not None:
            column_definition += f"({row['character_maximum_length']})"
        columns.append(column_definition)
    create_table_query += ", ".join(columns) + ")"

    with dest_engine.connect() as conn:
        conn.execute(create_table_query)


def extract_table(schema_name, table_name, **kwargs):
    src_conn = BaseHook.get_connection('automatica_db')
    engine = sqlalchemy.create_engine(
        f'mysql+pymysql://{src_conn.login}:{src_conn.password}@{src_conn.host}:{src_conn.port}/{src_conn.schema}')
    query = f"SELECT * FROM {schema_name}.{table_name}"
    df = pd.read_sql(query, engine)
    df.to_csv(f'/tmp/{schema_name}_{table_name}_data.csv', index=False)


def load_table(schema_name, table_name, **kwargs):
    dest_conn = BaseHook.get_connection('my_db')
    engine = sqlalchemy.create_engine(
        f'postgresql://{dest_conn.login}:{dest_conn.password}@{dest_conn.host}:{dest_conn.port}/{dest_conn.schema}')
    df = pd.read_csv(f'/tmp/{schema_name}_{table_name}_data.csv')
    df.to_sql(table_name, engine, schema=schema_name, if_exists='replace', index=False)


# Добавление соединений к базам данных
automatica_conn = {
    'conn_id': 'automatica_db',
    'conn_type': 'mysql',
    'host': 'campari.automatica.ly',
    'login': 'campari_tedo',
    'password': 'ag_o3iIa4dsO',
    'schema': 'campari',
    'port': 3306,
}

my_db_conn = {
    'conn_id': 'my_db',
    'conn_type': 'postgres',
    'host': 'qas.PGsilver01.corp.camparirus.ru',
    'login': 'superuser',
    'password': 'rOGvBCBRDpsWIzyg4epLP7Ow',
    'schema': 'public',
    'port': 5432,
}

from airflow import settings
from airflow.models import Connection


def add_connection(conn_details):
    session = settings.Session()
    conn = Connection(
        conn_id=conn_details['conn_id'],
        conn_type=conn_details['conn_type'],
        host=conn_details['host'],
        login=conn_details['login'],
        password=conn_details['password'],
        schema=conn_details['schema'],
        port=conn_details['port']
    )
    session.add(conn)
    session.commit()
    session.close()


add_connection(automatica_conn)
add_connection(my_db_conn)

# Получаем список всех схем
schema_list = get_schemas()

for schema in schema_list:
    create_schema_task = PythonOperator(
        task_id=f'create_schema_{schema}',
        python_callable=create_schema,
        op_args=[schema],
        provide_context=True,
        dag=dag,
    )

    table_list = get_tables(schema)

    for table in table_list:
        create_table_task = PythonOperator(
            task_id=f'create_table_{schema}_{table}',
            python_callable=create_table,
            op_args=[schema, table],
            provide_context=True,
            dag=dag,
        )

        extract_task = PythonOperator(
            task_id=f'extract_{schema}_{table}',
            python_callable=extract_table,
            op_args=[schema, table],
            provide_context=True,
            dag=dag,
        )

        load_task = PythonOperator(
            task_id=f'load_{schema}_{table}',
            python_callable=load_table,
            op_args=[schema, table],
            provide_context=True,
            dag=dag,
        )

        create_schema_task >> create_table_task >> extract_task >> load_task
