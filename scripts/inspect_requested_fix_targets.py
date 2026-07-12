from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TERMS = (
    "SCREWDRIVER",
    "KeycardReaderConfigScreen",
    "ScpKeyringMirror",
    "syncKeys(",
    "drawString(",
    "drawCenteredString(",
)

lines = []
for path in sorted((ROOT / "src/main/java").rglob("*.java")):
    text = path.read_text(encoding="utf-8")
    matches = []
    for number, line in enumerate(text.splitlines(), 1):
        if any(term in line for term in TERMS):
            matches.append(f"{number}: {line}")
    if matches:
        lines.append(str(path.relative_to(ROOT)))
        lines.extend(matches)
        lines.append("")

out = ROOT / "migration-reference/requested-fix-targets.txt"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text("\n".join(lines), encoding="utf-8")
print(f"Wrote {out} with {len(lines)} lines")
