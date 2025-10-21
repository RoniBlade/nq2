# main.py
import os, json, argparse
from typing import Any, Dict, List
import pandas as pd
import requests

from excel_reader import read_excel, to_prompt_payload
from ai_validator import ask_llm
from matrix_builder import build_tables

def parse_value(v: str):
    try:
        return json.loads(v)
    except Exception:
        return v

def apply_overrides(cfg: Dict[str, Any], sets: List[str]) -> Dict[str, Any]:
    """
    Применяет dot-notation overrides: --set a.b.c=value
    value парсим как JSON, если возможно.
    """
    for s in sets or []:
        if "=" not in s:
            print(f"[WARN] некорректный --set '{s}', жду key=val")
            continue
        key, val = s.split("=", 1)
        val = parse_value(val)
        node = cfg
        parts = key.split(".")
        for p in parts[:-1]:
            if p not in node or not isinstance(node[p], dict):
                node[p] = {}
            node = node[p]
        node[parts[-1]] = val
    return cfg

def get_model_info(host: str, model: str) -> Dict[str, Any] | None:
    """
    Пытаемся получить информацию о модели через Ollama /api/show.
    Если requests недоступен или ошибка — возвращаем None.
    """
    try:
        r = requests.post(f"{host}/api/show", json={"name": model}, timeout=10)
        if r.ok:
            return r.json()
    except Exception:
        return None
    return None

def build_output_paths(out_dir: str, out_base: str):
    os.makedirs(out_dir, exist_ok=True)
    paths = {
        "prompt": os.path.join(out_dir, f"{out_base}.llm_prompt.txt"),
        "raw": os.path.join(out_dir, f"{out_base}.llm_raw.txt"),
        "config": os.path.join(out_dir, f"{out_base}.llm_config.json"),
        "effective": os.path.join(out_dir, f"{out_base}.effective_config.json"),
        "top_csv": os.path.join(out_dir, f"{out_base}.business_roles.csv"),
        "map_csv": os.path.join(out_dir, f"{out_base}.role_mapping.csv"),
        "xlsx": os.path.join(out_dir, f"{out_base}.result.xlsx"),
    }
    return paths

def main():
    ap = argparse.ArgumentParser(description="Excel -> LLM (detector cfg) -> Common-roles matrices")
    # IO
    ap.add_argument("--file","-f", default="file.xlsx", help="Входной Excel")
    ap.add_argument("--out", default="out", help="Папка для результатов")
    ap.add_argument("--out-base", default="result", help="Базовое имя выходных файлов")
    # LLM
    ap.add_argument("--host", default="http://localhost:11434", help="Ollama host")
    ap.add_argument("--model", default="gpt-oss:20b", help="Имя модели Ollama")
    ap.add_argument("--timeout", type=int, default=45, help="LLM timeout (сек) для stream")
    ap.add_argument("--stream", action="store_true", help="Стрим-режим запроса к LLM (по умолчанию False — non-stream)")
    ap.add_argument("--print-model", action="store_true", help="Показать информацию о модели из Ollama /api/show")
    # Config
    ap.add_argument("--config", help="Путь к готовому JSON-конфигу (обходит LLM)")
    ap.add_argument("--set", action="append", dest="sets", help="Переопределения конфига: key=val, dot-notation, val можно как JSON")
    ap.add_argument("--print-effective", action="store_true", help="Печать эффективного конфига после всех применённых --set")
    args = ap.parse_args()

    # выводные пути
    paths = build_output_paths(args.out, args.out_base)

    # инфо о модели
    print(f"[RUN] file={args.file}")
    print(f"[RUN] out_dir={args.out}  out_base={args.out_base}")
    print(f"[RUN] model={args.model}  host={args.host}  stream={args.stream}  timeout={args.timeout}s")
    if args.print_model:
        info = get_model_info(args.host, args.model)
        if info:
            print("[MODEL INFO] name:", info.get("name"))
            if "parameters" in info: print("[MODEL INFO] parameters:", info["parameters"])
            if "model" in info: print("[MODEL INFO] model file:", info["model"])
            if "modified_at" in info: print("[MODEL INFO] modified_at:", info["modified_at"])
            details = info.get("details") or {}
            if details: print("[MODEL INFO] details:", json.dumps(details, ensure_ascii=False, indent=2))
        else:
            print("[MODEL INFO] (нет данных — возможно, модель не загружена/не поддерживается /api/show)")

    print("[1/4] Чтение Excel…")
    if not os.path.exists(args.file):
        raise SystemExit(f"Файл не найден: {args.file}")
    df, analysis = read_excel(args.file)
    payload = to_prompt_payload(analysis)

    if args.config and os.path.exists(args.config):
        print("[2/4] Использую готовый конфиг:", args.config)
        with open(args.config, "r", encoding="utf-8") as f:
            cfg = json.load(f)
        raw = None
        # сохраняем промпт для отладки
        with open(paths["prompt"], "w", encoding="utf-8") as f:
            f.write(payload)
    else:
        print("[2/4] Вопрос ИИ о структуре…")
        cfg, raw = ask_llm(args.host, args.model, payload, timeout_s=args.timeout, stream=args.stream)
        with open(paths["prompt"], "w", encoding="utf-8") as f:
            f.write(payload)
        with open(paths["raw"], "w", encoding="utf-8") as f:
            f.write(raw or "")
        if cfg is None:
            raise SystemExit("LLM не вернула корректный JSON (см. *.llm_raw.txt)")

    if args.sets:
        cfg = apply_overrides(cfg, args.sets)

    for k in ["columns","roles_parse","business_role_rule","output_headers"]:
        if k not in cfg:
            raise SystemExit(f"Нет ключа {k} в конфиге (после overrides)")

    with open(paths["config"], "w", encoding="utf-8") as f:
        json.dump(cfg, f, ensure_ascii=False, indent=2)
    if args.print_effective:
        print("\n[Effective config]\n" + json.dumps(cfg, ensure_ascii=False, indent=2) + "\n")
    with open(paths["effective"], "w", encoding="utf-8") as f:
        json.dump({
            "runtime": {
                "file": args.file,
                "out_dir": args.out,
                "out_base": args.out_base,
                "host": args.host,
                "model": args.model,
                "timeout": args.timeout,
                "stream": args.stream,
            },
            "config": cfg
        }, f, ensure_ascii=False, indent=2)

    print("[3/4] Построение таблиц…")
    top_df, map_df = build_tables(df, cfg)

    print("[4/4] Сохранение…")
    top_df.to_csv(paths["top_csv"], index=False, encoding="utf-8-sig")
    map_df.to_csv(paths["map_csv"], index=False, encoding="utf-8-sig")
    try:
        import xlsxwriter
        with pd.ExcelWriter(paths["xlsx"], engine="xlsxwriter") as xl:
            top_df.to_excel(xl, sheet_name="roles", index=False, startrow=0)
            map_df.to_excel(xl, sheet_name="roles", index=False, startrow=len(top_df)+2)
    except Exception:
        pass

    print("[DONE]")
    print("  prompt : ", paths["prompt"])
    if os.path.exists(paths["raw"]): print("  raw    : ", paths["raw"])
    print("  config : ", paths["config"])
    print("  effcfg : ", paths["effective"])
    print("  topcsv : ", paths["top_csv"])
    print("  mapcsv : ", paths["map_csv"])
    if os.path.exists(paths["xlsx"]): print("  xlsx   : ", paths["xlsx"])

if __name__ == "__main__":
    main()
