package com.bl4ues.scpclassifieddirective.compat;

import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.server.level.ServerPlayer;

/** Routes SCP-079 and physical Intercom microphone traffic into facility Speakers. */
@ForgeVoicechatPlugin
public final class Scp079VoiceChatGuard implements VoicechatPlugin {
    private static final String PLUGIN_ID =
            "scp_classified_directive_079_voice_guard";

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class,
                Scp079VoiceChatGuard::onMicrophone, Integer.MAX_VALUE);
    }

    private static void onMicrophone(MicrophonePacketEvent event) {
        if (!ModCompatibilityConfig.simpleVoiceChatEnabled()) return;
        ServerPlayer sender = minecraftPlayer(event.getSenderConnection());
        if (sender == null) return;

        if (Scp079PlayableManager.isCameraMode(sender)) {
            // The camera-mode player is an implementation anchor, not a physical
            // microphone. Only SCP-079's explicit Speaker route is allowed.
            SpeakerVoiceChatBridge.relay(event, sender);
            event.cancel();
            return;
        }

        // Ordinary proximity voice remains untouched, but an active Intercom
        // microphone within five blocks receives a filtered copy for its room.
        SpeakerVoiceChatBridge.relayIntercom(event, sender);
    }

    private static ServerPlayer minecraftPlayer(VoicechatConnection connection) {
        if (connection == null || connection.getPlayer() == null) return null;
        Object player = connection.getPlayer().getPlayer();
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }
}
