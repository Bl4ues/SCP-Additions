package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CrtPostProcessor;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079FacilityMapScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Converts physical mouse coordinates into the logical pre-CRT map surface. */
@Mixin(Scp079FacilityMapScreen.class)
public abstract class Scp079FacilityMapWarpInputMixin {
    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true,
            ordinal = 0)
    private int scpclassifieddirective$renderMouseX(int value) {
        Scp079FacilityMapScreen self = (Scp079FacilityMapScreen) (Object) this;
        return (int) Math.round(Scp079CrtPostProcessor.logicalX(value,
                scpclassifieddirective$renderMouseYRaw, self.width, self.height));
    }

    private int scpclassifieddirective$renderMouseYRaw;

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true,
            ordinal = 1)
    private int scpclassifieddirective$renderMouseY(int value) {
        scpclassifieddirective$renderMouseYRaw = value;
        return value;
    }

    @ModifyVariable(method = {"mouseClicked", "mouseDragged", "mouseReleased", "mouseScrolled"},
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double scpclassifieddirective$mouseX(double value) {
        Scp079FacilityMapScreen self = (Scp079FacilityMapScreen) (Object) this;
        return Scp079CrtPostProcessor.logicalX(value,
                scpclassifieddirective$mouseYRaw, self.width, self.height);
    }

    private double scpclassifieddirective$mouseYRaw;

    @ModifyVariable(method = {"mouseClicked", "mouseDragged", "mouseReleased", "mouseScrolled"},
            at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double scpclassifieddirective$mouseY(double value) {
        Scp079FacilityMapScreen self = (Scp079FacilityMapScreen) (Object) this;
        double result = Scp079CrtPostProcessor.logicalY(
                scpclassifieddirective$mouseXRaw, value, self.width, self.height);
        scpclassifieddirective$mouseYRaw = value;
        return result;
    }

    private double scpclassifieddirective$mouseXRaw;
}
