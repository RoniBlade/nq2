from dash import Dash, dcc, html, Input, Output
import webbrowser
from threading import Timer
import plotly.express as px
import pandas as pd
import numpy as np


class GeneticVisualizer:
    def __init__(self, fitness_func, num_islands=1):
        self.app = Dash(__name__)
        self.fitness_func = fitness_func
        self.num_islands = num_islands
        self.generations = [[] for _ in range(num_islands)]
        self.crossovers = [[] for _ in range(num_islands)]
        self.mutations = [[] for _ in range(num_islands)]
        self.elites = [[] for _ in range(num_islands)]
        self.current_gen = 0

        self._prepare_layout()

    def _tab_style(self):
        return {
            'backgroundColor': '#f0f8ff',
            'color': '#509ee3',
            'padding': '10px',
            'font-size': '18px',
            'border-radius': '10px'
        }

    def _selected_tab_style(self):
        return {
            'backgroundColor': '#509ee3',
            'color': 'white',
            'padding': '10px',
            'font-size': '18px',
            'border-radius': '10px'
        }

    def _button_style(self):
        return {
            'backgroundColor': '#509ee3',
            'color': 'white',
            'border': 'none',
            'padding': '10px',
            'margin-top': '10px',
            'border-radius': '10px',
            'cursor': 'pointer'
        }

    def _jury_component(self, tab_name):
        return html.Div(id=f'jury-component-{tab_name}', children=[
            html.H4(f"Оценка данных для {tab_name}", style={'margin-top': '10px'}),
            html.Pre(id=f'jury-info-{tab_name}', style={'whiteSpace': 'pre-wrap', 'font-size': '16px'})
        ])

    def _prepare_layout(self):
        """Создает макет интерфейса с вкладками и уникальными id для компонентов."""
        self.app.layout = html.Div(style={'backgroundColor': '#f0f8ff', 'fontFamily': 'Arial, sans-serif'}, children=[
            html.H1("Генетический алгоритм - Визуализация",
                    style={'textAlign': 'center', 'color': '#509ee3', 'margin': '20px'}),

            # Новый блок для лучшего решения
            html.Div([
                html.H3("Лучшее решение:", style={'textAlign': 'center', 'color': '#509ee3'}),
                html.Div(id='best-solution-display', style={'fontSize': '20px', 'textAlign': 'center', 'color': '#333'})
            ], style={'margin-bottom': '30px'}),

            html.Div([
                html.Label('Выберите остров:', style={'font-size': '18px'}),
                dcc.Dropdown(
                    id='island-dropdown',
                    options=[{'label': f'Остров {i + 1}', 'value': i} for i in range(self.num_islands)],
                    value=0,
                    style={'width': '200px', 'margin': '0 auto'})
            ], style={'textAlign': 'center', 'margin-bottom': '20px'}),

            dcc.Tabs([
                dcc.Tab(label='Популяции', style=self._tab_style(), selected_style=self._selected_tab_style(),
                        children=[
                            self._jury_component("population"),
                            dcc.Graph(id='population-graph'),
                            html.Div(id='population-info', style={'margin-top': '20px', 'font-size': '18px'}),
                            html.Button('Предыдущее поколение', id='prev-gen', n_clicks=0, style=self._button_style()),
                            html.Button('Следующее поколение', id='next-gen', n_clicks=0, style=self._button_style()),
                        ]),
                dcc.Tab(label='Кроссоверы и мутации', style=self._tab_style(),
                        selected_style=self._selected_tab_style(), children=[
                        self._jury_component("cross-mutation"),
                        html.Div(id='cross-mutation-info', style={'margin-top': '20px', 'font-size': '18px'}),
                    ]),
                dcc.Tab(label='Элитные особи', style=self._tab_style(), selected_style=self._selected_tab_style(),
                        children=[
                            self._jury_component("elite"),
                            html.Div(id='elite-info', style={'margin-top': '20px', 'font-size': '18px'}),
                        ]),
                dcc.Tab(label='Динамика развития', style=self._tab_style(), selected_style=self._selected_tab_style(),
                        children=[
                            dcc.Graph(id='fitness-dynamics-graph'),
                            html.Div(id='best-individual-info', style={'margin-top': '20px', 'font-size': '18px'})
                        ])
            ])
        ])

        self.app.callback(
            Output('population-graph', 'figure'),
            Output('population-info', 'children'),
            Output('jury-info-population', 'children'),
            Input('prev-gen', 'n_clicks'),
            Input('next-gen', 'n_clicks'),
            Input('island-dropdown', 'value')
        )(self.update_population_view)

        self.app.callback(
            Output('cross-mutation-info', 'children'),
            Output('jury-info-cross-mutation', 'children'),
            Input('next-gen', 'n_clicks'),
            Input('island-dropdown', 'value')
        )(self.update_crossovers_mutations)

        self.app.callback(
            Output('elite-info', 'children'),
            Output('jury-info-elite', 'children'),
            Input('next-gen', 'n_clicks'),
            Input('island-dropdown', 'value')
        )(self.update_elite_view)

        self.app.callback(
            Output('fitness-dynamics-graph', 'figure'),
            Input('island-dropdown', 'value')
        )(self.update_fitness_dynamics)

        self.app.callback(
            Output('best-individual-info', 'children'),
            Input('island-dropdown', 'value')
        )(self.update_best_individual)

        self.app.callback(
            Output('best-solution-display', 'children'),
            [Input('next-gen', 'n_clicks'),
             Input('prev-gen', 'n_clicks'),
             Input('island-dropdown', 'value')]
        )(self.update_best_solution)

    def update_population_view(self, prev_clicks, next_clicks, island_index):
        generations = self.generations[island_index]
        self.current_gen = (next_clicks - prev_clicks) % len(generations)
        population, fitness_scores = generations[self.current_gen]

        df = pd.DataFrame({
            "Индивид": [f"Индивид {i + 1}" for i in range(len(population))],
            "Приспособленность (F1)": fitness_scores,
            "Параметры": [f"Нейроны: {neurons}, Оптимизатор: {opt}, Активация: {act}"
                          for neurons, (opt, act) in population]
        })

        fig = px.scatter(
            df, x=[i for i in range(len(population))], y="Приспособленность (F1)",
            hover_data=["Параметры"], title=f"Популяция - Поколение {self.current_gen + 1}",
            size=[15] * len(population), color_discrete_sequence=["#509ee3"], template="plotly_white"
        )

        jury_info = f"Текущее поколение: {self.current_gen + 1}, Количество индивидов: {len(population)}"

        return fig, f"Текущее поколение: {self.current_gen + 1}", jury_info

    def update_fitness_dynamics(self, island_index):
        generations = self.generations[island_index]

        best_fitness_per_gen = [max(fitness_scores) for _, fitness_scores in generations]
        avg_fitness_per_gen = [np.mean(fitness_scores) for _, fitness_scores in generations]

        df = pd.DataFrame({
            "Поколение": [i + 1 for i in range(len(generations))],
            "Лучшее значение F1": best_fitness_per_gen,
            "Среднее значение F1": avg_fitness_per_gen
        })

        fig = px.line(df, x="Поколение", y=["Лучшее значение F1", "Среднее значение F1"],
                      labels={"value": "Приспособленность (F1)", "variable": "Метрика"},
                      title="Динамика развития популяций",
                      template="plotly_white")

        return fig

    def update_best_individual(self):
        best_individual = None
        best_fitness = -np.inf

        for island_generations in self.generations:
            for population, fitness_scores in island_generations:
                max_fitness = max(fitness_scores)
                best_index = fitness_scores.index(max_fitness)
                if max_fitness > best_fitness:
                    best_fitness = max_fitness
                    best_individual = population[best_index]

        neurons, (optimizer, activation) = best_individual
        best_individual_info = f"Лучший индивид: Нейроны: {neurons}, Оптимизатор: {optimizer}, Активация: {activation}, F1: {best_fitness:.4f}"

        return best_individual_info

    def update_best_solution(self, next_clicks, prev_clicks, island_index):
        """Обновляет информацию о лучшем решении для отображения на главном экране."""
        best_individual = None
        best_fitness = -np.inf

        # Ищем лучшее решение среди всех островов и поколений
        for island_generations in self.generations:
            for population, fitness_scores in island_generations:
                max_fitness = max(fitness_scores)
                best_index = fitness_scores.index(max_fitness)
                if max_fitness > best_fitness:
                    best_fitness = max_fitness
                    best_individual = population[best_index]

        if best_individual is not None:
            neurons, (optimizer, activation) = best_individual
            best_solution_info = f"Нейроны: {neurons}, Оптимизатор: {optimizer}, Активация: {activation}, F1: {best_fitness:.4f}"
        else:
            best_solution_info = "Лучшее решение не найдено"

        return best_solution_info

    def update_crossovers_mutations(self, n_clicks, island_index):
        gen_index = n_clicks % len(self.crossovers[island_index])
        cross = self.crossovers[island_index][gen_index]
        mut = self.mutations[island_index][gen_index]

        crossover_info = "\n".join([
            f"Родители: {p1} и {p2} → Потомки: {c1}, {c2}"
            for p1, p2, c1, c2 in cross
        ])

        mutation_info = "\n".join([
            f"Было: {before} → Стало: {after}"
            for before, after in mut
        ])

        jury_info = f"Количество кроссоверов: {len(cross)}, Количество мутаций: {len(mut)}"

        return html.Div([
            html.H3(f"Поколение {gen_index + 1}"),
            html.H4("Кроссоверы:"),
            html.Pre(crossover_info),
            html.H4("Мутации:"),
            html.Pre(mutation_info),
        ]), jury_info

    def update_elite_view(self, n_clicks, island_index):
        gen_index = n_clicks % len(self.elites[island_index])
        elites = self.elites[island_index][gen_index]

        items = [
            html.Li(
                f"Индивид {i + 1}: Нейроны: {neurons}, Оптимизатор: {opt}, Активация: {act}, "
                f"F1: {self.fitness_func((neurons, (opt, act))):.4f}"
            )
            for i, (neurons, (opt, act)) in enumerate(elites)
        ]

        jury_info = f"Количество элитных особей: {len(elites)}"

        return html.Div([
            html.H3(f"Элитные особи - Поколение {gen_index + 1}"),
            html.Ul(items, style={'list-style-type': 'none', 'padding': '0'}),
        ]), jury_info

    def add_generation(self, population, fitness_scores, elites, crossovers, mutations, island_index=0):
        self.generations[island_index].append((population, fitness_scores))
        self.elites[island_index].append(elites)
        self.crossovers[island_index].append(crossovers)
        self.mutations[island_index].append(mutations)

    def run(self, port=8050):
        webbrowser.open(f"http://127.0.0.1:{port}")
        Timer(1, self.app.run, kwargs={'port': port, 'debug': False, 'use_reloader': False}).start()
