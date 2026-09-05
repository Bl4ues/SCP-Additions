package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps camera-feed prompts tied to the crosshair instead of a free cursor. */
@Mixin(Scp079PlayableVisualsV2.class)
public abstract class Scp079PlayableVisualsV2CursorMixin {
    @Redirect(method = "pointer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;isDown()Z"),
            remap = false)
    private static boolean scpclassifieddirective$neverUseFreeCursor(
            KeyMapping keyMapping) {
        return false;
    }

    @ModifyConstant(method = "renderCameraHud",
            constant = @Constant(stringValue = "HOLD SHIFT  CURSOR"),
            remap = false)
    private static String scpclassifieddirective$removeCursorHint(
            String original) {
        return "";
    }
}
