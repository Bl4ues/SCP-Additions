package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformWallFixturePlacement.DoorButtonPhase;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-authoritative interaction for SCP:CD's authored Unity door buttons
 * when they live in a transformed local grid.
 *
 * Vanilla button events require a real BlockPos. Transformed construction
 * deliberately does not fake one, so the same CLOSED -> OPENING -> OPEN and
 * OPEN -> CLOSING -> CLOSED contract is reproduced against saved logical state.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TransformFacilityButtonRuntime {
    private static final int TRANSITION_TICKS = 21;
    private static final Map<MinecraftServer, Map<ButtonKey, Pending>> PENDING =
            new WeakHashMap<>();

    private TransformFacilityButtonRuntime() {
    }

    public static boolean useGroupCell(ServerPlayer player, UUID groupId,
            TransformGroup.GridPos cell) {
        if (player == null || groupId == null || cell == null
                || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(level.getServer());
        TransformGroup group = data.group(groupId);
        if (group == null || !group.dimension().equals(
                level.dimension().location())) return false;
        BlockState state = group.cells().get(cell);
        if (!TransformWallFixturePlacement.isDoorButton(state)) return false;
        TransformGroup.GridPos visual =
                TransformWallFixturePlacement.visualCell(cell, state);
        Vec3 center = group.cellCenter(visual);
        if (player.getEyePosition().distanceToSqr(center) > 36.0D) return false;
        return activate(level, ButtonTarget.group(group, cell, state));
    }

    public static boolean useSurfaceSlot(ServerPlayer player, UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot) {
        if (player == null || surfaceId == null || slot == null
                || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(level.getServer());
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface == null || !surface.dimension().equals(
                level.dimension().location())) return false;
        ConstructionSurface.SurfaceAttachment attachment =
                surface.attachments().get(slot);
        if (attachment == null
                || !TransformWallFixturePlacement.isDoorButton(
                        attachment.state())) return false;
        ConstructionSurface.SurfaceSlot visual =
                TransformWallFixturePlacement.visualSlot(surface, slot,
                        attachment.state(), TransformSurfaceGeometry.MAIN_SIDE);
        Vec3 center = TransformSurfaceGeometry.cellCenter(surface, visual,
                TransformSurfaceGeometry.MAIN_SIDE, false);
        if (player.getEyePosition().distanceToSqr(center) > 36.0D) return false;
        return activate(level, ButtonTarget.surface(surface, slot, 0,
                attachment.state()));
    }

    public static boolean useSurfaceOverlay(ServerPlayer player, UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int normalSign) {
        if (player == null || surfaceId == null || slot == null
                || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        int side = normalSign < 0 ? -1 : 1;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(level.getServer());
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface == null || !surface.dimension().equals(
                level.dimension().location())) return false;
        ConstructionSurface.SurfaceAttachment attachment =
                surface.overlay(slot, side);
        if (attachment == null
                || !TransformWallFixturePlacement.isDoorButton(
                        attachment.state())) return false;
        ConstructionSurface.SurfaceSlot visual =
                TransformWallFixturePlacement.visualSlot(surface, slot,
                        attachment.state(), side);
        Vec3 center = TransformSurfaceGeometry.cellCenter(surface, visual, side,
                true);
        if (player.getEyePosition().distanceToSqr(center) > 36.0D) return false;
        return activate(level, ButtonTarget.surface(surface, slot, side,
                attachment.state()));
    }

    private static boolean activate(ServerLevel level, ButtonTarget target) {
        DoorButtonPhase phase =
                TransformWallFixturePlacement.doorButtonPhase(target.state());
        if (phase == null) return false;
        if (phase == DoorButtonPhase.LOCKED
                || phase == DoorButtonPhase.OPENING
                || phase == DoorButtonPhase.CLOSING) {
            return true;
        }

        boolean opening = phase == DoorButtonPhase.CLOSED;
        DoorButtonPhase transition = opening
                ? DoorButtonPhase.OPENING : DoorButtonPhase.CLOSING;
        DoorButtonPhase endpoint = opening
                ? DoorButtonPhase.OPEN : DoorButtonPhase.CLOSED;
        BlockState next = TransformWallFixturePlacement.doorButtonState(
                target.state(), transition);
        set(level, target, next);

        PENDING.computeIfAbsent(level.getServer(), ignored -> new HashMap<>())
                .put(target.key(), new Pending(
                        level.getServer().getTickCount() + TRANSITION_TICKS,
                        endpoint));
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        Map<ButtonKey, Pending> pending = PENDING.get(server);
        if (pending == null || pending.isEmpty()) return;

        int tick = server.getTickCount();
        Iterator<Map.Entry<ButtonKey, Pending>> iterator =
                pending.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ButtonKey, Pending> entry = iterator.next();
            if (tick < entry.getValue().tick()) continue;
            complete(server, entry.getKey(), entry.getValue().endpoint());
            iterator.remove();
        }
    }

    private static void complete(MinecraftServer server, ButtonKey key,
            DoorButtonPhase endpoint) {
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        if (key.groupId() != null) {
            TransformGroup group = data.group(key.groupId());
            if (group == null) return;
            ServerLevel level = level(server, group.dimension());
            if (level == null) return;
            BlockState current = group.cells().get(key.groupCell());
            DoorButtonPhase phase =
                    TransformWallFixturePlacement.doorButtonPhase(current);
            if (!transitionMatches(phase, endpoint)) return;
            set(level, ButtonTarget.group(group, key.groupCell(), current),
                    TransformWallFixturePlacement.doorButtonState(
                            current, endpoint));
            return;
        }

        ConstructionSurface surface = data.surface(key.surfaceId());
        if (surface == null) return;
        ServerLevel level = level(server, surface.dimension());
        if (level == null) return;
        ConstructionSurface.SurfaceAttachment attachment =
                key.normalSign() == 0
                        ? surface.attachments().get(key.surfaceSlot())
                        : surface.overlay(key.surfaceSlot(), key.normalSign());
        if (attachment == null) return;
        DoorButtonPhase phase =
                TransformWallFixturePlacement.doorButtonPhase(
                        attachment.state());
        if (!transitionMatches(phase, endpoint)) return;
        set(level, ButtonTarget.surface(surface, key.surfaceSlot(),
                        key.normalSign(), attachment.state()),
                TransformWallFixturePlacement.doorButtonState(
                        attachment.state(), endpoint));
    }

    private static boolean transitionMatches(DoorButtonPhase current,
            DoorButtonPhase endpoint) {
        return endpoint == DoorButtonPhase.OPEN
                ? current == DoorButtonPhase.OPENING
                : current == DoorButtonPhase.CLOSING;
    }

    private static void set(ServerLevel level, ButtonTarget target,
            BlockState next) {
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(level.getServer());
        if (target.group() != null) {
            TransformGroup updated = target.group().withCell(
                    target.groupCell(), next);
            data.putGroupState(updated);
            TransformPowerQuery.refreshGroupCell(level.getServer(), updated,
                    target.groupCell());
            TransformConstructionNetwork.broadcastGroupCell(level,
                    updated.id(), target.groupCell(), next);
            return;
        }

        ConstructionSurface.SurfaceAttachment old =
                target.normalSign() == 0
                        ? target.surface().attachments().get(target.surfaceSlot())
                        : target.surface().overlay(target.surfaceSlot(),
                                target.normalSign());
        if (old == null) return;
        ConstructionSurface updated;
        if (target.normalSign() == 0) {
            updated = target.surface().withAttachment(target.surfaceSlot(),
                    next, old.deform());
            TransformConstructionNetwork.broadcastSurfaceSlot(level,
                    updated.id(), target.surfaceSlot(), next, old.deform());
        } else {
            updated = target.surface().withOverlay(target.surfaceSlot(),
                    target.normalSign(), next, old.deform());
            TransformConstructionNetwork.broadcastSurfaceOverlay(level,
                    updated.id(), target.surfaceSlot(), target.normalSign(),
                    next, old.deform());
        }
        data.putSurfaceState(updated);
        TransformPowerQuery.refreshSurfaceSlot(level.getServer(), updated,
                target.surfaceSlot());
    }

    private static ServerLevel level(MinecraftServer server,
            net.minecraft.resources.ResourceLocation dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().equals(dimension)) return level;
        }
        return null;
    }

    private record Pending(int tick, DoorButtonPhase endpoint) {
    }

    private record ButtonKey(UUID groupId, TransformGroup.GridPos groupCell,
            UUID surfaceId, ConstructionSurface.SurfaceSlot surfaceSlot,
            int normalSign) {
        static ButtonKey group(UUID id, TransformGroup.GridPos cell) {
            return new ButtonKey(id, cell, null, null, 0);
        }

        static ButtonKey surface(UUID id,
                ConstructionSurface.SurfaceSlot slot, int normalSign) {
            return new ButtonKey(null, null, id, slot,
                    normalSign == 0 ? 0 : (normalSign < 0 ? -1 : 1));
        }
    }

    private record ButtonTarget(TransformGroup group,
            TransformGroup.GridPos groupCell, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot surfaceSlot, int normalSign,
            BlockState state) {
        static ButtonTarget group(TransformGroup group,
                TransformGroup.GridPos cell, BlockState state) {
            return new ButtonTarget(group, cell, null, null, 0, state);
        }

        static ButtonTarget surface(ConstructionSurface surface,
                ConstructionSurface.SurfaceSlot slot, int normalSign,
                BlockState state) {
            return new ButtonTarget(null, null, surface, slot,
                    normalSign == 0 ? 0 : (normalSign < 0 ? -1 : 1), state);
        }

        ButtonKey key() {
            return group != null ? ButtonKey.group(group.id(), groupCell)
                    : ButtonKey.surface(surface.id(), surfaceSlot, normalSign);
        }
    }
}
