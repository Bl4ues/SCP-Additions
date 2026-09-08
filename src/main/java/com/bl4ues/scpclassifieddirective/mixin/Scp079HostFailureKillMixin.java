package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079HostFailureDeathGuard;
import com.bl4ues.scpclassifieddirective.facility.Scp079SignalInterruptionManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Wraps the final host-destruction kill in a non-human death marker. */
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
            player.kill();
        } finally {
            Scp079HostFailureDeathGuard.end(player);
        }
    }
}
