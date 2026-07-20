package com.bl4ues.scpinventory.capability;

import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class FabricScpInventoryCodec {
    private static final Codec<Map<ScpEquipmentSlot, ItemStack>> EQUIPMENT_CODEC =
            Codec.unboundedMap(
                    Codec.STRING.xmap(ScpEquipmentSlot::valueOf, ScpEquipmentSlot::name),
                    ItemStack.OPTIONAL_CODEC);

    public static final Codec<IScpInventory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("max_main_slots", IScpInventory.DEFAULT_MAIN_SLOT_COUNT)
                    .forGetter(IScpInventory::getMaxMainSlots),
            Codec.INT.optionalFieldOf("coin_count", 0).forGetter(IScpInventory::getCoinCount),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("inventory", List.of())
                    .forGetter(IScpInventory::getInventory),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("keys", List.of())
                    .forGetter(IScpInventory::getKeys),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("documents", List.of())
                    .forGetter(IScpInventory::getDocuments),
            EQUIPMENT_CODEC.optionalFieldOf("equipment", Map.of())
                    .forGetter(FabricScpInventoryCodec::equipment),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("active_usable", ItemStack.EMPTY)
                    .forGetter(IScpInventory::getActiveUsable)
    ).apply(instance, FabricScpInventoryCodec::create));

    private FabricScpInventoryCodec() {}

    private static Map<ScpEquipmentSlot, ItemStack> equipment(IScpInventory inventory) {
        Map<ScpEquipmentSlot, ItemStack> result = new EnumMap<>(ScpEquipmentSlot.class);
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            ItemStack stack = inventory.getEquipment(slot);
            if (!stack.isEmpty()) result.put(slot, stack);
        }
        return result;
    }

    private static IScpInventory create(int maxSlots, int coins,
            List<ItemStack> inventory, List<ItemStack> keys,
            List<ItemStack> documents, Map<ScpEquipmentSlot, ItemStack> equipment,
            ItemStack activeUsable) {
        ScpInventory result = new ScpInventory();
        result.setMaxMainSlots(maxSlots);
        result.setCoinCount(coins);
        result.setInventory(new ArrayList<>(inventory));
        result.setKeys(new ArrayList<>(keys));
        result.setDocuments(new ArrayList<>(documents));
        equipment.forEach(result::setEquipment);
        result.setActiveUsable(activeUsable);
        return result;
    }
}
