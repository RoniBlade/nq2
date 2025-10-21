#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os, io, re, glob, json, gzip, time, math, hashlib, logging, threading
from typing import Any, Dict, Iterable, List, Optional, Tuple, Callable

import numpy as np
import pandas as pd
from fastapi import FastAPI, File, UploadFile, Body, Query
from fastapi.responses import JSONResponse, FileResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware

# -------------------- APP & THEME --------------------
app = FastAPI(title="Perf Log Viewer")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
app.add_middleware(GZipMiddleware, minimum_size=1024)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CACHE_DIR = os.path.join(BASE_DIR, ".cache")
os.makedirs(CACHE_DIR, exist_ok=True)

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")

# -------------------- HELPERS --------------------
def _json_sanitize(o):
    if isinstance(o, (np.generic,)): return o.item()
    if isinstance(o, pd.Timestamp): return o.isoformat().replace("+00:00", "Z")
    if isinstance(o, dict): return {k: _json_sanitize(v) for k, v in o.items()}
    if isinstance(o, (list, tuple)): return [_json_sanitize(v) for v in o]
    try:
        if pd.isna(o): return None
    except Exception: ...
    if isinstance(o, (float, np.floating)):
        f = float(o)
        if math.isnan(f) or math.isinf(f): return None
    return o

def _strip(s: str) -> str:
    return (s or "").replace("\ufeff","").replace("\u200b","").strip()

def _fingerprint_paths(paths: List[str]) -> str:
    h = hashlib.md5()
    for p in sorted(paths):
        try:
            st = os.stat(p); h.update(f"{p}|{int(st.st_mtime)}|{st.st_size}".encode())
        except FileNotFoundError:
            h.update(f"{p}|missing".encode())
    return h.hexdigest()

def _read_text_lines(src: Any) -> Iterable[str]:
    if isinstance(src, (bytes, bytearray, io.BytesIO)):
        data = src if isinstance(src, (bytes, bytearray)) else src.getvalue()
        with io.TextIOWrapper(io.BytesIO(data), encoding="utf-8", errors="ignore") as tf:
            for line in tf: yield line
        return
    p = str(src)
    if p.endswith(".gz"):
        with gzip.open(p, "rt", encoding="utf-8", errors="ignore") as f:
            for line in f: yield line
    else:
        with open(p, "r", encoding="utf-8", errors="ignore") as f:
            for line in f: yield line

def parse_maybe_json(line: str) -> Optional[Dict[str, Any]]:
    s = _strip(line)
    i, j = s.find("{"), s.rfind("}")
    if i >= 0 and j > i:
        try: return json.loads(s[i:j+1])
        except Exception: return None
    return None

# -------------------- LOG DISCOVERY --------------------
def _find_logs_root(start: str) -> str:
    if os.path.isdir(start): return start
    cur = BASE_DIR
    for _ in range(8):
        cand = os.path.join(cur, "logs")
        if os.path.isdir(cand): return cand
        nxt = os.path.dirname(cur)
        if nxt == cur: break
        cur = nxt
    return start

def _normalize_ext(ext: str) -> Optional[str]:
    if not ext or ext.lower() == "все": return None
    return ext if ext.startswith(".") else f".{ext}"

def collect_files(path="logs", ext=".log", recursive=True, exclude_regex: Optional[str]=None) -> List[str]:
    root = _find_logs_root(path)
    want = _normalize_ext(ext)
    rx_exclude = re.compile(exclude_regex) if exclude_regex else None
    out = []
    if os.path.isdir(root):
        for r, _, files in os.walk(root):
            for fn in files:
                if want is None or os.path.splitext(fn)[1].lower() == want.lower() or fn.endswith(".gz"):
                    full = os.path.join(r, fn)
                    if rx_exclude and rx_exclude.search(full): continue
                    out.append(full)
            if not recursive: break
    else:
        for p in glob.glob(root, recursive=True):
            if os.path.isfile(p):
                if want is None or os.path.splitext(p)[1].lower() == want.lower() or p.endswith(".gz"):
                    if rx_exclude and rx_exclude.search(p): continue
                    out.append(p)
    return sorted(out)

# -------------------- ROBUST EXTRACTORS --------------------
IP_RE = re.compile(r"\b(?:\d{1,3}\.){3}\d{1,3}\b")
USERS_NUM_RE = re.compile(r"\b(?:users?|accounts?)\s*[:=]\s*(\d+)\b", re.I)
DONE_LINE_RE = re.compile(
    r"Done[:\s-]*.*?(?:taskId|task|id)\s*[:=]\s*(?P<task>\S+).*?(?:ip|host|addr(?:ess)?)\s*[:=]\s*(?P<ip>[\d\.]+).*?(?:users?|accounts?)\s*[:=]\s*(?P<users>\d+)",
    re.I,
)

def _dig_numeric(obj: Any, key_rx: re.Pattern) -> Optional[int]:
    if isinstance(obj, dict):
        for k, v in obj.items():
            if key_rx.search(str(k)):
                try: return int(str(v))
                except Exception: ...
        for v in obj.values():
            got = _dig_numeric(v, key_rx)
            if got is not None: return got
    if isinstance(obj, list):
        for v in obj:
            got = _dig_numeric(v, key_rx)
            if got is not None: return got
    return None

def _dig_text(obj: Any, key_rx: re.Pattern) -> Optional[str]:
    if isinstance(obj, dict):
        for k, v in obj.items():
            if key_rx.search(str(k)) and isinstance(v, str): return v
        for v in obj.values():
            got = _dig_text(v, key_rx)
            if got is not None: return got
    if isinstance(obj, list):
        for v in obj:
            got = _dig_text(v, key_rx)
            if got is not None: return got
    return None

# -------------------- PARSING --------------------
def parse_files(files: List[Any], progress: Callable[[str], None]) -> Tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    perf_rows, scan_rows, retry_rows, error_rows = [], [], [], []
    total = len(files)

    for i, p in enumerate(files, 1):
        t0 = time.time()
        progress(f"→ читаю: {p}")
        for raw in _read_text_lines(p):
            line = raw.strip()
            if not line: continue

            obj = parse_maybe_json(line)
            if isinstance(obj, dict):
                flat = dict(obj)
                for k, v in list(obj.items()):
                    if isinstance(v, dict) and "ms" in v:
                        flat[f"{k}_ms"] = v.get("ms")
                perf_rows.append(flat)

                users = _dig_numeric(obj, re.compile(r"(users?|accounts?)$", re.I))
                ip = _dig_text(obj, re.compile(r"^(ip|host|hostname|addr(?:ess)?)$", re.I))
                task = _dig_text(obj, re.compile(r"(taskId|task|id)$", re.I))
                if users is not None:
                    scan_rows.append({
                        "taskId": task, "ip": ip, "users": int(users),
                        "host": _dig_text(obj, re.compile(r"^(host|hostname)$", re.I)),
                        "_source": "json", "file": p,
                    })
                mret = re.search(r"\battempt\b\s*#?(\d+)", line, re.I)
                if mret and task:
                    retry_rows.append({"taskId": task, "attempt": int(mret.group(1))})
                if "ERROR" in line or '"level":"ERROR"' in line or '"level":"error"' in line:
                    cat = "other"
                    if re.search(r"auth|Authentication|Access\s*denied", line, re.I): cat="auth"
                    elif re.search(r"timeout|timed\s*out", line, re.I): cat="timeout"
                    elif re.search(r"connect(ion)?\s*refused|unreachable|No route", line, re.I): cat="connect"
                    elif re.search(r"\bssh\b|handshake|host key", line, re.I): cat="ssh"
                    elif re.search(r"kafka", line, re.I): cat="kafka"
                    elif re.search(r"sql|jdbc|database|postgres", line, re.I): cat="db"
                    error_rows.append({"category": cat, "line": line, "file": p})
                continue

            m = DONE_LINE_RE.search(line)
            if m:
                scan_rows.append({
                    "taskId": m.group("task"), "ip": m.group("ip"),
                    "users": int(m.group("users")), "host": None,
                    "_source": "done", "file": p,
                })
                continue

            mu = USERS_NUM_RE.search(line)
            if mu:
                users = int(mu.group(1))
                ip = (IP_RE.search(line).group(0) if IP_RE.search(line) else None)
                scan_rows.append({
                    "taskId": None, "ip": ip, "users": users,
                    "host": None, "_source": "regex", "file": p,
                })
                continue

            mret = re.search(r"\[RETRY\]\s*attempt\s*#(\d+)\s*for\s*taskId=([^\s,]+)", line)
            if mret:
                retry_rows.append({"taskId": mret.group(2), "attempt": int(mret.group(1))})
                continue

            if " ERROR " in f" {line} " or "[ERROR]" in line:
                cat = "other"
                if re.search(r"auth|Authentication|Access\s*denied", line, re.I): cat="auth"
                elif re.search(r"timeout|timed\s*out", line, re.I): cat="timeout"
                elif re.search(r"connect(ion)?\s*refused|unreachable|No route", line, re.I): cat="connect"
                elif re.search(r"\bssh\b|handshake|host key", line, re.I): cat="ssh"
                elif re.search(r"kafka", line, re.I): cat="kafka"
                elif re.search(r"sql|jdbc|database|postgres", line, re.I): cat="db"
                error_rows.append({"category": cat, "line": line, "file": p})

        progress(f"✓ разобран за {time.time()-t0:.3f}s  ({os.path.basename(p)})  [{i}/{total}]")

    perf_df = pd.DataFrame(perf_rows) if perf_rows else pd.DataFrame()
    scans_df = pd.DataFrame(scan_rows) if scan_rows else pd.DataFrame()
    retries_df = pd.DataFrame(retry_rows) if retry_rows else pd.DataFrame()
    errors_df = pd.DataFrame(error_rows) if error_rows else pd.DataFrame()

    if not perf_df.empty:
        perf_df.columns = [_strip(c) for c in perf_df.columns]
        for c in perf_df.columns:
            if c.endswith("_ms"): perf_df[c] = pd.to_numeric(perf_df[c], errors="coerce")
        if "total_ms" in perf_df.columns:
            perf_df["total_ms"] = pd.to_numeric(perf_df["total_ms"], errors="coerce")
        if "phases" in perf_df.columns:
            try:
                ph = pd.json_normalize(perf_df["phases"].fillna({}))
                ph.columns = [f"phase::{_strip(c)}" for c in ph.columns]
                perf_df = pd.concat([perf_df.drop(columns=["phases"]), ph], axis=1)
            except Exception: ...

    if not scans_df.empty:
        scans_df["users"] = pd.to_numeric(scans_df["users"], errors="coerce").fillna(0).astype(int)
        for c in ["ip", "host", "taskId"]:
            if c not in scans_df.columns: scans_df[c] = None
        scans_df = (scans_df
                    .sort_index()
                    .groupby(["taskId","ip"], dropna=False)
                    .agg(users=("users","max"),
                         host=("host","last"),
                         file=("file","last"),
                         sources=("_source", lambda s: ",".join(sorted(set(map(str, s))))))
                    .reset_index())

    aux = pd.DataFrame()
    aux.attrs["retries"] = retries_df
    aux.attrs["errors"]  = errors_df
    return perf_df, scans_df, aux

# -------------------- PAYLOAD --------------------
def _hist_bins_fd(series: pd.Series, max_bins=60):
    s = pd.to_numeric(series, errors="coerce").dropna()
    if s.empty: return [], []
    q75, q25 = np.percentile(s, 75), np.percentile(s, 25)
    iqr = max(q75 - q25, 1e-9)
    binw = 2 * iqr / (len(s) ** (1/3))
    if binw <= 0: binw = max((s.max()-s.min())/20, 1.0)
    bins = int(min(max_bins, max(5, math.ceil((s.max()-s.min())/binw))))
    counts, edges = np.histogram(s, bins=bins)
    centers = ((edges[:-1] + edges[1:]) / 2.0).tolist()
    return centers, counts.tolist()

def build_payload(perf_df: pd.DataFrame, scans_df: pd.DataFrame, aux_df: pd.DataFrame) -> Dict[str, Any]:
    retries_df = aux_df.attrs.get("retries", pd.DataFrame())
    errors_df  = aux_df.attrs.get("errors",  pd.DataFrame())

    # derive scans from perf if empty
    if (scans_df is None or scans_df.empty) and perf_df is not None and not perf_df.empty:
        cols_users = [c for c in perf_df.columns if re.search(r"(?:^|\.)(users?|accounts?)(?:$|\.)", str(c), re.I)]
        cols_ip    = [c for c in perf_df.columns if re.search(r"(?:^|\.)(ip|host|hostname|address)(?:$|\.)", str(c), re.I)]
        if cols_users:
            tmp = pd.DataFrame({"users": pd.to_numeric(perf_df[cols_users[0]], errors="coerce")})
            tmp["ip"] = perf_df[cols_ip[0]].astype(str) if cols_ip else None
            tmp["taskId"] = None; tmp["host"] = None
            scans_df = tmp; scans_df["users"] = scans_df["users"].fillna(0).astype(int)

    payload: Dict[str, Any] = {}

    # scans summary + light aggregates
    if scans_df is not None and not scans_df.empty:
        users_s = pd.to_numeric(scans_df["users"], errors="coerce").fillna(0)
        p99 = float(np.percentile(users_s, 99)) if len(users_s) else 0.0
        centers, counts = _hist_bins_fd(users_s.clip(upper=p99), max_bins=60)

        users_per_ip = (scans_df.groupby("ip", dropna=False)["users"]
                        .agg(users_total="sum", tasks="count")
                        .reset_index()
                        .sort_values("users_total", ascending=False))

        payload["users_hist"] = {"x": centers, "y": counts}
        payload["users_hist_meta"] = {"clip_to_p": 99, "clip_to": p99, "max_raw": int(users_s.max()) if len(users_s) else 0}
        payload["users_percentiles"] = {
            "p50": float(np.percentile(users_s, 50)),
            "p90": float(np.percentile(users_s, 90)),
            "p95": float(np.percentile(users_s, 95)),
            "p99": float(np.percentile(users_s, 99)),
        }
        payload["users_per_ip_top"] = users_per_ip.head(15).to_dict("records")
        payload["zero_ip_top"] = (scans_df.loc[scans_df["users"]==0]
                                  .groupby("ip", dropna=False).size()
                                  .reset_index(name="times")
                                  .sort_values("times", ascending=False).head(50)
                                  .to_dict("records"))
        payload["scans_summary"] = {
            "total_tasks": int(len(scans_df)),
            "unique_ip": int(scans_df["ip"].nunique()),
            "with_accounts": int((users_s>0).sum()),
            "zero_accounts": int((users_s==0).sum()),
            "avg_task_ms": float(pd.to_numeric(perf_df.get("total_ms", pd.Series(dtype=float)), errors="coerce").mean()) \
                           if perf_df is not None and not perf_df.empty and "total_ms" in perf_df.columns else None
        }
    else:
        payload["users_hist"] = {"x": [], "y": []}
        payload["users_hist_meta"] = {}
        payload["users_percentiles"] = {}
        payload["users_per_ip_top"] = []
        payload["zero_ip_top"] = []
        payload["scans_summary"] = None

    # retries distribution (counts only)
    if not retries_df.empty:
        attempts = (retries_df.groupby("taskId")["attempt"].max().reset_index(name="max_attempt"))
        dist = attempts["max_attempt"].value_counts().sort_index()
        payload["retries_dist"] = [{"attempt": int(k), "count": int(v)} for k, v in dist.items()]
        payload["retries_detailed"] = attempts.sort_values("max_attempt", ascending=False).head(200).to_dict("records")
    else:
        payload["retries_dist"] = []
        payload["retries_detailed"] = []

    # errors
    if not errors_df.empty:
        cats = (errors_df.groupby("category").size().reset_index(name="n").sort_values("n", ascending=False))
        payload["error_categories"] = cats.to_dict("records")
        payload["error_lines_preview"] = errors_df.head(40)[["category","line","file"]].to_dict("records")
        tbl = errors_df.copy()
        tbl["ip"] = [ (IP_RE.search(x).group(0) if isinstance(x,str) and IP_RE.search(x) else None) for x in tbl["line"] ]
        payload["errors_table"] = tbl[["category","ip","line","file"]].to_dict("records")
        payload["error_ip_top"] = (tbl["ip"].dropna().value_counts().reset_index()
                                   .rename(columns={"index":"ip","ip":"n"}).head(30).to_dict("records"))
    else:
        payload["error_categories"] = []; payload["error_lines_preview"] = []
        payload["errors_table"] = []; payload["error_ip_top"] = []

    # perf aggregates
    if perf_df is not None and not perf_df.empty:
        df = perf_df.copy()
        for c in df.columns:
            if c.endswith("_ms") or c == "total_ms" or c.startswith("phase::"):
                df[c] = pd.to_numeric(df[c], errors="coerce")
        if "component" not in df.columns:
            df["component"] = df.get("logger") or df.get("source") or "component"
        df["component_pretty"] = df["component"]
        agg = (df.groupby(["component","component_pretty"], dropna=False)
               .agg(n=("total_ms","size"),
                    avg_ms=("total_ms","mean"),
                    p95_ms=("total_ms", lambda s: s.quantile(0.95)),
                    max_ms=("total_ms","max"))
               .reset_index()
               .sort_values("avg_ms", ascending=False))
        payload["perf_components"] = _json_sanitize(agg.head(30).to_dict("records"))
        phase_cols = [c for c in df.columns if c.startswith("phase::")]
        if phase_cols:
            means = df[phase_cols].mean(numeric_only=True).dropna().sort_values(ascending=False)
            payload["perf_phase_means"] = [{"phase": c.replace("phase::",""), "avg_ms": float(v)} for c,v in means.head(30).items()]
        else:
            payload["perf_phase_means"] = []
    else:
        payload["perf_components"] = []; payload["perf_phase_means"] = []

    return _json_sanitize(payload)

# -------------------- JOBS --------------------
class ScanJob:
    def __init__(self, params: Dict[str, Any]):
        self.id = hashlib.md5(os.urandom(16)).hexdigest()[:12]
        self.params = params
        self.state = "queued"
        self.started = None
        self.ended = None
        self.progress_log: List[str] = []
        self.total = 0
        self.done = 0
        self.result = None
        self.error = None
        self._cancel = False
        self.lock = threading.Lock()
    def log(self, msg: str):
        with self.lock:
            self.progress_log.append(msg)
            if len(self.progress_log) > 1000: self.progress_log = self.progress_log[-1000:]

JOBS: Dict[str, ScanJob] = {}
LAST_RESULT: Optional[Dict[str, Any]] = None

def _cache_path(fpr: str) -> str: return os.path.join(CACHE_DIR, f"{fpr}.json.gz")

# ---------- cache lookup helper & endpoint ----------
def _cache_lookup(params: Dict[str, Any]) -> Dict[str, Any]:
    """Return immediate cache info for supplied params."""
    files = collect_files(
        path=params.get("path") or "logs",
        ext=params.get("ext") or ".log",
        recursive=bool(params.get("recursive", True)),
        exclude_regex=params.get("exclude_regex") or None,
    )
    fpr = _fingerprint_paths(files)
    cfile = _cache_path(fpr)
    if os.path.isfile(cfile):
        with gzip.open(cfile, "rt", encoding="utf-8") as f:
            payload = json.load(f)
        return {"hit": True, "payload": payload, "fingerprint": fpr, "files": files, "from_cache": True}
    return {"hit": False, "fingerprint": fpr, "files": files, "from_cache": False}

# -------------------- RUN JOB --------------------
def run_job(job: ScanJob):
    global LAST_RESULT
    try:
        job.state = "running"; job.started = time.time()
        p = job.params
        files = collect_files(path=p.get("path") or "logs",
                              ext=p.get("ext") or ".log",
                              recursive=bool(p.get("recursive", True)),
                              exclude_regex=p.get("exclude_regex") or None)
        job.total = len(files); job.log(f"collect: files={job.total}")
        fpr = _fingerprint_paths(files); cfile = _cache_path(fpr)

        # only-cache mode
        if p.get("use_cache_only"):
            if os.path.isfile(cfile):
                job.log("cache: hit (use_cache_only)")
                with gzip.open(cfile, "rt", encoding="utf-8") as f:
                    payload = json.load(f)
                job.done = job.total
                job.result = {"payload": payload, "files": files, "fingerprint": fpr, "from_cache": True}
                job.state = "done"; job.ended = time.time()
                LAST_RESULT = {**payload, "_meta": {"fingerprint": fpr, "from_cache": True}}
            else:
                job.log("cache: miss (use_cache_only)")
                job.state = "error"; job.error = "cache_miss"; job.ended = time.time()
            return

        if p.get("use_cache", True) and os.path.isfile(cfile):
            job.log("cache: hit")
            with gzip.open(cfile, "rt", encoding="utf-8") as f:
                payload = json.load(f)
            job.done = job.total
            job.result = {"payload": payload, "files": files, "fingerprint": fpr, "from_cache": True}
            job.state = "done"; job.ended = time.time()
            LAST_RESULT = {**payload, "_meta": {"fingerprint": fpr, "from_cache": True}}
            return

        if job.total == 0:
            pay = build_payload(pd.DataFrame(), pd.DataFrame(), pd.DataFrame())
            job.done = 0
            job.result = {"payload": pay, "files": [], "fingerprint": "empty", "from_cache": False}
            job.state = "done"; job.ended = time.time()
            LAST_RESULT = {**pay, "_meta": {"fingerprint": "empty", "from_cache": False}}
            return

        def pmsg(txt: str):
            # консоль + прогресс из хвоста "[i/n]"
            job.log(txt)
            m = re.search(r"\[(\d+)/(\d+)\]\s*$", txt)
            if m:
                job.done = int(m.group(1))
                job.total = int(m.group(2))

        t0 = time.time()
        perf_df, scans_df, aux_df = parse_files(files, progress=pmsg)
        job.done = job.total
        pay = build_payload(perf_df, scans_df, aux_df)
        job.result = {"payload": pay, "files": files, "fingerprint": fpr, "from_cache": False, "t_sec": time.time()-t0}
        try:
            with gzip.open(cfile, "wt", encoding="utf-8") as f: json.dump(pay, f, ensure_ascii=False)
        except Exception as e:
            job.log(f"cache: save error: {e}")
        job.state = "done"; job.ended = time.time()
        LAST_RESULT = {**pay, "_meta": {"fingerprint": fpr, "from_cache": False}}
    except Exception as e:
        logging.exception("job error")
        job.state = "error"; job.error = str(e); job.ended = time.time()

# -------------------- API --------------------
@app.post("/api/scan/start")
def api_scan_start(payload: Dict[str, Any] = Body(...)):
    job = ScanJob(payload or {}); JOBS[job.id] = job
    threading.Thread(target=run_job, args=(job,), daemon=True).start()
    return {"job": job.id}

@app.get("/api/scan/progress")
def api_scan_progress(job: str = Query(...)):
    j = JOBS.get(job)
    if not j: return JSONResponse({"error": "job not found"}, status_code=404)
    elapsed = (time.time()-j.started) if j.started else 0.0
    percent = (100.0 * (j.done or 0) / (j.total or 1)) if j.total else 0.0
    from_cache = bool(j.result and j.result.get("from_cache"))
    return {"state": j.state, "done": j.done, "total": j.total, "percent": round(percent,1),
            "elapsed": round(elapsed,1), "log": j.progress_log[-200:], "error": j.error, "cached": from_cache}

@app.get("/api/scan/result")
def api_scan_result(job: str = Query(...)):
    j = JOBS.get(job)
    if not j: return JSONResponse({"error": "job not found"}, status_code=404)
    if j.state != "done" or not j.result: return JSONResponse({"error":"not ready","state":j.state}, status_code=425)
    out = dict(j.result); out["duration"] = (j.ended or time.time())-(j.started or time.time())
    return _json_sanitize(out)

@app.post("/api/upload")
async def api_upload(files: List[UploadFile] = File(...)):
    bufs, names = [], []
    for f in files: bufs.append(await f.read()); names.append(f.filename)
    def pmsg(_): ...
    perf_df, scans_df, aux_df = parse_files(bufs, progress=pmsg)
    payload = build_payload(perf_df, scans_df, aux_df)
    return _json_sanitize({"payload": payload, "files": names,
                           "fingerprint": hashlib.md5((";".join(names)+str(sum(len(b) for b in bufs))).encode()).hexdigest(),
                           "from_cache": False})

@app.get("/api/last")
def api_last():
    if LAST_RESULT is None:
        return {}
    return _json_sanitize(LAST_RESULT)

@app.post("/api/cache/lookup")
def api_cache_lookup(payload: Dict[str, Any] = Body(...)):
    return _json_sanitize(_cache_lookup(payload or {}))

@app.get("/")
def index(): return FileResponse(os.path.join(BASE_DIR, "viewer.html"))

if __name__ == "__main__":
    import uvicorn, os as _os
    _os.environ.setdefault("WATCHFILES_FORCE_POLLING", "1")
    uvicorn.run("perf_log_viewer:app", host="0.0.0.0", port=8060, reload=False)
