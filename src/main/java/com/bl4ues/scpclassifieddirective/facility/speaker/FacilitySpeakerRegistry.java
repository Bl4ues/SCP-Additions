package com.bl4ues.scpclassifieddirective.facility.speaker;

import com.bl4ues.scpclassifieddirective.facility.Scp079RoomInteractionPolicy;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** Loaded physical speaker endpoints, resolved through authored room floors. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FacilitySpeakerRegistry {
    private static final Map<MinecraftServer, Set<SpeakerRef>> SPEAKERS =
            new WeakHashMap<>();

    private FacilitySpeakerRegistry() {
    }

    public static void register(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        synchronized (SPEAKERS) {
            SPEAKERS.computeIfAbsent(level.getServer(), ignored ->
                    ConcurrentHashMap.newKeySet()).add(new SpeakerRef(
                    level.dimension(), pos.immutable()));
        }
    }

    public static void unregister(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        synchronized (SPEAKERS) {
            Set<SpeakerRef> refs = SPEAKERS.get(level.getServer());
            if (refs != null) refs.remove(new SpeakerRef(level.dimension(), pos));
        }
    }

    public static List<SpeakerEndpoint> speakersForCamera(ServerLevel level,
            BlockPos cameraPos) {
        if (level == null || cameraPos == null) return List.of();
        FacilityRoomSnapshot room = FacilityMappingManager.roomSnapshots(level)
                .stream()
                .filter(candidate -> Scp079RoomInteractionPolicy
                        .withinExpandedFloor(candidate, cameraPos, 1))
                .findFirst().orElse(null);
        if (room == null) return List.of();
        return speakersForRoom(level, room);
    }

    /** Room-ID lookup reserved for selectors such as the future Intercom. */
    public static List<SpeakerEndpoint> speakersForRoom(ServerLevel level,
            UUID roomId) {
        if (level == null || roomId == null) return List.of();
        FacilityRoomSnapshot room = FacilityMappingManager.roomSnapshots(level)
                .stream().filter(candidate -> candidate.id().equals(roomId))
                .findFirst().orElse(null);
        return room == null ? List.of() : speakersForRoom(level, room);
    }

    private static List<SpeakerEndpoint> speakersForRoom(ServerLevel level,
            FacilityRoomSnapshot room) {

        Set<SpeakerRef> refs;
        synchronized (SPEAKERS) {
            refs = SPEAKERS.get(level.getServer());
            if (refs == null || refs.isEmpty()) return List.of();
            refs = Set.copyOf(refs);
        }

        return refs.stream()
                .filter(ref -> ref.dimension.equals(level.dimension()))
                .filter(ref -> level.hasChunkAt(ref.pos))
                .filter(ref -> level.getBlockState(ref.pos)
                        .is(SpeakerModule.BLOCK.get()))
                .filter(ref -> Scp079RoomInteractionPolicy.withinExpandedFloor(
                        room, ref.pos, 1))
                .map(ref -> new SpeakerEndpoint(ref.dimension, ref.pos,
                        Vec3.atCenterOf(ref.pos)))
                .sorted(Comparator.comparingLong(endpoint ->
                        endpoint.pos().asLong()))
                .toList();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        synchronized (SPEAKERS) {
            SPEAKERS.remove(event.getServer());
        }
    }

    private record SpeakerRef(ResourceKey<Level> dimension, BlockPos pos) {
    }

    public record SpeakerEndpoint(ResourceKey<Level> dimension, BlockPos pos,
            Vec3 soundPosition) {
        public SpeakerEndpoint {
            pos = pos.immutable();
        }
    }
}
