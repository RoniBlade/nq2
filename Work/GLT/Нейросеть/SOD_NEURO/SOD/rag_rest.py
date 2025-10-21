# -*- coding: utf-8 -*-
"""
REST-only RAG API — без UI и без /chat
Запуск: python rag_rest.py  (или uvicorn rag_rest:app --host 0.0.0.0 --port 8000)
"""

from typing import List, Dict, Any
from fastapi import FastAPI, Body, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

from rag_core import (
    Cfg, ingest_folder, make_llm_backend,
    hybrid_retrieve, make_prompt_text,
    sanitize_answer, guard_accept, sse_event
)

app = FastAPI(title="RAG REST API", version="1.0")

# Разрешаем CORS (если нужен фронт на другом домене)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_methods=["*"], allow_headers=["*"]
)

# ---- Глобальные объекты конфигурации и LLM ----
CFG = Cfg.from_yaml("config.yaml")
LLM = make_llm_backend(CFG)

# ---- Модели запроса/ответа ----
class AskIn(BaseModel):
    question: str

class AskOut(BaseModel):
    answer: str
    sources: List[str] = []
    debug: Dict[str, Any] = {}

# ---- Служебные эндпойнты ----
@app.get("/health")
def health():
    return {"ok": True, "config": {k: v for k, v in CFG.__dict__.items() if "prompt" not in k}}

@app.post("/ingest")
def api_ingest(folder: str = Body(default=None, embed=True),
               reset: bool = Body(default=False, embed=True)):
    return ingest_folder(folder or CFG.corpus_dir, CFG, reset=reset)

# ---- Получение ответа от LLM (синхронно) ----
@app.post("/ask", response_model=AskOut)
def ask(payload: AskIn):
    q = (payload.question or "").strip()
    if not q:
        return AskOut(answer="нет информации", sources=[], debug={"reason": "empty"})

    # RAG: поиск контекста
    ctx, sources, _ = hybrid_retrieve(q, CFG)
    if not ctx:
        return AskOut(answer="нет информации", sources=[], debug={"reason": "no_context"})

    # Промпт и генерация
    prompt = make_prompt_text(q, ctx, CFG)
    try:
        raw = LLM.complete(prompt)
    except Exception as e:
        return AskOut(answer=f"ошибка генерации: {e}", sources=[], debug={"reason": "llm_error"})

    # Санитация + проверка «основано на контексте»
    ans = sanitize_answer(raw, ctx)
    if ans != "нет информации" and not guard_accept(ans, ctx, CFG):
        ans = "нет информации"

    return AskOut(
        answer=ans,
        sources=sources if ans != "нет информации" else [],
        debug={"ctx_chars": len(ctx), "rerank": CFG.rerank_mode}
    )

# ---- Потоковый ответ от LLM (Server-Sent Events) ----
@app.get("/ask/stream")
def ask_stream(q: str = Query(..., description="Вопрос")):
    q = (q or "").strip()
    if not q:
        # Отдаём «нет информации» как поток для единообразия клиента
        def gen_empty():
            yield sse_event({"token": "нет информации", "done": False})
            yield sse_event({"done": True, "sources": [], "answer": "нет информации"})
        return StreamingResponse(gen_empty(), media_type="text/event-stream", headers={
            "Cache-Control": "no-cache", "Connection": "keep-alive", "X-Accel-Buffering": "no",
        })

    # RAG: поиск контекста
    ctx, sources, _ = hybrid_retrieve(q, CFG)
    if not ctx:
        def gen_noctx():
            yield sse_event({"token": "нет информации", "done": False})
            yield sse_event({"done": True, "sources": [], "answer": "нет информации"})
        return StreamingResponse(gen_noctx(), media_type="text/event-stream", headers={
            "Cache-Control": "no-cache", "Connection": "keep-alive", "X-Accel-Buffering": "no",
        })

    prompt = make_prompt_text(q, ctx, CFG)

    def token_stream():
        acc = []
        try:
            for piece in LLM.stream(prompt):
                if piece:
                    acc.append(piece)
                    yield sse_event({"token": piece, "done": False})
        except Exception as e:
            final = f"ошибка генерации: {e}"
            yield sse_event({"token": f"[{final}]", "done": False})
            yield sse_event({"done": True, "sources": [], "answer": final})
            return

        raw_full = ("".join(acc)).strip()
        final_ans = sanitize_answer(raw_full, ctx)
        if final_ans != "нет информации" and not guard_accept(final_ans, ctx, CFG):
            final_ans = "нет информации"

        yield sse_event({
            "done": True,
            "sources": sources if final_ans != "нет информации" else [],
            "answer": final_ans,
        })

    return StreamingResponse(token_stream(), media_type="text/event-stream", headers={
        "Cache-Control": "no-cache", "Connection": "keep-alive", "X-Accel-Buffering": "no",
    })

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
