from __future__ import annotations

import base64
import json
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PAYLOAD_DIR = Path(__file__).resolve().parent / "payloads"
FILES = {
    "context_interactions.b64": "config/scpinventory/context_interactions.json",
    "scpinventory.b64": "config/scpinventory/scpinventory.json",
    "294drinks.b64": "config/scpadditions/294drinks.json",
    "914recipes.b64": "config/scpadditions/914recipes.json",
    "modules.b64": "config/scpadditions/modules.json",
}

for payload_name, destination in FILES.items():
    encoded = (PAYLOAD_DIR / payload_name).read_text(encoding="utf-8").strip()
    data = zlib.decompress(base64.b64decode(encoded))
    json.loads(data)
    target = ROOT / destination
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)
    print(f"Wrote {destination}")
