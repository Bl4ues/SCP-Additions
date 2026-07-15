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

/** Keeps explicit JSON recipes authoritative and supplies generic fallbacks only when needed. */
public final class Scp914RecipeBridge {
    private Scp914RecipeBridge() {
    }

    public static Optional<Scp914RecipeManager.RecipeMatch> findRecipe(
            ServerLevel level,
            Scp914RecipeManager.Setting setting,
            List<ItemEntity> itemEntities,
            List<Entity> entities) {
        Optional<Scp914RecipeManager.RecipeMatch> explicit =
                Scp914RecipeManager.findRecipe(setting, itemEntities, entities);
        if (explicit.isPresent()) return explicit;

        // A generic item transformation must never silently ignore an entity
        // that is also standing in the intake. Entity behavior remains explicit.
        if (entities != null && !entities.isEmpty()) return Optional.empty();

        return Scp914GenericRecipeResolver.find(level, setting, itemEntities)
                .flatMap(match -> convert(setting, match));
    }

    private static Optional<Scp914RecipeManager.RecipeMatch> convert(
            Scp914RecipeManager.Setting setting,
            Scp914GenericRecipeResolver.GenericMatch generic) {
        List<Scp914RecipeManager.ItemOutput> outputs = new ArrayList<>();
        for (ItemStack stack : generic.outputs()) {
            if (stack == null || stack.isEmpty()) continue;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null) continue;
            outputs.add(new Scp914RecipeManager.ItemOutput(id, stack.getCount()));
        }

        ResourceLocation syntheticId = new ResourceLocation("scp_additions",
                "inferred/" + setting.serializedName() + "/"
                        + generic.sourceRecipe().getNamespace() + "/"
                        + generic.sourceRecipe().getPath());
        Scp914RecipeManager.RecipeDefinition definition =
                new Scp914RecipeManager.RecipeDefinition(
                        syntheticId,
                        setting,
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
}
