package com.bl4ues.scpinventory.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Shared recipe lookup, layout and ingredient-assignment utilities. */
public final class ScpCraftingRecipeHelper {
    private ScpCraftingRecipeHelper() {
    }

    public static TransientCraftingContainer createContainer(List<ItemStack> grid) {
        AbstractContainerMenu menu = new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
        TransientCraftingContainer container = new TransientCraftingContainer(menu, 3, 3);
        for (int i = 0; i < ScpCraftingState.GRID_SIZE; i++) {
            ItemStack stack = grid != null && i < grid.size() ? grid.get(i) : ItemStack.EMPTY;
            container.setItem(i, stack == null ? ItemStack.EMPTY : stack.copy());
        }
        return container;
    }

    public static Optional<CraftingRecipe> findMatching(Level level, List<ItemStack> grid) {
        if (level == null) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(
                RecipeType.CRAFTING, createContainer(grid), level);
    }

    public static Optional<CraftingRecipe> getById(Level level, ResourceLocation id) {
        if (level == null || id == null) return Optional.empty();
        Optional<? extends Recipe<?>> recipe = level.getRecipeManager().byKey(id);
        if (recipe.isEmpty() || !(recipe.get() instanceof CraftingRecipe crafting)) {
            return Optional.empty();
        }
        return Optional.of(crafting);
    }

    /** Returns a centered 3x3 ingredient layout suitable for automatic filling. */
    public static List<Ingredient> layout(CraftingRecipe recipe) {
        List<Ingredient> result = emptyLayout();
        if (recipe == null) return result;

        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        if (recipe instanceof ShapedRecipe shaped) {
            int width = Math.min(3, shaped.getWidth());
            int height = Math.min(3, shaped.getHeight());
            int offsetX = Math.max(0, (3 - width) / 2);
            int offsetY = Math.max(0, (3 - height) / 2);
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    int source = row * shaped.getWidth() + column;
                    if (source < ingredients.size()) {
                        result.set((row + offsetY) * 3 + column + offsetX,
                                ingredients.get(source));
                    }
                }
            }
            return result;
        }

        int target = 0;
        for (Ingredient ingredient : ingredients) {
            if (ingredient == null || ingredient.isEmpty()) continue;
            while (target < result.size() && !result.get(target).isEmpty()) target++;
            if (target >= result.size()) break;
            result.set(target++, ingredient);
        }
        return result;
    }

    public static ItemStack representative(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return ItemStack.EMPTY;
        ItemStack[] choices = ingredient.getItems();
        if (choices.length == 0) return ItemStack.EMPTY;
        ItemStack copy = choices[0].copy();
        copy.setCount(1);
        return copy;
    }

    /**
     * Finds a distinct source item for each occupied ingredient slot. The returned
     * array is indexed by grid slot and contains source-list indexes, or -1 for an
     * empty recipe slot. Returns null when at least one ingredient is missing.
     */
    public static int[] assignSources(List<ItemStack> sources,
                                      List<Ingredient> layout) {
        int[] assignment = new int[ScpCraftingState.GRID_SIZE];
        Arrays.fill(assignment, -1);
        boolean[] used = new boolean[sources == null ? 0 : sources.size()];
        return assignRecursive(sources == null ? List.of() : sources,
                layout == null ? emptyLayout() : layout,
                assignment, used, 0) ? assignment : null;
    }

    private static boolean assignRecursive(List<ItemStack> sources,
                                           List<Ingredient> layout,
                                           int[] assignment,
                                           boolean[] used,
                                           int gridSlot) {
        if (gridSlot >= ScpCraftingState.GRID_SIZE) return true;
        Ingredient ingredient = gridSlot < layout.size()
                ? layout.get(gridSlot) : Ingredient.EMPTY;
        if (ingredient == null || ingredient.isEmpty()) {
            return assignRecursive(sources, layout, assignment, used,
                    gridSlot + 1);
        }

        for (int source = 0; source < sources.size(); source++) {
            ItemStack stack = sources.get(source);
            if (used[source] || stack == null || stack.isEmpty()
                    || !ingredient.test(stack)) continue;
            used[source] = true;
            assignment[gridSlot] = source;
            if (assignRecursive(sources, layout, assignment, used,
                    gridSlot + 1)) return true;
            assignment[gridSlot] = -1;
            used[source] = false;
        }
        return false;
    }

    public static List<ItemStack> nonEmptyCopies(List<ItemStack> stacks) {
        List<ItemStack> result = new ArrayList<>();
        if (stacks == null) return result;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            ItemStack copy = stack.copy();
            copy.setCount(1);
            result.add(copy);
        }
        return result;
    }

    private static List<Ingredient> emptyLayout() {
        List<Ingredient> result = new ArrayList<>(ScpCraftingState.GRID_SIZE);
        for (int i = 0; i < ScpCraftingState.GRID_SIZE; i++) {
            result.add(Ingredient.EMPTY);
        }
        return result;
    }
}
