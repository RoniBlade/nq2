#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from typing import Any, Dict, Tuple, Optional, Sequence, Union
from tabulate import tabulate

# если используешь свою обёртку браузера — она может пригодиться для ручных проверок
# from anti_bot.anti_bot_browser import AntiBotBrowser

# твои сервисы
from services.google_trends_listing_service import GoogleTrendsListingService
from services.google_trends_explore_service import GoogleTrendsExploreService


Result = Union[Dict[str, Any], Tuple[Dict[str, Any], Any]]


def _unpack(result: Result) -> Tuple[Dict[str, Any], Optional[Any]]:
    """
    Универсальная распаковка результата сервиса:
      - dict -> (dict, None)
      - (dict, tap) -> (dict, tap)
    """
    if isinstance(result, tuple) and len(result) >= 1 and isinstance(result[0], dict):
        data = result[0]
        tap = result[1] if len(result) > 1 else None
        return data, tap
    if isinstance(result, dict):
        return result, None
    raise TypeError("Service returned unexpected result type; expected dict or (dict, tap) tuple.")


def show_listing_trends(enable_sniffer: bool = True) -> None:
    print("=== 📊 Google Trends: Ежедневные тренды ===")
    service = GoogleTrendsListingService()
    result = service.get_trending_table(enable_sniffer=enable_sniffer)
    data, tap = _unpack(result)

    if not data:
        print("Нет данных по ежедневным трендам.")
        return

    # Ожидаем список строк (list[Sequence]) — подгони заголовки под свою структуру
    headers = [
        "Тема",
        "Запросов",
        "Длина активности",
        "Начало",
        "Рост (abs)",
        "Рост (%)",
        "Составляющие тренда",
    ]
    try:
        print(tabulate(data, headers=headers, tablefmt="grid"))
    except Exception:
        # на случай, если формат отличается, просто выведем как есть
        print(data)

    if tap:
        print("\n[sniffer] активен — сырые и структурные файлы лежат в tap_out/")


def show_explore_data(
    explore_url: str,
    enable_sniffer: bool = True,
    headless: bool = True,
) -> None:
    print("\n=== 🔍 Google Trends: Обзор запроса (explore) ===")
    service = GoogleTrendsExploreService(url=explore_url, headless=headless)

    result: Result = service.get_explore_data(enable_sniffer=enable_sniffer)
    data, tap = _unpack(result)

    ts = data.get("popularity_timeseries") or []
    regions = data.get("regions") or []
    topics = data.get("topics") or []
    related = data.get("related_queries") or []

    print("\n Динамика популярности (точек):", len(ts))
    if ts:
        # покажем первые 10
        print(ts[:10], "...")

    if regions:
        print("\n Популярность по субрегионам:")
        try:
            print(tabulate(regions, headers=["Регион", "Рейтинг"], tablefmt="grid"))
        except Exception:
            print(regions)

    if topics:
        print("\n Ещё по теме:")
        try:
            print(tabulate(topics, headers=["Тема", "Рост"], tablefmt="grid"))
        except Exception:
            print(topics)

    if related:
        print("\n Похожие запросы:")
        try:
            print(tabulate(related, headers=["Запрос", "Рост"], tablefmt="grid"))
        except Exception:
            print(related)

    if tap:
        print("\n[sniffer] активен — смотрите tap_out/ для raw, .split/, _structured/ и метаданных.")


if __name__ == "__main__":
    # Пример explore: тема «Obsidian» (глобальная сущность). Поменяй q/geo/hl под свои нужды.
    # q принимает и строку запроса, и G-Entity (как /g/11n007kbcy)
    explore_url = "https://trends.google.com/trends/explore?geo=US&q=%2Fg%2F11n007kbcy&hl=ru"

    # Если нужно — можно сначала показать ежедневные тренды:
    # show_listing_trends(enable_sniffer=True)

    show_explore_data(
        explore_url=explore_url,
        enable_sniffer=True,   # при True сервис вернёт (data, tap); при False — только data
        headless=True,         # выстави False, если хочешь видеть браузер
    )

    # Пример ручной проверки антибота:
    # with AntiBotBrowser(headless=False) as bot:
    #     bot.goto("https://bot.sannysoft.com/")
    #     bot.wait_until_closed()
