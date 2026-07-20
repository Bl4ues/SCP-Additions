package com.bl4ues.scpinventory.crafting;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.network.CraftingActionPacket;
import com.bl4ues.scpinventory.network.CraftingStateSyncPacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import com.bl4ues.scpadditions.compat.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Executes all crafting mutations on the logical server. */
public final class ScpCraftingService {
    private ScpCraftingService() {
    }

    public static void handle(ServerPlayer player, int action, int source,
                              int target, ResourceLocation recipeId) {
        if (player == null || player.isSpectator()) return;
        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            ScpCraftingState.Data state = ScpCraftingState.load(player);
            boolean changed = switch (action) {
                case CraftingActionPacket.MOVE_MAIN_TO_GRID ->
                        moveMainToGrid(inventory, state, source, target);
                case CraftingActionPacket.MOVE_GRID_TO_MAIN ->
                        moveGridToMain(inventory, state, source, target);
                case CraftingActionPacket.MOVE_GRID_TO_GRID ->
                        moveGridToGrid(state, source, target);
                case CraftingActionPacket.AUTO_FILL ->
                        autoFill(player, inventory, state, recipeId);
                case CraftingActionPacket.CRAFT ->
                        craft(player, inventory, state);
                case CraftingActionPacket.TOGGLE_PIN ->
                        state.togglePinned(recipeId);
                default -> false;
            };
            if (changed) {
                ScpCraftingState.save(player, state);
                ModNetwork.syncTo(player, inventory);
            }
            syncState(player, state);
        });
    }

    /** Public hook for future blueprint items and scripted unlocks. */
    public static boolean learn(ServerPlayer player, ResourceLocation recipeId) {
        if (player == null || recipeId == null
                || ScpCraftingRecipeHelper.getById(player.level(), recipeId).isEmpty()) {
            return false;
        }
        ScpCraftingState.Data state = ScpCraftingState.load(player);
        boolean changed = state.learn(recipeId);
        if (changed) {
            ScpCraftingState.save(player, state);
            syncState(player, state);
        }
        return changed;
    }

    public static void syncState(ServerPlayer player, ScpCraftingState.Data state) {
        if (player == null || state == null) return;
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new CraftingStateSyncPacket(ScpCraftingState.toTag(state)));
    }

    private static boolean moveMainToGrid(IScpInventory inventory,
                                          ScpCraftingState.Data state,
                                          int source, int target) {
        if (!inventory.isValidMainSlot(source) || !isGridSlot(target)) return false;
        ItemStack mainStack = inventory.getInventoryItem(source);
        if (mainStack.isEmpty()) return false;
        ItemStack gridStack = state.getGridItem(target);
        inventory.setInventoryItem(source, gridStack);
        state.setGridItem(target, mainStack);
        return true;
    }

    private static boolean moveGridToMain(IScpInventory inventory,
                                          ScpCraftingState.Data state,
                                          int source, int target) {
        if (!isGridSlot(source)) return false;
        ItemStack gridStack = state.getGridItem(source);
        if (gridStack.isEmpty()) return false;
        int destination = target;
        if (!inventory.isValidMainSlot(destination)) {
            destination = firstEmptyMainSlot(inventory);
            if (destination < 0) return false;
        }
        ItemStack mainStack = inventory.getInventoryItem(destination);
        inventory.setInventoryItem(destination, gridStack);
        state.setGridItem(source, mainStack);
        return true;
    }

    private static boolean moveGridToGrid(ScpCraftingState.Data state,
                                          int source, int target) {
        if (!isGridSlot(source) || !isGridSlot(target) || source == target) {
            return false;
        }
        ItemStack first = state.getGridItem(source);
        ItemStack second = state.getGridItem(target);
        if (first.isEmpty() && second.isEmpty()) return false;
        state.setGridItem(source, second);
        state.setGridItem(target, first);
        return true;
    }

    private static boolean autoFill(ServerPlayer player,
                                    IScpInventory inventory,
                                    ScpCraftingState.Data state,
                                    ResourceLocation recipeId) {
        if (recipeId == null || !state.getLearntRecipes().contains(recipeId)) {
            return false;
        }
        Optional<CraftingRecipe> optional = ScpCraftingRecipeHelper.getById(
                player.level(), recipeId);
        if (optional.isEmpty()) return false;
        List<Ingredient> layout = ScpCraftingRecipeHelper.layout(optional.get());

        List<SourceRef> sources = new ArrayList<>();
        for (int slot = 0; slot < inventory.getMaxMainSlots(); slot++) {
            ItemStack stack = inventory.getInventoryItem(slot);
            if (!stack.isEmpty()) sources.add(new SourceRef(false, slot, stack.copy()));
        }
        for (int slot = 0; slot < ScpCraftingState.GRID_SIZE; slot++) {
            ItemStack stack = state.getGridItem(slot);
            if (!stack.isEmpty()) sources.add(new SourceRef(true, slot, stack.copy()));
        }

        int[] assignment = ScpCraftingRecipeHelper.assignSources(
                sources.stream().map(SourceRef::stack).toList(), layout);
        if (assignment == null) return false;

        boolean[] usedSources = new boolean[sources.size()];
        List<ItemStack> newMain = new ArrayList<>(inventory.getMaxMainSlots());
        for (int slot = 0; slot < inventory.getMaxMainSlots(); slot++) {
            newMain.add(inventory.getInventoryItem(slot).copy());
        }
        List<ItemStack> newGrid = new ArrayList<>(ScpCraftingState.GRID_SIZE);
        for (int slot = 0; slot < ScpCraftingState.GRID_SIZE; slot++) {
            int sourceIndex = assignment[slot];
            if (sourceIndex < 0) {
                newGrid.add(ItemStack.EMPTY);
                continue;
            }
            usedSources[sourceIndex] = true;
            SourceRef ref = sources.get(sourceIndex);
            ItemStack placed = ref.stack().copy();
            placed.setCount(1);
            newGrid.add(placed);
            if (!ref.fromGrid()) newMain.set(ref.index(), ItemStack.EMPTY);
        }

        for (int i = 0; i < sources.size(); i++) {
            SourceRef ref = sources.get(i);
            if (!ref.fromGrid() || usedSources[i]) continue;
            int empty = firstEmptySlot(newMain);
            if (empty < 0) return false;
            newMain.set(empty, ref.stack().copy());
        }

        inventory.setInventory(newMain);
        for (int slot = 0; slot < ScpCraftingState.GRID_SIZE; slot++) {
            state.setGridItem(slot, newGrid.get(slot));
        }
        return true;
    }

    private static boolean craft(ServerPlayer player,
                                 IScpInventory inventory,
                                 ScpCraftingState.Data state) {
        TransientCraftingContainer container = ScpCraftingRecipeHelper
                .createContainer(state.getGrid());
        Optional<CraftingRecipe> optional = player.level().getRecipeManager()
                .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,
                        container, player.level());
        if (optional.isEmpty()) return false;

        CraftingRecipe recipe = optional.get();
        ItemStack output = recipe.assemble(container, player.level().registryAccess());
        if (output.isEmpty()) return false;
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(container);

        List<ItemStack> nextGrid = new ArrayList<>(ScpCraftingState.GRID_SIZE);
        List<ItemStack> overflowRemaining = new ArrayList<>();
        for (int slot = 0; slot < ScpCraftingState.GRID_SIZE; slot++) {
            ItemStack current = state.getGridItem(slot).copy();
            if (!current.isEmpty()) current.shrink(1);
            ItemStack remainder = slot < remaining.size()
                    ? remaining.get(slot).copy() : ItemStack.EMPTY;
            if (current.isEmpty() && !remainder.isEmpty()) {
                current = remainder;
            } else if (!remainder.isEmpty()) {
                overflowRemaining.add(remainder);
            }
            nextGrid.add(current);
        }

        int requiredSlots = output.getCount();
        for (ItemStack stack : overflowRemaining) requiredSlots += stack.getCount();
        if (inventory.getFreeMainSlots() < requiredSlots) {
            ModNetwork.showInventoryFull(player);
            return false;
        }

        for (int slot = 0; slot < ScpCraftingState.GRID_SIZE; slot++) {
            state.setGridItem(slot, nextGrid.get(slot));
        }
        inventory.addInventoryItems(output.copy());
        for (ItemStack stack : overflowRemaining) inventory.addInventoryItems(stack);
        output.onCraftedBy(player.level(), player, output.getCount());
        state.learn(recipe.getId());
        return true;
    }

    private static int firstEmptyMainSlot(IScpInventory inventory) {
        for (int slot = 0; slot < inventory.getMaxMainSlots(); slot++) {
            if (inventory.getInventoryItem(slot).isEmpty()) return slot;
        }
        return -1;
    }

    private static int firstEmptySlot(List<ItemStack> stacks) {
        for (int slot = 0; slot < stacks.size(); slot++) {
            if (stacks.get(slot).isEmpty()) return slot;
        }
        return -1;
    }

    private static boolean isGridSlot(int slot) {
        return slot >= 0 && slot < ScpCraftingState.GRID_SIZE;
    }

    private record SourceRef(boolean fromGrid, int index, ItemStack stack) {
    }
}
