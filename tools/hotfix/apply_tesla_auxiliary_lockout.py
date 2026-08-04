from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

def replace(path: str, old: str, new: str, count: int = -1) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"Missing replacement anchor in {path}: {old[:140]!r}")
    target.write_text(text.replace(old, new, count), encoding="utf-8")

block = "src/main/java/net/mcreator/scpadditions/block/TeslaTerminalBlockBlock.java"
replace(block,
        "import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;",
        "import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;\nimport net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;")
replace(block,
        "\t\t\tboolean hasSecurityCredentials = TeslaTerminalController.hasSecurityCredentials(player);",
        "\t\t\tboolean hasSecurityCredentials = TeslaTerminalController.hasSecurityCredentials(player);\n\t\t\tboolean auxiliaryPowerOnline = Scp079FacilityAccessManager\n\t\t\t\t\t.isAuxiliaryPowerOnline(world);")
replace(block,
        "\t\t\t\t\tdata.writeBoolean(hasSecurityCredentials);\n\t\t\t\t\treturn new TeslaTerminalMenu(id, inventory, data);",
        "\t\t\t\t\tdata.writeBoolean(hasSecurityCredentials);\n\t\t\t\t\tdata.writeBoolean(auxiliaryPowerOnline);\n\t\t\t\t\treturn new TeslaTerminalMenu(id, inventory, data);")
replace(block,
        "\t\t\t\tdata.writeBoolean(hasSecurityCredentials);\n\t\t\t});",
        "\t\t\t\tdata.writeBoolean(hasSecurityCredentials);\n\t\t\t\tdata.writeBoolean(auxiliaryPowerOnline);\n\t\t\t});")

menu = "src/main/java/net/mcreator/scpadditions/world/inventory/TeslaTerminalMenu.java"
replace(menu,
        "import net.minecraft.world.inventory.AbstractContainerMenu;",
        "import net.minecraft.world.inventory.AbstractContainerMenu;\nimport net.minecraft.world.inventory.DataSlot;")
replace(menu,
        "import net.mcreator.scpadditions.init.ScpAdditionsModMenus;",
        "import net.mcreator.scpadditions.init.ScpAdditionsModMenus;\nimport net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;")
replace(menu,
        "\tpublic boolean initialHasSecurityCredentials = false;",
        "\tpublic boolean initialHasSecurityCredentials = false;\n\tpublic boolean auxiliaryPowerOnline = false;")
replace(menu,
        "\t\t\tif (extraData.readableBytes() >= 1) {\n\t\t\t\tthis.initialHasSecurityCredentials = extraData.readBoolean();\n\t\t\t}\n\t\t\taccess = ContainerLevelAccess.create(world, pos);\n\t\t}\n\t}",
        "\t\t\tif (extraData.readableBytes() >= 1) {\n\t\t\t\tthis.initialHasSecurityCredentials = extraData.readBoolean();\n\t\t\t}\n\t\t\tif (extraData.readableBytes() >= 1) {\n\t\t\t\tthis.auxiliaryPowerOnline = extraData.readBoolean();\n\t\t\t}\n\t\t\taccess = ContainerLevelAccess.create(world, pos);\n\t\t}\n\n\t\tthis.addDataSlot(new DataSlot() {\n\t\t\t@Override\n\t\t\tpublic int get() {\n\t\t\t\tif (TeslaTerminalMenu.this.world.isClientSide) {\n\t\t\t\t\treturn TeslaTerminalMenu.this.auxiliaryPowerOnline ? 1 : 0;\n\t\t\t\t}\n\t\t\t\treturn Scp079FacilityAccessManager.isAuxiliaryPowerOnline(\n\t\t\t\t\t\tTeslaTerminalMenu.this.world) ? 1 : 0;\n\t\t\t}\n\n\t\t\t@Override\n\t\t\tpublic void set(int value) {\n\t\t\t\tTeslaTerminalMenu.this.auxiliaryPowerOnline = value != 0;\n\t\t\t}\n\t\t});\n\t}")

screen = "src/main/java/net/mcreator/scpadditions/client/gui/TeslaTerminalScreen.java"
replace(screen,
        "\tprivate static final ResourceLocation SCREEN_ON_OVERRIDE = screen(\"11\");",
        "\tprivate static final ResourceLocation SCREEN_ON_OVERRIDE = screen(\"11\");\n\tprivate static final ResourceLocation SCREEN_AUXILIARY_OFFLINE = screen(\"12\");")
replace(screen,
        "\tprivate boolean clickVariant = false;",
        "\tprivate boolean clickVariant = false;\n\tprivate boolean lastAuxiliaryPowerOnline = false;")
replace(screen,
        "\t\tClientNetwork.requestInventorySync();\n\t}",
        "\t\tClientNetwork.requestInventorySync();\n\t\tlastAuxiliaryPowerOnline = menu.auxiliaryPowerOnline;\n\t}")
replace(screen,
        "\t\t}\n\n\t\tguiGraphics.pose().popPose();",
        "\t\t}\n\n\t\tif (!menu.auxiliaryPowerOnline) {\n\t\t\tRenderSystem.setShaderColor(1, 1, 1, 1);\n\t\t\tguiGraphics.blit(SCREEN_AUXILIARY_OFFLINE, 0, 0, 0, 0,\n\t\t\t\t\tTEX_W, TEX_H, TEX_W, TEX_H);\n\t\t}\n\n\t\tguiGraphics.pose().popPose();", 1)
replace(screen,
        "\t\tif (button != 0) {\n\t\t\treturn true;\n\t\t}\n\t\tdouble tx = textureX(mouseX);",
        "\t\tif (button != 0) {\n\t\t\treturn true;\n\t\t}\n\t\tif (!menu.auxiliaryPowerOnline) {\n\t\t\treturn true;\n\t\t}\n\t\tdouble tx = textureX(mouseX);")
replace(screen,
        "\tpublic void containerTick() {\n\t\tsuper.containerTick();\n\t\tif (visualTimer > 0) {",
        "\tpublic void containerTick() {\n\t\tsuper.containerTick();\n\t\tboolean auxiliaryOnline = menu.auxiliaryPowerOnline;\n\t\tif (auxiliaryOnline != lastAuxiliaryPowerOnline) {\n\t\t\tlastAuxiliaryPowerOnline = auxiliaryOnline;\n\t\t\tauthenticated = false;\n\t\t\tresetToMain();\n\t\t}\n\t\tif (!auxiliaryOnline) {\n\t\t\treturn;\n\t\t}\n\t\tif (visualTimer > 0) {")

for path in [
    ROOT / ".github/workflows/apply-tesla-auxiliary-lockout.yml",
    ROOT / "tools/hotfix/APPLY_TESLA_AUXILIARY_LOCKOUT",
    ROOT / "tools/hotfix/apply_tesla_auxiliary_lockout.py",
]:
    if path.exists():
        path.unlink()
