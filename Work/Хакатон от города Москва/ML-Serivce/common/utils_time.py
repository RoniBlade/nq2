from datetime import datetime
import pytz

def is_weekend_from_ts(ts_ms: int, tz="Europe/Warsaw"):
    dt = datetime.fromtimestamp(ts_ms/1000, tz=pytz.timezone(tz))
    return dt.weekday() >= 5, dt.hour
