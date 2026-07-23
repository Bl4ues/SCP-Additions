package net.mcreator.scpadditions.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Selects between explicit JSON recipes and inferred crafting behavior.
 * Explicit definitions win equal matches, while a more complete inferred
 * crafting recipe may take priority over an explicit recipe that accounts for
 * only a small part of the intake.
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

        // Very Fine may reuse a valid Fine transformation only when no explicit
        // or inferred Very Fine transformation exists.
        if (selected.isEmpty()
                && setting == Scp914RecipeManager.Setting.VERY_FINE) {
            selected = selectForSetting(level,
                    Scp914RecipeManager.Setting.FINE,
                    itemEntities, entities);
        }

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

        // Inferred crafting transformations apply only to loose-item intakes.
        if (entities != null && !entities.isEmpty()) return explicit;

        Optional<Scp914GenericRecipeResolver.GenericMatch> generic =
                Scp914GenericRecipeResolver.find(level, setting, itemEntities);
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
        // Explicit JSON remains authoritative when both candidates account for
        // the same amount of the intake.
        return genericCount > explicitCount ? converted : explicit;
    }

    private static Optional<Scp914RecipeManager.RecipeMatch> convert(
            Scp914RecipeManager.Setting requestedSetting,
            Scp914GenericRecipeResolver.GenericMatch generic) {
        List<Scp914RecipeManager.ItemOutput> outputs = new ArrayList<>();
        for (ItemStack stack : generic.outputs()) {
            if (stack == null || stack.isEmpty()) continue;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null) continue;
            outputs.add(new Scp914RecipeManager.ItemOutput(id,
                    stack.getCount()));
        }
        if (outputs.isEmpty()) return Optional.empty();

        ResourceLocation source = generic.sourceRecipe();
        ResourceLocation syntheticId = new ResourceLocation("scp_additions",
                "inferred/" + requestedSetting.serializedName() + "/"
                        + source.getNamespace() + "/" + source.getPath());
        Scp914RecipeManager.RecipeDefinition definition =
                new Scp914RecipeManager.RecipeDefinition(
                        syntheticId,
                        requestedSetting,
                        List.of(),
                        List.of(),
                        List.copyOf(outputs),
                        List.of(),
                        List.of(),
                        1.0F,
                        false,
                        "");
        return Optional.of(new Scp914RecipeManager.RecipeMatch(
                definition, generic.itemUses(), List.of()));
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
