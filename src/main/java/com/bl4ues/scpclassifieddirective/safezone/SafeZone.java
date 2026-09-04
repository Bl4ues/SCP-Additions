package com.bl4ues.scpclassifieddirective.safezone;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Immutable, dimension-bound safe-zone definition persisted with the world. */
public record SafeZone(UUID id, ResourceLocation dimension, BlockPos min,
        BlockPos max, boolean musicEnabled, String musicTrack,
        String automaticTrack) {
    public SafeZone {
        if (id == null) id = UUID.randomUUID();
        if (dimension == null) dimension = Level.OVERWORLD.location();
        BlockPos first = min == null ? BlockPos.ZERO : min.immutable();
        BlockPos second = max == null ? first : max.immutable();
        min = new BlockPos(Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ()));
        max = new BlockPos(Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ()));
        musicTrack = SafeZoneTrack.validManualId(musicTrack)
                ? musicTrack : "";
        automaticTrack = SafeZoneTrack.isAutomaticId(automaticTrack)
                ? automaticTrack : "";
    }

    public boolean isIn(ResourceKey<Level> level) {
        return level != null && dimension.equals(level.location());
    }

    public boolean contains(ResourceKey<Level> level, BlockPos pos) {
        return isIn(level) && pos != null
                && pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public boolean contains(ResourceKey<Level> level, Vec3 position) {
        return isIn(level) && position != null && bounds().contains(position);
    }

    public boolean intersects(ResourceKey<Level> level, AABB box) {
        return isIn(level) && box != null && bounds().intersects(box);
    }

    public AABB bounds() {
        return new AABB(min.getX(), min.getY(), min.getZ(),
                max.getX() + 1.0D, max.getY() + 1.0D,
                max.getZ() + 1.0D);
    }

    public long volume() {
        long width = (long) max.getX() - min.getX() + 1L;
        long height = (long) max.getY() - min.getY() + 1L;
        long depth = (long) max.getZ() - min.getZ() + 1L;
        return width * height * depth;
    }

    public String effectiveTrack() {
        return automaticTrack.isEmpty() ? musicTrack : automaticTrack;
    }

    public boolean hasAutomaticTrack() {
        return !automaticTrack.isEmpty();
    }

    public SafeZone withMusic(boolean enabled, String track) {
        return new SafeZone(id, dimension, min, max, enabled,
                SafeZoneTrack.validManualId(track) ? track : "",
                automaticTrack);
    }

    public SafeZone withAutomaticTrack(String track, boolean enableNewTrack) {
        String sanitized = SafeZoneTrack.isAutomaticId(track) ? track : "";
        boolean enabled = musicEnabled
                || enableNewTrack && !sanitized.isEmpty();
        return new SafeZone(id, dimension, min, max, enabled,
                musicTrack, sanitized);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Dimension", dimension.toString());
        tag.putLong("Min", min.asLong());
        tag.putLong("Max", max.asLong());
        tag.putBoolean("MusicEnabled", musicEnabled);
        tag.putString("MusicTrack", musicTrack);
        if (!automaticTrack.isEmpty()) {
            tag.putString("AutomaticTrack", automaticTrack);
        }
        return tag;
    }

    public static SafeZone load(CompoundTag tag) {
        if (tag == null || !tag.hasUUID("Id")) return null;
        ResourceLocation dimension = ResourceLocation.tryParse(
                tag.getString("Dimension"));
        if (dimension == null) return null;
        return new SafeZone(tag.getUUID("Id"), dimension,
                BlockPos.of(tag.getLong("Min")),
                BlockPos.of(tag.getLong("Max")),
                tag.getBoolean("MusicEnabled"),
                tag.getString("MusicTrack"),
                tag.getString("AutomaticTrack"));
    }
}
