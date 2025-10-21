# ai_validator.py
import json, re, time
from ollama import Client

SPEC = """
Ты — валидатор. Вход — JSON с полями: columns[], sample_rows[], rows_total, cols_total.
Верни РОВНО ОДИН JSON-конфиг:
{
  "columns": { "department": <str|null>, "position": <str|null>, "employment": <str|null>, "resource": <str|null>, "roles": <str|null>, "user": <str|null>, "business_role": <str|null> },
  "roles_parse": { "delimiters": [",",";","|"], "normalize_case": "title", "deduplicate": true, "sort_alpha": true },
  "business_role_rule": { "type": "use_column"|"generate", "template": "{position}_{resource}_{department}", "separator": "_" },
  "output_headers": { "department": "Департамент", "position": "Должность", "employment": "Занятость", "business_role": "Бизнес-роль", "resource": "Название ресурса", "role": "Роль" },
  "validation": { "errors": [], "warnings": [], "summary": { "rows_seen": <int>, "columns_count": <int> } }
}
Требования: опирайся ТОЛЬКО на вход; если колонка ролей не найдена — добавь ошибку MISSING_ROLES.
Входные данные:
"""

def _extract_json(text: str):
    try: return json.loads(text)
    except: pass
    m = re.search(r"```json\s*(\{.*?\})\s*```", text, flags=re.S|re.I)
    if m:
        try: return json.loads(m.group(1))
        except: pass
    for cand in sorted(re.findall(r"(\{.*\})", text, flags=re.S), key=len, reverse=True):
        try: return json.loads(cand)
        except: continue
    return None

def ask_llm(host: str, model: str, analysis_json: str, timeout_s: int = 60, stream: bool = False):
    client = Client(host=host)
    prompt = SPEC + analysis_json

    print("\n====[LLM PROMPT]====\n", prompt, "\n====[/PROMPT]====\n", sep="")
    raw = ""

    if stream:
        t0 = time.time()
        try:
            chunks = []
            for part in client.chat(model=model, messages=[{"role":"user","content":prompt}], stream=True):
                piece = part.get("message",{}).get("content","") or part.get("content","")
                if piece: chunks.append(piece)
                if time.time()-t0 > timeout_s: break
            raw = "".join(chunks).strip()
        except Exception:
            raw = ""

    if not raw:  # стабильный non-stream
        try:
            resp = client.chat(model=model, messages=[{"role":"user","content":prompt}], stream=False)
            raw = (resp.get("message",{}) or {}).get("content","") or resp.get("content","") or json.dumps(resp, ensure_ascii=False)
        except Exception:
            raw = ""

    print("\n====[LLM RAW]====\n", (raw or "<пусто>"), "\n====[/RAW]====\n", sep="")
    return _extract_json(raw), raw
