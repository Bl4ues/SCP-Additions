package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CrtPostProcessor;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079FacilityMapScreen;
import net.minecraft.client.Minecraft;
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
                rawMouseY(self), self.width, self.height));
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true,
            ordinal = 1)
    private int scpclassifieddirective$renderMouseY(int value) {
        Scp079FacilityMapScreen self = (Scp079FacilityMapScreen) (Object) this;
        return (int) Math.round(Scp079CrtPostProcessor.logicalY(
                rawMouseX(self), value, self.width, self.height));
    }

    @ModifyVariable(method = {"mouseClicked", "mouseDragged", "mouseReleased", "mouseScrolled"},
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double scpclassifieddirective$mouseX(double value) {
        Scp079FacilityMapScreen self = (Scp079FacilityMapScreen) (Object) this;
        return Scp079CrtPostProcessor.logicalX(value, rawMouseY(self),
                self.width, self.height);
    }

    @ModifyVariable(method = {"mouseClicked", "mouseDragged", "mouseReleased", "mouseScrolled"},
            at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double scpclassifieddirective$mouseY(double value) {
        Scp079FacilityMapScreen self = (Scp079FacilityMapScreen) (Object) this;
        return Scp079CrtPostProcessor.logicalY(rawMouseX(self), value,
                self.width, self.height);
    }

    private static double rawMouseX(Scp079FacilityMapScreen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.mouseHandler.xpos() * screen.width
                / Math.max(1.0D, minecraft.getWindow().getScreenWidth());
    }

    private static double rawMouseY(Scp079FacilityMapScreen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.mouseHandler.ypos() * screen.height
                / Math.max(1.0D, minecraft.getWindow().getScreenHeight());
    }
}
