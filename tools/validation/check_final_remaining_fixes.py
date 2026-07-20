#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--modern-registry", action="store_true")
    parser.add_argument("--validate-datapack", action="store_true")
    args = parser.parse_args()

    root = Path("src/main/java")
    read = lambda rel: (root / rel).read_text(encoding="utf-8")

    inventory_config = read("com/bl4ues/scpinventory/config/ScpInventoryConfig.java")
    item_manager = read("com/bl4ues/scpinventory/config/ItemConfigManager.java")
    item_client = read("com/bl4ues/scpinventory/client/ItemConfigClientHandler.java")
    context_client = read("com/bl4ues/scpinventory/client/ContextConfigClientHandler.java")
    context = read("com/bl4ues/scpinventory/context/ContextInteractionRegistry.java")
    coffee = read("net/mcreator/scpadditions/item/CoffeeItem.java")
    durability = read("net/mcreator/scpadditions/event/Scp173DurabilityEvents.java")
    mod_network = read("com/bl4ues/scpinventory/network/ModNetwork.java")
    login_events = read("com/bl4ues/scpinventory/events/InventoryModuleStateEvents.java")
    client_events = read("com/bl4ues/scpinventory/client/ClientGameplayEvents.java")
    usable_client = read("com/bl4ues/scpinventory/client/UsableHotbarSessionClient.java")
    inventory_action = read("com/bl4ues/scpinventory/network/InventoryActionPacket.java")

    assert "applyServerSnapshot" in inventory_config
    assert "clearServerSnapshot" in inventory_config
    assert "reloadFromDisk()" in inventory_config
    assert "ScpInventoryConfig.reloadFromDisk();" in item_manager
    assert "syncServerConfig" in item_manager

    assert "ScpInventoryConfig.reload();" in item_client
    assert "ScpInventoryConfig.reloadFromDisk();" not in item_client
    assert "ContextInteractionRegistry.reload();" in context_client
    assert "ContextInteractionRegistry.reloadFromDisk();" not in context_client

    assert "gas_mask:scp_1499" not in context
    if args.modern_registry:
        assert context.count("if (!BuiltInRegistries.BLOCK.containsKey(id))") == 1
        assert context.count("if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id))") == 1
    else:
        assert context.count("if (!ForgeRegistries.BLOCKS.containsKey(id))") == 1
        assert context.count("if (!ForgeRegistries.ENTITY_TYPES.containsKey(id))") == 1

    assert 'while (path.startsWith("cup_of_"))' in coffee
    assert "DAMAGE_THRESHOLD = 6.0F" in durability
    assert "MIN_ACCEPTED_DAMAGE = 1.0F" in durability
    assert "amount - DAMAGE_THRESHOLD" in durability
    assert "DAMAGE_MULTIPLIER" not in durability
    assert "private static final double KNOCKBACK_RESISTANCE = 1.0D" in durability
    assert "previousMaxHealth < MAX_HEALTH" in durability
    # Reject only the old unconditional-on-join healing behavior. A clamp from
    # health above the new maximum to the maximum is intentional and harmless.
    assert "if (scp173.getHealth() < scp173.getMaxHealth())" not in durability

    def converted_damage(amount: float) -> float | None:
        if amount <= 6.0:
            return None
        return max(1.0, amount - 6.0)

    assert converted_damage(0.0) is None
    assert converted_damage(6.0) is None
    assert converted_damage(7.0) == 1.0
    assert converted_damage(10.0) == 4.0
    assert converted_damage(20.0) == 14.0

    assert 'PROTOCOL_VERSION = "6"' in mod_network
    assert "ServerConfigSyncPacket.class" in mod_network
    assert (root / "com/bl4ues/scpinventory/network/ServerConfigSyncPacket.java").is_file()
    assert "syncServerConfig(player)" in login_events
    assert "clearServerSnapshot" in client_events
    assert "return stack != null && !stack.isEmpty();" in usable_client
    assert "ClientboundSetCarriedItemPacket" in inventory_action

    if args.validate_datapack:
        resources = Path("src/main/resources")
        assert (resources / "data/scp_additions/advancement").is_dir()
        assert (resources / "data/scp_additions/recipe").is_dir()
        assert not (resources / "data/scp_additions/advancements").exists()
        assert not (resources / "data/scp_additions/recipes").exists()
        meta = json.loads((resources / "pack.mcmeta").read_text(encoding="utf-8"))
        assert meta["pack"]["pack_format"] == 48
        for path in (resources / "data").rglob("*.json"):
            json.loads(path.read_text(encoding="utf-8"))


if __name__ == "__main__":
    main()
