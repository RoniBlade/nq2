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

def list_objects(bucket_name):
    try:
        response = s3_client.list_objects_v2(Bucket=bucket_name)
        return response.get('Contents', [])
    except Exception as e:
        print(f'Error listing objects in bucket: {e}')
        return []

def download_file(bucket_name, object_name, file_name):
    try:
        s3_client.download_file(bucket_name, object_name, file_name)
        print(f'{object_name} has been downloaded as {file_name}')
    except Exception as e:
        print(f'Error downloading file: {e}')

def download_all_files(bucket_name):
    objects = list_objects(bucket_name)
    if not objects:
        print('No objects found in bucket.')
        return

    # Скачать все файлы в директорию проекта
    download_directory = os.path.dirname(os.path.abspath(__file__))  # Текущая директория проекта
    os.makedirs(download_directory, exist_ok=True)

    for obj in objects:
        file_name = os.path.basename(obj['Key'])
        local_file_path = os.path.join(download_directory, file_name)
        download_file(bucket_name, obj['Key'], local_file_path)

# Пример использования
download_all_files('prdsaphistory')
