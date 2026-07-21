#!/usr/bin/env python3
from __future__ import annotations

import json
import shutil
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
DATA_ROOT = ROOT / "src/main/resources/data"
PACK_META = ROOT / "src/main/resources/pack.mcmeta"

DIRECTORY_RENAMES = {
    "advancements": "advancement",
    "recipes": "recipe",
    "loot_tables": "loot_table",
    "structures": "structure",
    "functions": "function",
    "predicates": "predicate",
    "item_modifiers": "item_modifier",
}

TAG_RENAMES = {
    "blocks": "block",
    "items": "item",
    "fluids": "fluid",
    "entity_types": "entity_type",
    "functions": "function",
    "game_events": "game_event",
    "damage_types": "damage_type",
}

LEGACY_RECIPE_GLOBS: tuple[str, ...] = ()
LEGACY_RECIPE_FILES = (
    "button_left_swap.json",
    "button_right_swap.json",
)


def merge_move(source: Path, target: Path) -> None:
    if not source.exists():
        return
    target.mkdir(parents=True, exist_ok=True)
    for child in source.iterdir():
        destination = target / child.name
        if child.is_dir():
            merge_move(child, destination)
        else:
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(child, destination)
    shutil.rmtree(source)


def rewrite_json(path: Path) -> None:
    try:
        data: Any = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exception:
        raise RuntimeError(f"Invalid JSON before 1.21 migration: {path}") from exception

    relative_parts = path.relative_to(DATA_ROOT).parts
    if "advancement" in relative_parts and isinstance(data, dict):
        display = data.get("display")
        if isinstance(display, dict):
            icon = display.get("icon")
            if isinstance(icon, dict) and "item" in icon and "id" not in icon:
                icon["id"] = icon.pop("item")

    if "recipe" in relative_parts and isinstance(data, dict):
        result = data.get("result")
        if isinstance(result, dict) and "item" in result and "id" not in result:
            result["id"] = result.pop("item")

    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def migrate_namespace(namespace: Path) -> None:
    for old_name, new_name in DIRECTORY_RENAMES.items():
        merge_move(namespace / old_name, namespace / new_name)

    tags = namespace / "tags"
    if tags.exists():
        for old_name, new_name in TAG_RENAMES.items():
            merge_move(tags / old_name, tags / new_name)

    recipe_dir = namespace / "recipe"
    if namespace.name == "scp_additions" and recipe_dir.exists():
        for pattern in LEGACY_RECIPE_GLOBS:
            for path in recipe_dir.glob(pattern):
                path.unlink()
        for filename in LEGACY_RECIPE_FILES:
            path = recipe_dir / filename
            if path.exists():
                path.unlink()

    for path in namespace.rglob("*.json"):
        rewrite_json(path)


def main() -> None:
    if DATA_ROOT.exists():
        for namespace in DATA_ROOT.iterdir():
            if namespace.is_dir():
                migrate_namespace(namespace)

    PACK_META.write_text(
        json.dumps(
            {
                "pack": {
                    "pack_format": 48,
                    "supported_formats": [34, 48],
                    "description": "SCP Additions resources for Minecraft 1.21.1",
                }
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
