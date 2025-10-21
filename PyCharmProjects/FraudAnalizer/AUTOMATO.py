    import pandas as pd
    import numpy as np
    import matplotlib.pyplot as plt
    from sklearn.model_selection import train_test_split
    from sklearn.metrics import mean_squared_error
    from sklearn.neighbors import LocalOutlierFactor
    from sklearn.cluster import DBSCAN
    from sklearn.neural_network import MLPRegressor
    from xgboost import XGBRegressor
    from keras.models import Sequential
    from keras.layers import LSTM, GRU, Dense
    from sklearn.preprocessing import StandardScaler


    # Функция для предобработки данных
    def preprocess_data(file_path):
        df = pd.read_excel(file_path)
        df['Date'] = pd.to_datetime(df['Date'], format='%d.%m.%Y')  # Преобразуем дату в формат datetime
        df['Date'] = df['Date'].dt.strftime('%Y-%m-%d')  # Форматируем дату как строку (для графиков)

        # Преобразуем данные (можно сделать любые дополнительные шаги для подготовки данных)
        X = df['Date'].values.reshape(-1, 1)  # Используем дату как единственный признак для примера
        y = df['Value'].values  # Целевая переменная

        # Масштабирование данных
        scaler = StandardScaler()
        X = scaler.fit_transform(X)

        return df, X, y


    # Функция для создания и обучения XGBoost модели
    def train_xgboost(X_train, y_train, X_test, y_test):
        model = XGBRegressor()
        model.fit(X_train, y_train)
        y_pred = model.predict(X_test)
        mse = mean_squared_error(y_test, y_pred)
        print(f"Mean Squared Error (XGBoost): {mse}")
        return model


    # Функция для создания и обучения MLP модели
    def train_mlp(X_train, y_train, X_test, y_test):
        model = MLPRegressor(max_iter=1000)
        model.fit(X_train, y_train)
        y_pred = model.predict(X_test)
        mse = mean_squared_error(y_test, y_pred)
        print(f"Mean Squared Error (MLP): {mse}")
        return model


    # Функция для создания и обучения RNN модели
    def create_rnn_model(input_shape):
        model = Sequential()
        model.add(LSTM(50, return_sequences=False, input_shape=input_shape))
        model.add(Dense(1))
        model.compile(optimizer='adam', loss='mean_squared_error')
        return model


    # Функция для создания и обучения GRU модели
    def create_gru_model(input_shape):
        model = Sequential()
        model.add(GRU(50, return_sequences=False, input_shape=input_shape))
        model.add(Dense(1))
        model.compile(optimizer='adam', loss='mean_squared_error')
        return model


    # Функция для обнаружения выбросов с использованием DBSCAN
    def detect_outliers_dbscan(X):
        dbscan = DBSCAN(eps=0.5, min_samples=5)
        outliers = dbscan.fit_predict(X)
        return outliers


    # Функция для обнаружения выбросов с использованием LOF
    def detect_outliers_lof(X):
        lof = LocalOutlierFactor(n_neighbors=20)
        outliers = lof.fit_predict(X)
        return outliers


    # Функция для построения графиков прогноза
    def plot_results(df, y_test, y_pred, title):
        plt.figure(figsize=(10, 6))
        plt.plot(df['Date'], y_test, label='True Values', color='blue')
        plt.plot(df['Date'][-len(y_pred):], y_pred, label='Predictions', color='red')
        plt.title(title)
        plt.xlabel('Date')
        plt.ylabel('Value')
        plt.xticks(rotation=45)
        plt.legend()
        plt.show()


    # Функция для построения графиков выбросов
    def plot_outliers(df, outliers, title):
        plt.figure(figsize=(10, 6))
        plt.plot(df['Date'], df['Value'], label='Data', color='blue')
        plt.scatter(df['Date'][outliers == -1], df['Value'][outliers == -1], color='red', label='Outliers')
        plt.title(title)
        plt.xlabel('Date')
        plt.ylabel('Value')
        plt.xticks(rotation=45)
        plt.legend()
        plt.show()


    # Функция для вывода метрик модели
    def print_metrics(y_test, y_pred):
        mse = mean_squared_error(y_test, y_pred)
        print(f"Mean Squared Error: {mse}")


    # Основной блок выполнения
    file_path = 'dataset.xlsx'  # Путь к вашему файлу
    df, X, y = preprocess_data(file_path)

    # Разделение данных на обучающую и тестовую выборки
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    # 1. Прогнозирование с использованием XGBoost
    print("Запуск XGBoost...")
    model_xgb = train_xgboost(X_train, y_train, X_test, y_test)
    y_pred_xgb = model_xgb.predict(X_test)

    # 2. Прогнозирование с использованием MLP
    print("Запуск MLP...")
    model_mlp = train_mlp(X_train, y_train, X_test, y_test)
    y_pred_mlp = model_mlp.predict(X_test)

    # 3. Прогнозирование с использованием RNN
    print("Запуск RNN...")
    X_rnn = X_train.reshape((X_train.shape[0], 1, X_train.shape[1]))  # Преобразуем в формат (samples, timesteps, features)
    rnn_model = create_rnn_model((X_rnn.shape[1], X_rnn.shape[2]))
    rnn_model.fit(X_rnn, y_train, epochs=10, batch_size=32)
    y_pred_rnn = rnn_model.predict(X_rnn)

    # 4. Прогнозирование с использованием GRU
    print("Запуск GRU...")
    gru_model = create_gru_model((X_rnn.shape[1], X_rnn.shape[2]))
    gru_model.fit(X_rnn, y_train, epochs=10, batch_size=32)
    y_pred_gru = gru_model.predict(X_rnn)

    # 5. Обнаружение выбросов с использованием DBSCAN
    print("Обнаружение выбросов с DBSCAN...")
    dbscan_outliers = detect_outliers_dbscan(X)

    # 6. Обнаружение выбросов с использованием LOF
    print("Обнаружение выбросов с LOF...")
    lof_outliers = detect_outliers_lof(X)

    # 7. Графики для XGBoost
    print("График для XGBoost...")
    plot_results(df, y_test, y_pred_xgb, title="XGBoost - Prediction vs True Values")

    # 8. Графики для MLP
    print("График для MLP...")
    plot_results(df, y_test, y_pred_mlp, title="MLP - Prediction vs True Values")

    # 9. Графики для RNN
    print("График для RNN...")
    plot_results(df, y_test, y_pred_rnn, title="RNN - Prediction vs True Values")

    # 10. Графики для GRU
    print("График для GRU...")
    plot_results(df, y_test, y_pred_gru, title="GRU - Prediction vs True Values")

    # 11. Графики выбросов с DBSCAN
    print("График для DBSCAN...")
    plot_outliers(df, dbscan_outliers, title="DBSCAN - Outlier Detection")

    # 12. Графики выбросов с LOF
    print("График для LOF...")
    plot_outliers(df, lof_outliers, title="LOF - Outlier Detection")

    # Вывод метрик для оценки прогнозов
    print("Метрики для XGBoost:")
    print_metrics(y_test, y_pred_xgb)

    print("Метрики для MLP:")
    print_metrics(y_test, y_pred_mlp)

    print("Метрики для RNN:")
    print_metrics(y_test, y_pred_rnn)

    print("Метрики для GRU:")
    print_metrics(y_test, y_pred_gru)
