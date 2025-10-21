# -*- coding: utf-8 -*-
"""Офлайн-индексация документов без рестарта сервера."""
import sys
from pathlib import Path
from rag_core import ingest_folder, Cfg

if __name__ == "__main__":
    cfg = Cfg.from_yaml("config.yaml")
    folder = None
    reset = False
    for arg in sys.argv[1:]:
        if arg == "--reset":
            reset = True
        else:
            folder = arg
    Path(folder or cfg.corpus_dir).mkdir(parents=True, exist_ok=True)
    res = ingest_folder(folder or cfg.corpus_dir, cfg, reset=reset)
    print(res)
