import psycopg2
import logging

# Настройка логирования
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')

# Параметры для подключения к базе данных PostgreSQL
db_config = {
    'dbname': 'postgres',
    'user': 'superuser',
    'password': 'rOGvBCBRDpsWIzyg4epLP7Ow',
    'host': '185.53.105.10',
    'port': '80'
}

# Схема для проверки таблиц
schema_name = 'edwmappings'

# Подключение к базе данных
def connect_db():
    return psycopg2.connect(**db_config)

# Получение списка таблиц из схемы
def get_table_names(conn, schema):
    with conn.cursor() as cur:
        cur.execute("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = %s
        """, (schema,))
        return [table[0] for table in cur.fetchall()]

# Проверка загруженных данных в таблицах
def check_uploaded_data():
    try:
        with connect_db() as conn:
            # Получаем список всех таблиц из указанной схемы
            table_names = get_table_names(conn, schema_name)
            logging.info(f"Найдено {len(table_names)} таблиц в схеме '{schema_name}'.")

            with conn.cursor() as cur:
                for table_name in table_names:
                    try:
                        # Проверяем количество строк в каждой таблице
                        cur.execute(f"SELECT COUNT(*) FROM {schema_name}.{table_name}")
                        row_count = cur.fetchone()[0]
                        logging.info(f"Таблица '{table_name}' содержит {row_count} строк.")
                    except Exception as e:
                        logging.error(f"Ошибка при проверке таблицы '{table_name}': {e}")
    except Exception as e:
        logging.error(f"Ошибка подключения к базе данных: {e}")

if __name__ == "__main__":
    check_uploaded_data()
