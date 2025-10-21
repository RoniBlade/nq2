import requests
import psycopg2
import json

# Константы для API и базы данных
API_URL = "https://fias-public-service.nalog.ru/api/spas/v2.0/GetRegions"
API_KEY = "4ffa98b0-be07-488c-bb39-8df5d1ff1800"
DB_HOST = "185.53.105.10"
DB_PORT = 80
DB_NAME = "postgres"
DB_USER = "superuser"
DB_PASSWORD = "rOGvBCBRDpsWIzyg4epLP7Ow"
TABLE_NAME = "loading_fias.dictionary_fias_region"

# Параметры заголовков для запроса
headers = {
    "master-token": API_KEY,
    "Content-Type": "application/json"
}

def fetch_data():
    response = requests.get(API_URL, headers=headers)
    if response.status_code == 200:
        data = response.json()
        print("Полученные данные от API:", json.dumps(data, indent=4, ensure_ascii=False))  # Печать структуры
        return data.get("addresses", [])  # Извлечение данных из ключа "addresses"
    else:
        print(f"Ошибка: {response.status_code}, {response.text}")
        return None

def insert_data_to_db(data):
    conn = None
    try:
        # Подключение к базе данных
        conn = psycopg2.connect(
            host=DB_HOST,
            port=DB_PORT,
            dbname=DB_NAME,
            user=DB_USER,
            password=DB_PASSWORD
        )
        cursor = conn.cursor()

        # Подготовка SQL запроса для вставки данных
        insert_query = f"""
        INSERT INTO {TABLE_NAME} (
            object_id, object_level_id, operation_type_id, object_guid, address_type, full_name,
            region_code, is_active, path, address_details_postal_code, address_details_ifns_ul,
            address_details_ifns_fl, address_details_ifns_tul, address_details_ifns_tfl, address_details_okato,
            address_details_oktmo, address_details_kladr_code, address_details_cadastral_number,
            address_details_apart_building, address_details_remove_cadastr, address_details_oktmo_budget,
            address_details_is_adm_capital, address_details_is_mun_capital, successor_ref, hierarchy_object_type,
            hierarchy_region_code, hierarchy_name, hierarchy_type_name, hierarchy_type_short_name,
            hierarchy_type_form_code, hierarchy_object_id, hierarchy_object_level_id, hierarchy_object_guid,
            hierarchy_full_name, hierarchy_full_name_short, hierarchy_kladr_code, federal_district_id,
            federal_district_full_name, federal_district_short_name, federal_district_center_id
        ) VALUES (
            %(object_id)s, %(object_level_id)s, %(operation_type_id)s, %(object_guid)s, %(address_type)s, %(full_name)s,
            %(region_code)s, %(is_active)s, %(path)s, %(postal_code)s, %(ifns_ul)s,
            %(ifns_fl)s, %(ifns_tul)s, %(ifns_tfl)s, %(okato)s,
            %(oktmo)s, %(kladr_code)s, %(cadastral_number)s,
            %(apart_building)s, %(remove_cadastr)s, %(oktmo_budget)s,
            %(is_adm_capital)s, %(is_mun_capital)s, %(successor_ref)s, %(object_type)s,
            %(hierarchy_region_code)s, %(name)s, %(type_name)s, %(type_short_name)s,
            %(type_form_code)s, %(hierarchy_object_id)s, %(hierarchy_object_level_id)s, %(hierarchy_object_guid)s,
            %(hierarchy_full_name)s, %(hierarchy_full_name_short)s, %(hierarchy_kladr_code)s, %(federal_district_id)s,
            %(federal_district_full_name)s, %(federal_district_short_name)s, %(federal_district_center_id)s
        )
        """

        # Вставка данных
        for entry in data:
            if entry is None:
                continue

            # Извлечение вложенных данных с проверкой на None
            address_details = entry.get("address_details") or {}
            hierarchy = (entry.get("hierarchy") or [{}])[0]  # Первый элемент в иерархии или пустой словарь
            federal_district = entry.get("federal_district") or {}

            # Создание словаря данных для вставки
            record = {
                "object_id": entry.get("object_id"),
                "object_level_id": entry.get("object_level_id"),
                "operation_type_id": entry.get("operation_type_id"),
                "object_guid": entry.get("object_guid"),
                "address_type": entry.get("address_type"),
                "full_name": entry.get("full_name"),
                "region_code": entry.get("region_code"),
                "is_active": entry.get("is_active"),
                "path": entry.get("path"),
                # address_details
                "postal_code": address_details.get("postal_code"),
                "ifns_ul": address_details.get("ifns_ul"),
                "ifns_fl": address_details.get("ifns_fl"),
                "ifns_tul": address_details.get("ifns_tul"),
                "ifns_tfl": address_details.get("ifns_tfl"),
                "okato": address_details.get("okato"),
                "oktmo": address_details.get("oktmo"),
                "kladr_code": address_details.get("kladr_code"),
                "cadastral_number": address_details.get("cadastral_number"),
                "apart_building": address_details.get("apart_building"),
                "remove_cadastr": address_details.get("remove_cadastr"),
                "oktmo_budget": address_details.get("oktmo_budget"),
                "is_adm_capital": address_details.get("is_adm_capital"),
                "is_mun_capital": address_details.get("is_mun_capital"),
                # successor_ref
                "successor_ref": entry.get("successor_ref"),
                # hierarchy
                "object_type": hierarchy.get("object_type"),
                "hierarchy_region_code": hierarchy.get("region_code"),
                "name": hierarchy.get("name"),
                "type_name": hierarchy.get("type_name"),
                "type_short_name": hierarchy.get("type_short_name"),
                "type_form_code": hierarchy.get("type_form_code"),
                "hierarchy_object_id": hierarchy.get("object_id"),
                "hierarchy_object_level_id": hierarchy.get("object_level_id"),
                "hierarchy_object_guid": hierarchy.get("object_guid"),
                "hierarchy_full_name": hierarchy.get("full_name"),
                "hierarchy_full_name_short": hierarchy.get("full_name_short"),
                "hierarchy_kladr_code": hierarchy.get("kladr_code"),
                # federal_district
                "federal_district_id": federal_district.get("id"),
                "federal_district_full_name": federal_district.get("full_name"),
                "federal_district_short_name": federal_district.get("short_name"),
                "federal_district_center_id": federal_district.get("center_id")
            }

            cursor.execute(insert_query, record)

        # Сохранение изменений
        conn.commit()
        print("Данные успешно загружены в базу данных.")

    except (Exception, psycopg2.DatabaseError) as error:
        print(f"Ошибка при подключении к базе данных: {error}")
        if conn:
            conn.rollback()
    finally:
        if conn:
            cursor.close()
            conn.close()

# Получение данных из API и загрузка их в базу данных
data = fetch_data()
if data:
    insert_data_to_db(data)
