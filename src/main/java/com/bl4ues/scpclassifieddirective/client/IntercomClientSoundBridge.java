package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.network.Scp079AudioNetwork;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forwards genuinely client-local positional ambience to nearby Intercoms. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class IntercomClientSoundBridge {
    private IntercomClientSoundBridge() {
    }

    @SubscribeEvent
    public static void onPositionSound(PlayLevelSoundEvent.AtPosition event) {
        if (!(event.getLevel() instanceof ClientLevel level)
                || event.getSound() == null || event.getPosition() == null) {
            return;
        }
        relay(level, event.getPosition(), event.getSound().value().getLocation(),
                event.getNewVolume(), event.getNewPitch());
    }

    @SubscribeEvent
    public static void onEntitySound(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getLevel() instanceof ClientLevel level)
                || event.getSound() == null || event.getEntity() == null) {
            return;
        }
        relay(level, event.getEntity().position(),
                event.getSound().value().getLocation(),
                event.getNewVolume(), event.getNewPitch());
    }

    private static void relay(ClientLevel level, Vec3 position,
            ResourceLocation sound, float volume, float pitch) {
        if (sound == null || volume <= 0.0F
                || !IntercomAudioClient.canCapture(level, position)) return;
        Scp079AudioNetwork.reportIntercomSound(sound, position, volume, pitch);
    }
}
