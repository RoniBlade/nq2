# excel_reader.py
import json
import pandas as pd

def read_excel(path: str, preview_rows: int = 5):
    df = pd.read_excel(path)
    preview = df.head(preview_rows).astype(object).where(pd.notna(df.head(preview_rows)), None)
    analysis = {
        "columns": list(df.columns),
        "sample_rows": preview.to_dict(orient="records"),
        "rows_total": int(len(df)),
        "cols_total": int(len(df.columns)),
    }
    return df, analysis

def to_prompt_payload(analysis: dict) -> str:
    return json.dumps(analysis, ensure_ascii=False)
