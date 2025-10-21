import csv


def check_and_fix_quotes(input_file, output_file=None):
    fixed_rows = []
    total_rows = 0

    with open(input_file, 'r', encoding='utf-8') as file:
        reader = csv.DictReader(file, delimiter=';', quoting=csv.QUOTE_NONE)

        for row in reader:
            total_rows += 1
            fixed_row = {}

            for column_name, value in row.items():
                # Удаление кавычек вокруг значений
                if value:
                    value = value.strip('"')

                # Удаление значений "00000000"
                if value == '00000000':
                    value = ''  # Оставляем ячейку пустой

                fixed_row[column_name] = value

            fixed_rows.append(fixed_row)

    # Сохранение результатов
    if output_file:
        with open(output_file, 'w', newline='', encoding='utf-8') as file:
            fieldnames = reader.fieldnames
            writer = csv.DictWriter(file, fieldnames=fieldnames, delimiter=';', quoting=csv.QUOTE_NONE, escapechar='\\')

            writer.writeheader()
            writer.writerows(fixed_rows)

        print(f'Файл успешно сохранён в {output_file}.')


# Пример использования
input_file = 'bseg_export7.csv'
output_file = 'C:\\Users\\Adam\\Desktop\\problematic_rows_1' + input_file

check_and_fix_quotes(input_file, output_file)