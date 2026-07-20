from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA_ROOT = ROOT / "src/main/java"


def replace_required(text: str, before: str, after: str, label: str) -> str:
    if before not in text:
        if after in text:
            return text
        raise RuntimeError(f"Could not find {label}")
    return text.replace(before, after)


# The inventory interface now requires the dynamic registry lookup needed by
# ItemStack serialization in Minecraft 1.21.1.
interface = JAVA_ROOT / "com/bl4ues/scpinventory/capability/IScpInventory.java"
text = interface.read_text(encoding="utf-8")
if "import net.minecraft.core.HolderLookup;" not in text:
    text = text.replace(
        "import net.minecraft.nbt.CompoundTag;\n",
        "import net.minecraft.core.HolderLookup;\nimport net.minecraft.nbt.CompoundTag;\n",
    )
text = text.replace(
    "    CompoundTag serializeNBT();\n    void deserializeNBT(CompoundTag tag);",
    "    CompoundTag serializeNBT(HolderLookup.Provider registries);\n"
    "    void deserializeNBT(CompoundTag tag, HolderLookup.Provider registries);",
)
interface.write_text(text, encoding="utf-8")


inventory_path = JAVA_ROOT / "com/bl4ues/scpinventory/capability/ScpInventory.java"
text = inventory_path.read_text(encoding="utf-8")
if "import net.minecraft.core.HolderLookup;" not in text:
    text = text.replace(
        "import net.minecraft.nbt.CompoundTag;\n",
        "import net.minecraft.core.HolderLookup;\n"
        "import net.minecraft.nbt.CompoundTag;\n"
        "import net.minecraft.nbt.Tag;\n",
    )

text = replace_required(
    text,
    "    public CompoundTag serializeNBT() {",
    "    public CompoundTag serializeNBT(HolderLookup.Provider registries) {",
    "registry-aware serializeNBT signature",
)
text = text.replace(
    "saveStackList(inventory, true)",
    "saveStackList(inventory, true, registries)",
).replace(
    "saveStackList(keys, false)",
    "saveStackList(keys, false, registries)",
).replace(
    "saveStackList(documents, false)",
    "saveStackList(documents, false, registries)",
).replace(
    "saveEquipment(tag, \"ActiveUsable\", activeUsable)",
    "saveEquipment(tag, \"ActiveUsable\", activeUsable, registries)",
).replace(
    "saveEquipment(equipTag, slot.getTagName(), getEquipment(slot))",
    "saveEquipment(equipTag, slot.getTagName(), getEquipment(slot), registries)",
)

text = replace_required(
    text,
    "    public void deserializeNBT(CompoundTag tag) {",
    "    public void deserializeNBT(CompoundTag tag, HolderLookup.Provider registries) {",
    "registry-aware deserializeNBT signature",
)
text = text.replace(
    "loadEquipment(tag, \"ActiveUsable\")",
    "loadEquipment(tag, \"ActiveUsable\", registries)",
).replace(
    "ItemStack.of(invList.getCompound(i))",
    "ItemStack.parseOptional(registries, invList.getCompound(i))",
).replace(
    "loadKeyList(keys, tag.getList(\"Keys\", 10))",
    "loadKeyList(keys, tag.getList(\"Keys\", 10), registries)",
).replace(
    "loadStackList(documents, tag.getList(\"Documents\", 10))",
    "loadStackList(documents, tag.getList(\"Documents\", 10), registries)",
).replace(
    "loadEquipment(equipTag, slot.getTagName())",
    "loadEquipment(equipTag, slot.getTagName(), registries)",
).replace(
    "migrateLegacyEquipment(equipTag)",
    "migrateLegacyEquipment(equipTag, registries)",
)

text = text.replace(
    "private void migrateLegacyEquipment(CompoundTag equipTag)",
    "private void migrateLegacyEquipment(CompoundTag equipTag, HolderLookup.Provider registries)",
)
for key in ("Head", "Chest", "Accessory", "Trinket", "Weapon"):
    text = text.replace(
        f'loadEquipment(equipTag, "{key}")',
        f'loadEquipment(equipTag, "{key}", registries)',
    )

helpers_start = text.index(
    "    private static ListTag saveStackList(List<ItemStack> stacks, boolean keepEmptySlots) {"
)
helpers_end = text.index("\n}", helpers_start)
new_helpers = '''    private static ListTag saveStackList(
            List<ItemStack> stacks,
            boolean keepEmptySlots,
            HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty() && !keepEmptySlots) continue;
            list.add(saveStack(stack, registries));
        }
        return list;
    }

    private static void loadStackList(
            List<ItemStack> target,
            ListTag list,
            HolderLookup.Provider registries) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!stack.isEmpty()) target.add(stack);
        }
    }

    private static void loadKeyList(
            List<ItemStack> target,
            ListTag list,
            HolderLookup.Provider registries) {
        for (int i = 0; i < list.size() && target.size() < MAX_KEY_COUNT; i++) {
            ItemStack stack = toSingleItemOrEmpty(
                    ItemStack.parseOptional(registries, list.getCompound(i)));
            if (!stack.isEmpty()) target.add(stack);
        }
    }

    private static void saveEquipment(
            CompoundTag parent,
            String key,
            ItemStack stack,
            HolderLookup.Provider registries) {
        if (!stack.isEmpty()) {
            parent.put(key, saveStack(stack, registries));
        }
    }

    private static ItemStack loadEquipment(
            CompoundTag parent,
            String key,
            HolderLookup.Provider registries) {
        return parent.contains(key)
                ? ItemStack.parseOptional(registries, parent.getCompound(key))
                : ItemStack.EMPTY;
    }

    private static CompoundTag saveStack(
            ItemStack stack,
            HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty()) return new CompoundTag();
        Tag saved = stack.saveOptional(registries);
        if (saved instanceof CompoundTag compound) return compound;
        throw new IllegalStateException("ItemStack did not serialize to a CompoundTag: " + stack);
    }
'''
text = text[:helpers_start] + new_helpers + text[helpers_end:]
inventory_path.write_text(text, encoding="utf-8")


capability = JAVA_ROOT / "com/bl4ues/scpinventory/capability/ScpInventoryCapability.java"
text = capability.read_text(encoding="utf-8")
text = text.replace(
    "inventory.deserializeNBT(tag);",
    "inventory.deserializeNBT(tag, provider);",
).replace(
    "return inventory.serializeNBT();",
    "return inventory.serializeNBT(provider);",
)
capability.write_text(text, encoding="utf-8")


provider_path = JAVA_ROOT / "com/bl4ues/scpinventory/capability/ScpInventoryProvider.java"
text = provider_path.read_text(encoding="utf-8")
if "import net.minecraft.core.HolderLookup;" not in text:
    text = text.replace(
        "import net.minecraft.nbt.CompoundTag;\n",
        "import net.minecraft.core.HolderLookup;\nimport net.minecraft.nbt.CompoundTag;\n",
    )
text = text.replace(
    "public CompoundTag serializeNBT() {\n        return backend.serializeNBT();\n    }",
    "public CompoundTag serializeNBT(HolderLookup.Provider registries) {\n"
    "        return backend.serializeNBT(registries);\n    }",
).replace(
    "public void deserializeNBT(CompoundTag nbt) {\n        backend.deserializeNBT(nbt);\n    }",
    "public void deserializeNBT(CompoundTag nbt, HolderLookup.Provider registries) {\n"
    "        backend.deserializeNBT(nbt, registries);\n    }",
)
provider_path.write_text(text, encoding="utf-8")


network = JAVA_ROOT / "com/bl4ues/scpinventory/network/ModNetwork.java"
text = network.read_text(encoding="utf-8")
text = text.replace(
    "new SyncInventoryPacket(inventory.serializeNBT())",
    "new SyncInventoryPacket(inventory.serializeNBT(player.registryAccess()))",
)
network.write_text(text, encoding="utf-8")


client = JAVA_ROOT / "com/bl4ues/scpinventory/client/ClientPacketHandlers.java"
text = client.read_text(encoding="utf-8")
text = text.replace(
    "inventory.deserializeNBT(inventoryTag.copy());",
    "inventory.deserializeNBT(inventoryTag.copy(), minecraft.player.registryAccess());",
)
client.write_text(text, encoding="utf-8")

print("Migrated SCP inventory ItemStack serialization to registry-aware 1.21.1 APIs")
