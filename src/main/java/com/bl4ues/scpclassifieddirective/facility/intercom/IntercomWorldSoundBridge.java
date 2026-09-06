package com.bl4ues.scpclassifieddirective.facility.intercom;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.speaker.SpeakerBroadcastManager;
import com.bl4ues.scpclassifieddirective.network.Scp079AudioNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * Treats an active Intercom as a tiny local microphone for authoritative world
 * sounds. The five-block pickup falloff is applied before copies are emitted at
 * each mapped-room Speaker endpoint.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IntercomWorldSoundBridge {
    private static final Set<String> SELF_FEEDBACK = Set.of(
            "intercom_on", "intercom_off", "intercom_loop",
            "speaker_on", "speaker_off", "speaker_loop");

    private IntercomWorldSoundBridge() {
    }

    @SubscribeEvent
    public static void onPositionSound(PlayLevelSoundEvent.AtPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || event.getSound() == null || event.getPosition() == null) {
            return;
        }
        relay(level, event.getPosition(), event.getSound().value().getLocation(),
                event.getNewVolume(), event.getNewPitch());
    }

    @SubscribeEvent
    public static void onEntitySound(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || event.getSound() == null || event.getEntity() == null) {
            return;
        }
        relay(level, event.getEntity().position(),
                event.getSound().value().getLocation(), event.getNewVolume(),
                event.getNewPitch());
    }

    private static void relay(ServerLevel level, Vec3 sourcePosition,
            ResourceLocation sound, float volume, float pitch) {
        if (sound == null || volume <= 0.0F) return;
        if (ScpClassifiedDirectiveMod.MODID.equals(sound.getNamespace())
                && SELF_FEEDBACK.contains(sound.getPath())) return;

        for (SpeakerBroadcastManager.AudioSource output :
                SpeakerBroadcastManager.intercomAudioSources(level,
                        sourcePosition)) {
            float relayedVolume = Mth.clamp(volume * output.captureGain(),
                    0.0F, 4.0F);
            if (relayedVolume <= 0.001F) continue;
            Scp079AudioNetwork.sendSpeakerCue(level.getServer(),
                    output.dimension(), sound,
                    output.position().x, output.position().y,
                    output.position().z, relayedVolume,
                    Mth.clamp(pitch, 0.05F, 2.0F));
        }
    }
}
