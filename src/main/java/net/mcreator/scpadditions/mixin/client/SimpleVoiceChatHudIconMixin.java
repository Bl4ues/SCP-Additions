package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.client.Scp939ClientState;
import net.mcreator.scpadditions.client.SimpleVoiceChatHudBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Repositions only Simple Voice Chat's primary HUD status/microphone icon. */
@Pseudo
@Mixin(targets = "de.maxhenkel.voicechat.voice.client.RenderEvents", remap = false)
public abstract class SimpleVoiceChatHudIconMixin {
    @Inject(method = "renderIcon", at = @At("HEAD"), cancellable = true,
            require = 0, remap = false)
    private void scpAdditions$positionVoiceHudIcon(GuiGraphics graphics,
            ResourceLocation texture, CallbackInfo callback) {
        if (Scp939ClientState.pinned()) {
            callback.cancel();
            return;
        }
        if (!SimpleVoiceChatHudBridge.shouldRelocateHudIcon()) return;
        SimpleVoiceChatHudBridge.renderRelocatedHudIcon(graphics, texture);
        callback.cancel();
    }
}
