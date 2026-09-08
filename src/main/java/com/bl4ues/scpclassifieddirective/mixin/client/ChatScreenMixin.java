package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import com.bl4ues.scpclassifieddirective.client.ClientModulePreferences;
import com.bl4ues.scpclassifieddirective.client.FacilityChatLayout;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079ChatLayout;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079TerminalChatHistoryClient;
import com.bl4ues.scpclassifieddirective.network.Scp079SpeechNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Relocates chat for facility UI and gives playable SCP-079 its own terminal. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    private static final int VANILLA_EDITABLE_TEXT = 0xE0E0E0;
    private static final int VANILLA_UNEDITABLE_TEXT = 0x707070;

    @Shadow protected EditBox input;

    @Inject(method = "init", at = @At("TAIL"))
    private void scpClassifiedDirective$positionFacilityInput(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (Scp079PlayableClient.active()) {
            scpClassifiedDirective$setVanillaInputTextHidden(true);
            Scp079ChatLayout.positionInput(this.input,
                    minecraft.getWindow().getGuiScaledWidth(),
                    minecraft.getWindow().getGuiScaledHeight());
            return;
        }
        scpClassifiedDirective$setVanillaInputTextHidden(false);
        if (!ClientModulePreferences.facilityChatInterfaceEnabled()) return;
        ChatComponent chat = minecraft.gui.getChat();
        FacilityChatLayout.beginOpenAnimation();
        this.input.setX(FacilityChatLayout.inputX());
        this.input.setY(FacilityChatLayout.inputY(chat)
                + FacilityChatLayout.openOffsetScreen(chat)
                + FacilityChatLayout.INPUT_TEXT_OFFSET);
        this.input.setWidth(FacilityChatLayout.inputWidth(chat));
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void scpClassifiedDirective$animateFacilityInput(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (Scp079PlayableClient.active()) {
            // The redirect below is authoritative. Transparent native text is a
            // second line of defence for renderer replacements/injections from
            // other mods that invoke the EditBox through a different call site.
            scpClassifiedDirective$setVanillaInputTextHidden(true);
            Scp079ChatLayout.positionInput(this.input,
                    minecraft.getWindow().getGuiScaledWidth(),
                    minecraft.getWindow().getGuiScaledHeight());
            return;
        }
        scpClassifiedDirective$setVanillaInputTextHidden(false);
        if (!ClientModulePreferences.facilityChatInterfaceEnabled()) return;
        ChatComponent chat = minecraft.gui.getChat();
        this.input.setX(FacilityChatLayout.inputX());
        this.input.setY(FacilityChatLayout.inputY(chat)
                + FacilityChatLayout.openOffsetScreen(chat)
                + FacilityChatLayout.INPUT_TEXT_OFFSET);
        this.input.setWidth(FacilityChatLayout.inputWidth(chat));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void scpClassifiedDirective$renderScp079Input(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!Scp079PlayableClient.active()) return;
        Scp079ChatLayout.renderInput(graphics, this.input,
                Minecraft.getInstance());
    }

    /**
     * Non-command input from playable SCP-079 is speech, not global text chat.
     * Store it in the dedicated terminal history and send only the authoritative
     * speech request to the server. Slash commands retain vanilla handling.
     */
    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void scpClassifiedDirective$routeScp079Speech(String message,
            boolean addToRecentChat, CallbackInfoReturnable<Boolean> cir) {
        if (!Scp079PlayableClient.active()) return;
        ChatScreen screen = (ChatScreen) (Object) this;
        String normalized = screen.normalizeChatMessage(message);
        if (normalized.isBlank() || normalized.startsWith("/")) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (addToRecentChat) minecraft.gui.getChat().addRecentChat(normalized);
        Scp079TerminalChatHistoryClient.record(
                Scp079ChatLayout.terminalText("079> " + normalized));
        Scp079SpeechNetwork.request(normalized);
        cir.setReturnValue(true);
    }

    /**
     * Keep EditBox only as an input engine. Rendering it underneath the terminal
     * panel produced the tiny duplicate modern-font text visible behind SCP-079.
     */
    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"),
            require = 0)
    private void scpClassifiedDirective$hideVanilla079Input(EditBox input,
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!Scp079PlayableClient.active()) {
            input.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V",
                    ordinal = 0))
    private void scpClassifiedDirective$renderFacilityInputFrame(GuiGraphics graphics,
            int left, int top, int right, int bottom, int color) {
        if (Scp079PlayableClient.active()) {
            graphics.fill(left, top, right, bottom, 0x00000000);
            return;
        }
        if (!ClientModulePreferences.facilityChatInterfaceEnabled()) {
            graphics.fill(left, top, right, bottom, color);
            return;
        }
        FacilityChatLayout.drawInputFrame(graphics,
                Minecraft.getInstance().gui.getChat(), this.input);
    }

    private void scpClassifiedDirective$setVanillaInputTextHidden(boolean hidden) {
        if (this.input == null) return;
        this.input.setTextColor(hidden ? 0x00000000 : VANILLA_EDITABLE_TEXT);
        this.input.setTextColorUneditable(hidden
                ? 0x00000000 : VANILLA_UNEDITABLE_TEXT);
    }
}
