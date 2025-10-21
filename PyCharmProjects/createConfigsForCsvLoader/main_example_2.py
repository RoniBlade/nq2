import pandas as pd
import re

# Имя Excel-файла
file_path = '../pythonProject3/SI FCST Evolution.xlsx'

# Получаем имя файла без расширения
file_name = file_path.split('/')[-1].split('.')[0]

# Читаем все листы из Excel-файла, указывая, что названия столбцов находятся во второй строке (header=1)
sheets = pd.read_excel(file_path, sheet_name=None, header=0)

# Получаем список всех листов
all_sheet_names = list(sheets.keys())

# Запрашиваем у пользователя, какие листы использовать, или использовать все
input_sheets = input(
    f"Введите названия листов, которые нужно использовать (по одному на строку, оставьте пустым для использования всех листов):\n").strip()

if input_sheets:
    # Разбиваем многострочный ввод на список и удаляем лишние пробелы
    selected_sheets = [sheet.strip() for sheet in input_sheets.splitlines() if sheet.strip()]
else:
    selected_sheets = all_sheet_names


# Функция для преобразования строки в CamelCase и удаления специальных символов, включая кириллические буквы
def to_camel_case(name):
    # Преобразуем имя в строку
    name = str(name)
    # Удаляем специальные символы, оставляя буквы, цифры и пробелы (как латинские, так и кириллические)
    name = re.sub(r'[^a-zA-Zа-яА-Я0-9 ]', '', name)
    parts = name.split()

    # Если parts пуст, возвращаем значение по умолчанию
    if not parts:
        return "unnamedColumn"

    # Преобразуем в CamelCase
    return parts[0].lower() + ''.join(word.capitalize() for word in parts[1:])


# Функция для формирования имени таблицы с первой заглавной буквой
def format_table_name(file_name, sheet_name):
    # Преобразуем оба имени в CamelCase
    formatted_file_name = to_camel_case(file_name).capitalize()
    formatted_sheet_name = to_camel_case(sheet_name).capitalize()
    return f"{formatted_file_name}_{formatted_sheet_name}"


# Функция для определения типа данных столбца
def get_sql_type(dtype):
    if pd.api.types.is_integer_dtype(dtype):
        return 'NUMERIC'  # Используем NUMERIC для целых чисел
    elif pd.api.types.is_float_dtype(dtype):
        return 'NUMERIC(15, 6)'  # Используем NUMERIC с указанием точности и масштаба для чисел с плавающей точкой
    elif pd.api.types.is_bool_dtype(dtype):
        return 'BOOLEAN'
    else:
        return 'VARCHAR(255)'


# Генерируем SQL-запросы для выбранных таблиц
for sheet_name in selected_sheets:
    if sheet_name in sheets:
        data = sheets[sheet_name]

        # Формируем название таблицы с первой заглавной буквой и добавляем схему
        table_name = f"loading_businessfiles.{format_table_name(file_name, sheet_name)}"

        # Генерируем часть скрипта с определением столбцов
        columns = []
        for col_name, dtype in data.dtypes.items():
            # Преобразуем название столбца в CamelCase и убираем спецсимволы
            camel_case_col_name = to_camel_case(col_name)
            sql_type = get_sql_type(dtype)
            columns.append(f"    {camel_case_col_name} {sql_type}")

        columns_script = ",\n".join(columns)

        # Формируем полный SQL-запрос для создания таблицы
        sql_script = f"CREATE TABLE {table_name} (\n{columns_script}\n);\n"

        # Выводим скрипт
        print(sql_script)
    else:
        print(f"Лист '{sheet_name}' не найден в файле.")
