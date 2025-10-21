import pandas as pd
from openpyxl import Workbook
import logging

# Настройка логирования
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def adjust_column_width(ws):
    """
    Настраивает ширину столбцов по содержимому для улучшенного отображения.
    """
    logger.info("Настройка ширины столбцов в Excel...")
    for column_cells in ws.columns:
        max_length = 0
        column = column_cells[0].column_letter  # Получаем букву столбца
        for cell in column_cells:
            try:
                if cell.value:
                    max_length = max(max_length, len(str(cell.value)))
            except Exception as e:
                logger.warning(f"Ошибка при определении длины значения в ячейке: {e}")
        adjusted_width = (max_length + 2)
        ws.column_dimensions[column].width = adjusted_width
    logger.info("Ширина столбцов успешно настроена.")

def process_realization_blocks(df):
    """
    Обрабатывает DataFrame, разделяя его на блоки реализации.
    Возвращает список блоков с данными.
    """
    logger.info("Начало обработки данных для разделения на блоки реализации...")
    blocks = []
    current_block = None

    for i, row in df.iterrows():
        # Начало нового блока "Реализация"
        if pd.notna(row[0]) and str(row[0]).startswith("Реализация"):
            if current_block:
                blocks.append(current_block)
            current_block = {
                'Документ реализации': row[0],
                'Филиалы': [],
            }
            logger.info(f"Найден новый блок реализации: {row[0]}")

        # Обрабатываем строку с филиалом
        elif pd.notna(row[0]) and "ФИТ" in str(row[0]):
            filial_data = {
                'Филиал': row[0],
                'Поставщик': row[5] if pd.notna(row[5]) else None,
                'План первоначальный (КЛ)': row[8] if pd.notna(row[8]) else None,
                'Факт по филиалу': row[10] if pd.notna(row[10]) else None,
                'Факт по документу поступления': row[11] if pd.notna(row[11]) else None,
                'Резерв НВР': row[12] if pd.notna(row[12]) else None,
                'Документы поступления': []
            }
            current_block['Филиалы'].append(filial_data)
            logger.info(f"Добавлен филиал: {row[0]}")

        # Обрабатываем строку с документом поступления
        elif pd.notna(row[0]) and (str(row[0]).startswith("Поступление") or str(row[0]).startswith("Корректировка поступления")):
            document_postupleniya = {
                'Документ поступления': row[0]
            }
            if current_block and current_block['Филиалы']:
                current_block['Филиалы'][-1]['Документы поступления'].append(document_postupleniya)
                logger.info(f"Добавлен документ поступления: {row[0]}")

    if current_block:
        blocks.append(current_block)
    logger.info("Обработка данных завершена.")

    return blocks

def write_blocks_to_excel(blocks, output_path='Realization_Blocks_Transformed_Data_Unique.xlsx'):
    """
    Записывает блоки данных в новый Excel файл.
    """
    logger.info("Начало записи данных в Excel файл...")
    wb = Workbook()
    ws = wb.active
    ws.title = "Realization Blocks Data"

    headers = [
        'Документ реализации', 'Филиал', 'Документ поступления', 'Поставщик',
        'План первоначальный (КЛ)', 'Факт по филиалу', 'Факт по документу поступления', 'Резерв НВР'
    ]
    ws.append(headers)

    for block in blocks:
        for filial in block['Филиалы']:
            if filial['Документы поступления']:
                for doc in filial['Документы поступления']:
                    ws.append([
                        block['Документ реализации'],
                        filial['Филиал'],
                        doc['Документ поступления'],
                        filial['Поставщик'],
                        filial['План первоначальный (КЛ)'],
                        filial['Факт по филиалу'],
                        filial['Факт по документу поступления'],
                        filial['Резерв НВР']
                    ])
                    logger.info(f"Добавлена строка для документа поступления: {doc['Документ поступления']}")
            else:
                ws.append([
                    block['Документ реализации'],
                    filial['Филиал'],
                    None,
                    filial['Поставщик'],
                    filial['План первоначальный (КЛ)'],
                    filial['Факт по филиалу'],
                    filial['Факт по документу поступления'],
                    filial['Резерв НВР']
                ])
                logger.info(f"Добавлена строка для филиала без документа поступления: {filial['Филиал']}")

    adjust_column_width(ws)

    wb.save(output_path)
    logger.info(f"Данные успешно сохранены в файл {output_path}")

# Пример использования функции
example_data_path = 'Отчет НВР по ПТУ Часть 3.xlsx'  # Замените на путь к вашему файлу
example_data = pd.read_excel(example_data_path, sheet_name=None)

# Преобразуем данные в блоки
blocks = process_realization_blocks(example_data['Sheet1'])

# Записываем блоки в Excel
write_blocks_to_excel(blocks, 'Realization_Blocks_Transformed_Data_3.xlsx')
