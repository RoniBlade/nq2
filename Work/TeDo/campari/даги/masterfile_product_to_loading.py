from airflow import DAG
from airflow.operators.bash import BashOperator
from airflow.operators.python import BranchPythonOperator
from airflow.operators.dummy import DummyOperator
from airflow.operators.trigger_dagrun import TriggerDagRunOperator
from airflow.utils.dates import days_ago
import os

default_args = {
    'owner': 'dag_owner',
    'start_date': days_ago(0),
    'depends_on_past': True,
    'retries': 0,
}

commands = [
    "echo 'Running: masterfileproduct_producthierarchy' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfile_product_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileproduct_producthierarchy/masterfileproduct_producthierarchy.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileproduct_producthierarchy/log_masterfileproduct_producthierarchy.properties",
    "echo 'Running: masterfileproduct_product_master_file' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfile_product_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileproduct_product_master_file/masterfileproduct_product_master_file.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileproduct_product_master_file/log_masterfileproduct_product_master_file.properties",
]

def check_file_exists(filepath: str, **kwargs):
    if os.path.exists(filepath):
        print("Файл найден")
        return "execute_masterfileproduct_producthierarchy"
    else:
        print("Файл не найден")
        return "no_file_task"

def extract_task_name(command):
    marker = "Running: "
    if marker in command:
        start = command.index(marker) + len(marker)
        end = command.index("'", start)
        return command[start:end]
    else:
        return f"task_{hash(command)}"

with DAG(
    'master_file_product_to_loading',
    default_args=default_args,
    schedule_interval='*/2 * * * *',
    catchup=False,
    max_active_runs=1,
    description='DAG для выполнения команд с проверкой файла и вызовом другого DAG',
    tags=['master_file_product', 'loading', 'business_files']
) as dag:

    chmod_task = BashOperator(
        task_id='chmod_files',
        bash_command='chmod -R 666 /opt/airflow/s3businessfiles/*',
    )

    check_file = BranchPythonOperator(
        task_id='check_file',
        python_callable=check_file_exists,
        op_kwargs={'filepath': '/opt/airflow/s3businessfiles/1_MasterfileProduct.xlsx'},
    )

    load_tasks = []
    for command in commands:
        task_name = extract_task_name(command)
        load_task = BashOperator(
            task_id=f'execute_{task_name}',
            bash_command=f"""
            echo "START: Task {task_name}" && \
            {command} && \
            echo "END: Task {task_name}"
            """,
        )
        load_tasks.append(load_task)

    no_file_task = DummyOperator(task_id='no_file_task')

    trigger_target_dag = TriggerDagRunOperator(
        task_id='trigger_target_dag',
        trigger_dag_id='master_file_product_to_target',
        wait_for_completion=True,
    )

    final_task = DummyOperator(task_id='master_file_product_final_task')

    chmod_task >> check_file
    check_file >> load_tasks[0]
    for i in range(len(load_tasks) - 1):
        load_tasks[i] >> load_tasks[i + 1]
    load_tasks[-1] >> trigger_target_dag >> final_task
    check_file >> no_file_task >> final_task
