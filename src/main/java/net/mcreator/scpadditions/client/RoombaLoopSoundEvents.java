package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.RoombaEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Maintains one low-volume crossfaded motor track per nearby Roomba. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoombaLoopSoundEvents {
    private static final double RANGE = 48.0D;
    private static final int SCAN_INTERVAL_TICKS = 8;
    private static final Map<Integer, Track> TRACKS = new HashMap<>();

    private RoombaLoopSoundEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopEverything();
            return;
        }

        advanceTracks(minecraft);
        if (minecraft.player.tickCount % SCAN_INTERVAL_TICKS == 0) {
            scanNearbyRoombas(minecraft);
        }
    }

    private static void advanceTracks(Minecraft minecraft) {
        for (Track track : new ArrayList<>(TRACKS.values())) {
            track.active.removeIf(RoombaLoopSound::isStopped);
            if (!track.entity.isAlive() || track.entity.isRemoved()) {
                track.stop();
                TRACKS.remove(track.entity.getId());
                continue;
            }

            if (track.current == null || track.current.isStopped()) {
                track.startSegment(minecraft);
            } else if (track.current.needsSuccessor()) {
                track.current.markSuccessorQueued();
                track.startSegment(minecraft);
            }
        }
    }

    private static void scanNearbyRoombas(Minecraft minecraft) {
        Set<Integer> seen = new HashSet<>();
        AABB area = minecraft.player.getBoundingBox().inflate(RANGE);
        for (RoombaEntity roomba : minecraft.level.getEntitiesOfClass(
                RoombaEntity.class, area,
                entity -> entity.isAlive() && !entity.isRemoved())) {
            int id = roomba.getId();
            seen.add(id);
            Track track = TRACKS.computeIfAbsent(id,
                    ignored -> new Track(roomba));
            if (track.current == null || track.current.isStopped()) {
                track.startSegment(minecraft);
            }
        }

        for (Integer id : new ArrayList<>(TRACKS.keySet())) {
            if (!seen.contains(id)) {
                Track removed = TRACKS.remove(id);
                if (removed != null) {
                    removed.stop();
                }
            }
        }
    }

    private static void stopEverything() {
        TRACKS.values().forEach(Track::stop);
        TRACKS.clear();
    }

    private static final class Track {
        private final RoombaEntity entity;
        private final List<RoombaLoopSound> active = new ArrayList<>();
        private RoombaLoopSound current;

        private Track(RoombaEntity entity) {
            this.entity = entity;
        }

        private void startSegment(Minecraft minecraft) {
            RoombaLoopSound segment = new RoombaLoopSound(entity);
            active.add(segment);
            current = segment;
            minecraft.getSoundManager().play(segment);
        }

        private void stop() {
            for (RoombaLoopSound sound : active) {
                if (!sound.isStopped()) {
                    sound.finish();
                }
            }
            active.clear();
            current = null;
        }
    }
}
