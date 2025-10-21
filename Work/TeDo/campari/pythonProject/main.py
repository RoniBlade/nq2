import boto3
import os
import logging

print("Start application")

logger = logging.getLogger('s3uploader')
logging.basicConfig(level=logging.INFO,
                    filename="app_logs.log",
                    filemode="a",
                    format="%(asctime)s %(levelname)s %(message)s")

logging.info('-- НАЧАЛО ВЫПОЛНЕНИЯ ПРОГРАММЫ --')

AWS_ACCESS_KEY_ID = '00f44b7116d28d3bb60d'
AWS_SECRET_ACCESS_KEY = 'LtVeOKCNswK40cjaki3/8sHTs4LwVxaz4FPNPyHI'
S3_BUCKET_NAME = 'prdsaphistory'
S3_ENDPOINT_URL = 'https://s3.objstor.cloud4u.com:443'

# Создаем сессию и клиента S3
session = boto3.session.Session()
s3 = session.client(
    service_name='s3',
    aws_access_key_id=AWS_ACCESS_KEY_ID,
    aws_secret_access_key=AWS_SECRET_ACCESS_KEY,
    endpoint_url=S3_ENDPOINT_URL,
    verify=False
)


def upload_files_to_s3(local_directory, bucket_name):
    for root, dirs, files in os.walk(local_directory):
        for filename in files:
            local_path = os.path.join(root, filename)
            relative_path = os.path.relpath(local_path, local_directory)
            s3_path = relative_path.replace("\\", "/")  # Для совместимости с S3 путями

            print(f"Uploading {local_path} to {bucket_name}/{s3_path}")
            logging.info(f"Uploading {local_path} to {bucket_name}/{s3_path}")

            try:
                s3.upload_file(local_path, bucket_name, s3_path)
                logging.info(f'Файл {filename} успешно загружен в {bucket_name}/{s3_path}.')
            except Exception as e:
                print(f'Error uploading {filename}: {e}')
                logging.error(f'Ошибка при загрузке файла {filename}: {e}')


local_directory = "C:\\Users\\Adam\\Documents\\Work\\campari\\pythonProject\\files"
upload_files_to_s3(local_directory, S3_BUCKET_NAME)

logging.info('-- ЗАВЕРШЕНИЕ ВЫПОЛНЕНИЯ ПРОГРАММЫ --')
print('-- ЗАВЕРШЕНИЕ ВЫПОЛНЕНИЯ ПРОГРАММЫ --')
