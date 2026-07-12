package com.bl4ues.scpinventory.event;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.entity.Scp173Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public final class Scp173TargetRecoveryEvents {
    private static final double RECOVERY_RANGE = 48.0D;
    private static final double RECOVERY_RANGE_SQR = RECOVERY_RANGE * RECOVERY_RANGE;

    private Scp173TargetRecoveryEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || player.isCreative()
                || player.isSpectator()
                || !player.isAlive()) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(RECOVERY_RANGE);
        for (Scp173Entity scp173 : player.serverLevel().getEntitiesOfClass(Scp173Entity.class, area,
                entity -> entity.isAlive() && entity.distanceToSqr(player) <= RECOVERY_RANGE_SQR)) {
            if (!scp173.isActivated()) {
                continue;
            }

            Entity target = scp173.getTarget();
            boolean invalidTarget = !(target instanceof Player targetPlayer)
                    || !targetPlayer.isAlive()
                    || targetPlayer.isRemoved()
                    || targetPlayer.isCreative()
                    || targetPlayer.isSpectator();
            if (invalidTarget && scp173.isObservedBy(player)) {
                scp173.setTarget(player);
            }
        }
    }
}
