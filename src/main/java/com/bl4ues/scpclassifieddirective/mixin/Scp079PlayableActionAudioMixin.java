package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.network.Scp079ActionAudioNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Emits door feedback only from the exact server action that succeeded. */
@Mixin(Scp079PlayableManager.class)
public abstract class Scp079PlayableActionAudioMixin {
    @Inject(method = "toggleDoor", at = @At("RETURN"), remap = false)
    private static void scpclassifieddirective$doorToggleSucceeded(
            ServerLevel level, BlockPos door,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || level == null) return;
        ServerPlayer controller = Scp079PlayableManager.controller(
                level.getServer());
        Scp079ActionAudioNetwork.send(controller,
                Scp079ActionAudioNetwork.Cue.DOOR_TOGGLE);
    }

    @Inject(method = "lockDoor", at = @At("RETURN"), remap = false)
    private static void scpclassifieddirective$doorLockSucceeded(
            ServerLevel level, BlockPos door,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || level == null) return;
        ServerPlayer controller = Scp079PlayableManager.controller(
                level.getServer());
        Scp079ActionAudioNetwork.send(controller,
                Scp079ActionAudioNetwork.Cue.LOCK_OR_TESLA);
    }
}
