package com.bl4ues.scpclassifieddirective.compat;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;

/** Cleans native Opus resources owned by custom voice-routing filters. */
@ForgeVoicechatPlugin
public final class Scp1576VoiceLifecyclePlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return "scp_classified_directive_1576_lifecycle";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(PlayerDisconnectedEvent.class,
                event -> {
                    Scp1576VoiceChatBridge.forgetSpeaker(event.getPlayerUuid());
                    SpeakerVoiceChatBridge.forgetOperator(event.getPlayerUuid());
                });
        registration.registerEvent(VoicechatServerStoppedEvent.class,
                event -> {
                    Scp1576VoiceChatBridge.closeAll();
                    SpeakerVoiceChatBridge.closeAll();
                });
    }
}
