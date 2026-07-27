from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CUSTOM_MODELS = [
    ROOT / "src/main/resources/assets/scp_additions/models/custom/teslagateinactive.json",
    ROOT / "src/main/resources/assets/scp_additions/models/custom/teslagateactive1.json",
]
MODEL_ROOT = ROOT / "src/main/resources/assets/scp_additions/models"
OLD_SIZE = '"texture_size": [\n    256,\n    256\n  ]'
NEW_SIZE = '"texture_size": [\n    128,\n    128\n  ]'
OLD_TEXTURE = 'scp_additions:block/teslagate"'
NEW_TEXTURE = 'scp_additions:block/teslagate2"'

changed = []
for path in CUSTOM_MODELS:
    text = path.read_text(encoding="utf-8")
    if OLD_SIZE not in text:
        raise RuntimeError(f"Expected 256x256 Tesla atlas declaration not found in {path}")
    path.write_text(text.replace(OLD_SIZE, NEW_SIZE, 1), encoding="utf-8")
    changed.append(path.relative_to(ROOT).as_posix())

for path in MODEL_ROOT.rglob("*.json"):
    text = path.read_text(encoding="utf-8")
    if OLD_TEXTURE not in text:
        continue
    path.write_text(text.replace(OLD_TEXTURE, NEW_TEXTURE), encoding="utf-8")
    changed.append(path.relative_to(ROOT).as_posix())

for temporary in [
    ROOT / "scripts/adjust_tesla_uv_512.py",
    ROOT / ".github/workflows/adjust-tesla-uv-512.yml",
]:
    if temporary.exists():
        temporary.unlink()

print("Adjusted Tesla texture scale in:")
for path in sorted(set(changed)):
    print(f"- {path}")
