package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.mcreator.scpadditions.client.ClientModulePreferences;
import net.mcreator.scpadditions.client.FacilityChatLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Moves the vanilla chat input field underneath the top-down facility console. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Shadow protected EditBox input;

    @Inject(method = "init", at = @At("TAIL"))
    private void scpAdditions$positionFacilityInput(CallbackInfo ci) {
        if (!ClientModulePreferences.facilityChatInterfaceEnabled()) return;
        Minecraft minecraft = Minecraft.getInstance();
        ChatComponent chat = minecraft.gui.getChat();
        this.input.setX(FacilityChatLayout.inputX());
        this.input.setY(FacilityChatLayout.inputY(chat));
        this.input.setWidth(FacilityChatLayout.inputWidth(chat));
    }

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V",
                    ordinal = 0))
    private void scpAdditions$renderFacilityInputFrame(GuiGraphics graphics,
            int left, int top, int right, int bottom, int color) {
        if (!ClientModulePreferences.facilityChatInterfaceEnabled()) {
            graphics.fill(left, top, right, bottom, color);
            return;
        }
        FacilityChatLayout.drawInputFrame(graphics,
                Minecraft.getInstance().gui.getChat(), this.input);
    }
}
