import boto3
import os
import psycopg2
import shutil
import logging
import re  # Для работы с регулярными выражениями

# Настройка логирования
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')

# Подключение к S3 с учетными данными
s3 = boto3.client(
    's3',
    endpoint_url='http://s3.objstor.cloud4u.com:80',
    aws_access_key_id='00f44b7116d28d3bb60d',
    aws_secret_access_key='LtVeOKCNswK40cjaki3/8sHTs4LwVxaz4FPNPyHI'
)

bucket_name = 'prdsaphistory'

# Функция для получения всех файлов в бакете, включая файлы из подпапок
def list_all_files(bucket_name):
    files = []
    continuation_token = None
    while True:
        if continuation_token:
            response = s3.list_objects_v2(Bucket=bucket_name, ContinuationToken=continuation_token)
        else:
            response = s3.list_objects_v2(Bucket=bucket_name)

        contents = response.get('Contents', [])
        files.extend(contents)

        # Если есть продолжение списка, обновляем continuation_token
        continuation_token = response.get('NextContinuationToken')
        if not continuation_token:
            break
    return files

# Получаем список всех файлов, включая файлы из подпапок
files = list_all_files(bucket_name)

# Вывод всех файлов, найденных в S3
logging.info(f'Файлы в S3: {[file["Key"] for file in files]}')

# Подключение к PostgreSQL
conn = psycopg2.connect(
    dbname='postgres',
    user='superuser',
    password='rOGvBCBRDpsWIzyg4epLP7Ow',
    host='185.53.105.10',
    port='80'
)
cur = conn.cursor()

# Получаем список всех таблиц из схемы saphistory_backup
cur.execute("""
    SELECT table_name 
    FROM information_schema.tables 
    WHERE table_schema = 'saphistory_backup'
""")
tables = cur.fetchall()

# Преобразуем список таблиц в удобный для поиска формат (в нижнем регистре)
table_names = [table[0].lower() for table in tables]

# Счётчик файлов без соответствий и список таких файлов
no_match_count = 0
no_match_files = []

# Список таблиц, которые не были сопоставлены с файлами
tables_with_matches = set()

for file in files:
    file_key = file['Key']  # Полный путь включая подпапки
    if not file_key.endswith('.csv'):
        continue  # Пропускаем, если это не файл CSV

    filename = os.path.basename(file_key)  # Имя файла без пути

    # Специальная проверка для файлов с префиксом 'bpc_'
    if filename.startswith('bpc_'):
        cleaned_name = re.sub(r'_export\.csv$', '', filename, flags=re.IGNORECASE).lower()
    else:
        # Убираем "export" и "_export" из остальных файлов (независимо от регистра), приводим к нижнему регистру
        cleaned_name = re.sub(r'_?export', '', filename, flags=re.IGNORECASE).lower().replace(".csv", "").strip()

    # Проверка, существует ли таблица с полным именем файла
    matched_table = next((table for table in table_names if cleaned_name == table), None)

    if matched_table:
        # Если таблица найдена, сохраняем её в список таблиц с совпадениями
        tables_with_matches.add(matched_table)

        # Создание папки для файла
        folder_name = f"./s3/{file_key.replace('/', '_')}"  # Заменяем '/' для создания папки
        os.makedirs(folder_name, exist_ok=True)

        # Копирование csvLoader-release.jar
        shutil.copy('resources/csvLoader-release.jar', folder_name)

        # Копирование csvLoader.properties
        shutil.copy('resources/csvLoader.properties', folder_name)

        # Редактирование скопированного файла csvLoader.properties
        properties_file_path = f'{folder_name}/csvLoader.properties'
        with open(properties_file_path, 'r') as file:
            content = file.read()

        # Замена значений на найденное имя таблицы, схему и корректный путь файла
        content = content.replace('csv.uploadScheme=default', 'csv.uploadScheme=saphistory_backup')
        content = content.replace('csv.fileName=', f'csv.fileName=/s3/{file_key}')
        content = content.replace('csv.uploadTable=acdoca_ex', f'csv.uploadTable={matched_table}')

        # Сохранение изменений
        with open(properties_file_path, 'w') as file:
            file.write(content)

    else:
        # Если не найдено соответствия, увеличиваем счётчик
        no_match_count += 1
        no_match_files.append(filename)

# Закрытие соединения с БД
cur.close()
conn.close()

# Список таблиц, которые не были сопоставлены с файлами
tables_without_matches = [table for table in table_names if table not in tables_with_matches]

# Логирование общего количества файлов и таблиц без соответствий
logging.info(f'Файлов без соответствий: {no_match_count}')
logging.info(f'Список файлов без соответствий: {no_match_files}')
logging.info(f'Таблиц без соответствий: {len(tables_without_matches)}')
logging.info(f'Список таблиц без соответствий: {tables_without_matches}')
