package com.bl4ues.scpinventory.event;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.entity.Scp173Entity;
import com.bl4ues.scpinventory.entity.Scp173TargetConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public final class Scp173OpportunisticTargetEvents {
    private static final double TARGET_RANGE = 48.0D;
    private static final double TARGET_RANGE_SQR = TARGET_RANGE * TARGET_RANGE;

    private Scp173OpportunisticTargetEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || player.isCreative()
                || player.isSpectator()) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(TARGET_RANGE);
        for (Scp173Entity scp173 : player.serverLevel().getEntitiesOfClass(Scp173Entity.class, area,
                entity -> entity.isAlive() && entity.isActivated() && entity.distanceToSqr(player) <= TARGET_RANGE_SQR)) {
            LivingEntity target = findBestTarget(scp173);
            if (target != null && target != scp173.getTarget()) {
                scp173.setTarget(target);
            }
        }
    }

    private static LivingEntity findBestTarget(Scp173Entity scp173) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        AABB area = scp173.getBoundingBox().inflate(TARGET_RANGE);
        for (LivingEntity entity : scp173.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != scp173 && isValidPrey(entity))) {
            double distance = scp173.distanceToSqr(entity);
            if (distance <= TARGET_RANGE_SQR && distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }

    private static boolean isValidPrey(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return Scp173TargetConfig.isConfiguredTarget(entity);
    }
}
