import json
from pathlib import Path

path = Path("src/main/resources/assets/scp_additions/lang/en_us_3_0.json")
data = json.loads(path.read_text(encoding="utf-8"))
required = {
    "subtitles.scp_additions.elevator_button_press": "Elevator button clicks",
    "subtitles.scp_additions.elevator_button_accept": "Elevator accepts request",
}
for key, value in required.items():
    if data.get(key) != value:
        raise SystemExit(f"Missing or incorrect subtitle entry: {key}")
path.write_text(
    json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n",
    encoding="utf-8",
)
print("Elevator subtitle language patch compacted.")
