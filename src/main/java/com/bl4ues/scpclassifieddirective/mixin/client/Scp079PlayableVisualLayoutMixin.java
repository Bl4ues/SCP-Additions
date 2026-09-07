package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Moves edge HUD text inward while keeping the Facility Map power anchor. */
@Mixin(Scp079PlayableVisualsV2.class)
public abstract class Scp079PlayableVisualLayoutMixin {
    @ModifyConstant(method = "renderLocalHud",
            constant = @Constant(intValue = 24), require = 1)
    private static int scpclassifieddirective$localHorizontalInset(int original) {
        return 34;
    }

    @ModifyConstant(method = "renderLocalHud",
            constant = @Constant(intValue = 23), require = 1)
    private static int scpclassifieddirective$localTopInset(int original) {
        return 33;
    }

    @ModifyConstant(method = "renderLocalHud",
            constant = @Constant(intValue = 44), require = 1)
    private static int scpclassifieddirective$localSecondLine(int original) {
        return 54;
    }

    @ModifyConstant(method = "renderCameraHud",
            constant = @Constant(intValue = 24), require = 1)
    private static int scpclassifieddirective$cameraHorizontalInset(int original) {
        return 34;
    }

    @ModifyConstant(method = "renderCameraHud",
            constant = @Constant(intValue = 23), require = 1)
    private static int scpclassifieddirective$cameraTopInset(int original) {
        return 33;
    }

    @ModifyConstant(method = "renderCameraHud",
            constant = @Constant(intValue = 43), require = 1)
    private static int scpclassifieddirective$cameraSecondLine(int original) {
        return 53;
    }

    @ModifyConstant(method = "renderCameraHud",
            constant = @Constant(intValue = 61), require = 1)
    private static int scpclassifieddirective$cameraThirdLine(int original) {
        return 71;
    }
}
