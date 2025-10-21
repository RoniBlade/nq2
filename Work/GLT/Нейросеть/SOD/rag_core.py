# -*- coding: utf-8 -*-
"""
Ядро RAG: чтение конфига, Chroma + DefaultEmbeddingFunction (кэшируется 1 раз),
BM25, слияние кандидатов, реранк, сбор контекста, формирование промпта,
а также LLM-бекенды (Ollama / llama.cpp).
"""
from __future__ import annotations
import os
import json
import logging
import uuid
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import List, Dict, Any, Tuple, Optional

# ====================== CONFIG ======================

@dataclass
class Cfg:
    db_dir: str = "chroma_db"
    collection: str = "kb_main"
    collection_salt: str = "v2"
    corpus_dir: str = "documents"
    bm25_jsonl: str = "bm25_corpus.jsonl"

    top_k_dense: int = 24
    top_k_bm25: int = 30
    rerank_top: int = 8
    rerank_mode: str = "mmr"  # "mmr" | "cosine" | "none"
    max_context_chars: int = 9000
    chunk_size: int = 800
    chunk_overlap: int = 120

    llm_backend: str = "ollama"  # "ollama" | "llama_cpp"
    ollama_model: str = "gpt-oss:20b"
    ollama_host: str = "http://localhost:11434"

    llama_cpp_model_path: str = "models/model-q4_K.gguf"
    num_ctx: int = 4096
    rag_max_tokens: int = 192
    temperature: float = 0.1
    top_p: float = 0.9
    n_threads: int = 8
    n_gpu_layers: int = 0

    emb_backend: str = "sbert"  # "e5" | "sbert"
    emb_model_name: str = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
    emb_device: str = "cpu"

    guard_support_short: float = 0.12
    guard_fuzzy_short: float = 0.20
    guard_support_long: float = 0.10
    guard_fuzzy_long: float = 0.18

    # маппинг prompt.* из YAML → поля ниже
    prompt_system: str = (
        "Вы — ассистент. Отвечайте кратко на русском, используя предоставленный ниже Контекст.\n"
        "Если контекст пустой, скажите: нет информации.\n"
    )
    prompt_user_template: str = (
        "Вопрос: {question}\n\nКонтекст:\n{context}\n\nОтвет:"
    )

    @staticmethod
    def from_yaml(path: str) -> "Cfg":
        import yaml
        base = asdict(Cfg())
        with open(path, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f) or {}
        # развернём prompt.*
        if isinstance(data.get("prompt"), dict):
            pr = data["prompt"]
            data["prompt_system"] = pr.get("system", base["prompt_system"])
            data["prompt_user_template"] = pr.get("user_template", base["prompt_user_template"])
            del data["prompt"]
        merged = {**base, **data}
        return Cfg(**merged)

# =================== EMBEDDINGS (для косинус/ммр) ===================

class SBertEmbedder:
    def __init__(self, model_name: str, device: str):
        from sentence_transformers import SentenceTransformer
        self.model = SentenceTransformer(model_name, device=device)
    def encode(self, texts: List[str]):
        return self.model.encode(texts, show_progress_bar=False, convert_to_numpy=True)

def make_embedder(cfg: Cfg) -> SBertEmbedder:
    logging.info(f"Embeddings: SBERT | model={cfg.emb_model_name} | device={cfg.emb_device}")
    return SBertEmbedder(cfg.emb_model_name, cfg.emb_device)

# =================== CHROMA (Variant B) ===================

class ChromaDB:
    """
    Используем встроенную DefaultEmbeddingFunction (all-MiniLM-L6-v2).
    Кэш ONNX фиксируем в ./.chroma_cache → скачивается 1 раз.
    """
    def __init__(self, cfg: Cfg):
        # фиксируем кэш-папку для ONNX-модели Chroma
        cache_root = Path(".chroma_cache").absolute()
        cache_root.mkdir(parents=True, exist_ok=True)
        os.environ.setdefault("XDG_CACHE_HOME", str(cache_root))  # Linux/macOS
        os.environ.setdefault("LOCALAPPDATA", str(cache_root))     # Windows

        import chromadb
        from chromadb.utils.embedding_functions import DefaultEmbeddingFunction
        self.client = chromadb.PersistentClient(path=cfg.db_dir)

        # первая и единственная загрузка модели случится при создании EF
        self.ef = DefaultEmbeddingFunction()

        self.collection = self.client.get_or_create_collection(
            name=cfg.collection + "_" + cfg.collection_salt,
            embedding_function=self.ef
        )

    def add_documents(self, items: List[Dict[str, Any]]):
        """Upsert документов (детерминированные id по file+chunk)."""
        if not items:
            return 0
        docs = [it["text"] for it in items]
        ids = [f'{it["file"]}:{it["chunk"]}' for it in items]
        metas = [{"file": it["file"], "chunk": it["chunk"]} for it in items]

        # перед add удалим существующие id (если перезаливаем корпус)
        try:
            self.collection.delete(ids=ids)
        except Exception:
            pass

        self.collection.add(documents=docs, metadatas=metas, ids=ids)
        return len(items)

    def top_k(self, query: str, k: int) -> List[Dict[str, Any]]:
        res = self.collection.query(
            query_texts=[query],
            n_results=k or 10,
            include=["documents", "metadatas", "distances"]
        )
        docs = res.get("documents", [[]])[0]
        metas = res.get("metadatas", [[]])[0]
        dists = res.get("distances", [[]])[0]
        out = []
        for i, text in enumerate(docs):
            md = metas[i] or {}
            out.append({
                "text": text,
                "file": md.get("file"),
                "chunk": md.get("chunk"),
                "_score_dense": (1.0 - float(dists[i] or 0.0))
            })
        return out

def make_vectordb(cfg: Cfg) -> ChromaDB:
    return ChromaDB(cfg)

# =================== BM25 (простой jsonl) ===================

class BM25Store:
    def __init__(self, path: str):
        self.path = path
        self.items: List[Dict[str, Any]] = []
    def load(self):
        if not Path(self.path).exists():
            logging.info("BM25 loaded: 0 chunks")
            return
        with open(self.path, "r", encoding="utf-8") as f:
            self.items = [json.loads(line) for line in f]
        logging.info(f"BM25 loaded: {len(self.items)} chunks")
    def top_n(self, query: str, n: int) -> List[Dict[str, Any]]:
        q = query.lower().split()
        scored = []
        for it in self.items:
            text = (it.get("text") or "").lower()
            score = sum(1 for t in set(q) if t in text)
            scored.append({**it, "_score_bm25": float(score)})
        scored.sort(key=lambda x: x["_score_bm25"], reverse=True)
        return scored[:n]

# =================== RETRIEVAL / RERANK / CONTEXT ===================

def dense_top(query: str, vectordb: ChromaDB, k: int) -> List[Dict[str, Any]]:
    try:
        return vectordb.top_k(query, k)
    except Exception as e:
        logging.warning(f"dense_top error: {e}")
        return []

def merge_candidates(dense: List[Dict[str, Any]], sparse: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    m: Dict[Tuple[str, int], Dict[str, Any]] = {}
    for it in (dense or []):
        key = (it.get("file") or "", int(it.get("chunk") or 0))
        base = m.get(key, {})
        base.update(it)
        m[key] = base
    for it in (sparse or []):
        key = (it.get("file") or "", int(it.get("chunk") or 0))
        base = m.get(key, {})
        for k in ("text", "file", "chunk"):
            base[k] = base.get(k) or it.get(k)
        base["_score_bm25"] = max(base.get("_score_bm25", 0.0), it.get("_score_bm25", 0.0))
        base["_score_dense"] = base.get("_score_dense", 0.0)
        m[key] = base
    out = list(m.values())
    for it in out:
        it["_score"] = it.get("_score_dense", 0.0) + 0.3 * it.get("_score_bm25", 0.0)
    out.sort(key=lambda x: x["_score"], reverse=True)
    return out

def cosine_rerank(query: str, items: List[Dict[str, Any]], embedder: SBertEmbedder, top_n: int) -> List[Dict[str, Any]]:
    if not items:
        return []
    import numpy as np
    vec_q = embedder.encode([query])[0]
    texts = [it["text"] for it in items]
    vecs = embedder.encode(texts)
    def cos(a, b):
        na = (a**2).sum()**0.5 + 1e-6
        nb = (b**2).sum()**0.5 + 1e-6
        return float((a @ b) / (na * nb))
    for it, v in zip(items, vecs):
        it["_cos"] = cos(vec_q, v)
    items.sort(key=lambda x: x["_cos"], reverse=True)
    return items[: (top_n or len(items))]

def mmr_rerank(query: str, items: List[Dict[str, Any]], embedder: SBertEmbedder, top_n: int, lam: float = 0.7) -> List[Dict[str, Any]]:
    if not items:
        return []
    scored = items[:]
    selected: List[Dict[str, Any]] = []
    while scored and len(selected) < (top_n or len(items)):
        cand = scored.pop(0)
        if not selected:
            selected.append(cand); continue
        def sim(a, b):
            sa = set((a.get("text") or "").lower().split())
            sb = set((b.get("text") or "").lower().split())
            return len(sa & sb) / (len(sa | sb) + 1e-6)
        mmr_score = lam * cand.get("_score", 0.0) - (1 - lam) * max(sim(cand, s) for s in selected)
        cand["_mmr"] = mmr_score
        selected.append(cand)
        scored.sort(key=lambda x: x.get("_score", 0.0), reverse=True)
    return selected[: (top_n or len(selected))]

def hybrid_retrieve(query: str, cfg: Cfg) -> Tuple[str, List[str], List[Dict[str, Any]]]:
    embedder = make_embedder(cfg)  # для реранка cosine/mmr
    vectordb = make_vectordb(cfg)
    bm25 = BM25Store(cfg.bm25_jsonl)
    bm25.load()

    dense = dense_top(query, vectordb, cfg.top_k_dense)
    sparse = bm25.top_n(query, cfg.top_k_bm25) if cfg.top_k_bm25 > 0 else []
    merged = merge_candidates(dense, sparse)

    mode = (cfg.rerank_mode or "mmr").lower()
    if mode == "none":
        reranked = merged[: (cfg.rerank_top or len(merged))]
    elif mode == "cosine":
        reranked = cosine_rerank(query, merged, embedder, cfg.rerank_top)
    else:
        reranked = mmr_rerank(query, merged, embedder, cfg.rerank_top)

    # собрать контекст (без обрезки, если max_context_chars <= 0)
    parts: List[str] = []
    sources: List[str] = []
    total = 0
    for it in reranked:
        text = (it.get("text") or "").strip()
        if not text:
            continue
        header = f'### {Path(it.get("file","?")).name} [chunk {it.get("chunk","?")}]\n'
        seg = header + text + "\n\n"
        if (cfg.max_context_chars and cfg.max_context_chars > 0) and (total + len(seg) > cfg.max_context_chars):
            break
        parts.append(seg); total += len(seg)
        if it.get("file"):
            sources.append(it["file"])
    ctx = "".join(parts)
    return ctx, sorted(set(sources)), reranked

# ====================== PROMPT ======================

def make_prompt_text(question: str, context: str, cfg: Cfg) -> str:
    return cfg.prompt_system + "\n" + cfg.prompt_user_template.format(question=question, context=context)

# ====================== LLM BACKENDS ======================

class OllamaBackend:
    def __init__(self, cfg: Cfg):
        self.host = cfg.ollama_host
        self.model = cfg.ollama_model
        self.max_tokens = cfg.rag_max_tokens
        self.temp = cfg.temperature
        self.top_p = cfg.top_p
    def complete(self, prompt: str) -> str:
        import requests
        resp = requests.post(f"{self.host}/api/generate", json={
            "model": self.model,
            "prompt": prompt,
            "options": {"temperature": self.temp, "top_p": self.top_p},
            "stream": False,
            "keep_alive": "12h",  # держим модель «тёплой»
        }, timeout=600)
        resp.raise_for_status()
        data = resp.json()
        return data.get("response", "")
    def stream(self, prompt: str):
        raise NotImplementedError

class LlamaCppBackend:
    def __init__(self, cfg: Cfg):
        from llama_cpp import Llama
        self._llama = Llama(model_path=cfg.llama_cpp_model_path, n_ctx=cfg.num_ctx,
                            n_threads=cfg.n_threads, n_gpu_layers=cfg.n_gpu_layers)
        self.max_tokens = cfg.rag_max_tokens
        self.temp = cfg.temperature
        self.top_p = cfg.top_p
    def complete(self, prompt: str) -> str:
        out = self._llama.create_completion(prompt, max_tokens=self.max_tokens,
                                            temperature=self.temp, top_p=self.top_p)
        return (out.get("choices") or [{}])[0].get("text", "")
    def stream(self, prompt: str):
        yield from self._llama.create_completion(prompt, max_tokens=self.max_tokens,
                                                 temperature=self.temp, top_p=self.top_p, stream=True)

def make_llm(cfg: Cfg):
    return OllamaBackend(cfg) if (cfg.llm_backend or "").lower() == "ollama" else LlamaCppBackend(cfg)

# ====================== INGEST ======================

def chunk_text(text: str, size: int, overlap: int) -> List[str]:
    text = (text or "").replace("\r\n", "\n").replace("\r", "\n")
    out: List[str] = []
    i = 0
    step = max(1, size - overlap)
    while i < len(text):
        out.append(text[i:i+size])
        i += step
    return out

def ingest_folder(folder: str, cfg: Cfg) -> int:
    """
    Режет файлы из `folder` на чанки, пишет BM25 jsonl,
    и кладёт документы в Chroma с её встроенной embedding_function.
    """
    folder = folder or cfg.corpus_dir
    Path(folder).mkdir(parents=True, exist_ok=True)

    items: List[Dict[str, Any]] = []
    for p in sorted(Path(folder).rglob("*")):
        if p.is_dir():
            continue
        if p.suffix.lower() not in {".txt", ".md"}:
            continue
        text = p.read_text(encoding="utf-8", errors="ignore")
        chunks = chunk_text(text, cfg.chunk_size, cfg.chunk_overlap)
        for i, ch in enumerate(chunks):
            items.append({"file": str(p), "chunk": i, "text": ch})

    # BM25 jsonl
    with open(cfg.bm25_jsonl, "w", encoding="utf-8") as f:
        for it in items:
            f.write(json.dumps(it, ensure_ascii=False) + "\n")
    logging.info(f"Ingested {len(items)} chunks into BM25 jsonl: {cfg.bm25_jsonl}")

    # Chroma upsert (это вызовет скачивание ONNX модели ОДИН раз → в ./.chroma_cache)
    db = make_vectordb(cfg)
    added = db.add_documents(items)
    logging.info(f"Upserted {added} chunks into Chroma collection at: {cfg.db_dir}")

    return len(items)
