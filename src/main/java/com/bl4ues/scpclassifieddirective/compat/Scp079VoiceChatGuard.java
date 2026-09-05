package com.bl4ues.scpclassifieddirective.compat;

import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Prevents the implementation-only spectator anchor used by playable SCP-079
 * camera feeds from becoming a physical Simple Voice Chat microphone source.
 * Local-host control intentionally remains unaffected so voice can still come
 * from SCP-079's actual physical computer when that mode is being used.
 */
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
        if (sender != null && Scp079PlayableManager.isCameraMode(sender)) {
            SpeakerVoiceChatBridge.relay(event, sender);
            event.cancel();
        }
    }

    private static ServerPlayer minecraftPlayer(VoicechatConnection connection) {
        if (connection == null || connection.getPlayer() == null) return null;
        Object player = connection.getPlayer().getPlayer();
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }
}
