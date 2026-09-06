package com.bl4ues.scpclassifieddirective.facility.speaker;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessSavedData;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079ScreenState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared source-to-endpoint routing. SCP-079 owns the first source type; the
 * future Intercom can call the same start/stop API without changing speakers.
 */
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
        stop(operator);

        List<FacilitySpeakerRegistry.SpeakerEndpoint> valid = endpoints.stream()
                .filter(endpoint -> endpoint != null)
                .filter(endpoint -> {
                    ServerLevel level = operator.getServer().getLevel(
                            endpoint.dimension());
                    return level != null && level.getBlockState(endpoint.pos())
                            .is(SpeakerModule.BLOCK.get());
                }).toList();
        if (valid.isEmpty()) return false;

        Scp079FacilityAccessSavedData.TrackedPosition host = source == SourceType.SCP_079
                ? resolveScp079Host(operator) : null;
        Broadcast broadcast = new Broadcast(operator.getServer(),
                operator.getUUID(), source, List.copyOf(valid), host);
        ACTIVE.put(operator.getUUID(), broadcast);
        valid.forEach(endpoint -> setEndpoint(operator.getServer(), endpoint,
                true));
        if (source == SourceType.SCP_079 && host != null) {
            // The broadcast must already be registered before the CRT changes
            // state so its startup cue is relayed through these same Speakers.
            Scp079ScreenState.setSpeakerActive(operator.getServer(), host, true);
        }
        return true;
    }

    public static boolean stop(ServerPlayer operator) {
        return operator != null && stop(operator.getServer(), operator.getUUID());
    }

    public static boolean stop(MinecraftServer server, UUID operatorId) {
        if (server == null || operatorId == null) return false;
        Broadcast broadcast = ACTIVE.get(operatorId);
        if (broadcast == null || broadcast.server != server
                || !ACTIVE.remove(operatorId, broadcast)) return false;
        if (broadcast.source == SourceType.SCP_079 && broadcast.scp079Host != null) {
            Scp079ScreenState.setSpeakerActive(server, broadcast.scp079Host, false);
        }
        broadcast.endpoints.forEach(endpoint -> {
            ServerLevel level = server.getLevel(endpoint.dimension());
            if (level != null && !isEndpointActive(level, endpoint.pos())) {
                setEndpoint(server, endpoint, false);
            }
        });
        return true;
    }

    public static boolean isBroadcasting(ServerPlayer operator) {
        if (operator == null) return false;
        Broadcast broadcast = ACTIVE.get(operator.getUUID());
        return broadcast != null && broadcast.server == operator.getServer()
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

    /** Immutable snapshots safe for Simple Voice Chat's packet thread. */
    public static List<VoiceSource> voiceSources(MinecraftServer server,
            UUID operatorId) {
        Broadcast broadcast = ACTIVE.get(operatorId);
        if (broadcast == null || broadcast.server != server) return List.of();
        List<VoiceSource> result = new ArrayList<>();
        for (FacilitySpeakerRegistry.SpeakerEndpoint endpoint :
                broadcast.endpoints) {
            result.add(new VoiceSource(endpoint.dimension(), endpoint.pos(),
                    endpoint.soundPosition(), broadcast.source));
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
                if (ACTIVE.remove(entry.getKey(), broadcast)
                        && broadcast.source == SourceType.SCP_079
                        && broadcast.scp079Host != null) {
                    Scp079ScreenState.setSpeakerActive(level.getServer(),
                            broadcast.scp079Host, false);
                }
            } else {
                ACTIVE.replace(entry.getKey(), broadcast,
                        new Broadcast(broadcast.server, broadcast.operatorId,
                                broadcast.source, List.copyOf(remaining),
                                broadcast.scp079Host));
            }
        }
    }

    private static Scp079FacilityAccessSavedData.TrackedPosition resolveScp079Host(
            ServerPlayer operator) {
        if (operator == null || operator.getServer() == null) return null;
        BlockPos hostPos = Scp079PlayableManager.hostPosition(operator);
        if (hostPos == null) return null;
        long packed = hostPos.asLong();
        for (Scp079FacilityAccessSavedData.TrackedPosition host
                : Scp079FacilityAccessSavedData.get(operator.getServer()).hosts()) {
            if (host.packedPos() == packed) return host;
        }
        return null;
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

    private record Broadcast(MinecraftServer server, UUID operatorId,
            SourceType source,
            List<FacilitySpeakerRegistry.SpeakerEndpoint> endpoints,
            Scp079FacilityAccessSavedData.TrackedPosition scp079Host) {
    }

    public record VoiceSource(net.minecraft.resources.ResourceKey<
            net.minecraft.world.level.Level> dimension, BlockPos pos,
            net.minecraft.world.phys.Vec3 position, SourceType sourceType) {
    }
}
