import pandas as pd
from typing import List, Tuple

def build_features(df: pd.DataFrame, config: dict, target_cols: List[str]) -> Tuple[pd.DataFrame, pd.DataFrame, List[str], dict]:
    """
    Для данного сенсора строим лаги и скользящие средние по КАЖДОМУ таргету.
    Сбор признаков через concat (без фрагментации).
    """
    if not target_cols:
        return pd.DataFrame(), pd.DataFrame(), [], {"rows": 0}

    df = df.copy().sort_values("ts").reset_index(drop=True)
    n = len(df)

    base = pd.DataFrame({
        "hour": df["hour"] if "hour" in df else 0,
        "is_weekend": df["is_weekend"] if "is_weekend" in df else 0
    })

    lags_cfg = int((config.get("features") or {}).get("lags", 24))
    windows_cfg = list((config.get("features") or {}).get("rolling_windows", [6, 12, 24]))
    # чтобы не выесть весь ряд:
    lags = max(1, min(lags_cfg, max(1, n - 1)))
    windows = [w for w in windows_cfg if w < n]

    feat_frames = [base]
    used_targets = []

    for col in target_cols:
        if col not in df.columns:
            continue
        s = df[col].astype("float64")

        # лаги
        lag_dict = {f"{col}_lag_{k}": s.shift(k) for k in range(1, lags + 1)}
        lag_df = pd.concat(lag_dict, axis=1) if lag_dict else pd.DataFrame()

        # скользящие
        roll_dict = {f"{col}_roll_mean_{w}": s.shift(1).rolling(window=w).mean() for w in windows}
        roll_df = pd.concat(roll_dict, axis=1) if roll_dict else pd.DataFrame()

        block = []
        if not lag_df.empty: block.append(lag_df)
        if not roll_df.empty: block.append(roll_df)
        if block:
            feat_frames.append(pd.concat(block, axis=1))
            used_targets.append(col)

    if len(feat_frames) == 1:
        return pd.DataFrame(), pd.DataFrame(), [], {"rows": 0}

    feat_df = pd.concat(feat_frames, axis=1)
    full = pd.concat([feat_df, df[used_targets]], axis=1)

    before = len(full)
    full = full.dropna()
    after = len(full)
    print(f"[FEATURES] used_targets={used_targets} rows before={before} after={after} feat_cols={feat_df.shape[1]}")

    if after == 0:
        return pd.DataFrame(), pd.DataFrame(), [], {"rows": 0}

    feature_cols = list(feat_df.columns)
    X = full[feature_cols]
    y = full[used_targets]
    return X, y, feature_cols, {"rows": after}
