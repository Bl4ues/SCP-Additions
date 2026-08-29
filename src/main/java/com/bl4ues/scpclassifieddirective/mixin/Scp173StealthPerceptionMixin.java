package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import com.bl4ues.scpclassifieddirective.stealth.PerceptionFramework;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies stealth acquisition to SCP-173's bespoke target/observation paths. */
@Mixin(Scp173Entity.class)
public abstract class Scp173StealthPerceptionMixin {
    @Inject(method = "findObservingPlayer", at = @At("RETURN"), cancellable = true)
    private void scpclassifieddirective$gateInitialObserver(
            CallbackInfoReturnable<Player> cir) {
        Player player = cir.getReturnValue();
        if (player == null) return;
        Scp173Entity scp173 = (Scp173Entity) (Object) this;
        if (!PerceptionFramework.canAcquire(scp173, player)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "findNearestTargetEntity", at = @At("RETURN"), cancellable = true)
    private void scpclassifieddirective$gateStrategicTarget(
            CallbackInfoReturnable<LivingEntity> cir) {
        if (!(cir.getReturnValue() instanceof Player player)) return;
        Scp173Entity scp173 = (Scp173Entity) (Object) this;
        if (!PerceptionFramework.canAcquire(scp173, player)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "updateClientObservation", at = @At("HEAD"), cancellable = true)
    private void scpclassifieddirective$gateClientObservation(
            ServerPlayer player, boolean visible, CallbackInfo ci) {
        if (!visible || player == null) return;
        Scp173Entity scp173 = (Scp173Entity) (Object) this;
        if (!PerceptionFramework.canAcquire(scp173, player)) ci.cancel();
    }
}
