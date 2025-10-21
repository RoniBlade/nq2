from datetime import datetime, timedelta
from airflow import DAG
from airflow.operators.python_operator import PythonOperator
from airflow.hooks.base_hook import BaseHook
import pandas as pd
import sqlalchemy

default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'start_date': datetime(2024, 7, 11),
    'email_on_failure': False,
    "email_on_retry": False,
    'retries': 1,
    'retry_delay': timedelta(minutes=5)
}


dag = DAG(
    'etl_dag_per_table',
    default_args=default_args,
    description='',
    schedule_interval=
)

def get_schemas():
    src_conn = BaseHook.get_connection('automatica_db')
    engine = sqlalchemy.create_engine