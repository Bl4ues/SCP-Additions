package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import com.bl4ues.scpclassifieddirective.client.CustomPauseMenuScreen;
import com.bl4ues.scpclassifieddirective.client.PauseMenuLanCompatibilityClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes the custom LAN panel through modded ShareToLanScreen controls. */
@Mixin(value = CustomPauseMenuScreen.class, remap = false)
public abstract class PauseMenuLanCompatibilityMixin {
    // CustomPauseMenuScreen is ours, but these overrides inherit Minecraft
    // methods and therefore receive SRG names in the production JAR. Each hook
    // accepts both the development/Mojmap name and the 1.20.1 SRG runtime name.
    @Inject(method = {"render", "m_88315_"}, at = @At("TAIL"), require = 1)
    private void scpClassifiedDirective$renderLanModOptions(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        PauseMenuLanCompatibilityClient.render(
                (CustomPauseMenuScreen) (Object) this,
                graphics, mouseX, mouseY, partialTick);
    }

    @Inject(method = {"mouseClicked", "m_6375_"}, at = @At("HEAD"),
            cancellable = true, require = 1)
    private void scpClassifiedDirective$lanMouseClicked(double mouseX, double mouseY,
            int button, CallbackInfoReturnable<Boolean> callback) {
        if (PauseMenuLanCompatibilityClient.mouseClicked(
                (CustomPauseMenuScreen) (Object) this,
                mouseX, mouseY, button)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = {"mouseReleased", "m_6348_"}, at = @At("HEAD"),
            cancellable = true, require = 1)
    private void scpClassifiedDirective$lanMouseReleased(double mouseX, double mouseY,
            int button, CallbackInfoReturnable<Boolean> callback) {
        if (PauseMenuLanCompatibilityClient.mouseReleased(
                (CustomPauseMenuScreen) (Object) this,
                mouseX, mouseY, button)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = {"mouseDragged", "m_7979_"}, at = @At("HEAD"),
            cancellable = true, require = 1)
    private void scpClassifiedDirective$lanMouseDragged(double mouseX, double mouseY,
            int button, double dragX, double dragY,
            CallbackInfoReturnable<Boolean> callback) {
        if (PauseMenuLanCompatibilityClient.mouseDragged(
                (CustomPauseMenuScreen) (Object) this,
                mouseX, mouseY, button, dragX, dragY)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = {"mouseScrolled", "m_6050_"}, at = @At("HEAD"),
            cancellable = true, require = 1)
    private void scpClassifiedDirective$lanMouseScrolled(double mouseX, double mouseY,
            double delta, CallbackInfoReturnable<Boolean> callback) {
        if (PauseMenuLanCompatibilityClient.mouseScrolled(
                (CustomPauseMenuScreen) (Object) this,
                mouseX, mouseY, delta)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = {"keyPressed", "m_7933_"}, at = @At("HEAD"),
            cancellable = true, require = 1)
    private void scpClassifiedDirective$lanKeyPressed(int keyCode, int scanCode,
            int modifiers, CallbackInfoReturnable<Boolean> callback) {
        if (PauseMenuLanCompatibilityClient.keyPressed(
                (CustomPauseMenuScreen) (Object) this,
                keyCode, scanCode, modifiers)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = {"keyReleased", "m_7920_"}, at = @At("HEAD"),
            cancellable = true, require = 1)
    private void scpClassifiedDirective$lanKeyReleased(int keyCode, int scanCode,
            int modifiers, CallbackInfoReturnable<Boolean> callback) {
        if (PauseMenuLanCompatibilityClient.keyReleased(
                (CustomPauseMenuScreen) (Object) this,
                keyCode, scanCode, modifiers)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = {"charTyped", "m_5534_"}, at = @At("HEAD"),
            cancellable = true, require = 1)
    private void scpClassifiedDirective$lanCharTyped(char codePoint, int modifiers,
            CallbackInfoReturnable<Boolean> callback) {
        if (PauseMenuLanCompatibilityClient.charTyped(
                (CustomPauseMenuScreen) (Object) this,
                codePoint, modifiers)) {
            callback.setReturnValue(true);
        }
    }
}
