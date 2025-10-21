import pandas as pd
import re
import psycopg2

# Параметры подключения к базе данных PostgreSQL
pg_conn_params = {
    'dbname': 'postgres',
    'user': 'superuser',
    'password': 'rOGvBCBRDpsWIzyg4epLP7Ow',
    'host': '10.250.10.5',
    'port': '5432'
}

# Имя Excel-файла
file_path = '2_MasterfileClients.xlsx'

# Функция для приведения строки к нижнему регистру и удаления специальных символов
def clean_column_name(name):
    # Преобразуем имя в строку
    name = str(name)
    # Удаляем специальные символы, оставляя только буквы, цифры и пробелы
    name = re.sub(r'[^a-zA-Z0-9 ]', '', name)
    # Убираем пробелы и приводим к нижнему регистру
    return name.replace(' ', '').lower()

# Чтение Excel-файла для получения списка листов
sheets = pd.read_excel(file_path, sheet_name=None, header=0)  # Изменено: header=3, т.к. данные начинаются с 4-й строки
all_sheet_names = list(sheets.keys())

# Запрашиваем у пользователя название листа
input_sheet_name = input(f"Введите название листа (доступные листы: {', '.join(all_sheet_names)}):\n").strip()

# Проверяем, что лист существует в файле
if input_sheet_name in all_sheet_names:
    # Загружаем данные из указанного листа
    data = pd.read_excel(file_path, sheet_name=input_sheet_name, header=0)  # Изменено: header=3

    # Извлекаем и обрабатываем названия столбцов из Excel
    excel_column_names = data.columns
    cleaned_excel_columns = [clean_column_name(col) for col in excel_column_names]

    # Отладка: выводим очищенные названия столбцов из Excel
    print(f"Очищенные названия столбцов из Excel: {cleaned_excel_columns}")

    # Подключаемся к базе данных и получаем список столбцов из таблицы в схеме loading_businessfiles
    try:
        conn = psycopg2.connect(**pg_conn_params)
        cursor = conn.cursor()

        # Получаем список таблиц в схеме loading_businessfiles
        cursor.execute(f"""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'loading_businessfiles'
        """)
        tables = cursor.fetchall()
        print(f"Доступные таблицы в схеме loading_businessfiles: {[table[0] for table in tables]}")

        # Запрашиваем у пользователя название таблицы для сопоставления
        input_table_name = input("Введите название таблицы из схемы loading_businessfiles для сопоставления:\n").strip()

        if (input_table_name,) in tables:
            # Получаем список столбцов выбранной таблицы
            cursor.execute(f"""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'loading_businessfiles'
                AND table_name = '{input_table_name}'
            """)
            db_columns = [row[0].lower() for row in cursor.fetchall()]  # Приводим к нижнему регистру

            # Отладка: выводим названия столбцов из базы данных
            print(f"Названия столбцов из базы данных: {db_columns}")

            # Формируем сопоставление между названиями столбцов Excel и столбцами базы данных
            column_mapping = []
            for i, excel_col in enumerate(cleaned_excel_columns):
                if excel_col in db_columns:
                    column_mapping.append(f"{i + 1}={excel_col}")

            # Формируем строку конфигурации для маппинга
            config_mapping = f"excel.columnMapping={';'.join(column_mapping)}\nexcel.firstDataRow=4\nexcel.sheetName={input_sheet_name}"

            # Выводим конфиг-маппинг
            print("\nСгенерированный конфиг-маппинг:")
            print(config_mapping)
        else:
            print(f"Таблица '{input_table_name}' не найдена в схеме loading_businessfiles.")

    except Exception as e:
        print(f"Ошибка при подключении к базе данных: {e}")
    finally:
        if conn:
            cursor.close()
            conn.close()
else:
    print(f"Лист '{input_sheet_name}' не найден в файле.")
