from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
path = ROOT / "src/main/java/net/mcreator/scpadditions/fabric/mixin/ResultSlotCraftedMixin.java"
text = path.read_text(encoding="utf-8")
updated = text.replace(
    "import net.minecraft.world.Container;",
    "import net.minecraft.world.inventory.CraftingContainer;",
).replace(
    "@Shadow private Container craftSlots;",
    '@Shadow(aliases = "field_7870") private CraftingContainer craftSlots;',
)
if updated == text:
    if 'aliases = "field_7870"' not in text:
        raise RuntimeError("Could not locate ResultSlot crafting-grid shadow")
    print("ResultSlot crafting-grid shadow already mapped")
else:
    path.write_text(updated, encoding="utf-8")
    print(f"Mapped ResultSlot crafting-grid shadow in {path.relative_to(ROOT)}")
