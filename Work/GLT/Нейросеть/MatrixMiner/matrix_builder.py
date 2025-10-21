# matrix_builder.py
import re, unicodedata
import numpy as np
import pandas as pd
from typing import List, Dict, Any

def _clean(s):
    if not isinstance(s, str): return s
    s = unicodedata.normalize("NFKC", s).replace("\u00A0"," ")
    return re.sub(r"\s+"," ", s.strip())

def _norm_roles(raw, delims: List[str], case: str, dedup: bool, sort_alpha: bool) -> List[str]:
    if raw is None or (isinstance(raw, float) and np.isnan(raw)): return []
    parts = re.split(rf"\s*(?:{'|'.join(map(re.escape, delims))})\s*", _clean(str(raw)))
    out=[]
    for p in parts:
        if not p: continue
        r=_clean(p)
        if case=="lower": r=r.lower()
        elif case=="upper": r=r.upper()
        elif case=="title": r=r.lower().capitalize()
        if not dedup or r not in out: out.append(r)
    return sorted(out, key=str.lower) if sort_alpha else out

def _safe_get(row: pd.Series, col: str) -> str:
    if not col: return ""
    try:
        if col in row.index:
            v = row[col]
            return _clean(str(v)) if v is not None else ""
    except Exception:
        pass
    try:
        v = row.get(col, "")
        return _clean(str(v)) if v is not None else ""
    except Exception:
        return ""

def _biz_name(row: pd.Series, cfg: Dict[str, Any]) -> str:
    rule, cols = cfg["business_role_rule"], cfg["columns"]
    # use_column только если колонка реально присутствует в row
    if rule.get("type")=="use_column" and cols.get("business_role") and cols["business_role"] in row.index:
        return _safe_get(row, cols["business_role"])
    # generate
    tpl = rule.get("template", "{position}_{resource}_{department}")
    name = tpl.format(
        department=_safe_get(row, cols.get("department")),
        position=_safe_get(row, cols.get("position")),
        employment=_safe_get(row, cols.get("employment")),
        resource=_safe_get(row, cols.get("resource")),
        user=_safe_get(row, cols.get("user")),
    )
    sep = rule.get("separator","_")
    return re.sub(r"\s+", sep, name)

def build_tables(df: pd.DataFrame, cfg: Dict[str, Any]):
    cols, parse, hdr = cfg["columns"], cfg["roles_parse"], cfg["output_headers"]

    needed = [c for c in [
        cols.get("department"),
        cols.get("position"),
        cols.get("employment"),
        cols.get("resource"),
        cols.get("roles"),
        cols.get("business_role"),
        cols.get("user"),
    ] if c and c in df.columns]
    use = df[needed].copy()
    for c in use.columns: use[c] = use[c].apply(_clean)

    roles_col = cols.get("roles")
    if not roles_col or roles_col not in use.columns:
        raise RuntimeError("Колонка 'roles' обязательна по контракту LLM → builder.")

    use["__roles_list__"] = use[roles_col].apply(lambda x: _norm_roles(
        x, parse.get("delimiters") or [",",";","|"],
        parse.get("normalize_case","title"),
        bool(parse.get("deduplicate",True)),
        bool(parse.get("sort_alpha",True))
    ))

    # ключ группировки без user!
    key_cols = [x for x in [
        cols.get("department"),
        cols.get("position"),
        cols.get("employment"),
        cols.get("resource"),
    ] if x and x in use.columns]

    if key_cols:
        grp = use.groupby(key_cols, dropna=False)["__roles_list__"]
    else:
        fake = "__all__"
        use[fake] = 0
        grp = use.groupby([fake])["__roles_list__"]

    grouped = grp.apply(lambda lists: sorted(set.intersection(*map(set, lists))) if len(lists)>0 else []) \
                 .reset_index(name="__common__")

    grouped["__bizrole__"] = grouped.apply(
        lambda r: (r["__common__"][0] if isinstance(r["__common__"], list) and len(r["__common__"])==1 else _biz_name(r, cfg)),
        axis=1
    )

    # Таблица 1: паспорт бизнес-роли
    top_cols = key_cols + ["__bizrole__"]
    top_df = grouped[top_cols].copy()
    ren = {}
    if cols.get("department") in top_df.columns: ren[cols["department"]] = hdr["department"]
    if cols.get("position")   in top_df.columns: ren[cols["position"]]   = hdr["position"]
    if cols.get("employment") in top_df.columns: ren[cols["employment"]] = hdr["employment"]
    ren["__bizrole__"] = hdr["business_role"]
    top_df = top_df.rename(columns=ren)

    # Таблица 2: описание общей роли
    res_col = cols.get("resource")
    map_df = grouped.copy()
    map_df["__roles_str__"] = map_df["__common__"].apply(lambda xs: ", ".join(xs))
    if res_col and res_col in map_df.columns:
        map_df = map_df.rename(columns={"__bizrole__": hdr["business_role"], res_col: hdr["resource"]})
    else:
        map_df = map_df.rename(columns={"__bizrole__": hdr["business_role"]})

    if hdr["department"] not in map_df.columns and cols.get("department") in map_df.columns:
        map_df = map_df.rename(columns={cols["department"]: hdr["department"]})

    want = [hdr["business_role"], hdr["department"]]
    if hdr["resource"] in map_df.columns: want.append(hdr["resource"])
    map_df = map_df[[c for c in want if c in map_df.columns] + ["__roles_str__"]].copy()
    map_df = map_df.rename(columns={"__roles_str__": hdr["role"]})
    map_df = map_df.sort_values([c for c in [hdr["business_role"], hdr["department"]] if c in map_df.columns]).reset_index(drop=True)

    return top_df, map_df
