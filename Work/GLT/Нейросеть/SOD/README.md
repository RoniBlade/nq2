# RAG Hybrid Server (BM25 + E5/SBERT + MMR)

## Состав
- `rag_server.py` — FastAPI сервер (REST + SSE + простая UI-страница `/ui`)
- `rag_core.py` — ядро (векторизация, BM25, MMR-реранк, guard, промпт из `config.yaml`)
- `ingest.py` — офлайн-индексация папки документов
- `config.yaml` — все настройки (вместо ENV)
- `requirements.txt` — зависимости
- `documents/` — сюда класть `.txt`/`.md`

## Установка
```bash
python -m venv .venv
# Win:
.venv\Scripts\activate
# Linux/Mac:
source .venv/bin/activate

pip install -r requirements.txt
```

## Индексация
Положите файлы в `documents/` и выполните:
```bash
python ingest.py --reset
```
(флаг `--reset` очищает старую базу и BM25-корпус)

## Запуск сервера
```bash
python rag_server.py
```
По умолчанию сервер на `http://localhost:8000`
- UI: `http://localhost:8000/ui`
- REST:
  - `POST /ingest` — { "folder": "documents", "reset": false }
  - `POST /chat` — { "question": "..." }
  - `GET /chat/stream?q=...` — SSE-стрим ответа

## Конфиг
Все параметры и промпт в `config.yaml`. Меняйте и перезапускайте сервер.

## Бэкенды LLM
- `llm_backend: "ollama"` — нужен запущенный Ollama (`ollama serve`), модель в `ollama_model`
- `llm_backend: "llama_cpp"` — нужен `llama-cpp-python` и GGUF в `llama_cpp_model_path`

## Примечания
- При смене эмбеддингов (`emb_backend`/`emb_model_name`) делайте `python ingest.py --reset`
- BM25-корпус хранится в `bm25_jsonl`
- Коллекция Chroma: `collection` + необязательный `collection_salt`
