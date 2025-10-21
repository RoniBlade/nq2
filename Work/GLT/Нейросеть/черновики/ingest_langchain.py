# -*- coding: utf-8 -*-
r"""
ingest_langchain.py — офлайн-перезагрузка базы знаний без рестарта сервера.

Запуск:
  python ./sod_neuro/ingest_langchain.py                    # индексирует ./documents
  python ./sod_neuro/ingest_langchain.py --folder docs      # индексирует ./docs
  python ./sod_neuro/ingest_langchain.py --reset            # ПОЛНЫЙ сброс Chroma и BM25

ВАЖНО: при смене EMB_BACKEND / EMB_MODEL_NAME делайте --reset,
чтобы переиндексировать векторную базу той же моделью, что используется при запросах.
"""
import argparse
import json
import sys
from pathlib import Path

# --- надёжный импорт серверного модуля ---
HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
for p in (str(HERE), str(ROOT)):
    if p not in sys.path:
        sys.path.insert(0, p)

try:
    # если запускаем из папки sod_neuro/
    from rag_server_langchain import ingest_folder, CFG  # type: ignore
except ImportError:
    # если запускаем из корня проекта
    from sod_neuro.rag_server_langchain import ingest_folder, CFG  # type: ignore


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--folder", default=CFG.CORPUS_DIR, help="папка с .txt/.md")
    ap.add_argument("--reset", action="store_true", help="полный сброс индекса перед индексацией")
    args = ap.parse_args()

    Path(args.folder).mkdir(parents=True, exist_ok=True)
    try:
        res = ingest_folder(args.folder, reset=args.reset)
        print(json.dumps(res, ensure_ascii=False, indent=2))
        return 0
    except AttributeError as e:
        # самый частый случай — старый вызов vectordb.persist() в rag_server_langchain.py
        if "persist" in str(e).lower():
            print("Ошибка: в rag_server_langchain.py нельзя вызывать vectordb.persist() — удалите этот вызов.")
        raise
    except Exception:
        raise


if __name__ == "__main__":
    raise SystemExit(main())
