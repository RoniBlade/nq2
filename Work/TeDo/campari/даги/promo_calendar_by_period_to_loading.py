from airflow import DAG
from airflow.operators.bash import BashOperator
from airflow.operators.python import BranchPythonOperator
from airflow.operators.dummy import DummyOperator
from airflow.operators.trigger_dagrun import TriggerDagRunOperator
from airflow.utils.dates import days_ago
import os

# Аргументы по умолчанию
default_args = {
    'owner': 'dag_owner',
    'start_date': days_ago(0),
    'depends_on_past': True,
    'retries': 0,
}

# Команды для загрузки
commands = [
    "echo 'Running: promo_calendar_by_period' && java -jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/promo_calendar_by_period_excelLoader-release.jar /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/promo_calendar_by_period/promo_calendar_by_period.properties /opt/airflow/plugins/excelLoader/excelLoader-businessfiles/promo_calendar_by_period/log_promo_calendar_by_period.properties",
]

# Проверка наличия файла
def check_file_exists(filepath: str, **kwargs):
    if os.path.exists(filepath):
        print("Файл найден")
        return "execute_promo_calendar_by_period"
    else:
        print("Файл не найден")
        return "no_file_task"

# Функция для извлечения имени задачи
def extract_task_name(command):
    marker = "Running: "
    if marker in command:
        start = command.index(marker) + len(marker)
        end = command.index("'", start)
        return command[start:end]
    else:
        return f"task_{hash(command)}"

with DAG(
    'promo_calendar_by_period_to_loading',
    default_args=default_args,
    schedule_interval='*/2 * * * *',
    catchup=False,
    max_active_runs=1,
    description='DAG для выполнения команд с проверкой файла и вызовом другого DAG',
    tags=['si_fcst_evolution', 'loading', 'business_files']
) as dag:

    # Задача изменения прав
    chmod_task = BashOperator(
        task_id='chmod_files',
        bash_command='chmod -R 666 /opt/airflow/s3businessfiles/*',
    )

    # Проверка наличия файла
    check_file = BranchPythonOperator(
        task_id='check_file',
        python_callable=check_file_exists,
        op_kwargs={'filepath': '/opt/airflow/s3businessfiles/Promo calendar by period.xlsx'},
    )

    # Создание задач загрузки
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

    # Задача в случае отсутствия файла
    no_file_task = DummyOperator(task_id='no_file_task')

    # Задача триггера следующего DAG
    trigger_target_dag = TriggerDagRunOperator(
        task_id='trigger_target_dag',
        trigger_dag_id='promo_calendar_by_period_to_target',
        wait_for_completion=True,
    )

    # Финальная задача
    final_task = DummyOperator(task_id='promo_calendar_by_period_final_task')

    # Задаем последовательность выполнения задач
    chmod_task >> check_file
    check_file >> load_tasks[0]
    for i in range(len(load_tasks) - 1):
        load_tasks[i] >> load_tasks[i + 1]
    load_tasks[-1] >> trigger_target_dag >> final_task
    check_file >> no_file_task >> final_task
