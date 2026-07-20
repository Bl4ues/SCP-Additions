from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA_ROOT = ROOT / "src/main/java"

changed = 0
for path in JAVA_ROOT.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    updated = text.replace(
        "ItemStack.isSameItemSameTags(",
        "ItemStack.isSameItemSameComponents(",
    )

    if ".isEdible()" in updated:
        updated = updated.replace("stack.isEdible()", "stack.has(DataComponents.FOOD)")
        if "DataComponents.FOOD" in updated and "import net.minecraft.core.component.DataComponents;" not in updated:
            package_end = updated.find("\n", updated.find("package ")) + 1
            updated = (
                updated[:package_end]
                + "\nimport net.minecraft.core.component.DataComponents;\n"
                + updated[package_end:]
            )

    if updated != text:
        path.write_text(updated, encoding="utf-8")
        changed += 1

print(f"Updated {changed} files for Minecraft 1.21 item API names")
