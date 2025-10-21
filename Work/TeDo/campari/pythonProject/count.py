import os
import logging
import csv

print("Start application")

logger = logging.getLogger('line_counter')
logging.basicConfig(level=logging.INFO,
                    filename="app_logs.log",
                    filemode="a",
                    format="%(asctime)s %(levelname)s %(message)s")

logging.info('-- НАЧАЛО ВЫПОЛНЕНИЯ ПРОГРАММЫ --')


def count_lines_in_file(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            lines = file.readlines()
            return len(lines)
    except Exception as e:
        logging.error(f'Ошибка при подсчете строк в файле {file_path}: {e}')
        return None


def save_line_counts_to_csv(local_directory, csv_file_path):
    with open(csv_file_path, 'w', newline='', encoding='utf-8') as csvfile:
        csv_writer = csv.writer(csvfile)
        csv_writer.writerow(['Filename', 'Line Count'])

        for root, dirs, files in os.walk(local_directory):
            for filename in files:
                local_path = os.path.join(root, filename)
                line_count = count_lines_in_file(local_path)
                if line_count is not None:
                    csv_writer.writerow([filename, line_count])
                    logging.info(f'Количество строк в файле {filename}: {line_count}')
                    print(f'Количество строк в файле {filename}: {line_count}')


local_directory = "C:\\Users\\Adam\\Documents\\Work\\campari\\pythonProject\\files"
csv_file_path = os.path.join(local_directory, "line_counts.csv")
save_line_counts_to_csv(local_directory, csv_file_path)

logging.info('-- ЗАВЕРШЕНИЕ ВЫПОЛНЕНИЯ ПРОГРАММЫ --')
print('-- ЗАВЕРШЕНИЕ ВЫПОЛНЕНИЯ ПРОГРАММЫ --')
