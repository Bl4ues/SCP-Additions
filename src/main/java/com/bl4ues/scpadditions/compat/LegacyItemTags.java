package com.bl4ues.scpadditions.compat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Preserves the mod's 1.20.1 custom ItemStack NBT contract on Minecraft 1.21.1
 * by storing the same CompoundTag in the CUSTOM_DATA component.
 */
public final class LegacyItemTags {
    private LegacyItemTags() {
    }

    public static boolean hasTag(ItemStack stack) {
        CustomData data = stack == null ? null : stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty();
    }

    public static CompoundTag getTag(ItemStack stack) {
        CustomData data = stack == null ? null : stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.getUnsafe();
    }

    public static CompoundTag getOrCreateTag(ItemStack stack) {
        if (stack == null) throw new IllegalArgumentException("stack cannot be null");
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            CompoundTag tag = new CompoundTag();
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            data = stack.get(DataComponents.CUSTOM_DATA);
        }
        return data.getUnsafe();
    }

    public static void setTag(ItemStack stack, CompoundTag tag) {
        if (stack == null) return;
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static void update(ItemStack stack,
            java.util.function.Consumer<CompoundTag> updater) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, updater);
    }
}
