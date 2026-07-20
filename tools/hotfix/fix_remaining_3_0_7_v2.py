#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ROOT = Path.cwd()
JAVA = ROOT / "src/main/java"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = read(path)
    if new in text:
        print(f"already applied: {label}")
        return
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected one {label} target in {path}, found {count}")
    write(path, text.replace(old, new, 1))
    print(f"applied: {label}")


def add_forced_inventory_reload() -> None:
    path = JAVA / "com/bl4ues/scpinventory/config/ScpInventoryConfig.java"
    old = """    public static void reload() {
        if (serverSnapshotActive) {
            return;
        }
        loaded = false;
        load();
    }
"""
    new = old + """
    /**
     * Reloads the host's local file even when the physical client and integrated
     * server share this class and a synchronized server snapshot is active.
     */
    public static synchronized void reloadFromDisk() {
        serverSnapshotActive = false;
        loaded = false;
        load();
    }
"""
    replace_once(path, old, new, "forced SCP Inventory disk reload")

    changed = 0
    already = 0
    for source in JAVA.rglob("*.java"):
        if source == path or "/client/" in source.as_posix():
            continue
        text = read(source)
        old_count = text.count("ScpInventoryConfig.reload();")
        if old_count:
            write(source, text.replace("ScpInventoryConfig.reload();", "ScpInventoryConfig.reloadFromDisk();"))
            changed += old_count
            print(f"updated local inventory reload: {source.relative_to(ROOT)}")
        already += text.count("ScpInventoryConfig.reloadFromDisk();")
    if changed == 0 and already == 0:
        raise RuntimeError("No local SCP Inventory reload call was found")


def add_forced_context_reload() -> None:
    path = JAVA / "com/bl4ues/scpinventory/context/ContextInteractionRegistry.java"
    old = """    public static void reload() {
        loaded = false;
        load();
    }
"""
    new = old + """
    /** Reloads the host's file instead of a client snapshot shared in singleplayer. */
    public static synchronized void reloadFromDisk() {
        serverSnapshotJson = null;
        loaded = false;
        load();
    }
"""
    replace_once(path, old, new, "forced context-interaction disk reload")

    changed = 0
    already = 0
    for source in JAVA.rglob("*.java"):
        if source == path or "/client/" in source.as_posix():
            continue
        text = read(source)
        old_count = text.count("ContextInteractionRegistry.reload();")
        if old_count:
            write(source, text.replace("ContextInteractionRegistry.reload();", "ContextInteractionRegistry.reloadFromDisk();"))
            changed += old_count
            print(f"updated local context reload: {source.relative_to(ROOT)}")
        already += text.count("ContextInteractionRegistry.reloadFromDisk();")
    if changed == 0 and already == 0:
        raise RuntimeError("No local context-interaction reload call was found")


def ignore_missing_context_targets() -> None:
    path = JAVA / "com/bl4ues/scpinventory/context/ContextInteractionRegistry.java"
    text = read(path)

    obsolete = """        // The old default 1499 entity rule resolves to a vanilla pig in the
        // external gas-mask mod. Ignore it even in pre-hotfix user configs.
        if (\"entity\".equals(type) && \"gas_mask:scp_1499\".equals(idText)) {
            return null;
        }

"""
    text = text.replace(obsolete, "")

    forge_block = """            block = ForgeRegistries.BLOCKS.getValue(id);
            if (block == null || block == Blocks.AIR) {
"""
    forge_block_new = """            if (!ForgeRegistries.BLOCKS.containsKey(id)) {
                return null;
            }
            block = ForgeRegistries.BLOCKS.getValue(id);
            if (block == null || block == Blocks.AIR) {
"""
    forge_entity = """            entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (entityType == null) {
"""
    forge_entity_new = """            if (!ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
                return null;
            }
            entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (entityType == null) {
"""

    modern_block = """            block = BuiltInRegistries.BLOCK.get(id);
            if (block == null || block == Blocks.AIR) {
"""
    modern_block_new = """            if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                return null;
            }
            block = BuiltInRegistries.BLOCK.get(id);
            if (block == null || block == Blocks.AIR) {
"""
    modern_entity = """            entityType = BuiltInRegistries.ENTITY_TYPE.get(id);
            if (entityType == null) {
"""
    modern_entity_new = """            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                return null;
            }
            entityType = BuiltInRegistries.ENTITY_TYPE.get(id);
            if (entityType == null) {
"""

    forge_block_guard = """            if (!ForgeRegistries.BLOCKS.containsKey(id)) {
                return null;
            }
"""
    forge_entity_guard = """            if (!ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
                return null;
            }
"""
    modern_block_guard = """            if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                return null;
            }
"""
    modern_entity_guard = """            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                return null;
            }
"""
    for guard in (forge_block_guard, forge_entity_guard, modern_block_guard, modern_entity_guard):
        while guard + guard in text:
            text = text.replace(guard + guard, guard)

    if forge_block in text and forge_entity in text:
        text = text.replace(forge_block, forge_block_new, 1)
        text = text.replace(forge_entity, forge_entity_new, 1)
    elif modern_block in text and modern_entity in text:
        text = text.replace(modern_block, modern_block_new, 1)
        text = text.replace(modern_entity, modern_entity_new, 1)
    elif ("BLOCKS.containsKey(id)" in text or "BLOCK.containsKey(id)" in text) and (
            "ENTITY_TYPES.containsKey(id)" in text or "ENTITY_TYPE.containsKey(id)" in text):
        print("already applied: missing context target rejection")
        write(path, text)
        return
    else:
        raise RuntimeError("Could not identify context registry lookup implementation")

    write(path, text)
    print("applied: missing block/entity context targets are ignored")


def fix_cup_names() -> None:
    path = JAVA / "net/mcreator/scpadditions/item/CoffeeItem.java"
    old = """\t\t\t\tString path = id.contains(\":\") ? id.substring(id.indexOf(':') + 1) : id;
\t\t\t\treturn Component.literal(\"Cup of \" + toTitleCase(path.replace('_', ' ')));
"""
    new = """\t\t\t\tString path = id.contains(\":\") ? id.substring(id.indexOf(':') + 1) : id;
\t\t\t\twhile (path.startsWith(\"cup_of_\")) {
\t\t\t\t\tpath = path.substring(\"cup_of_\".length());
\t\t\t\t}
\t\t\t\treturn Component.literal(\"Cup of \" + toTitleCase(path.replace('_', ' ')));
"""
    replace_once(path, old, new, "SCP-294 duplicate cup-name prefix")


def fix_scp173_damage() -> None:
    path = JAVA / "net/mcreator/scpadditions/event/Scp173DurabilityEvents.java"
    text = read(path)

    constants = {
        "private static final double ARMOR = 80.0D;": "private static final double ARMOR = 0.0D;",
        "private static final double ARMOR_TOUGHNESS = 40.0D;": "private static final double ARMOR_TOUGHNESS = 0.0D;",
        "private static final float DAMAGE_MULTIPLIER = 0.02F;\n    private static final float MIN_SURVIVABLE_DAMAGE = 0.25F;":
            "private static final float DAMAGE_THRESHOLD = 6.0F;\n    private static final float MIN_ACCEPTED_DAMAGE = 1.0F;",
    }
    for old, new in constants.items():
        if old in text:
            text = text.replace(old, new, 1)
        elif new not in text:
            raise RuntimeError(f"Missing SCP-173 constant target: {old}")

    join_old = """        setBaseAttribute(scp173, Attributes.MAX_HEALTH, MAX_HEALTH);
        setBaseAttribute(scp173, Attributes.ARMOR, ARMOR);
        setBaseAttribute(scp173, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS);
        setBaseAttribute(scp173, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE);
        if (scp173.getHealth() < scp173.getMaxHealth()) scp173.setHealth(scp173.getMaxHealth());
"""
    join_new = """        float previousMaxHealth = scp173.getMaxHealth();
        float previousHealth = scp173.getHealth();
        setBaseAttribute(scp173, Attributes.MAX_HEALTH, MAX_HEALTH);
        setBaseAttribute(scp173, Attributes.ARMOR, ARMOR);
        setBaseAttribute(scp173, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS);
        setBaseAttribute(scp173, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE);

        // Initialize old/fresh 80-health statues proportionally, but never heal a
        // damaged 173 every time its chunk or world is loaded.
        if (previousHealth > 0.0F && previousMaxHealth > 0.0F
                && previousMaxHealth < MAX_HEALTH) {
            double ratio = Math.min(1.0D, previousHealth / previousMaxHealth);
            scp173.setHealth((float) Math.max(1.0D, MAX_HEALTH * ratio));
        } else if (previousHealth > scp173.getMaxHealth()) {
            scp173.setHealth(scp173.getMaxHealth());
        }
"""
    if join_old in text:
        text = text.replace(join_old, join_new, 1)
    elif join_new not in text:
        raise RuntimeError("Missing SCP-173 join-health target")

    damage_old = """        float amount = event.getAmount();
        if (amount <= 0.0F) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(Math.max(MIN_SURVIVABLE_DAMAGE, amount * DAMAGE_MULTIPLIER));
"""
    damage_new = """        float amount = event.getAmount();
        if (amount <= DAMAGE_THRESHOLD) {
            event.setCanceled(true);
            return;
        }

        // Six damage or less cannot scratch the statue. Stronger attacks deal
        // one point at seven damage, then scale upward with their excess power.
        event.setAmount(Math.max(MIN_ACCEPTED_DAMAGE, amount - DAMAGE_THRESHOLD));
"""
    if damage_old in text:
        text = text.replace(damage_old, damage_new, 1)
    elif damage_new not in text:
        raise RuntimeError("Missing SCP-173 damage conversion target")

    write(path, text)
    print("applied: SCP-173 damage threshold and persistent damage")


def verify() -> None:
    inventory = read(JAVA / "com/bl4ues/scpinventory/config/ScpInventoryConfig.java")
    item_manager = read(JAVA / "com/bl4ues/scpinventory/config/ItemConfigManager.java")
    context = read(JAVA / "com/bl4ues/scpinventory/context/ContextInteractionRegistry.java")
    coffee = read(JAVA / "net/mcreator/scpadditions/item/CoffeeItem.java")
    durability = read(JAVA / "net/mcreator/scpadditions/event/Scp173DurabilityEvents.java")

    assert "reloadFromDisk()" in inventory
    assert "ScpInventoryConfig.reloadFromDisk();" in item_manager
    assert "gas_mask:scp_1499" not in context
    assert ("ENTITY_TYPES.containsKey(id)" in context or "ENTITY_TYPE.containsKey(id)" in context)
    assert "while (path.startsWith(\"cup_of_\"))" in coffee
    assert "DAMAGE_THRESHOLD = 6.0F" in durability
    assert "amount - DAMAGE_THRESHOLD" in durability
    assert "DAMAGE_MULTIPLIER" not in durability
    assert "previousMaxHealth < MAX_HEALTH" in durability


if __name__ == "__main__":
    if not JAVA.is_dir():
        raise RuntimeError(f"Run this script from the repository root; missing {JAVA}")
    add_forced_inventory_reload()
    add_forced_context_reload()
    ignore_missing_context_targets()
    fix_cup_names()
    fix_scp173_damage()
    verify()
