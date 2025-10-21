# -*- coding: utf-8 -*-
"""
RAG Hybrid Server — FastAPI + Chroma(DefaultEmbeddingFunction) + BM25 + LLM (Ollama / LLaMA.cpp)
Запуск: uvicorn rag_server:app --host 0.0.0.0 --port 8000 --log-level debug --reload
"""
import logging
from pathlib import Path
from typing import List, Dict, Any

from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, JSONResponse
from pydantic import BaseModel

from rag_core import (
    Cfg,
    make_llm,
    hybrid_retrieve,
    make_prompt_text,
    ingest_folder,  # используем существующий офлайн-ингест
)

# ======== APP / CFG / LLM ========

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

app = FastAPI(title="RAG Hybrid Server")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

CFG: Cfg = Cfg.from_yaml("config.yaml")
LLM = make_llm(CFG)

# ======== MODELS ========

class ChatIn(BaseModel):
    question: str

class ChatOut(BaseModel):
    answer: str
    sources: List[str] = []
    debug: Dict[str, Any] | None = None


# ======== HELPERS ========

def preprocess_question(t: str) -> tuple[str, str]:
    """микро-хелпер: приветствия и прочее — отдельно"""
    t = (t or "").strip()
    if t.lower() in {"привет", "здравствуй", "здравствуйте", "hey", "hi"}:
        return t, "SMALLTALK"
    return t, "NORMAL"


# ======== ROUTES ========
@app.get("/ui", response_class=HTMLResponse)
def ui_alias():
    return HTMLResponse(ui_page())

@app.get("/", response_class=HTMLResponse)
def root():
    return HTMLResponse(ui_page())

@app.post("/chat", response_model=ChatOut)
def chat(payload: ChatIn):
    q = (payload.question or "").strip()
    if not q:
        return ChatOut(answer="нет информации", sources=[], debug={"reason": "empty"})

    eff_q, mode = preprocess_question(q)
    if mode == "SMALLTALK":
        return ChatOut(answer="Привет! Отвечаю по данным из твоей базы.", sources=[])

    context, sources, _ = hybrid_retrieve(eff_q, CFG)
    if not context:
        return ChatOut(answer="нет информации", sources=[])

    prompt = make_prompt_text(eff_q, context, CFG)
    try:
        answer = LLM.complete(prompt)  # см. реализацию в rag_core.py
    except Exception as e:
        logging.exception("LLM error")
        return ChatOut(answer="ошибка генерации ответа", sources=sources, debug={"error": str(e)})

    return ChatOut(answer=(answer or "").strip() or "нет информации", sources=sources)

@app.post("/upload")
async def upload(files: List[UploadFile] = File(...)):
    """
    Принимает .txt/.md файлы, кладёт их в CFG.corpus_dir и запускает ingest_folder().
    Возвращает список сохранённых путей и число чанков после переиндексации.
    """
    dest = Path(CFG.corpus_dir)
    dest.mkdir(parents=True, exist_ok=True)

    saved: List[str] = []
    skipped: List[str] = []

    for f in files:
        name = (f.filename or "").strip()
        if not name:
            continue
        if not name.lower().endswith((".txt", ".md")):
            skipped.append(name)
            continue
        target = dest / name
        data = await f.read()
        target.write_bytes(data)
        saved.append(str(target))

    # Полный ре-ингест (BM25 пересобирается, в Chroma — upsert)
    total_chunks = ingest_folder(str(dest), CFG)

    return JSONResponse({"saved": saved, "skipped": skipped, "chunks": total_chunks})


# ======== UI (HTML + CSS + JS) ========

def ui_page() -> str:
    return """
<!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8"/>
  <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate"/>
  <meta http-equiv="Pragma" content="no-cache"/>
  <meta http-equiv="Expires" content="0"/>
  <title>RAG Hybrid</title>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <style>
    :root{--bg:#0f1115;--panel:#151823;--accent:#5b8cff;--text:#e8e8f0;--user:#1f2a44;--bot:#1c2130}
    *{box-sizing:border-box}
    body{margin:0;background:var(--bg);color:var(--text);font:16px/1.5 system-ui,Segoe UI,Roboto}
    .wrap{max-width:980px;margin:0 auto;padding:24px}
    .panel{background:var(--panel);border:1px solid #23283a;border-radius:16px;padding:16px;min-height:520px;display:flex;flex-direction:column}
    .chat{flex:1;overflow:auto;padding:8px 4px}
    .msg{margin:6px 0;padding:12px 14px;border-radius:12px;white-space:pre-wrap;max-width:78%}
    .user{background:#1f2a44;align-self:flex-end;border-top-right-radius:6px}
    .bot{background:#1c2130;align-self:flex-start;border-top-left-radius:6px}
    .row{display:flex;gap:8px;margin-top:12px}
    input[type=text]{flex:1;padding:12px 14px;border-radius:10px;border:1px solid #2a3045;background:#0f1320;color:#e8e8f0}
    button{padding:10px 12px;border-radius:10px;border:1px solid #2a3045;background:var(--accent);color:#fff;font-weight:600;cursor:pointer}
    button[disabled]{opacity:.6;cursor:not-allowed}
    .sources{margin-top:6px;font-size:13px;color:#b9c3d6}.sources code{background:#141826;padding:2px 6px;border-radius:6px}

    /* uploader */
    .uploader{display:flex;gap:8px;margin:10px 0;align-items:center;flex-wrap:wrap}
    .uploader input[type=file]{display:none}
    .uploader .pick{border:1px dashed #2a3045;padding:10px 12px;border-radius:10px;background:#0f1320;cursor:pointer}
    .uploader .drop{flex:1;min-height:44px;border:1px dashed #2a3045;border-radius:10px;padding:10px;display:flex;align-items:center;justify-content:center;opacity:.9}
    .uploader .drop.drag{outline:2px solid var(--accent);opacity:1}
    .small{font-size:12px;color:#aeb7cb}
  </style>
</head>
<body>
  <div class="wrap">
    <div class="panel">

      <!-- uploader -->
      <div class="uploader" id="uploader">
        <label class="pick" for="files">Выбрать файлы</label>
        <input id="files" type="file" multiple accept=".txt,.md"/>
        <div id="drop" class="drop">Перетащите .txt/.md сюда</div>
        <button id="upload" type="button" title="Загрузить выбранные файлы">Загрузить в базу</button>
        <span id="upmsg" class="small"></span>
      </div>

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

  // uploader refs
  const fileInput = document.getElementById('files');
  const dropZone  = document.getElementById('drop');
  const uploadBtn = document.getElementById('upload');
  const upMsg     = document.getElementById('upmsg');

  function el(tag, cls, text=''){ const d=document.createElement(tag); if(cls) d.className=cls; d.textContent=text; return d; }
  function addUser(text){ const d=el('div','msg user',text); chat.appendChild(d); chat.scrollTop=chat.scrollHeight; }
  function addBot(text){ const d=el('div','msg bot'); d.innerHTML='<div>'+text+'</div>'; chat.appendChild(d); chat.scrollTop=chat.scrollHeight; }
  function addSources(arr){ if(!arr||!arr.length) return; const d=el('div','sources'); d.innerHTML='Источники:<br>'+arr.map(x=>`<code>${x}</code>`).join('<br>'); chat.lastChild.appendChild(d); }

  // ========= Chat =========
  form.addEventListener('submit', async (e)=>{
    e.preventDefault();
    const q = input.value.trim();
    if(!q) return;
    addUser(q);
    input.value=''; btn.disabled=true;

    try{
      const r = await fetch('/chat', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({question:q})});
      const j = await r.json();
      addBot(j.answer||'нет информации');
      addSources(j.sources||[]);
    }catch(e2){
      addBot('ошибка: '+e2);
    }
    btn.disabled=false;
  });

  clr.addEventListener('click', ()=>{ chat.innerHTML=''; });
  input.focus();

  // ========= Uploader =========
  function setMsg(t){ upMsg.textContent = t; }
  function filesFromDataTransfer(dt){ return Array.from(dt.files || []); }
  function addFilesToInput(newFiles){
    const data = new DataTransfer();
    Array.from(fileInput.files||[]).forEach(f=>data.items.add(f));
    newFiles.forEach(f=>data.items.add(f));
    fileInput.files = data.files;
  }

  // клик по "Выбрать файлы"
  document.querySelector('label[for="files"]').addEventListener('click', ()=> fileInput.click());

  // drag & drop
  ;['dragenter','dragover'].forEach(ev=>dropZone.addEventListener(ev, e=>{ e.preventDefault(); e.stopPropagation(); dropZone.classList.add('drag'); }));
  ;['dragleave','drop'].forEach(ev=>dropZone.addEventListener(ev, e=>{ e.preventDefault(); e.stopPropagation(); dropZone.classList.remove('drag'); }));
  dropZone.addEventListener('drop', e=>{
    const fs = filesFromDataTransfer(e.dataTransfer).filter(f=>/\\.(txt|md)$/i.test(f.name));
    if(fs.length){ addFilesToInput(fs); setMsg(`Выбрано файлов: ${fileInput.files.length}`); }
  });
  fileInput.addEventListener('change', ()=> setMsg(`Выбрано файлов: ${fileInput.files.length}`));

  // отправка на /upload
  uploadBtn.addEventListener('click', async ()=>{
    const fs = Array.from(fileInput.files||[]);
    if(!fs.length){ setMsg('Сначала выберите .txt/.md'); return; }

    uploadBtn.disabled = true; setMsg('Загрузка и индексация…');

    const fd = new FormData();
    fs.forEach(f=>fd.append('files', f, f.name));

    try{
      const r = await fetch('/upload', { method:'POST', body: fd });
      if(!r.ok) throw new Error(`HTTP ${r.status}`);
      const j = await r.json();
      setMsg(`Готово: файлов ${j.saved?.length||0}, пропущено ${j.skipped?.length||0}, чанков в индексе: ${j.chunks||0}`);
      addBot(`✔ Документы добавлены. Чанков в индексе: ${j.chunks||0}`);
      if((j.skipped||[]).length){ addBot('Пропущены (не .txt/.md): ' + j.skipped.join(', ')); }
    }catch(err){
      setMsg('Ошибка загрузки/ингеста'); addBot('ошибка: '+err);
    }finally{
      uploadBtn.disabled = false;
    }
  });
</script>
</body>
</html>
"""
