package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079SpeakerCueSoundInstance;
import com.bl4ues.scpclassifieddirective.network.Scp079AudioNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forwards positional client audio that a nearby active Intercom can hear. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class IntercomClientSoundBridge {
    private IntercomClientSoundBridge() {
    }

    /**
     * PlayLevelSoundEvent only covers sounds entering through Level.playSound.
     * A fair amount of block machinery and ambience creates SoundInstances
     * directly, so capture at the final SoundEngine-facing Forge event instead.
     * Server-authored copies are deduplicated by IntercomWorldSoundBridge.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        ClientLevel level = Minecraft.getInstance().level;
        if (sound == null || level == null || sound.isRelative()
                || sound instanceof Scp079SpeakerCueSoundInstance) {
            return;
        }

        ResourceLocation id = sound.getLocation();
        if (id == null || isFacilityOutput(id)) return;
        Vec3 position = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        float volume = sound.getVolume();
        float pitch = sound.getPitch();
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y)
                || !Double.isFinite(position.z) || !Float.isFinite(volume)
                || !Float.isFinite(pitch) || volume <= 0.0F
                || !IntercomAudioClient.canCapture(level, position)) {
            return;
        }
        Scp079AudioNetwork.reportIntercomSound(id, position, volume, pitch);
    }

    private static boolean isFacilityOutput(ResourceLocation sound) {
        if (!ScpClassifiedDirectiveMod.MODID.equals(sound.getNamespace())) {
            return false;
        }
        return switch (sound.getPath()) {
            case "intercom_on", "intercom_off", "intercom_loop",
                    "speaker_on", "speaker_off", "speaker_loop" -> true;
            default -> false;
        };
    }
}
