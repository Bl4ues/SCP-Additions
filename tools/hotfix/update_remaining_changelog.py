#!/usr/bin/env python3
from pathlib import Path

path = Path("CHANGELOG.md")
text = path.read_text(encoding="utf-8")
anchor = "- Corrected SCP-294 cups having missing-texture outputs;\n"
addition = """- Corrected SCP-294 cups having missing-texture outputs;
- Corrected duplicated `Cup of Cup of` names on configurable SCP-294 drinks;
- Made item category changes update immediately after saving them through the in-game editor;
- Made context interactions ignore missing block and entity IDs instead of incorrectly assigning them to vanilla entries;
- Made SCP-173 immune to attacks dealing 6 damage or less while allowing stronger weapons to damage and eventually destroy it, without making it vulnerable to knockback;
- Prevented damaged SCP-173 instances from restoring all health when their chunk or world is loaded again;
"""
if addition in text:
    raise SystemExit(0)
if text.count(anchor) != 1:
    raise RuntimeError(f"Expected one changelog anchor, found {text.count(anchor)}")
path.write_text(text.replace(anchor, addition, 1), encoding="utf-8")
