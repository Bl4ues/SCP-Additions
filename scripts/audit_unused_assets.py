#!/usr/bin/env python3
"""Temporary CI bootstrap used only to obtain a source snapshot on this branch."""

from pathlib import Path

report_dir = Path("build/reports/asset-audit")
report_dir.mkdir(parents=True, exist_ok=True)
(report_dir / "bootstrap.txt").write_text(
    "Temporary source-snapshot bootstrap; not intended for merge.\n",
    encoding="utf-8",
)
