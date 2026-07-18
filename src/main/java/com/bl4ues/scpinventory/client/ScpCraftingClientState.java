package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.crafting.ScpCraftingState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

/** Client mirror of the server-authoritative portable crafting state. */
public final class ScpCraftingClientState {
    private static ScpCraftingState.Data state = new ScpCraftingState.Data();
    private static long revision;

    private ScpCraftingClientState() {
    }

    public static void apply(CompoundTag tag) {
        state = ScpCraftingState.fromTag(tag == null ? new CompoundTag() : tag);
        revision++;
    }

    public static void reset() {
        state = new ScpCraftingState.Data();
        revision++;
    }

    public static List<ItemStack> getGrid() {
        return state.getGrid();
    }

    public static Set<ResourceLocation> getLearntRecipes() {
        return state.getLearntRecipes();
    }

    public static Set<ResourceLocation> getPinnedRecipes() {
        return state.getPinnedRecipes();
    }

    public static long getRevision() {
        return revision;
    }
}
