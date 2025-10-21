import random
import numpy as np
import warnings
from sklearn.neural_network import MLPClassifier
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
from sklearn.metrics import f1_score
from sklearn.preprocessing import StandardScaler
from genetic_viz.visualizer import GeneticVisualizer  # Импортируем визуализатор

# Подавление предупреждений
warnings.filterwarnings("ignore", category=UserWarning)

# Загрузка и нормализация данных
data = load_iris()
X, y = data.data, data.target
scaler = StandardScaler()
X = scaler.fit_transform(X)
X_train, X_test, y_train, y_test = train_test_split(X, y, stratify=y, test_size=0.3, random_state=42, shuffle=True)


def generate_initial_population(size):
    """Генерация начальной популяции."""
    return [
        ([random.randint(1, 2) for _ in range(random.randint(2, 2))],
         [random.choice(['adam', 'sgd', 'lbfgs']), random.choice(['relu', 'logistic', 'tanh'])])
        for _ in range(size)
    ]


def fitness(individual):
    """Вычисляет значение F1-метрики для особи."""
    neurons, params = individual
    mlp = MLPClassifier(hidden_layer_sizes=neurons, solver=params[0], activation=params[1], max_iter=300, alpha=0.01,
                        random_state=42)
    try:
        mlp.fit(X_train, y_train)
        y_pred = mlp.predict(X_test)
        return f1_score(y_test, y_pred, average='weighted')
    except Exception as e:
        print(f"Ошибка при оценке приспособленности: {e}")
        return 0.0


def crossover(parent1, parent2):
    """Кроссовер с двумя потомками."""
    neurons1, params1 = parent1
    neurons2, params2 = parent2
    point = random.randint(1, min(len(neurons1), len(neurons2)))

    child1 = (neurons1[:point] + neurons2[point:],
              [random.choice((params1[0], params2[0])), random.choice((params1[1], params2[1]))])
    child2 = (neurons2[:point] + neurons1[point:],
              [random.choice((params1[0], params2[0])), random.choice((params1[1], params2[1]))])

    return child1, child2


def mutate(individual, mutation_rate=0.2):
    """Мутация особи. Возвращает пару (до и после) только если мутация произошла."""
    neurons, params = individual

    if random.random() < mutation_rate:
        new_neurons = [random.randint(1, 2) for _ in range(len(neurons))]
        new_params = [random.choice(['adam', 'sgd']), random.choice(['relu', 'logistic'])]

        if new_neurons != neurons or new_params != params:
            return (individual, (new_neurons, new_params))  # Возвращаем до и после

    return None  # Если мутация не произошла


def apply_elitism(population, fitness_scores, elite_fraction=0.1):
    """Применение элитизма."""
    elite_size = max(1, int(len(population) * elite_fraction))
    elite_indices = np.argsort(fitness_scores)[-elite_size:]
    return [population[i] for i in elite_indices]


def tournament_selection(population, fitness_scores, k=3):
    """Турнирный отбор."""
    selected = random.sample(list(zip(population, fitness_scores)), k)
    return max(selected, key=lambda x: x[1])[0]


def select_best_solution(population, fitness_scores):
    """Выбор лучшего решения."""
    max_fitness = max(fitness_scores)
    best_index = fitness_scores.index(max_fitness)
    return population[best_index], max_fitness


# Инициализация визуализатора с количеством островов
NUM_ISLANDS = 3
viz = GeneticVisualizer(fitness, num_islands=NUM_ISLANDS)


def genetic_algorithm(population_size=10, generations=10, elite_fraction=0.1, mutation_rate=0.2):
    """Основной алгоритм с островной моделью."""
    MIGRATION_INTERVAL = 5  # Миграция каждые 5 поколений
    MIGRATION_SIZE = 2      # Количество особей для миграции

    # Инициализация островов
    islands = []
    for _ in range(NUM_ISLANDS):
        population = generate_initial_population(population_size)
        fitness_scores = [fitness(ind) for ind in population]
        islands.append({'population': population, 'fitness_scores': fitness_scores})

    for generation in range(generations):
        print(f"\nПоколение {generation + 1}")
        for i, island in enumerate(islands):
            print(f"\nОстров {i + 1}")
            population = island['population']
            fitness_scores = island['fitness_scores']

            elites = apply_elitism(population, fitness_scores, elite_fraction)
            print(f"Элитные особи: {elites}")

            new_population = elites.copy()
            crossovers, mutations = [], []

            while len(new_population) < population_size:
                parent1 = tournament_selection(population, fitness_scores)
                parent2 = tournament_selection(population, fitness_scores)

                # Кроссовер
                child1, child2 = crossover(parent1, parent2)
                crossovers.append((parent1, parent2, child1, child2))

                # Мутации
                mutation1 = mutate(child1, mutation_rate)
                mutation2 = mutate(child2, mutation_rate)

                if mutation1:
                    new_population.append(mutation1[1])
                    mutations.append(mutation1)
                else:
                    new_population.append(child1)

                if mutation2:
                    new_population.append(mutation2[1])
                    mutations.append(mutation2)
                else:
                    new_population.append(child2)

            # Обновление популяции и приспособленности
            population = new_population[:population_size]
            fitness_scores = [fitness(ind) for ind in population]

            print(f"Популяция: {population}")
            print(f"Кроссоверы: {crossovers}")
            print(f"Мутации: {mutations}")

            island['population'] = population
            island['fitness_scores'] = fitness_scores

            viz.add_generation(population, fitness_scores, elites, crossovers, mutations, island_index=i)

        if (generation + 1) % MIGRATION_INTERVAL == 0:
            print("\nМиграция между островами")
            for i in range(NUM_ISLANDS):
                next_i = (i + 1) % NUM_ISLANDS
                island_from = islands[i]
                island_to = islands[next_i]

                # Получаем индексы лучших особей для миграции с текущего острова
                sorted_indices_from = np.argsort(island_from['fitness_scores'])
                migrants_indices = sorted_indices_from[-MIGRATION_SIZE:]
                migrants = [island_from['population'][idx] for idx in migrants_indices]
                migrants_fitness = [island_from['fitness_scores'][idx] for idx in migrants_indices]

                # Получаем индексы худших особей на целевом острове
                sorted_indices_to = np.argsort(island_to['fitness_scores'])
                worst_indices_to = sorted_indices_to[:MIGRATION_SIZE]

                # Выполняем фильтрацию и замену только если мигрант лучше особи на целевом острове
                for idx, migrant, migrant_fitness in zip(worst_indices_to, migrants, migrants_fitness):
                    if migrant_fitness > island_to['fitness_scores'][idx]:  # Проверка, лучше ли мигрант
                        island_to['population'][idx] = migrant
                        island_to['fitness_scores'][idx] = migrant_fitness
                        print(f"Миграция: особь {migrant} с fitness {migrant_fitness:.4f} "
                              f"заменяет особь на острове {next_i + 1}")
                    else:
                        print(f"Миграция отменена для особи {migrant} с fitness {migrant_fitness:.4f}, "
                              f"так как она хуже особи на острове {next_i + 1}")
            print("Миграция завершена")

    best_individual = None
    best_fitness = -np.inf
    for island in islands:
        population = island['population']
        fitness_scores = island['fitness_scores']
        island_best_individual, island_best_fitness = select_best_solution(population, fitness_scores)
        if island_best_fitness > best_fitness:
            best_individual = island_best_individual
            best_fitness = island_best_fitness

    return best_individual, best_fitness


# Запуск алгоритма
best_individual, best_fitness = genetic_algorithm(population_size=20, generations=10)
neurons, params = best_individual
optimizer, activation = params
print(f"\nЛучшее решение: {neurons}, Оптимизатор: {optimizer}, Активация: {activation}, F1: {best_fitness:.4f}")

# Запуск визуализатора
viz.run()
