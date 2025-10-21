import boto3
from botocore.client import Config
import os
import urllib3
import warnings

# Отключение предупреждений InsecureRequestWarning
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
warnings.filterwarnings("ignore", category=urllib3.exceptions.InsecureRequestWarning)

# Конфигурация клиента S3 для Cloud4U с отключенной проверкой SSL
s3_client = boto3.client(
    's3',
    endpoint_url='https://s3.objstor.cloud4u.com:443',
    aws_access_key_id='00f44b7116d28d3bb60d',
    aws_secret_access_key='LtVeOKCNswK40cjaki3/8sHTs4LwVxaz4FPNPyHI',
    config=Config(signature_version='s3v4', retries={'max_attempts': 10, 'mode': 'standard'}),
    verify=False  # Отключение проверки SSL-сертификатов
)


def upload_file(bucket_name, file_name, object_name=None):
    if object_name is None:
        object_name = file_name

    try:
        print(f'Начинаю загрузку файла: {file_name} в бакет: {bucket_name} с именем объекта: {object_name}')
        s3_client.upload_file(file_name, bucket_name, object_name)
        print(f'Файл {file_name} успешно загружен в {bucket_name} как {object_name}')
    except Exception as e:
        print(f'Ошибка при загрузке файла {file_name} в бакет {bucket_name}: {e}')


def upload_files_from_list(bucket_name, files):
    print(f'Начинаю загрузку файлов в бакет {bucket_name}...')

    for file_info in files:
        file_name = file_info['file_name']
        object_name = file_info.get('object_name', os.path.basename(file_name))

        if not os.path.exists(file_name):
            print(f'Файл {file_name} не существует. Пропускаю...')
            continue

        print(f'Подготовка к загрузке файла: {file_name}...')
        upload_file(bucket_name, file_name, object_name)

    print(f'Загрузка всех файлов завершена.')


# Пример использования
files_to_upload = [
    {'file_name': 'bseg_export.csv'},
]

print('--- Начало загрузки файлов в S3 ---')
upload_files_from_list('prdsaphistory', files_to_upload)
print('--- Все файлы успешно загружены ---')
