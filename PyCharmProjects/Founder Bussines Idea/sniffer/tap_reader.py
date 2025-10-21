#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Google Trends batchexecute → pretty JSON (без аргументов).

Как работает по умолчанию:
  • Берёт вход из папки tap_out/ (все .txt внутри, рекурсивно).
  • Парсит batchexecute и декодирует payload.
  • Пишет общий JSON в out/trends_parsed.json.
  • Дополнительно кладёт разложение по RPC в out/rpc/*.json.

Настраивается в секции CONFIG ниже.
Зависимости: только стандартная библиотека Python 3.
Лицензия: MIT
"""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict, Iterable, List, Tuple, Union

# ---------- CONFIG (правьте под себя) ----------
CONFIG = {
    # Файлы или директории для парсинга (рекурсивно)
    "INPUTS": [
        r"tap_out",  # можно добавить пути типа r"C:\...\tap_out"
    ],
    # Какие расширения брать внутри директорий
    "SCAN_EXTS": {".txt"},  # при необходимости добавьте ".log", ".json"
    # Куда писать общий JSON (None — не писать)
    "OUT_PATH": r"out/trends_parsed.json",
    # Куда раскладывать по RPC (None — не раскладывать)
    "SPLIT_PER_RPC_DIR": r"out/rpc",
    # Писать ли также «decoded» (сырой декодированный wrb.fr) вместе с нормализованным
    "ALSO_RAW": False,
}
# -----------------------------------------------

Json = Union[dict, list, str, int, float, bool, None]


# ---------- Core parsing ----------

def extract_json_arrays(blob: str) -> List[str]:
    """
    Достаём верхнеуровневые JSON-массивы из batchexecute-блоба.
    Игнорируем префиксы-размеры (числа/счётчики). Парсер
    «понимает» строки и не путается из-за скобок внутри кавычек.

    Возвращает подстроки вида:
        [[ "wrb.fr", "g4kJzf", "<payload string>", ... ], ["di",...], ...]
    """
    arrays: List[str] = []
    i, n = 0, len(blob)
    while True:
        start = blob.find('[[', i)
        if start == -1:
            break

        depth = 0
        in_str = False
        esc = False
        j = start
        while j < n:
            ch = blob[j]
            if in_str:
                if esc:
                    esc = False
                elif ch == '\\':
                    esc = True
                elif ch == '"':
                    in_str = False
            else:
                if ch == '"':
                    in_str = True
                elif ch == '[':
                    depth += 1
                elif ch == ']':
                    depth -= 1
                    if depth == 0:
                        arrays.append(blob[start:j+1])
                        i = j + 1
                        break
            j += 1
        else:
            # не нашли закрывающую скобку — выходим
            break

    return arrays


def parse_batchexecute(blob: str) -> List[Dict[str, Any]]:
    """
    Парсим blob в список RPC-записей.

    Структура элемента:
    {
      "rpc": "<имя rpc>",              # например "g4kJzf", "i0OFE", ...
      "raw_block": [...],              # исходный массив wrb.fr
      "payload_raw": "<string>",       # сырой payload (строка)
      "payload": <decoded JSON>,       # декодированный JSON
      "meta": { ... }                  # дополнительные поля из wrb.fr
    }
    """
    out: List[Dict[str, Any]] = []

    for top_level_str in extract_json_arrays(blob):
        try:
            top = json.loads(top_level_str)
        except json.JSONDecodeError:
            continue  # пропускаем мусор/обрывки

        # Ищем подмассивы, начинающиеся с "wrb.fr"
        for sub in top:
            if not (isinstance(sub, list) and sub and sub[0] == "wrb.fr"):
                continue

            entry: Dict[str, Any] = {
                "rpc": sub[1] if len(sub) > 1 else None,
                "raw_block": sub,
                "payload_raw": sub[2] if len(sub) > 2 else None,
                "payload": None,
                "meta": {},
            }

            if len(sub) > 3:
                entry["meta"] = {
                    f"f{i-3}": sub[i] for i in range(3, len(sub))
                }

            raw = entry["payload_raw"]
            if isinstance(raw, str):
                try:
                    entry["payload"] = json.loads(raw)
                except json.JSONDecodeError:
                    entry["payload"] = raw  # оставим как есть, если не парсится

            out.append(entry)

    return out


# ---------- Нормализаторы (best-effort) ----------

def normalize_g4kJzf(entry: Dict[str, Any]) -> Dict[str, Any]:
    """
    g4kJzf обычно «interest over time».

    Выдаём:
    {
      "rpc": "g4kJzf",
      "series": [
        {
          "label": "<метка>",
          "points": [{"t": 1756684800, "value": 12, "partial": false}, ...]
        }, ...
      ],
      "time_range": {"start": ..., "end": ...}
    }
    """
    payload = entry.get("payload")
    normalized = {"rpc": "g4kJzf"}

    if not isinstance(payload, list):
        return {**normalized, "raw": payload}

    series_list = payload[0] if payload and isinstance(payload[0], list) else []
    series_out = []
    t_min = None
    t_max = None

    for s in series_list:
        if not (isinstance(s, list) and len(s) >= 5 and isinstance(s[4], list)):
            continue
        label = s[0] if isinstance(s[0], str) else None
        points = []
        for dp in s[4]:
            # [value:int, 0, [[start:int],[end:int]], partial:bool]
            if isinstance(dp, list) and dp:
                value = dp[0]
                partial = False
                ts = None
                if len(dp) >= 4 and isinstance(dp[2], list) and len(dp[2]) >= 1:
                    try:
                        ts = int(dp[2][0][0])
                    except Exception:
                        ts = None
                    try:
                        t_end = int(dp[2][-1][0])
                    except Exception:
                        t_end = ts
                    t_min = ts if t_min is None else min(t_min, ts or t_min)
                    t_max = t_end if t_max is None else max(t_max, t_end or t_max)
                if len(dp) >= 4 and isinstance(dp[-1], bool):
                    partial = dp[-1]
                points.append({"t": ts, "value": value, "partial": partial})
        if points:
            series_out.append({"label": label, "points": points})

    result = {**normalized, "series": series_out}
    if t_min is not None and t_max is not None:
        result["time_range"] = {"start": t_min, "end": t_max}
    else:
        result["raw"] = payload

    return result


RPC_NORMALIZERS = {
    "g4kJzf": normalize_g4kJzf,
    # можно добавлять свои:
    # "i0OFE": normalize_i0OFE,
    # "DqDTgb": normalize_DqDTgb,
    # "Tnt4U": normalize_Tnt4U,
}


def normalize_entry(entry: Dict[str, Any]) -> Dict[str, Any]:
    rpc = entry.get("rpc")
    fn = RPC_NORMALIZERS.get(rpc)
    if fn:
        try:
            return fn(entry)
        except Exception:
            return {"rpc": rpc, "raw": entry.get("payload")}
    return {"rpc": rpc, "raw": entry.get("payload")}


# ---------- IO helpers ----------

def iter_input_paths(inputs: List[str], allow_exts: set[str]) -> Iterable[Path]:
    for p in inputs:
        path = Path(p)
        if path.is_dir():
            for f in path.rglob("*"):
                if f.is_file() and (not allow_exts or f.suffix.lower() in allow_exts):
                    yield f
        elif path.is_file():
            if not allow_exts or Path(p).suffix.lower() in allow_exts:
                yield path


def load_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="latin1", errors="replace")


def write_json(path: Path, data: Json) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


# ---------- main ----------

def main() -> int:
    inputs = CONFIG["INPUTS"]
    exts = CONFIG["SCAN_EXTS"]
    out_path = CONFIG["OUT_PATH"]
    split_dir = CONFIG["SPLIT_PER_RPC_DIR"]
    also_raw = CONFIG["ALSO_RAW"]

    all_entries: List[Dict[str, Any]] = []
    scanned = 0

    for path in iter_input_paths(inputs, exts):
        scanned += 1
        try:
            blob = load_text(path)
        except Exception:
            continue

        for entry in parse_batchexecute(blob):
            entry["_source"] = str(path)
            all_entries.append(entry)

    normalized = [normalize_entry(e) for e in all_entries]
    output: Json = (
        {"normalized": normalized, "decoded": all_entries}
        if also_raw else normalized
    )

    if split_dir:
        outdir = Path(split_dir)
        groups: Dict[str, List[Dict[str, Any]]] = {}
        for item in normalized:
            key = str(item.get("rpc") or "unknown")
            groups.setdefault(key, []).append(item)
        for rpc, items in groups.items():
            write_json(outdir / f"{rpc}.json", items)

    if out_path:
        write_json(Path(out_path), output)

    print(f"[OK] scanned files: {scanned}")
    print(f"[OK] parsed wrb.fr entries: {len(all_entries)}")
    if out_path:
        print(f"[OK] wrote: {out_path}")
    if split_dir:
        print(f"[OK] split per RPC under: {split_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
