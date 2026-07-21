#!/usr/bin/env python3
"""Restore loader-port content that must remain equivalent to Forge 1.20.1."""

from __future__ import annotations

import argparse
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MASTER_REF = "origin/master"
NEOFORGE_REF = "origin/neoforge-1.21.1-port-3.0.7"

KEYCARD_RECIPES = (
    "lv_2_kr_left.json",
    "lv_2_lto_r.json",
    "lv_2_rto_l.json",
    "lv_3_kr_left.json",
    "lv_3_kr_right.json",
    "lv_3_lto_r.json",
    "lv_3_rto_l.json",
    "lv_4_kr_left.json",
    "lv_4_kr_right.json",
    "lv_4_lto_r.json",
    "lv_4_rto_l.json",
    "lv_5_kr_left.json",
    "lv_5_kr_right.json",
    "lv_5_lto_r.json",
    "lv_5_rto_l.json",
    "lv_6_kr_left.json",
    "lv_6_kr_right.json",
    "lv_6_lto_r.json",
    "lv_6_rto_l.json",
)

TESLA_SHARED_FILES = (
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGateVolume.java",
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGatePulseHelper.java",
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGateUpdateTickProcedure.java",
)


def git_show(ref: str, path: str) -> bytes:
    return subprocess.check_output(["git", "show", f"{ref}:{path}"], cwd=ROOT)


def write_bytes(path: str, content: bytes) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(content)


def patch_inventory_and_vitals() -> None:
    inventory = ROOT / "src/main/java/com/bl4ues/scpinventory/client/gui/ScpInventoryScreen.java"
    source = inventory.read_text(encoding="utf-8")
    old = "        renderBackground(g, mouseX, mouseY, partialTick);"
    new = (
        "        // Preserve the sharp 1.20.1 inventory presentation; the 1.21 helper blurs the world.\n"
        "        renderTransparentBackground(g);"
    )
    if old in source:
        source = source.replace(old, new, 1)
    elif new not in source:
        raise RuntimeError("ScpInventoryScreen background call was not found")
    inventory.write_text(source, encoding="utf-8")

    vitals = ROOT / "src/main/java/net/mcreator/scpadditions/vitals/client/PlayerVitalsOverlay.java"
    source = vitals.read_text(encoding="utf-8")
    constants_old = "    private static final int BAR_GAP = 18;"
    constants_new = """    private static final int BAR_GAP = 18;
    // The established presentation corresponds to 360 GUI pixels (720p at GUI scale 2).
    // Normalizing to that height prevents Minecraft 1.21's auto-selected GUI scale from
    // making the physical HUD drastically larger or smaller than the Forge edition.
    private static final float REFERENCE_GUI_HEIGHT = 360.0F;
    private static final float MIN_HUD_SCALE = 0.50F;
    private static final float MAX_HUD_SCALE = 2.00F;"""
    if "REFERENCE_GUI_HEIGHT" not in source:
        if constants_old not in source:
            raise RuntimeError("PlayerVitalsOverlay constants anchor was not found")
        source = source.replace(constants_old, constants_new, 1)

    row_old = "        int rowY = screenHeight - BOTTOM_MARGIN;"
    row_new = """        float hudScale = Math.max(MIN_HUD_SCALE,
                Math.min(MAX_HUD_SCALE, screenHeight / REFERENCE_GUI_HEIGHT));
        int logicalScreenHeight = Math.max(1, Math.round(screenHeight / hudScale));
        graphics.pose().pushPose();
        graphics.pose().scale(hudScale, hudScale, 1.0F);

        int rowY = logicalScreenHeight - BOTTOM_MARGIN;"""
    if "int logicalScreenHeight" not in source:
        if row_old not in source:
            raise RuntimeError("PlayerVitalsOverlay row anchor was not found")
        source = source.replace(row_old, row_new, 1)

    end_old = """            graphics.drawString(minecraft.font, healthText,
                    BAR_X + 6, rowY + 3, TEXT, false);
        }
    }

    private static void drawBar"""
    end_new = """            graphics.drawString(minecraft.font, healthText,
                    BAR_X + 6, rowY + 3, TEXT, false);
        }

        graphics.pose().popPose();
    }

    private static void drawBar"""
    if "graphics.pose().popPose();\n    }\n\n    private static void drawBar" not in source:
        if end_old not in source:
            raise RuntimeError("PlayerVitalsOverlay method-end anchor was not found")
        source = source.replace(end_old, end_new, 1)
    vitals.write_text(source, encoding="utf-8")


def restore_keycard_recipes() -> None:
    destination = ROOT / "src/main/resources/data/scp_additions/recipe"
    destination.mkdir(parents=True, exist_ok=True)
    for name in KEYCARD_RECIPES:
        legacy_path = f"src/main/resources/data/scp_additions/recipes/{name}"
        value = json.loads(git_show(MASTER_REF, legacy_path).decode("utf-8"))
        result = value.get("result")
        if isinstance(result, dict) and "item" in result and "id" not in result:
            result["id"] = result.pop("item")
        (destination / name).write_text(
            json.dumps(value, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )

    build_gradle = ROOT / "build.gradle"
    source = build_gradle.read_text(encoding="utf-8")
    filtered = []
    for line in source.splitlines():
        stripped = line.strip()
        if stripped.startswith("exclude('data/scp_additions/recipes/lv_"):
            continue
        filtered.append(line)
    build_gradle.write_text("\n".join(filtered) + "\n", encoding="utf-8")

    migrator = ROOT / "tools/port_1_21/migrate_data_resources.py"
    source = migrator.read_text(encoding="utf-8")
    start = source.index("LEGACY_RECIPE_GLOBS = (")
    end = source.index("\n\n\ndef merge_move", start)
    replacement = """LEGACY_RECIPE_GLOBS: tuple[str, ...] = ()
LEGACY_RECIPE_FILES = (
    \"button_left_swap.json\",
    \"button_right_swap.json\",
)"""
    source = source[:start] + replacement + source[end:]
    migrator.write_text(source, encoding="utf-8")


def restore_shared_configuration() -> None:
    path = "config/scpinventory/context_interactions.json"
    write_bytes(path, git_show(MASTER_REF, path))


def restore_fabric_shared_tesla_logic() -> None:
    for path in TESLA_SHARED_FILES:
        write_bytes(path, git_show(NEOFORGE_REF, path))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--loader", choices=("neoforge", "fabric"), required=True)
    args = parser.parse_args()

    patch_inventory_and_vitals()
    restore_keycard_recipes()
    restore_shared_configuration()
    if args.loader == "fabric":
        restore_fabric_shared_tesla_logic()

    print(f"Restored master feature and presentation parity for {args.loader}.")


if __name__ == "__main__":
    main()
