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
    // The source files are intentionally quiet environmental beds. These gains
    // keep them clearly audible without competing with encounter music.
    private static final float LCZ_INSIDE_VOLUME = 1.35F;
    private static final float CORE_INSIDE_VOLUME = 1.70F;
    private static final double SPILL_RADIUS = 12.0D;
    private static final int CORE_ROOM_BORDER = 1;
    private static final int RETRY_DELAY_TICKS = 20;
    private static final int INACTIVE_GRACE_TICKS = 20;

    private static FacilityZoneAmbientSound active;
    private static FacilityZoneAmbientSound activeBoost;
    private static FacilityZoneAmbientSound fading;
    private static FacilityZoneAmbientSound fadingBoost;
    private static Area activeArea = Area.NONE;
    private static int retryTicks;
    private static int inactiveTicks;

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
        if (!ClientModulePreferences.facilityAmbienceEnabled()) {
            stopImmediately(minecraft);
            return;
        }

        Selection desired = select(minecraft);
        if (desired.area() != activeArea) {
            transitionTo(minecraft, desired);
        } else if (active != null) {
            setLayerVolumes(desired.volume());
        }

        if (retryTicks > 0) retryTicks--;
        if (active != null) {
            if (minecraft.getSoundManager().isActive(active)) {
                inactiveTicks = 0;
            } else if (++inactiveTicks > INACTIVE_GRACE_TICKS) {
                // Streaming sources can report inactive briefly while their
                // asynchronous channel is being created or cycled. Recreate the
                // loop only after a sustained loss instead of killing it on the
                // very tick it was started.
                active = null;
                inactiveTicks = 0;
                if (activeArea.hasAudio()) retryTicks = RETRY_DELAY_TICKS;
            }
        } else {
            inactiveTicks = 0;
        }
        if (fading != null
                && !minecraft.getSoundManager().isActive(fading)) {
            fading = null;
        }
        if (fadingBoost != null
                && !minecraft.getSoundManager().isActive(fadingBoost)) {
            fadingBoost = null;
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

        // Ambience considers every authored room occupying this column. A
        // smaller/unassigned overlapping room must not silence the mapped floor
        // beneath it; Core Room remains the highest-priority layer.
        if (insideMappedRoom) {
            return insideArea.hasAudio()
                    ? new Selection(insideArea, insideVolume(insideArea))
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
                insideVolume(nearestArea) * smooth));
    }

    private static float insideVolume(Area area) {
        return area == Area.CORE_ROOM
                ? CORE_INSIDE_VOLUME : LCZ_INSIDE_VOLUME;
    }

    private static Area areaFor(FacilityRoomSnapshot room) {
        if (room == null) return Area.NONE;
        // Every mapped room can be assigned to a Core Room Floor Station so it
        // inherits that floor's labels. That association does NOT make every
        // room a Core Room. Only the room that physically contains/touches its
        // assigned station gets the Core Room bed.
        if (isCoreRoom(room)) {
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
        if (active != null || activeBoost != null) {
            if (fading != null) minecraft.getSoundManager().stop(fading);
            if (fadingBoost != null) {
                minecraft.getSoundManager().stop(fadingBoost);
            }
            if (active != null) {
                active.beginFadeOut();
                fading = active;
            }
            if (activeBoost != null) {
                activeBoost.beginFadeOut();
                fadingBoost = activeBoost;
            }
            active = null;
            activeBoost = null;
        }
        activeArea = desired.area();
        retryTicks = 0;
        inactiveTicks = 0;
        if (activeArea.hasAudio()) {
            start(minecraft, activeArea, desired.volume());
        }
    }

    private static void start(Minecraft minecraft, Area area, float volume) {
        SoundEvent event = soundFor(area);
        if (event == null) return;
        if (activeBoost != null) {
            minecraft.getSoundManager().stop(activeBoost);
            activeBoost = null;
        }

        float primaryVolume = Math.min(1.0F,
                Math.max(FacilityZoneAmbientSound.MIN_VOLUME, volume));
        float boostVolume = Math.max(0.0F, volume - 1.0F);
        active = new FacilityZoneAmbientSound(event, primaryVolume);
        inactiveTicks = 0;
        minecraft.getSoundManager().play(active);
        if (boostVolume > FacilityZoneAmbientSound.MIN_VOLUME) {
            activeBoost = new FacilityZoneAmbientSound(event, boostVolume);
            minecraft.getSoundManager().play(activeBoost);
        }
    }

    private static void setLayerVolumes(float volume) {
        if (active != null) {
            active.setTargetVolume(Math.min(1.0F,
                    Math.max(FacilityZoneAmbientSound.MIN_VOLUME, volume)));
        }
        if (activeBoost != null) {
            activeBoost.setTargetVolume(Math.max(
                    FacilityZoneAmbientSound.MIN_VOLUME, volume - 1.0F));
        }
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
        if (activeBoost != null) minecraft.getSoundManager().stop(activeBoost);
        if (fading != null) minecraft.getSoundManager().stop(fading);
        if (fadingBoost != null) minecraft.getSoundManager().stop(fadingBoost);
        active = null;
        activeBoost = null;
        fading = null;
        fadingBoost = null;
        activeArea = Area.NONE;
        retryTicks = 0;
        inactiveTicks = 0;
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
