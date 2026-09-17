package com.bl4ues.scpclassifieddirective.facility.mapping;

import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionTemplate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * One portable authored-room payload for the future procedural facility.
 * Logical Facility Mapping geometry and physical transformed construction share
 * the same origin/yaw transform, preventing the map footprint from drifting
 * away from off-grid walls, curved surfaces, doors and other authored cells.
 */
public final class FacilityAuthoredRoomTemplate {
    private final FacilityRoomTemplate mapping;
    private final TransformConstructionTemplate construction;

    public FacilityAuthoredRoomTemplate(FacilityRoomTemplate mapping,
            TransformConstructionTemplate construction) {
        this.mapping = mapping == null
                ? new FacilityRoomTemplate(java.util.List.of(), "", null)
                : mapping;
        this.construction = construction == null
                ? new TransformConstructionTemplate(java.util.List.of(),
                        java.util.List.of())
                : construction;
    }

    public static FacilityAuthoredRoomTemplate capture(ServerLevel level,
            FacilityRoom room, AABB physicalBounds, Vec3 origin) {
        return new FacilityAuthoredRoomTemplate(
                FacilityRoomTemplate.capture(room, origin),
                TransformConstructionTemplate.capture(level, physicalBounds,
                        origin));
    }

    public Instance instantiate(ResourceLocation dimension, Vec3 targetOrigin,
            float yawDegrees) {
        return new Instance(mapping.instantiate(dimension, targetOrigin,
                        yawDegrees),
                construction.instantiate(dimension, targetOrigin, yawDegrees));
    }

    public FacilityRoomTemplate mapping() {
        return mapping;
    }

    public TransformConstructionTemplate construction() {
        return construction;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("Mapping", mapping.save());
        tag.put("Construction", construction.save());
        return tag;
    }

    public static FacilityAuthoredRoomTemplate load(CompoundTag tag) {
        if (tag == null) return new FacilityAuthoredRoomTemplate(null, null);
        return new FacilityAuthoredRoomTemplate(
                FacilityRoomTemplate.load(tag.getCompound("Mapping")),
                TransformConstructionTemplate.load(
                        tag.getCompound("Construction")));
    }

    public record Instance(FacilityRoom room,
            TransformConstructionTemplate.Instance construction) {
    }
}
