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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Forwards positional client audio that a nearby active Intercom can hear. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class IntercomClientSoundBridge {
    private static final int MAX_PENDING_PER_TICK = 256;
    private static final Queue<PendingSound> PENDING =
            new ConcurrentLinkedQueue<>();

    private IntercomClientSoundBridge() {
    }

    /**
     * PlayLevelSoundEvent only covers sounds entering through Level.playSound.
     * A fair amount of block machinery and ambience creates SoundInstances
     * directly, so capture at the final SoundEngine-facing Forge event instead.
     *
     * PlaySoundEvent fires before AbstractSoundInstance has necessarily resolved
     * its concrete Sound. Calling getVolume/getPitch from this event can therefore
     * dereference a null resolved sound. Queue the instance and read those values
     * at the end of a client tick, after SoundEngine.play has finished resolving it.
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
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y)
                || !Double.isFinite(position.z)
                || !IntercomAudioClient.canCapture(level, position)) {
            return;
        }
        PENDING.offer(new PendingSound(level, sound, id, position));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            PENDING.clear();
            return;
        }

        for (int processed = 0; processed < MAX_PENDING_PER_TICK; processed++) {
            PendingSound pending = PENDING.poll();
            if (pending == null) break;
            if (pending.level != level
                    || !IntercomAudioClient.canCapture(level, pending.position)) {
                continue;
            }

            float volume;
            float pitch;
            try {
                volume = pending.sound.getVolume();
                pitch = pending.sound.getPitch();
            } catch (NullPointerException unresolvedSound) {
                // A SoundInstance which is still unresolved cannot be relayed
                // safely. More importantly, it must never take down the client.
                continue;
            }

            if (!Float.isFinite(volume) || !Float.isFinite(pitch)
                    || volume <= 0.0F) {
                continue;
            }
            Scp079AudioNetwork.reportIntercomSound(pending.id,
                    pending.position, volume, pitch);
        }
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

    private record PendingSound(ClientLevel level, SoundInstance sound,
            ResourceLocation id, Vec3 position) {
    }
}
