package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.network.Scp079AudioNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * Sends a quiet copy of sounds around SCP-079's physical host to its human
 * operator while the operator is inhabiting a remote surveillance camera.
 * Camera-local sounds remain normal server sounds and are therefore spatially
 * anchored to the camera rather than being mixed into this relay.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079RemoteHostAudioBridge {
    private static final double HOST_LISTEN_RADIUS = 18.0D;
    private static final double HOST_LISTEN_RADIUS_SQR =
            HOST_LISTEN_RADIUS * HOST_LISTEN_RADIUS;
    private static final float RELAY_VOLUME = 0.82F;
    private static final Set<String> LOCAL_CRT_CUES = Set.of(
            "scp079_1", "scp079_2");

    private Scp079RemoteHostAudioBridge() {
    }

    @SubscribeEvent
    public static void onPositionSound(PlayLevelSoundEvent.AtPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || event.getSound() == null
                || event.getPosition() == null
                || level.getServer() == null) return;

        ServerPlayer operator = Scp079PlayableManager.controller(level.getServer());
        if (operator == null || !Scp079PlayableManager.isCameraMode(operator)) {
            return;
        }
        BlockPos hostPos = Scp079PlayableManager.hostPosition(operator);
        if (hostPos == null || !level.dimension().equals(
                operator.getServer().getLevel(operator.level().dimension()) == null
                        ? level.dimension() : level.dimension())) {
            return;
        }

        Vec3 sourcePos = event.getPosition();
        Vec3 host = Vec3.atCenterOf(hostPos);
        if (sourcePos.distanceToSqr(host) > HOST_LISTEN_RADIUS_SQR) return;

        // If the camera itself is close enough to receive this sound naturally,
        // do not create a second host-monitor copy. In that case the operator is
        // hearing it through the camera microphone, which is exactly what we want.
        double naturalRadius = 16.0D * Math.max(1.0D, event.getNewVolume());
        if (operator.position().distanceToSqr(sourcePos)
                <= naturalRadius * naturalRadius) return;

        ResourceLocation soundId = event.getSound().value().getLocation();
        if (LOCAL_CRT_CUES.contains(soundId.getPath())) {
            // SCP-079's own physical on/off noise is intentionally absent from
            // the remote host monitor. It is still audible when physically at
            // the host or when a camera is actually close enough to hear it.
            return;
        }
        SoundSource source = event.getSource();
        if (source == SoundSource.MUSIC || source == SoundSource.RECORDS
                || source == SoundSource.MASTER) return;

        Scp079AudioNetwork.sendRemoteHostSound(operator, soundId, source,
                Math.max(0.001F, event.getNewVolume() * RELAY_VOLUME),
                event.getNewPitch());
    }
}
