import os
from dotenv import load_dotenv

load_dotenv()

import os
import json
from typing import List, Dict, Optional

def get_proxy_list() -> List[Dict[str, Optional[str]]]:
    """
    Retrieve proxy list from environment variable PROXY_LIST_JSON (as JSON string).
    Returns a list of proxy configuration dicts or an empty list if none or invalid.
    """
    raw = os.getenv("PROXY_LIST_JSON", "[]")
    try:
        proxies = json.loads(raw)
        if not isinstance(proxies, list):
            print("❌ Ошибка: PROXY_LIST_JSON должен быть списком")
            return []
        return proxies
    except json.JSONDecodeError:
        print("❌ Ошибка: PROXY_LIST_JSON невалидный JSON")
        return []

