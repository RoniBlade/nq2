import os
import re
import json
import gzip
import hashlib
from typing import Optional
from bs4 import BeautifulSoup

from parsers.google_trend_explore_parser import GoogleTrendsExploreParser
from anti_bot.anti_bot_browser import AntiBotBrowser
from sniffer.sniffer import NetworkTap, TapRule, TapContextForBrowserContext


class GoogleTrendsExploreService:
    def __init__(self, url: str, headless: bool = True):
        self.url = url
        self.headless = headless

    # ——— единая фабрика правил сниффера
    @staticmethod
    def _default_tap_rule() -> TapRule:
        return TapRule(
            debug=True,
            allow_ctypes=None,            # не фильтруем по Content-Type
            allow_resource_types=None,    # не фильтруем по типам (xhr/fetch/…)
            allow_url=None,
            deny_url=None,                # ничего не блочим
            max_bytes=1_000_000_000,      # потолок 1 ГБ (по факту no-drop)
            skip_sse=False,               # пишем и SSE (осторожно с объёмом)
            save_binary=True,
            gzip_text=True,               # удобнее хранить
            save_request_body=True,
            request_body_max=1_000_000,
            decode_batchexecute=True,     # разбор Google batchexecute
            index_path=os.path.join("tap_out", "_index.jsonl"),
            emit_curl=True
        )

    # ——— mirror: сохранить чистый widgetdata JSON как у твоего примера
    @staticmethod
    def _strip_xssi(text: str) -> str:
        if text.startswith(")]}',") or text.startswith(")]}'"):
            # у гугла часто есть ведущая запятая после префикса
            text = text[4:].lstrip(", \r\n\t")
        return text

    @staticmethod
    def _hash_sig(url: str, status: int, body_head: bytes) -> str:
        h = hashlib.sha1()
        h.update(url.encode("utf-8", "ignore"))
        h.update(str(status).encode("ascii"))
        h.update(body_head)
        return h.hexdigest()

    def _install_widgetdata_mirror(self, page, out_dir: str):
        """
        Слушаем все ответы. Если это widgetdata/* — пишем json.gz в tap_out/_structured/...
        """
        struct_base = os.path.join(out_dir, "_structured")
        os.makedirs(struct_base, exist_ok=True)

        def handle_response(resp):
            try:
                url = resp.url
                if "trends/api/widgetdata" not in url:
                    return

                # читаем ответ (как текст), снимаем XSSI
                text = resp.text()
                if not text:
                    return
                text = self._strip_xssi(text)

                # парсим JSON; если вдруг "json внутри строки" — попробуем ещё раз
                try:
                    obj = json.loads(text)
                except Exception:
                    try:
                        obj = json.loads(json.loads(text))
                    except Exception:
                        return  # не JSON — пропускаем

                kind = "interest_over_time" if "multiline" in url else \
                       "interest_by_region" if "comparedgeo" in url else "widgetdata"
                subdir = os.path.join(struct_base, kind)
                os.makedirs(subdir, exist_ok=True)

                # стабильное имя файла (как у tap): sha1(url+status+первые 64 байта тела)
                status = resp.status
                body_head = text.encode("utf-8", "ignore")[:64]
                sig = self._hash_sig(url, status, body_head)
                fpath = os.path.join(subdir, f"{sig}.json.gz")

                with gzip.open(fpath, "wt", encoding="utf-8") as f:
                    json.dump(obj, f, ensure_ascii=False, indent=2)

                # можно раскомментировать для отладки:
                # print(f"[mirror] {kind} -> {fpath}")

            except Exception:
                # не мешаем основному потоку
                pass

        page.on("response", handle_response)

    def _kick_explore_requests(self, page) -> None:
        """
        Форсируем генерацию запросов widgetdata/*:
        - ждём networkidle
        - делаем пару скроллов
        """
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

    def get_explore_data(
        self,
        timeout_ms: int = 15_000,
        enable_sniffer: bool = True,
        sniffer_out: str = "tap_out",
        dedup_cache: str = os.path.join(".tap_cache", "seen.sqlite"),
    ):
        """
        Возвращает:
          - при enable_sniffer=True: (payload_dict, tap)
          - иначе: payload_dict
        """
        tap = None
        rule = self._default_tap_rule()

        with AntiBotBrowser(headless=self.headless) as bot:
            # вешаем наш «зеркальный» слушатель — именно он даёт тебе JSON как в примере
            self._install_widgetdata_mirror(bot.page, sniffer_out)

            if enable_sniffer:
                tap = NetworkTap(out_dir=sniffer_out, rule=rule, dedup_db=dedup_cache)
                tap.rule.deny_url = None  # подстраховка, чтобы ничего не отрезалось

                # подключаем сниффер ДО перехода на страницу
                with TapContextForBrowserContext(bot.context, tap):
                    bot.goto(self.url)
                    self._kick_explore_requests(bot.page)
                    html = self._wait_for_explore_data(bot.page, timeout_ms)
            else:
                bot.goto(self.url)
                self._kick_explore_requests(bot.page)
                html = self._wait_for_explore_data(bot.page, timeout_ms)

        parser = GoogleTrendsExploreParser(html)
        payload = {
            "popularity_timeseries": parser.get_popularity_timeseries(),
            "regions": parser.get_regions(),
            "topics": parser.get_topics(),
            "related_queries": parser.get_related_queries(),
        }
        return (payload, tap) if enable_sniffer else payload

    def _wait_for_explore_data(self, page, timeout_ms: int = 10_000) -> str:
        """
        Ждём, пока на странице появится хотя бы один из надёжных маркеров:
        - контейнер линейного графика, или
        - таблица “табличное представление”, или
        - блоки “Похожие запросы/Related queries”, “Ещё по теме/Related topics”,
          “Популярность по субрегионам/Interest by subregion”.
        """
        interval = 500
        attempts = max(1, timeout_ms // interval)

        # RU/EN якоря
        label_re = re.compile(
            r"(Ещё по теме|Related topics|Похожие запросы|Related queries|"
            r"Популярность по субрегионам|Interest by subregion)",
            re.I
        )

        for _ in range(attempts):
            html = page.content()
            soup = BeautifulSoup(html, "html.parser")

            # менее хрупкий признак графика — наличие svg в контейнере
            has_chart_svg = soup.select_one("div.fe-line-chart-content-container svg")
            has_chart_table = soup.select_one("div[aria-label*='табличное представление' i] table") \
                               or soup.select_one("div[aria-label*='table view' i] table")

            has_label = soup.find(string=label_re)

            if has_chart_svg or has_chart_table or has_label:
                return html

            page.wait_for_timeout(interval)

        raise TimeoutError("❌ Не дождались Explore-данных Google Trends.")
