package com.bl4ues.scpclassifieddirective.facility.speaker;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079ScreenState;
import com.bl4ues.scpclassifieddirective.facility.intercom.IntercomModule;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Shared source-to-endpoint routing for SCP-079 and physical Intercoms. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpeakerBroadcastManager {
    private static final Map<UUID, Broadcast> ACTIVE = new ConcurrentHashMap<>();
    private static final float SWITCH_CUE_VOLUME = 1.75F;

    private SpeakerBroadcastManager() {
    }

    public enum SourceType {
        SCP_079,
        INTERCOM
    }

    public static boolean start(ServerPlayer operator, SourceType source,
            List<FacilitySpeakerRegistry.SpeakerEndpoint> endpoints) {
        if (operator == null || operator.getServer() == null
                || endpoints == null || endpoints.isEmpty()) return false;
        if (source == SourceType.INTERCOM) return false;
        stop(operator);

        List<FacilitySpeakerRegistry.SpeakerEndpoint> valid = validEndpoints(
                operator.getServer(), endpoints);
        if (valid.isEmpty()) return false;

        Scp079ScreenState.HostRef host = Scp079ScreenState.speakerHost(operator);
        UUID broadcastId = operator.getUUID();
        Broadcast broadcast = new Broadcast(operator.getServer(), broadcastId,
                operator.getUUID(), source, List.copyOf(valid), host,
                null, null);
        ACTIVE.put(broadcastId, broadcast);
        valid.forEach(endpoint -> setEndpoint(operator.getServer(), endpoint,
                true));
        if (host != null) {
            // The broadcast must already be registered before the CRT changes
            // state so its startup cue is relayed through these same Speakers.
            Scp079ScreenState.setSpeakerActive(operator.getServer(), host, true);
        }
        return true;
    }

    public static boolean startIntercom(ServerLevel sourceLevel,
            BlockPos sourcePos, ServerPlayer activator,
            List<FacilitySpeakerRegistry.SpeakerEndpoint> endpoints) {
        if (sourceLevel == null || sourcePos == null || activator == null
                || endpoints == null || sourceLevel.getServer() == null) {
            return false;
        }
        MinecraftServer server = sourceLevel.getServer();
        UUID broadcastId = intercomId(sourceLevel.dimension(), sourcePos);
        stopById(server, broadcastId);

        List<FacilitySpeakerRegistry.SpeakerEndpoint> valid = validEndpoints(
                server, endpoints);
        if (valid.isEmpty()) return false;

        Broadcast broadcast = new Broadcast(server, broadcastId,
                activator.getUUID(), SourceType.INTERCOM, List.copyOf(valid),
                null, sourceLevel.dimension(), sourcePos.immutable());
        ACTIVE.put(broadcastId, broadcast);
        valid.forEach(endpoint -> setEndpoint(server, endpoint, true));
        return true;
    }

    /** Reconciles mapped-room Speaker membership without restarting the Intercom. */
    public static boolean refreshIntercom(ServerLevel sourceLevel,
            BlockPos sourcePos,
            List<FacilitySpeakerRegistry.SpeakerEndpoint> endpoints) {
        if (sourceLevel == null || sourcePos == null || endpoints == null) {
            return false;
        }
        MinecraftServer server = sourceLevel.getServer();
        UUID id = intercomId(sourceLevel.dimension(), sourcePos);
        Broadcast old = ACTIVE.get(id);
        if (old == null || old.server != server
                || old.source != SourceType.INTERCOM) return false;

        List<FacilitySpeakerRegistry.SpeakerEndpoint> valid = validEndpoints(
                server, endpoints);
        if (valid.isEmpty()) {
            stopById(server, id);
            return false;
        }
        if (old.endpoints.equals(valid)) return true;

        Broadcast replacement = new Broadcast(old.server, old.broadcastId,
                old.operatorId, old.source, List.copyOf(valid), old.scp079Host,
                old.sourceDimension, old.sourcePos);
        if (!ACTIVE.replace(id, old, replacement)) return false;

        for (FacilitySpeakerRegistry.SpeakerEndpoint endpoint : valid) {
            if (!old.endpoints.contains(endpoint)) setEndpoint(server, endpoint,
                    true);
        }
        for (FacilitySpeakerRegistry.SpeakerEndpoint endpoint : old.endpoints) {
            if (!valid.contains(endpoint)) {
                ServerLevel level = server.getLevel(endpoint.dimension());
                if (level != null && !isEndpointActive(level, endpoint.pos())) {
                    setEndpoint(server, endpoint, false);
                }
            }
        }
        return true;
    }

    public static boolean stopIntercom(ServerLevel sourceLevel,
            BlockPos sourcePos) {
        if (sourceLevel == null || sourcePos == null) return false;
        return stopById(sourceLevel.getServer(), intercomId(
                sourceLevel.dimension(), sourcePos));
    }

    public static boolean isIntercomActive(ServerLevel sourceLevel,
            BlockPos sourcePos) {
        if (sourceLevel == null || sourcePos == null) return false;
        Broadcast broadcast = ACTIVE.get(intercomId(sourceLevel.dimension(),
                sourcePos));
        return broadcast != null && broadcast.server == sourceLevel.getServer()
                && broadcast.source == SourceType.INTERCOM;
    }

    public static boolean stop(ServerPlayer operator) {
        return operator != null && stop(operator.getServer(), operator.getUUID());
    }

    public static boolean stop(MinecraftServer server, UUID operatorId) {
        return stopById(server, operatorId);
    }

    private static boolean stopById(MinecraftServer server, UUID broadcastId) {
        if (server == null || broadcastId == null) return false;
        Broadcast broadcast = ACTIVE.get(broadcastId);
        if (broadcast == null || broadcast.server != server
                || !ACTIVE.remove(broadcastId, broadcast)) return false;
        finishBroadcast(broadcast);
        return true;
    }

    private static void finishBroadcast(Broadcast broadcast) {
        if (broadcast.source == SourceType.SCP_079
                && broadcast.scp079Host != null) {
            Scp079ScreenState.setSpeakerActive(broadcast.server,
                    broadcast.scp079Host, false);
        }
        broadcast.endpoints.forEach(endpoint -> {
            ServerLevel level = broadcast.server.getLevel(endpoint.dimension());
            if (level != null && !isEndpointActive(level, endpoint.pos())) {
                setEndpoint(broadcast.server, endpoint, false);
            }
        });
    }

    public static boolean isBroadcasting(ServerPlayer operator) {
        if (operator == null) return false;
        Broadcast broadcast = ACTIVE.get(operator.getUUID());
        return broadcast != null && broadcast.server == operator.getServer()
                && broadcast.source == SourceType.SCP_079
                && !broadcast.endpoints.isEmpty();
    }

    public static boolean isEndpointActive(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;
        for (Broadcast broadcast : ACTIVE.values()) {
            if (broadcast.server != level.getServer()) continue;
            for (FacilitySpeakerRegistry.SpeakerEndpoint endpoint :
                    broadcast.endpoints) {
                if (endpoint.dimension().equals(level.dimension())
                        && endpoint.pos().equals(pos)) return true;
            }
        }
        return false;
    }

    /** Immutable SCP-079 snapshots safe for Simple Voice Chat's packet thread. */
    public static List<VoiceSource> voiceSources(MinecraftServer server,
            UUID operatorId) {
        Broadcast broadcast = ACTIVE.get(operatorId);
        if (broadcast == null || broadcast.server != server
                || broadcast.source != SourceType.SCP_079) return List.of();
        List<VoiceSource> result = new ArrayList<>();
        for (FacilitySpeakerRegistry.SpeakerEndpoint endpoint :
                broadcast.endpoints) {
            result.add(new VoiceSource(endpoint.dimension(), endpoint.pos(),
                    endpoint.soundPosition(), broadcast.source,
                    broadcast.broadcastId, 1.0F));
        }
        return List.copyOf(result);
    }

    /** Every active Intercom close enough to this speaker can capture their voice. */
    public static List<VoiceSource> intercomVoiceSources(ServerPlayer speaker) {
        if (speaker == null || !(speaker.level() instanceof ServerLevel level)) {
            return List.of();
        }
        List<AudioSource> sources = intercomAudioSources(level,
                speaker.position());
        List<VoiceSource> result = new ArrayList<>(sources.size());
        for (AudioSource source : sources) {
            result.add(new VoiceSource(source.dimension(), source.pos(),
                    source.position(), SourceType.INTERCOM, source.sourceId(),
                    source.captureGain()));
        }
        return List.copyOf(result);
    }

    /** Speaker endpoints reached by Intercom microphones within five blocks. */
    public static List<AudioSource> intercomAudioSources(ServerLevel level,
            Vec3 capturedPosition) {
        if (level == null || capturedPosition == null) return List.of();
        List<AudioSource> result = new ArrayList<>();
        for (Broadcast broadcast : ACTIVE.values()) {
            if (broadcast.server != level.getServer()
                    || broadcast.source != SourceType.INTERCOM
                    || broadcast.sourceDimension == null
                    || !broadcast.sourceDimension.equals(level.dimension())
                    || broadcast.sourcePos == null
                    || !level.hasChunkAt(broadcast.sourcePos)) {
                continue;
            }
            BlockState state = level.getBlockState(broadcast.sourcePos);
            if (!state.is(IntercomModule.BLOCK.get())
                    || !state.getValue(IntercomModule.ACTIVE)) continue;

            Vec3 microphone = IntercomModule.microphonePosition(
                    broadcast.sourcePos, state);
            double distance = microphone.distanceTo(capturedPosition);
            if (distance > IntercomModule.CAPTURE_RADIUS) continue;
            float gain = Mth.clamp((float) (1.0D
                    - distance / IntercomModule.CAPTURE_RADIUS), 0.0F, 1.0F);
            if (gain <= 0.001F) continue;

            for (FacilitySpeakerRegistry.SpeakerEndpoint endpoint :
                    broadcast.endpoints) {
                result.add(new AudioSource(broadcast.broadcastId,
                        endpoint.dimension(), endpoint.pos(),
                        endpoint.soundPosition(), gain));
            }
        }
        return List.copyOf(result);
    }

    public static void removeEndpoint(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        for (Map.Entry<UUID, Broadcast> entry : List.copyOf(ACTIVE.entrySet())) {
            Broadcast broadcast = entry.getValue();
            if (broadcast.server != level.getServer()) continue;
            List<FacilitySpeakerRegistry.SpeakerEndpoint> remaining =
                    broadcast.endpoints.stream().filter(endpoint ->
                            !endpoint.dimension().equals(level.dimension())
                                    || !endpoint.pos().equals(pos)).toList();
            if (remaining.size() == broadcast.endpoints.size()) continue;
            if (remaining.isEmpty()) {
                if (ACTIVE.remove(entry.getKey(), broadcast)) {
                    if (broadcast.source == SourceType.SCP_079
                            && broadcast.scp079Host != null) {
                        Scp079ScreenState.setSpeakerActive(level.getServer(),
                                broadcast.scp079Host, false);
                    }
                }
            } else {
                ACTIVE.replace(entry.getKey(), broadcast,
                        new Broadcast(broadcast.server, broadcast.broadcastId,
                                broadcast.operatorId, broadcast.source,
                                List.copyOf(remaining), broadcast.scp079Host,
                                broadcast.sourceDimension, broadcast.sourcePos));
            }
        }
    }

    private static List<FacilitySpeakerRegistry.SpeakerEndpoint> validEndpoints(
            MinecraftServer server,
            List<FacilitySpeakerRegistry.SpeakerEndpoint> endpoints) {
        return endpoints.stream()
                .filter(endpoint -> endpoint != null)
                .filter(endpoint -> {
                    ServerLevel level = server.getLevel(endpoint.dimension());
                    return level != null && level.getBlockState(endpoint.pos())
                            .is(SpeakerModule.BLOCK.get());
                }).toList();
    }

    private static UUID intercomId(ResourceKey<Level> dimension, BlockPos pos) {
        String key = ScpClassifiedDirectiveMod.MODID + ":intercom:"
                + dimension.location() + ":" + pos.asLong();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static void setEndpoint(MinecraftServer server,
            FacilitySpeakerRegistry.SpeakerEndpoint endpoint, boolean active) {
        ServerLevel level = server.getLevel(endpoint.dimension());
        if (level == null) return;
        BlockState state = level.getBlockState(endpoint.pos());
        if (!state.is(SpeakerModule.BLOCK.get())) return;
        if (state.getValue(SpeakerModule.ACTIVE) == active) return;
        level.setBlock(endpoint.pos(), state.setValue(
                SpeakerModule.ACTIVE, active), Block.UPDATE_CLIENTS);
        level.playSound(null, endpoint.pos(),
                active ? SpeakerModule.ON.get() : SpeakerModule.OFF.get(),
                SoundSource.BLOCKS, SWITCH_CUE_VOLUME, 1.0F);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.entrySet().removeIf(entry -> entry.getValue().server
                == event.getServer());
    }

    private record Broadcast(MinecraftServer server, UUID broadcastId,
            UUID operatorId, SourceType source,
            List<FacilitySpeakerRegistry.SpeakerEndpoint> endpoints,
            Scp079ScreenState.HostRef scp079Host,
            ResourceKey<Level> sourceDimension, BlockPos sourcePos) {
    }

    public record VoiceSource(ResourceKey<Level> dimension, BlockPos pos,
            Vec3 position, SourceType sourceType, UUID sourceId,
            float captureGain) {
    }

    public record AudioSource(UUID sourceId, ResourceKey<Level> dimension,
            BlockPos pos, Vec3 position, float captureGain) {
    }
}
