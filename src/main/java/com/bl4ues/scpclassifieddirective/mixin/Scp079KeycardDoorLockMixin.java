package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079DoorControlPolicy;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Server authority for the no-LOCK rule on keycard-controlled doors. */
@Mixin(value = Scp079PlayableManager.class, remap = false)
public abstract class Scp079KeycardDoorLockMixin {
    @Inject(method = "lockDoor", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$rejectKeycardDoorLock(
            ServerLevel level, BlockPos door,
            CallbackInfoReturnable<Boolean> cir) {
        if (Scp079DoorControlPolicy.hasKeycardReader(level, door)) {
            cir.setReturnValue(false);
        }
    }
}
