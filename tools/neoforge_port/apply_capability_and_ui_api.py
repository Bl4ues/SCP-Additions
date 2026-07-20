from __future__ import annotations

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java"


def target(relative: str) -> Path:
    return JAVA / relative


def read(relative: str) -> str:
    return target(relative).read_text(encoding="utf-8")


def write(relative: str, text: str) -> None:
    path = target(relative)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def migrate_scp294_block_entity(relative: str) -> None:
    text = read(relative)
    text = text.replace("import com.bl4ues.scpadditions.compat.LazyOptional;\n", "")
    text = text.replace("import net.neoforged.neoforge.capabilities.ForgeCapabilities;\n", "")
    text = text.replace("import net.neoforged.neoforge.capabilities.Capability;\n", "")
    text = text.replace(
        "private final LazyOptional<? extends IItemHandler>[] handlers = SidedInvWrapper.create(this, Direction.values());",
        "private final IItemHandler[] handlers = new IItemHandler[] {\n"
        "\t\t\tnew SidedInvWrapper(this, Direction.DOWN),\n"
        "\t\t\tnew SidedInvWrapper(this, Direction.UP),\n"
        "\t\t\tnew SidedInvWrapper(this, Direction.NORTH),\n"
        "\t\t\tnew SidedInvWrapper(this, Direction.SOUTH),\n"
        "\t\t\tnew SidedInvWrapper(this, Direction.WEST),\n"
        "\t\t\tnew SidedInvWrapper(this, Direction.EAST)\n"
        "\t};\n"
        "\tprivate final IItemHandler unsidedHandler = new SidedInvWrapper(this, null);"
    )
    text = re.sub(
        r"\n\t@Override\n\tpublic <T> LazyOptional<T> getCapability\(Capability<T> capability, @Nullable Direction facing\) \{.*?\n\t\}\n",
        "\n",
        text,
        flags=re.S,
    )
    text = re.sub(
        r"\n\t@Override\n\tpublic void setRemoved\(\) \{.*?\n\t\}\n",
        "\n",
        text,
        flags=re.S,
    )
    if "public IItemHandler getItemHandler" not in text:
        text = text.rstrip()[:-1] + '''

\tpublic IItemHandler getItemHandler(@Nullable Direction side) {
\t\treturn side == null ? unsidedHandler : handlers[side.ordinal()];
\t}
}
'''
    write(relative, text)


for file_name in (
        "Scp294BlockEntity.java",
        "Scp294StockingBlockEntity.java",
        "Scp294OutOfRangeBlockEntity.java"):
    migrate_scp294_block_entity(
        "net/mcreator/scpadditions/block/entity/" + file_name)

write("net/mcreator/scpadditions/block/entity/Scp294Capabilities.java", '''package net.mcreator.scpadditions.block.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.mcreator.scpadditions.init.ScpAdditionsModBlockEntities;

/** NeoForge item-handler providers for all three SCP-294 block states. */
public final class Scp294Capabilities {
    private Scp294Capabilities() {
    }

    @SuppressWarnings("unchecked")
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                (BlockEntityType<Scp294BlockEntity>) (BlockEntityType<?>)
                        ScpAdditionsModBlockEntities.SCP_294.get(),
                (blockEntity, side) -> blockEntity.getItemHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                (BlockEntityType<Scp294StockingBlockEntity>) (BlockEntityType<?>)
                        ScpAdditionsModBlockEntities.SCP_294_STOCKING.get(),
                (blockEntity, side) -> blockEntity.getItemHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                (BlockEntityType<Scp294OutOfRangeBlockEntity>) (BlockEntityType<?>)
                        ScpAdditionsModBlockEntities.SCP_294_OUT_OF_RANGE.get(),
                (blockEntity, side) -> blockEntity.getItemHandler(side));
    }
}
''')

mod = read("net/mcreator/scpadditions/ScpAdditionsMod.java")
if "import net.mcreator.scpadditions.block.entity.Scp294Capabilities;" not in mod:
    mod = mod.replace(
        "import net.mcreator.scpadditions.init.ScpAdditionsModBlockEntities;",
        "import net.mcreator.scpadditions.init.ScpAdditionsModBlockEntities;\n"
        "import net.mcreator.scpadditions.block.entity.Scp294Capabilities;")
if "bus.addListener(Scp294Capabilities::register);" not in mod:
    mod = mod.replace(
        "ScpAdditionsModBlockEntities.REGISTRY.register(bus);",
        "ScpAdditionsModBlockEntities.REGISTRY.register(bus);\n"
        "        bus.addListener(Scp294Capabilities::register);",
        1,
    )
write("net/mcreator/scpadditions/ScpAdditionsMod.java", mod)

menu = read("net/mcreator/scpadditions/world/inventory/Scp294GuiMenu.java")
menu = menu.replace(
    "import net.neoforged.neoforge.capabilities.ForgeCapabilities;",
    "import net.neoforged.neoforge.capabilities.Capabilities;")
menu = re.sub(
    r"itemstack\.getCapability\(ForgeCapabilities\.ITEM_HANDLER, null\)\.ifPresent\(capability -> \{\n\s*this\.internal = capability;\n\s*this\.bound = true;\n\s*\}\);",
    "IItemHandler capability = itemstack.getCapability(\n"
    "                        Capabilities.ItemHandler.ITEM);\n"
    "                if (capability != null) {\n"
    "                    this.internal = capability;\n"
    "                    this.bound = true;\n"
    "                }",
    menu,
)
menu = re.sub(
    r"boundEntity\.getCapability\(ForgeCapabilities\.ITEM_HANDLER, null\)\.ifPresent\(capability -> \{\n\s*this\.internal = capability;\n\s*this\.bound = true;\n\s*\}\);",
    "{\n"
    "                    IItemHandler capability = boundEntity.getCapability(\n"
    "                            Capabilities.ItemHandler.ENTITY);\n"
    "                    if (capability != null) {\n"
    "                        this.internal = capability;\n"
    "                        this.bound = true;\n"
    "                    }\n"
    "                }",
    menu,
)
menu = re.sub(
    r"boundBlockEntity\.getCapability\(ForgeCapabilities\.ITEM_HANDLER, null\)\.ifPresent\(capability -> \{\n\s*this\.internal = capability;\n\s*this\.bound = true;\n\s*\}\);",
    "{\n"
    "                    IItemHandler capability = this.world.getCapability(\n"
    "                            Capabilities.ItemHandler.BLOCK, pos, null);\n"
    "                    if (capability != null) {\n"
    "                        this.internal = capability;\n"
    "                        this.bound = true;\n"
    "                    }\n"
    "                }",
    menu,
)
write("net/mcreator/scpadditions/world/inventory/Scp294GuiMenu.java", menu)

honey = read("net/mcreator/scpadditions/procedures/Scp1176OnBlockRightClickedProcedure.java")
honey = honey.replace(
    "import net.neoforged.neoforge.capabilities.ForgeCapabilities;",
    "import net.neoforged.neoforge.capabilities.Capabilities;")
honey = re.sub(
    r"entity\.getCapability\(ForgeCapabilities\.ITEM_HANDLER, null\)\.ifPresent\(capability -> \{\n\s*if \(capability instanceof IItemHandlerModifiable _modHandler\)\n\s*_modHandler\.setStackInSlot\(_slotid, _setstack\);\n\s*\}\);",
    "var capability = entity.getCapability(Capabilities.ItemHandler.ENTITY);\n"
    "                if (capability instanceof IItemHandlerModifiable _modHandler)\n"
    "                    _modHandler.setStackInSlot(_slotid, _setstack);",
    honey,
)
write("net/mcreator/scpadditions/procedures/Scp1176OnBlockRightClickedProcedure.java", honey)

# Straight 1.21.1 name/signature migrations.
for java_file in JAVA.rglob("*.java"):
    text = java_file.read_text(encoding="utf-8")
    original = text

    text = text.replace(".saturationMod(", ".saturationModifier(")
    text = re.sub(
        r"BuiltInRegistries\.([A-Z0-9_]+)\.getValue\(",
        r"BuiltInRegistries.\1.get(",
        text,
    )
    text = re.sub(
        r"new ResourceLocation\(([^,\n]+?\.trim\(\))\)",
        r"ResourceLocation.parse(\1)",
        text,
    )

    signature = re.compile(
        r"public void appendHoverText\(ItemStack (\w+),\s*"
        r"(?:@Nullable\s+)?Level\s+(\w+),\s*"
        r"List<Component>\s+(\w+),\s*TooltipFlag\s+(\w+)\)"
    )
    text = signature.sub(
        r"public void appendHoverText(ItemStack \1, Item.TooltipContext context, "
        r"List<Component> \3, TooltipFlag \4)",
        text,
    )
    text = re.sub(
        r"super\.appendHoverText\(([^,]+),\s*[^,]+,\s*([^,]+),\s*([^)]+)\);",
        r"super.appendHoverText(\1, context, \2, \3);",
        text,
    )

    duration_pattern = re.compile(
        r"public int getUseDuration\(ItemStack (\w+)\)"
    )
    if duration_pattern.search(text):
        text = duration_pattern.sub(
            r"public int getUseDuration(ItemStack \1, LivingEntity user)", text)
        if "import net.minecraft.world.entity.LivingEntity;" not in text:
            package_end = text.find(";", text.find("package "))
            text = (text[:package_end + 1]
                    + "\n\nimport net.minecraft.world.entity.LivingEntity;"
                    + text[package_end + 1:])

    if text != original:
        java_file.write_text(text, encoding="utf-8")

# Screen and input signatures.
for relative in (
        "com/bl4ues/scpinventory/client/gui/ScpInventoryScreen.java",
        "com/bl4ues/scpinventory/client/gui/ItemConfigScreen.java"):
    text = read(relative).replace(
        "renderBackground(g);",
        "renderBackground(g, mouseX, mouseY, partialTick);")
    write(relative, text)

screen = read("com/bl4ues/scpinventory/client/gui/ScpInventoryScreen.java")
screen = screen.replace(
    "public boolean mouseScrolled(double mouseX, double mouseY, double delta)",
    "public boolean mouseScrolled(double mouseX, double mouseY, "
    "double scrollX, double scrollY)")
screen = screen.replace("mouseScrolled(mouseX, mouseY, delta)",
                        "mouseScrolled(mouseX, mouseY, scrollY)")
screen = screen.replace("itemList.mouseScrolled(delta)",
                        "itemList.mouseScrolled(scrollY)")
screen = screen.replace("super.mouseScrolled(mouseX, mouseY, delta)",
                        "super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)")
write("com/bl4ues/scpinventory/client/gui/ScpInventoryScreen.java", screen)

context_screen = read("com/bl4ues/scpinventory/client/gui/ContextConfigScreen.java")
context_screen = re.sub(r"\s*\w+Box\.tick\(\);", "", context_screen)
context_screen = context_screen.replace(
    "public boolean mouseScrolled(double mouseX, double mouseY, double delta)",
    "public boolean mouseScrolled(double mouseX, double mouseY, "
    "double scrollX, double scrollY)")
context_screen = context_screen.replace("super.mouseScrolled(mouseX, mouseY, delta)",
                                        "super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)")
write("com/bl4ues/scpinventory/client/gui/ContextConfigScreen.java", context_screen)

craft_input = read("com/bl4ues/scpinventory/client/CraftingInputHandler.java")
craft_input = craft_input.replace("event.getScrollDelta()", "event.getScrollDeltaY()")
write("com/bl4ues/scpinventory/client/CraftingInputHandler.java", craft_input)

scrollable = read("com/bl4ues/scpinventory/client/gui/components/ScrollableItemList.java")
scrollable = scrollable.replace(
    "stack.getTooltipLines(mc.player, TooltipFlag.Default.NORMAL)",
    "stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, "
    "TooltipFlag.Default.NORMAL)")
if "import net.minecraft.world.item.Item;" not in scrollable:
    scrollable = scrollable.replace(
        "import net.minecraft.world.item.ItemStack;",
        "import net.minecraft.world.item.Item;\n"
        "import net.minecraft.world.item.ItemStack;")
write("com/bl4ues/scpinventory/client/gui/components/ScrollableItemList.java", scrollable)

# Holder-based effects and attributes.
status = read("com/bl4ues/scpinventory/client/gui/components/StatusPanel.java")
status = status.replace("effect.getEffect().getDisplayName()",
                        "effect.getEffect().value().getDisplayName()")
status = status.replace("effect.getEffect().getCategory()",
                        "effect.getEffect().value().getCategory()")
status = status.replace(
    "effect.getEffect() == ScpAdditionsModMobEffects.SCP_1176_HONEYED.get()",
    "effect.getEffect().value() == ScpAdditionsModMobEffects.SCP_1176_HONEYED.get()")
status = status.replace(
    "BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect())",
    "BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value())")
status = status.replace(
    "private String formatAttribute(Attribute attribute)",
    "private String formatAttribute(net.minecraft.core.Holder<Attribute> attribute)")
status = status.replace(
    "InventoryScreen.renderEntityInInventoryFollowsMouse(g, previewX + previewW / 2, playerPreviewY, 54,\n"
    "                previewX + previewW / 2 - mouseX, previewY + 32 - mouseY, mc.player);",
    "InventoryScreen.renderEntityInInventoryFollowsMouse(g, previewX, previewY,\n"
    "                previewX + previewW, previewY + previewH, 54, 0.0F,\n"
    "                mouseX, mouseY, mc.player);")
write("com/bl4ues/scpinventory/client/gui/components/StatusPanel.java", status)

timeline = read("com/bl4ues/scpinventory/client/StatusEffectTimelineClient.java")
timeline = timeline.replace(
    "BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect())",
    "BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value())")
write("com/bl4ues/scpinventory/client/StatusEffectTimelineClient.java", timeline)

pickup = read("com/bl4ues/scpinventory/client/PickupPromptClient.java")
pickup = pickup.replace(
    "mc.getFrameTime()",
    "mc.getTimer().getGameTimeDeltaPartialTick(false)")
write("com/bl4ues/scpinventory/client/PickupPromptClient.java", pickup)

print("Applied NeoForge capability, tooltip, input, and GUI API migrations")
