#!/usr/bin/env python3
from __future__ import annotations

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


def remove_local_reload(text: str) -> str:
    return text.replace(
        "            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ItemConfigReloadPacket());\n",
        "",
    )


edit(
    "src/main/java/com/bl4ues/scpinventory/network/ItemConfigSavePacket.java",
    remove_local_reload,
)
edit(
    "src/main/java/com/bl4ues/scpinventory/network/ItemConfigDeletePacket.java",
    remove_local_reload,
)


def patch_context_reload(text: str) -> str:
    marker = "ModNetwork.syncServerConfig(source.getServer().getPlayerList().getPlayers());"
    method_start = "    public static int reload(CommandSourceStack source) {"
    start = text.find(method_start)
    if start < 0:
        raise RuntimeError("ContextConfigManager.reload was not found")
    end = text.find("\n    }", start)
    if end < 0:
        raise RuntimeError("ContextConfigManager.reload end was not found")
    method = text[start:end]
    if marker in method:
        return text
    needle = "        ContextInteractionRegistry.reload();\n"
    if needle not in method:
        raise RuntimeError("ContextInteractionRegistry.reload call was not found")
    patched = method.replace(needle, needle + "        " + marker + "\n", 1)
    return text[:start] + patched + text[end:]


edit(
    "src/main/java/com/bl4ues/scpinventory/context/ContextConfigManager.java",
    patch_context_reload,
)


def patch_global_reload(text: str) -> str:
    marker = "ModNetwork.syncServerConfig(source.getServer().getPlayerList().getPlayers());"
    if marker in text:
        return text
    needle = "            ModNetwork.syncModuleState(source.getServer().getPlayerList().getPlayers());\n"
    if needle not in text:
        raise RuntimeError("Global module-state reload synchronization was not found")
    return text.replace(needle, needle + "            " + marker + "\n", 1)


edit(
    "src/main/java/net/mcreator/scpadditions/config/ScpAdditionsReloadCommand.java",
    patch_global_reload,
)


def patch_weapon_mirror(text: str) -> str:
    import_line = "import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;\n"
    if import_line not in text:
        needle = "import net.minecraft.network.FriendlyByteBuf;\n"
        if needle not in text:
            raise RuntimeError("InventoryActionPacket FriendlyByteBuf import was not found")
        text = text.replace(needle, needle + import_line, 1)

    send_line = "player.connection.send(new ClientboundSetCarriedItemPacket(mirrorSlot));"
    if send_line in text:
        return text
    old = (
        "        inventory.setItem(mirrorSlot, copy);\n"
        "        if (mirrorSlot < VANILLA_HOTBAR_END_EXCLUSIVE) inventory.selected = mirrorSlot;\n"
        "        ScpPickupRouter.syncVanillaInventory(player);\n"
    )
    new = (
        "        inventory.setItem(mirrorSlot, copy);\n"
        "        if (mirrorSlot < VANILLA_HOTBAR_END_EXCLUSIVE) {\n"
        "            inventory.selected = mirrorSlot;\n"
        "            player.connection.send(new ClientboundSetCarriedItemPacket(mirrorSlot));\n"
        "        }\n"
        "        ScpPickupRouter.syncVanillaInventory(player);\n"
    )
    if old not in text:
        raise RuntimeError("Weapon mirror selection block was not found")
    return text.replace(old, new, 1)


edit(
    "src/main/java/com/bl4ues/scpinventory/network/InventoryActionPacket.java",
    patch_weapon_mirror,
)

print(f"Incremental multiplayer hotfix changed {len(changed)} files")
for rel in changed:
    print(rel)
