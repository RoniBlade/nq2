from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
import pandas as pd
from tabulate import tabulate
import requests

# Настройки сервера и User-Agent
SERVER = "https://biqas.pepsico.fas.tedo.ru"
USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"

# Функция для получения токена
def get_token(email, password):
    headers = {
        'Content-Type': 'application/json',
        'User-Agent': USER_AGENT
    }
    response = requests.post(f"{SERVER}/api/session", json={"username": email, "password": password}, headers=headers)
    response.raise_for_status()
    return response.json().get('id')

# Список пользователей и токенов
users = [
    ("biqas1@pepsico.fas.tedo.ru", "nNezxkO3Ol2EI9jFNgnon4ia81k"),
    ("testuserb@example.com", "Password2"),
    ("testuserc@example.com", "Password3"),
]
tokens = {user[0]: get_token(user[0], user[1]) for user in users}


def run_report(email, token, report_name, report_details, code):
    headers = {
        'Content-Type': 'application/json',
        'User-Agent': USER_AGENT,
        'X-Metabase-Session': token
    }

    # Переделаем обновление кода компании в параметрах запроса
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
    response.raise_for_status()
    try:
        json_data = response.json()
        if 'error' in json_data:
            error_message = json_data['error']
            print("Сообщение об ошибке:", error_message)
    except ValueError:
        print("Ответ не в формате JSON:", response.text)
    end_time = datetime.now()
    execution_time = (end_time - start_time).total_seconds()
    print(
        f"Отчет '{report_name}' для пользователя {email} для кода {code} и параметров: {[param['value'] for param in updated_details['parameters']]} завершен в {end_time.strftime('%Y-%m-%d %H:%M:%S')}, время выполнения: {execution_time} сек")
    return {'Пользователь': email, 'Отчет': report_name, 'Код компании': code,
            'Параметры': [param['value'] for param in updated_details['parameters']],
            'Время выполнения (сек)': execution_time}

# Функция для запуска отчетов для одного пользователя и одного кода компании
def run_reports_for_user(email, token, query, code):
    report_name, report_details = query
    result = run_report(email, token, report_name, report_details, code)
    return [result]

# Список запросов
queries = [
    ("Регламентированные отчеты (форма 1, форма 2) v1", {"url": "https://biqas.pepsico.fas.tedo.ru/api/dashboard/8/dashcard/35/card/58/query",
                                                         "parameters": [
                                                             {"type": "string/=", "value": ["RU03"], "id": "a70b1d0",
                                                              "target": ["variable", ["template-tag", "comp"]]},
                                                             {"type": "date/single", "value": "2023-04-01",
                                                              "id": "94226b61",
                                                              "target": ["variable", ["template-tag", "date"]]},
                                                             {"type": "string/=", "value": ["RU01"], "id": "8d8dc526",
                                                              "target": ["variable", ["template-tag", "versn"]]}],
                                                          "dashboard_id": 8
              }
    ),
    ("Регламентированные отчеты (форма 1, форма 2) v2", {"url": "https://biqas.pepsico.fas.tedo.ru/api/dashboard/9/dashcard/34/card/57/query",
                 "parameters":[{"type":"string/=","value":["RU03"],"id":"3e76b7b","target":["variable",["template-tag","company"]]},{"type":"date/single","value":"2023-04-01","id":"3d40058b","target":["variable",["template-tag","date"]]},{"type":"string/=","value":["2022"],"id":"63eb572e","target":["variable",["template-tag","version"]]}],"dashboard_id":9}
    ),
    ("Оборотно сальдовая ведомость по счетам",
     {"url": "https://biqas.pepsico.fas.tedo.ru/api/dashboard/2/dashcard/27/card/49/query",
      "parameters":[{"type":"date/single","value":"2023-01-01","id":"72860db6","target":["variable",["template-tag","start_date"]]},{"type":"date/single","value":"2023-03-31","id":"4b402cbe","target":["variable",["template-tag","end_date"]]},{"type":"string/=","value":["RU03"],"id":"3c9da159","target":["variable",["template-tag","comp"]]}],"dashboard_id":2}     ),
    ("Оборотно-сальдовая ведомость по контрагентам",
     {"url": "https://biqas.pepsico.fas.tedo.ru/api/dashboard/2/dashcard/39/card/65/query",
      "parameters":[{"type":"date/single","value":"2023-01-01","id":"72860db6","target":["variable",["template-tag","start_date"]]},{"type":"date/single","value":"2023-03-31","id":"4b402cbe","target":["variable",["template-tag","end_date"]]},{"type":"string/=","value":["RU03"],"id":"3c9da159","target":["variable",["template-tag","comp"]]},{"type":"string/=","value":["Поставщик"],"id":"f58c1193","target":["variable",["template-tag","ven_type"]]},{"type":"string/=","value":["-"],"id":"bc353b5d","target":["variable",["template-tag","lifnr"]]},{"type":"string/=","value":["-"],"id":"c9d1da59","target":["variable",["template-tag","kunnr"]]},{"type":"string/=","value":["No"],"id":"a81bef2b","target":["variable",["template-tag","flag"]]}],"dashboard_id":2}
     ),
    ("Отчет по Запасам",
     {"url": "https://biqas.pepsico.fas.tedo.ru/api/dashboard/11/dashcard/32/card/54/query",
      "parameters":[{"type":"string/=","value":["RU03"],"id":"8eae91dd","target":["variable",["template-tag","bukrs"]]},{"type":"string/=","value":["2023"],"id":"26120b1d","target":["variable",["template-tag","year"]]},{"type":"string/=","value":["001"],"id":"b71f1e23","target":["variable",["template-tag","poper"]]},{"type":"string/=","value":["-"],"id":"a7cc424d","target":["variable",["template-tag","matnr"]]}],"dashboard_id":11}
     ),
    ("Регистр учета ВНА",
     {"url": "https://biqas.pepsico.fas.tedo.ru/api/dashboard/3/dashcard/25/card/41/query",
      "parameters":[{"type":"string/=","value":["RU03"],"id":"89e2a4dc","target":["variable",["template-tag","comp"]]},{"type":"string/=","value":["2023"],"id":"ee80025a","target":["variable",["template-tag","gjahr"]]},{"type":"string/=","value":["1"],"id":"f34efa12","target":["variable",["template-tag","peraf"]]},{"type":"string/=","value":["-"],"id":"ac0c5519","target":["variable",["template-tag","asset"]]},{"type":"string/=","value":["-"],"id":"3eea3ab9","target":["variable",["template-tag","as_sub"]]},{"type":"string/=","value":["-"],"id":"f73327a3","target":["variable",["template-tag","as_class"]]},{"type":"string/=","value":["-"],"id":"ddb0eb7","target":["variable",["template-tag","plant"]]}],"dashboard_id":3}
     ),
    ("Регистр учета РБП",
     {"url": "https://biqas.pepsico.fas.tedo.ru/api/dashboard/6/dashcard/26/card/44/query",
      "parameters":[{"type":"string/=","value":["RU03"],"id":"f7f28ca7","target":["variable",["template-tag","comp"]]},{"type":"date/single","value":"2023-01-31","id":"2199ae18","target":["variable",["template-tag","end_date"]]},{"type":"string/=","value":["-"],"id":"85851c06","target":["variable",["template-tag","acc_obj_cat"]]},{"type":"string/=","value":["-"],"id":"b7636ccd","target":["variable",["template-tag","acc_object"]]},{"type":"string/=","value":["-"],"id":"d73c5d2e","target":["variable",["template-tag","acc_obj_num"]]}],"dashboard_id":6}
     ),
    ("Реестр бухгалтерских документов",
     {"url": "https://biqas.pepsico.fas.tedo.ru/api/dashboard/1/dashcard/37/card/62/query",
      "parameters":[{"type":"date/single","value":"2023-01-01","id":"d1d13059","target":["variable",["template-tag","start_date"]]},{"type":"date/single","value":"2023-01-31","id":"e0fe5040","target":["variable",["template-tag","end_date"]]},{"type":"string/=","value":["RU03"],"id":"b1059afc","target":["variable",["template-tag","comp"]]},{"type":"string/=","value":["0001000061"],"id":"fd95978e","target":["variable",["template-tag","account"]]},{"type":"string/=","value":["-"],"id":"3ed42146","target":["variable",["template-tag","ven_type"]]},{"type":"string/=","value":["-"],"id":"3007acad","target":["variable",["template-tag","lifnr"]]},{"type":"string/=","value":["-"],"id":"86342389","target":["variable",["template-tag","kunnr"]]},{"type":"string/=","value":["-"],"id":"552db31f","target":["variable",["template-tag","blart"]]},{"type":"string/=","value":["-"],"id":"c648212a","target":["variable",["template-tag","belnr"]]}],"dashboard_id":1}
     ),

    ("Карточка счета",
     {"url": "https://biqas.pepsico.fas.tedo.ru/api/dashboard/7/dashcard/17/card/14/query",
      "parameters":[{"type":"string/=","value":["0001000061"],"id":"fda9b4e4","target":["variable",["template-tag","account"]]},{"type":"string/=","value":["RU03"],"id":"38450c3e","target":["variable",["template-tag","comp"]]},{"type":"date/single","value":"2023-01-01","id":"a65cdd8c","target":["variable",["template-tag","start_date"]]},{"type":"date/single","value":"2023-01-31","id":"c6d91509","target":["variable",["template-tag","end_date"]]}],"dashboard_id":7}
     ),

    ("План счетов",
     {"url": "https://biqas.pepsico.fas.tedo.ru/api/dashboard/4/dashcard/30/card/52/query",
      "parameters":[{"type":"string/=","value":["RU03"],"id":"50113db7","target":["variable",["template-tag","comp"]]},{"type":"string/=","value":["PI00"],"id":"a3333136","target":["variable",["template-tag","chart"]]}],"dashboard_id":4}
     ),
    ("Отчет по результатам переоценки на отчетную дату",
     {"url": "https://biqas.pepsico.fas.tedo.ru/api/dashboard/10/dashcard/28/card/50/query",
      "parameters":[{"type":"string/=","value":["RU03"],"id":"14d77ab0","target":["variable",["template-tag","comp"]]},{"type":"date/single","value":"2023-02-28","id":"c44a508a","target":["variable",["template-tag","datum"]]},{"type":"string/=","value":["-"],"id":"38171b25","target":["variable",["template-tag","lifnr"]]},{"type":"string/=","value":["-"],"id":"86442b12","target":["variable",["template-tag","kunnr"]]}],"dashboard_id":10}
     ),

]

# Запуск отчётов
def select_and_run_reports():
    # Вывод списка отчётов для выбора
    print("Список доступных отчётов:")
    for idx, query in enumerate(queries):
        print(f"{idx + 1}. {query[0]}")

    # Выбор отчётов для запуска
    selected_reports = input("Введите номера отчётов для запуска через запятую: ").split(',')
    selected_reports = [int(num.strip()) - 1 for num in selected_reports if num.strip().isdigit() and int(num.strip()) - 1 in range(len(queries))]

    all_results = []
    for idx in selected_reports:
        query = queries[idx]
        results = []
        with ThreadPoolExecutor(max_workers=len(users)) as executor:
            futures = []
            for user in users:
                for code in ["RU03", "RU04", "RU20"]:
                    future = executor.submit(run_reports_for_user, user[0], tokens[user[0]], query, code)
                    futures.append(future)
            for future in as_completed(futures):
                results.extend(future.result())
        df = pd.DataFrame(results)
        df.sort_values(by=['Пользователь', 'Отчет'], inplace=True)
        print(tabulate(df, headers='keys', tablefmt='psql'))
        all_results.extend(results)

# Запускаем функцию выбора и запуска отчётов
select_and_run_reports()

#4,8,9

