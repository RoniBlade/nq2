import pandas as pd
from datetime import datetime
import math

# Загрузка данных из Excel файла
file_path = '../ada2.xlsx'


# hist_prices_df = pd.read_excel(file_path, sheet_name='Hist_prices')
# df_registry = pd.read_excel(file_path, sheet_name='Registry')


# def get_assets(row):
#     for j in range(1, 6):
#         current = df_registry.at[i, f'ticker {j}']
#         if isinstance(current, str):
#             df_new.at[i, 'Tickers'].append(current)


def get_options_data(df_registry):
    df_registry = (df_registry.loc[df_registry['product_type_code'] == 'АК'])
    df_registry.index = range(1, len(df_registry) + 1)
    df_new = pd.DataFrame({
        'Option #': range(1, len(df_registry) + 1),
        'S_inv': df_registry['input_volume'],
        'Contract date': [datetime.strptime(str(x), '%Y-%m-%d %H:%M:%S').date() for x in df_registry['contract_date']],
        'Expiration date': [datetime.strptime(str(x), '%Y-%m-%d %H:%M:%S').date() for x in
                            df_registry['expiration_date']],
        'Tickers': pd.Series(),
        'Assets': pd.Series(),
        'Exercise barrier': df_registry['exercise_barrier_rate'],
        'Barrier rate': df_registry['coupon_barrier_rate'],
        'Coupon': df_registry['coupon'],
        'Coupons': pd.Series()
    }, index=range(1, len(df_registry) + 1))

    # # Для дат купонов предполагается, что они находятся в столбцах AB, AC, AD, AE
    # for i in range(1, 37):
    #     arr = []
    #     column_name = f'Coupon date {i}'
    #     source_column_index = i + 25  # Начиная с 26-го столбца (AB) и далее
    #     df_new[column_name] = df_registry.iloc[:, source_column_index]

    start_col_index = df_registry.columns.get_loc('coupon_dates')

    for i in range(1, len(df_registry) + 1):
        arr = []
        for j in range(1, 6):
            current = df_registry.at[i, f'ticker {j}']
            if isinstance(current, str):
                arr.append(current)
        df_new.at[i, 'Tickers'] = tuple(sorted(arr))
        df_new.at[i, 'Assets'] = len(arr)

        coupons = []
        for k in range(start_col_index, len(df_registry.columns)):
            coupon_value = df_registry.iloc[i - 1, k]
            if pd.isnull(coupon_value):
                break
            # Преобразование timestamp в дату
            coupon_date = pd.to_datetime(coupon_value).date() if pd.notnull(coupon_value) else None
            coupons.append(coupon_date)
        df_new.at[i, 'Coupons'] = tuple(coupons)
    return df_new


def calculate_percentage_change(column):
    return column.pct_change(-1, fill_method=None) * 100


def get_return_per(df_hist_price):
    results_df = df_hist_price.iloc[:, 1:].apply(calculate_percentage_change)
    return results_df[:-1]


def calculate_std_dev_from_percentage_change(df, columns, num_rows):
    """
    Рассчитывает процентное изменение для всех столбцов DataFrame, за исключением первого,
    и затем вычисляет стандартное отклонение для выбранных столбцов на основе заданного количества строк.

    :param df: DataFrame, hist_prices_df
    :param columns: Список столбцов, для которых нужно рассчитать стандартное отклонение.
    :param num_rows: Количество строк, которые будут использоваться для расчета стандартного отклонения.
    :return: DataFrame со стандартными отклонениями для указанных столбцов.
    """
    # Расчет процентного изменения
    percentage_changes = df.iloc[:, 1:].pct_change(-1) * 100
    percentage_changes = percentage_changes[:-1]
    selected_df = percentage_changes[columns].iloc[0:num_rows]
    std_devs = selected_df.std(ddof=1)

    return std_devs.to_frame()

# Пример использования функции
# std_devs_df = calculate_std_dev_from_percentage_change(df, ['AFLT', 'GAZP', 'GMKN', 'SNGSP'], 46)

def extract_last_values(df, tickers):
    """
    Извлекает последнее значение для каждого тикера из DataFrame.

    :param df: DataFrame, hist_prices_df
    :param tickers: Список тикеров, для которых нужно извлечь значения.
    :return: Словарь с последними значениями для каждого тикера.
    """
    extracted_last_values = {}
    for i, ticker in enumerate(tickers):
        value = df[ticker].iloc[i] if i + 1 < len(df) else None
        extracted_last_values[ticker] = value

    return extracted_last_values

# Пример использования функции
#last_values = extract_last_values(df, ['AFLT', 'GAZP', 'GMKN', 'SNGSP'])

def create_filtered_dataframe(file_path, sheet_name, product_type):
    """
    Загружает DataFrame из Excel-файла, фильтрует его и создает новый DataFrame с заданными столбцами. df_registry

    :param file_path: Путь к Excel-файлу.
    :param sheet_name: Название листа в Excel-файле.
    :param product_type: Тип продукта для фильтрации.
    :return: Новый отфильтрованный DataFrame.
    """
    # Загрузка DataFrame
    df_registry = pd.read_excel(file_path, sheet_name=sheet_name)

    # Фильтрация строк
    df_filtered = df_registry.loc[df_registry['product_type_code'] == product_type]

    # Создание нового DataFrame с нужными столбцами
    df_new = pd.DataFrame()

    # Заполнение столбцов
    df_new['Option #'] = range(1, len(df_filtered) + 1)
    df_new['Product ID'] = df_filtered['product_id']
    df_new['Type'] = df_filtered['product_type_code']
    df_new['S_inv'] = df_filtered['input_volume']
    df_new['Contract date'] = df_filtered['contract_date']
    df_new['Expiration date'] = df_filtered['expiration_date']
    df_new['Exercise date'] = df_filtered['exercise_date'].fillna(" ")
    df_new['Ticker 1'] = df_filtered['ticker 1']
    df_new['Ticker 2'] = df_filtered['ticker 2']
    df_new['Ticker 3'] = df_filtered['ticker 3']
    df_new['Ticker 4'] = df_filtered['ticker 4']
    df_new['Ticker 5'] = df_filtered['ticker 5']
    df_new['Exercise barrier rate'] = df_filtered['exercise_barrier_rate']
    df_new['Coupon barrier rate'] = df_filtered['coupon_barrier_rate']
    df_new['Coupon'] = df_filtered['coupon']

    # Добавление дат купонов
    for i in range(1, 37):
        column_name = f'Coupon date {i}'
        source_column_index = i + 25  # Начиная с 26-го столбца (AB) и далее
        df_new[column_name] = df_filtered.iloc[:, source_column_index]

    return df_new

# Пример использования функции
#file_path = 'ada.xlsx'
#sheet_name = 'Registry'
#product_type = 'АК'
#new_df = create_filtered_dataframe(file_path, sheet_name, product_type)








