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
    "echo 'Running: support_file_fcst_version' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/support_file_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/support_file_fcst_version/support_file_fcst_version.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/support_file_fcst_version/log_support_file_fcst_version.properties",
    "echo 'Running: support_file_employee_target' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/support_file_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/support_file_employee_target/support_file_employee_target.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/support_file_employee_target/log_support_file_employee_target.properties",
    "echo 'Running: support_file_distr_target' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/support_file_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/support_file_distr_target/support_file_distr_target.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/support_file_distr_target/log_support_file_distr_target.properties",
    "echo 'Running: support_file_work_days' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/support_file_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/support_file_work_days/support_file_work_days.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/support_file_work_days/log_support_file_work_days.properties",
]

def check_file_exists(filepath: str, **kwargs):
    if os.path.exists(filepath):
        print("Файл найден")
        return "execute_support_file_fcst_version"
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
    'support_file_to_loading',
    default_args=default_args,
    schedule_interval='*/2 * * * *',
    catchup=False,
    max_active_runs=1,
    description='DAG для выполнения команд с проверкой файла и вызовом другого DAG',
    tags=['support_file', 'loading', 'business_files']
) as dag:

    chmod_task = BashOperator(
        task_id='chmod_files',
        bash_command='chmod -R 666 /opt/airflow/s3businessfiles/*',
    )

    check_file = BranchPythonOperator(
        task_id='check_file',
        python_callable=check_file_exists,
        op_kwargs={'filepath': '/opt/airflow/s3businessfiles/Support file.xlsx'},
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
        trigger_dag_id='support_file_to_target',
        wait_for_completion=True,
    )

    final_task = DummyOperator(task_id='support_file_final_task')

    chmod_task >> check_file
    check_file >> load_tasks[0]
    for i in range(len(load_tasks) - 1):
        load_tasks[i] >> load_tasks[i + 1]
    load_tasks[-1] >> trigger_target_dag >> final_task
    check_file >> no_file_task >> final_task
