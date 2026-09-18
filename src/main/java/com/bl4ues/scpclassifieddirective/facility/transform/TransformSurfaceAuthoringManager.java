package com.bl4ues.scpclassifieddirective.facility.transform;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server-authoritative three-point authoring flow for construction surfaces. */
public final class TransformSurfaceAuthoringManager {
    private static final int MAX_SURFACE_SLOTS = 65_536;
    private static final Map<MinecraftServer, Map<UUID, PendingSurface>> PENDING =
            new WeakHashMap<>();

    private TransformSurfaceAuthoringManager() {
    }

    public static void selectPoint(ServerPlayer player, Vec3 hit) {
        if (!TransformConstructionManager.canEdit(player) || hit == null
                || !(player.level() instanceof ServerLevel level)) return;
        Map<UUID, PendingSurface> pending = PENDING.computeIfAbsent(
                level.getServer(), ignored -> new HashMap<>());
        PendingSurface current = pending.get(player.getUUID());
        ResourceKey<Level> dimension = level.dimension();

        if (current == null || !current.dimension().equals(dimension)) {
            pending.put(player.getUUID(), new PendingSurface(dimension, hit, null));
            player.displayClientMessage(Component.literal(
                    "Surface point 1 set. Choose the other end of the baseline."),
                    true);
            return;
        }

        if (current.end() == null) {
            Vec3 end = new Vec3(hit.x, current.start().y, hit.z);
            if (end.distanceToSqr(current.start()) < 0.04D) {
                player.displayClientMessage(Component.literal(
                        "The surface baseline is too short."), true);
                return;
            }
            pending.put(player.getUUID(), new PendingSurface(dimension,
                    current.start(), end));
            player.displayClientMessage(Component.literal(
                    "Baseline set. Choose the wall height."), true);
            return;
        }

        double height = hit.y - current.start().y;
        if (Math.abs(height) < 0.125D) {
            player.displayClientMessage(Component.literal(
                    "Choose a height farther from the baseline."), true);
            return;
        }
        Vec3 topStart = current.start().add(0.0D, height, 0.0D);
        Vec3 topEnd = current.end().add(0.0D, height, 0.0D);
        ConstructionSurface surface = new ConstructionSurface(UUID.randomUUID(),
                level.dimension().location(), current.start(), current.end(),
                topStart, topEnd, Vec3.ZERO, Map.of());
        if ((long) surface.columns() * surface.rows() > MAX_SURFACE_SLOTS) {
            player.displayClientMessage(Component.literal(
                    "Surface is too large."), true);
            return;
        }

        TransformConstructionSavedData.get(level.getServer()).putSurface(surface);
        pending.remove(player.getUUID());
        TransformConstructionManager.refreshSurface(level.getServer(),
                surface.id());
        player.displayClientMessage(Component.literal(
                "Construction surface created."), true);
    }

    public static void cancel(ServerPlayer player) {
        if (!TransformConstructionManager.canEdit(player)
                || player.getServer() == null) return;
        Map<UUID, PendingSurface> pending = PENDING.get(player.getServer());
        boolean removed = pending != null
                && pending.remove(player.getUUID()) != null;
        player.displayClientMessage(Component.literal(removed
                ? "Surface selection cancelled."
                : "No surface selection is active."), true);
    }

    private record PendingSurface(ResourceKey<Level> dimension, Vec3 start,
            Vec3 end) {
    }
}
