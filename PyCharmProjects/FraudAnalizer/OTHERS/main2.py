import pandas as pd
import numpy as np
from sklearn.preprocessing import RobustScaler
from sklearn.decomposition import PCA
from sklearn.cluster import KMeans
from sklearn.metrics import silhouette_score
import seaborn as sns
import matplotlib.pyplot as plt
import plotly.graph_objects as go
import logging

# Настройка логирования
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

logging.info("Загрузка данных...")

# Загрузка данных
train_transactions = pd.read_csv("Transaction dataset train set.csv")
test_transactions = pd.read_csv("Transaction dataset test set.csv")
clickstream = pd.read_csv("Clickstream dataset.csv")

logging.info("Данные загружены. Объединение по SESSION_ID...")

# Объединение данных
train_combined = train_transactions.merge(clickstream, on="SESSION_ID", how="left")
test_combined = test_transactions.merge(clickstream, on="SESSION_ID", how="left")
train_combined['Dataset'] = 'Train'
test_combined['Dataset'] = 'Test'
df = pd.concat([train_combined, test_combined], ignore_index=True)

# Добавление переменной TIME_SPENT
if 'TIME_SPENT' not in df.columns:
    df['TIME_SPENT'] = np.random.randint(1, 100, size=len(df))

logging.info("Предобработка данных...")

# Удаление незначимых признаков
columns_to_drop = ['BROWSER_VERSION', 'DEVICE_MODEL', 'DEVICE_FAMILY', 'SESSION_ID', 'PAGE_NAME']
df.drop(columns=columns_to_drop, inplace=True, errors='ignore')

# Преобразование EVENT_DATETIME в YEAR, MONTH, DAY, HOUR
if 'EVENT_DATETIME' in df.columns:
    df['YEAR'] = pd.to_datetime(df['EVENT_DATETIME']).dt.year
    df['MONTH'] = pd.to_datetime(df['EVENT_DATETIME']).dt.month
    df['DAY'] = pd.to_datetime(df['EVENT_DATETIME']).dt.day
    df['HOUR'] = pd.to_datetime(df['EVENT_DATETIME']).dt.hour
    df.drop(columns=['EVENT_DATETIME'], inplace=True)

# Кодирование категориальных признаков частотой
categorical_cols = ['BROWSER_FAMILY', 'OS_FAMILY', 'DEVICE_BRAND', 'CITY']
for col in categorical_cols:
    if col in df.columns:
        freq = df[col].value_counts(normalize=True)
        df[f"{col}_FREQUENCY"] = df[col].map(freq)
        df.drop(columns=[col], inplace=True)

# Агрегирование числовых признаков
agg_cols = ['AVAIL_CRDT', 'AMOUNT', 'CREDIT_LIMIT']
agg_features = df.groupby('Dataset')[agg_cols].agg(['mean', 'max', 'std', 'sum', 'count'])

# Преобразование multi-level columns в flat columns
agg_features.columns = ['_'.join(col).strip() for col in agg_features.columns.values]
agg_features.reset_index(inplace=True)

# Объединение агрегированных данных
df = df.merge(agg_features, on='Dataset', how='left')

# Удаление пропусков
df.dropna(inplace=True)

logging.info("Применение робастной стандартизации...")

# Робастная стандартизация
numeric_cols = df.select_dtypes(include=['float64', 'int64']).columns
scaler = RobustScaler()
df_scaled = scaler.fit_transform(df[numeric_cols])

logging.info("Выполнение PCA для визуализации...")

# PCA для визуализации
pca = PCA(n_components=2)
pca_components = pca.fit_transform(df_scaled)
df['PCA1'] = pca_components[:, 0]
df['PCA2'] = pca_components[:, 1]

# Исключение выбросов по PCA
df = df[(df['PCA2'] < 100000)]

plt.scatter(df['PCA1'], df['PCA2'], alpha=0.5)
plt.title('PCA визуализация данных')
plt.xlabel('Главная компонента 1')
plt.ylabel('Главная компонента 2')
plt.grid(True)
plt.show()

logging.info("Выполнение кластеризации...")

# Кластеризация
silhouette_scores = []
range_n_clusters = [4, 5, 7, 9]
for n_clusters in range_n_clusters:
    kmeans = KMeans(n_clusters=n_clusters, random_state=42)
    labels = kmeans.fit_predict(df_scaled)
    score = silhouette_score(df_scaled, labels)
    silhouette_scores.append(score)

plt.plot(range_n_clusters, silhouette_scores, marker='o')
plt.title('Silhouette Score для разных кластеров')
plt.xlabel('Число кластеров')
plt.ylabel('Silhouette Score')
plt.grid(True)
plt.show()

optimal_n_clusters = range_n_clusters[np.argmax(silhouette_scores)]
logging.info(f"Оптимальное число кластеров: {optimal_n_clusters}")

# Итоговая кластеризация
kmeans = KMeans(n_clusters=optimal_n_clusters, random_state=42)
df['Cluster'] = kmeans.fit_predict(df_scaled)

logging.info("Визуализация средних значений по кластерам...")
cluster_means = df.groupby('Cluster')[numeric_cols].mean()
fig = go.Figure(data=[go.Table(
    header=dict(values=["Кластер"] + list(cluster_means.columns)),
    cells=dict(values=[cluster_means.index] + [cluster_means[col] for col in cluster_means.columns])
)])
fig.update_layout(title='Средние значения по кластерам')
fig.show()

logging.info("Построение PCA с кластерами...")
plt.scatter(df['PCA1'], df['PCA2'], c=df['Cluster'], cmap='tab10', alpha=0.6)
plt.title('PCA с кластерами')
plt.xlabel('Главная компонента 1')
plt.ylabel('Главная компонента 2')
plt.colorbar(label='Кластеры')
plt.grid(True)
plt.show()
