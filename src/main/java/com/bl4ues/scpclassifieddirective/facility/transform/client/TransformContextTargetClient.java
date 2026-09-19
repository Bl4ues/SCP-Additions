package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import com.bl4ues.scpclassifieddirective.inventory.context.ContextInteractionRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

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
                target.surfaceSlot(), attachment.deform(),
                target.normalSign() == 0 ? 1 : target.normalSign(),
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
