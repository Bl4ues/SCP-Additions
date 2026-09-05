package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayablePersistence;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the playable role persistent while Spectator remains implementation-only. */
@Mixin(Scp079PlayableManager.class)
public abstract class Scp079PlayablePersistenceMixin {
    @Inject(method = "assume", at = @At("HEAD"), remap = false)
    private static void scpclassifieddirective$captureOrigin(ServerPlayer player,
            BlockPos hostPos, CallbackInfoReturnable<Boolean> cir) {
        Scp079PlayablePersistence.beginAssume(player);
    }

    @Inject(method = "assume", at = @At("RETURN"), remap = false)
    private static void scpclassifieddirective$storeRole(ServerPlayer player,
            BlockPos hostPos, CallbackInfoReturnable<Boolean> cir) {
        Scp079PlayablePersistence.finishAssume(player, hostPos,
                cir.getReturnValue());
    }

    @Inject(method = "release", at = @At("RETURN"), remap = false)
    private static void scpclassifieddirective$restorePersistentOrigin(
            ServerPlayer player, CallbackInfo ci) {
        Scp079PlayablePersistence.restoreOriginalAndClear(player);
    }
}
