package com.bl4ues.scpinventory.crafting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Persistent 3x3 grid and recipe knowledge owned by one player. */
public final class ScpCraftingState {
    public static final int GRID_SIZE = 9;
    private static final String ROOT_KEY = "ScpInventoryCrafting";
    private static final String GRID_KEY = "Grid";
    private static final String LEARNT_KEY = "LearntRecipes";
    private static final String PINNED_KEY = "PinnedRecipes";

    private ScpCraftingState() {
    }

    public static Data load(Player player) {
        if (player == null) return new Data();
        CompoundTag persisted = player.getPersistentData()
                .getCompound(Player.PERSISTED_NBT_TAG);
        return fromTag(persisted.getCompound(ROOT_KEY));
    }

    public static void save(Player player, Data data) {
        if (player == null || data == null) return;
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(ROOT_KEY, toTag(data));
        root.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static CompoundTag toTag(Data data) {
        CompoundTag tag = new CompoundTag();
        ListTag gridTag = new ListTag();
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack stack = data.getGridItem(i);
            gridTag.add(stack.isEmpty() ? new CompoundTag() : stack.save(new CompoundTag()));
        }
        tag.put(GRID_KEY, gridTag);
        tag.put(LEARNT_KEY, saveIds(data.learntRecipes));
        tag.put(PINNED_KEY, saveIds(data.pinnedRecipes));
        return tag;
    }

    public static Data fromTag(CompoundTag tag) {
        Data data = new Data();
        if (tag == null || tag.isEmpty()) return data;

        ListTag gridTag = tag.getList(GRID_KEY, 10);
        for (int i = 0; i < GRID_SIZE; i++) {
            data.grid.set(i, i < gridTag.size()
                    ? copySingle(ItemStack.of(gridTag.getCompound(i)))
                    : ItemStack.EMPTY);
        }
        loadIds(tag.getList(LEARNT_KEY, 8), data.learntRecipes);
        loadIds(tag.getList(PINNED_KEY, 8), data.pinnedRecipes);
        data.pinnedRecipes.retainAll(data.learntRecipes);
        return data;
    }

    private static ListTag saveIds(Set<ResourceLocation> ids) {
        ListTag tag = new ListTag();
        for (ResourceLocation id : ids) tag.add(StringTag.valueOf(id.toString()));
        return tag;
    }

    private static void loadIds(ListTag tag, Set<ResourceLocation> target) {
        target.clear();
        for (int i = 0; i < tag.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(i));
            if (id != null) target.add(id);
        }
    }

    private static ItemStack copySingle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    public static final class Data {
        private final List<ItemStack> grid = new ArrayList<>(GRID_SIZE);
        private final LinkedHashSet<ResourceLocation> learntRecipes = new LinkedHashSet<>();
        private final LinkedHashSet<ResourceLocation> pinnedRecipes = new LinkedHashSet<>();

        public Data() {
            for (int i = 0; i < GRID_SIZE; i++) grid.add(ItemStack.EMPTY);
        }

        public List<ItemStack> getGrid() {
            return grid;
        }

        public ItemStack getGridItem(int slot) {
            if (slot < 0 || slot >= GRID_SIZE) return ItemStack.EMPTY;
            return grid.get(slot);
        }

        public void setGridItem(int slot, ItemStack stack) {
            if (slot < 0 || slot >= GRID_SIZE) return;
            grid.set(slot, copySingle(stack));
        }

        public Set<ResourceLocation> getLearntRecipes() {
            return learntRecipes;
        }

        public Set<ResourceLocation> getPinnedRecipes() {
            return pinnedRecipes;
        }

        public boolean learn(ResourceLocation id) {
            return id != null && learntRecipes.add(id);
        }

        public boolean togglePinned(ResourceLocation id) {
            if (id == null || !learntRecipes.contains(id)) return false;
            if (!pinnedRecipes.remove(id)) pinnedRecipes.add(id);
            return true;
        }
    }
}
