# -*- coding: utf-8 -*-
"""
Офлайн-ингест: режет документы на чанки, пишет BM25 JSONL
и кладёт тексты в Chroma (с её embedding_function) — модель кэшируется 1 раз.
"""
import sys
from pathlib import Path
from rag_core import Cfg, ingest_folder

if __name__ == "__main__":
    cfg = Cfg.from_yaml("config.yaml")

    folder = None
    reset = False
    for arg in sys.argv[1:]:
        if arg == "--reset":
            reset = True
        else:
            folder = arg

    corpus = Path(folder or cfg.corpus_dir)
    corpus.mkdir(parents=True, exist_ok=True)

    if reset:
        # очищаем только BM25; Chroma — персистентная (в cfg.db_dir)
        try:
            Path(cfg.bm25_jsonl).unlink(missing_ok=True)
            print(f"Reset BM25 file: {cfg.bm25_jsonl}")
        except Exception as e:
            print(f"BM25 reset error: {e}")

    # основной процесс
    total = ingest_folder(str(corpus), cfg)
    print(f"Ingest done. Chunks: {total}")
