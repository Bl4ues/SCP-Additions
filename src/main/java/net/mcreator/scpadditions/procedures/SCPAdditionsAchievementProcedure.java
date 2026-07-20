package net.mcreator.scpadditions.procedures;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementHolder;

import net.mcreator.scpadditions.ScpAdditionsMod;

import javax.annotation.Nullable;

@EventBusSubscriber
public class SCPAdditionsAchievementProcedure {
    private static final ResourceLocation ROOT_ADVANCEMENT =
            ResourceLocation.parse("scp_additions:scp_additions_ach");

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        execute(event, event.getEntity());
    }

    public static void execute(Entity entity) {
        execute(null, entity);
    }

    private static void execute(@Nullable Event event, Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        AdvancementHolder advancement = player.server.getAdvancements()
                .get(ROOT_ADVANCEMENT);
        if (advancement == null) {
            ScpAdditionsMod.LOGGER.error(
                    "Unable to award missing root advancement {}",
                    ROOT_ADVANCEMENT);
            return;
        }

        AdvancementProgress progress = player.getAdvancements()
                .getOrStartProgress(advancement);
        if (!progress.isDone()) {
            for (String criteria : progress.getRemainingCriteria()) {
                player.getAdvancements().award(advancement, criteria);
            }
        }
    }
}
