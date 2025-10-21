# -*- coding: utf-8 -*-
"""
RAG Hybrid Server (BM25 + E5/SBERT + MMR Rerank) — FastAPI + Chroma + (Ollama gpt-oss:20b | llama.cpp)

Запуск сервера:
  # Пример PowerShell
  $env:LLM_BACKEND="ollama"                          # "ollama" | "llama_cpp"
  $env:OLLAMA_MODEL="gpt-oss:20b"                    # модель для Ollama
  $env:OLLAMA_HOST="http://localhost:11434"         # адрес демона Ollama
  $env:LLAMA_CPP_MODEL_PATH="models/model-q4_K.gguf"# путь к GGUF (для llama.cpp)
  $env:NUM_CTX="4096"                                # общий ctx
  $env:RAG_MAX_TOKENS="192"                          # max ответных токенов
  $env:EMB_BACKEND="sbert"                           # "e5" | "sbert"
  $env:EMB_MODEL_NAME="sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
  $env:EMB_DEVICE="cpu"                               # "cpu" | "cuda" | "mps"
  $env:RERANK_MODE="mmr"                              # "mmr" | "cosine" | "none"
  $env:TOP_K_DENSE="24"; $env:TOP_K_BM25="30"; $env:RERANK_TOP="8"
  $env:MAX_CONTEXT_CHARS="9000"
  $env:GUARD_SUPPORT_SHORT="0.12"; $env:GUARD_FUZZY_SHORT="0.20"
  $env:GUARD_SUPPORT_LONG="0.10";  $env:GUARD_FUZZY_LONG="0.18"
  $env:COLLECTION="kb_main"; $env:COLLECTION_SALT="v2"

Индекс без рестарта (офлайн-перезагрузка базы):
  python ./ingest_langchain.py --folder ./documents [--reset]

Важно:
  - При смене EMB_* делайте реиндекс python ingest_langchain.py --reset
  - COLLECTION_SALT добавляется к имени коллекции (kb_main:v2), исключая «старый контекст».
"""
import os
import re
import json
import uuid
import glob
import time
import hashlib
import shutil
import logging
from dataclasses import dataclass
from pathlib import Path
from typing import List, Tuple, Dict, Any, Optional, Generator

import numpy as np

from fastapi import FastAPI, Query, Body
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse, JSONResponse, HTMLResponse

# Vector store
from langchain_chroma import Chroma

# Embeddings / ranking
from sentence_transformers import SentenceTransformer
from rank_bm25 import BM25Okapi
from rapidfuzz.fuzz import token_set_ratio

# llama.cpp (опционально)
try:
    from llama_cpp import Llama  # type: ignore
except Exception:  # pragma: no cover
    Llama = None  # noqa

# Ollama (опционально)
try:
    import ollama  # type: ignore
    _OLLAMA_AVAILABLE = True
except Exception:  # pragma: no cover
    _OLLAMA_AVAILABLE = False

# NLP utils
import pymorphy3

# ========= LOGGING =========
logging.basicConfig(level=logging.INFO, format="%(levelname)s | %(message)s")
log = logging.getLogger("rag")

# ========= SAFE ENV PARSERS =========
def env_int(name: str, default: int) -> int:
    v = os.getenv(name)
    try:
        return int(v) if v is not None else default
    except Exception:
        return default

def env_float(name: str, default: float) -> float:
    v = os.getenv(name)
    try:
        return float(v) if v is not None else default
    except Exception:
        return default

def env_bool(name: str, default: bool = False) -> bool:
    v = os.getenv(name)
    if v is None:
        return default
    return v.strip().lower() in ("1", "true", "yes", "y", "on")

def env_str(name: str, default: str) -> str:
    v = os.getenv(name)
    return v if v is not None else default

# ========= CONFIG =========
@dataclass
class Cfg:
    # Paths
    DB_DIR: str = os.getenv("DB_DIR", "chroma_db")
    COLLECTION: str = os.getenv("COLLECTION", "kb_main")
    COLLECTION_SALT: str = env_str("COLLECTION_SALT", "")
    CORPUS_DIR: str = os.getenv("CORPUS_DIR", "documents")
    BM25_JSONL: str = os.getenv("BM25_JSONL", "bm25_corpus.jsonl")

    # Retrieval / Ranking
    TOP_K_DENSE: int = env_int("TOP_K_DENSE", 16)
    TOP_K_BM25: int = env_int("TOP_K_BM25", 16)  # 0 = отключить BM25
    RERANK_TOP: int = env_int("RERANK_TOP", 6)
    RERANK_MODE: str = env_str("RERANK_MODE", "mmr")  # "mmr" | "cosine" | "none"
    MAX_CONTEXT_CHARS: int = env_int("MAX_CONTEXT_CHARS", 6000)
    CHUNK_SIZE: int = env_int("CHUNK_SIZE", 800)
    CHUNK_OVERLAP: int = env_int("CHUNK_OVERLAP", 120)

    # LLM backend
    LLM_BACKEND: str = env_str("LLM_BACKEND", "ollama")  # "ollama" | "llama_cpp"

    # LLaMA.cpp
    LLAMA_CPP_MODEL_PATH: str = env_str("LLAMA_CPP_MODEL_PATH", "models/model-q4_K.gguf")
    NUM_CTX: int = env_int("NUM_CTX", 4096)
    RAG_MAX_TOKENS: int = env_int("RAG_MAX_TOKENS", 128)
    TEMPERATURE: float = env_float("RAG_TEMPERATURE", 0.1)
    TOP_P: float = env_float("RAG_TOP_P", 0.9)
    N_THREADS: int = env_int("N_THREADS", os.cpu_count() or 4)
    N_GPU_LAYERS: int = env_int("N_GPU_LAYERS", 0)

    # Ollama
    OLLAMA_HOST: str = env_str("OLLAMA_HOST", "http://localhost:11434")
    OLLAMA_MODEL: str = env_str("OLLAMA_MODEL", "gpt-oss:20b")

    # === Embeddings (HF) ===
    # EMB_BACKEND: "e5" (с префиксами "query:/passage:") или "sbert" (без префиксов)
    EMB_BACKEND: str = env_str("EMB_BACKEND", "e5")
    EMB_MODEL_NAME: str = env_str("EMB_MODEL_NAME", "intfloat/multilingual-e5-base")
    EMB_DEVICE: str = env_str("EMB_DEVICE", "cpu")

    # Поведение препроцессора вопросов
    REFORMULATE_SHORT: bool = env_bool("REFORMULATE_SHORT", False)

    # Пороги «гварда»
    GUARD_SUPPORT_SHORT: float = env_float("GUARD_SUPPORT_SHORT", 0.12)
    GUARD_FUZZY_SHORT: float   = env_float("GUARD_FUZZY_SHORT", 0.20)
    GUARD_SUPPORT_LONG: float  = env_float("GUARD_SUPPORT_LONG", 0.10)
    GUARD_FUZZY_LONG: float    = env_float("GUARD_FUZZY_LONG", 0.18)

CFG = Cfg()

# ========= NLP UTILS =========
_morph = pymorphy3.MorphAnalyzer()
RUS_STOP = {
    "это","и","а","но","или","что","к","в","на","о","об","из","для","как","по",
    "же","ли","они","он","она","оно","мы","вы","я","бы","то","той","этот","эта",
    "эти","тот","та","те","нет","информации","ответ","контекст","вопрос",
    "есть","быть","из-за","из–за"
}

def to_lemmas(text: str) -> List[str]:
    words = re.findall(r"[A-Za-zА-Яа-яёЁ]+", (text or "").lower())
    lem: List[str] = []
    for w in words:
        if w in RUS_STOP:
            continue
        try:
            lem.append(_morph.parse(w)[0].normal_form)
        except Exception:
            lem.append(w)
    return lem

def norm_text(x: str) -> str:
    return (x or "").replace("\u00A0", " ").strip().lower()

# ========= EMBEDDINGS (E5 & SBERT wrappers) =========
class E5Embeddings:
    """SentenceTransformer с E5-префиксами (поддерживает RU)."""
    def __init__(self, model_name: str, device: str = "cpu"):
        self.model_name = model_name
        self.device = device
        self.model = SentenceTransformer(model_name, device=device)

    def embed_query(self, q: str):
        vec = self.model.encode([f"query: {q}"], normalize_embeddings=True, show_progress_bar=False)[0]
        return vec.tolist()

    def embed_documents(self, docs: List[str]):
        docs_pref = [f"passage: {d}" for d in docs]
        embs = self.model.encode(docs_pref, normalize_embeddings=True, show_progress_bar=False)
        return [e.tolist() for e in embs]

class SbertEmbeddings:
    """Обычный SentenceTransformer (мультиязычный SBERT, без префиксов)."""
    def __init__(self, model_name: str, device: str = "cpu"):
        self.model_name = model_name
        self.device = device
        self.model = SentenceTransformer(model_name, device=device)

    def embed_query(self, q: str):
        vec = self.model.encode([q], normalize_embeddings=True, show_progress_bar=False)[0]
        return vec.tolist()

    def embed_documents(self, docs: List[str]):
        embs = self.model.encode(docs, normalize_embeddings=True, show_progress_bar=False)
        return [e.tolist() for e in embs]

def make_embedder():
    backend = (CFG.EMB_BACKEND or "e5").lower()
    if backend == "sbert":
        log.info("Embeddings: SBERT | model=%s | device=%s", CFG.EMB_MODEL_NAME, CFG.EMB_DEVICE)
        return SbertEmbeddings(CFG.EMB_MODEL_NAME, device=CFG.EMB_DEVICE)
    else:
        log.info("Embeddings: E5 | model=%s | device=%s", CFG.EMB_MODEL_NAME, CFG.EMB_DEVICE)
        return E5Embeddings(CFG.EMB_MODEL_NAME, device=CFG.EMB_DEVICE)

EMB = make_embedder()

# ========= VECTOR DB =========
def _collection_name() -> str:
    name = CFG.COLLECTION if not CFG.COLLECTION_SALT else f"{CFG.COLLECTION}-{CFG.COLLECTION_SALT}"
    name = re.sub(r"[^A-Za-z0-9._-]", "-", name)
    name = re.sub(r"\.{2,}", ".", name)
    name = re.sub(r"^[^A-Za-z0-9]+|[^A-Za-z0-9]+$", "", name)
    return name[:63] or "kb"

vectordb = Chroma(
    collection_name=_collection_name(),
    embedding_function=EMB,
    persist_directory=CFG.DB_DIR,
)

# ========= BM25 КОРПУС =========
class BM25Store:
    def __init__(self, jsonl_path: str):
        self.jsonl_path = jsonl_path
        self.docs: List[Dict[str, Any]] = []
        self.tokens: List[List[str]] = []
        self.bm25: Optional[BM25Okapi] = None

    def load(self):
        self.docs, self.tokens = [], []
        if not Path(self.jsonl_path).exists():
            log.warning("BM25 JSONL not found: %s", self.jsonl_path)
            return
        with open(self.jsonl_path, "r", encoding="utf-8") as f:
            for line in f:
                try:
                    obj = json.loads(line)
                    txt = obj.get("text", "")
                    self.docs.append(obj)
                    self.tokens.append(to_lemmas(norm_text(txt)))
                except Exception:
                    continue
        if self.docs:
            self.bm25 = BM25Okapi(self.tokens)
            log.info("BM25 loaded: %d chunks", len(self.docs))

    def add_many(self, items: List[Dict[str, Any]]):
        with open(self.jsonl_path, "a", encoding="utf-8") as f:
            for it in items:
                f.write(json.dumps(it, ensure_ascii=False) + "\n")
        for it in items:
            self.docs.append(it)
            self.tokens.append(to_lemmas(norm_text(it["text"])))
        if self.tokens:
            self.bm25 = BM25Okapi(self.tokens)

    def top_n(self, query: str, k: int) -> List[Dict[str, Any]]:
        if not self.bm25:
            return []
        q_tokens = to_lemmas(norm_text(query))
        scores = self.bm25.get_scores(q_tokens)
        idx = sorted(range(len(scores)), key=lambda i: scores[i], reverse=True)[:k]
        return [{**self.docs[i], "_score_bm25": float(scores[i])} for i in idx]

BM25 = BM25Store(CFG.BM25_JSONL)
BM25.load()

# ========= CHUNKING / INGEST =========
def _stable_id(file_path: str, chunk_idx: int, text: str) -> str:
    h = hashlib.sha1((file_path + "::" + str(chunk_idx) + "::" + text[:128]).encode("utf-8")).hexdigest()
    return h

def split_text(text: str, size: int = CFG.CHUNK_SIZE, overlap: int = CFG.CHUNK_OVERLAP) -> List[str]:
    text = (text or "").strip().replace("\r\n", "\n")
    if not text:
        return []
    if len(text) <= size:
        return [text]
    chunks: List[str] = []
    i = 0
    while i < len(text):
        chunk = text[i:i + size]
        # дружелюбное разбиение по абзацам/предложениям
        cut = max(chunk.rfind("\n\n"), chunk.rfind(". "), chunk.rfind("! "), chunk.rfind("? "))
        if cut < int(size * 0.6):
            cut = len(chunk)
        chunks.append(chunk[:cut].strip())
        i += max(1, cut - overlap)
    return [c for c in chunks if c]

def read_text_file(path: str) -> str:
    try:
        with open(path, "r", encoding="utf-8") as f:
            return f.read()
    except Exception:
        return ""

def ingest_folder(folder: str, reset: bool = False) -> Dict[str, Any]:
    global vectordb
    if reset:
        shutil.rmtree(CFG.DB_DIR, ignore_errors=True)
        Path(CFG.DB_DIR).mkdir(parents=True, exist_ok=True)
        Path(CFG.BM25_JSONL).unlink(missing_ok=True)
        BM25.docs, BM25.tokens, BM25.bm25 = [], [], None
        vectordb = Chroma(
            collection_name=_collection_name(),
            embedding_function=EMB,
            persist_directory=CFG.DB_DIR,
        )

    folder = str(folder)
    paths: List[str] = []
    for ext in ("*.txt", "*.md"):
        paths += glob.glob(str(Path(folder) / ext), recursive=True)
        paths += glob.glob(str(Path(folder) / "**" / ext), recursive=True)
    if not paths:
        return {"added": 0, "chunks": 0, "message": "no files found"}

    added_docs, added_chunks = 0, 0
    bm25_items: List[Dict[str, Any]] = []

    for p in paths:
        txt = read_text_file(p).strip()
        if not txt:
            continue
        chunks = split_text(txt)
        if not chunks:
            continue

        metadatas = []
        ids = []
        for i, ch in enumerate(chunks):
            cid = _stable_id(Path(p).as_posix(), i, ch)
            ids.append(cid)
            metadatas.append({
                "doc_path": str(Path(p).as_posix()),
                "chunk": i,
                "doc_id": hashlib.sha1(Path(p).as_posix().encode("utf-8")).hexdigest(),
                "chunk_id": cid,
            })

        vectordb.add_texts(chunks, metadatas=metadatas, ids=ids)

        for i, ch in enumerate(chunks):
            bm25_items.append({
                "id": ids[i],
                "file": metadatas[i]["doc_path"],
                "chunk": i,
                "text": ch
            })

        added_docs += 1
        added_chunks += len(chunks)

    if bm25_items:
        BM25.add_many(bm25_items)

    return {"added": added_docs, "chunks": added_chunks, "message": "ok"}

# ========= HYBRID RETRIEVE + РЕРАНК =========

def dense_top(query: str, k: int) -> List[Dict[str, Any]]:
    # LangChain Chroma: берём top-k по косинусу
    try:
        docs_scores = vectordb.similarity_search_with_relevance_scores(query, k=k)
        out: List[Dict[str, Any]] = []
        for d, s in docs_scores:
            out.append({
                "id": d.metadata.get("chunk_id") or d.metadata.get("id") or str(uuid.uuid4()),
                "file": d.metadata.get("doc_path") or d.metadata.get("file"),
                "chunk": d.metadata.get("chunk"),
                "text": d.page_content,
                "_score_dense": float(s) if s is not None else None
            })
        return out
    except Exception:
        docs = vectordb.similarity_search(query, k=k)
        out: List[Dict[str, Any]] = []
        for d in docs:
            out.append({
                "id": d.metadata.get("chunk_id") or str(uuid.uuid4()),
                "file": d.metadata.get("doc_path") or d.metadata.get("file"),
                "chunk": d.metadata.get("chunk"),
                "text": d.page_content
            })
        return out


def merge_candidates(dense: List[Dict[str, Any]], sparse: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    by_key: Dict[str, Dict[str, Any]] = {}
    def key(it: Dict[str, Any]) -> str:
        return f'{it.get("file")}::{it.get("chunk")}::{hash(it.get("text",""))}'
    for src in (dense, sparse):
        for it in src:
            k = key(it)
            if k not in by_key:
                by_key[k] = dict(it)
            else:
                cur = by_key[k]
                if it.get("_score_dense") is not None:
                    cur["_score_dense"] = float(it["_score_dense"])
                if it.get("_score_bm25") is not None:
                    cur["_score_bm25"] = float(it["_score_bm25"])
    return list(by_key.values())


def cosine_rerank(query: str, items: List[Dict[str, Any]], topn: int) -> List[Dict[str, Any]]:
    if not items:
        return []
    q_emb = np.array(EMB.embed_query(query), dtype=np.float32)
    texts = [it["text"] for it in items]
    embs = EMB.embed_documents(texts)
    sims = [float(np.dot(q_emb, np.array(e, dtype=np.float32))) for e in embs]
    for it, sc in zip(items, sims):
        it["_score_cosine"] = sc
    items.sort(key=lambda x: x.get("_score_cosine", 0.0), reverse=True)
    return items[:topn]


def mmr_rerank(query: str, items: List[Dict[str, Any]], topn: int, lambda_: float = 0.5) -> List[Dict[str, Any]]:
    if not items:
        return []
    q_emb = np.array(EMB.embed_query(query), dtype=np.float32)
    texts = [it["text"] for it in items]
    embs = np.array(EMB.embed_documents(texts), dtype=np.float32)
    q_sims = embs @ q_emb
    if len(items) <= topn:
        # просто отсортируем по близости к запросу
        order = np.argsort(-q_sims)[:topn]
        return [items[i] for i in order]

    selected = []
    candidates = set(range(len(items)))
    # старт с лучшего по запросу
    first = int(np.argmax(q_sims))
    selected.append(first)
    candidates.remove(first)

    while len(selected) < topn and candidates:
        best_i = None
        best_score = -1e9
        for i in candidates:
            # максимальная схожесть с уже выбранными (diversity)
            max_sim_sel = max(float(embs[i] @ embs[j]) for j in selected)
            score = lambda_ * float(q_sims[i]) - (1.0 - lambda_) * max_sim_sel
            if score > best_score:
                best_score = score
                best_i = i
        selected.append(best_i)
        candidates.remove(best_i)

    return [items[i] for i in selected]


def hybrid_retrieve(query: str) -> Tuple[str, List[str], List[Dict[str, Any]]]:
    t0 = time.time()
    dense = dense_top(query, CFG.TOP_K_DENSE)
    sparse = BM25.top_n(query, CFG.TOP_K_BM25) if CFG.TOP_K_BM25 > 0 else []
    merged = merge_candidates(dense, sparse)

    if CFG.RERANK_MODE.lower() == "none":
        merged.sort(key=lambda x: (x.get("_score_dense") or 0.0), reverse=True)
        reranked = merged[: (CFG.RERANK_TOP or CFG.TOP_K_DENSE)]
    elif CFG.RERANK_MODE.lower() == "cosine":
        reranked = cosine_rerank(query, merged, CFG.RERANK_TOP)
    else:
        reranked = mmr_rerank(query, merged, CFG.RERANK_TOP, lambda_=0.55)

    context_parts: List[str] = []
    sources: List[str] = []
    total = 0

    for it in reranked:
        part = (it.get("text") or "").strip()
        header = f'### {Path(it.get("file","?")).name} [chunk {it.get("chunk","?")}]\n'
        segment = header + part + "\n\n"
        if total + len(segment) > CFG.MAX_CONTEXT_CHARS:
            break
        context_parts.append(segment)
        total += len(segment)
        if it.get("file"):
            sources.append(it["file"])

    ctx = "".join(context_parts)
    t1 = time.time()
    log.debug("retrieve %.1fms | dense=%d sparse=%d merged=%d reranked=%d ctx=%dB",
              (t1 - t0) * 1000, len(dense), len(sparse), len(merged), len(reranked), len(ctx))
    return ctx, sorted(set(sources)), reranked

# ========= GUARD + POST-PРОЦЕСС =========

def support_ratio(ans: str, ctx: str) -> float:
    a = to_lemmas(ans)
    c = set(to_lemmas(ctx))
    if not a:
        return 0.0
    hit = sum(1 for w in a if w in c)
    return hit / max(1, len(a))

def fuzzy_ratio(ans: str, ctx: str) -> float:
    return token_set_ratio(ans, ctx) / 100.0

def guard_accept(ans: str, ctx: str) -> bool:
    a = (ans or "").strip()
    if not a or a.lower() == "нет информации":
        return False
    s1 = support_ratio(a, ctx)
    s2 = fuzzy_ratio(a, ctx)
    if len(a) <= 140:
        return (s1 >= CFG.GUARD_SUPPORT_SHORT) or (s2 >= CFG.GUARD_FUZZY_SHORT)
    else:
        return (s1 >= CFG.GUARD_SUPPORT_LONG) or (s2 >= CFG.GUARD_FUZZY_LONG)

def normalize_noinfo(ans: str) -> str:
    s = (ans or "").strip()
    low = s.lower()
    if re.search(r"(not\s*found|неfound|no\s*info)\b", low):
        return "нет информации"
    if "нет информации" in low:
        return "нет информации"
    if re.search(r"\bнет\s+инф(о|ормац)", low):
        return "нет информации"
    if any(p in low for p in ["не знаю", "недостаточно данных", "сведений нет", "не содержится"]):
        return "нет информации"
    return s

_MIXMAP = str.maketrans({
    "A":"А","a":"а","B":"В","C":"С","c":"с","E":"Е","e":"е","H":"Н","K":"К","k":"к",
    "M":"М","m":"м","O":"О","o":"о","P":"Р","p":"р","T":"Т","t":"т","X":"Х","x":"х",
    "Y":"У","y":"у"
})

def _fix_mixed_scripts(text: str) -> str:
    def fix_word(w: str) -> str:
        if re.search(r"[A-Za-z]", w) and re.search(r"[А-Яа-яёЁ]", w):
            return w.translate(_MIXMAP)
        return w
    return re.sub(r"[A-Za-zА-Яа-яёЁ]+", lambda m: fix_word(m.group(0)), text)

def sanitize_answer(ans: str, ctx: str) -> str:
    s = normalize_noinfo(ans).strip()
    if s == "нет информации":
        return s
    s = _fix_mixed_scripts(s)
    s = re.sub(r"\s{2,}", " ", s).strip(" ,.;:—-")
    s = re.sub(r"\s+([,.!?])", r"\1", s).strip()
    return s or "нет информации"

# ========= PROMPT =========
STRICT_SYSTEM = (
    "Вы — ассистент по работе с документами, отвечающий ТОЛЬКО на основе данных из раздела «Контекст» ниже. "
    "Всегда отвечайте на русском.\n\n"
    "Правила:\n"
    "1) Используйте только факты из «Контекста». Никаких догадок, обобщений и внешних знаний.\n"
    "2) Если нужной информации нет в «Контексте» — ответьте РОВНО: нет информации\n"
    "3) Отвечайте кратко (1–5 пунктов или 1–3 лаконичные фразы). Если вопрос предполагает инструкцию — дайте пошаговый список.\n"
    "4) Сохраняйте точные формулировки, цифры, даты и обозначения так, как они приведены в контексте.\n"
    "5) Не перечисляйте источники и технические детали индекса — это делает система. Не добавляйте от себя предупреждения и дисклеймеры.\n"
)

USER_TEMPLATE = "Вопрос: {question}\n\nКонтекст:\n{context}\n\nОтвет:"


def make_prompt_text(question: str, context: str) -> str:
    return STRICT_SYSTEM + "\n\n" + USER_TEMPLATE.format(question=question, context=context)

# ========= LLM BACKENDS =========
class LLMBackend:
    def complete(self, prompt: str) -> str:
        raise NotImplementedError

    def stream(self, prompt: str):
        raise NotImplementedError


class LlamaCppBackend(LLMBackend):
    def __init__(self):
        if Llama is None:
            raise RuntimeError("llama_cpp не установлен")
        log.info("Loading GGUF via llama.cpp: %s", CFG.LLAMA_CPP_MODEL_PATH)
        self._llama = Llama(
            model_path=CFG.LLAMA_CPP_MODEL_PATH,
            n_ctx=CFG.NUM_CTX,
            n_threads=CFG.N_THREADS,
            n_gpu_layers=CFG.N_GPU_LAYERS,
            logits_all=False,
            embedding=False,
        )

    def complete(self, prompt: str) -> str:
        out = self._llama.create_completion(
            prompt=prompt,
            max_tokens=CFG.RAG_MAX_TOKENS,
            temperature=CFG.TEMPERATURE,
            top_p=CFG.TOP_P,
            stream=False,
            repeat_penalty=1.15,
        )
        return (out["choices"][0]["text"] or "").strip()

    def stream(self, prompt: str):
        for ev in self._llama.create_completion(
            prompt=prompt,
            max_tokens=CFG.RAG_MAX_TOKENS,
            temperature=CFG.TEMPERATURE,
            top_p=CFG.TOP_P,
            stream=True,
            repeat_penalty=1.15,
        ):
            piece = ev["choices"][0].get("text", "")
            if piece:
                yield piece


class OllamaBackend(LLMBackend):
    def __init__(self):
        if not _OLLAMA_AVAILABLE:
            raise RuntimeError("Пакет 'ollama' не установлен. pip install ollama")
        # перенаправляем на нужный хост, если указан
        if CFG.OLLAMA_HOST:
            try:
                ollama.set_base_url(CFG.OLLAMA_HOST)
            except Exception:
                pass
        self.model = CFG.OLLAMA_MODEL
        log.info("Using Ollama model: %s @ %s", self.model, CFG.OLLAMA_HOST)

    def complete(self, prompt: str) -> str:
        res = ollama.generate(
            model=self.model,
            prompt=prompt,
            options={
                "num_ctx": CFG.NUM_CTX,
                "temperature": CFG.TEMPERATURE,
                "top_p": CFG.TOP_P,
                "num_predict": CFG.RAG_MAX_TOKENS,
            },
            stream=False,
        )
        return (res.get("response") or "").strip()

    def stream(self, prompt: str):
        for chunk in ollama.generate(
            model=self.model,
            prompt=prompt,
            options={
                "num_ctx": CFG.NUM_CTX,
                "temperature": CFG.TEMPERATURE,
                "top_p": CFG.TOP_P,
                "num_predict": CFG.RAG_MAX_TOKENS,
            },
            stream=True,
        ):
            piece = chunk.get("response", "")
            if piece:
                yield piece


def make_llm_backend() -> LLMBackend:
    be = CFG.LLM_BACKEND.lower()
    if be == "ollama":
        return OllamaBackend()
    elif be == "llama_cpp":
        return LlamaCppBackend()
    else:
        raise RuntimeError(f"Unknown LLM_BACKEND: {CFG.LLM_BACKEND}")

LLM = make_llm_backend()

# ========= FASTAPI APP =========
from pydantic import BaseModel

app = FastAPI(title="RAG Hybrid (BM25 + E5/SBERT + MMR + dual-output)", version="3.1")
app.add_middleware(
    CORSMiddleware, allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"],
)

# ========= SCHEMAS =========
class ChatIn(BaseModel):
    question: str

class ChatOut(BaseModel):
    answer: str
    sources: List[str] = []
    debug: Dict[str, Any] = {}

# ========= HELPERS =========
GREETINGS = ("привет", "ку", "здравствуй", "здравствуйте", "hi", "hello", "хай", "йо")

def is_smalltalk(text: str) -> bool:
    low = (text or "").strip().lower()
    if re.fullmatch(r"(привет|здравствуй|здравствуйте|hi|hello|ку|йо)[!,. ]*", low):
        return True
    if re.fullmatch(r"(кто ты|ты кто|что ты умеешь|расскажи о себе)[?!. ]*", low):
        return True
    if re.fullmatch(r"что ты[?!. ]*", low):
        return True
    return False

def preprocess_question(q: str) -> Tuple[str, str]:
    text = (q or "").strip()
    if is_smalltalk(text):
        return text, "SMALLTALK"
    tokens = re.findall(r"[A-Za-zА-Яа-яёЁ0-9\-]+", text)
    if CFG.REFORMULATE_SHORT and 1 <= len(tokens) <= 3 and not re.search(r"[?!\.]", text):
        return f"Что такое {text}?", "REFORMULATED"
    return text, "OK"

def sse_event(data: dict) -> str:
    return f"data: {json.dumps(data, ensure_ascii=False)}\n\n"

# ========= ROUTES =========
@app.get("/")
def root():
    return {
        "ok": True,
        "db": CFG.DB_DIR,
        "collection": _collection_name(),
        "bm25": Path(CFG.BM25_JSONL).exists(),
        "corpus": CFG.CORPUS_DIR,
        "llm_backend": CFG.LLM_BACKEND,
        "ollama": {"host": CFG.OLLAMA_HOST, "model": CFG.OLLAMA_MODEL},
        "llama_cpp": {
            "model_path": CFG.LLAMA_CPP_MODEL_PATH,
            "n_ctx": CFG.NUM_CTX,
            "n_threads": CFG.N_THREADS,
            "n_gpu_layers": CFG.N_GPU_LAYERS,
            "temperature": CFG.TEMPERATURE,
            "top_p": CFG.TOP_P,
            "max_tokens": CFG.RAG_MAX_TOKENS,
        },
        "embedder": {
            "backend": CFG.EMB_BACKEND,
            "model_name": CFG.EMB_MODEL_NAME,
            "device": CFG.EMB_DEVICE,
        },
    }

@app.get("/embeddings/info")
def embeddings_info():
    return {
        "backend": CFG.EMB_BACKEND,
        "model_name": CFG.EMB_MODEL_NAME,
        "device": CFG.EMB_DEVICE,
    }

@app.post("/ingest")
def api_ingest(folder: str = Body(default=CFG.CORPUS_DIR, embed=True),
               reset: bool = Body(default=False, embed=True)):
    res = ingest_folder(folder, reset=reset)
    return res

@app.post("/chat", response_model=ChatOut)
def chat(payload: ChatIn):
    raw_q = (payload.question or "").strip()
    if not raw_q:
        return ChatOut(answer="нет информации", sources=[], debug={"reason": "empty"})
    eff_q, mode = preprocess_question(raw_q)
    if mode == "SMALLTALK":
        msg = "Привет! Отвечаю по данным из твоей базы."
        return ChatOut(answer=msg, sources=[], debug={"smalltalk": True})

    context, sources, _ = hybrid_retrieve(eff_q)
    if not context:
        return ChatOut(answer="нет информации", sources=[], debug={"reason": "no_context"})

    prompt = make_prompt_text(eff_q, context)
    try:
        raw_ans = LLM.complete(prompt)
    except Exception as e:
        return ChatOut(answer=f"ошибка генерации: {e}", sources=[], debug={"reason": "llm_error"})

    final_ans = sanitize_answer((raw_ans or "").strip(), context)
    if final_ans != "нет информации" and not guard_accept(final_ans, context):
        final_ans = "нет информации"

    dbg = {
        "dense_k": CFG.TOP_K_DENSE,
        "sparse_k": CFG.TOP_K_BM25,
        "rerank_top": CFG.RERANK_TOP,
        "rerank_mode": CFG.RERANK_MODE,
        "ctx_chars": len(context),
    }
    return ChatOut(answer=final_ans, sources=sources if final_ans != "нет информации" else [], debug=dbg)

@app.get("/chat/stream")
def chat_stream(q: str = Query(..., description="Вопрос")):
    raw_q = (q or "").strip()
    if not raw_q:
        return JSONResponse({"answer": "нет информации", "sources": []}, status_code=200)

    eff_q, mode = preprocess_question(raw_q)
    if mode == "SMALLTALK":
        def greet() -> Generator[str, None, None]:
            final = "Привет! Отвечаю по данным из твоей базы."
            yield sse_event({"token": final, "done": False})
            yield sse_event({"done": True, "sources": [], "answer": final})
        return StreamingResponse(greet(), media_type="text/event-stream", headers={
            "Cache-Control": "no-cache", "Connection": "keep-alive", "X-Accel-Buffering": "no",
        })

    context, sources, _ = hybrid_retrieve(eff_q)
    if not context:
        def noinfo() -> Generator[str, None, None]:
            yield sse_event({"token": "нет информации", "done": False})
            yield sse_event({"done": True, "sources": [], "answer": "нет информации"})
        return StreamingResponse(noinfo(), media_type="text/event-stream", headers={
            "Cache-Control": "no-cache", "Connection": "keep-alive", "X-Accel-Buffering": "no",
        })

    prompt = make_prompt_text(eff_q, context)

    def token_stream() -> Generator[str, None, None]:
        acc: List[str] = []
        try:
            for piece in LLM.stream(prompt):
                acc.append(piece)
                yield sse_event({"token": piece, "done": False})
        except Exception as e:
            final = f"ошибка генерации: {e}"
            yield sse_event({"token": f"[{final}]", "done": False})
            yield sse_event({"done": True, "sources": [], "answer": final})
            return

        raw_full = ("".join(acc)).strip()
        final_ans = sanitize_answer(raw_full, context)
        if final_ans != "нет информации" and not guard_accept(final_ans, context):
            final_ans = "нет информации"

        yield sse_event({
            "done": True,
            "sources": sources if final_ans != "нет информации" else [],
            "answer": final_ans,
        })

    return StreamingResponse(token_stream(), media_type="text/event-stream", headers={
        "Cache-Control": "no-cache", "Connection": "keep-alive", "X-Accel-Buffering": "no",
    })

# ---------- Minimal UI (история, один ответ) ---------- (история + dual view) ----------
@app.get("/ui", response_class=HTMLResponse)
def ui_page():
    return """
<!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8"/>
  <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate"/>
  <meta http-equiv="Pragma" content="no-cache"/>
  <meta http-equiv="Expires" content="0"/>
  <title>RAG Hybrid (BM25 + E5/SBERT + MMR)</title>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <style>
    :root{--bg:#0f1115;--panel:#151823;--accent:#5b8cff;--text:#e8e8f0;--muted:#9aa3b2;--user:#1f2a44;--bot:#1c2130}
    *{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:16px/1.5 system-ui,Segoe UI,Roboto}
    .wrap{max-width:980px;margin:0 auto;padding:24px}
    .head{display:flex;gap:12px;align-items:center;margin-bottom:16px}
    .dot{width:10px;height:10px;border-radius:50%;background:var(--accent);box-shadow:0 0 12px var(--accent)}
    .title{font-weight:700;font-size:18px}
    .panel{background:var(--panel);border:1px solid #23283a;border-radius:16px;padding:16px;min-height:520px;display:flex;flex-direction:column}
    .chat{flex:1;overflow:auto;padding:8px 4px;scroll-behavior:smooth}
    .msg{margin:6px 0;padding:12px 14px;border-radius:12px;white-space:pre-wrap;max-width:78%}
    .user{background:var(--user);align-self:flex-end;border-top-right-radius:6px}
    .bot{background:var(--bot);align-self:flex-start;border-top-left-radius:6px}
    .row{display:flex;gap:8px;margin-top:12px}
    input[type=text]{flex:1;padding:12px 14px;border-radius:10px;border:1px solid #2a3045;background:#0f1320;color:#e8e8f0}
    button{padding:10px 12px;border-radius:10px;border:1px solid #2a3045;background:var(--accent);color:#fff;font-weight:600;cursor:pointer}
    button[disabled]{opacity:.6;cursor:not-allowed}
    .card{background:#0f1320;border:1px solid #23283a;border-radius:12px;padding:12px;margin-top:6px}
    .sources{margin-top:6px;font-size:13px;color:#b9c3d6}.sources code{background:#141826;padding:2px 6px;border-radius:6px}
  </style>
</head>
<body>
<div class="wrap">
  <div class="head"><div class="dot"></div><div class="title">RAG Hybrid — BM25 + E5/SBERT + MMR</div></div>
  <div class="panel">
    <div id="chat" class="chat"></div>
    <form id="f" class="row" autocomplete="off">
      <input id="q" type="text" placeholder="Введите вопрос и нажмите Enter…"/>
      <button id="send" type="submit">Отправить</button>
      <button id="clear" type="button" style="margin-left:auto">Очистить историю</button>
    </form>
  </div>
</div>

<script>
  const chat  = document.getElementById('chat');
  const input = document.getElementById('q');
  const btn   = document.getElementById('send');
  const clr   = document.getElementById('clear');
  const form  = document.getElementById('f');

  let history = JSON.parse(localStorage.getItem('rag_history_v3') || '[]');

  function save(item){ history.push(item); localStorage.setItem('rag_history_v3', JSON.stringify(history)); }
  function el(tag, cls, text=''){ const d=document.createElement(tag); if(cls) d.className=cls; if(text!==undefined) d.textContent=text; return d; }
  function addUser(text){ const d=el('div','msg user',text); chat.appendChild(d); chat.scrollTop=chat.scrollHeight; }
  function addBotBox(){
    const wrap=el('div','msg bot'); const card=el('div','card'); card.innerHTML='<b>Ответ</b><div id="final" style="margin-top:6px"></div>';
    wrap.appendChild(card);
    const src=el('div','sources'); wrap.appendChild(src);
    chat.appendChild(wrap); chat.scrollTop=chat.scrollHeight;
    return {wrap, finalBox:card.querySelector('#final'), srcBox:src};
  }
  function setDis(x){ btn.disabled=x; input.disabled=x; }

  async function ask(){
    const q = input.value.trim();
    if(!q) return;
    addUser(q);
    input.value=''; setDis(true);

    const view = addBotBox();
    let es, acc='';

    // Пытаемся открыть SSE
    try { es = new EventSource(`/chat/stream?q=${encodeURIComponent(q)}`); }
    catch(e) { es = null; }

    // Если SSE даже не создался — сразу POST fallback
    if(!es){
      try{
        const r = await fetch('/chat', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({question:q}) });
        const j = await r.json();
        view.finalBox.textContent = j.answer || 'нет информации';
        if(j.sources?.length) view.srcBox.innerHTML = 'Источники:<br>' + j.sources.map(x=>`<code>${x}</code>`).join('<br>');
        save({q, answer:j.answer, sources:j.sources||[]});
      }catch(e2){
        view.finalBox.textContent = 'ошибка: ' + e2;
      }
      setDis(false);
      return;
    }

    es.onmessage = (ev)=>{
      try{
        const data = JSON.parse(ev.data);
        if(data.token && !data.done){ acc += data.token; view.finalBox.textContent = acc; }
        if(data.done){
          es.close();
          view.finalBox.textContent = (data.answer || acc || 'нет информации').trim();
          if(data.sources?.length) view.srcBox.innerHTML = 'Источники:<br>' + data.sources.map(x=>`<code>${x}</code>`).join('<br>');
          save({q, answer:(data.answer||acc||'').trim(), sources:data.sources||[]});
          setDis(false);
        }
      }catch(_){}
    };

    es.onerror = async ()=>{
      try{ es.close(); }catch(_){}
      // Фоллбэк на POST /chat
      try{
        const r = await fetch('/chat', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({question:q}) });
        const j = await r.json();
        view.finalBox.textContent = j.answer || (acc||'нет информации');
        if(j.sources?.length) view.srcBox.innerHTML = 'Источники:<br>' + j.sources.map(x=>`<code>${x}</code>`).join('<br>');
        save({q, answer:j.answer||acc||'', sources:j.sources||[]});
      }catch(e2){
        view.finalBox.textContent = 'ошибка соединения (SSE+fallback): ' + e2;
      }
      setDis(false);
    };
  }

  // Enter всегда работает: форма перехватывает submit
  form.addEventListener('submit', (e)=>{ e.preventDefault(); ask(); });
  // На всякий случай продублируем на keydown
  input.addEventListener('keydown', (e)=>{ if(e.key === 'Enter'){ e.preventDefault(); ask(); } });

  clr.addEventListener('click', ()=>{ localStorage.removeItem('rag_history_v3'); history=[]; chat.innerHTML=''; });

  // Фокус сразу в поле
  input.focus();
</script>
</body>
</html>
"""

# (опц.) CLI-ингест, если запускаешь как обычный скрипт
if __name__ == "__main__":
    print("Use API /ingest or run: python ./ingest_langchain.py --folder ./documents")
