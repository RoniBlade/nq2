
# -*- coding: utf-8 -*-
"""
rag_core.py — ядро RAG-сервера (BM25 + E5/SBERT + MMR + Guard + LLM backends)
Совместим с rag_server.py и ingest.py. Конфигурация берётся из config.yaml.
"""

import re
import json
import glob
import uuid
import shutil
import hashlib
import logging
from dataclasses import dataclass
from pathlib import Path
from typing import List, Dict, Any, Tuple, Optional
from dataclasses import dataclass, fields

import numpy as np
import pymorphy3
from rank_bm25 import BM25Okapi
from sentence_transformers import SentenceTransformer
from langchain_chroma import Chroma
from rapidfuzz.fuzz import token_set_ratio
import yaml

# ==== Optional LLM backends ====
try:
    from llama_cpp import Llama  # type: ignore
except Exception:
    Llama = None

try:
    import ollama  # type: ignore
    _OLLAMA_AVAILABLE = True
except Exception:
    _OLLAMA_AVAILABLE = False

# ========= LOGGING =========
log = logging.getLogger("rag.core")

# ========= Prompt defaults =========
DEFAULT_PROMPT_SYSTEM = (
    "Вы — ассистент по работе с документами, отвечающий ТОЛЬКО на основе данных из раздела «Контекст» ниже. "
    "Всегда отвечайте на русском.\n\n"
    "Правила:\n"
    "1) Используйте только факты из «Контекста». Никаких догадок, обобщений и внешних знаний.\n"
    "2) Если нужной информации нет в «Контексте» — ответьте РОВНО: нет информации\n"
    "3) Отвечайте кратко (1–5 пунктов или 1–3 лаконичные фразы). Если вопрос предполагает инструкцию — дайте пошаговый список.\n"
    "4) Сохраняйте точные формулировки, цифры, даты и обозначения так, как они приведены в контексте.\n"
    "5) Не перечисляйте источники и технические детали индекса — это делает система. Не добавляйте от себя предупреждения и дисклеймеры.\n"
)

DEFAULT_PROMPT_USER_TEMPLATE = "Вопрос: {question}\n\nКонтекст:\n{context}\n\nОтвет:"

# ========= CONFIG =========
@dataclass
class Cfg:
    # Paths
    db_dir: str
    collection: str
    collection_salt: str
    corpus_dir: str
    bm25_jsonl: str

    # Retrieval / Ranking
    top_k_dense: int
    top_k_bm25: int
    rerank_top: int
    rerank_mode: str
    max_context_chars: int
    chunk_size: int
    chunk_overlap: int

    # LLM backend
    llm_backend: str

    # Ollama
    ollama_model: str
    ollama_host: str

    # llama.cpp
    llama_cpp_model_path: str
    num_ctx: int
    rag_max_tokens: int
    temperature: float
    top_p: float
    n_threads: int
    n_gpu_layers: int

    # Embeddings
    emb_backend: str
    emb_model_name: str
    emb_device: str

    # Guard thresholds
    guard_support_short: float
    guard_fuzzy_short: float
    guard_support_long: float
    guard_fuzzy_long: float

    # Prompt
    prompt_system: str = DEFAULT_PROMPT_SYSTEM
    prompt_user_template: str = DEFAULT_PROMPT_USER_TEMPLATE

    @staticmethod
    def from_yaml(path: str) -> "Cfg":
        with open(path, "r", encoding="utf-8") as f:
            raw = yaml.safe_load(f) or {}

        # Заберём и уберём вложенный блок prompt,
        # а также отбросим все ключи, которых нет в датаклассе.
        prompt_block = raw.pop("prompt", {}) or {}

        # Разрешённые поля из датакласса
        allowed = {f.name for f in fields(Cfg)}

        # Базовые данные без лишних ключей (например, комментариев/нестандартных полей)
        data = {k: v for k, v in raw.items() if k in allowed}

        # Разворачиваем prompt -> два поля, с дефолтами на всякий случай
        data["prompt_system"] = (
            prompt_block.get("system", raw.get("prompt_system", DEFAULT_PROMPT_SYSTEM))
        )
        data["prompt_user_template"] = (
            prompt_block.get("user_template", raw.get("prompt_user_template", DEFAULT_PROMPT_USER_TEMPLATE))
        )

        return Cfg(**data)


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

# ========= EMBEDDINGS =========
class E5Embeddings:
    """SentenceTransformer c E5-префиксами."""
    def __init__(self, model_name: str, device: str = "cpu"):
        self.model = SentenceTransformer(model_name, device=device)

    def embed_query(self, q: str):
        return self.model.encode([f"query: {q}"], normalize_embeddings=True, show_progress_bar=False)[0].tolist()

    def embed_documents(self, docs: List[str]):
        docs_pref = [f"passage: {d}" for d in docs]
        embs = self.model.encode(docs_pref, normalize_embeddings=True, show_progress_bar=False)
        return [e.tolist() for e in embs]

class SbertEmbeddings:
    """Обычный SBERT без префиксов."""
    def __init__(self, model_name: str, device: str = "cpu"):
        self.model = SentenceTransformer(model_name, device=device)

    def embed_query(self, q: str):
        return self.model.encode([q], normalize_embeddings=True, show_progress_bar=False)[0].tolist()

    def embed_documents(self, docs: List[str]):
        embs = self.model.encode(docs, normalize_embeddings=True, show_progress_bar=False)
        return [e.tolist() for e in embs]

def make_embedder(cfg: Cfg):
    if (cfg.emb_backend or "e5").lower() == "sbert":
        log.info("Embeddings: SBERT | model=%s | device=%s", cfg.emb_model_name, cfg.emb_device)
        return SbertEmbeddings(cfg.emb_model_name, cfg.emb_device)
    else:
        log.info("Embeddings: E5 | model=%s | device=%s", cfg.emb_model_name, cfg.emb_device)
        return E5Embeddings(cfg.emb_model_name, cfg.emb_device)

# ========= VECTOR DB =========
def _collection_name(cfg: Cfg) -> str:
    name = cfg.collection if not cfg.collection_salt else f"{cfg.collection}-{cfg.collection_salt}"
    name = re.sub(r"[^A-Za-z0-9._-]", "-", name)
    name = re.sub(r"\.{2,}", ".", name)
    name = re.sub(r"^[^A-Za-z0-9]+|[^A-Za-z0-9]+$", "", name)
    return name[:63] or "kb"

def make_vectordb(cfg: Cfg, embedder) -> Chroma:
    return Chroma(
        collection_name=_collection_name(cfg),
        embedding_function=embedder,
        persist_directory=cfg.db_dir,
    )

# ========= BM25 STORE =========
class BM25Store:
    def __init__(self, jsonl_path: str):
        self.jsonl_path = jsonl_path
        self.docs: List[Dict[str, Any]] = []
        self.tokens: List[List[str]] = []
        self.bm25: Optional[BM25Okapi] = None

    def load(self):
        self.docs, self.tokens, self.bm25 = [], [], None
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

# ========= INGESTION =========
def _stable_id(file_path: str, chunk_idx: int, text: str) -> str:
    h = hashlib.sha1((file_path + "::" + str(chunk_idx) + "::" + text[:128]).encode("utf-8")).hexdigest()
    return h

def split_text(text: str, size: int, overlap: int) -> List[str]:
    text = (text or "").strip().replace("\r\n", "\n")
    if not text:
        return []
    if len(text) <= size:
        return [text]
    chunks: List[str] = []
    i = 0
    while i < len(text):
        chunk = text[i:i + size]
        cut = max(chunk.rfind("\n\n"), chunk.rfind(". "), chunk.rfind("! "), chunk.rfind("? "))
        if cut < int(size * 0.6):
            cut = len(chunk)
        chunks.append(chunk[:cut].strip())
        i += max(1, cut - overlap)
    return [c for c in chunks if c]

def ingest_folder(folder: str, cfg: Cfg, reset: bool = False) -> Dict[str, Any]:
    embedder = make_embedder(cfg)

    if reset:
        shutil.rmtree(cfg.db_dir, ignore_errors=True)
        Path(cfg.db_dir).mkdir(parents=True, exist_ok=True)
        Path(cfg.bm25_jsonl).unlink(missing_ok=True)

    vectordb = make_vectordb(cfg, embedder)
    bm25 = BM25Store(cfg.bm25_jsonl)
    bm25.load()

    folder = str(folder or cfg.corpus_dir)
    paths: List[str] = []
    for ext in ("*.txt", "*.md"):
        paths += glob.glob(str(Path(folder) / ext), recursive=False)
        paths += glob.glob(str(Path(folder) / "**" / ext), recursive=True)
    if not paths:
        return {"added": 0, "chunks": 0, "message": "no files found"}

    added_docs, added_chunks = 0, 0
    bm25_items: List[Dict[str, Any]] = []

    for p in paths:
        try:
            txt = Path(p).read_text(encoding="utf-8", errors="ignore").strip()
        except Exception:
            continue
        if not txt:
            continue
        chunks = split_text(txt, cfg.chunk_size, cfg.chunk_overlap)
        if not chunks:
            continue

        metadatas, ids = [], []
        for i, ch in enumerate(chunks):
            cid = _stable_id(Path(p).as_posix(), i, ch)
            ids.append(cid)
            metadatas.append({
                "doc_path": str(Path(p).as_posix()),
                "chunk": i,
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
        bm25.add_many(bm25_items)

    return {"added": added_docs, "chunks": added_chunks, "message": "ok"}

# ========= RETRIEVAL + RERANK =========
def dense_top(query: str, vectordb: Chroma, k: int) -> List[Dict[str, Any]]:
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

def cosine_rerank(query: str, items: List[Dict[str, Any]], emb, topn: int) -> List[Dict[str, Any]]:
    if not items:
        return []
    q_emb = np.array(emb.embed_query(query), dtype=np.float32)
    texts = [it["text"] for it in items]
    embs = emb.embed_documents(texts)
    sims = [float(np.dot(q_emb, np.array(e, dtype=np.float32))) for e in embs]
    for it, sc in zip(items, sims):
        it["_score_cosine"] = sc
    items.sort(key=lambda x: x.get("_score_cosine", 0.0), reverse=True)
    return items[:topn]

def mmr_rerank(query: str, items: List[Dict[str, Any]], emb, topn: int, lambda_: float = 0.55) -> List[Dict[str, Any]]:
    if not items:
        return []
    q_emb = np.array(emb.embed_query(query), dtype=np.float32)
    texts = [it["text"] for it in items]
    embs = np.array(emb.embed_documents(texts), dtype=np.float32)
    q_sims = embs @ q_emb
    if len(items) <= topn:
        order = np.argsort(-q_sims)[:topn]
        return [items[i] for i in order]

    selected = []
    candidates = set(range(len(items)))
    first = int(np.argmax(q_sims))
    selected.append(first)
    candidates.remove(first)

    while len(selected) < topn and candidates:
        best_i = None
        best_score = -1e9
        for i in candidates:
            max_sim_sel = max(float(embs[i] @ embs[j]) for j in selected)
            score = lambda_ * float(q_sims[i]) - (1.0 - lambda_) * max_sim_sel
            if score > best_score:
                best_score = score
                best_i = i
        selected.append(best_i)
        candidates.remove(best_i)

    return [items[i] for i in selected]

def hybrid_retrieve(query: str, cfg: Cfg) -> Tuple[str, List[str], List[Dict[str, Any]]]:
    emb = make_embedder(cfg)
    vectordb = make_vectordb(cfg, emb)
    bm25 = BM25Store(cfg.bm25_jsonl)
    bm25.load()

    dense = dense_top(query, vectordb, cfg.top_k_dense)
    sparse = bm25.top_n(query, cfg.top_k_bm25) if cfg.top_k_bm25 > 0 else []
    merged = merge_candidates(dense, sparse)

    mode = (cfg.rerank_mode or "mmr").lower()
    if mode == "none":
        merged.sort(key=lambda x: (x.get("_score_dense") or 0.0), reverse=True)
        reranked = merged[: (cfg.rerank_top or cfg.top_k_dense)]
    elif mode == "cosine":
        reranked = cosine_rerank(query, merged, emb, cfg.rerank_top)
    else:
        reranked = mmr_rerank(query, merged, emb, cfg.rerank_top, lambda_=0.55)

    context_parts: List[str] = []
    sources: List[str] = []
    total = 0

    for it in reranked:
        part = (it.get("text") or "").strip()
        header = f'### {Path(it.get("file","?")).name} [chunk {it.get("chunk","?")}]\n'
        segment = header + part + "\n\n"
        if total + len(segment) > cfg.max_context_chars:
            break
        context_parts.append(segment)
        total += len(segment)
        if it.get("file"):
            sources.append(it["file"])

    ctx = "".join(context_parts)
    return ctx, sorted(set(sources)), reranked

# ========= GUARD + SANITIZE =========
def support_ratio(ans: str, ctx: str) -> float:
    a = to_lemmas(ans)
    c = set(to_lemmas(ctx))
    if not a:
        return 0.0
    hit = sum(1 for w in a if w in c)
    return hit / max(1, len(a))

def fuzzy_ratio(ans: str, ctx: str) -> float:
    return token_set_ratio(ans, ctx) / 100.0

def guard_accept(ans: str, ctx: str, cfg: Cfg) -> bool:
    a = (ans or "").strip()
    if not a or a.lower() == "нет информации":
        return False
    s1 = support_ratio(a, ctx)
    s2 = fuzzy_ratio(a, ctx)
    if len(a) <= 140:
        return (s1 >= cfg.guard_support_short) or (s2 >= cfg.guard_fuzzy_short)
    else:
        return (s1 >= cfg.guard_support_long) or (s2 >= cfg.guard_fuzzy_long)

def normalize_noinfo(ans: str) -> str:
    s = (ans or "").strip()
    low = s.lower()
    if "нет информации" in low:
        return "нет информации"
    if re.search(r"(not\s*found|no\s*info)\b", low):
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
def make_prompt_text(question: str, context: str, cfg: Cfg) -> str:
    system = cfg.prompt_system or DEFAULT_PROMPT_SYSTEM
    user_t = cfg.prompt_user_template or DEFAULT_PROMPT_USER_TEMPLATE
    return system.rstrip() + "\n\n" + user_t.format(question=question, context=context)

# ========= LLM BACKENDS =========
class LLMBackend:
    def complete(self, prompt: str) -> str:
        raise NotImplementedError
    def stream(self, prompt: str):
        raise NotImplementedError

class LlamaCppBackend(LLMBackend):
    def __init__(self, cfg: Cfg):
        if Llama is None:
            raise RuntimeError("llama_cpp не установлен (pip install llama-cpp-python)")
        log.info("Loading GGUF via llama.cpp: %s", cfg.llama_cpp_model_path)
        self._llama = Llama(
            model_path=cfg.llama_cpp_model_path,
            n_ctx=cfg.num_ctx,
            n_threads=cfg.n_threads,
            n_gpu_layers=cfg.n_gpu_layers,
            logits_all=False,
            embedding=False,
        )
        self.cfg = cfg

    def complete(self, prompt: str) -> str:
        out = self._llama.create_completion(
            prompt=prompt,
            max_tokens=self.cfg.rag_max_tokens,
            temperature=self.cfg.temperature,
            top_p=self.cfg.top_p,
            stream=False,
            repeat_penalty=1.15,
        )
        return (out["choices"][0]["text"] or "").strip()

    def stream(self, prompt: str):
        for ev in self._llama.create_completion(
            prompt=prompt,
            max_tokens=self.cfg.rag_max_tokens,
            temperature=self.cfg.temperature,
            top_p=self.cfg.top_p,
            stream=True,
            repeat_penalty=1.15,
        ):
            piece = ev["choices"][0].get("text", "")
            if piece:
                yield piece

class OllamaBackend(LLMBackend):
    def __init__(self, cfg: Cfg):
        if not _OLLAMA_AVAILABLE:
            raise RuntimeError("Пакет 'ollama' не установлен (pip install ollama)")
        if cfg.ollama_host:
            try:
                ollama.set_base_url(cfg.ollama_host)
            except Exception:
                pass
        self.model = cfg.ollama_model
        self.cfg = cfg
        log.info("Using Ollama model: %s @ %s", self.model, cfg.ollama_host or "default")

    def complete(self, prompt: str) -> str:
        res = ollama.generate(
            model=self.model,
            prompt=prompt,
            options={
                "num_ctx": self.cfg.num_ctx,
                "temperature": self.cfg.temperature,
                "top_p": self.cfg.top_p,
                "num_predict": self.cfg.rag_max_tokens,
            },
            stream=False,
        )
        return (res.get("response") or "").strip()

    def stream(self, prompt: str):
        for chunk in ollama.generate(
            model=self.model,
            prompt=prompt,
            options={
                "num_ctx": self.cfg.num_ctx,
                "temperature": self.cfg.temperature,
                "top_p": self.cfg.top_p,
                "num_predict": self.cfg.rag_max_tokens,
            },
            stream=True,
        ):
            piece = chunk.get("response", "")
            if piece:
                yield piece

def make_llm_backend(cfg: Cfg) -> "LLMBackend":
    be = (cfg.llm_backend or "ollama").lower()
    if be == "ollama":
        return OllamaBackend(cfg)
    elif be == "llama_cpp":
        return LlamaCppBackend(cfg)
    else:
        raise RuntimeError(f"Unknown LLM_BACKEND: {cfg.llm_backend}")

# ========= CHAT HELPERS =========
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
    return text, "OK"

def sse_event(data: dict) -> str:
    return f"data: {json.dumps(data, ensure_ascii=False)}\n\n"
