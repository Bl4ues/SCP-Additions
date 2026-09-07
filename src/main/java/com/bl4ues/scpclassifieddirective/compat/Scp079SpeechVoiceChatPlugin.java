package com.bl4ues.scpclassifieddirective.compat;

import com.bl4ues.scpclassifieddirective.facility.Scp079SpeechManager;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;

/** Optional Simple Voice Chat transport for generated SCP-079 speech. */
@ForgeVoicechatPlugin
public final class Scp079SpeechVoiceChatPlugin implements VoicechatPlugin {
    private static final String PLUGIN_ID =
            "scp_classified_directive_079_speech";
    private static volatile Scp079SpeechVoiceChatBackend backend;

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class,
                Scp079SpeechVoiceChatPlugin::onStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class,
                Scp079SpeechVoiceChatPlugin::onStopped);
    }

    private static void onStarted(VoicechatServerStartedEvent event) {
        Scp079SpeechVoiceChatBackend next =
                new Scp079SpeechVoiceChatBackend(event.getVoicechat());
        Scp079SpeechVoiceChatBackend previous = backend;
        backend = next;
        if (previous != null) Scp079SpeechManager.removeBackend(previous);
        Scp079SpeechManager.installBackend(next);
    }

    private static void onStopped(VoicechatServerStoppedEvent event) {
        Scp079SpeechVoiceChatBackend previous = backend;
        backend = null;
        if (previous != null) Scp079SpeechManager.removeBackend(previous);
    }
}
