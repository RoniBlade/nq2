import logging
import psycopg2
from sshtunnel import SSHTunnelForwarder
import pandas as pd
import numpy as np
import boto3
import os
import random

# Настройка логирования
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')

# SSH параметры
ssh_host = '10.250.10.5'
ssh_port = 22
ssh_user = 'atlatyrov'
ssh_password = 'potteri1972'

# Параметры подключения к базе данных
db_name = 'postgres'
db_user = 'superuser'
db_password = 'rOGvBCBRDpsWIzyg4epLP7Ow'
db_host = 'localhost'
db_port = 5432

# Рабочая директория
base_dir = '/home/alatyrov/table_structures'

# Убедимся, что папка для сохранения существует
if not os.path.exists(base_dir):
    os.makedirs(base_dir)

# Подключение к PostgreSQL через SSH туннель
with SSHTunnelForwarder(
    (ssh_host, ssh_port),
    ssh_username=ssh_user,
    ssh_password=ssh_password,
    remote_bind_address=(db_host, db_port),
    local_bind_address=('127.0.0.1', 6543)  # Локальный порт, который будет перенаправлен
) as tunnel:
    logging.info("SSH туннель установлен.")

    # Подключение к PostgreSQL через туннель
    conn = psycopg2.connect(
        dbname=db_name,
        user=db_user,
        password=db_password,
        host='127.0.0.1',  # Используем локальный адрес, предоставляемый туннелем
        port=tunnel.local_bind_port  # Порт локального перенаправления
    )
    cur = conn.cursor()

    # Получаем список таблиц из схемы loading_edwmappings
    cur.execute("""
        SELECT table_name 
        FROM information_schema.tables
        WHERE table_schema = 'loading_edwmappings'
    """)
    tables = cur.fetchall()

    # Преобразуем список таблиц в удобный для поиска формат (в нижнем регистре)
    table_names = [table[0].lower() for table in tables]

    tables_structures = {}

    # Получаем структуру для каждой таблицы
    for table_name in table_names:
        cur.execute("""
            SELECT column_name FROM information_schema.columns
            WHERE table_schema = 'loading_edwmappings'
            AND table_name = %s;
        """, (table_name,))
        structure = cur.fetchall()
        tables_structures[table_name] = structure

    # Закрытие соединения с базой данных
    cur.close()
    conn.close()

logging.info("Подключение к базе данных через SSH туннель завершено.")

# Подключение к S3
s3 = boto3.client(
    's3',
    endpoint_url='http://s3.objstor.cloud4u.com:80',
    aws_access_key_id='00f44b7116d28d3bb60d',
    aws_secret_access_key='LtVeOKCNswK40cjaki3/8sHTs4LwVxaz4FPNPyHI'
)

bucket_name = 'edwmappings'

# Функция для генерации случайных данных
def generate_random_data(columns, num_rows=10):
    data = {}
    for column in columns:
        if 'id' in column.lower() or 'num' in column.lower():
            # Генерация случайных чисел для столбцов с 'id' или 'num'
            data[column] = np.random.randint(1000, 9999, size=num_rows)
        elif 'date' in column.lower():
            # Генерация случайных дат
            data[column] = pd.date_range('2023-01-01', periods=num_rows).tolist()
        else:
            # Генерация случайных строк
            data[column] = ['Random_' + str(random.randint(1000, 9999)) for _ in range(num_rows)]
    return pd.DataFrame(data)

# Шаг 1: Создание Excel-файлов с данными и конфигурационных файлов для каждой таблицы
for table_name, columns in tables_structures.items():
    # Преобразуем список кортежей в список строк (имена столбцов)
    column_names = [col[0] for col in columns]

    # Генерация случайных данных
    df_random = generate_random_data(column_names, num_rows=10)

    # Полный путь для сохранения Excel-файла
    excel_filename = f'{table_name}.xlsx'
    excel_filepath = os.path.join(base_dir, excel_filename)

    # Сохраняем Excel-файл для этой таблицы
    df_random.to_excel(excel_filepath, index=False)

    # Генерация конфигурационного файла для этой таблицы
    mapping = ";".join([f"{i+1}={col}" for i, col in enumerate(column_names)])
    config_content = f"""\

postgresql.DBaddress=10.250.10.5
postgresql.Port=5432
postgresql.DBname=postgres
postgresql.Login=superuser
postgresql.Password=rOGvBCBRDpsWIzyg4epLP7Ow
postgresql.ssh.UseSSH=true
postgresql.ssh.Login=atlatyrov
postgresql.ssh.Password=potteri1972
postgresql.ssh.ForwardPort=5440
postgresql.maxConnection=2

database.truncateBeforeUpload=true

excel.columnMapping={mapping}
excel.firstDataRow=2
excel.sheetName=Sheet1
excel.fileName={excel_filepath}
excel.uploadTable={table_name}
excel.uploadScheme=loading_edwmappings

excel.maxRowsChunk=10000

copy.copyAfterProcess=true
copy.ProcessedDir=/home/alatyrov/processed/
copy.ErrorDir=/home/alatyrov/error/

processing.maxRecordSize=100
"""

    # Полный путь для сохранения конфигурационного файла
    config_filename = f'{table_name}_config.properties'
    config_filepath = os.path.join(base_dir, config_filename)

    # Сохраняем конфигурационный файл
    with open(config_filepath, 'w') as config_file:
        config_file.write(config_content)

    logging.info(f"Создан Excel-файл: {excel_filename} и конфигурационный файл: {config_filename} для таблицы '{table_name}'.")

# Шаг 2: Загрузка созданных Excel-файлов и конфигураций на S3
for filename in os.listdir(base_dir):
    file_path = os.path.join(base_dir, filename)
    if filename.endswith('.xlsx') or filename.endswith('_config.properties'):
        # Загрузка файла на S3
        with open(file_path, 'rb') as file_data:
            s3.upload_fileobj(file_data, bucket_name, filename)

        logging.info(f"Файл '{filename}' загружен на S3 в бакет '{bucket_name}'.")

logging.info("Все файлы успешно созданы, заполнены и загружены на S3.")
