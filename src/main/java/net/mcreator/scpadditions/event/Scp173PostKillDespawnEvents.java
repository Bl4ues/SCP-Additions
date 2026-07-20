package net.mcreator.scpadditions.event;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp173Entity;

import java.util.UUID;

@EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class Scp173PostKillDespawnEvents {
    private static final double NEARBY_PLAYER_RANGE = 48.0D;
    private static final double NEARBY_PLAYER_RANGE_SQR = NEARBY_PLAYER_RANGE * NEARBY_PLAYER_RANGE;

    private Scp173PostKillDespawnEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer killedPlayer)) return;
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof Scp173Entity scp173) || scp173.level().isClientSide) return;
        if (!(scp173.level() instanceof ServerLevel level)) return;
        if (!hasOtherLivingPlayerNearby(level, scp173, killedPlayer.getUUID())) scp173.discard();
    }

    private static boolean hasOtherLivingPlayerNearby(ServerLevel level, Scp173Entity scp173, UUID killedPlayerId) {
        for (Player player : level.players()) {
            if (!(player instanceof ServerPlayer serverPlayer)
                    || player.getUUID().equals(killedPlayerId)
                    || !player.isAlive()
                    || player.isRemoved()
                    || player.isCreative()
                    || player.isSpectator()) continue;
            if (serverPlayer.distanceToSqr(scp173) <= NEARBY_PLAYER_RANGE_SQR) return true;
        }
        return false;
    }
}
