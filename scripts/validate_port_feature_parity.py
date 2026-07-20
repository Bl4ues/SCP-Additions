#!/usr/bin/env python3
"""Validate that a loader port preserves the complete Forge 3.0.7 surface."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parents[1]
JAVA_PREFIXES = (
    "src/main/java/net/mcreator/scpadditions/",
    "src/main/java/com/bl4ues/scpinventory/",
)
RESOURCE_PREFIXES = (
    "src/main/resources/assets/scp_additions/",
    "src/main/resources/assets/scpinventory/",
    "src/main/resources/assets/scp_unity_extra_blocks/",
    "src/main/resources/assets/scp_ublocks/",
    "src/main/resources/data/",
    "config/",
)
IGNORED_BASE_PATHS = {
    "src/main/resources/META-INF/mods.toml",
    "src/main/resources/META-INF/neoforge.mods.toml",
    "src/main/resources/META-INF/accesstransformer.cfg",
    "src/main/resources/fabric.mod.json",
    "src/main/resources/scp_additions.accesswidener",
}
IGNORED_JAVA_PARTS = (
    "/fabric/",
    "/compat/",
    "/network/compat/",
)
MIGRATED_FILES = {
    "src/main/java/net/mcreator/scpadditions/item/HazmatArmorMaterial.java":
        "src/main/java/net/mcreator/scpadditions/init/ScpAdditionsModArmorMaterials.java",
}
REGISTER_PATTERN = re.compile(r"\bregister\s*\(\s*\"([^\"]+)\"")


def git(*args: str) -> str:
    return subprocess.check_output(
        ["git", *args], cwd=ROOT, text=True, encoding="utf-8", errors="replace"
    )


def tracked_at(ref: str) -> set[str]:
    return set(git("ls-tree", "-r", "--name-only", ref).splitlines())


def current_tracked() -> set[str]:
    return set(git("ls-files").splitlines())


def relevant(path: str) -> bool:
    if path in IGNORED_BASE_PATHS:
        return False
    return path.startswith(JAVA_PREFIXES + RESOURCE_PREFIXES)


def gameplay_java(path: str) -> bool:
    return path.startswith(JAVA_PREFIXES) and not any(part in path for part in IGNORED_JAVA_PARTS)


def read_at(ref: str, path: str) -> str:
    return git("show", f"{ref}:{path}")


def registry_ids(texts: Iterable[str]) -> set[str]:
    result: set[str] = set()
    for text in texts:
        result.update(REGISTER_PATTERN.findall(text))
    return result


def semantic_json(text: str):
    return json.loads(text)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-ref", default="origin/master")
    parser.add_argument("--report", default="build/reports/port-parity/report.json")
    args = parser.parse_args()

    base_files = tracked_at(args.base_ref)
    port_files = current_tracked()
    relevant_base = {path for path in base_files if relevant(path)}
    migrated_files = {
        old: replacement for old, replacement in MIGRATED_FILES.items()
        if old in relevant_base and old not in port_files and replacement in port_files
    }
    missing_files = sorted(
        path for path in relevant_base
        if path not in port_files and path not in migrated_files
    )

    base_java = sorted(path for path in relevant_base if gameplay_java(path))
    missing_java = sorted(
        path for path in base_java
        if path not in port_files and path not in migrated_files
    )

    init_java = [
        path for path in base_java
        if "/init/" in path and path.endswith(".java") and path in port_files
    ]
    base_ids = registry_ids(read_at(args.base_ref, path) for path in init_java)
    port_ids = registry_ids((ROOT / path).read_text(encoding="utf-8", errors="replace") for path in init_java)
    missing_registry_ids = sorted(base_ids - port_ids)

    config_paths = sorted(
        path for path in relevant_base
        if path.startswith("config/") and path.endswith(".json") and path in port_files
    )
    changed_configs: list[str] = []
    invalid_configs: list[str] = []
    for path in config_paths:
        try:
            base_json = semantic_json(read_at(args.base_ref, path))
            port_json = semantic_json((ROOT / path).read_text(encoding="utf-8"))
        except (json.JSONDecodeError, UnicodeDecodeError) as exc:
            invalid_configs.append(f"{path}: {exc}")
            continue
        if base_json != port_json:
            changed_configs.append(path)

    report = {
        "base_ref": args.base_ref,
        "base_feature_files": len(relevant_base),
        "base_gameplay_java_files": len(base_java),
        "base_registry_ids": len(base_ids),
        "migrated_files": migrated_files,
        "missing_files": missing_files,
        "missing_gameplay_java": missing_java,
        "missing_registry_ids": missing_registry_ids,
        "changed_config_json": changed_configs,
        "invalid_config_json": invalid_configs,
    }

    report_path = ROOT / args.report
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")

    print(json.dumps(report, indent=2))
    failures = missing_files or missing_java or missing_registry_ids or changed_configs or invalid_configs
    if failures:
        print("Port feature parity validation failed.")
        return 1
    print("Port preserves the tracked SCP Additions 3.0.7 feature surface.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
