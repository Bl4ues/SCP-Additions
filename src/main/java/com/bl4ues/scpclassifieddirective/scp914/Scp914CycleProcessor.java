package com.bl4ues.scpclassifieddirective.scp914;

import com.bl4ues.scpclassifieddirective.data.Scp914Processor;
import com.bl4ues.scpclassifieddirective.data.Scp914RecipeBridge;
import com.bl4ues.scpclassifieddirective.data.Scp914RecipeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Captures and commits one SCP-914 transformation from the physical intake
 * chamber defined by the GeckoLib model. No editable machine offsets or old
 * component blocks participate in this path.
 */
public final class Scp914CycleProcessor {
    private Scp914CycleProcessor() {
    }

    public static boolean process(ServerLevel level, BlockPos controllerPos,
            Direction front, Scp914RecipeManager.Setting setting) {
        AABB searchArea = Scp914Structure.intakeArea(controllerPos, front);
        Vec3 intakeCenter = Scp914Structure.intakeCenter(controllerPos, front);
        Vec3 outputCenter = Scp914Structure.outputCenter(controllerPos, front);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
                        searchArea,
                        item -> !item.isRemoved() && !item.getItem().isEmpty())
                .stream()
                .sorted(Comparator.comparingDouble(
                        entity -> entity.distanceToSqr(intakeCenter)))
                .toList();

        List<Entity> entities = level.getEntitiesOfClass(Entity.class,
                        searchArea,
                        entity -> !(entity instanceof ItemEntity)
                                && !(entity instanceof ServerPlayer)
                                && !entity.isRemoved())
                .stream()
                .sorted(Comparator.comparingDouble(
                        entity -> entity.distanceToSqr(intakeCenter)))
                .toList();

        List<ServerPlayer> players = level.getEntitiesOfClass(
                        ServerPlayer.class, searchArea,
                        player -> !player.isRemoved() && player.isAlive())
                .stream()
                .sorted(Comparator.comparingDouble(
                        entity -> entity.distanceToSqr(intakeCenter)))
                .toList();

        Optional<Scp914RecipeManager.RecipeMatch> match =
                Scp914RecipeBridge.findRecipe(level, setting, items, entities);
        if (match.isEmpty() && players.isEmpty()) return false;

        if (match.isPresent()) {
            Scp914Processor.applyRecipe(level, outputCenter, match.get());
        } else {
            Scp914Processor.consumeLooseItems(items);
        }
        for (ServerPlayer player : players) {
            Scp914Processor.processPlayer(player, outputCenter, setting);
        }
        return true;
    }
}
