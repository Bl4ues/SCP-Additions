import base64
import bz2
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / ".github/scripts/complete_core_room_elevator.py"
PLACEHOLDER = "[...content truncated for display...]"
EXPECTED_LENGTH = 48_340

text = SOURCE.read_text(encoding="utf-8")
match = re.search(r"b64decode\('([^']+)'\)", text)
if match is None:
    raise RuntimeError("Unable to find the elevator integration payload prefix")

prefix = match.group(1).replace(PLACEHOLDER, "")
chunk_names = [
    "00",
    "01_0",
    "01_1",
    "01_2",
    "01_3a",
    "01_3b",
    "02",
    "03",
    "04",
]
suffix = "".join(
    (ROOT / f".github/scripts/elevator_payload_suffix_{name}.txt")
    .read_text(encoding="utf-8")
    .strip()
    for name in chunk_names
)
payload = prefix + suffix
if len(payload) != EXPECTED_LENGTH:
    raise RuntimeError(
        f"Unexpected elevator payload length: {len(payload)} != {EXPECTED_LENGTH}"
    )

program = bz2.decompress(base64.b64decode(payload)).decode("utf-8")
exec(compile(program, "complete_core_room_elevator.py", "exec"), {"__name__": "__main__"})
