import json
from typing import Iterator, List, Tuple
from confluent_kafka import Consumer, Producer

def build_consumer(bootstrap: str, group_id: str, topics: List[str]) -> Consumer:
    c = Consumer({
        "bootstrap.servers": bootstrap,
        "group.id": group_id,
        "auto.offset.reset": "earliest",
        "enable.auto.commit": True
    })
    c.subscribe(topics)
    print(f"[KAFKA] consumer built, topics={topics}, group={group_id}")
    return c

def read_stream(consumer: Consumer) -> Iterator[Tuple[str, dict] | None]:
    """
    Возвращаем (topic, message_dict) или None, если пусто.
    topic == sensor_id (по требованию ТЗ).
    """
    while True:
        msg = consumer.poll(0.5)
        if msg is None:
            yield None
            continue
        if msg.error():
            print(f"[KAFKA][ERROR] {msg.error()}")
            continue
        try:
            val = json.loads(msg.value().decode("utf-8"))
            flow_keys = list((val.get("flow") or {}).keys())
            print(f"[KAFKA][MSG] topic={msg.topic()} offset={msg.offset()} keys={flow_keys}")
            yield (msg.topic(), val)
        except Exception as e:
            print(f"[KAFKA][DECODE_ERR] {e}")
            continue

def build_producer(bootstrap: str) -> Producer:
    p = Producer({"bootstrap.servers": bootstrap})
    print(f"[KAFKA] producer built")
    return p

def publish_event(producer: Producer, topic: str, payload: dict):
    producer.produce(topic, json.dumps(payload).encode("utf-8"))
    producer.flush()
    print(f"[KAFKA][PUBLISH] topic={topic} type={payload.get('eventType')} sensor={payload.get('sensorId')}")
