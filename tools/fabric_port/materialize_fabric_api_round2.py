from __future__ import annotations

import base64
import gzip
import re
from pathlib import Path

root = Path(__file__).resolve().parents[2]
bootstrap = root / "tools/fabric_port/bootstrap_fabric_api_round2.py"
source = bootstrap.read_text(encoding="utf-8")
match = re.search(r'PAYLOAD = """(.*?)"""', source, re.DOTALL)
if match is None:
    raise RuntimeError("Fabric API round 2 payload was not found")

payload = match.group(1)
# The connector write dropped one deterministic character from the compressed
# payload. Restore it before decoding; reject every other unexpected shape.
if len(payload) == 15979:
    payload = payload[:2331] + "Z" + payload[2331:]
elif len(payload) != 15980:
    raise RuntimeError(f"Unexpected Fabric API round 2 payload length: {len(payload)}")

target = root / "tools/fabric_port/apply_fabric_api_round2.py"
target.write_bytes(gzip.decompress(base64.b64decode(payload)))
print(f"Materialized {target.relative_to(root)} ({target.stat().st_size} bytes)")
