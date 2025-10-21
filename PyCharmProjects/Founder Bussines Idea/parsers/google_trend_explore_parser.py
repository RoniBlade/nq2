from bs4 import BeautifulSoup
import re

class GoogleTrendsExploreParser:
    def __init__(self, html: str):
        self.soup = BeautifulSoup(html, "html.parser")

    def get_popularity_timeseries(self):
        return self.get_svg_trend_data()

    def get_svg_trend_data(self):
        """
        Извлекает SVG-график тренда и нормализует значения от 0 до 100.
        """
        chart_container = self.soup.select_one("div.fe-line-chart-content-container")
        if not chart_container:
            return []

        svg = chart_container.find("svg")
        if not svg:
            return []

        # Получим размеры (для масштабирования)
        rect = svg.find("rect")
        if not rect:
            return []

        try:
            height = float(rect.get("height", "0"))
        except ValueError:
            return []

        # Путь графика
        path = svg.find("path", attrs={"stroke": "#4c8df6"})
        if not path:
            return []

        d_attr = path.get("d", "")
        if not d_attr.startswith("M"):
            return []

        # Извлекаем y-координаты
        y_coords = []
        coords = re.findall(r"[ML](\d+\.?\d*),(\d+\.?\d*)", d_attr)
        for x, y in coords:
            try:
                y = float(y)
                y_coords.append(y)
            except ValueError:
                continue

        if not y_coords:
            return []

        y_min = min(y_coords)
        y_max = max(y_coords)
        y_range = y_max - y_min if y_max > y_min else 1

        # Нормализуем: чем выше на графике, тем выше значение (обратно пропорционально y)
        trend_data = [
            round(100 * (1 - (y - y_min) / y_range), 2)
            for y in y_coords
        ]

        return trend_data

    def get_regions(self):
        """
        Парсит популярность по субрегионам из таблицы
        """
        table = self.soup.select_one("div[aria-label*='табличное представление'] table")
        if not table:
            return []

        regions = []
        rows = table.select("tbody tr")
        for row in rows:
            cols = row.find_all("td")
            if len(cols) < 2:
                continue
            region = cols[0].get_text(strip=True)
            value = cols[1].get_text(strip=True)
            regions.append((region, value))

        return regions

    def get_topics(self):
        """
        Парсит 'Ещё по теме'
        """
        return self._parse_cards_by_title("Ещё по теме")

    def get_related_queries(self):
        """
        Парсит 'Похожие запросы'
        """
        return self._parse_cards_by_title("Похожие запросы")

    def _parse_cards_by_title(self, h4_text):
        """
        Общий парсер карточек по заголовку
        """
        header = self.soup.find("h4", string=h4_text)
        if not header:
            return []

        container = header.find_parent("div")
        if not container:
            return []

        spans = container.find_all("span")
        items = []
        for span in spans:
            txt = span.get_text(strip=True)
            if txt and txt != h4_text:
                items.append(txt)
        return items
