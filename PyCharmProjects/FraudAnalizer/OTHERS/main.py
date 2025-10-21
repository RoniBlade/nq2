import pandas as pd
import numpy as np
from sklearn.impute import KNNImputer
from sklearn.preprocessing import RobustScaler, StandardScaler
from sklearn.decomposition import PCA
from sklearn.cluster import KMeans
from sklearn.metrics import silhouette_score
import seaborn as sns
import matplotlib.pyplot as plt
import plotly.graph_objects as go

# Загрузка данных
train_transactions = pd.read_csv("Transaction dataset train set.csv")
test_transactions = pd.read_csv("Transaction dataset test set.csv")
clickstream = pd.read_csv("Clickstream dataset.csv")

# Объединение данных по SESSION_ID
train_combined = train_transactions.merge(clickstream, on="SESSION_ID", how="left")
test_combined = test_transactions.merge(clickstream, on="SESSION_ID", how="left")
train_combined['Dataset'] = 'Train'
test_combined['Dataset'] = 'Test'
df = pd.concat([train_combined, test_combined], ignore_index=True)

# Добавление нового признака TIME_SPENT (если он отсутствует, замените на свою логику)
if 'TIME_SPENT' not in df.columns:
    df['TIME_SPENT'] = np.random.randint(1, 100, size=len(df))  # Пример генерации

# Подсчет пропущенных значений
missing_values = df.isna().sum()
missing_percentage = (missing_values / len(df)) * 100
missing_summary = pd.DataFrame({'Column': missing_values.index, 'Missing_Count': missing_values.values,
                                'Missing_Percentage': missing_percentage})
print(missing_summary.sort_values(by='Missing_Percentage', ascending=False))

# Удаление столбцов с пропущенными значениями > 50%
threshold = 50
columns_to_drop = missing_summary[missing_summary['Missing_Percentage'] > threshold]['Column']
df.drop(columns=columns_to_drop, inplace=True)

# Заполнение пропусков: категориальные - "Generic PC", числовые - KNN
for col in df.select_dtypes(include=['object']):
    df[col].fillna("Generic PC", inplace=True)

imputer = KNNImputer(n_neighbors=5)
numeric_cols = df.select_dtypes(include=['float64', 'int64']).columns
df[numeric_cols] = imputer.fit_transform(df[numeric_cols])

# Рассчет метрик за временные периоды
time_periods = ['week', 'month']  # Пример
agg_metrics = ['mean', 'max', 'std']
for period in time_periods:
    for metric in agg_metrics:
        period_col = f'{metric}_{period}'
        df[period_col] = df.groupby('SESSION_ID')['AMOUNT'].transform(metric)

# Гистограммы распределения признаков
for col in numeric_cols:
    sns.histplot(df[col], kde=True)
    plt.title(f'Распределение {col}')
    plt.show()

# Робастная стандартизация
scaler = RobustScaler()
df_scaled = scaler.fit_transform(df[numeric_cols])

# PCA для визуализации
pca = PCA(n_components=2)
pca_components = pca.fit_transform(df_scaled)

plt.scatter(pca_components[:, 0], pca_components[:, 1], alpha=0.5)
plt.title('PCA визуализация данных')
plt.xlabel('Главная компонента 1')
plt.ylabel('Главная компонента 2')
plt.grid(True)
plt.show()

# Исключение выбросов (пример с использованием Z-score)
from scipy.stats import zscore
z_scores = np.abs(zscore(df[numeric_cols]))
df_no_outliers = df[(z_scores < 3).all(axis=1)]

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
print(f'Оптимальное число кластеров: {optimal_n_clusters}')

# Таблицы средних значений и визуализация кластеров
kmeans = KMeans(n_clusters=optimal_n_clusters, random_state=42)
df['Cluster'] = kmeans.fit_predict(df_scaled)

cluster_means = df.groupby('Cluster')[numeric_cols].mean()
fig = go.Figure(data=[go.Table(
    header=dict(values=["Кластер"] + list(cluster_means.columns)),
    cells=dict(values=[cluster_means.index] + [cluster_means[col] for col in cluster_means.columns])
)])
fig.update_layout(title='Средние значения по кластерам')
fig.show()

# Визуализация PCA с кластерами
plt.scatter(pca_components[:, 0], pca_components[:, 1], c=df['Cluster'], cmap='tab10', alpha=0.6)
plt.title('PCA с кластерами')
plt.xlabel('Главная компонента 1')
plt.ylabel('Главная компонента 2')
plt.colorbar(label='Кластеры')
plt.grid(True)
plt.show()

# Распределение категориальных признаков
categorical_cols = df.select_dtypes(include=['object']).columns
for col in categorical_cols:
    cluster_distribution = df.groupby(['Cluster', col]).size().unstack(fill_value=0)
    print(cluster_distribution)
