from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
JAVA_ROOT = ROOT / "src/main/java"

EXPR = r"[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*(?:\([^()\n]*\))?)*"
PATTERNS = (
    (re.compile(rf"(?P<expr>{EXPR})\.getOrCreateTag\(\)"),
     lambda m: f"LegacyItemTags.getOrCreateTag({m.group('expr')})"),
    (re.compile(rf"(?P<expr>{EXPR})\.getTag\(\)"),
     lambda m: f"LegacyItemTags.getTag({m.group('expr')})"),
    (re.compile(rf"(?P<expr>{EXPR})\.hasTag\(\)"),
     lambda m: f"LegacyItemTags.hasTag({m.group('expr')})"),
    (re.compile(rf"(?P<expr>{EXPR})\.setTag\((?P<arg>[^;\n]*)\)"),
     lambda m: f"LegacyItemTags.setTag({m.group('expr')}, {m.group('arg')})"),
)

changed = 0
for path in JAVA_ROOT.rglob("*.java"):
    if path.name == "LegacyItemTags.java":
        continue
    text = path.read_text(encoding="utf-8")
    updated = text
    for pattern, replacement in PATTERNS:
        updated = pattern.sub(replacement, updated)
    if updated != text:
        if "import com.bl4ues.scpadditions.compat.LegacyItemTags;" not in updated:
            package_end = updated.find("\n", updated.find("package ")) + 1
            updated = (
                updated[:package_end]
                + "\nimport com.bl4ues.scpadditions.compat.LegacyItemTags;\n"
                + updated[package_end:]
            )
        path.write_text(updated, encoding="utf-8")
        changed += 1

remaining = []
for path in JAVA_ROOT.rglob("*.java"):
    if path.name == "LegacyItemTags.java":
        continue
    text = path.read_text(encoding="utf-8")
    if any(token in text for token in (
        ".getOrCreateTag()", ".getTag()", ".hasTag()", ".setTag("
    )):
        remaining.append(str(path.relative_to(ROOT)))

print(f"Updated {changed} files for ItemStack custom data")
if remaining:
    print("Potential legacy item tag calls remaining:")
    print("\n".join(remaining))
