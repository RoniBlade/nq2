import os

def split_csv_file(input_file, output_dir, chunk_size_lines):
    if not output_dir.endswith('/'):
        output_dir += '/'

    file_number = 1
    current_line_count = 0
    current_output_file = None

    with open(input_file, 'r', encoding='utf-8') as infile:
        header = infile.readline()  # читаем заголовок
        print(f"Заголовок: {header.strip()}")  # Выводим заголовок
        for line_number, line in enumerate(infile, 1):
            if current_output_file is None:
                output_file_path = f'{output_dir}bseg_export{file_number}.csv'
                current_output_file = open(output_file_path, 'w', encoding='utf-8')
                current_output_file.write(header)  # записываем заголовок в новый файл
                print(f"\nСоздан файл: {output_file_path}")

            current_output_file.write(line)
            current_line_count += 1

            if current_line_count >= chunk_size_lines:
                current_output_file.close()
                print(f"Файл {output_file_path} завершен. Количество строк: {current_line_count}")
                current_line_count = 0
                file_number += 1
                current_output_file = None

        if current_output_file:
            current_output_file.close()
            print(f"Файл {output_file_path} завершен. Количество строк: {current_line_count}")

    print("\nПроцесс разделения файла завершен.")

# Пример использования
input_file = 'bseg_export.csv'  # Убедитесь, что этот путь правильный
output_dir = 'C:/Users/Adam/Desktop/bseg_split/'  # Папка на рабочем столе или другая доступная папка
chunk_size_lines = 50000

split_csv_file(input_file, output_dir, chunk_size_lines)
