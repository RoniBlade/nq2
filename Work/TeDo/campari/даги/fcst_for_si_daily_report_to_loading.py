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
    "echo 'Running: fcst_for_si_daily_report_budgetactual' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_daily_report_budgetactual/fcst_for_si_daily_report_budgetactual.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_daily_report_budgetactual/log_fcst_for_si_daily_report_budgetactual.properties",
    "echo 'Running: fcst_for_si_daily_report_fcstactual' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_daily_report_fcstactual/fcst_for_si_daily_report_fcstactual.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_daily_report_fcstactual/log_fcst_for_si_daily_report_fcstactual.properties",
    "echo 'Running: fcst_for_si_daily_report_nivactual' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_daily_report_nivactual/fcst_for_si_daily_report_nivactual.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_daily_report_nivactual/log_fcst_for_si_daily_report_nivactual.properties",
    "echo 'Running: fcst_for_si_daily_report_pending' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_daily_report_pending/fcst_for_si_daily_report_pending.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_daily_report_pending/log_fcst_for_si_daily_report_pending.properties",
    "echo 'Running: fcst_for_si_daily_report_pending_yesterday' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_daily_report_pending_yesterday/fcst_for_si_daily_report_pending_yesterday.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_daily_report_pending_yesterday/log_fcst_for_si_daily_report_pending_yesterday.properties",
    "echo 'Running: fcst_for_si_daily_report_volume_net_sales_actual' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_daily_report_volume_net_sales_actual/fcst_for_si_daily_report_volume_net_sales_actual.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_daily_report_volume_net_sales_actual/log_fcst_for_si_daily_report_volume_net_sales_actual.properties",
    "echo 'Running: fcst_for_si_daily_report_volume_net_sales_yesterday' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_daily_report_volume_net_sales_yesterday/fcst_for_si_daily_report_volume_net_sales_yesterday.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_daily_report_volume_net_sales_yesterday/log_fcst_for_si_daily_report_volume_net_sales_yesterday.properties",
    "echo 'Running: ffcst_for_si_daily_report_sales_plan_actual' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/fcst_for_si_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/ffcst_for_si_daily_report_sales_plan_actual/ffcst_for_si_daily_report_sales_plan_actual.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/ffcst_for_si_daily_report_sales_plan_actual/log_ffcst_for_si_daily_report_sales_plan_actual.properties",
]

def check_file_exists(filepath: str, **kwargs):
    if os.path.exists(filepath):
        print("Файл найден")
        return "execute_fcst_for_si_daily_report_budgetactual"  # Первая задача в группе загрузки
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
    'fcst_for_si_daily_report_to_loading',
    default_args=default_args,
    schedule_interval='*/2 * * * *',
    catchup=False,
    max_active_runs=1,
    description='DAG для выполнения команд с проверкой файла и вызовом другого DAG',
    tags=['fcst_for_si', 'loading', 'business_files']
) as dag:

    start_task = DummyOperator(task_id='start_task')

    chmod_task = BashOperator(
        task_id='chmod_files',
        bash_command='chmod -R 666 /opt/airflow/s3businessfiles/*',
    )

    check_file = BranchPythonOperator(
        task_id='check_file',
        python_callable=check_file_exists,
        op_kwargs={'filepath': '/opt/airflow/s3businessfiles/FCST for SI Daily Report.xlsx'},
    )

    no_file_task = DummyOperator(task_id='no_file_task')

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

    trigger_target_dag = TriggerDagRunOperator(
        task_id='trigger_target_dag',
        trigger_dag_id='fcst_for_si_daily_report_to_target',
        wait_for_completion=True,
    )

    final_task = DummyOperator(task_id='fcst_for_si_daily_report_final_task')

    start_task >> chmod_task >> check_file
    check_file >> load_tasks[0]
    for i in range(len(load_tasks) - 1):
        load_tasks[i] >> load_tasks[i + 1]
    load_tasks[-1] >> trigger_target_dag >> final_task
    check_file >> no_file_task >> final_task
