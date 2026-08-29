package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.stealth.AdvancedCrouchController;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Smooths the local camera between standing, crouching and crawl eye heights. */
@Mixin(Player.class)
public abstract class PlayerSmoothCrouchMixin {
    @Inject(method = "getStandingEyeHeight", at = @At("RETURN"), cancellable = true)
    private void scpclassifieddirective$smoothCrouchEyeHeight(Pose pose,
            EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        cir.setReturnValue(AdvancedCrouchController.smoothEyeHeight(
                player, pose, cir.getReturnValue()));
    }
}
