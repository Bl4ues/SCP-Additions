from pathlib import Path

root = Path(__file__).resolve().parents[2]
path = root / "src/main/java/net/mcreator/scpadditions/network/ScpAdditionsModVariables.java"
text = path.read_text(encoding="utf-8")
updated = text.replace(
    "DataFixTypes.LEVEL, DataFixTypes.LEVEL)",
    "DataFixTypes.LEVEL)",
)
if updated != text:
    path.write_text(updated, encoding="utf-8")
    print(f"Repaired idempotent SavedData.Factory migration in {path.relative_to(root)}")
else:
    print("SavedData.Factory migration is already idempotent")
