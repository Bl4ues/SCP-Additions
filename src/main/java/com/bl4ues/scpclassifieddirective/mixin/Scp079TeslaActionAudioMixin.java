package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079TeslaSuppression;
import com.bl4ues.scpclassifieddirective.network.Scp079ActionAudioNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Emits the SCP-079 Tesla cue only after manual suppression succeeds. */
@Mixin(Scp079TeslaSuppression.class)
public abstract class Scp079TeslaActionAudioMixin {
    @Inject(method = "tryPlayerSuppress", at = @At("RETURN"), remap = false)
    private static void scpclassifieddirective$teslaSuppressionSucceeded(
            ServerLevel level, BlockPos gatePos,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || level == null) return;
        ServerPlayer controller = Scp079PlayableManager.controller(
                level.getServer());
        Scp079ActionAudioNetwork.send(controller,
                Scp079ActionAudioNetwork.Cue.LOCK_OR_TESLA);
    }
}
