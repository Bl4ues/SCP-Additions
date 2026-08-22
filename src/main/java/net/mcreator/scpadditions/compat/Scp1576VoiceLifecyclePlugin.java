package net.mcreator.scpadditions.compat;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;

/** Cleans native Opus resources owned by the SCP-1576 voice filter. */
@ForgeVoicechatPlugin
public final class Scp1576VoiceLifecyclePlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return "scp_additions_1576_lifecycle";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(PlayerDisconnectedEvent.class,
                event -> Scp1576VoiceChatBridge.forgetSpeaker(
                        event.getPlayerUuid()));
        registration.registerEvent(VoicechatServerStoppedEvent.class,
                event -> Scp1576VoiceChatBridge.closeAll());
    }
}
