package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoom;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import com.bl4ues.scpclassifieddirective.sound.FacilityAmbientSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Intermittent mapped-facility noises heard from a temporary virtual point in
 * the surrounding space. No world block or entity is created for ambience.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FacilityDistantNoiseClient {
    /*
     * Every mapped room receives one deterministic cue per window. Randomizing
     * the target tick inside the window keeps the cadence organic while avoiding
     * long silent streaks caused by repeatedly failing a second probability roll.
     * Neighboring clients resolve the same window, sound and direction.
     */
    private static final long WINDOW_TICKS = 720L;
    private static final long MIN_OFFSET_TICKS = 180L;
    private static final long OFFSET_RANGE_TICKS = 361L;
    private static final long LATE_GRACE_TICKS = 3L;

    private static final double MIN_DISTANCE = 6.5D;
    private static final double DISTANCE_RANGE = 4.5D;
    private static final double MAX_VERTICAL_OFFSET = 1.75D;
    private static final int CORE_ROOM_BORDER = 1;
    private static final float PLAYBACK_VOLUME = 3.00F;
    private static final float BOOST_VOLUME = 1.50F;

    private static final long OFFSET_SALT = 0x5343484544554C45L;
    private static final long SOUND_SALT = 0x534F554E445F4944L;
    private static final long ANGLE_SALT = 0x414E474C455F3031L;
    private static final long RANGE_SALT = 0x52414E47455F3031L;
    private static final long HEIGHT_SALT = 0x4845494748543031L;

    private static ResourceLocation activeDimension;
    private static UUID activeRoomId;
    private static long consumedWindow = Long.MIN_VALUE;

    private FacilityDistantNoiseClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !minecraft.player.isAlive()
                || !ClientModulePreferences.facilityAmbienceEnabled()) {
            clearTracking();
            return;
        }

        ResourceLocation dimension = minecraft.level.dimension().location();
        FacilityRoomSnapshot room = FacilityMappingClientState.roomAt(
                dimension, minecraft.player.blockPosition());
        if (room == null) {
            trackRoom(dimension, null);
            return;
        }

        trackRoom(dimension, room.id());

        long gameTime = minecraft.level.getGameTime();
        long window = Math.floorDiv(gameTime, WINDOW_TICKS);
        if (consumedWindow == window) return;

        long seed = seed(room.id(), window);
        long offset = MIN_OFFSET_TICKS
                + (long) Math.floor(unit(mix64(seed ^ OFFSET_SALT))
                * OFFSET_RANGE_TICKS);
        long targetTick = window * WINDOW_TICKS + offset;
        if (gameTime < targetTick) return;

        consumedWindow = window;
        if (gameTime - targetTick > LATE_GRACE_TICKS) return;

        boolean coreRoom = isCoreRoom(room);
        int count = FacilityAmbientSounds.distantNoiseCount(coreRoom);
        if (count <= 0) return;

        int soundIndex = (int) Math.floorMod(
                mix64(seed ^ SOUND_SALT), (long) count);
        SoundEvent sound = FacilityAmbientSounds.distantNoise(
                coreRoom, soundIndex);
        if (sound == null) return;

        // Capture a fixed virtual source when the cue starts. It does not move
        // with the camera/head, which keeps the authored distant sound spatial.
        double angle = unit(mix64(seed ^ ANGLE_SALT)) * Math.PI * 2.0D;
        double radius = MIN_DISTANCE
                + unit(mix64(seed ^ RANGE_SALT)) * DISTANCE_RANGE;
        double vertical = (unit(mix64(seed ^ HEIGHT_SALT)) * 2.0D - 1.0D)
                * MAX_VERTICAL_OFFSET;
        Vec3 listener = minecraft.player.position();
        Vec3 source = listener.add(Math.cos(angle) * radius, vertical,
                Math.sin(angle) * radius);

        minecraft.level.playLocalSound(source.x, source.y, source.z, sound,
                SoundSource.AMBIENT, PLAYBACK_VOLUME, 1.0F, false);
        minecraft.level.playLocalSound(source.x, source.y, source.z, sound,
                SoundSource.AMBIENT, BOOST_VOLUME, 1.0F, false);
    }

    private static boolean isCoreRoom(FacilityRoomSnapshot room) {
        if (room == null || room.floorStation() == null) return false;
        BlockPos station = room.floorStation();
        for (FacilityFloorPatch patch : room.patches()) {
            if (station.getX() < patch.minX() - CORE_ROOM_BORDER
                    || station.getX() > patch.maxX() + CORE_ROOM_BORDER
                    || station.getZ() < patch.minZ() - CORE_ROOM_BORDER
                    || station.getZ() > patch.maxZ() + CORE_ROOM_BORDER) {
                continue;
            }
            if (station.getY() >= patch.y() - 1
                    && station.getY() <= patch.y()
                    + FacilityRoom.CAMERA_COLUMN_HEIGHT) {
                return true;
            }
        }
        return room.name() != null
                && room.name().toLowerCase(Locale.ROOT).contains("core room");
    }

    private static void trackRoom(ResourceLocation dimension, UUID roomId) {
        if (!Objects.equals(activeDimension, dimension)
                || !Objects.equals(activeRoomId, roomId)) {
            activeDimension = dimension;
            activeRoomId = roomId;
            consumedWindow = Long.MIN_VALUE;
        }
    }

    private static void clearTracking() {
        activeDimension = null;
        activeRoomId = null;
        consumedWindow = Long.MIN_VALUE;
    }

    private static long seed(UUID roomId, long window) {
        long roomSeed = roomId.getMostSignificantBits()
                ^ Long.rotateLeft(roomId.getLeastSignificantBits(), 23);
        return mix64(roomSeed ^ window * 0x9E3779B97F4A7C15L);
    }

    private static long mix64(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return value;
    }

    private static double unit(long value) {
        return (value >>> 11) * 0x1.0p-53;
    }
}
