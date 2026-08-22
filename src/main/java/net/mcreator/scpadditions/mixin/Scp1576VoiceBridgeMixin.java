package net.mcreator.scpadditions.mixin;

import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.mcreator.scpadditions.compat.Scp1576VoiceChatBridge;
import net.mcreator.scpadditions.compat.SimpleVoiceChatCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks SCP-1576 before the normal dead-microphone route cancels the event. */
@Mixin(value = SimpleVoiceChatCompatibility.class, remap = false)
public abstract class Scp1576VoiceBridgeMixin {
    @Inject(method = "onMicrophone", at = @At("HEAD"), remap = false)
    private static void scpAdditions$relayDeadVoiceThrough1576(
            MicrophonePacketEvent event, CallbackInfo callback) {
        Scp1576VoiceChatBridge.relay(event);
    }

    @Inject(method = "onLocationalSound", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpAdditions$doNotMirror1576BackIntoDeathFeed(
            LocationalSoundPacketEvent event, CallbackInfo callback) {
        if (Scp1576VoiceChatBridge.isScp1576Channel(
                event.getPacket().getChannelId())) {
            callback.cancel();
        }
    }
}
