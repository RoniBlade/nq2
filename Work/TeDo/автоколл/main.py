import random
from scipy.stats import norm
import time
import loads as load
from datetime import timedelta, date
import pandas as pd
import numpy as np
from datetime import datetime
from dateutil.relativedelta import relativedelta
import json
import math

sim_dates = []  # Р”Р°С‚С‹, РґР»СЏ РєРѕС‚РѕСЂС‹С… РЅСѓР¶РЅР° СЃРёРјСѓР»СЏС†РёСЏ.
file_path = 'ada.xlsx'
chol_matr = {}

#file_path_new = '/home/Shared/NIFI/NIFI_DATA/Auto/Pack1/ada.xlsx'
sheet_name = 'Hist_prices'


def date_factor(date1, date2):
    """
    РђРЅР°Р»РѕРі С„СѓРЅРєС†РёРё YEARFRAC РІ Excel.
    :param date1:
    :param date2:
    :return:
    """
    years_diff = relativedelta(date2, date1).years
    months_diff = relativedelta(date2, date1).months
    days_diff = relativedelta(date2, date1).days

    # РџРѕРґСЃС‡РµС‚ РґРѕР»Рё РіРѕРґР°
    return years_diff + months_diff / 12 + days_diff / 365


def get_valid_date(current_dt, is_sim=False):
    if is_sim:
        itr = len(sim_dates) - 1
        while current_dt < sim_dates[itr]:
            itr = itr - 1
        return sim_dates[itr]
    else:
        itr = 0
        while current_dt < hist_prices_df['Date'][itr]:
            itr = itr + 1
        return hist_prices_df['Date'][itr] if current_dt == hist_prices_df['Date'][itr] else hist_prices_df['Date'][
            itr - 1]


# Р¤СѓРЅРєС†РёСЏ РґР»СЏ СЃРµСЂРёР°Р»РёР·Р°С†РёРё РјР°С‚СЂРёС†С‹ РҐРѕР»РµС†РєРѕРіРѕ
def serialize_cholesky(matrix):
    return matrix.tolist()  # РџСЂРµРѕР±СЂР°Р·РѕРІР°РЅРёРµ np.array РІ СЃРїРёСЃРѕРє СЃРїРёСЃРєРѕРІ


# Р¤СѓРЅРєС†РёСЏ РґР»СЏ РґРµСЃРµСЂРёР°Р»РёР·Р°С†РёРё РјР°С‚СЂРёС†С‹ РҐРѕР»РµС†РєРѕРіРѕ
def deserialize_cholesky(serialized_matrix):
    return np.array(
        serialized_matrix)  # РџСЂРµРѕР±СЂР°Р·РѕРІР°РЅРёРµ СЃРїРёСЃРєР° СЃРїРёСЃРєРѕРІ РѕР±СЂР°С‚РЅРѕ РІ np.array


def get_base_price(tickers, contract):
    result = []
    # print(get_valid_date(contract))
    for ticker in tickers:
        result.append(hist_prices_df.loc[hist_prices_df['Date'] == get_valid_date(contract)][ticker].values[0])

    return result


def get_ticker_price(ticker, contract_date):
    try:
        row_position = hist_prices_df[hist_prices_df['Date'] == contract_date].index[0]
        column_position = hist_prices_df.columns.get_loc(ticker)
        return hist_prices_df.iloc[row_position, column_position]
    except (IndexError, KeyError) as e:
        print(f"РџСЂРѕРёР·РѕС€Р»Р° РѕС€РёР±РєР° РїСЂРё РѕР±СЂР°Р±РѕС‚РєРµ {ticker}: {e}")
        return ""


def trading_days_to_expiration(date_array, expiration_date):
    count = 0
    for date in date_array:
        if date <= expiration_date:
            count += 1
            # print(f"Р”Р°С‚Р° {date} <= {expiration_date} (СѓС‡РёС‚С‹РІР°РµС‚СЃСЏ)")
    return count


def calculate_std_dev_from_percentage_change(df, columns, num_rows):
    """
    :param df: DataFrame, hist_prices_df
    :param columns: РЎРїРёСЃРѕРє СЃС‚РѕР»Р±С†РѕРІ, РґР»СЏ РєРѕС‚РѕСЂС‹С… РЅСѓР¶РЅРѕ СЂР°СЃСЃС‡РёС‚Р°С‚СЊ СЃС‚Р°РЅРґР°СЂС‚РЅРѕРµ РѕС‚РєР»РѕРЅРµРЅРёРµ.
    :param num_rows: РљРѕР»РёС‡РµСЃС‚РІРѕ СЃС‚СЂРѕРє, РєРѕС‚РѕСЂС‹Рµ Р±СѓРґСѓС‚ РёСЃРїРѕР»СЊР·РѕРІР°С‚СЊСЃСЏ РґР»СЏ СЂР°СЃС‡РµС‚Р° СЃС‚Р°РЅРґР°СЂС‚РЅРѕРіРѕ РѕС‚РєР»РѕРЅРµРЅРёСЏ.
    :return: РЎР»РѕРІР°СЂСЊ СЃРѕ СЃС‚Р°РЅРґР°СЂС‚РЅС‹РјРё РѕС‚РєР»РѕРЅРµРЅРёСЏРјРё РґР»СЏ СѓРєР°Р·Р°РЅРЅС‹С… СЃС‚РѕР»Р±С†РѕРІ.
    """
    # Р Р°СЃС‡РµС‚ РїСЂРѕС†РµРЅС‚РЅРѕРіРѕ РёР·РјРµРЅРµРЅРёСЏ
    percentage_changes = df.iloc[:, 1:].pct_change(-1, fill_method=None)
    percentage_changes = percentage_changes[:-1]
    selected_df = percentage_changes[columns].iloc[0:num_rows]
    std_devs = selected_df.std(ddof=1)

    return std_devs.to_dict()  # Р’РѕР·РІСЂР°С‰Р°РµС‚ СЃР»РѕРІР°СЂСЊ


def extract_last_values(df, tickers):
    """
    РР·РІР»РµРєР°РµС‚ РїРѕСЃР»РµРґРЅРµРµ Р·РЅР°С‡РµРЅРёРµ РґР»СЏ РєР°Р¶РґРѕРіРѕ С‚РёРєРµСЂР° РёР· DataFrame.

    :param df: DataFrame, hist_prices_df
    :param tickers: РЎРїРёСЃРѕРє С‚РёРєРµСЂРѕРІ, РґР»СЏ РєРѕС‚РѕСЂС‹С… РЅСѓР¶РЅРѕ РёР·РІР»РµС‡СЊ Р·РЅР°С‡РµРЅРёСЏ.
    :return: РЎР»РѕРІР°СЂСЊ СЃ РїРѕСЃР»РµРґРЅРёРјРё Р·РЅР°С‡РµРЅРёСЏРјРё РґР»СЏ РєР°Р¶РґРѕРіРѕ С‚РёРєРµСЂР°.
    """
    extracted_last_values = {}
    for ticker in tickers:
        # РР·РІР»РµРєР°РµРј РїРѕСЃР»РµРґРЅРµРµ РЅРµРЅСѓР»РµРІРѕРµ Р·РЅР°С‡РµРЅРёРµ РёР· СЃС‚РѕР»Р±С†Р°, СЃРѕРѕС‚РІРµС‚СЃС‚РІСѓСЋС‰РµРіРѕ С‚РёРєРµСЂСѓ
        value = df[ticker].dropna().iloc[0] if not df[ticker].empty else None
        extracted_last_values[ticker] = value

    return extracted_last_values


def calculate_number_trading_dates(sim_size, df):
    # РџРѕРґСЃС‡РµС‚ РЅРµРїСѓСЃС‚С‹С… СЏС‡РµРµРє РІ Price_sim
    non_empty_cells_count = sim_size

    # РќР°С…РѕР¶РґРµРЅРёРµ РјР°РєСЃРёРјР°Р»СЊРЅРѕР№ РґР°С‚С‹ РёСЃС‚РµС‡РµРЅРёСЏ РЅР° Р»РёСЃС‚Рµ 'Registry'
    max_expiration_date = df['expiration_date'].max()

    # РџСЂРµРѕР±СЂР°Р·РѕРІР°РЅРёРµ РґР°С‚
    d4_date = datetime.strptime('28.12.2023', '%d.%m.%Y')

    # Р’С‹С‡РёСЃР»РµРЅРёРµ РґСЂРѕР±РЅРѕРіРѕ РєРѕР»РёС‡РµСЃС‚РІР° Р»РµС‚ РјРµР¶РґСѓ d4_date Рё max_expiration_date
    years_difference = relativedelta(max_expiration_date, d4_date).years
    months_difference = relativedelta(max_expiration_date, d4_date).months
    days_difference = relativedelta(max_expiration_date, d4_date).days

    # РџРѕРґСЃС‡РµС‚ РґРѕР»Рё РіРѕРґР°
    fractional_years = years_difference + months_difference / 12 + days_difference / 365

    # Р’С‹С‡РёСЃР»РµРЅРёРµ Рё РѕРєСЂСѓРіР»РµРЅРёРµ СЂРµР·СѓР»СЊС‚Р°С‚Р°
    result = round(non_empty_cells_count / fractional_years)
    # print("Р РµР·СѓР»СЊС‚Р°С‚:", result)
    return result


def get_dividends():
    df_div = pd.read_excel(file_path, sheet_name='Dividends', skiprows=44, usecols=range(1, 97))
    cols_names = df_div.columns.tolist()

    data_dict = {}

    for i in range(0, len(cols_names), 2):
        key = cols_names[i]
        if not pd.isna(key) and key.strip() != '':
            values = []
            for r in df_div.itertuples(index=False):
                if isinstance(r[i], pd.Timestamp):  # Р”РѕР±Р°РІР»СЏРµРј РґР°С‚Сѓ.
                    if pd.notna(r[i]):
                        if r[i].date() > sim_dates[
                            -1]:  # Р”Р°С‚Р° РґРёРІРёРґРµРЅРґР° Р±РѕР»СЊС€Рµ РґР°С‚ СЃРёРјСѓР»СЏС†РёР№.
                            break
                        values.append(get_valid_date(r[i].date(), is_sim=True))
                elif pd.notna(r[i]):
                    values.append(r[i])
            second_column_values = [val for j, val in enumerate(df_div[cols_names[i + 1]].tolist()) if
                                    pd.notna(val) and j < len(values)]
            data_dict[key] = [values, second_column_values]

    return data_dict


def get_rfr():
    # Р—Р°РіСЂСѓР·РєР° Рё РѕР±СЂР°Р±РѕС‚РєР° РґР°РЅРЅС‹С… РёР· Р»РёСЃС‚Р° 'RFR'
    df_rfr = pd.read_excel(file_path, sheet_name='RFR', skiprows=5, nrows=2, header=None)
    df_rfr.dropna(axis=1, how='all', inplace=True)
    return df_rfr


def calculate_growth_rate_year(df, option_index, df_transposed):
    current_option_expiration_date = df.loc[df['Option #'] == option_index, 'Expiration date'].iloc[0]

    d4_date = datetime.strptime('28.12.2023', '%d.%m.%Y')

    # Р’С‹С‡РёСЃР»РµРЅРёРµ РґСЂРѕР±РЅРѕРіРѕ РєРѕР»РёС‡РµСЃС‚РІР° Р»РµС‚ РјРµР¶РґСѓ d4_date Рё current_option_expiration_date
    years_difference = relativedelta(current_option_expiration_date, d4_date).years
    months_difference = relativedelta(current_option_expiration_date, d4_date).months
    days_difference = relativedelta(current_option_expiration_date, d4_date).days

    # РџРѕРґСЃС‡РµС‚ РґРѕР»Рё РіРѕРґР°
    fractional_years = years_difference + months_difference / 12 + days_difference / 365

    value_d10 = fractional_years

    df_final = df_transposed.reset_index(drop=True)
    # РџРѕРёСЃРє Р±Р»РёР¶Р°Р№С€РµРіРѕ Р·РЅР°С‡РµРЅРёСЏ РІ СЃС‚РѕР»Р±С†Рµ 'РЎСЂРѕРє РґРѕ РїРѕРіР°С€РµРЅРёСЏ, Р»РµС‚'
    df_final['Р Р°Р·РЅРёС†Р°'] = np.abs(df_final["РЎСЂРѕРє РґРѕ РїРѕРіР°С€РµРЅРёСЏ, Р»РµС‚"] - value_d10)
    closest_row = df_final[df_final['Р Р°Р·РЅРёС†Р°'] == df_final['Р Р°Р·РЅРёС†Р°'].min()]

    # РџРѕР»СѓС‡РµРЅРёРµ Р·РЅР°С‡РµРЅРёСЏ РёР· РІС‚РѕСЂРѕРіРѕ СЃС‚РѕР»Р±С†Р° СЃРѕРѕС‚РІРµС‚СЃС‚РІСѓСЋС‰РµР№ СЃС‚СЂРѕРєРё
    if not closest_row.empty:
        result_value = closest_row["Р”РѕС…РѕРґРЅРѕСЃС‚СЊ, % РіРѕРґРѕРІС‹С…"].iloc[0]
        # print("Р‘Р»РёР¶Р°Р№С€РµРµ Р·РЅР°С‡РµРЅРёРµ РІ 'Р”РѕС…РѕРґРЅРѕСЃС‚СЊ, % РіРѕРґРѕРІС‹С…':", result_value)
        return result_value
    else:
        # print("РЎРѕРѕС‚РІРµС‚СЃС‚РІСѓСЋС‰РµРµ Р·РЅР°С‡РµРЅРёРµ РЅРµ РЅР°Р№РґРµРЅРѕ.")
        return None


def get_cholesky(historical, uniq_actives):
    """
    Р Р°СЃС‡РµС‚ РјР°С‚СЂРёС† РєРѕСЂСЂРµР»СЏС†РёРё Рё СЂР°Р·Р»РѕР¶РµРЅРёР№ РҐРѕР»РµС†РєРѕРіРѕ.

    :param historical:
    :param uniq_actives:
    :return:
    """
    df_returns = load.get_return_per(historical)

    for tpl in uniq_actives:  # РџСЂРѕС…РѕРґ РїРѕ СѓРЅРёРєР°Р»СЊРЅС‹Рј РЅР°Р±РѕСЂР°Рј Р°РєС‚РёРІРѕРІ.
        data = {}  # РџСЂРѕС†РµРЅС‚РЅС‹Рµ СЂР°Р·РЅРѕСЃС‚Рё С‚РµРєСѓС‰РµРіРѕ РЅР°Р±РѕСЂР°.
        for active in tpl:  # РџСЂРѕС…РѕРґ РїРѕ Р°РєС‚РёРІР°Рј С‚РµРєСѓС‰РµРіРѕ РЅР°Р±РѕСЂР°.
            data[active] = df_returns[active].values
        df_corr = pd.DataFrame(data).corr()
        chol_matr[tpl] = np.linalg.cholesky(df_corr)


def get_sim_size(val_date: date, sim_per: date):  # Р•РґРёРЅРѕР¶РґС‹.
    # РР·РІР»РµС‡РµРЅРёРµ РїСЂР°Р·РґРЅРёС‡РЅС‹С… РґРЅРµР№.
    holidays = pd.read_excel(file_path, sheet_name='Holidays', header=1)
    holidays = [datetime.strptime(str(x), '%Y-%m-%d %H:%M:%S').date() for x in holidays['Holidays']]
    last_date = val_date

    while last_date <= sim_per:
        if last_date.weekday() == 4:  # РџСЏС‚РЅРёС†Р°, РѕС‚СЃРµРєР°РµРј РІС‹С…РѕРґРЅС‹Рµ.
            current = last_date + timedelta(days=3)
        else:
            current = last_date + timedelta(days=1)  # Р”РѕР±Р°РІР»РµРЅРёРµ СЃР»РµРґСѓСЋС‰РµРіРѕ РґРЅСЏ.
        # РџСЂРѕРІРµСЂРєР° РЅР° РїСЂР°Р·РґРЅРёРєРё Рё РІС‹С…РѕРґРЅС‹Рµ.
        while current in holidays or current.weekday() == 5 or current.weekday() == 6:
            current = current + timedelta(days=1)
        sim_dates.append(current)
        last_date = sim_dates[-1]

    return len(sim_dates)


# if __name__ == '__main__':
#     # РџРѕРґС‚СЏРіРёРІР°РµРј source С‚Р°Р±Р»РёС‡РєРё.
#     hist_prices_df = pd.read_excel(file_path, sheet_name='Hist_prices')
#     hist_prices_df['Date'] = [datetime.strptime(str(x), '%Y-%m-%d %H:%M:%S').date() for x in hist_prices_df['Date']]
#
#     df_registry = pd.read_excel(file_path, sheet_name='Registry')
#     df_options = load.get_options_data(df_registry)
#     df_returns = load.get_return_per(hist_prices_df)
#
#     df_rfr = get_rfr()
#     df_transposed = df_rfr.transpose()
#     df_transposed.columns = ["РЎСЂРѕРє РґРѕ РїРѕРіР°С€РµРЅРёСЏ, Р»РµС‚", "Р”РѕС…РѕРґРЅРѕСЃС‚СЊ, % РіРѕРґРѕРІС‹С…"]
#
#     hist_prices_df_deser = ser_deser(hist_prices_df, 'hist_prices')
#
#     ser(df_registry, 'registry')
#     ser(df_options, 'options')
#     ser(df_returns, 'returns')
#     ser(df_transposed, 'rfr')
#     # TODO СЃРµСЂРёР°Р»РёР·РѕРІР°С‚СЊ РІСЃРµ С„СЂРµР№РјС‹ РІ json

if __name__ == '__main__':
    # TODO РїРѕР»СѓС‡РёС‚СЊ РІСЃРµ С„СЂРµР№РјС‹ РёР· json
    sim_amount = []

    # РџРѕРґС‚СЏРіРёРІР°РµРј source С‚Р°Р±Р»РёС‡РєРё.
    hist_prices_df = pd.read_excel(file_path, sheet_name='Hist_prices')
    hist_prices_df['Date'] = [datetime.strptime(str(x), '%Y-%m-%d %H:%M:%S').date() for x in hist_prices_df['Date']]

    df_registry = pd.read_excel(file_path, sheet_name='Registry')
    df_options = load.get_options_data(df_registry)
    df_returns = load.get_return_per(hist_prices_df)

    df_rfr = get_rfr()
    df_transposed = df_rfr.transpose()
    df_transposed.columns = ["РЎСЂРѕРє РґРѕ РїРѕРіР°С€РµРЅРёСЏ, Р»РµС‚", "Р”РѕС…РѕРґРЅРѕСЃС‚СЊ, % РіРѕРґРѕРІС‹С…"]

    # ("hist_prices_df:")
    # print(hist_prices_df.head())

    # print("\ndf_registry:")
    # print(df_registry.head())

    # print("\ndf_options:")
    # print(df_options.head())

    # print("\ndf_returns:")
    # print(df_returns.head())

    valuation_date = date(2023, 12, 28)  # Р”Р°С‚Р° РѕС†РµРЅРєРё.
    # РњР°РєСЃРёРјР°Р»СЊРЅР°СЏ РґР°С‚Р° СЌРєСЃРїРёСЂР°С†РёРё.
    sim_period = datetime.strptime(str(df_registry['expiration_date'].max()), '%Y-%m-%d %H:%M:%S').date()
    # print(sim_period)
    # print(sim_dates)
    # РњР°РєСЃРёРјР°Р»СЊРЅРѕРµ РєРѕР»-РІРѕ РґРЅРµР№ СЃРёРјСѓР»СЏС†РёР№.
    sim_size = get_sim_size(valuation_date, sim_period)
    dividends = get_dividends()

    column_names = hist_prices_df.columns[1:]

    # РџРѕР»СѓС‡РµРЅРёРµ РїРѕСЃР»РµРґРЅРёС… С†РµРЅ
    last_price = extract_last_values(hist_prices_df, column_names)
    # print(last_price)

    get_cholesky(hist_prices_df, df_options[
        'Tickers'].unique())  # Р’С‹С‡РёСЃР»СЏРµС‚СЃСЏ РµРґРёРЅРѕР¶РґС‹ СЂР°Р·Р»РѕР¶РµРЅРёСЏ РҐРѕР»РµС†РєРѕРіРѕ.

    start_time = time.time()  # РќР° Р±СѓРґСѓС‰РµРµ РґР»СЏ Р·Р°РјРµСЂРѕРІ.

    results_for_sim = []

    cols = [c for c in df_options.columns if c != 'S_inv' and c != 'Option #']
    uniq = df_options.drop_duplicates(subset=cols)['Option #'].values

    with open('/home/Shared/NIFI/NIFI_DATA/Auto/Pack1/results.txt', 'w') as file:
        for sim in range(1000):  # Р’РЅРµС€РЅРёР№ С†РёРєР» РїРѕ РѕРїС†РёРѕРЅР°Рј.
            start_time_option = time.time()  # РќР° Р±СѓРґСѓС‰РµРµ РґР»СЏ Р·Р°РјРµСЂРѕРІ.
            results = []
            coeff = 1.0
            # Uncorrelated Z-stats - РѕР±С‰Р°СЏ РґР»СЏ РІСЃРµС… РѕРїС†РёРѕРЅРѕРІ С‡Р°СЃС‚СЊ.
            uncorr_stat = np.array([[norm.ppf(random.random()) for j in range(5)] for i in range(sim_size)])
            for index, row in df_options.iloc[9861:19720].iterrows():  # РџСЂРѕС…РѕРґ РїРѕ РѕРїС†РёРѕРЅР°Рј.
                # for index, row in df_options.iterrows():
                # print(index)
                start_time = time.time()  # РќР° Р±СѓРґСѓС‰РµРµ РґР»СЏ Р·Р°РјРµСЂРѕРІ.
                if index in uniq:  # РЈРЅРёРєР°Р»СЊРЅС‹Р№ РѕРїС†РёРѕРЅ, РґР»СЏ РЅРµРіРѕ РґРµР»Р°РµРј РїРѕР»РЅС‹Р№ С†РёРєР».
                    # print('РЈРќРРљРђР›Р¬РќР«Р™')
                    contract_date = row[
                        'Contract date']  # Р”Р°С‚Р° РєРѕРЅС‚СЂР°РєС‚Р° (РґР»СЏ РѕРїСЂРµРґРµР»РµРЅРёСЏ trading date).
                    # print("Р”Р°С‚Р° РєРѕРЅС‚СЂР°РєС‚Р°:", contract_date)

                    expiration_date = row['Expiration date']  # Р”Р°С‚Р° СЌРєСЃРїРёСЂР°С†РёРё.
                    # print("Р”Р°С‚Р° СЌРєСЃРїРёСЂР°С†РёРё:", expiration_date)

                    barrier = row['Barrier rate']
                    # print("Barrier rate:", barrier)

                    coupon = row['Coupon']
                    # print("Coupon:", coupon)

                    ex_barrier = row['Exercise barrier']
                    # print(ex_barrier)

                    s_inv = row['S_inv']
                    # print("S_inv:", s_inv)

                    # Р Р°СЃС‡РµС‚ Trading days to expiration
                    trading_days_expiration = trading_days_to_expiration(sim_dates, expiration_date)
                    # print('trading_days_to_expiration', trading_days_expiration)

                    growth_rate_year = calculate_growth_rate_year(df_options, index, df_transposed)
                    # print('growth_rate_year', growth_rate_year)

                    number_trading_dates = calculate_number_trading_dates(sim_size, df_registry)

                    growth_rate = (1 + growth_rate_year) ** (1 / number_trading_dates) - 1
                    # print('growth_rate', growth_rate)

                    volatility = calculate_std_dev_from_percentage_change(hist_prices_df, list(row['Tickers']),
                                                                          trading_days_expiration)
                    # print('volatility')
                    # print(volatility)

                    # print('РћРїС†РёРѕРЅ ', index)
                    base_price = get_base_price(row['Tickers'], contract_date)

                    trading_dates = [contract_date]
                    for dt in row['Coupons']:
                        if dt <= valuation_date:
                            trading_dates.append(get_valid_date(dt))
                        else:
                            trading_dates.append(get_valid_date(dt, True))
                    # print('Trading dates')
                    # print(trading_dates)
                    dates = [dt for dt in sim_dates if
                             dt <= expiration_date]  # РЎРїРёСЃРѕРє РґР°С‚, РґР»СЏ РєРѕС‚РѕСЂС‹С… РЅСѓР¶РЅР° СЃРёРјСѓР»СЏС†РёСЏ.

                    # Correlated Z-stats СЃ СѓС‡РµС‚РѕРј РѕС‚СЂРµР·Р°РЅРёСЏ РЅРµРЅСѓР¶РЅС‹С… РґР°С‚ Рё Р°РєС‚РёРІРѕРІ.
                    corr_stat = np.matmul(chol_matr[row["Tickers"]],
                                          uncorr_stat[0:len(dates), 0:row['Assets']].transpose()).transpose()
                    sim_prices = {}  # РЎРёРјСѓР»СЏС†РёРё С†РµРЅ РґР»СЏ С‚РµРєСѓС‰РµРіРѕ РѕРїС†РёРѕРЅР°.
                    min_ticker = 10000.0
                    # РџСЂРѕС…РѕРґ РїРѕ С‚РёРєРµСЂР°Рј РґР»СЏ СЃРёРјСѓР»СЏС†РёРё Рё РѕРїСЂРµРґРµР»РµРЅРёСЏ СЃС‚Р°РІРѕРє РЅР° РґР°С‚Сѓ СЌРєСЃРїРёСЂР°С†РёРё.
                    for idx, ticker in enumerate(row['Tickers']):
                        sim_prices[ticker] = []
                        # РЎРёРјСѓР»СЏС†РёСЏ РґР»СЏ РєР°Р¶РґРѕРіРѕ РґРЅСЏ.
                        for j, day in enumerate(dates):
                            # Р Р°СЃС‡РµС‚ С†РµРЅС‹ РїРѕ С„РѕСЂРјСѓР»Рµ.
                            if j == 0:
                                last_price_val = last_price[ticker]
                            divs = 0.0
                            if day in dividends[ticker][0]:
                                divs = dividends[ticker][1][dividends[ticker][0].index(day)]
                            current_sum = max(last_price_val * math.exp(
                                (growth_rate - volatility[ticker] ** 2 / 2) + corr_stat[j][idx] * volatility[
                                    ticker]) - divs, 0)
                            sim_prices[ticker].append(current_sum)
                            last_price_val = current_sum

                    sim_prices['Date'] = sim_dates[0:len(dates)]
                    sim_prices = pd.DataFrame(sim_prices)

                    cf_values = []
                    dcf_values = []
                    # РџСЂРѕС…РѕРґ РїРѕ С‚СЂРµР№РґРёРЅРі РґР°С‚Р°Рј.
                    for idx, dt in enumerate(trading_dates):
                        if idx != 0:  # РџРµСЂРІСѓСЋ РґР°С‚Сѓ РЅРµ СѓС‡РёС‚С‹РІР°РµРј.
                            min_per = 10000.0
                            if idx == 1:  # РРЅРёС†РёР°Р»РёР·Р°С†РёСЏ cum_coap
                                cum_coap = 1
                                previous_coap = 1
                            else:
                                cum_coap = previous_coap + 1 if cf_values[idx - 2] == 0 else 1
                            previous_coap = cum_coap

                            # РџСЂРѕС…РѕРґ РїРѕ Р°РєС‚РёРІР°Рј.
                            for p, ticker in enumerate(row['Tickers']):
                                base = base_price[p]

                                if valuation_date >= dt:  # Р•СЃС‚СЊ РёСЃС‚РѕСЂРёС‡РµСЃРєРёРµ РґР°РЅРЅС‹Рµ.
                                    current = hist_prices_df.loc[hist_prices_df['Date'] == dt][ticker].values[0] / base
                                else:  # РСЃРїРѕР»СЊР·СѓСЋС‚СЃСЏ РґР°РЅРЅС‹Рµ СЃРёРјСѓР»СЏС†РёРё.
                                    current = sim_prices.loc[sim_prices['Date'] == dt][ticker].values[0] / base
                                min_per = min_per if min_per < current else current  # РћР±РЅРѕРІР»РµРЅРёРµ РјРёРЅРёРјР°Р»СЊРЅРѕР№ СЃС‚Р°РІРєРё.
                                if idx == len(
                                        trading_dates) - 1:  # РЎРјРѕС‚СЂРёРј РјРёРЅРёРјР°Р»СЊРЅСѓСЋ СЃС‚Р°РІРєСѓ Р°РєС‚РёРІР° РЅР° РґР°С‚Сѓ СЌРєСЃРїРёСЂР°С†РёРё.
                                    min_ticker = min_ticker if current >= min_ticker else current

                            cf_value = 0 if min_per < barrier else date_factor(trading_dates[idx - cum_coap],
                                                                               dt) * coupon * s_inv

                            cf_values.append(cf_value)
                            dcf_value = (cf_value / (1 + growth_rate_year) ** date_factor(valuation_date,
                                                                                          dt)) if dt > valuation_date else 0

                            dcf_values.append(dcf_value)

                    part_sum = s_inv * min_ticker if min_ticker < ex_barrier else s_inv

                    final_amount = sum(dcf_values) + part_sum / (growth_rate_year + 1) ** date_factor(valuation_date,
                                                                                                      expiration_date)

                    coeff = final_amount / s_inv  # РћР±РЅРѕРІР»СЏРµРј Р·РЅР°С‡РµРЅРёРµ РєРѕСЌС„С„РёС†РёРµРЅС‚Р°.
                    # print(coeff)
                else:
                    # print("Р”РЈР‘Р›Р¬")
                    # print(coeff)
                    # print(row['S_inv'])
                    final_amount = row['S_inv'] * coeff  # РСЃРїРѕР»СЊР·СѓРµРј РєСЂР°Р№РЅРµРµ Р·РЅР°С‡РµРЅРёРµ
                    # print(final_amount)
                    # print()

                results.append(round(final_amount, 2))
                file.write(f"{sim} - {index} - {round(final_amount, 2)}\n")

            end_time_option = time.time()
            results_for_sim.append(results)
            elapsed_time_option = end_time_option - start_time_option
            # print(elapsed_time_option)

    num_options = 9859
    num_simulations = 1000

    sums_for_options = [0] * num_options

    for simulation_results in results_for_sim:
        for option_index in range(num_options):
            sums_for_options[option_index] += simulation_results[option_index]

    print(1)

    average_for_options = [sums / num_simulations for sums in sums_for_options]

    # РЎРѕС…СЂР°РЅРµРЅРёРµ СЃСЂРµРґРЅРёС… Р·РЅР°С‡РµРЅРёР№ РІ С„Р°Р№Р»
    start = 9862
    with open('average_results_p1.txt', 'w') as file:
        for average in average_for_options:
            file.write(f"{start}: {average}\n")
            start = start + 1
