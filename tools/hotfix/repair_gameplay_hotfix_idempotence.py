#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
path = ROOT / "src/main/java/net/mcreator/scpadditions/entity/Scp173Entity.java"
text = path.read_text(encoding="utf-8")
updated = text
while "dotThreshold, dotThreshold," in updated:
    updated = updated.replace("dotThreshold, dotThreshold,", "dotThreshold,")
if updated != text:
    path.write_text(updated, encoding="utf-8")
    print(f"Collapsed duplicate SCP-173 observation thresholds in {path.relative_to(ROOT)}")
else:
    print("SCP-173 observation threshold migration is already idempotent")
