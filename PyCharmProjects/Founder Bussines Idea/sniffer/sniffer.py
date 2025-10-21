# sniffer/network_tap.py
# NetworkTap — Playwright сниффер с поддержкой Google batchexecute (в т.ч. байтовый парсер) + глубокий трейс.
# v2025-09-07

import os, re, json, hashlib, time, sqlite3, gzip, io, urllib.parse
from dataclasses import dataclass
from typing import Optional, List, Dict, Any, Callable, Tuple

print("[NetworkTap] version=2025-09-07 debug+robust-batchextract+deep-logging+bytes-splitter")

# ------------------ utils ------------------

XSSI_PREFIXES = (")]}',", ")]}'")
XSSI_PREFIXES_BYTES = tuple(p.encode("ascii") for p in XSSI_PREFIXES)

def _strip_xssi(text: str) -> str:
    for p in XSSI_PREFIXES:
        if text.startswith(p):
            text = text[len(p):]
            break
    return text.lstrip(", \r\n\t")

def _strip_xssi_bytes(b: bytes) -> bytes:
    for p in XSSI_PREFIXES_BYTES:
        if b.startswith(p):
            b = b[len(p):]
            break
    return b.lstrip(b", \r\n\t")

def _sha1_bytes(b: bytes) -> str:
    return hashlib.sha1(b).hexdigest()

def _sha1_str(s: str) -> str:
    return hashlib.sha1(s.encode("utf-8", "ignore")).hexdigest()

def _now() -> int:
    return int(time.time())

def _safe_host(url: str) -> str:
    host = urllib.parse.urlsplit(url).netloc
    return re.sub(r"[^a-zA-Z0-9_.-]", "_", host)

def _parse_content_disposition_filename(h: str) -> Optional[str]:
    if not h:
        return None
    m = re.search(r'filename\*?=(?:UTF-8\'\')?"?([^";]+)"?', h)
    return urllib.parse.unquote(m.group(1)) if m else None

def shlex_quote(s: str) -> str:
    if not s:
        return "''"
    if re.fullmatch(r"[A-Za-z0-9_@%+=:,./-]+", s):
        return s
    return "'" + s.replace("'", "'\"'\"'") + "'"

def _val_kind_and_size(v) -> Tuple[str, Optional[int]]:
    try:
        if isinstance(v, dict): return "dict", len(v)
        if isinstance(v, list): return "list", len(v)
        if isinstance(v, str):  return "str",  len(v)
        if isinstance(v, bytes):return "bytes", len(v)
        if v is None:           return "null", 0
        if isinstance(v, (int, float, bool)): return type(v).__name__, None
        return type(v).__name__, None
    except Exception:
        return type(v).__name__, None

def _short_preview(v, max_len: int) -> str:
    try:
        if isinstance(v, (dict, list)):
            s = json.dumps(v, ensure_ascii=False)
        else:
            s = str(v)
        if len(s) > max_len:
            return s[:max_len] + "…(cut)"
        return s
    except Exception:
        t = type(v).__name__
        return f"<{t} preview error>"

# ---- batchexecute helpers ----

def _json_loads_loose(s: Any):
    """Попробовать распарсить JSON 1-2 раза, если это строка с JSON внутри строки."""
    if isinstance(s, (dict, list)):
        return s
    if isinstance(s, bytes):
        try:
            s = s.decode("utf-8", "ignore")
        except Exception:
            return s
    if not isinstance(s, str):
        return s
    try:
        x = json.loads(s)
        if isinstance(x, str):
            try:
                return json.loads(x)
            except Exception:
                return x
        return x
    except Exception:
        return s

def _batchexecute_parts(text: str):
    """
    Разбирает тело вида:
      )]}'\n
      <len>\n<json_chunk>\n (повторяется)
    + fallback на «целиком один JSON/текст».
    Возвращает список объектов (list/dict/str).
    """
    s = _strip_xssi(text)
    i, n, out = 0, len(s), []
    while i < n:
        while i < n and s[i] in " \t\r\n":
            i += 1
        if i >= n:
            break
        j = i
        while j < n and s[j].isdigit():
            j += 1
        if j > i and j < n and s[j] == "\n":
            ln = int(s[i:j])
            start = j + 1
            end = start + ln
            chunk = s[start:end]
            i = end + (1 if end < n and s[end] == "\n" else 0)
            out.append(_json_loads_loose(chunk))
            continue
        # fallback: остаток — как один JSON / текст
        try:
            out.append(_json_loads_loose(s[i:]))
        except Exception:
            out.append(s[i:])
        break
    return out

def _batchexecute_parts_bytes(body: bytes):
    """
    То же, что _batchexecute_parts, но работает с bytes и НЕ требует предварительной декодировки.
    """
    s = _strip_xssi_bytes(body)
    i, n, out = 0, len(s), []
    while i < n:
        # пропускаем пустые/пробельные байты
        while i < n and s[i] in b" \t\r\n":
            i += 1
        if i >= n:
            break
        # читаем число длины до \n
        j = i
        while j < n and 48 <= s[j] <= 57:  # ASCII digits
            j += 1
        if j > i and j < n and s[j] == 0x0A:  # '\n'
            try:
                ln = int(s[i:j].decode("ascii", "ignore") or "0")
            except Exception:
                ln = 0
            start = j + 1
            end = start + ln
            chunk = s[start:end]
            i = end + (1 if end < n and s[end] == 0x0A else 0)
            out.append(_json_loads_loose(chunk))
            continue
        # fallback: остаток как один JSON/текст
        try:
            out.append(_json_loads_loose(s[i:]))
        except Exception:
            out.append(s[i:].decode("utf-8", "ignore"))
        break
    return out

def _batchextract_payloads(parts, allowed_rpcids=None, debug_cb: Optional[Callable[[Dict[str, Any]], None]] = None, robust: bool = True):
    """
    Собирает записи вида [<tag:str>, <rpcid:str>, <payload>, ...]
    - Не привязан к конкретному тегу (wrb.fr/di/...).
    - Если задан allowed_rpcids (set/list) — фильтрует по нему.
    - robust=True: ищет payload не только в node[2], но и в нескольких последующих слотах.
    """
    res = []

    def walk(node):
        # Классический шаблон: ["wrb.fr"/"di"/..., "<rpcid>", <payload>, ...]
        if isinstance(node, list) and len(node) >= 3 and isinstance(node[0], str) and isinstance(node[1], str):
            rpcid = node[1]
            chosen_idx = None
            cand = None
            slots = node[2:6] if robust else node[2:3]
            for k, sl in enumerate(slots, start=2):
                pl = _json_loads_loose(sl)
                if pl is not None:
                    cand = pl
                    chosen_idx = k
                    break
            if (allowed_rpcids is None) or (rpcid in allowed_rpcids):
                res.append({"rpcid": rpcid, "payload": cand})
                if debug_cb:
                    kind, size = _val_kind_and_size(cand)
                    debug_cb(dict(event="payload_append", rpcid=rpcid, slot_index=chosen_idx, size=size, kind=kind))
            else:
                if debug_cb:
                    debug_cb(dict(event="payload_skip_rpcid", rpcid=rpcid))
        if isinstance(node, list):
            for ch in node:
                walk(ch)
        elif isinstance(node, dict):
            for ch in node.values():
                walk(ch)

    for p in parts:
        walk(p)
    if debug_cb:
        debug_cb(dict(event="payload_done", total=len(res)))
    return res

# ------------------ data ------------------

@dataclass
class TapRule:
    # Контент-фильтры
    allow_ctypes: Optional[List[str]] = None           # ["application/json","text/plain",...]
    allow_resource_types: Optional[List[str]] = None   # ["xhr","fetch","document","websocket"]
    allow_url: Optional[re.Pattern] = None
    deny_url: Optional[re.Pattern] = None
    # Лимиты
    max_bytes: Optional[int] = 2_000_000              # None -> без лимита
    body_timeout_ms: int = 10_000
    skip_sse: bool = True
    save_binary: bool = False
    gzip_text: bool = True
    # Метаданные запроса
    save_request_body: bool = True
    request_body_max: int = 2048
    # Допы
    decode_batchexecute: bool = True
    index_path: Optional[str] = os.path.join("tap_out", "_index.jsonl")  # глобальный индекс
    emit_curl: bool = True  # сохранить curl-рецепт повтора
    # Debug / trace
    debug: bool = False
    log_dir: Optional[str] = os.path.join("tap_out", "_log")
    keep_raw: bool = True                 # сохранять .raw.gz
    keep_decoded_text: bool = True        # сохранять .decoded.txt.gz
    trace_batchexecute: bool = True       # лог частей/извлечения
    robust_batchextract: bool = True      # искать payload не только в node[2]
    ignore_rpcids_filter: bool = False    # не фильтровать по ?rpcids=...
    # формат логов
    log_preview_len: int = 200            # длина превью строк/JSON в логах
    log_preview_payloads: int = 12        # сколько payload’ов показывать в превью
    log_save_decisions: bool = True       # логировать причины выбора формата сохранения

@dataclass
class TapItem:
    url: str
    method: str
    resource_type: Optional[str]
    status: int
    content_type: str
    content_length: Optional[int]
    saved_path: Optional[str]
    meta_path: Optional[str]
    is_json: bool
    is_text: bool
    content_hash: str
    headers: Dict[str, str]
    request_headers: Dict[str, str]
    request_body_saved: bool
    snippet: Optional[str] = None
    json_top_keys: Optional[List[str]] = None

class _DedupStore:
    """Персистентный дедуп по content_hash (sqlite)."""
    def __init__(self, db_path: Optional[str]):
        self.db_path = db_path
        self.mem = set()
        self._conn = None
        if db_path:
            os.makedirs(os.path.dirname(db_path), exist_ok=True)
            self._conn = sqlite3.connect(db_path, check_same_thread=False)
            self._conn.execute("CREATE TABLE IF NOT EXISTS seen (hash TEXT PRIMARY KEY)")
            self._conn.commit()
    def seen(self, h: str) -> bool:
        if h in self.mem:
            return True
        if self._conn:
            cur = self._conn.execute("SELECT 1 FROM seen WHERE hash=?", (h,))
            row = cur.fetchone()
            if row:
                self.mem.add(h)
                return True
        return False
    def add(self, h: str) -> None:
        self.mem.add(h)
        if self._conn:
            try:
                self._conn.execute("INSERT OR IGNORE INTO seen(hash) VALUES(?)", (h,))
                self._conn.commit()
            except Exception:
                pass

class _NullDedupStore:
    """Полное отключение дедупликации."""
    def seen(self, h: str) -> bool: return False
    def add(self, h: str) -> None:  pass

# ------------------ tap ------------------

class NetworkTap:
    """
    Пассивный сниффер Playwright Context/Page.
    - Лог в tap_out/_log/tap.ndjson при rule.debug=True.
    - Для снятия ограничений можно:
        * rule.allow_ctypes=None, rule.allow_resource_types=None
        * rule.max_bytes=None, rule.save_binary=True, rule.skip_sse=False
        * rule.ignore_rpcids_filter=True (batchexecute)
        * dedup_db="off" (полностью отключить дедуп)
    """
    def __init__(
        self,
        out_dir: str = "tap_out",
        rule: Optional[TapRule] = None,
        on_item: Optional[Callable[[TapItem], None]] = None,
        dedup_db: Optional[str] = None,
    ):
        self.out_dir = out_dir
        os.makedirs(out_dir, exist_ok=True)
        self.rule = rule or TapRule(
            allow_ctypes=["application/json","text/plain","application/ld+json","application/json+protobuf"],
            allow_resource_types=["xhr","fetch","document","websocket"]
        )
        self.items: List[TapItem] = []
        self._on_item = on_item
        self._handlers: Dict[Any, Tuple] = {}      # page -> (handlers)
        self._ctx_handlers: Dict[Any, Tuple] = {}  # context -> (handler,)
        # дедуп
        self._dedup = _NullDedupStore() if dedup_db == "off" else _DedupStore(dedup_db)
        if self.rule.deny_url is None:
            self.rule.deny_url = re.compile(r"api\.ipify\.org")
        # лог
        self._log_path = None
        if self.rule.log_dir:
            os.makedirs(self.rule.log_dir, exist_ok=True)
            self._log_path = os.path.join(self.rule.log_dir, "tap.ndjson")

    def _dbg(self, **kv):
        """Структурный лог одной строкой JSON (append)."""
        if not self.rule.debug:
            return
        try:
            kv.setdefault("ts", _now())
            line = json.dumps(kv, ensure_ascii=False)
        except Exception:
            line = str(kv)
        if self._log_path:
            try:
                with open(self._log_path, "a", encoding="utf-8") as f:
                    f.write(line + "\n")
            except Exception:
                pass
        else:
            print("[tapDBG]", line)

    # ---------- public API ----------

    def attach_to_context(self, context, include_existing_pages: bool = True) -> None:
        def on_new_page(page):
            try: self.attach_to_page(page)
            except Exception as e:
                self._dbg(stage="attach_to_page_error", error=str(e))
                print(f"[tap] attach_to_page error: {e}")
        context.on("page", on_new_page)
        self._ctx_handlers[context] = (on_new_page,)
        if include_existing_pages:
            for p in context.pages:
                self.attach_to_page(p)

    def detach_from_context(self, context) -> None:
        h = self._ctx_handlers.pop(context, None)
        if h:
            on_new_page, = h
            try: context.off("page", on_new_page)
            except Exception: pass
        for p in list(self._handlers.keys()):
            try:
                if p.context == context:
                    self.detach_from_page(p)
            except Exception:
                pass

    def attach_to_page(self, page) -> None:
        """Вешать ДО goto()."""
        def on_response(resp):    self._handle_response(resp)
        def on_ws(ws):            self._handle_websocket(ws)
        def on_req_failed(req):   self._handle_request_failed(req)
        def on_req_finished(req): self._handle_request_finished(req)
        page.on("response", on_response)
        page.on("websocket", on_ws)
        page.on("requestfailed", on_req_failed)
        page.on("requestfinished", on_req_finished)
        self._handlers[page] = (on_response, on_ws, on_req_failed, on_req_finished)
        self._dbg(stage="attach_to_page_ok")

    def detach_from_page(self, page) -> None:
        h = self._handlers.pop(page, None)
        if not h: return
        on_response, on_ws, on_req_failed, on_req_finished = h
        for evt, fn in (("response", on_response),
                        ("websocket", on_ws),
                        ("requestfailed", on_req_failed),
                        ("requestfinished", on_req_finished)):
            try: page.off(evt, fn)
            except Exception: pass
        self._dbg(stage="detach_from_page_ok")

    # ---------- internals ----------

    def _match(self, url: str, ctype: str, rtype: Optional[str]) -> bool:
        if self.rule.allow_resource_types and rtype and rtype not in self.rule.allow_resource_types:
            self._dbg(stage="filter_rtype", url=url, rtype=rtype)
            return False
        if self.rule.allow_ctypes and ctype and not any(ct in ctype for ct in self.rule.allow_ctypes):
            # допускаем пустой ctype для XHR без заголовка
            if ctype:
                self._dbg(stage="filter_ctype", url=url, ctype=ctype)
                return False
        if self.rule.allow_url and not self.rule.allow_url.search(url):
            self._dbg(stage="filter_allow_url", url=url)
            return False
        if self.rule.deny_url and self.rule.deny_url.search(url):
            self._dbg(stage="filter_deny_url", url=url)
            return False
        return True

    def _io_write(self, path: str, data: bytes, gzip_text: bool) -> str:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        if gzip_text:
            path = path + ".gz"
            with gzip.open(path, "wb") as f:
                f.write(data)
        else:
            with open(path, "wb") as f:
                f.write(data)
        return path

    def _save_textlike(self, host: str, url: str, text: str, is_json: bool) -> Tuple[str, str]:
        sig_src = (url + text[:256]).encode()
        sig = _sha1_bytes(sig_src)
        folder = os.path.join(self.out_dir, host); os.makedirs(folder, exist_ok=True)
        ext = ".json" if is_json else ".txt"
        fpath = os.path.join(folder, f"{sig}{ext}")
        if is_json:
            try:
                payload = json.dumps(json.loads(text), ensure_ascii=False, indent=2).encode("utf-8")
            except Exception:
                payload = text.encode("utf-8", "ignore")
        else:
            payload = text.encode("utf-8", "ignore")
        saved = self._io_write(fpath, payload, gzip_text=self.rule.gzip_text)
        return saved, sig

    def _save_json_obj(self, host: str, url: str, obj: Any, suffix=".json") -> Tuple[str, str]:
        raw = json.dumps(obj, ensure_ascii=False, indent=2).encode("utf-8")
        sig_src = url.encode() + raw[:256]
        sig = _sha1_bytes(sig_src)
        folder = os.path.join(self.out_dir, host); os.makedirs(folder, exist_ok=True)
        fpath = os.path.join(folder, f"{sig}{suffix}")
        saved = self._io_write(fpath, raw, gzip_text=self.rule.gzip_text)
        return saved, sig

    def _save_binary(self, host: str, url: str, body: bytes) -> Tuple[str, str]:
        sig_src = url.encode() + body[:256]
        sig = _sha1_bytes(sig_src)
        folder = os.path.join(self.out_dir, host); os.makedirs(folder, exist_ok=True)
        fpath = os.path.join(folder, f"{sig}.bin")
        saved = self._io_write(fpath, body, gzip_text=False)
        return saved, sig

    def _save_meta(self, host: str, sig: str, meta: Dict[str, Any]) -> str:
        folder = os.path.join(self.out_dir, host); os.makedirs(folder, exist_ok=True)
        mpath = os.path.join(folder, f"{sig}.meta.json")
        with open(mpath, "w", encoding="utf-8") as f:
            json.dump(meta, f, ensure_ascii=False, indent=2)
        # глобальный индекс
        if self.rule.index_path:
            try:
                os.makedirs(os.path.dirname(self.rule.index_path), exist_ok=True)
                with open(self.rule.index_path, "a", encoding="utf-8") as idx:
                    slim = {
                        "ts": meta.get("captured_at"),
                        "host": _safe_host(meta["url"]),
                        "url": meta["url"],
                        "path": urllib.parse.urlsplit(meta["url"]).path,
                        "status": meta["status"],
                        "ctype": meta["content_type"],
                        "rtype": meta["resource_type"],
                        "saved": meta["saved_path"],
                        "meta": mpath,
                        "json": meta["is_json"],
                        "text": meta["is_text"],
                        "len": meta["content_length"],
                        "hash": meta["content_hash"],
                    }
                    idx.write(json.dumps(slim, ensure_ascii=False) + "\n")
            except Exception:
                pass
        return mpath

    # ---- events ----

    def _handle_websocket(self, ws) -> None:
        # Можно включить сбор небольших фреймов при необходимости
        self._dbg(stage="ws_attach")

    def _handle_request_failed(self, req) -> None:
        try:
            failure = None
            try:
                failure = getattr(req, "failure", None)
                if callable(failure):
                    failure = failure()
            except Exception:
                pass
            meta = {
                "event": "requestfailed",
                "url": req.url,
                "method": req.method,
                "resource_type": self._get_resource_type(req),
                "failure": failure or {},
                "when": _now(),
            }
            folder = os.path.join(self.out_dir, _safe_host(req.url), "_events")
            os.makedirs(folder, exist_ok=True)
            fname = os.path.join(folder, f"{_sha1_str(req.url + str(time.time()))[:12]}_failed.json")
            with open(fname, "w", encoding="utf-8") as f:
                json.dump(meta, f, ensure_ascii=False, indent=2)
            self._dbg(stage="request_failed", url=req.url, failure=failure)
        except Exception:
            pass

    def _handle_request_finished(self, req) -> None:
        try:
            meta = {
                "event": "requestfinished",
                "url": req.url,
                "method": req.method,
                "resource_type": self._get_resource_type(req),
                "when": _now(),
            }
            folder = os.path.join(self.out_dir, _safe_host(req.url), "_events")
            os.makedirs(folder, exist_ok=True)
            fname = os.path.join(folder, f"{_sha1_str(req.url + str(time.time()))[:12]}_finished.json")
            with open(fname, "w", encoding="utf-8") as f:
                json.dump(meta, f, ensure_ascii=False, indent=2)
            self._dbg(stage="request_finished", url=req.url)
        except Exception:
            pass

    def _get_resource_type(self, req) -> Optional[str]:
        try:
            r = getattr(req, "resource_type", None)
            if callable(r):
                r = r()
            if isinstance(r, str):
                return r
        except Exception:
            pass
        return None

    def _build_replay_recipe(self, url: str, method: str, req_headers: Dict[str, str], body_snippet: Optional[str]) -> Dict[str, Any]:
        deny = {"cookie", "authorization", "content-length"}
        headers = {k: v for k, v in (req_headers or {}).items() if k.lower() not in deny}
        parts = [f"curl {shlex_quote(url)}", "-i", f"-X {method}"]
        for k, v in headers.items():
            parts.append(f"-H {shlex_quote(f'{k}: {v}')}")
        if body_snippet and not body_snippet.endswith("(truncated)"):
            parts.append(f"--data-raw {shlex_quote(body_snippet)}")
        curl = " \\\n  ".join(parts)
        return {"headers": headers, "curl": curl}

    def _handle_response(self, resp) -> None:
        try:
            req = resp.request
            url   = resp.url
            method = req.method
            rtype  = self._get_resource_type(req)
            status = resp.status

            # headers / request headers
            try: headers = dict(resp.headers)
            except Exception: headers = {}
            try:
                req_headers = getattr(req, "headers", {}) or {}
                if not isinstance(req_headers, dict):
                    req_headers = dict(req_headers)
            except Exception:
                req_headers = {}

            # content headers
            ctype = headers.get("content-type", "")
            clen = headers.get("content-length")
            clen_int = int(clen) if (clen and str(clen).isdigit()) else None

            if not self._match(url, ctype, rtype):
                return

            # SSE guard
            if self.rule.skip_sse and "text/event-stream" in (ctype or ""):
                sig = _sha1_str(url + str(status) + (ctype or ""))
                meta = self._build_meta(url, method, rtype, status, headers, req_headers,
                                        clen_int, None, None, ctype, False, False, None, req)
                self._save_meta(_safe_host(url), sig, meta)
                self._dbg(stage="skip_sse", url=url)
                return

            if clen_int and self.rule.max_bytes and clen_int > self.rule.max_bytes:
                self._dbg(stage="skip_too_large_declared", url=url, decl=clen_int, max=self.rule.max_bytes)
                return

            try:
                body = resp.body()
            except Exception as e:
                self._dbg(stage="body_read_error", url=url, error=str(e))
                return
            if not body or (self.rule.max_bytes and len(body) > self.rule.max_bytes):
                self._dbg(stage="skip_too_large", url=url, size=(len(body) if body else 0), max=self.rule.max_bytes)
                return

            content_hash = _sha1_bytes(body)
            host = _safe_host(url)
            sig_for_debug = _sha1_bytes((url + str(status) + (ctype or "")).encode() + body[:64])

            # сырое тело до любых преобразований
            if self.rule.keep_raw:
                try:
                    raw_dir = os.path.join(self.out_dir, host)
                    os.makedirs(raw_dir, exist_ok=True)
                    with gzip.open(os.path.join(raw_dir, f"{sig_for_debug}.raw.gz"), "wb") as f:
                        f.write(body)
                except Exception:
                    pass
            self._dbg(stage="read_body", url=url, status=status, ctype=ctype, rtype=rtype,
                      body_len=len(body), hash=content_hash, sig=sig_for_debug)

            if self._dedup.seen(content_hash):
                self._dbg(stage="skip_dedup", url=url, hash=content_hash)
                return
            self._dedup.add(content_hash)

            is_json = is_text = False
            text = None
            top_keys = None
            saved = None
            sig = None
            extra_meta: Dict[str, Any] = {}
            save_reason = None

            # 1) JSON / TEXT
            if ("application/json" in (ctype or "")) or ("text/plain" in (ctype or "")) or ("application/ld+json" in (ctype or "")) or ("application/json+protobuf" in (ctype or "")):
                # спец: Trends batchexecute — сначала парсим СЫРЫЕ БАЙТЫ
                if self.rule.decode_batchexecute and "/_/TrendsUi/data/batchexecute" in url:
                    urlp = urllib.parse.urlsplit(url)
                    q = urllib.parse.parse_qs(urlp.query)
                    allowed_rpcids = None
                    if (not self.rule.ignore_rpcids_filter) and "rpcids" in q and q["rpcids"]:
                        allowed_rpcids = set(",".join(q["rpcids"]).split(","))
                    self._dbg(stage="batchex_begin", url=url,
                              allowed_rpcids=sorted(list(allowed_rpcids)) if allowed_rpcids else [])

                    # сплитим всегда
                    sig_for_split = _sha1_bytes(url.encode() + body[:256])
                    base_folder = os.path.join(self.out_dir, host)
                    split_dir = os.path.join(base_folder, f"{sig_for_split}.split")
                    os.makedirs(split_dir, exist_ok=True)

                    parts = _batchexecute_parts_bytes(body)

                    # статистика частей
                    kinds_count: Dict[str,int] = {}
                    for part in parts:
                        kind, _ = _val_kind_and_size(part)
                        kinds_count[kind] = kinds_count.get(kind, 0) + 1

                    if self.rule.trace_batchexecute:
                        preview = []
                        for idx, part in enumerate(parts):
                            kind, sz = _val_kind_and_size(part)
                            head = _short_preview(part, self.rule.log_preview_len)
                            preview.append({"i": idx, "kind": kind, "size": sz, "head": head})
                        self._dbg(stage="batchex_parts",
                                  url=url, count=len(parts), kinds=kinds_count, preview=preview)

                    # сохраняем разваленные части
                    for idx, part in enumerate(parts):
                        p = os.path.join(split_dir, f"{idx:03d}.json" if isinstance(part, (dict, list)) else f"{idx:03d}.txt")
                        with open(p, "w", encoding="utf-8") as f:
                            if isinstance(part, (dict, list)):
                                json.dump(part, f, ensure_ascii=False, indent=2)
                            else:
                                f.write(str(part))

                    # извлекаем полезную нагрузку
                    payloads = _batchextract_payloads(
                        parts,
                        allowed_rpcids=allowed_rpcids,
                        debug_cb=(lambda ev: self._dbg(stage="batchex_extract", url=url, **ev)) if self.rule.trace_batchexecute else None,
                        robust=self.rule.robust_batchextract
                    )

                    # статистика по payload’ам
                    p_kind_count: Dict[str,int] = {}
                    p_rpc_count: Dict[str,int] = {}
                    p_preview = []
                    for i, pl in enumerate(payloads[: self.rule.log_preview_payloads]):
                        rpc = pl.get("rpcid")
                        val = pl.get("payload")
                        kind, size = _val_kind_and_size(val)
                        p_kind_count[kind] = p_kind_count.get(kind, 0) + 1
                        if rpc: p_rpc_count[rpc] = p_rpc_count.get(rpc, 0) + 1
                        p_preview.append({
                            "i": i,
                            "rpcid": rpc,
                            "kind": kind,
                            "size": size,
                            "head": _short_preview(val, self.rule.log_preview_len)
                        })
                    extra_meta["batchexecute"] = {
                        "rpcids": sorted(list(allowed_rpcids)) if allowed_rpcids else [],
                        "parts_dir": split_dir
                    }
                    self._dbg(stage="batchex_summary",
                              url=url,
                              parts_count=len(parts), parts_kinds=kinds_count,
                              payloads_count=len(payloads),
                              payloads_kinds=p_kind_count,
                              payloads_rpcids=p_rpc_count,
                              payloads_preview=p_preview)

                    if payloads:
                        saved, sig = self._save_json_obj(host, url, {"batchexecute": payloads})
                        is_json, is_text = True, False
                        top_keys = ["batchexecute"]
                        save_reason = "batchextract:payloads>0"
                    else:
                        # нет payload'ов — сохраняем сырой текст без XSSI
                        decoded_local = body.decode("utf-8", "ignore")
                        text = _strip_xssi(decoded_local)
                        saved, sig = self._save_textlike(host, url, text, False)
                        is_json, is_text = False, True
                        top_keys = None
                        save_reason = "batchextract:empty"

                # обычный JSON / текст (если ветка batchexecute не сохранила)
                if not saved:
                    decoded = body.decode("utf-8", "ignore")
                    # при необходимости — сохранить расшифровку для отладки
                    if self.rule.keep_decoded_text:
                        try:
                            with gzip.open(os.path.join(self.out_dir, host, f"{sig_for_debug}.decoded.txt.gz"), "wb") as f:
                                f.write(decoded.encode("utf-8", "ignore"))
                        except Exception:
                            pass
                    text = _strip_xssi(decoded)
                    try:
                        obj = json.loads(text)
                        is_json = True
                        if isinstance(obj, dict):
                            top_keys = list(obj.keys())[:32]
                        elif isinstance(obj, list) and obj and isinstance(obj[0], dict):
                            top_keys = list(obj[0].keys())[:32]
                        saved, sig = self._save_textlike(host, url, text, True)
                        save_reason = "plain:valid_json"
                    except Exception:
                        is_text = True
                        saved, sig = self._save_textlike(host, url, text, False)
                        save_reason = "plain:non_json_text"

            # 2) бинарь
            if not saved:
                if not self.rule.save_binary:
                    self._dbg(stage="skip_binary_disabled", url=url)
                    return
                saved, sig = self._save_binary(host, url, body)
                save_reason = "binary:pass_through"

            # META
            meta = self._build_meta(
                url, method, rtype, status, headers, req_headers,
                clen_int, saved, content_hash, ctype, is_json, is_text, top_keys, req
            )
            # полезные поля URL
            urlp = urllib.parse.urlsplit(url)
            meta.update({
                "host": urlp.netloc,
                "pathname": urlp.path,
                "query": dict(urllib.parse.parse_qsl(urlp.query, keep_blank_values=True)),
                "filename": _parse_content_disposition_filename(headers.get("content-disposition","")),
            })
            if extra_meta:
                meta.update(extra_meta)
            # recipe/curl
            if self.rule.emit_curl:
                try:
                    meta["replay"] = self._build_replay_recipe(url, method, req_headers, meta.get("request_body_snippet"))
                except Exception:
                    pass

            meta_path = self._save_meta(host, sig, meta)

            item = TapItem(
                url=url, method=method, resource_type=rtype, status=status,
                content_type=ctype, content_length=clen_int, saved_path=saved,
                meta_path=meta_path, is_json=is_json, is_text=is_text,
                content_hash=content_hash, headers=headers, request_headers=req_headers,
                request_body_saved=bool(meta.get("request_body_snippet")),
                snippet=(None if is_json else (text[:200] if text else None)),
                json_top_keys=top_keys
            )
            self.items.append(item)
            if self._on_item: self._on_item(item)

            # финальный сводный лог по ответу
            if self.rule.log_save_decisions:
                req_snip = meta.get("request_body_snippet")
                req_len = len(req_snip) if isinstance(req_snip, str) else None
                self._dbg(stage="save_result",
                          url=url, status=status, rtype=rtype, ctype=ctype,
                          body_len=len(body), json=is_json, text=is_text,
                          saved_path=saved, meta_path=meta_path,
                          content_hash=content_hash, reason=save_reason,
                          json_top_keys=top_keys,
                          request_snippet_len=req_len,
                          request_snippet_head=(req_snip[:self.rule.log_preview_len] + "…(cut)") if (isinstance(req_snip, str) and len(req_snip) > self.rule.log_preview_len) else req_snip)

        except Exception as e:
            self._dbg(stage="tap_error", error=str(e))
            print(f"[tap] error: {e}")

    def _build_meta(
        self, url: str, method: str, rtype: Optional[str], status: int,
        headers: Dict[str, str], req_headers: Dict[str, str],
        clen_int: Optional[int], saved: Optional[str],
        content_hash: Optional[str], ctype: Optional[str],
        is_json: bool, is_text: bool, top_keys: Optional[List[str]], req
    ) -> Dict[str, Any]:
        post_snippet = None
        if self.rule.save_request_body:
            try:
                post_data = getattr(req, "post_data", None)
                if callable(post_data):
                    post_data = post_data()
            except Exception:
                post_data = None
            if post_data:
                s = str(post_data)
                if len(s) > self.rule.request_body_max:
                    s = s[:self.rule.request_body_max] + "...(truncated)"
                post_snippet = s

        meta = {
            "url": url,
            "method": method,
            "resource_type": rtype,
            "status": status,
            "headers": headers,
            "request_headers": req_headers,
            "request_body_snippet": post_snippet,
            "content_type": ctype,
            "content_length": clen_int,
            "saved_path": saved,
            "content_hash": content_hash,
            "is_json": is_json,
            "is_text": is_text,
            "json_top_keys": top_keys,
            "captured_at": _now(),
        }

        # попытка разобрать f.req из тела — поможет отладке
        if post_snippet and "f.req=" in post_snippet:
            try:
                qs = urllib.parse.parse_qs(post_snippet, keep_blank_values=True)
                if "f.req" in qs:
                    raw = urllib.parse.unquote_plus(qs["f.req"][0])
                    fr = _json_loads_loose(raw)
                    meta["req_f_req"] = fr  # может быть большим — смотри мету
            except Exception:
                pass

        return meta

# -------- convenient contexts --------

class TapContext:
    """Обёртка для Page: with TapContext(page, tap): ..."""
    def __init__(self, page, tap: Optional[NetworkTap] = None):
        self.page = page
        self.tap = tap or NetworkTap()
    def __enter__(self):
        self.tap.attach_to_page(self.page)
        return self.tap
    def __exit__(self, exc_type, exc, tb):
        try: self.tap.detach_from_page(self.page)
        except Exception: pass

class TapContextForBrowserContext:
    """Обёртка для BrowserContext — ловит все страницы, включая поп-апы."""
    def __init__(self, context, tap: Optional[NetworkTap] = None, include_existing_pages: bool = True):
        self.context = context
        self.tap = tap or NetworkTap()
        self.include_existing_pages = include_existing_pages
    def __enter__(self):
        self.tap.attach_to_context(self.context, include_existing_pages=self.include_existing_pages)
        return self.tap
    def __exit__(self, exc_type, exc, tb):
        try: self.tap.detach_from_context(self.context)
        except Exception: pass
