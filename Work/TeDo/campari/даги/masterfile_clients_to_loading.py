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
    "echo 'Running: masterfileclients_channel' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfile_clients_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_channel/masterfileclients_channel.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_channel/log_masterfileclients_channel.properties",
    "echo 'Running: masterfileclients_channel2' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfile_clients_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_channel2/masterfileclients_channel2.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_channel2/log_masterfileclients_channel2.properties",
    "echo 'Running: masterfileclients_clientshierarchy' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfile_clients_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_clientshierarchy/masterfileclients_clientshierarchy.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_clientshierarchy/log_masterfileclients_clientshierarchy.properties",
    "echo 'Running: masterfileclients_clientsmasterfile' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfile_clients_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_clientsmasterfile/masterfileclients_clientsmasterfile.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_clientsmasterfile/log_masterfileclients_clientsmasterfile.properties",
    "echo 'Running: masterfileclients_geo_dimentions' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfile_clients_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_geo_dimentions/masterfileclients_geo_dimentions.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_geo_dimentions/log_masterfileclients_geo_dimentions.properties",
    "echo 'Running: masterfileclients_hierarchy_type_a' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfile_clients_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_hierarchy_type_a/masterfileclients_hierarchy_type_a.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_hierarchy_type_a/log_masterfileclients_hierarchy_type_a.properties",
    "echo 'Running: masterfileclients_l5_l4' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfile_clients_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_l5_l4/masterfileclients_l5_l4.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_l5_l4/log_masterfileclients_l5_l4.properties",
    "echo 'Running: masterfileclients_org_structure' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfile_clients_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_org_structure/masterfileclients_org_structure.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_org_structure/log_masterfileclients_org_structure.properties",
    "echo 'Running: masterfileclients_quarter' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfile_clients_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_quarter/masterfileclients_quarter.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_quarter/log_masterfileclients_quarter.properties",
    "echo 'Running: masterfileclients_top_chains' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfile_clients_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_top_chains/masterfileclients_top_chains.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/masterfileclients_top_chains/log_masterfileclients_top_chains.properties"
]

def check_file_exists(filepath: str, **kwargs):
    if os.path.exists(filepath):
        print("Файл найден")
        return "execute_masterfileclients_channel"  # task_id первой задачи
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
    'master_file_clients_to_loading',
    default_args=default_args,
    schedule_interval='*/2 * * * *',
    catchup=False,
    max_active_runs=1,
    description='DAG для выполнения команд с проверкой файла и вызовом другого DAG',
    tags=['masterfile_clients', 'loading', 'business_files']
) as dag:

    start_task = DummyOperator(task_id='start_task')

    chmod_task = BashOperator(
        task_id='chmod_files',
        bash_command='chmod -R 666 /opt/airflow/s3businessfiles/*',
    )

    check_file = BranchPythonOperator(
        task_id='check_file',
        python_callable=check_file_exists,
        op_kwargs={'filepath': '/opt/airflow/s3businessfiles/2_MasterfileClients.xlsx'},
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
        trigger_dag_id='master_file_clients_to_target',
        wait_for_completion=True,
    )

    final_task = DummyOperator(task_id='master_file_clients_final_task')

    start_task >> chmod_task >> check_file
    check_file >> load_tasks[0]
    for i in range(len(load_tasks) - 1):
        load_tasks[i] >> load_tasks[i + 1]
    load_tasks[-1] >> trigger_target_dag >> final_task
    check_file >> no_file_task >> final_task
