#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
errors: list[str] = []


def text(rel: str) -> str:
    path = ROOT / rel
    if not path.exists():
        errors.append(f"missing file: {rel}")
        return ""
    return path.read_text(encoding="utf-8")


def require(rel: str, *fragments: str) -> None:
    source = text(rel)
    for fragment in fragments:
        if fragment not in source:
            errors.append(f"{rel}: missing {fragment!r}")


def forbid(rel: str, *fragments: str) -> None:
    source = text(rel)
    for fragment in fragments:
        if fragment in source:
            errors.append(f"{rel}: forbidden stale fragment {fragment!r}")


require(
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGateVolume.java",
    "new AABB(controller).inflate(1.0D)",
)
require(
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGateUpdateTickProcedure.java",
    "TeslaGateVolume.at(x, y, z)",
    "int activationDelay = manualOverride ? 1 : 5",
)
forbid(
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGateUpdateTickProcedure.java",
    "detectionRadius",
    "3.5D",
)
require(
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGatePulseHelper.java",
    "TeslaGateVolume.at(x, y, z)",
    "LETHAL_DAMAGE = 200.0F",
)
forbid(
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGatePulseHelper.java",
    "pulseRadius",
    "2.25D",
)
require(
    "src/main/java/net/mcreator/scpadditions/entity/Scp173Entity.java",
    "PLAYER_OBSERVED_DOT_THRESHOLD = 0.0D",
    "MOB_OBSERVED_DOT_THRESHOLD = 0.8660254037844386D",
    "SCP_131_OBSERVED_DOT_THRESHOLD = 0.70D",
)
require(
    "src/main/java/net/mcreator/scpadditions/data/Scp294DrinkManager.java",
    "hasBundledItemModel",
    "using the generic configurable cup",
)
require(
    "src/main/java/com/bl4ues/scpinventory/network/ModNetwork.java",
    'PROTOCOL_VERSION = "6"',
    "ServerConfigSyncPacket.class",
    "syncServerConfig(ServerPlayer player)",
)
require(
    "src/main/java/com/bl4ues/scpinventory/events/InventoryModuleStateEvents.java",
    "ModNetwork.syncServerConfig(player)",
)
require(
    "src/main/java/com/bl4ues/scpinventory/config/ScpInventoryConfig.java",
    "applyServerSnapshot",
    "serverSnapshotActive",
    "clearServerSnapshot",
)
require(
    "src/main/java/com/bl4ues/scpinventory/context/ContextInteractionRegistry.java",
    "applyServerSnapshot",
    '"gas_mask:scp_1499".equals(idText)',
)
require(
    "src/main/java/com/bl4ues/scpinventory/context/ContextConfigManager.java",
    "ModNetwork.syncServerConfig(source.getServer().getPlayerList().getPlayers())",
)
require(
    "src/main/java/net/mcreator/scpadditions/config/ScpAdditionsReloadCommand.java",
    "ModNetwork.syncServerConfig(source.getServer().getPlayerList().getPlayers())",
)
require(
    "src/main/java/com/bl4ues/scpinventory/client/UsableHotbarSessionClient.java",
    "return stack != null && !stack.isEmpty();",
    "isSameMutableUsableItem(active, activeStack)",
)
require(
    "src/main/java/com/bl4ues/scpinventory/network/InventoryActionPacket.java",
    "ClientboundSetCarriedItemPacket",
    "player.connection.send(new ClientboundSetCarriedItemPacket(mirrorSlot))",
)
forbid(
    "src/main/java/com/bl4ues/scpinventory/network/ItemConfigSavePacket.java",
    "new ItemConfigReloadPacket()",
)
forbid(
    "src/main/java/com/bl4ues/scpinventory/network/ItemConfigDeletePacket.java",
    "new ItemConfigReloadPacket()",
)

context = json.loads(text("config/scpinventory/context_interactions.json") or "{}")
for entry in context.get("interactions", []):
    if isinstance(entry, dict) and entry.get("id") == "gas_mask:scp_1499":
        errors.append("bundled context config still contains gas_mask:scp_1499")

lang = json.loads(text("src/main/resources/assets/scp_additions/lang/en_us.json") or "{}")
if not lang.get("advancements.scp_572_a_achievement.title"):
    errors.append("SCP-572 advancement title is missing")

if errors:
    print("Multiplayer hotfix validation failed:")
    for error in errors:
        print(f"- {error}")
    raise SystemExit(1)

print("Multiplayer hotfix regression surface is present and stale implementations are absent.")
