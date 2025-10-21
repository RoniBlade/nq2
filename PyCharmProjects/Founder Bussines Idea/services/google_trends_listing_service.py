# services/google_trends_explore_service.py
import os
import re
from bs4 import BeautifulSoup

from parsers.google_trend_explore_parser import GoogleTrendsExploreParser
from anti_bot.anti_bot_browser import AntiBotBrowser
from sniffer.sniffer import NetworkTap, TapRule, TapContextForBrowserContext


class GoogleTrendsExploreService:
    def __init__(self, url: str, headless: bool = False):
        self.url = url
        self.headless = headless

    def get_explore_data(
        self,
        timeout_ms: int = 15_000,
        enable_sniffer: bool = True,
        sniffer_out: str = "tap_out",
    ):
        """
        Возвращает:
          - при enable_sniffer=True: (payload_dict, tap)
          - иначе: payload_dict
        """
        tap = None

        # 🎛️ правила сниффера — максимально «всё пиши, всё пропускай»
        rule = TapRule(
            debug=True,
            allow_ctypes=None,            # не фильтровать по Content-Type
            allow_resource_types=None,    # не фильтровать по типу ресурса
            allow_url=None,
            deny_url=None,                # не блокировать ничего
            max_bytes=1_000_000_000,      # высокий потолок размера (1 ГБ)
            skip_sse=False,               # не пропускать SSE (осторожно с объёмом)
            save_binary=True,             # сохранять бинарь
            gzip_text=False,              # писать .json/.txt без gzip (удобнее смотреть)
            save_request_body=True,
            request_body_max=1_000_000,   # не обрезать POST (f.req) слишком рано
            decode_batchexecute=True,     # разбор Google batchexecute
            index_path=None,              # без глобального индекса (включи при нужде)
            emit_curl=True,
        )

        with AntiBotBrowser(headless=self.headless) as bot:
            if enable_sniffer:
                tap = NetworkTap(
                    out_dir=sniffer_out,
                    rule=rule,
                    dedup_db=os.path.join(".tap_cache", "seen.sqlite"),  # персистентный дедуп
                )
                tap.rule.deny_url = None  # убрать дефолтный бан api.ipify.org (если был)

                # 📡 подключаемся ко ВСЕМ страницам контекста ДО перехода
                with TapContextForBrowserContext(bot.context, tap):
                    bot.goto(self.url)
                    self._kick_explore_requests(bot.page)
                    html = self._wait_for_explore_data(bot.page, timeout_ms)
            else:
                bot.goto(self.url)
                self._kick_explore_requests(bot.page)
                html = self._wait_for_explore_data(bot.page, timeout_ms)

        # 🧩 парсим HTML как раньше
        parser = GoogleTrendsExploreParser(html)
        payload = {
            "popularity_timeseries": parser.get_popularity_timeseries(),
            "regions": parser.get_regions(),
            "topics": parser.get_topics(),
            "related_queries": parser.get_related_queries(),
        }
        return (payload, tap) if enable_sniffer else payload

    # ——— помощники ————————————————————————————————————————————————

    def _kick_explore_requests(self, page) -> None:
        """Чуть «пинаем» страницу, чтобы отстрелялись все ленивые запросы."""
        try:
            page.wait_for_load_state("networkidle", timeout=10_000)
        except Exception:
            pass
        try:
            for _ in range(3):
                page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
                page.wait_for_timeout(900)
        except Exception:
            pass

    def _wait_for_explore_data(self, page, timeout_ms: int = 10_000) -> str:
        """
        Ждём, пока на странице появится хотя бы один надёжный маркер:
         • контейнер линейного графика (svg),
         • таблица «табличное представление / table view»,
         • блоки «Похожие запросы/Related queries», «Ещё по теме/Related topics»,
           «Популярность по субрегионам/Interest by subregion».
        """
        interval = 500
        attempts = max(1, timeout_ms // interval)

        label_re = re.compile(
            r"(Ещё по теме|Related topics|Похожие запросы|Related queries|"
            r"Популярность по субрегионам|Interest by subregion)",
            re.I,
        )

        for _ in range(attempts):
            html = page.content()
            soup = BeautifulSoup(html, "html.parser")

            has_chart_svg = soup.select_one("div.fe-line-chart-content-container svg")
            has_chart_table = (
                soup.select_one("div[aria-label*='табличное представление' i] table")
                or soup.select_one("div[aria-label*='table view' i] table")
            )
            has_label = soup.find(string=label_re)

            if has_chart_svg or has_chart_table or has_label:
                return html

            page.wait_for_timeout(interval)

        raise TimeoutError("❌ Не дождались Explore-данных Google Trends.")
