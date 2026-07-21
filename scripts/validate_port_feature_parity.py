#!/usr/bin/env python3
"""Validate that a loader port preserves the complete Forge 3.0.7 surface."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
from pathlib import Path
from typing import Any, Iterable

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
RESOURCE_PATH_REPLACEMENTS = (
    ("/advancements/", "/advancement/"),
    ("/recipes/", "/recipe/"),
    ("/loot_tables/", "/loot_table/"),
    ("/structures/", "/structure/"),
    ("/functions/", "/function/"),
    ("/predicates/", "/predicate/"),
    ("/item_modifiers/", "/item_modifier/"),
    ("/tags/blocks/", "/tags/block/"),
    ("/tags/items/", "/tags/item/"),
    ("/tags/fluids/", "/tags/fluid/"),
    ("/tags/entity_types/", "/tags/entity_type/"),
    ("/tags/functions/", "/tags/function/"),
    ("/tags/game_events/", "/tags/game_event/"),
    ("/tags/damage_types/", "/tags/damage_type/"),
)
REGISTER_PATTERN = re.compile(r"\bregister\s*\(\s*\"([^\"]+)\"")


def git_bytes(*args: str) -> bytes:
    return subprocess.check_output(["git", *args], cwd=ROOT)


def git(*args: str) -> str:
    return git_bytes(*args).decode("utf-8", errors="replace")


def tracked_at(ref: str) -> set[str]:
    return set(git("ls-tree", "-r", "--name-only", ref).splitlines())


def current_tracked() -> set[str]:
    return set(git("ls-files").splitlines())


def relevant(path: str) -> bool:
    if path in IGNORED_BASE_PATHS:
        return False
    return path.startswith(JAVA_PREFIXES + RESOURCE_PREFIXES)


def gameplay_java(path: str) -> bool:
    return path.startswith(JAVA_PREFIXES) and not any(
        part in path for part in IGNORED_JAVA_PARTS
    )


def migrated_resource_path(path: str) -> str:
    migrated = path
    for old, new in RESOURCE_PATH_REPLACEMENTS:
        migrated = migrated.replace(old, new)
    return migrated


def expected_port_path(path: str, port_files: set[str]) -> str | None:
    replacement = MIGRATED_FILES.get(path)
    if replacement and replacement in port_files:
        return replacement
    migrated = migrated_resource_path(path)
    if migrated in port_files:
        return migrated
    if path in port_files:
        return path
    return None


def read_at(ref: str, path: str) -> str:
    return git("show", f"{ref}:{path}")


def read_bytes_at(ref: str, path: str) -> bytes:
    return git_bytes("show", f"{ref}:{path}")


def registry_ids(texts: Iterable[str]) -> set[str]:
    result: set[str] = set()
    for text in texts:
        result.update(REGISTER_PATTERN.findall(text))
    return result


def semantic_json(text: str) -> Any:
    return json.loads(text)


def migrate_expected_json(path: str, value: Any) -> Any:
    if not isinstance(value, dict):
        return value
    migrated = json.loads(json.dumps(value))
    migrated_path = migrated_resource_path(path)
    if "/advancement/" in migrated_path:
        display = migrated.get("display")
        if isinstance(display, dict):
            icon = display.get("icon")
            if isinstance(icon, dict) and "item" in icon and "id" not in icon:
                icon["id"] = icon.pop("item")
    if "/recipe/" in migrated_path:
        result = migrated.get("result")
        if isinstance(result, dict) and "item" in result and "id" not in result:
            result["id"] = result.pop("item")
    return migrated


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-ref", default="origin/master")
    parser.add_argument("--report", default="build/reports/port-parity/report.json")
    args = parser.parse_args()

    base_files = tracked_at(args.base_ref)
    port_files = current_tracked()
    relevant_base = {path for path in base_files if relevant(path)}
    migrated_files = {
        old: replacement
        for old, replacement in MIGRATED_FILES.items()
        if old in relevant_base and old not in port_files and replacement in port_files
    }
    mapped_resources = {
        path: migrated_resource_path(path)
        for path in relevant_base
        if path.startswith("src/main/resources/data/")
        and path != migrated_resource_path(path)
        and migrated_resource_path(path) in port_files
    }

    missing_files = sorted(
        path for path in relevant_base
        if expected_port_path(path, port_files) is None
    )

    base_java = sorted(path for path in relevant_base if gameplay_java(path))
    missing_java = sorted(
        path for path in base_java
        if expected_port_path(path, port_files) is None
    )

    init_java = [
        path for path in base_java
        if "/init/" in path and path.endswith(".java") and path in port_files
    ]
    base_ids = registry_ids(read_at(args.base_ref, path) for path in init_java)
    port_ids = registry_ids(
        (ROOT / path).read_text(encoding="utf-8", errors="replace")
        for path in init_java
    )
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

    changed_resources: list[str] = []
    invalid_resources: list[str] = []
    for base_path in sorted(
        path for path in relevant_base
        if path.startswith("src/main/resources/data/")
    ):
        port_path = expected_port_path(base_path, port_files)
        if port_path is None:
            continue
        try:
            if base_path.endswith(".json"):
                expected = migrate_expected_json(
                    base_path, semantic_json(read_at(args.base_ref, base_path))
                )
                actual = semantic_json(
                    (ROOT / port_path).read_text(encoding="utf-8")
                )
                if expected != actual:
                    changed_resources.append(f"{base_path} -> {port_path}")
            elif read_bytes_at(args.base_ref, base_path) != (ROOT / port_path).read_bytes():
                changed_resources.append(f"{base_path} -> {port_path}")
        except (json.JSONDecodeError, UnicodeDecodeError, OSError) as exc:
            invalid_resources.append(f"{base_path} -> {port_path}: {exc}")

    report = {
        "base_ref": args.base_ref,
        "base_feature_files": len(relevant_base),
        "base_gameplay_java_files": len(base_java),
        "base_registry_ids": len(base_ids),
        "migrated_files": migrated_files,
        "mapped_resource_count": len(mapped_resources),
        "missing_files": missing_files,
        "missing_gameplay_java": missing_java,
        "missing_registry_ids": missing_registry_ids,
        "changed_config_json": changed_configs,
        "invalid_config_json": invalid_configs,
        "changed_data_resources": changed_resources,
        "invalid_data_resources": invalid_resources,
    }

    report_path = ROOT / args.report
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")

    print(json.dumps(report, indent=2))
    failures = (
        missing_files
        or missing_java
        or missing_registry_ids
        or changed_configs
        or invalid_configs
        or changed_resources
        or invalid_resources
    )
    if failures:
        print("Port feature parity validation failed.")
        return 1
    print("Port preserves the tracked SCP Additions 3.0.7 feature surface.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
