package com.bl4ues.scpclassifieddirective.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Server-side inference engine for SCP-914. Explicit JSON recipes remain the
 * authoritative path; this resolver exists so ordinary vanilla and modded
 * content still produces a meaningful result without hand-authored rules.
 *
 * <p>The five settings intentionally have different semantics:</p>
 * <ul>
 *     <li>Rough recursively decomposes items to the earliest material the
 *     crafting graph or lexical fallback can find.</li>
 *     <li>Coarse reverses one crafting step and returns every ingredient.</li>
 *     <li>1:1 searches for a semantic/category/quality equivalent.</li>
 *     <li>Fine behaves like a gridless crafting table, with a lexical crafting
 *     fallback when no exact recipe accepts the intake.</li>
 *     <li>Very Fine deliberately embraces SCP-914's unpredictability and picks
 *     a semantically related but potentially surprising item or entity.</li>
 * </ul>
 */
public final class Scp914GenericRecipeResolver {
    private static final int MAX_INTAKE_UNITS = 256;
    private static final int MAX_ROUGH_DEPTH = 10;
    private static final int SEMANTIC_POOL = 18;

    private static final Set<String> STOP_WORDS = Set.of(
            "item", "block", "entity", "minecraft", "spawn", "egg", "the",
            "a", "an", "of", "and", "mod", "generic");
    private static final Set<String> BASE_FORM_WORDS = Set.of(
            "ingot", "nugget", "gem", "dust", "raw", "ore", "crystal",
            "shard", "fragment", "scrap", "leather", "hide", "string",
            "fiber", "stick", "rod", "log", "wood", "plank", "stone",
            "cobblestone", "sand", "gravel", "clay", "bone", "coal",
            "charcoal", "redstone", "quartz", "obsidian", "flint");
    private static final Set<String> CATEGORY_WORDS = Set.of(
            "sword", "blade", "knife", "dagger", "axe", "pickaxe", "pick",
            "shovel", "spade", "hoe", "hammer", "helmet", "chestplate",
            "leggings", "boots", "armor", "armour", "bow", "crossbow",
            "gun", "rifle", "pistol", "tool", "machine", "device", "gear");

    private static final List<Set<String>> LEXICAL_FAMILIES = List.of(
            family("pig", "piglin", "hoglin", "hog", "boar", "swine", "pork"),
            family("cow", "cattle", "bull", "bovine", "beef"),
            family("sheep", "ram", "ewe", "ovine", "wool", "mutton"),
            family("chicken", "hen", "rooster", "poultry", "feather"),
            family("sword", "blade", "katana", "saber", "sabre", "rapier"),
            family("knife", "dagger", "blade", "cutter"),
            family("axe", "hatchet", "chopper"),
            family("pickaxe", "pick", "miner", "mining"),
            family("shovel", "spade", "dig", "digger"),
            family("bow", "crossbow", "archery", "arrow", "bolt"),
            family("helmet", "headgear", "head", "mask"),
            family("boots", "boot", "shoe", "shoes", "footwear"),
            family("iron", "steel", "ferrous", "metal"),
            family("gold", "golden", "gilded"),
            family("diamond", "gem", "crystal"),
            family("wood", "wooden", "timber", "log", "plank"),
            family("fire", "flame", "burn", "blaze", "ember", "magma"),
            family("water", "aqua", "ocean", "sea", "river"),
            family("ice", "frost", "snow", "frozen", "cold"),
            family("poison", "venom", "toxic", "toxin"),
            family("zombie", "undead", "corpse", "rotting"),
            family("skeleton", "bone", "skull"),
            family("spider", "arachnid", "web", "cobweb"),
            family("flesh", "meat", "raw", "cooked", "food")
    );

    private Scp914GenericRecipeResolver() {
    }

    public static Optional<GenericMatch> find(ServerLevel level,
            Scp914RecipeManager.Setting setting,
            List<ItemEntity> itemEntities,
            List<Entity> entities) {
        if (level == null) return Optional.empty();
        List<ItemEntity> items = itemEntities == null ? List.of() : itemEntities;
        List<Entity> livingInputs = entities == null ? List.of() : entities;
        if (items.isEmpty() && livingInputs.isEmpty()) return Optional.empty();

        return switch (setting) {
            case ROUGH -> findDisassembly(level, items, livingInputs, true);
            case COARSE -> findDisassembly(level, items, livingInputs, false);
            case ONE_TO_ONE -> findOneToOne(level, items, livingInputs);
            case FINE -> findFine(level, items);
            case VERY_FINE -> findVeryFine(level, items, livingInputs);
        };
    }

    private static Optional<GenericMatch> findFine(ServerLevel level,
            List<ItemEntity> entities) {
        List<ItemUnit> units = expandUnits(entities);
        if (units.isEmpty()) return Optional.empty();

        List<ForwardCandidate> candidates = collectForwardCandidates(level, units);
        ItemStack output;
        ResourceLocation source;
        if (!candidates.isEmpty()) {
            ForwardCandidate selected = selectMostComplete(candidates, level.random);
            output = selected.result().copy();
            source = selected.recipeId();
        } else {
            Optional<RecipeResult> lexical = lexicalCraftFallback(level, units);
            if (lexical.isPresent()) {
                output = lexical.get().stack().copy();
                source = lexical.get().recipeId();
            } else {
                Item sourceItem = units.get(0).stack().getItem();
                Item target = selectItemEquivalent(sourceItem, level.random,
                        false, true);
                output = new ItemStack(target);
                source = synthetic("fine/lexical", itemId(sourceItem));
            }
        }

        List<ItemStack> outputs = new ArrayList<>();
        outputs.add(output);
        return Optional.of(new GenericMatch(
                Scp914RecipeManager.Setting.FINE,
                source,
                allItemUses(entities),
                List.of(),
                splitStacks(outputs),
                List.of(),
                totalItemCount(entities),
                totalItemCount(entities)));
    }

    private static Optional<GenericMatch> findOneToOne(ServerLevel level,
            List<ItemEntity> items, List<Entity> entities) {
        List<ItemStack> outputs = new ArrayList<>();
        List<ResourceLocation> entityOutputs = new ArrayList<>();

        for (ItemEntity itemEntity : items) {
            if (itemEntity == null || itemEntity.isRemoved()
                    || itemEntity.getItem().isEmpty()) continue;
            ItemStack source = itemEntity.getItem();
            Item target = selectItemEquivalent(source.getItem(), level.random,
                    true, false);
            addCounted(outputs, target, source.getCount());
        }

        for (Entity entity : entities) {
            if (entity == null || entity.isRemoved()) continue;
            EntityType<?> target = selectEntityEquivalent(entity.getType(),
                    level.random, false);
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target);
            if (id != null) entityOutputs.add(id);
        }

        int total = totalItemCount(items) + validEntityCount(entities);
        if (outputs.isEmpty() && entityOutputs.isEmpty()) return Optional.empty();
        return Optional.of(new GenericMatch(
                Scp914RecipeManager.Setting.ONE_TO_ONE,
                synthetic("1_to_1/lexical", firstSourceId(items, entities)),
                allItemUses(items),
                consumeEntities(entities),
                splitStacks(outputs),
                List.copyOf(entityOutputs),
                total,
                total));
    }

    private static Optional<GenericMatch> findVeryFine(ServerLevel level,
            List<ItemEntity> items, List<Entity> entities) {
        List<ItemStack> outputs = new ArrayList<>();
        List<ResourceLocation> entityOutputs = new ArrayList<>();
        List<ItemUnit> units = expandUnits(items);

        // If the intake itself forms a real recipe, use the crafted result as the
        // semantic seed. Very Fine then mutates that result instead of merely
        // performing a second crafting step.
        if (!units.isEmpty()) {
            List<ForwardCandidate> direct = collectForwardCandidates(level, units);
            if (!direct.isEmpty()) {
                ForwardCandidate selected = selectMostComplete(direct, level.random);
                SemanticResult result = selectVeryFineResult(
                        itemProfile(selected.result().getItem()), level.random);
                appendSemanticResult(outputs, entityOutputs, result,
                        Math.max(1, selected.result().getCount()));
            } else {
                for (ItemEntity itemEntity : items) {
                    if (itemEntity == null || itemEntity.isRemoved()
                            || itemEntity.getItem().isEmpty()) continue;
                    ItemStack source = itemEntity.getItem();
                    SemanticResult result = selectVeryFineResult(
                            itemProfile(source.getItem()), level.random);
                    appendSemanticResult(outputs, entityOutputs, result,
                            source.getCount());
                }
            }
        }

        for (Entity entity : entities) {
            if (entity == null || entity.isRemoved()) continue;
            SemanticResult result = selectVeryFineResult(
                    entityProfile(entity.getType()), level.random);
            appendSemanticResult(outputs, entityOutputs, result, 1);
        }

        int total = totalItemCount(items) + validEntityCount(entities);
        if (outputs.isEmpty() && entityOutputs.isEmpty()) return Optional.empty();
        return Optional.of(new GenericMatch(
                Scp914RecipeManager.Setting.VERY_FINE,
                synthetic("very_fine/semantic", firstSourceId(items, entities)),
                allItemUses(items),
                consumeEntities(entities),
                splitStacks(outputs),
                List.copyOf(entityOutputs),
                total,
                total));
    }

    private static Optional<GenericMatch> findDisassembly(ServerLevel level,
            List<ItemEntity> items, List<Entity> entities, boolean recursive) {
        List<ItemStack> outputs = new ArrayList<>();
        for (ItemEntity itemEntity : items) {
            if (itemEntity == null || itemEntity.isRemoved()
                    || itemEntity.getItem().isEmpty()) continue;
            ItemStack input = itemEntity.getItem().copy();
            if (recursive) {
                decomposeToBase(level, input, 0, new HashSet<>(), outputs);
            } else {
                outputs.addAll(immediateComponents(level, input));
            }
        }

        int total = totalItemCount(items) + validEntityCount(entities);
        if (total <= 0) return Optional.empty();
        Scp914RecipeManager.Setting setting = recursive
                ? Scp914RecipeManager.Setting.ROUGH
                : Scp914RecipeManager.Setting.COARSE;
        return Optional.of(new GenericMatch(
                setting,
                synthetic((recursive ? "rough" : "coarse") + "/decompose",
                        firstSourceId(items, entities)),
                allItemUses(items),
                consumeEntities(entities),
                splitStacks(outputs),
                List.of(),
                total,
                total));
    }

    private static void decomposeToBase(ServerLevel level, ItemStack stack,
            int depth, Set<Item> lineage, List<ItemStack> output) {
        if (stack == null || stack.isEmpty()) return;
        Item item = stack.getItem();
        if (depth >= MAX_ROUGH_DEPTH || lineage.contains(item)) {
            output.add(stack.copy());
            return;
        }

        Optional<ReverseCandidate> reverse = bestReverseCandidate(level, stack);
        List<ItemStack> components = reverse
                .map(candidate -> scaleReverse(candidate, stack.getCount()))
                .orElseGet(() -> lexicalComponents(stack, level.random));

        if (components.isEmpty()) {
            LexicalProfile profile = itemProfile(item);
            if (isLikelyBaseMaterial(profile)) {
                output.add(stack.copy());
            } else {
                Item fallback = fallbackBaseMaterial(item);
                ItemStack result = new ItemStack(fallback,
                        Math.max(1, stack.getCount()));
                output.add(result);
            }
            return;
        }

        Set<Item> nextLineage = new HashSet<>(lineage);
        nextLineage.add(item);
        for (ItemStack component : components) {
            if (component.isEmpty()) continue;
            if (nextLineage.contains(component.getItem())) {
                output.add(component.copy());
            } else {
                decomposeToBase(level, component, depth + 1,
                        nextLineage, output);
            }
        }
    }

    private static List<ItemStack> immediateComponents(ServerLevel level,
            ItemStack input) {
        Optional<ReverseCandidate> reverse = bestReverseCandidate(level, input);
        if (reverse.isPresent()) {
            return scaleReverse(reverse.get(), input.getCount());
        }
        List<ItemStack> lexical = lexicalComponents(input, level.random);
        if (!lexical.isEmpty()) return lexical;
        if (isLikelyBaseMaterial(itemProfile(input.getItem()))) {
            return List.of(input.copy());
        }
        return List.of(new ItemStack(fallbackBaseMaterial(input.getItem()),
                Math.max(1, input.getCount())));
    }

    private static Optional<ReverseCandidate> bestReverseCandidate(
            ServerLevel level, ItemStack input) {
        List<ReverseCandidate> candidates = new ArrayList<>();
        for (CraftingRecipe recipe : recipes(level)) {
            if (!eligible(recipe)) continue;
            ItemStack result = result(level, recipe);
            if (result.isEmpty()
                    || !ItemStack.isSameItem(result, input)) continue;
            List<ItemStack> ingredients = concreteIngredients(recipe,
                    level.random, input.getItem());
            if (ingredients.isEmpty()) continue;
            candidates.add(new ReverseCandidate(recipe.getId(),
                    Math.max(1, result.getCount()), ingredients));
        }

        List<ItemStack> synthetic = syntheticComponents(input);
        if (!synthetic.isEmpty()) {
            candidates.add(new ReverseCandidate(
                    synthetic("disassembly", itemId(input.getItem())),
                    1, synthetic));
        }
        if (candidates.isEmpty()) return Optional.empty();

        double best = candidates.stream().mapToDouble(candidate ->
                candidate.ingredients().size()
                        / (double) Math.max(1, candidate.outputCount()))
                .max().orElse(0.0D);
        List<ReverseCandidate> strongest = candidates.stream()
                .filter(candidate -> Math.abs(candidate.ingredients().size()
                        / (double) Math.max(1, candidate.outputCount()) - best)
                        < 0.0001D)
                .toList();
        return Optional.of(random(strongest, level.random));
    }

    private static List<ItemStack> scaleReverse(ReverseCandidate candidate,
            int inputCount) {
        int batches = Math.max(1, (int) Math.ceil(inputCount
                / (double) Math.max(1, candidate.outputCount())));
        List<ItemStack> output = new ArrayList<>();
        for (ItemStack ingredient : candidate.ingredients()) {
            if (ingredient == null || ingredient.isEmpty()) continue;
            ItemStack copy = ingredient.copy();
            copy.setCount(Math.max(1, ingredient.getCount() * batches));
            output.add(copy);
        }
        return output;
    }

    private static List<ItemStack> concreteIngredients(CraftingRecipe recipe,
            RandomSource random, Item outputItem) {
        List<ItemStack> ingredients = new ArrayList<>();
        for (Ingredient ingredient : nonEmptyIngredients(recipe)) {
            ItemStack chosen = chooseIngredientStack(ingredient, random);
            if (chosen.isEmpty() || chosen.getItem() == outputItem) continue;
            ingredients.add(chosen);
        }
        return ingredients;
    }

    private static List<ItemStack> lexicalComponents(ItemStack input,
            RandomSource random) {
        List<ItemStack> synthetic = syntheticComponents(input);
        if (!synthetic.isEmpty()) return scaleComponents(synthetic,
                Math.max(1, input.getCount()));

        Item source = input.getItem();
        LexicalProfile sourceProfile = itemProfile(source);
        List<Scored<Item>> candidates = new ArrayList<>();
        for (Item candidate : ForgeRegistries.ITEMS.getValues()) {
            if (candidate == null || candidate == Items.AIR || candidate == source)
                continue;
            LexicalProfile target = itemProfile(candidate);
            if (!isLikelyBaseMaterial(target)) continue;
            int sharedMaterial = sharedMaterialTokens(sourceProfile, target);
            if (sharedMaterial <= 0) continue;
            double score = sharedMaterial * 4.0D
                    + lexicalSimilarity(sourceProfile, target) * 2.0D
                    - Math.max(0, target.tokens().size()
                            - sourceProfile.tokens().size()) * 0.15D;
            candidates.add(new Scored<>(candidate, score));
        }
        if (candidates.isEmpty()) return List.of();
        Item selected = pickFromTop(candidates, random, 6);
        return List.of(new ItemStack(selected, Math.max(1, input.getCount())));
    }

    private static List<ItemStack> syntheticComponents(ItemStack input) {
        if (input == null || input.isEmpty()) return List.of();
        Item item = input.getItem();
        List<ItemStack> components = new ArrayList<>();

        if (item instanceof SwordItem sword) {
            addRepeated(components, tierMaterial(sword.getTier()), 2);
            addRepeated(components, new ItemStack(Items.STICK), 1);
            return components;
        }
        if (item instanceof DiggerItem digger) {
            int materialCount = item instanceof ShovelItem ? 1
                    : item instanceof HoeItem ? 2 : 3;
            addRepeated(components, tierMaterial(digger.getTier()),
                    materialCount);
            addRepeated(components, new ItemStack(Items.STICK), 2);
            return components;
        }
        if (item instanceof ArmorItem armor) {
            ItemStack material = materialFromTokens(itemProfile(item));
            int count = switch (armor.getEquipmentSlot()) {
                case HEAD -> 5;
                case CHEST -> 8;
                case LEGS -> 7;
                case FEET -> 4;
                default -> 0;
            };
            addRepeated(components, material, count);
            return components;
        }
        return List.of();
    }

    private static Optional<RecipeResult> lexicalCraftFallback(ServerLevel level,
            List<ItemUnit> units) {
        LexicalProfile intake = aggregateProfile(units);
        List<Scored<RecipeResult>> candidates = new ArrayList<>();
        for (CraftingRecipe recipe : recipes(level)) {
            if (!eligible(recipe)) continue;
            ItemStack result = result(level, recipe);
            if (result.isEmpty()) continue;
            LexicalProfile target = itemProfile(result.getItem());
            double score = lexicalSimilarity(intake, target) * 4.0D;
            if (intake.category().equals(target.category())) score += 1.4D;
            int iq = intake.quality();
            int tq = target.quality();
            if (iq >= 0 && tq >= 0 && tq >= iq) score += 0.25D;
            candidates.add(new Scored<>(new RecipeResult(recipe.getId(), result),
                    score));
        }
        if (candidates.isEmpty()) return Optional.empty();
        candidates.sort(Comparator.comparingDouble(
                (Scored<RecipeResult> scored) -> scored.score()).reversed());
        int pool = Math.min(SEMANTIC_POOL, candidates.size());
        List<Scored<RecipeResult>> best = candidates.subList(0, pool);
        // When nothing is linguistically related there is still a valid craft
        // result. This is deliberate: the lexical layer is the final safeguard
        // that prevents an arbitrary modded item from making 914 do nothing.
        return Optional.of(weightedPick(best, level.random));
    }

    private static List<ForwardCandidate> collectForwardCandidates(
            ServerLevel level, List<ItemUnit> units) {
        List<ForwardCandidate> candidates = new ArrayList<>();
        for (CraftingRecipe recipe : recipes(level)) {
            if (!eligible(recipe)) continue;
            List<Ingredient> ingredients = nonEmptyIngredients(recipe);
            if (ingredients.isEmpty() || ingredients.size() > units.size())
                continue;
            Optional<List<ItemUnit>> assignment = assignIngredients(ingredients,
                    units);
            if (assignment.isEmpty()) continue;
            ItemStack result = result(level, recipe);
            if (result.isEmpty()) continue;
            candidates.add(new ForwardCandidate(recipe.getId(),
                    assignment.get(), result));
        }
        return candidates;
    }

    private static ForwardCandidate selectMostComplete(
            List<ForwardCandidate> candidates, RandomSource random) {
        int bestUse = candidates.stream().mapToInt(candidate ->
                candidate.assignment().size()).max().orElse(0);
        List<ForwardCandidate> best = candidates.stream()
                .filter(candidate -> candidate.assignment().size() == bestUse)
                .toList();
        return random(best, random);
    }

    private static Optional<List<ItemUnit>> assignIngredients(
            List<Ingredient> ingredients, List<ItemUnit> units) {
        boolean[] used = new boolean[units.size()];
        List<ItemUnit> assignment = new ArrayList<>();
        return assignRecursive(ingredients, units, 0, used, assignment)
                ? Optional.of(List.copyOf(assignment)) : Optional.empty();
    }

    private static boolean assignRecursive(List<Ingredient> ingredients,
            List<ItemUnit> units, int ingredientIndex, boolean[] used,
            List<ItemUnit> assignment) {
        if (ingredientIndex >= ingredients.size()) return true;
        Ingredient ingredient = ingredients.get(ingredientIndex);
        for (int i = 0; i < units.size(); i++) {
            if (used[i] || !ingredient.test(units.get(i).stack())) continue;
            used[i] = true;
            assignment.add(units.get(i));
            if (assignRecursive(ingredients, units, ingredientIndex + 1,
                    used, assignment)) return true;
            assignment.remove(assignment.size() - 1);
            used[i] = false;
        }
        return false;
    }

    private static Item selectItemEquivalent(Item source, RandomSource random,
            boolean oneToOne, boolean fineFallback) {
        LexicalProfile origin = itemProfile(source);
        List<Scored<Item>> candidates = new ArrayList<>();
        for (Item candidate : ForgeRegistries.ITEMS.getValues()) {
            if (candidate == null || candidate == Items.AIR || candidate == source
                    || isUnsafeItem(candidate)) continue;
            LexicalProfile target = itemProfile(candidate);
            double lexical = lexicalSimilarity(origin, target);
            double score = lexical * 4.2D;
            if (origin.category().equals(target.category())) score += 2.4D;
            if (source.getMaxStackSize() == candidate.getMaxStackSize())
                score += 0.2D;

            int oq = origin.quality();
            int tq = target.quality();
            if (oq >= 0 && tq >= 0) {
                int delta = tq - oq;
                if (oneToOne) {
                    if (Math.abs(delta) <= 1) score += 1.3D;
                    else score -= Math.abs(delta) * 0.65D;
                } else if (fineFallback) {
                    if (delta >= 0) score += 0.4D;
                }
            }
            candidates.add(new Scored<>(candidate, score));
        }
        if (candidates.isEmpty()) return fallbackBaseMaterial(source);
        return pickFromTop(candidates, random, oneToOne ? 10 : 14);
    }

    private static EntityType<?> selectEntityEquivalent(EntityType<?> source,
            RandomSource random, boolean veryFine) {
        LexicalProfile origin = entityProfile(source);
        List<Scored<EntityType<?>>> candidates = new ArrayList<>();
        for (EntityType<?> candidate : ForgeRegistries.ENTITY_TYPES.getValues()) {
            if (candidate == null || candidate == source
                    || !isSafeEntityType(candidate)) continue;
            LexicalProfile target = entityProfile(candidate);
            double score = lexicalSimilarity(origin, target) * 4.6D;
            if (source.getCategory() == candidate.getCategory())
                score += veryFine ? 0.45D : 1.6D;
            if (origin.namespace().equals(target.namespace())) score += 0.15D;
            candidates.add(new Scored<>(candidate, score));
        }
        if (candidates.isEmpty()) return source;
        return pickFromTop(candidates, random, veryFine ? 18 : 10);
    }

    private static SemanticResult selectVeryFineResult(LexicalProfile origin,
            RandomSource random) {
        List<Scored<Item>> itemCandidates = new ArrayList<>();
        for (Item candidate : ForgeRegistries.ITEMS.getValues()) {
            if (candidate == null || candidate == Items.AIR
                    || isUnsafeItem(candidate)) continue;
            LexicalProfile target = itemProfile(candidate);
            double lexical = lexicalSimilarity(origin, target);
            double score = lexical * 4.5D;
            if (origin.category().equals(target.category())) score += 0.8D;
            int oq = origin.quality();
            int tq = target.quality();
            if (oq >= 0 && tq >= 0) {
                int delta = tq - oq;
                if (delta == 1 || delta == 2) score += 1.4D;
                else if (delta > 2) score += 0.45D;
                else if (delta == 0) score += 0.2D;
                else score -= Math.abs(delta) * 0.2D;
            }
            itemCandidates.add(new Scored<>(candidate, score));
        }

        List<Scored<EntityType<?>>> entityCandidates = new ArrayList<>();
        for (EntityType<?> candidate : ForgeRegistries.ENTITY_TYPES.getValues()) {
            if (candidate == null || !isSafeEntityType(candidate)) continue;
            LexicalProfile target = entityProfile(candidate);
            double lexical = lexicalSimilarity(origin, target);
            double score = lexical * 5.0D + 0.15D;
            entityCandidates.add(new Scored<>(candidate, score));
        }

        itemCandidates.sort(Comparator.comparingDouble(
                (Scored<Item> scored) -> scored.score()).reversed());
        entityCandidates.sort(Comparator.comparingDouble(
                (Scored<EntityType<?>> scored) -> scored.score()).reversed());

        double bestItem = itemCandidates.isEmpty() ? -1.0D
                : itemCandidates.get(0).score();
        double bestEntity = entityCandidates.isEmpty() ? -1.0D
                : entityCandidates.get(0).score();
        boolean entityHasMeaning = bestEntity >= 1.2D;
        boolean chooseEntity = entityHasMeaning
                && (bestEntity > bestItem + 0.45D
                || random.nextFloat() < 0.22F);

        if (chooseEntity) {
            EntityType<?> type = pickFromTop(entityCandidates, random,
                    SEMANTIC_POOL);
            return new SemanticResult(null, type);
        }
        if (!itemCandidates.isEmpty()) {
            Item item = pickFromTop(itemCandidates, random, SEMANTIC_POOL);
            return new SemanticResult(item, null);
        }
        if (!entityCandidates.isEmpty()) {
            EntityType<?> type = pickFromTop(entityCandidates, random,
                    SEMANTIC_POOL);
            return new SemanticResult(null, type);
        }
        return new SemanticResult(Items.FLINT, null);
    }

    private static void appendSemanticResult(List<ItemStack> itemOutputs,
            List<ResourceLocation> entityOutputs, SemanticResult result,
            int count) {
        if (result.item() != null) {
            addCounted(itemOutputs, result.item(), Math.max(1, count));
            return;
        }
        if (result.entity() != null) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(
                    result.entity());
            if (id != null) {
                int copies = Math.max(1, Math.min(16, count));
                for (int i = 0; i < copies; i++) entityOutputs.add(id);
            }
        }
    }

    private static LexicalProfile itemProfile(Item item) {
        ResourceLocation id = itemId(item);
        ItemStack stack = new ItemStack(item);
        String display;
        try {
            display = stack.getHoverName().getString();
        } catch (Exception ignored) {
            display = "";
        }
        Set<String> tokens = tokens(id == null ? "" : id.getPath(),
                item.getDescriptionId(), display,
                item.getClass().getSimpleName());
        return new LexicalProfile(tokens, qualityRank(item, tokens),
                itemCategory(item), id == null ? "" : id.getNamespace());
    }

    private static LexicalProfile entityProfile(EntityType<?> type) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        String display;
        try {
            display = type.getDescription().getString();
        } catch (Exception ignored) {
            display = "";
        }
        Set<String> tokens = tokens(id == null ? "" : id.getPath(),
                type.getDescriptionId(), display);
        return new LexicalProfile(tokens, qualityRank(tokens),
                "entity:" + type.getCategory().getName(),
                id == null ? "" : id.getNamespace());
    }

    private static LexicalProfile aggregateProfile(List<ItemUnit> units) {
        Set<String> tokens = new LinkedHashSet<>();
        String category = "mixed";
        String namespace = "";
        int quality = -1;
        boolean first = true;
        for (ItemUnit unit : units) {
            LexicalProfile profile = itemProfile(unit.stack().getItem());
            tokens.addAll(profile.tokens());
            quality = Math.max(quality, profile.quality());
            if (first) {
                category = profile.category();
                namespace = profile.namespace();
                first = false;
            } else if (!category.equals(profile.category())) {
                category = "mixed";
            }
        }
        return new LexicalProfile(Set.copyOf(tokens), quality, category,
                namespace);
    }

    private static double lexicalSimilarity(LexicalProfile a,
            LexicalProfile b) {
        if (a.tokens().isEmpty() || b.tokens().isEmpty()) return 0.0D;
        int common = 0;
        for (String token : a.tokens()) if (b.tokens().contains(token)) common++;
        int union = a.tokens().size() + b.tokens().size() - common;
        if (union <= 0) return 0.0D;
        return common / (double) union;
    }

    private static int sharedMaterialTokens(LexicalProfile a,
            LexicalProfile b) {
        int count = 0;
        for (String token : a.tokens()) {
            if (CATEGORY_WORDS.contains(token)) continue;
            if (b.tokens().contains(token)) count++;
        }
        return count;
    }

    private static Set<String> tokens(String... values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            String camel = value.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
            String normalized = camel.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", " ");
            for (String raw : normalized.trim().split("\\s+")) {
                if (raw.length() < 2 || STOP_WORDS.contains(raw)) continue;
                result.add(raw);
                if (raw.endsWith("s") && raw.length() > 3) {
                    result.add(raw.substring(0, raw.length() - 1));
                }
            }
        }
        List<String> snapshot = List.copyOf(result);
        for (Set<String> family : LEXICAL_FAMILIES) {
            boolean related = snapshot.stream().anyMatch(family::contains);
            if (related) result.addAll(family);
        }
        return Set.copyOf(result);
    }

    private static String itemCategory(Item item) {
        if (item instanceof ArmorItem armor) {
            return "armor:" + armor.getEquipmentSlot().getName();
        }
        if (item instanceof SwordItem) return "sword";
        if (item instanceof DiggerItem) return "digger:" + item.getClass().getName();
        if (item instanceof ProjectileWeaponItem) return "projectile_weapon";
        if (item instanceof BlockItem) return "block";
        return item.getClass() == Item.class ? "generic"
                : item.getClass().getName();
    }

    private static int qualityRank(Item item, Set<String> tokens) {
        if (item instanceof TieredItem tiered) return tierRank(tiered.getTier());
        return qualityRank(tokens);
    }

    private static int qualityRank(Set<String> tokens) {
        if (tokens.contains("netherite")) return 6;
        if (tokens.contains("diamond")) return 5;
        if (tokens.contains("steel")) return 4;
        if (tokens.contains("iron") || tokens.contains("chainmail")) return 4;
        if (tokens.contains("gold") || tokens.contains("golden")) return 3;
        if (tokens.contains("copper")) return 2;
        if (tokens.contains("stone")) return 1;
        if (tokens.contains("wood") || tokens.contains("wooden")
                || tokens.contains("leather")) return 0;
        return -1;
    }

    private static int tierRank(Tier tier) {
        if (tier == Tiers.WOOD) return 0;
        if (tier == Tiers.STONE) return 1;
        if (tier == Tiers.GOLD) return 3;
        if (tier == Tiers.IRON) return 4;
        if (tier == Tiers.DIAMOND) return 5;
        if (tier == Tiers.NETHERITE) return 6;
        return -1;
    }

    private static ItemStack tierMaterial(Tier tier) {
        if (tier == Tiers.WOOD) return new ItemStack(Items.OAK_PLANKS);
        if (tier == Tiers.STONE) return new ItemStack(Items.COBBLESTONE);
        if (tier == Tiers.IRON) return new ItemStack(Items.IRON_INGOT);
        if (tier == Tiers.GOLD) return new ItemStack(Items.GOLD_INGOT);
        if (tier == Tiers.DIAMOND) return new ItemStack(Items.DIAMOND);
        if (tier == Tiers.NETHERITE) return new ItemStack(Items.NETHERITE_INGOT);
        return ItemStack.EMPTY;
    }

    private static ItemStack materialFromTokens(LexicalProfile profile) {
        Set<String> tokens = profile.tokens();
        if (tokens.contains("netherite")) return new ItemStack(Items.NETHERITE_INGOT);
        if (tokens.contains("diamond")) return new ItemStack(Items.DIAMOND);
        if (tokens.contains("gold") || tokens.contains("golden"))
            return new ItemStack(Items.GOLD_INGOT);
        if (tokens.contains("iron") || tokens.contains("steel")
                || tokens.contains("chainmail")) return new ItemStack(Items.IRON_INGOT);
        if (tokens.contains("leather")) return new ItemStack(Items.LEATHER);
        return ItemStack.EMPTY;
    }

    private static boolean isLikelyBaseMaterial(LexicalProfile profile) {
        for (String token : profile.tokens()) {
            if (BASE_FORM_WORDS.contains(token)) return true;
        }
        return false;
    }

    private static Item fallbackBaseMaterial(Item source) {
        Item[] fallbacks = {
                Items.FLINT, Items.COBBLESTONE, Items.STICK, Items.STRING,
                Items.LEATHER, Items.IRON_NUGGET, Items.REDSTONE
        };
        ResourceLocation id = itemId(source);
        int hash = id == null ? System.identityHashCode(source) : id.hashCode();
        return fallbacks[Math.floorMod(hash, fallbacks.length)];
    }

    private static boolean isUnsafeItem(Item item) {
        ResourceLocation id = itemId(item);
        if (id == null) return true;
        String path = id.getPath();
        return path.equals("air") || path.equals("barrier")
                || path.equals("structure_void") || path.equals("debug_stick")
                || path.contains("command_block") || path.equals("jigsaw")
                || path.equals("structure_block");
    }

    private static boolean isSafeEntityType(EntityType<?> type) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null) return false;
        String path = id.getPath();
        return !Set.of("player", "item", "experience_orb", "area_effect_cloud",
                "lightning_bolt", "fishing_bobber", "marker", "interaction",
                "block_display", "item_display", "text_display")
                .contains(path);
    }

    private static List<ItemUnit> expandUnits(List<ItemEntity> entities) {
        int total = totalItemCount(entities);
        if (total <= 0 || total > MAX_INTAKE_UNITS) return List.of();
        List<ItemUnit> units = new ArrayList<>(total);
        for (ItemEntity entity : entities) {
            if (entity == null || entity.isRemoved()) continue;
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

    private static List<Scp914RecipeManager.ItemUse> allItemUses(
            List<ItemEntity> entities) {
        List<Scp914RecipeManager.ItemUse> uses = new ArrayList<>();
        for (ItemEntity entity : entities) {
            if (entity == null || entity.isRemoved() || entity.getItem().isEmpty())
                continue;
            uses.add(new Scp914RecipeManager.ItemUse(entity,
                    entity.getItem().getCount()));
        }
        return List.copyOf(uses);
    }

    private static List<Scp914RecipeManager.EntityUse> consumeEntities(
            List<Entity> entities) {
        List<Scp914RecipeManager.EntityUse> uses = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity != null && !entity.isRemoved()) {
                uses.add(new Scp914RecipeManager.EntityUse(entity, true));
            }
        }
        return List.copyOf(uses);
    }

    private static int totalItemCount(List<ItemEntity> entities) {
        int total = 0;
        if (entities == null) return 0;
        for (ItemEntity entity : entities) {
            if (entity != null && !entity.isRemoved() && !entity.getItem().isEmpty())
                total += entity.getItem().getCount();
        }
        return total;
    }

    private static int validEntityCount(List<Entity> entities) {
        if (entities == null) return 0;
        int total = 0;
        for (Entity entity : entities) if (entity != null && !entity.isRemoved()) total++;
        return total;
    }

    private static List<ItemStack> scaleComponents(List<ItemStack> components,
            int multiplier) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : components) {
            if (stack == null || stack.isEmpty()) continue;
            ItemStack copy = stack.copy();
            copy.setCount(Math.max(1, copy.getCount() * multiplier));
            result.add(copy);
        }
        return result;
    }

    private static List<ItemStack> splitStacks(List<ItemStack> stacks) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : combineStacks(stacks)) {
            int remaining = stack.getCount();
            int max = Math.max(1, stack.getMaxStackSize());
            while (remaining > 0) {
                ItemStack copy = stack.copy();
                int count = Math.min(max, remaining);
                copy.setCount(count);
                result.add(copy);
                remaining -= count;
            }
        }
        return List.copyOf(result);
    }

    private static List<ItemStack> combineStacks(List<ItemStack> stacks) {
        Map<StackKey, ItemStack> combined = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            StackKey key = new StackKey(stack.getItem(),
                    stack.getTag() == null ? "" : stack.getTag().toString());
            ItemStack existing = combined.get(key);
            if (existing == null) combined.put(key, stack.copy());
            else existing.grow(stack.getCount());
        }
        return List.copyOf(combined.values());
    }

    private static void addCounted(List<ItemStack> target, Item item, int count) {
        if (item == null || item == Items.AIR || count <= 0) return;
        target.add(new ItemStack(item, count));
    }

    private static void addRepeated(List<ItemStack> target, ItemStack template,
            int count) {
        if (template == null || template.isEmpty() || count <= 0) return;
        for (int i = 0; i < count; i++) {
            ItemStack copy = template.copy();
            copy.setCount(1);
            target.add(copy);
        }
    }

    private static ItemStack chooseIngredientStack(Ingredient ingredient,
            RandomSource random) {
        ItemStack[] choices = ingredient.getItems();
        if (choices.length == 0) return ItemStack.EMPTY;
        List<ItemStack> valid = new ArrayList<>();
        for (ItemStack choice : choices) if (!choice.isEmpty()) valid.add(choice);
        if (valid.isEmpty()) return ItemStack.EMPTY;
        ItemStack selected = valid.get(random.nextInt(valid.size())).copy();
        selected.setCount(1);
        return selected;
    }

    private static boolean eligible(CraftingRecipe recipe) {
        return recipe != null && !recipe.isSpecial()
                && !nonEmptyIngredients(recipe).isEmpty();
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

    private static ResourceLocation itemId(Item item) {
        return ForgeRegistries.ITEMS.getKey(item);
    }

    private static ResourceLocation firstSourceId(List<ItemEntity> items,
            List<Entity> entities) {
        if (items != null) {
            for (ItemEntity entity : items) {
                if (entity != null && !entity.isRemoved()
                        && !entity.getItem().isEmpty()) {
                    ResourceLocation id = itemId(entity.getItem().getItem());
                    if (id != null) return id;
                }
            }
        }
        if (entities != null) {
            for (Entity entity : entities) {
                if (entity != null && !entity.isRemoved()) {
                    ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(
                            entity.getType());
                    if (id != null) return id;
                }
            }
        }
        return new ResourceLocation("minecraft", "air");
    }

    private static ResourceLocation synthetic(String prefix,
            ResourceLocation source) {
        ResourceLocation safe = source == null
                ? new ResourceLocation("minecraft", "air") : source;
        return new ResourceLocation("scp_classified_directive",
                "inferred/" + prefix + "/" + safe.getNamespace() + "/"
                        + safe.getPath());
    }

    private static <T> T pickFromTop(List<Scored<T>> candidates,
            RandomSource random, int limit) {
        candidates.sort(Comparator.comparingDouble(
                (Scored<T> scored) -> scored.score()).reversed());
        int size = Math.min(Math.max(1, limit), candidates.size());
        return weightedPick(candidates.subList(0, size), random);
    }

    private static <T> T weightedPick(List<Scored<T>> candidates,
            RandomSource random) {
        double min = candidates.stream().mapToDouble(Scored::score)
                .min().orElse(0.0D);
        double total = 0.0D;
        for (Scored<T> candidate : candidates) {
            total += Math.max(0.08D, candidate.score() - min + 0.25D);
        }
        double roll = random.nextDouble() * Math.max(0.001D, total);
        for (Scored<T> candidate : candidates) {
            roll -= Math.max(0.08D, candidate.score() - min + 0.25D);
            if (roll <= 0.0D) return candidate.value();
        }
        return candidates.get(0).value();
    }

    private static <T> T random(List<T> values, RandomSource random) {
        return values.get(random.nextInt(values.size()));
    }

    private static Set<String> family(String... values) {
        return Set.of(values);
    }

    public record GenericMatch(Scp914RecipeManager.Setting setting,
            ResourceLocation sourceRecipe,
            List<Scp914RecipeManager.ItemUse> itemUses,
            List<Scp914RecipeManager.EntityUse> entityUses,
            List<ItemStack> outputs,
            List<ResourceLocation> entityOutputs,
            int matchedInputCount,
            int totalInputCount) {
        public boolean usesAllInputs() {
            return totalInputCount > 0 && matchedInputCount >= totalInputCount;
        }
    }

    private record ItemUnit(ItemEntity entity, ItemStack stack) {
    }

    private record ForwardCandidate(ResourceLocation recipeId,
            List<ItemUnit> assignment, ItemStack result) {
    }

    private record ReverseCandidate(ResourceLocation recipeId, int outputCount,
            List<ItemStack> ingredients) {
    }

    private record RecipeResult(ResourceLocation recipeId, ItemStack stack) {
    }

    private record LexicalProfile(Set<String> tokens, int quality,
            String category, String namespace) {
    }

    private record SemanticResult(Item item, EntityType<?> entity) {
    }

    private record Scored<T>(T value, double score) {
    }

    private record StackKey(Item item, String tag) {
    }
}
