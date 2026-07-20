package net.mcreator.scpadditions.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Selects between explicit JSON recipes and inferred crafting behavior.
 * Explicit definitions are always authoritative; inferred recipes are only a
 * fallback when no configured recipe matches the selected setting and intake.
 */
public final class Scp914RecipeBridge {
    private Scp914RecipeBridge() {
    }

    public static Optional<Scp914RecipeManager.RecipeMatch> findRecipe(
            ServerLevel level,
            Scp914RecipeManager.Setting setting,
            List<ItemEntity> itemEntities,
            List<Entity> entities) {
        Optional<Scp914RecipeManager.RecipeMatch> selected = selectForSetting(
                level, setting, itemEntities, entities);

        // Very Fine may reuse a valid Fine transformation only when no explicit
        // or inferred Very Fine transformation exists.
        if (selected.isEmpty() && setting == Scp914RecipeManager.Setting.VERY_FINE) {
            selected = selectForSetting(level, Scp914RecipeManager.Setting.FINE,
                    itemEntities, entities);
        }

        return selected.map(match -> consumeCompleteIntake(match, itemEntities));
    }

    private static Optional<Scp914RecipeManager.RecipeMatch> selectForSetting(
            ServerLevel level,
            Scp914RecipeManager.Setting setting,
            List<ItemEntity> itemEntities,
            List<Entity> entities) {
        Optional<Scp914RecipeManager.RecipeMatch> explicit =
                Scp914RecipeManager.findRecipe(setting, itemEntities, entities);
        if (explicit.isPresent()) {
            return explicit;
        }

        if (entities != null && !entities.isEmpty()) {
            return Optional.empty();
        }
        return Scp914GenericRecipeResolver.find(level, setting, itemEntities)
                .flatMap(match -> convert(setting, match));
    }

    private static Optional<Scp914RecipeManager.RecipeMatch> convert(
            Scp914RecipeManager.Setting requestedSetting,
            Scp914GenericRecipeResolver.GenericMatch generic) {
        List<Scp914RecipeManager.ItemOutput> outputs = new ArrayList<>();
        for (ItemStack stack : generic.outputs()) {
            if (stack == null || stack.isEmpty()) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id == null) continue;
            outputs.add(new Scp914RecipeManager.ItemOutput(id, stack.getCount()));
        }
        if (outputs.isEmpty()) return Optional.empty();

        ResourceLocation source = generic.sourceRecipe();
        ResourceLocation syntheticId = ResourceLocation.fromNamespaceAndPath("scp_additions", "inferred/" + requestedSetting.serializedName() + "/"
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
                if (entity == null || entity.isRemoved() || entity.getItem().isEmpty()) continue;
                fullUses.add(new Scp914RecipeManager.ItemUse(entity, entity.getItem().getCount()));
            }
        }
        return new Scp914RecipeManager.RecipeMatch(
                match.recipe(), List.copyOf(fullUses), match.entityUses());
    }
}
