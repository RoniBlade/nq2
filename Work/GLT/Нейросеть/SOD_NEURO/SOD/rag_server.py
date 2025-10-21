# -*- coding: utf-8 -*-
"""
RAG Hybrid Server — FastAPI + Chroma + BM25 + LLM (Ollama / LLaMA.cpp)
Запуск: python rag_server.py
"""
import logging
from pathlib import Path
from typing import List, Dict, Any, Generator

from fastapi import FastAPI, Body, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse, JSONResponse, HTMLResponse
from pydantic import BaseModel

from rag_core import (
    Cfg, make_llm_backend, ingest_folder,
    hybrid_retrieve, sanitize_answer, guard_accept,
    make_prompt_text, preprocess_question, sse_event
)

logging.basicConfig(level=logging.INFO, format="%(levelname)s | %(message)s")
log = logging.getLogger("rag.server")

CFG = Cfg.from_yaml("config.yaml")
LLM = make_llm_backend(CFG)

app = FastAPI(title="RAG Hybrid Server", version="4.1")
app.add_middleware(
    CORSMiddleware, allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"],
)

class ChatIn(BaseModel):
    question: str

class ChatOut(BaseModel):
    answer: str
    sources: List[str] = []
    debug: Dict[str, Any] = {}

@app.get("/")
def root():
    return {"ok": True, "config": {k:v for k,v in CFG.__dict__.items() if "prompt" not in k}}

@app.post("/ingest")
def api_ingest(folder: str = Body(default=None, embed=True),
               reset: bool = Body(default=False, embed=True)):
    res = ingest_folder(folder or CFG.corpus_dir, CFG, reset=reset)
    return res

@app.post("/chat", response_model=ChatOut)
def chat(payload: ChatIn):
    q = (payload.question or "").strip()
    if not q:
        return ChatOut(answer="нет информации", sources=[], debug={"reason":"empty"})
    eff_q, mode = preprocess_question(q)
    if mode == "SMALLTALK":
        return ChatOut(answer="Привет! Отвечаю по данным из твоей базы.", sources=[], debug={"smalltalk": True})

    ctx, sources, _ = hybrid_retrieve(eff_q, CFG)
    if not ctx:
        return ChatOut(answer="нет информации", sources=[], debug={"reason":"no_context"})

    prompt = make_prompt_text(eff_q, ctx, CFG)
    try:
        raw = LLM.complete(prompt)
    except Exception as e:
        return ChatOut(answer=f"ошибка генерации: {e}", sources=[], debug={"reason":"llm_error"})

    ans = sanitize_answer(raw, ctx)
    if ans != "нет информации" and not guard_accept(ans, ctx, CFG):
        ans = "нет информации"
    dbg = {"ctx_chars": len(ctx), "rerank": CFG.rerank_mode}
    return ChatOut(answer=ans, sources=sources if ans != "нет информации" else [], debug=dbg)

@app.get("/chat/stream")
def chat_stream(q: str = Query(..., description="Вопрос")):
    q = (q or "").strip()
    if not q:
        return JSONResponse({"answer": "нет информации", "sources": []}, status_code=200)

    eff_q, mode = preprocess_question(q)
    if mode == "SMALLTALK":
        def greet():
            final = "Привет! Отвечаю по данным из твоей базы."
            yield sse_event({"token": final, "done": False})
            yield sse_event({"done": True, "sources": [], "answer": final})
        return StreamingResponse(greet(), media_type="text/event-stream", headers={
            "Cache-Control": "no-cache", "Connection": "keep-alive", "X-Accel-Buffering": "no",
        })

    context, sources, _ = hybrid_retrieve(eff_q, CFG)
    if not context:
        def noinfo():
            yield sse_event({"token": "нет информации", "done": False})
            yield sse_event({"done": True, "sources": [], "answer": "нет информации"})
        return StreamingResponse(noinfo(), media_type="text/event-stream", headers={
            "Cache-Control": "no-cache", "Connection": "keep-alive", "X-Accel-Buffering": "no",
        })

    prompt = make_prompt_text(eff_q, context, CFG)

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
        final_ans = sanitize_answer(raw_full, context)
        if final_ans != "нет информации" and not guard_accept(final_ans, context, CFG):
            final_ans = "нет информации"

        yield sse_event({
            "done": True,
            "sources": sources if final_ans != "нет информации" else [],
            "answer": final_ans,
        })

    return StreamingResponse(token_stream(), media_type="text/event-stream", headers={
        "Cache-Control": "no-cache", "Connection": "keep-alive", "X-Accel-Buffering": "no",
    })

# ---------- Minimal UI (встроенная страница) ----------
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

    try { es = new EventSource(`/chat/stream?q=${encodeURIComponent(q)}`); }
    catch(e) { es = null; }

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

  form.addEventListener('submit', (e)=>{ e.preventDefault(); ask(); });
  input.addEventListener('keydown', (e)=>{ if(e.key === 'Enter'){ e.preventDefault(); ask(); } });
  clr.addEventListener('click', ()=>{ localStorage.removeItem('rag_history_v3'); history=[]; chat.innerHTML=''; });
  input.focus();
</script>
</body>
</html>
"""

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
