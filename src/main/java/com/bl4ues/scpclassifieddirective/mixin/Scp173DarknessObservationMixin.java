package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp173ObservationLighting;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;

/** Makes SCP-173 observation depend on actual visible illumination. */
@Mixin(value = Scp173Entity.class, remap = false)
public abstract class Scp173DarknessObservationMixin {
    @Shadow private LivingEntity lastObservationGraceObserver;
    @Shadow @Final private Map<UUID, Integer> clientObservationUntilTicks;

    @Inject(method = "shouldFreezeFor(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void scpclassifieddirective$requireVisibleLight(
            LivingEntity observer, CallbackInfoReturnable<Boolean> cir) {
        Scp173Entity statue = (Scp173Entity) (Object) this;
        if (observer instanceof ServerPlayer player
                && Scp079PlayableManager.isController(player)) {
            cir.setReturnValue(false);
            return;
        }
        if (!Scp173ObservationLighting.canObserve(statue, observer)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hasObservationGrace()Z", at = @At("HEAD"),
            cancellable = true, remap = false)
    private void scpclassifieddirective$dropGraceInDarkness(
            CallbackInfoReturnable<Boolean> cir) {
        LivingEntity observer = lastObservationGraceObserver;
        if (observer != null && !Scp173ObservationLighting.canObserve(
                (Scp173Entity) (Object) this, observer)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hasClientObservationLock()Z", at = @At("HEAD"),
            cancellable = true, remap = false)
    private void scpclassifieddirective$ignoreDarkClientLocks(
            CallbackInfoReturnable<Boolean> cir) {
        Scp173Entity statue = (Scp173Entity) (Object) this;
        if (Scp173ObservationLighting.ambientLight(statue)
                >= Scp173ObservationLighting.MIN_OBSERVATION_LIGHT) {
            return;
        }
        boolean assistedObserver = false;
        for (Player player : statue.level().players()) {
            if (!clientObservationUntilTicks.containsKey(player.getUUID())) continue;
            if (player instanceof ServerPlayer serverPlayer
                    && Scp079PlayableManager.isController(serverPlayer)) continue;
            if (Scp173ObservationLighting.hasIndependentVisionAssist(
                    player, statue)) {
                assistedObserver = true;
                break;
            }
        }
        if (!assistedObserver) cir.setReturnValue(false);
    }

    @Inject(method = "hasClientObservationLock(Lnet/minecraft/world/entity/player/Player;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void scpclassifieddirective$ignoreDarkPlayerLock(Player player,
            CallbackInfoReturnable<Boolean> cir) {
        Scp173Entity statue = (Scp173Entity) (Object) this;
        if (player instanceof ServerPlayer serverPlayer
                && Scp079PlayableManager.isController(serverPlayer)) {
            cir.setReturnValue(false);
            return;
        }
        if (!Scp173ObservationLighting.canObserve(statue, player)) {
            cir.setReturnValue(false);
        }
    }
}
