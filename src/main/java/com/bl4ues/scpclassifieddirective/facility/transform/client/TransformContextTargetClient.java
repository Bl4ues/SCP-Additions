package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformWallFixturePlacement;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import com.bl4ues.scpclassifieddirective.inventory.context.ContextInteractionRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Local-grid target bridge for the normal Context Interaction HUD.
 *
 * Construction selection uses clean logical cells; contextual prompts use the
 * much smaller configured anchor of the actual fixture. Both resolve to the
 * same saved logical address and neither depends on a vanilla proxy BlockPos.
 */
public final class TransformContextTargetClient {
    private TransformContextTargetClient() {
    }

    // A proximity prompt must not raycast every full cube in the facility each
    // tick. Cache only actual door-button addresses, rebuilding the tiny index
    // when a group/surface snapshot is replaced by its authoritative state.
    private record GroupButtons(TransformGroup source,
            List<TransformGroup.GridPos> addresses) { }
    private record SurfaceButton(ConstructionSurface.SurfaceSlot slot,
            boolean overlay, int normalSign) { }
    private record SurfaceButtons(ConstructionSurface source,
            List<SurfaceButton> addresses) { }
    private static final Map<UUID, GroupButtons> GROUP_BUTTONS = new HashMap<>();
    private static final Map<UUID, SurfaceButtons> SURFACE_BUTTONS = new HashMap<>();

    public static void clearButtonCache() {
        GROUP_BUTTONS.clear();
        SURFACE_BUTTONS.clear();
    }

    /** Nearby authored buttons share vanilla's proximity/offscreen prompt
     * behavior. Other fixtures keep precise raycast targeting. */
    public static List<Target> nearbyDoorButtons(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || minecraft.level == null) return List.of();
        Vec3 eye = player.getEyePosition(1.0F);
        var dimension = minecraft.level.dimension().location();
        List<Target> found = new ArrayList<>();
        for (TransformGroup group : TransformConstructionClientState.groups(dimension)) {
            GroupButtons cached = GROUP_BUTTONS.get(group.id());
            if (cached == null || cached.source() != group) {
                List<TransformGroup.GridPos> addresses = new ArrayList<>();
                group.cells().forEach((cell, state) -> {
                    if (TransformWallFixturePlacement.isDoorButton(state))
                        addresses.add(cell);
                });
                cached = new GroupButtons(group, List.copyOf(addresses));
                GROUP_BUTTONS.put(group.id(), cached);
            }
            if (cached.addresses().isEmpty()) continue;
            Vec3 eyeLocal = TransformMath.worldToLocal(group.origin(), eye,
                    group.rotationX(), group.rotationY(), group.rotationZ());
            for (TransformGroup.GridPos cell : cached.addresses()) {
                BlockState state = group.cells().get(cell);
                if (state == null || state.isAir()) continue;
                TransformGroup.GridPos visual =
                        TransformWallFixturePlacement.visualCell(cell, state);
                Vec3 local = new Vec3(visual.x(), visual.y(), visual.z());
                double distanceSqr = local.distanceToSqr(eyeLocal);
                if (distanceSqr > 64.0D) continue;
                found.add(new Target(Kind.GROUP, state,
                        Math.sqrt(distanceSqr), group.id(), cell,
                        null, null, 0));
            }
        }
        for (ConstructionSurface surface :
                TransformConstructionClientState.surfaces(dimension)) {
            SurfaceButtons cached = SURFACE_BUTTONS.get(surface.id());
            if (cached == null || cached.source() != surface) {
                List<SurfaceButton> addresses = new ArrayList<>();
                surface.attachments().forEach((slot, attachment) -> {
                    if (TransformWallFixturePlacement.isDoorButton(
                            attachment.state())) {
                        addresses.add(new SurfaceButton(slot, false,
                                TransformSurfaceGeometry.MAIN_SIDE));
                    }
                });
                surface.overlays().forEach((slot, attachment) -> {
                    if (TransformWallFixturePlacement.isDoorButton(
                            attachment.state())) {
                        addresses.add(new SurfaceButton(slot.slot(), true,
                                slot.normalSign()));
                    }
                });
                cached = new SurfaceButtons(surface, List.copyOf(addresses));
                SURFACE_BUTTONS.put(surface.id(), cached);
            }
            for (SurfaceButton address : cached.addresses()) {
                ConstructionSurface.SurfaceAttachment attachment =
                        address.overlay()
                        ? surface.overlay(address.slot(), address.normalSign())
                        : surface.attachments().get(address.slot());
                if (attachment == null || attachment.state().isAir()) continue;
                Vec3 center = TransformSurfaceGeometry.logicalPoint(surface,
                        address.slot(), false, address.normalSign(),
                        address.overlay(), 0.5D, 0.5D, 0.5D);
                double distanceSqr = center.distanceToSqr(eye);
                if (distanceSqr > 64.0D) continue;
                found.add(new Target(address.overlay()
                        ? Kind.SURFACE_OVERLAY : Kind.SURFACE_MAIN,
                        attachment.state(), Math.sqrt(distanceSqr),
                        null, null, surface.id(), address.slot(),
                        address.normalSign()));
            }
        }
        return found;
    }

    public static Target find(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || minecraft.level == null) return null;

        Target group = groupTarget(player);
        Target surface = surfaceTarget(player);
        if (group == null) return surface;
        if (surface == null) return group;
        return group.distance() <= surface.distance() ? group : surface;
    }

    private static Target groupTarget(LocalPlayer player) {
        TransformGroupPlacementClient.PayloadTarget hit =
                TransformGroupPlacementClient.findContextTarget(player);
        if (hit == null || ContextInteractionRegistry.getBlockRules(
                hit.state().getBlock()).isEmpty()) return null;
        return new Target(Kind.GROUP, hit.state(), hit.distance(),
                hit.group().id(), hit.cell(), null, null, 0);
    }

    private static Target surfaceTarget(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        TransformSurfaceRaycast.Target hit = TransformSurfaceRaycast.target(
                player, TransformConstructionClientState.surfaces(
                        minecraft.level.dimension().location()));
        if (hit == null || hit.layer() == TransformSurfaceRaycast.Layer.GUIDE) {
            return null;
        }
        ConstructionSurface.SurfaceAttachment attachment =
                hit.layer().overlay()
                        ? hit.surface().overlay(hit.slot(), hit.normalSign())
                        : hit.surface().attachments().get(hit.slot());
        if (attachment == null || attachment.state().isAir()
                || ContextInteractionRegistry.getBlockRules(
                        attachment.state().getBlock()).isEmpty()) return null;
        return new Target(hit.layer().overlay()
                        ? Kind.SURFACE_OVERLAY : Kind.SURFACE_MAIN,
                attachment.state(), hit.distance(), null, null,
                hit.surface().id(), hit.slot(), hit.normalSign());
    }

    public static Vec3 anchor(Target target,
            ContextInteractionRegistry.Rule rule) {
        if (target == null || rule == null) return Vec3.ZERO;
        Vec3 vanillaLocal = rule.resolveBlockAnchor(
                BlockPos.ZERO, target.state());
        if (target.kind() == Kind.GROUP) {
            TransformGroup group =
                    TransformConstructionClientState.group(target.groupId());
            if (group == null || target.groupCell() == null) return Vec3.ZERO;
            Vec3 local = new Vec3(
                    target.groupCell().x() - 0.5D + vanillaLocal.x,
                    target.groupCell().y() - 0.5D + vanillaLocal.y,
                    target.groupCell().z() - 0.5D + vanillaLocal.z);
            return TransformMath.localToWorld(group.origin(), local,
                    group.rotationX(), group.rotationY(), group.rotationZ());
        }

        ConstructionSurface surface =
                TransformConstructionClientState.surface(target.surfaceId());
        if (surface == null || target.surfaceSlot() == null) return Vec3.ZERO;
        ConstructionSurface.SurfaceAttachment attachment =
                target.kind() == Kind.SURFACE_OVERLAY
                        ? surface.overlay(target.surfaceSlot(),
                                target.normalSign())
                        : surface.attachments().get(target.surfaceSlot());
        if (attachment == null) return Vec3.ZERO;
        return TransformSurfaceGeometry.logicalPoint(surface,
                target.surfaceSlot(),
                TransformSurfaceGeometry.effectiveDeform(attachment),
                target.normalSign() == 0
                        ? TransformSurfaceGeometry.MAIN_SIDE
                        : target.normalSign(),
                target.kind() == Kind.SURFACE_OVERLAY,
                vanillaLocal.x, vanillaLocal.y, vanillaLocal.z);
    }

    public static boolean alive(Target target) {
        if (target == null) return false;
        if (target.kind() == Kind.GROUP) {
            TransformGroup group =
                    TransformConstructionClientState.group(target.groupId());
            BlockState state = group == null ? null
                    : group.cells().get(target.groupCell());
            return state != null && !state.isAir();
        }
        ConstructionSurface surface =
                TransformConstructionClientState.surface(target.surfaceId());
        if (surface == null) return false;
        ConstructionSurface.SurfaceAttachment attachment =
                target.kind() == Kind.SURFACE_OVERLAY
                        ? surface.overlay(target.surfaceSlot(),
                                target.normalSign())
                        : surface.attachments().get(target.surfaceSlot());
        return attachment != null && !attachment.state().isAir();
    }

    public static void use(Target target) {
        if (target == null) return;
        switch (target.kind()) {
            case GROUP -> TransformConstructionNetwork.useGroupCell(
                    target.groupId(), target.groupCell());
            case SURFACE_MAIN -> TransformConstructionNetwork.useSurfaceSlot(
                    target.surfaceId(), target.surfaceSlot());
            case SURFACE_OVERLAY -> TransformConstructionNetwork.useSurfaceOverlay(
                    target.surfaceId(), target.surfaceSlot(),
                    target.normalSign());
        }
    }

    public enum Kind {
        GROUP, SURFACE_MAIN, SURFACE_OVERLAY
    }

    public record Target(Kind kind, BlockState state, double distance,
            java.util.UUID groupId, TransformGroup.GridPos groupCell,
            java.util.UUID surfaceId,
            ConstructionSurface.SurfaceSlot surfaceSlot, int normalSign) {
    }
}
