package com.bl4ues.scpclassifieddirective.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Selects between explicit SCP-914 JSON definitions and the inferred server-side
 * transformation engine. Explicit recipes remain authoritative on equal matches,
 * while a generic transformation may win when it accounts for more of the
 * physical intake.
 */
public final class Scp914RecipeBridge {
    private Scp914RecipeBridge() {
    }

    public static Optional<Scp914RecipeManager.RecipeMatch> findRecipe(
            ServerLevel level,
            Scp914RecipeManager.Setting setting,
            List<ItemEntity> itemEntities,
            List<Entity> entities) {
        Optional<Scp914RecipeManager.RecipeMatch> selected =
                selectForSetting(level, setting, itemEntities, entities);
        return selected.map(match -> consumeCompleteIntake(match,
                itemEntities));
    }

    private static Optional<Scp914RecipeManager.RecipeMatch> selectForSetting(
            ServerLevel level,
            Scp914RecipeManager.Setting setting,
            List<ItemEntity> itemEntities,
            List<Entity> entities) {
        Optional<Scp914RecipeManager.RecipeMatch> explicit =
                Scp914RecipeManager.findRecipe(setting, itemEntities, entities);

        Optional<Scp914GenericRecipeResolver.GenericMatch> generic =
                Scp914GenericRecipeResolver.find(level, setting, itemEntities,
                        entities);
        if (generic.isEmpty()) return explicit;

        Optional<Scp914RecipeManager.RecipeMatch> converted =
                convert(setting, generic.get());
        if (converted.isEmpty()) return explicit;
        if (explicit.isEmpty()) return converted;

        int totalInputs = Scp914RecipeManager.totalIntakeCount(itemEntities,
                entities);
        int explicitCount = Scp914RecipeManager.matchedInputCount(
                explicit.get());
        int genericCount = generic.get().matchedInputCount();
        boolean explicitComplete = totalInputs > 0
                && explicitCount >= totalInputs;
        boolean genericComplete = generic.get().usesAllInputs();

        if (genericComplete != explicitComplete) {
            return genericComplete ? converted : explicit;
        }
        // Hand-authored behavior wins ties. The inference layer is a safeguard,
        // not a way to make administrators fight their own recipe file.
        return genericCount > explicitCount ? converted : explicit;
    }

    private static Optional<Scp914RecipeManager.RecipeMatch> convert(
            Scp914RecipeManager.Setting requestedSetting,
            Scp914GenericRecipeResolver.GenericMatch generic) {
        List<Scp914RecipeManager.ItemOutput> itemOutputs = new ArrayList<>();
        for (ItemStack stack : generic.outputs()) {
            if (stack == null || stack.isEmpty()) continue;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null) continue;
            itemOutputs.add(new Scp914RecipeManager.ItemOutput(id,
                    stack.getCount()));
        }

        Map<ResourceLocation, Integer> entityCounts = new LinkedHashMap<>();
        for (ResourceLocation id : generic.entityOutputs()) {
            if (id != null && ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
                entityCounts.merge(id, 1, Integer::sum);
            }
        }
        List<Scp914RecipeManager.EntityOutput> entityOutputs = new ArrayList<>();
        entityCounts.forEach((id, count) -> entityOutputs.add(
                new Scp914RecipeManager.EntityOutput(id, count)));

        // Rough/Coarse entity inference may intentionally have no synthetic item
        // output: the entity itself is killed at the output chamber so its real
        // loot table supplies the result.
        if (itemOutputs.isEmpty() && entityOutputs.isEmpty()
                && generic.entityUses().isEmpty()) {
            return Optional.empty();
        }

        ResourceLocation source = generic.sourceRecipe();
        ResourceLocation syntheticId = new ResourceLocation(
                "scp_classified_directive",
                "inferred/" + requestedSetting.serializedName() + "/"
                        + source.getNamespace() + "/" + source.getPath());
        Scp914RecipeManager.RecipeDefinition definition =
                new Scp914RecipeManager.RecipeDefinition(
                        syntheticId,
                        requestedSetting,
                        List.of(),
                        List.of(),
                        List.copyOf(itemOutputs),
                        List.of(),
                        List.copyOf(entityOutputs),
                        1.0F,
                        false,
                        "");
        return Optional.of(new Scp914RecipeManager.RecipeMatch(
                definition, generic.itemUses(), generic.entityUses()));
    }

    private static Scp914RecipeManager.RecipeMatch consumeCompleteIntake(
            Scp914RecipeManager.RecipeMatch match,
            List<ItemEntity> itemEntities) {
        List<Scp914RecipeManager.ItemUse> fullUses = new ArrayList<>();
        if (itemEntities != null) {
            for (ItemEntity entity : itemEntities) {
                if (entity == null || entity.isRemoved()
                        || entity.getItem().isEmpty()) {
                    continue;
                }
                fullUses.add(new Scp914RecipeManager.ItemUse(entity,
                        entity.getItem().getCount()));
            }
        }
        return new Scp914RecipeManager.RecipeMatch(
                match.recipe(), List.copyOf(fullUses), match.entityUses());
    }
}
