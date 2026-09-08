package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079TerminalChatHistoryClient;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fixed, preference-independent chat history for playable SCP-079. */
@Mixin(value = ChatComponent.class, priority = 3000)
public abstract class Scp079ChatComponentMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void scpclassifieddirective$renderScp079Chat(GuiGraphics graphics,
            int tickCount, int mouseX, int mouseY, CallbackInfo ci) {
        if (!Scp079PlayableClient.active()) return;
        ci.cancel();
        Minecraft minecraft = Minecraft.getInstance();
        Scp079TerminalChatHistoryClient.render(graphics, minecraft,
                minecraft.screen instanceof ChatScreen);
    }

    /** Capture ordinary/system chat that still reaches the local client. */
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"), require = 0)
    private void scpclassifieddirective$captureSimpleMessage(Component message,
            CallbackInfo ci) {
        if (Scp079PlayableClient.active()) {
            Scp079TerminalChatHistoryClient.record(message);
        }
    }

    /** Capture signed/tagged player chat when it bypasses the simple overload. */
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), require = 0)
    private void scpclassifieddirective$captureTaggedMessage(Component message,
            MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
        if (Scp079PlayableClient.active()) {
            Scp079TerminalChatHistoryClient.record(message);
        }
    }
}
