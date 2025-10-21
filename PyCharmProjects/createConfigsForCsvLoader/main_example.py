import psycopg2

# Параметры подключения к базе данных
pg_conn_params = {
    'dbname': 'postgres',
    'user': 'superuser',
    'password': 'rOGvBCBRDpsWIzyg4epLP7Ow',
    'host': '10.250.10.5',
    'port': '5432'
}

# Соединение с базой данных
conn = psycopg2.connect(**pg_conn_params)
cursor = conn.cursor()

# Функция для получения списка таблиц из схемы
def get_tables_in_schema(schema_name):
    cursor.execute(f"""
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = '{schema_name}'
    """)
    return [row[0] for row in cursor.fetchall()]

# Функция для копирования структуры таблицы в новую схему
def copy_table_structure(table_name, source_schema, target_schema):
    cursor.execute(f"""
        CREATE TABLE IF NOT EXISTS {target_schema}.{table_name} 
        (LIKE {source_schema}.{table_name} INCLUDING ALL)
    """)
    conn.commit()

# Функция для копирования данных из одной таблицы в другую
def copy_table_data(table_name, source_schema, target_schema):
    cursor.execute(f"""
        INSERT INTO {target_schema}.{table_name}
        SELECT * FROM {source_schema}.{table_name}
    """)
    conn.commit()

# Основная функция для переноса всех таблиц
def transfer_schema(source_schema, target_schema, copy_data=False):
    tables = get_tables_in_schema(source_schema)
    for table in tables:
        print(f"Копируем структуру таблицы {table}...")
        copy_table_structure(table, source_schema, target_schema)
        if copy_data:
            print(f"Переносим данные из таблицы {table}...")
            copy_table_data(table, source_schema, target_schema)
    print(f"Перенос схемы {source_schema} в {target_schema} завершен.")

# Пример использования:
# Если вы хотите перенести только структуру таблиц, используйте:
transfer_schema('loading_saphistory', 'saphistory_backup')

# Если вы хотите перенести структуру и данные, установите флаг copy_data в True:
# transfer_schema('loading_saphistory', 'saphistory_backup', copy_data=True)

# Закрываем соединение
cursor.close()
conn.close()
