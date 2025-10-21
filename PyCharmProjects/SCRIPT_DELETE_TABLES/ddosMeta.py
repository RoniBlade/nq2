from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
import pandas as pd
from tabulate import tabulate
import requests
import logging

# Настройки сервера и User-Agent
SERVER = "https://biqas.pepsico.fas.tedo.ru"
USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"

# Настройка логирования
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Функция для получения токена
def get_token(email, password):
    logging.info(f"Получение токена для {email}")
    headers = {
        'Content-Type': 'application/json',
        'User-Agent': USER_AGENT
    }
    response = requests.post(f"{SERVER}/api/session", json={"username": email, "password": password}, headers=headers)
    logging.info(f"Ответ от сервера для получения токена: {response.status_code} - {response.text}")
    response.raise_for_status()
    return response.json().get('id')

# Функция для создания пользователя
def create_user(first_name, last_name, email, password, token):
    logging.info(f"Создание пользователя {email}")
    headers = {
        'Content-Type': 'application/json',
        'User-Agent': USER_AGENT,
        'X-Metabase-Session': token
    }
    data = {
        "first_name": first_name,
        "last_name": last_name,
        "email": email,
        "password": password
    }
    response = requests.post(f"{SERVER}/api/user/", json=data, headers=headers)
    logging.info(f"Ответ от сервера для создания пользователя {email}: {response.status_code} - {response.text}")
    if response.status_code == 400:
        error_message = response.json().get("errors", {}).get("email", [""])[0]
        if "уже занят" in error_message:
            logging.warning(f"Пользователь {email} уже создан")
            return False
        else:
            logging.error(f"Ошибка при создании пользователя {email}: {response.json()}")
            response.raise_for_status()
    else:
        response.raise_for_status()
    return True

# Генерация 50 фиктивных пользователей
def generate_users(num_users=50):
    users = []
    for i in range(1, num_users + 1):
        first_name = f"DDOS_V2_{i}"
        last_name = f"User_{i}"
        email = f"ddos_v2_{i}@example.com"
        password = first_name  # Пароль совпадает с именем
        users.append((first_name, last_name, email, password))
    return users

# Получение токена суперпользователя для создания новых пользователей
superuser_email = "biqas1@pepsico.fas.tedo.ru"
superuser_password = "nNezxkO3Ol2EI9jFNgnon4ia81k"

try:
    superuser_token = get_token(superuser_email, superuser_password)
except requests.exceptions.HTTPError as e:
    logging.critical(f"Не удалось получить токен суперпользователя: {e}")
    exit(1)

users = generate_users(50)
# Создание пользователей и получение их токенов
tokens = {}
for user in users:
    first_name, last_name, email, password = user
    try:
        if create_user(first_name, last_name, email, password, superuser_token):
            tokens[email] = get_token(email, password)
        else:
            tokens[email] = get_token(email, password)
    except requests.exceptions.HTTPError as e:
        logging.error(f"Не удалось создать пользователя {email}: {e}")
        try:
            tokens[email] = get_token(email, password)
        except requests.exceptions.HTTPError as auth_error:
            logging.error(f"Не удалось авторизовать пользователя {email}: {auth_error}")
            continue

def run_report(email, token, report_name, report_details, code):
    logging.info(f"Запуск отчета '{report_name}' для пользователя {email} с кодом {code}")
    headers = {
        'Content-Type': 'application/json',
        'User-Agent': USER_AGENT,
        'X-Metabase-Session': token
    }

    updated_parameters = []
    for param in report_details["parameters"]:
        if "company" in param.get("target", "")[1] or "comp" in param.get("target", "")[1]:
            updated_parameters.append({
                **param,
                "value": [code]
            })
        else:
            updated_parameters.append(param)

    updated_details = {
        **report_details,
        "parameters": updated_parameters
    }

    start_time = datetime.now()
    response = requests.post(updated_details["url"], json=updated_details, headers=headers)
    logging.info(f"Ответ от сервера для отчета '{report_name}': {response.status_code}")
    response.raise_for_status()
    try:
        json_data = response.json()
        if 'error' in json_data:
            error_message = json_data['error']
            logging.error(f"Сообщение об ошибке: {error_message}")
    except ValueError:
        logging.error(f"Ответ не в формате JSON: {response.text}")
    end_time = datetime.now()
    execution_time = (end_time - start_time).total_seconds()
    logging.info(
        f"Отчет '{report_name}' для пользователя {email} для кода {code} и параметров: {[param['value'] for param in updated_details['parameters']]} завершен в {end_time.strftime('%Y-%m-%d %H:%M:%S')}, время выполнения: {execution_time} сек")
    return {'Пользователь': email, 'Отчет': report_name, 'Код компании': code,
            'Параметры': [param['value'] for param in updated_details['parameters']],
            'Время выполнения (сек)': execution_time}

# Функция для запуска отчетов для одного пользователя и одного кода компании
def run_reports_for_user(email, token, report_name, report_details, code):
    return run_report(email, token, report_name, report_details, code)

# Список запросов
queries = [
    ("Оборотно-сальдовая ведомость по контрагентам",
     {"url": "https://biqas.pepsico.fas.tedo.ru/api/dashboard/2/dashcard/39/card/65/query",
      "parameters":[{"type":"date/single","value":"2023-01-01","id":"72860db6","target":["variable",["template-tag","start_date"]]},{"type":"date/single","value":"2023-03-31","id":"4b402cbe","target":["variable",["template-tag","end_date"]]},{"type":"string/=","value":["RU03"],"id":"3c9da159","target":["variable",["template-tag","comp"]]},{"type":"string/=","value":["Поставщик"],"id":"f58c1193","target":["variable",["template-tag","ven_type"]]},{"type":"string/=","value":["-"],"id":"bc353b5d","target":["variable",["template-tag","lifnr"]]},{"type":"string/=","value":["-"],"id":"c9d1da59","target":["variable",["template-tag","kunnr"]]},{"type":"string/=","value":["No"],"id":"a81bef2b","target":["variable",["template-tag","flag"]]}],"dashboard_id":2}
     ),
]

def select_and_run_reports():
    all_results = []
    with ThreadPoolExecutor(max_workers=50) as executor:  # Увеличили количество потоков до 50
        futures = []
        for report_name, report_details in queries:
            for user in users:
                email = user[2]
                if email in tokens:
                    for code in ["RU03", "RU04", "RU20"]:
                        future = executor.submit(run_reports_for_user, email, tokens[email], report_name, report_details, code)
                        futures.append(future)
        for future in as_completed(futures):
            all_results.append(future.result())

    if all_results:
        df = pd.DataFrame(all_results)

        # Сохранение результатов в Excel файл
        df.to_excel("report_results.xlsx", index=False)

        print(tabulate(df, headers='keys', tablefmt='psql'))
    else:
        logging.info("Нет результатов для отображения.")

# Запускаем функцию выбора и запуска отчётов
select_and_run_reports()
