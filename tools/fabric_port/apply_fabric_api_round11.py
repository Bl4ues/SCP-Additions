from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
changed: list[str] = []


def edit(rel: str, transform) -> None:
    path = ROOT / rel
    old = path.read_text(encoding="utf-8")
    new = transform(old)
    if new != old:
        path.write_text(new, encoding="utf-8")
        changed.append(rel)


def patch_main(text: str) -> str:
    if "EntityAttributeCreationEvent" not in text:
        text = text.replace(
            "import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;",
            "import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;\n"
            "import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;\n"
            "import net.neoforged.neoforge.registries.RegisterEvent;",
        )
    needle = "        FabricSubscriberBootstrap.registerAll(MOD_BUS);\n"
    replacement = (
        needle
        + "        MOD_BUS.post(new RegisterEvent());\n"
        + "        MOD_BUS.post(new EntityAttributeCreationEvent());\n"
    )
    if "MOD_BUS.post(new RegisterEvent())" not in text:
        text = text.replace(needle, replacement)
    return text


edit(
    "src/main/java/net/mcreator/scpadditions/fabric/ScpAdditionsFabric.java",
    patch_main,
)


def patch_client(text: str) -> str:
    if "ItemTooltipCallback" not in text:
        text = text.replace(
            "import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;",
            "import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;\n"
            "import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;",
        )
    needle = "        modBus.post(new RegisterGuiLayersEvent());\n"
    registration = (
        needle
        + "        ItemTooltipCallback.EVENT.register((stack, context, type, lines) ->\n"
        + "                NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.ItemTooltipEvent(stack, lines)));\n"
    )
    if "ItemTooltipCallback.EVENT.register" not in text:
        text = text.replace(needle, registration)
    return text


edit(
    "src/main/java/net/mcreator/scpadditions/fabric/FabricClientEventBridge.java",
    patch_client,
)

print(f"Fabric API round 11 changed {len(changed)} files")
for item in changed:
    print(item)
