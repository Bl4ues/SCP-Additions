package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079RoomAbilityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.SignalGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes active blackout light cells report no redstone power for their duration. */
@Mixin(SignalGetter.class)
public abstract class Scp079BlackoutRedstoneMixin {
    @Inject(method = "hasNeighborSignal", at = @At("HEAD"), cancellable = true)
    private void scpclassifieddirective$suppressBlackoutLightPower(
            BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (self instanceof ServerLevel level
                && Scp079RoomAbilityManager.isLightSuppressed(level, pos)) {
            cir.setReturnValue(false);
        }
    }
}
