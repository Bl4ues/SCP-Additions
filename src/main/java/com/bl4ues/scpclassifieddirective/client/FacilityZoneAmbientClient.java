package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoom;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import com.bl4ues.scpclassifieddirective.sound.FacilityAmbientSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Locale;

/**
 * Selects a quiet environmental layer from Facility Mapping. This is ambience,
 * not music: SCP tracks and Safe Zone music deliberately do not suspend it.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FacilityZoneAmbientClient {
    private static final float INSIDE_VOLUME = 0.22F;
    private static final double SPILL_RADIUS = 12.0D;
    private static final int RETRY_DELAY_TICKS = 100;

    private static FacilityZoneAmbientSound active;
    private static FacilityZoneAmbientSound fading;
    private static Area activeArea = Area.NONE;
    private static int retryTicks;

    private FacilityZoneAmbientClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !minecraft.player.isAlive()) {
            stopImmediately(minecraft);
            return;
        }

        Selection desired = select(minecraft);
        if (desired.area() != activeArea) {
            transitionTo(minecraft, desired);
        } else if (active != null) {
            active.setTargetVolume(desired.volume());
        }

        if (retryTicks > 0) retryTicks--;
        if (active != null
                && !minecraft.getSoundManager().isActive(active)) {
            active = null;
            if (activeArea.hasAudio()) retryTicks = RETRY_DELAY_TICKS;
        }
        if (fading != null
                && !minecraft.getSoundManager().isActive(fading)) {
            fading = null;
        }
        if (active == null && activeArea.hasAudio()
                && retryTicks <= 0) {
            start(minecraft, activeArea, desired.volume());
        }
    }

    private static Selection select(Minecraft minecraft) {
        ResourceLocation dimension = minecraft.level.dimension().location();
        List<FacilityRoomSnapshot> rooms =
                FacilityMappingClientState.rooms(dimension);
        if (rooms.isEmpty()) return Selection.NONE;

        BlockPos blockPos = minecraft.player.blockPosition();
        Area insideArea = Area.NONE;
        boolean insideMappedRoom = false;
        for (FacilityRoomSnapshot room : rooms) {
            if (!room.containsColumn(blockPos)) continue;
            insideMappedRoom = true;
            Area candidate = areaFor(room);
            if (candidate.priority() > insideArea.priority()) {
                insideArea = candidate;
            }
        }

        // Entering any mapped room ends spill from a different room. Regions
        // whose ambience files do not exist yet therefore remain quiet.
        if (insideMappedRoom) {
            return insideArea.hasAudio()
                    ? new Selection(insideArea, INSIDE_VOLUME)
                    : Selection.NONE;
        }

        Vec3 position = minecraft.player.position();
        double nearestDistance = Double.POSITIVE_INFINITY;
        Area nearestArea = Area.NONE;
        for (FacilityRoomSnapshot room : rooms) {
            Area candidate = areaFor(room);
            if (!candidate.hasAudio()) continue;
            double distance = distanceToRoom(position, room);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestArea = candidate;
            }
        }

        if (!nearestArea.hasAudio() || nearestDistance > SPILL_RADIUS) {
            return Selection.NONE;
        }
        float proximity = (float) Mth.clamp(
                1.0D - nearestDistance / SPILL_RADIUS, 0.0D, 1.0D);
        float smooth = proximity * proximity * (3.0F - 2.0F * proximity);
        return new Selection(nearestArea, Math.max(
                FacilityZoneAmbientSound.MIN_VOLUME,
                INSIDE_VOLUME * smooth));
    }

    private static Area areaFor(FacilityRoomSnapshot room) {
        if (room == null) return Area.NONE;
        BlockPos station = room.floorStation();
        if (station != null && room.containsColumn(station)) {
            return Area.CORE_ROOM;
        }

        String labels = ((room.floorLongLabel() == null ? ""
                : room.floorLongLabel()) + " "
                + (room.floorShortLabel() == null ? ""
                : room.floorShortLabel()))
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();

        if (labels.contains("super heavy containment zone")
                || token(labels, "shcz")) return Area.SHCZ;
        if (labels.contains("heavy containment zone")
                || token(labels, "hcz")) return Area.HCZ;
        if (labels.contains("entrance zone")
                || token(labels, "ez")) return Area.ENTRANCE;
        if (labels.contains("light containment zone")
                || token(labels, "lcz")) {
            if (matchesSublevel(labels, 1)) return Area.LCZ_SL1;
            if (matchesSublevel(labels, 2)) return Area.LCZ_SL2;
            if (matchesSublevel(labels, 3)) return Area.LCZ_SL3;
        }
        return Area.NONE;
    }

    private static double distanceToRoom(Vec3 position,
            FacilityRoomSnapshot room) {
        double bestSqr = Double.POSITIVE_INFINITY;
        for (FacilityFloorPatch patch : room.patches()) {
            double dx = axisDistance(position.x, patch.minX(),
                    patch.maxX() + 1.0D);
            double dy = axisDistance(position.y, patch.y(),
                    patch.y() + FacilityRoom.CAMERA_COLUMN_HEIGHT + 1.0D);
            double dz = axisDistance(position.z, patch.minZ(),
                    patch.maxZ() + 1.0D);
            bestSqr = Math.min(bestSqr, dx * dx + dy * dy + dz * dz);
        }
        return Math.sqrt(bestSqr);
    }

    private static double axisDistance(double value, double min, double max) {
        if (value < min) return min - value;
        if (value > max) return value - max;
        return 0.0D;
    }

    private static boolean matchesSublevel(String text, int number) {
        String n = Integer.toString(number);
        return text.contains("sublevel " + n)
                || text.contains("sublevel-" + n)
                || text.contains("sublevel_" + n)
                || text.contains("sl " + n)
                || text.contains("sl-" + n)
                || text.contains("sl_" + n)
                || text.contains("sl" + n);
    }

    private static boolean token(String value, String token) {
        int index = -1;
        while ((index = value.indexOf(token, index + 1)) >= 0) {
            boolean left = index == 0
                    || !Character.isLetterOrDigit(value.charAt(index - 1));
            int end = index + token.length();
            boolean right = end >= value.length()
                    || !Character.isLetterOrDigit(value.charAt(end));
            if (left && right) return true;
        }
        return false;
    }

    private static void transitionTo(Minecraft minecraft, Selection desired) {
        if (active != null) {
            if (fading != null) minecraft.getSoundManager().stop(fading);
            active.beginFadeOut();
            fading = active;
            active = null;
        }
        activeArea = desired.area();
        retryTicks = 0;
        if (activeArea.hasAudio()) {
            start(minecraft, activeArea, desired.volume());
        }
    }

    private static void start(Minecraft minecraft, Area area, float volume) {
        SoundEvent event = soundFor(area);
        if (event == null) return;
        active = new FacilityZoneAmbientSound(event, volume);
        minecraft.getSoundManager().play(active);
    }

    private static SoundEvent soundFor(Area area) {
        return switch (area) {
            case CORE_ROOM -> FacilityAmbientSounds.CORE_ROOM.get();
            case LCZ_SL1 -> FacilityAmbientSounds.LCZ_SL1.get();
            case LCZ_SL2 -> FacilityAmbientSounds.LCZ_SL2.get();
            case LCZ_SL3 -> FacilityAmbientSounds.LCZ_SL3.get();
            case ENTRANCE, HCZ, SHCZ, NONE -> null;
        };
    }

    private static void stopImmediately(Minecraft minecraft) {
        if (active != null) minecraft.getSoundManager().stop(active);
        if (fading != null) minecraft.getSoundManager().stop(fading);
        active = null;
        fading = null;
        activeArea = Area.NONE;
        retryTicks = 0;
    }

    private enum Area {
        NONE(false, 0),
        LCZ_SL1(true, 20),
        LCZ_SL2(true, 20),
        LCZ_SL3(true, 20),
        ENTRANCE(false, 20),
        HCZ(false, 20),
        SHCZ(false, 20),
        CORE_ROOM(true, 100);

        private final boolean audio;
        private final int priority;

        Area(boolean audio, int priority) {
            this.audio = audio;
            this.priority = priority;
        }

        boolean hasAudio() {
            return audio;
        }

        int priority() {
            return priority;
        }
    }

    private record Selection(Area area, float volume) {
        private static final Selection NONE =
                new Selection(Area.NONE, FacilityZoneAmbientSound.MIN_VOLUME);
    }
}
