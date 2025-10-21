from airflow import DAG
from airflow.operators.bash import BashOperator
from airflow.operators.trigger_dagrun import TriggerDagRunOperator
from airflow.utils.dates import days_ago

default_args = {
    'owner': 'dag_owner',
    'start_date': days_ago(0),
    'depends_on_past': False,
    'retries': 0,
}

with DAG(
    'fias_to_loading',
    default_args=default_args,
    schedule_interval="0 0 * * *",  # Раз в неделю
    catchup=False,
    max_active_runs=1,
    description='DAG для загрузки данных из FIAS через JAR',
    tags=['fias', 'loading']
) as dag:

    load_fias_task = BashOperator(
        task_id='load_fias_via_jar',
        bash_command="""
        echo "Запуск JAR для загрузки FIAS" && \
        java -jar /opt/airflow/plugins/fiasLoader/fias-release.jar
        """
    )

    trigger_target_dag = TriggerDagRunOperator(
        task_id='trigger_fias_to_target',
        trigger_dag_id='fias_to_target',
        wait_for_completion=True,
    )

    load_fias_task >> trigger_target_dag
