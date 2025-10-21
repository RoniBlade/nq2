from bs4 import BeautifulSoup
from typing import List, Tuple, Optional


class GoogleTrendsListingParser:
    def __init__(self, html: str):
        self.soup = BeautifulSoup(html, "html.parser")

    def parse_table(self) -> List[List[str]]:
        tbody = self.soup.select_one("table[role=grid] tbody:not([aria-hidden=true])")
        if not tbody:
            return []

        rows = tbody.find_all("tr")
        return [self._parse_row(row) for row in rows if self._parse_row(row)]

    def _parse_row(self, row) -> Optional[List[str]]:
        cells = row.find_all("td")
        if len(cells) < 4:
            return None

        title, queries, status_value, age = "", "", "", ""

        divs = cells[1].find_all("div", recursive=True)
        for div in divs:
            text = div.get_text(strip=True)
            html = str(div)

            if not title:
                title = text
            elif "Поисковых запросов" in text:
                queries = text.replace("Поисковых запросов:", "").strip()
            elif any(icon in html for icon in ["trending_up", "timelapse"]):
                _, status_value = self._extract_icon_and_text(div)
            else:
                age = text

        growth_abs_value, growth_abs_percent = self._parse_growth_cell(cells[2])
        related_text = self._parse_related_keywords(cells[4]) if len(cells) > 4 else ""

        return [
            title,
            queries,
            status_value,
            age,
            growth_abs_value,
            growth_abs_percent,
            related_text
        ]

    def _parse_growth_cell(self, cell) -> Tuple[str, str]:
        growth_divs = cell.find_all("div")
        if not growth_divs:
            return "", ""

        _, raw = self._extract_icon_and_text(growth_divs[0])
        parts = raw.split("+")
        if len(parts) == 2:
            return parts[0].strip(), parts[1].strip()
        return parts[0].strip() if parts else "", ""

    def _parse_related_keywords(self, cell) -> str:
        spans = cell.find_all("span")
        keywords = [s.get_text(strip=True) for s in spans if self._is_valid_keyword(s.get_text(strip=True))]
        return ", ".join(keywords)

    @staticmethod
    def _extract_icon_and_text(div, icons=("trending_up", "timelapse", "arrow_upward")) -> Tuple[str, str]:
        html = str(div)
        text = div.get_text(" ", strip=True).replace('\xa0', ' ')
        for icon in icons:
            if icon in html:
                cleaned = text.replace(icon, "").replace("↗", "").replace("arrow_upward", "").strip()
                return icon, cleaned
        return "", text.strip()

    @staticmethod
    def _is_valid_keyword(text: str) -> bool:
        trash_words = {"Поисковый", "запрос", "query_stats", "Обзор"}
        return bool(text) and not any(word in text for word in trash_words)
