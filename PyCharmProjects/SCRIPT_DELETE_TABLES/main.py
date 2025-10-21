import pandas as pd

# Чтение данных из текущего Excel-файла
file_path = 'report_results.xlsx'  # Путь к вашему файлу
data = pd.read_excel(file_path, engine='openpyxl')

# Преобразование данных
data.insert(0, 'Номер', range(1, len(data) + 1))  # Добавление номера строк

# Добавление колонки "Комментарий"
def compare_last_two_weeks(row):
    columns = [col for col in data.columns if col.startswith('Неделя')]
    if len(columns) < 2:
        return "Недостаточно данных для сравнения"

    last_week = row[columns[-1]]
    previous_week = row[columns[-2]]

    if pd.isna(last_week) or pd.isna(previous_week):
        return "Недостаточно данных для сравнения"

    if last_week > previous_week:
        return f"Время увеличилось на {last_week - previous_week:.2f} сек"
    elif last_week < previous_week:
        return f"Время уменьшилось на {previous_week - last_week:.2f} сек"
    else:
        return "Время не изменилось"

# Преобразование названий недель
columns_mapping = {col: f"Неделя {col}" if isinstance(col, int) else col for col in data.columns}
data.rename(columns=columns_mapping, inplace=True)

data['Комментарий'] = data.apply(compare_last_two_weeks, axis=1)

# Сохранение преобразованных данных в новый Excel-файл
output_path = 'transformed_report.xlsx'
data.to_excel(output_path, index=False)

print(f"Данные успешно преобразованы и сохранены в файл {output_path}")
