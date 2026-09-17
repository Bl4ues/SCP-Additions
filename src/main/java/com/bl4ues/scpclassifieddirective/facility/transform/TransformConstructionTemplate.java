package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface.SurfaceAttachment;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface.SurfaceSlot;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup.GridPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Portable authored-construction payload for the future procedural facility.
 * Geometry is stored relative to a room origin, so an authored room can be
 * copied to another world position/yaw without losing off-grid cells, curved
 * surfaces, block states, deformation flags, or local rotations.
 */
public final class TransformConstructionTemplate {
    private final List<GroupTemplate> groups;
    private final List<SurfaceTemplate> surfaces;

    public TransformConstructionTemplate(List<GroupTemplate> groups,
            List<SurfaceTemplate> surfaces) {
        this.groups = groups == null ? List.of() : List.copyOf(groups);
        this.surfaces = surfaces == null ? List.of() : List.copyOf(surfaces);
    }

    public List<GroupTemplate> groups() {
        return groups;
    }

    public List<SurfaceTemplate> surfaces() {
        return surfaces;
    }

    public static TransformConstructionTemplate capture(ServerLevel level,
            AABB bounds, Vec3 origin) {
        if (level == null || bounds == null || origin == null) {
            return new TransformConstructionTemplate(List.of(), List.of());
        }
        List<GroupTemplate> groups = new ArrayList<>();
        for (TransformGroup group : TransformConstructionManager.groups(level)) {
            if (!bounds.contains(group.origin())) continue;
            groups.add(GroupTemplate.capture(group, origin));
        }
        List<SurfaceTemplate> surfaces = new ArrayList<>();
        for (ConstructionSurface surface
                : TransformConstructionManager.surfaces(level)) {
            if (!intersects(bounds, surface)) continue;
            surfaces.add(SurfaceTemplate.capture(surface, origin));
        }
        return new TransformConstructionTemplate(groups, surfaces);
    }

    public Instance instantiate(ResourceLocation dimension, Vec3 targetOrigin,
            float yawDegrees) {
        ResourceLocation safeDimension = dimension == null
                ? new ResourceLocation("minecraft", "overworld") : dimension;
        Vec3 safeOrigin = targetOrigin == null ? Vec3.ZERO : targetOrigin;
        List<TransformGroup> groups = new ArrayList<>();
        for (GroupTemplate group : this.groups) {
            groups.add(group.instantiate(safeDimension, safeOrigin, yawDegrees));
        }
        List<ConstructionSurface> surfaces = new ArrayList<>();
        for (SurfaceTemplate surface : this.surfaces) {
            surfaces.add(surface.instantiate(safeDimension, safeOrigin,
                    yawDegrees));
        }
        return new Instance(groups, surfaces);
    }

    /**
     * Installs one instantiated room payload into SavedData. This intentionally
     * accepts an already-authored instance so future procedural generation can
     * perform its complete room obstruction pass before mutating the world.
     */
    public static void install(ServerLevel level, Instance instance) {
        if (level == null || instance == null) return;
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        for (TransformGroup group : instance.groups()) data.putGroup(group);
        for (ConstructionSurface surface : instance.surfaces()) {
            data.putSurface(surface);
        }
        TransformConstructionManager.refresh(level.getServer());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag groups = new ListTag();
        for (GroupTemplate group : this.groups) groups.add(group.save());
        tag.put("Groups", groups);
        ListTag surfaces = new ListTag();
        for (SurfaceTemplate surface : this.surfaces) surfaces.add(surface.save());
        tag.put("Surfaces", surfaces);
        return tag;
    }

    public static TransformConstructionTemplate load(CompoundTag tag) {
        if (tag == null) return new TransformConstructionTemplate(List.of(),
                List.of());
        List<GroupTemplate> groups = new ArrayList<>();
        ListTag groupList = tag.getList("Groups", Tag.TAG_COMPOUND);
        for (int i = 0; i < groupList.size(); i++) {
            GroupTemplate group = GroupTemplate.load(groupList.getCompound(i));
            if (group != null) groups.add(group);
        }
        List<SurfaceTemplate> surfaces = new ArrayList<>();
        ListTag surfaceList = tag.getList("Surfaces", Tag.TAG_COMPOUND);
        for (int i = 0; i < surfaceList.size(); i++) {
            SurfaceTemplate surface = SurfaceTemplate.load(
                    surfaceList.getCompound(i));
            if (surface != null) surfaces.add(surface);
        }
        return new TransformConstructionTemplate(groups, surfaces);
    }

    private static boolean intersects(AABB bounds, ConstructionSurface surface) {
        int columns = Math.max(1, surface.columns());
        int rows = Math.max(1, surface.rows());
        int uSamples = Math.min(16, columns);
        int vSamples = Math.min(8, rows);
        for (int u = 0; u <= uSamples; u++) {
            for (int v = 0; v <= vSamples; v++) {
                if (bounds.contains(surface.gridPoint(
                        u / (double) uSamples, v / (double) vSamples))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Vec3 rotateY(Vec3 vector, float yawDegrees) {
        double radians = Math.toRadians(yawDegrees);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        return new Vec3(vector.x * cos - vector.z * sin, vector.y,
                vector.x * sin + vector.z * cos);
    }

    private static Vec3 relative(Vec3 point, Vec3 origin) {
        return point.subtract(origin);
    }

    private static Vec3 absolute(Vec3 relative, Vec3 target, float yaw) {
        return target.add(rotateY(relative, yaw));
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

    public record Instance(List<TransformGroup> groups,
            List<ConstructionSurface> surfaces) {
        public Instance {
            groups = groups == null ? List.of() : List.copyOf(groups);
            surfaces = surfaces == null ? List.of() : List.copyOf(surfaces);
        }
    }

    public record GroupTemplate(Vec3 relativeOrigin, float rotationX,
            float rotationY, float rotationZ, Map<GridPos,
            net.minecraft.world.level.block.state.BlockState> cells) {
        public GroupTemplate {
            relativeOrigin = relativeOrigin == null ? Vec3.ZERO : relativeOrigin;
            cells = cells == null ? Map.of() : Map.copyOf(cells);
        }

        private static GroupTemplate capture(TransformGroup group, Vec3 origin) {
            return new GroupTemplate(relative(group.origin(), origin),
                    group.rotationX(), group.rotationY(), group.rotationZ(),
                    group.cells());
        }

        private TransformGroup instantiate(ResourceLocation dimension,
                Vec3 target, float yaw) {
            return new TransformGroup(UUID.randomUUID(), dimension,
                    absolute(relativeOrigin, target, yaw), rotationX,
                    rotationY + yaw, rotationZ, cells);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            putVec(tag, "Origin", relativeOrigin);
            tag.putFloat("RotationX", rotationX);
            tag.putFloat("RotationY", rotationY);
            tag.putFloat("RotationZ", rotationZ);
            ListTag list = new ListTag();
            for (Map.Entry<GridPos,
                    net.minecraft.world.level.block.state.BlockState> entry
                    : cells.entrySet()) {
                CompoundTag cell = new CompoundTag();
                cell.putInt("X", entry.getKey().x());
                cell.putInt("Y", entry.getKey().y());
                cell.putInt("Z", entry.getKey().z());
                cell.put("State", BlockStateCodec.save(entry.getValue()));
                list.add(cell);
            }
            tag.put("Cells", list);
            return tag;
        }

        private static GroupTemplate load(CompoundTag tag) {
            if (tag == null) return null;
            Map<GridPos, net.minecraft.world.level.block.state.BlockState> cells =
                    new LinkedHashMap<>();
            ListTag list = tag.getList("Cells", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag cell = list.getCompound(i);
                cells.put(new GridPos(cell.getInt("X"), cell.getInt("Y"),
                                cell.getInt("Z")),
                        BlockStateCodec.load(cell.getCompound("State")));
            }
            return new GroupTemplate(getVec(tag, "Origin"),
                    tag.getFloat("RotationX"), tag.getFloat("RotationY"),
                    tag.getFloat("RotationZ"), cells);
        }
    }

    public record SurfaceTemplate(Vec3 bottomStart, Vec3 bottomEnd,
            Vec3 topStart, Vec3 topEnd, Vec3 curveOffset,
            Map<SurfaceSlot, SurfaceAttachment> attachments) {
        public SurfaceTemplate {
            bottomStart = bottomStart == null ? Vec3.ZERO : bottomStart;
            bottomEnd = bottomEnd == null ? Vec3.ZERO : bottomEnd;
            topStart = topStart == null ? Vec3.ZERO : topStart;
            topEnd = topEnd == null ? Vec3.ZERO : topEnd;
            curveOffset = curveOffset == null ? Vec3.ZERO : curveOffset;
            attachments = attachments == null ? Map.of()
                    : Map.copyOf(attachments);
        }

        private static SurfaceTemplate capture(ConstructionSurface surface,
                Vec3 origin) {
            return new SurfaceTemplate(relative(surface.bottomStart(), origin),
                    relative(surface.bottomEnd(), origin),
                    relative(surface.topStart(), origin),
                    relative(surface.topEnd(), origin), surface.curveOffset(),
                    surface.attachments());
        }

        private ConstructionSurface instantiate(ResourceLocation dimension,
                Vec3 target, float yaw) {
            return new ConstructionSurface(UUID.randomUUID(), dimension,
                    absolute(bottomStart, target, yaw),
                    absolute(bottomEnd, target, yaw),
                    absolute(topStart, target, yaw),
                    absolute(topEnd, target, yaw), rotateY(curveOffset, yaw),
                    attachments);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            putVec(tag, "BottomStart", bottomStart);
            putVec(tag, "BottomEnd", bottomEnd);
            putVec(tag, "TopStart", topStart);
            putVec(tag, "TopEnd", topEnd);
            putVec(tag, "CurveOffset", curveOffset);
            ListTag list = new ListTag();
            for (Map.Entry<SurfaceSlot, SurfaceAttachment> entry
                    : attachments.entrySet()) {
                CompoundTag attachment = new CompoundTag();
                attachment.putInt("Column", entry.getKey().column());
                attachment.putInt("Row", entry.getKey().row());
                attachment.putBoolean("Deform", entry.getValue().deform());
                attachment.put("State", BlockStateCodec.save(
                        entry.getValue().state()));
                list.add(attachment);
            }
            tag.put("Attachments", list);
            return tag;
        }

        private static SurfaceTemplate load(CompoundTag tag) {
            if (tag == null) return null;
            Map<SurfaceSlot, SurfaceAttachment> attachments =
                    new LinkedHashMap<>();
            ListTag list = tag.getList("Attachments", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag attachment = list.getCompound(i);
                SurfaceSlot slot = new SurfaceSlot(
                        attachment.getInt("Column"), attachment.getInt("Row"));
                attachments.put(slot, new SurfaceAttachment(
                        BlockStateCodec.load(attachment.getCompound("State")),
                        attachment.getBoolean("Deform")));
            }
            return new SurfaceTemplate(getVec(tag, "BottomStart"),
                    getVec(tag, "BottomEnd"), getVec(tag, "TopStart"),
                    getVec(tag, "TopEnd"), getVec(tag, "CurveOffset"),
                    attachments);
        }
    }
}
