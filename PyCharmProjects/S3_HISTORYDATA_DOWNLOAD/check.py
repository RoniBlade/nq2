import pandas as pd


def check_numeric_values_in_column(input_file, column_name):
    chunk_size = 10000  # размер чанка для чтения файла частями
    non_numeric_values = {}  # Словарь для хранения ненумерических значений
    total_rows = 0  # Общее количество обработанных строк

    for chunk in pd.read_csv(input_file, sep=';', chunksize=chunk_size, dtype=str):
        if column_name in chunk.columns:
            for index, value in chunk[column_name].items():
                total_rows += 1  # Увеличиваем общий счетчик строк

                try:
                    # Пробуем преобразовать значение в число (float)
                    float(value)
                except ValueError:
                    # Если не удалось преобразовать, сохраняем значение и увеличиваем счетчик
                    if value in non_numeric_values:
                        non_numeric_values[value] += 1
                    else:
                        non_numeric_values[value] = 1

                    # Выводим индекс и строку с ненумерическим значением
                    print(f"Ненумерическое значение найдено в строке {total_rows} (индекс в чанке: {index}): {value}")

        # Промежуточный вывод каждые 10,000 строк
        print(f"Обработано {total_rows} строк. Найдено ненумерических значений: {len(non_numeric_values)}")

    # Финальный вывод
    if non_numeric_values:
        print(f"\nНайдены ненумерические значения в столбце {column_name}:")
        for value, count in non_numeric_values.items():
            print(f"Значение: {value}, Количество: {count}")
    else:
        print(f"\nВсе значения в столбце {column_name} являются числовыми.")


# Пример использования
input_file = 'bseg_export.csv'  # Убедитесь, что этот путь правильный
column_name = 'FDWBT'

check_numeric_values_in_column(input_file, column_name)
