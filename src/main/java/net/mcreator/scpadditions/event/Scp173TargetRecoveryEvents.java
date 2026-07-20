package net.mcreator.scpadditions.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.entity.Scp173Entity;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
                || !player.isAlive()
                || !ScpAdditionsModulesConfig.get().scp173.enabled) return;

        AABB area = player.getBoundingBox().inflate(RECOVERY_RANGE);
        for (Scp173Entity scp173 : player.serverLevel().getEntitiesOfClass(Scp173Entity.class, area,
                entity -> entity.isAlive() && entity.distanceToSqr(player) <= RECOVERY_RANGE_SQR)) {
            if (!scp173.isActivated()) continue;
            Entity target = scp173.getTarget();
            boolean invalidTarget = !(target instanceof Player targetPlayer)
                    || !targetPlayer.isAlive()
                    || targetPlayer.isRemoved()
                    || targetPlayer.isCreative()
                    || targetPlayer.isSpectator();
            if (invalidTarget && scp173.isObservedBy(player)) scp173.setTarget(player);
        }
    }
}
