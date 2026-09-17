package com.bl4ues.scpclassifieddirective.facility.transform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistent off-grid construction groups and parametric surfaces. */
public final class TransformConstructionSavedData extends SavedData {
    private static final String DATA_NAME =
            "scp_classified_directive_transform_construction";

    private final Map<UUID, TransformGroup> groups = new LinkedHashMap<>();
    private final Map<UUID, ConstructionSurface> surfaces = new LinkedHashMap<>();
    /**
     * Structural/editor revision only. Runtime BlockState animation updates use
     * quiet writes plus tiny delta packets instead of rebroadcasting the entire
     * authored facility every animation frame.
     */
    private long revision;

    private TransformConstructionSavedData() {
    }

    public static TransformConstructionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                TransformConstructionSavedData::load,
                TransformConstructionSavedData::new, DATA_NAME);
    }

    private static TransformConstructionSavedData load(CompoundTag tag) {
        TransformConstructionSavedData data = new TransformConstructionSavedData();
        ListTag groups = tag.getList("Groups", Tag.TAG_COMPOUND);
        for (int index = 0; index < groups.size(); index++) {
            TransformGroup group = TransformGroup.load(groups.getCompound(index));
            if (group != null) data.groups.put(group.id(), group);
        }
        ListTag surfaces = tag.getList("Surfaces", Tag.TAG_COMPOUND);
        for (int index = 0; index < surfaces.size(); index++) {
            ConstructionSurface surface = ConstructionSurface.load(
                    surfaces.getCompound(index));
            if (surface != null) data.surfaces.put(surface.id(), surface);
        }
        return data;
    }

    public synchronized List<TransformGroup> groups() {
        return List.copyOf(groups.values());
    }

    public synchronized List<ConstructionSurface> surfaces() {
        return List.copyOf(surfaces.values());
    }

    public synchronized long revision() {
        return revision;
    }

    public synchronized TransformGroup group(UUID id) {
        return id == null ? null : groups.get(id);
    }

    public synchronized ConstructionSurface surface(UUID id) {
        return id == null ? null : surfaces.get(id);
    }

    public synchronized void putGroup(TransformGroup group) {
        putGroup(group, true);
    }

    public synchronized void putGroupState(TransformGroup group) {
        putGroup(group, false);
    }

    private void putGroup(TransformGroup group, boolean structural) {
        if (group == null) return;
        groups.put(group.id(), group);
        if (structural) revision++;
        setDirty();
    }

    public synchronized void putSurface(ConstructionSurface surface) {
        putSurface(surface, true);
    }

    public synchronized void putSurfaceState(ConstructionSurface surface) {
        putSurface(surface, false);
    }

    private void putSurface(ConstructionSurface surface, boolean structural) {
        if (surface == null) return;
        surfaces.put(surface.id(), surface);
        if (structural) revision++;
        setDirty();
    }

    public synchronized boolean removeGroup(UUID id) {
        if (id == null || groups.remove(id) == null) return false;
        revision++;
        setDirty();
        return true;
    }

    public synchronized boolean removeSurface(UUID id) {
        if (id == null || surfaces.remove(id) == null) return false;
        revision++;
        setDirty();
        return true;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag) {
        ListTag groups = new ListTag();
        for (TransformGroup group : this.groups.values()) {
            groups.add(group.save());
        }
        tag.put("Groups", groups);
        ListTag surfaces = new ListTag();
        for (ConstructionSurface surface : this.surfaces.values()) {
            surfaces.add(surface.save());
        }
        tag.put("Surfaces", surfaces);
        return tag;
    }
}
