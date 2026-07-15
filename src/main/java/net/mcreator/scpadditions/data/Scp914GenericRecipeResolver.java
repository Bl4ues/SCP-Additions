package net.mcreator.scpadditions.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Conservative fallback that derives SCP-914 transformations from the crafting
 * recipes currently loaded by the server. Explicit SCP-914 JSON recipes always
 * take priority and never pass through this resolver.
 */
public final class Scp914GenericRecipeResolver {
    private static final int MAX_FORWARD_INGREDIENTS = 9;

    private Scp914GenericRecipeResolver() {
    }

    public static Optional<GenericMatch> find(ServerLevel level,
                                              Scp914RecipeManager.Setting setting,
                                              List<ItemEntity> itemEntities) {
        if (level == null || itemEntities == null || itemEntities.isEmpty()) {
            return Optional.empty();
        }

        return switch (setting) {
            case ROUGH -> findReverse(level, itemEntities, true);
            case COARSE -> findReverse(level, itemEntities, false);
            case ONE_TO_ONE -> findOneToOne(level, itemEntities);
            case FINE -> findForward(level, itemEntities, false);
            case VERY_FINE -> findForward(level, itemEntities, true);
        };
    }

    private static Optional<GenericMatch> findForward(ServerLevel level,
                                                       List<ItemEntity> entities,
                                                       boolean veryFine) {
        List<ItemUnit> units = expandUnits(entities, MAX_FORWARD_INGREDIENTS);
        if (units.isEmpty()) return Optional.empty();

        List<ForwardCandidate> candidates = new ArrayList<>();
        for (CraftingRecipe recipe : recipes(level)) {
            if (!eligible(recipe)) continue;
            List<Ingredient> ingredients = nonEmptyIngredients(recipe);
            if (ingredients.size() != units.size()) continue;
            Optional<List<ItemUnit>> assignment = assignIngredients(ingredients, units);
            if (assignment.isEmpty()) continue;

            ItemStack result = result(level, recipe);
            if (result.isEmpty()) continue;
            candidates.add(new ForwardCandidate(recipe.getId(), assignment.get(), result));
        }
        if (candidates.isEmpty()) return Optional.empty();

        ForwardCandidate selected = random(candidates, level.random);
        ItemStack output = selected.result().copy();
        ResourceLocation resolvedRecipe = selected.recipeId();
        if (veryFine) {
            Optional<RecipeResult> successor = findHomogeneousSuccessor(level, output);
            if (successor.isPresent()) {
                output = successor.get().stack();
                resolvedRecipe = successor.get().recipeId();
            }
        }

        List<ItemStack> outputs = new ArrayList<>();
        outputs.add(output);
        outputs.addAll(craftingRemainders(selected.assignment()));
        return Optional.of(new GenericMatch(
                settingName(veryFine ? Scp914RecipeManager.Setting.VERY_FINE : Scp914RecipeManager.Setting.FINE),
                resolvedRecipe,
                collapseUses(selected.assignment()),
                combineStacks(outputs)));
    }

    private static Optional<GenericMatch> findReverse(ServerLevel level,
                                                       List<ItemEntity> entities,
                                                       boolean rough) {
        List<ItemUnit> units = expandUnits(entities, 2);
        if (units.size() != 1) return Optional.empty();
        ItemUnit input = units.get(0);

        List<ReverseCandidate> candidates = new ArrayList<>();
        for (CraftingRecipe recipe : recipes(level)) {
            if (!eligible(recipe)) continue;
            ItemStack result = result(level, recipe);
            if (result.isEmpty() || result.getCount() != 1
                    || !ItemStack.isSameItem(result, input.stack())) continue;

            List<ItemStack> recovered = new ArrayList<>();
            for (Ingredient ingredient : nonEmptyIngredients(recipe)) {
                ItemStack concrete = chooseIngredientStack(ingredient, level.random);
                if (!concrete.isEmpty() && !ItemStack.isSameItem(concrete, input.stack())) {
                    recovered.add(concrete);
                }
            }
            if (!recovered.isEmpty()) candidates.add(new ReverseCandidate(recipe.getId(), recovered));
        }
        if (candidates.isEmpty()) return Optional.empty();

        ReverseCandidate selected = random(candidates, level.random);
        List<ItemStack> recovered = rough
                ? roughRecovery(selected.ingredients(), level.random)
                : coarseRecovery(selected.ingredients(), level.random);
        return Optional.of(new GenericMatch(
                settingName(rough ? Scp914RecipeManager.Setting.ROUGH : Scp914RecipeManager.Setting.COARSE),
                selected.recipeId(),
                List.of(new Scp914RecipeManager.ItemUse(input.entity(), 1)),
                combineStacks(recovered)));
    }

    private static Optional<GenericMatch> findOneToOne(ServerLevel level,
                                                        List<ItemEntity> entities) {
        List<ItemUnit> units = expandUnits(entities, MAX_FORWARD_INGREDIENTS);
        if (units.isEmpty()) return Optional.empty();

        List<ForwardCandidate> direct = new ArrayList<>();
        for (CraftingRecipe recipe : recipes(level)) {
            if (!eligible(recipe)) continue;
            List<Ingredient> ingredients = nonEmptyIngredients(recipe);
            if (ingredients.size() != units.size()) continue;
            Optional<List<ItemUnit>> assignment = assignIngredients(ingredients, units);
            if (assignment.isEmpty()) continue;
            ItemStack result = result(level, recipe);
            if (!result.isEmpty()) direct.add(new ForwardCandidate(recipe.getId(), assignment.get(), result));
        }

        if (direct.size() > 1) {
            ForwardCandidate selected = random(direct, level.random);
            List<ItemStack> outputs = new ArrayList<>();
            outputs.add(selected.result().copy());
            outputs.addAll(craftingRemainders(selected.assignment()));
            return Optional.of(new GenericMatch(settingName(Scp914RecipeManager.Setting.ONE_TO_ONE),
                    selected.recipeId(), collapseUses(selected.assignment()), combineStacks(outputs)));
        }

        if (units.size() != 1) return Optional.empty();
        ItemUnit input = units.get(0);
        List<RecipeResult> equivalents = new ArrayList<>();
        for (CraftingRecipe recipe : recipes(level)) {
            if (!eligible(recipe)) continue;
            ItemStack output = result(level, recipe);
            if (output.isEmpty() || output.getCount() != 1
                    || ItemStack.isSameItem(output, input.stack())) continue;
            if (isEquivalentCategory(input.stack().getItem(), output.getItem())) {
                equivalents.add(new RecipeResult(recipe.getId(), output));
            }
        }
        if (equivalents.isEmpty()) return Optional.empty();

        RecipeResult selected = random(equivalents, level.random);
        return Optional.of(new GenericMatch(settingName(Scp914RecipeManager.Setting.ONE_TO_ONE),
                selected.recipeId(), List.of(new Scp914RecipeManager.ItemUse(input.entity(), 1)),
                List.of(selected.stack().copy())));
    }

    private static Optional<RecipeResult> findHomogeneousSuccessor(ServerLevel level, ItemStack intermediate) {
        if (intermediate.isEmpty()) return Optional.empty();
        ItemStack one = intermediate.copy();
        one.setCount(1);

        List<RecipeResult> successors = new ArrayList<>();
        for (CraftingRecipe recipe : recipes(level)) {
            if (!eligible(recipe)) continue;
            List<Ingredient> ingredients = nonEmptyIngredients(recipe);
            if (ingredients.isEmpty() || ingredients.size() > intermediate.getCount()) continue;
            boolean allMatch = true;
            for (Ingredient ingredient : ingredients) {
                if (!ingredient.test(one)) {
                    allMatch = false;
                    break;
                }
            }
            if (!allMatch) continue;
            ItemStack result = result(level, recipe);
            if (result.isEmpty() || ItemStack.isSameItem(result, intermediate)) continue;
            successors.add(new RecipeResult(recipe.getId(), result));
        }
        return successors.isEmpty() ? Optional.empty() : Optional.of(random(successors, level.random));
    }

    private static Optional<List<ItemUnit>> assignIngredients(List<Ingredient> ingredients,
                                                               List<ItemUnit> units) {
        boolean[] used = new boolean[units.size()];
        List<ItemUnit> assignment = new ArrayList<>();
        return assignRecursive(ingredients, units, 0, used, assignment)
                ? Optional.of(List.copyOf(assignment)) : Optional.empty();
    }

    private static boolean assignRecursive(List<Ingredient> ingredients, List<ItemUnit> units,
                                           int ingredientIndex, boolean[] used,
                                           List<ItemUnit> assignment) {
        if (ingredientIndex >= ingredients.size()) return true;
        Ingredient ingredient = ingredients.get(ingredientIndex);
        for (int i = 0; i < units.size(); i++) {
            if (used[i] || !ingredient.test(units.get(i).stack())) continue;
            used[i] = true;
            assignment.add(units.get(i));
            if (assignRecursive(ingredients, units, ingredientIndex + 1, used, assignment)) return true;
            assignment.remove(assignment.size() - 1);
            used[i] = false;
        }
        return false;
    }

    private static List<ItemUnit> expandUnits(List<ItemEntity> entities, int maximum) {
        int total = 0;
        for (ItemEntity entity : entities) {
            if (entity == null || entity.isRemoved() || entity.getItem().isEmpty()) continue;
            total += entity.getItem().getCount();
            if (total > maximum) return List.of();
        }
        if (total <= 0) return List.of();

        List<ItemUnit> units = new ArrayList<>(total);
        for (ItemEntity entity : entities) {
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) continue;
            for (int i = 0; i < stack.getCount(); i++) {
                ItemStack one = stack.copy();
                one.setCount(1);
                units.add(new ItemUnit(entity, one));
            }
        }
        return List.copyOf(units);
    }

    private static List<Scp914RecipeManager.ItemUse> collapseUses(List<ItemUnit> units) {
        Map<ItemEntity, Integer> counts = new LinkedHashMap<>();
        for (ItemUnit unit : units) counts.merge(unit.entity(), 1, Integer::sum);
        List<Scp914RecipeManager.ItemUse> uses = new ArrayList<>();
        counts.forEach((entity, count) -> uses.add(new Scp914RecipeManager.ItemUse(entity, count)));
        return List.copyOf(uses);
    }

    private static List<ItemStack> craftingRemainders(List<ItemUnit> units) {
        List<ItemStack> remainders = new ArrayList<>();
        for (ItemUnit unit : units) {
            ItemStack stack = unit.stack();
            if (stack.hasCraftingRemainingItem()) {
                ItemStack remainder = stack.getCraftingRemainingItem();
                if (!remainder.isEmpty()) remainders.add(remainder.copy());
            }
        }
        return remainders;
    }

    private static List<ItemStack> roughRecovery(List<ItemStack> ingredients, RandomSource random) {
        if (ingredients.isEmpty() || random.nextFloat() < 0.28F) return List.of();
        ItemStack selected = ingredients.get(random.nextInt(ingredients.size())).copy();
        selected.setCount(1);
        return List.of(selected);
    }

    private static List<ItemStack> coarseRecovery(List<ItemStack> ingredients, RandomSource random) {
        List<ItemStack> recovered = new ArrayList<>();
        for (ItemStack ingredient : ingredients) {
            if (random.nextFloat() <= 0.68F) {
                ItemStack copy = ingredient.copy();
                copy.setCount(1);
                recovered.add(copy);
            }
        }
        if (recovered.isEmpty() && !ingredients.isEmpty()) {
            ItemStack fallback = ingredients.get(random.nextInt(ingredients.size())).copy();
            fallback.setCount(1);
            recovered.add(fallback);
        }
        return recovered;
    }

    private static ItemStack chooseIngredientStack(Ingredient ingredient, RandomSource random) {
        ItemStack[] choices = ingredient.getItems();
        if (choices.length == 0) return ItemStack.EMPTY;
        List<ItemStack> valid = new ArrayList<>();
        for (ItemStack choice : choices) if (!choice.isEmpty()) valid.add(choice);
        if (valid.isEmpty()) return ItemStack.EMPTY;
        ItemStack selected = valid.get(random.nextInt(valid.size())).copy();
        selected.setCount(1);
        return selected;
    }

    private static List<ItemStack> combineStacks(List<ItemStack> stacks) {
        Map<StackKey, ItemStack> combined = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            StackKey key = new StackKey(stack.getItem(), stack.getTag() == null ? "" : stack.getTag().toString());
            ItemStack existing = combined.get(key);
            if (existing == null) combined.put(key, stack.copy());
            else existing.grow(stack.getCount());
        }
        return List.copyOf(combined.values());
    }

    private static boolean eligible(CraftingRecipe recipe) {
        return recipe != null && !recipe.isSpecial() && !nonEmptyIngredients(recipe).isEmpty();
    }

    private static List<Ingredient> nonEmptyIngredients(CraftingRecipe recipe) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient != null && !ingredient.isEmpty()) ingredients.add(ingredient);
        }
        return ingredients;
    }

    private static ItemStack result(ServerLevel level, CraftingRecipe recipe) {
        ItemStack result = recipe.getResultItem(level.registryAccess());
        return result == null ? ItemStack.EMPTY : result.copy();
    }

    private static List<CraftingRecipe> recipes(ServerLevel level) {
        return level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING);
    }

    private static boolean isEquivalentCategory(Item input, Item output) {
        if (input instanceof ArmorItem inputArmor && output instanceof ArmorItem outputArmor) {
            return inputArmor.getEquipmentSlot() == outputArmor.getEquipmentSlot();
        }
        if (input instanceof SwordItem && output instanceof SwordItem) return true;
        if (input instanceof DiggerItem && output instanceof DiggerItem) return true;
        if (input instanceof ProjectileWeaponItem && output instanceof ProjectileWeaponItem) return true;
        return input.getClass() != Item.class && input.getClass().equals(output.getClass())
                && input.getMaxStackSize() == output.getMaxStackSize();
    }

    private static String settingName(Scp914RecipeManager.Setting setting) {
        return setting.serializedName();
    }

    private static <T> T random(List<T> values, RandomSource random) {
        return values.get(random.nextInt(values.size()));
    }

    public record GenericMatch(String setting, ResourceLocation sourceRecipe,
                               List<Scp914RecipeManager.ItemUse> itemUses,
                               List<ItemStack> outputs) {
    }

    private record ItemUnit(ItemEntity entity, ItemStack stack) {
    }

    private record ForwardCandidate(ResourceLocation recipeId, List<ItemUnit> assignment,
                                    ItemStack result) {
    }

    private record ReverseCandidate(ResourceLocation recipeId, List<ItemStack> ingredients) {
    }

    private record RecipeResult(ResourceLocation recipeId, ItemStack stack) {
    }

    private record StackKey(Item item, String tag) {
    }
}
