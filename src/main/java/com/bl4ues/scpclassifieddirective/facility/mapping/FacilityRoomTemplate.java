package com.bl4ues.scpclassifieddirective.facility.mapping;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Portable logical room footprint for future procedural placement. Precise
 * polygon outlines are stored relative to an authored origin and can be moved
 * and yaw-rotated together with their transformed construction payload.
 */
public final class FacilityRoomTemplate {
    private final List<PatchTemplate> patches;
    private final String name;
    private final Vec3 relativeStation;

    public FacilityRoomTemplate(List<PatchTemplate> patches, String name,
            Vec3 relativeStation) {
        this.patches = patches == null ? List.of() : List.copyOf(patches);
        this.name = name == null ? "" : name;
        this.relativeStation = relativeStation;
    }

    public static FacilityRoomTemplate capture(FacilityRoom room, Vec3 origin) {
        if (room == null || origin == null) {
            return new FacilityRoomTemplate(List.of(), "", null);
        }
        List<PatchTemplate> patches = new ArrayList<>();
        for (FacilityFloorPatch patch : room.patches()) {
            List<FacilityFloorPatch.Vertex> relative = new ArrayList<>();
            for (FacilityFloorPatch.Vertex vertex : patch.outline()) {
                relative.add(new FacilityFloorPatch.Vertex(
                        vertex.x() - origin.x, vertex.z() - origin.z));
            }
            patches.add(new PatchTemplate(patch.y() - origin.y, relative));
        }
        Vec3 station = room.floorStation() == null ? null
                : Vec3.atCenterOf(room.floorStation()).subtract(origin);
        return new FacilityRoomTemplate(patches, room.name(), station);
    }

    public FacilityRoom instantiate(ResourceLocation dimension, Vec3 targetOrigin,
            float yawDegrees) {
        Vec3 target = targetOrigin == null ? Vec3.ZERO : targetOrigin;
        List<FacilityFloorPatch> patches = new ArrayList<>();
        for (PatchTemplate patch : this.patches) {
            List<FacilityFloorPatch.Vertex> vertices = new ArrayList<>();
            for (FacilityFloorPatch.Vertex vertex : patch.vertices()) {
                Vec3 rotated = rotateY(new Vec3(vertex.x(), 0.0D, vertex.z()),
                        yawDegrees);
                vertices.add(new FacilityFloorPatch.Vertex(target.x + rotated.x,
                        target.z + rotated.z));
            }
            FacilityFloorPatch mapped = FacilityFloorPatch.polygon(
                    (int) Math.round(target.y + patch.relativeY()), vertices);
            if (mapped != null) patches.add(mapped);
        }
        BlockPos station = null;
        if (relativeStation != null) {
            Vec3 position = target.add(rotateY(relativeStation, yawDegrees));
            station = BlockPos.containing(position);
        }
        return new FacilityRoom(UUID.randomUUID(), dimension, patches, name,
                station);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        if (relativeStation != null) putVec(tag, "Station", relativeStation);
        ListTag list = new ListTag();
        for (PatchTemplate patch : patches) list.add(patch.save());
        tag.put("Patches", list);
        return tag;
    }

    public static FacilityRoomTemplate load(CompoundTag tag) {
        if (tag == null) return new FacilityRoomTemplate(List.of(), "", null);
        List<PatchTemplate> patches = new ArrayList<>();
        ListTag list = tag.getList("Patches", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            PatchTemplate patch = PatchTemplate.load(list.getCompound(i));
            if (patch != null) patches.add(patch);
        }
        Vec3 station = tag.contains("Station", Tag.TAG_COMPOUND)
                ? getVec(tag, "Station") : null;
        return new FacilityRoomTemplate(patches, tag.getString("Name"), station);
    }

    private static Vec3 rotateY(Vec3 value, float yaw) {
        double radians = Math.toRadians(yaw);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        return new Vec3(value.x * cos - value.z * sin, value.y,
                value.x * sin + value.z * cos);
    }

    private static void putVec(CompoundTag tag, String key, Vec3 value) {
        CompoundTag vector = new CompoundTag();
        vector.putDouble("X", value.x);
        vector.putDouble("Y", value.y);
        vector.putDouble("Z", value.z);
        tag.put(key, vector);
    }

    private static Vec3 getVec(CompoundTag tag, String key) {
        CompoundTag vector = tag.getCompound(key);
        return new Vec3(vector.getDouble("X"), vector.getDouble("Y"),
                vector.getDouble("Z"));
    }

    public record PatchTemplate(double relativeY,
            List<FacilityFloorPatch.Vertex> vertices) {
        public PatchTemplate {
            vertices = vertices == null ? List.of() : List.copyOf(vertices);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("Y", relativeY);
            ListTag list = new ListTag();
            for (FacilityFloorPatch.Vertex vertex : vertices) {
                CompoundTag point = new CompoundTag();
                point.putDouble("X", vertex.x());
                point.putDouble("Z", vertex.z());
                list.add(point);
            }
            tag.put("Vertices", list);
            return tag;
        }

        private static PatchTemplate load(CompoundTag tag) {
            if (tag == null) return null;
            List<FacilityFloorPatch.Vertex> vertices = new ArrayList<>();
            ListTag list = tag.getList("Vertices", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag point = list.getCompound(i);
                vertices.add(new FacilityFloorPatch.Vertex(point.getDouble("X"),
                        point.getDouble("Z")));
            }
            return vertices.size() < 3 ? null
                    : new PatchTemplate(tag.getDouble("Y"), vertices);
        }
    }
}
