from __future__ import annotations

import base64
import hashlib
from pathlib import Path

EXPECTED_SHA256 = "838866362be0e024efdaf7617b2125bdf305bbf23f248792bc024586e5abd0d5"
EXPECTED_SIZE = 28117
EXPECTED_ENCODED_SIZE = 37492

root = Path(__file__).resolve().parents[2]
parts_dir = root / "tools/fabric_port/round3_parts"
parts = sorted(parts_dir.glob("part*.b64"))
if len(parts) != 10:
    raise RuntimeError(f"Expected 10 Fabric round 3 parts, found {len(parts)}")

encoded = "".join(path.read_text(encoding="ascii").strip() for path in parts)
if len(encoded) != EXPECTED_ENCODED_SIZE:
    raise RuntimeError(f"Unexpected encoded Fabric round 3 size: {len(encoded)}")

# The GitHub text connector substituted one verified Base64 character while
# storing part03. Repair that exact transport position before decoding, then
# require the complete source checksum below; no other mutation is accepted.
transport_index = 13362
if encoded[transport_index] == "F":
    encoded = encoded[:transport_index] + "9" + encoded[transport_index + 1:]

raw = base64.b64decode(encoded, validate=True)
actual_sha256 = hashlib.sha256(raw).hexdigest()
if len(raw) != EXPECTED_SIZE or actual_sha256 != EXPECTED_SHA256:
    raise RuntimeError(
        f"Fabric round 3 payload mismatch: size={len(raw)}, sha256={actual_sha256}"
    )

target = root / "tools/fabric_port/apply_fabric_api_round3.py"
target.write_bytes(raw)
print(f"Materialized {target.relative_to(root)} ({len(raw)} bytes, sha256={actual_sha256})")
