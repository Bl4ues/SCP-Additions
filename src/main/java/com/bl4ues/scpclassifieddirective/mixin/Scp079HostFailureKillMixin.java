package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079HostFailureDeathGuard;
import com.bl4ues.scpclassifieddirective.facility.Scp079SignalInterruptionManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Wraps the final host-destruction kill in a non-human SCP-079 death. */
@Mixin(value = Scp079SignalInterruptionManager.class, remap = false)
public abstract class Scp079HostFailureKillMixin {
    @Redirect(method = "onServerTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;kill()V"),
            require = 1)
    private static void scpclassifieddirective$killWithoutHumanCorpse(
            ServerPlayer player) {
        Scp079HostFailureDeathGuard.begin(player);
        try {
            DamageSource source = new DamageSource(player.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.GENERIC_KILL)) {
                @Override
                public Component getLocalizedDeathMessage(LivingEntity entity) {
                    return Component.literal("SCP-079 was destroyed.");
                }
            };
            player.hurt(source, Float.MAX_VALUE);
            if (player.isAlive()) {
                player.setHealth(0.0F);
                player.die(source);
            }
        } finally {
            Scp079HostFailureDeathGuard.end(player);
        }
    }
}
