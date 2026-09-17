package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * Completes transformed placement after the proxy manager consumes the click.
 * Vanilla placement rules still decide stairs/slabs/facing/etc, then cardinal
 * directions are converted from world space into the authored local frame.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TransformPlacementStateRuntime {
    private static final double MAX_MATCH_DISTANCE_SQR = 4.0D;

    private TransformPlacementStateRuntime() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void afterProxyPlacement(
            PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getItemStack().getItem() instanceof BlockItem item)
                || !level.getBlockState(event.getPos()).is(
                        TransformConstructionModule.getProxy())) {
            return;
        }

        BlockHitResult hit = event.getHitVec();
        BlockState contextual = item.getBlock().getStateForPlacement(
                new BlockPlaceContext(new UseOnContext(player,
                        InteractionHand.MAIN_HAND, hit)));
        if (contextual == null) contextual = item.getBlock().defaultBlockState();

        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        Match match = nearestMatching(data, level, hit.getLocation(), item);
        if (match == null) return;

        if (match.group != null) {
            TransformGroup group = match.group;
            BlockState local = localizeForGroup(contextual, group,
                    match.gridPos, hit.getLocation());
            if (local.equals(group.cells().get(match.gridPos))) return;
            data.putGroupState(group.withCell(match.gridPos, local));
            TransformConstructionNetwork.broadcastGroupCell(level, group.id(),
                    match.gridPos, local);
        } else if (match.surface != null) {
            ConstructionSurface surface = match.surface;
            ConstructionSurface.SurfaceAttachment old =
                    surface.attachments().get(match.surfaceSlot);
            if (old == null) return;
            BlockState local = localizeForSurface(contextual, surface,
                    match.surfaceSlot);
            if (local.equals(old.state())) return;
            data.putSurfaceState(surface.withAttachment(match.surfaceSlot, local,
                    old.deform()));
            TransformConstructionNetwork.broadcastSurfaceSlot(level,
                    surface.id(), match.surfaceSlot, local, old.deform());
        }
        TransformConstructionManager.refresh(level.getServer());
    }

    private static Match nearestMatching(TransformConstructionSavedData data,
            ServerLevel level, Vec3 world, BlockItem item) {
        Match best = null;
        double distance = MAX_MATCH_DISTANCE_SQR;
        for (TransformGroup group : data.groups()) {
            if (!group.dimension().equals(level.dimension().location())) continue;
            Vec3 local = TransformMath.worldToLocal(group.origin(), world,
                    group.rotationX(), group.rotationY(), group.rotationZ());
            for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                    : group.cells().entrySet()) {
                if (entry.getValue().getBlock() != item.getBlock()) continue;
                TransformGroup.GridPos pos = entry.getKey();
                double candidate = local.distanceToSqr(
                        new Vec3(pos.x(), pos.y(), pos.z()));
                if (candidate < distance) {
                    distance = candidate;
                    best = Match.group(group, pos);
                }
            }
        }
        for (ConstructionSurface surface : data.surfaces()) {
            if (!surface.dimension().equals(level.dimension().location())) continue;
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.attachments().entrySet()) {
                if (entry.getValue().state().getBlock() != item.getBlock()) continue;
                ConstructionSurface.SurfaceSlot slot = entry.getKey();
                double u = (slot.column() + 0.5D) / surface.columns();
                double v = (slot.row() + 0.5D) / surface.rows();
                Vec3 center = surface.gridPoint(u, v)
                        .add(surface.gridNormal(u, v).scale(0.5D));
                double candidate = center.distanceToSqr(world);
                if (candidate < distance) {
                    distance = candidate;
                    best = Match.surface(surface, slot);
                }
            }
        }
        return best;
    }

    private static BlockState localizeForGroup(BlockState state,
            TransformGroup group, TransformGroup.GridPos cell, Vec3 worldHit) {
        Vec3 x = TransformMath.rotate(new Vec3(1, 0, 0), group.rotationX(),
                group.rotationY(), group.rotationZ());
        Vec3 y = TransformMath.rotate(new Vec3(0, 1, 0), group.rotationX(),
                group.rotationY(), group.rotationZ());
        Vec3 z = TransformMath.rotate(new Vec3(0, 0, 1), group.rotationX(),
                group.rotationY(), group.rotationZ());
        state = localize(state, x, y, z);
        if (!state.hasProperty(BlockStateProperties.ATTACH_FACE)) return state;

        Vec3 localHit = TransformMath.worldToLocal(group.origin(), worldHit,
                group.rotationX(), group.rotationY(), group.rotationZ());
        Vec3 delta = localHit.subtract(cell.x(), cell.y(), cell.z());
        Direction towardSupport = dominantDirection(delta);
        Direction outward = towardSupport.getOpposite();
        return attachToLocalFace(state, outward);
    }

    private static BlockState localizeForSurface(BlockState state,
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot) {
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 x = surface.gridTangent(u, v);
        Vec3 z = surface.gridNormal(u, v);
        Vec3 y = TransformMath.safeNormalize(z.cross(x),
                surface.gridVertical(u));
        state = localize(state, x, y, z);
        // Surface payloads extend from local Z=0 toward +Z. Wall controls need
        // their support behind them on the authored plane, so SOUTH is the
        // canonical local outward direction regardless of world-space curve.
        if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
            state = attachToLocalFace(state, Direction.SOUTH);
        }
        return state;
    }

    private static BlockState attachToLocalFace(BlockState state,
            Direction outward) {
        if (!state.hasProperty(BlockStateProperties.ATTACH_FACE)) return state;
        AttachFace face = outward == Direction.UP ? AttachFace.FLOOR
                : outward == Direction.DOWN ? AttachFace.CEILING
                : AttachFace.WALL;
        state = state.setValue(BlockStateProperties.ATTACH_FACE, face);
        if (outward.getAxis().isHorizontal()
                && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                    outward);
        }
        return state;
    }

    private static BlockState localize(BlockState state, Vec3 localX,
            Vec3 localY, Vec3 localZ) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction world = state.getValue(BlockStateProperties.FACING);
            Direction local = localDirection(world, localX, localY, localZ,
                    false);
            state = state.setValue(BlockStateProperties.FACING, local);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction world = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction local = localDirection(world, localX, localY, localZ,
                    true);
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, local);
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            Direction.Axis worldAxis = state.getValue(BlockStateProperties.AXIS);
            Vec3 world = switch (worldAxis) {
                case X -> new Vec3(1, 0, 0);
                case Y -> new Vec3(0, 1, 0);
                case Z -> new Vec3(0, 0, 1);
            };
            double dx = Math.abs(world.dot(localX));
            double dy = Math.abs(world.dot(localY));
            double dz = Math.abs(world.dot(localZ));
            Direction.Axis localAxis = dy >= dx && dy >= dz
                    ? Direction.Axis.Y : dx >= dz
                    ? Direction.Axis.X : Direction.Axis.Z;
            state = state.setValue(BlockStateProperties.AXIS, localAxis);
        }
        return state;
    }

    private static Direction localDirection(Direction worldDirection,
            Vec3 localX, Vec3 localY, Vec3 localZ, boolean horizontalOnly) {
        Vec3 world = Vec3.atLowerCornerOf(worldDirection.getNormal());
        double x = world.dot(localX);
        double y = world.dot(localY);
        double z = world.dot(localZ);
        if (!horizontalOnly && Math.abs(y) >= Math.abs(x)
                && Math.abs(y) >= Math.abs(z)) {
            return y >= 0 ? Direction.UP : Direction.DOWN;
        }
        if (Math.abs(x) >= Math.abs(z)) {
            return x >= 0 ? Direction.EAST : Direction.WEST;
        }
        return z >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static Direction dominantDirection(Vec3 delta) {
        double ax = Math.abs(delta.x);
        double ay = Math.abs(delta.y);
        double az = Math.abs(delta.z);
        if (ay >= ax && ay >= az) {
            return delta.y >= 0.0D ? Direction.UP : Direction.DOWN;
        }
        if (ax >= az) return delta.x >= 0.0D ? Direction.EAST : Direction.WEST;
        return delta.z >= 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    private static final class Match {
        private final TransformGroup group;
        private final TransformGroup.GridPos gridPos;
        private final ConstructionSurface surface;
        private final ConstructionSurface.SurfaceSlot surfaceSlot;

        private Match(TransformGroup group, TransformGroup.GridPos gridPos,
                ConstructionSurface surface,
                ConstructionSurface.SurfaceSlot surfaceSlot) {
            this.group = group;
            this.gridPos = gridPos;
            this.surface = surface;
            this.surfaceSlot = surfaceSlot;
        }

        private static Match group(TransformGroup group,
                TransformGroup.GridPos pos) {
            return new Match(group, pos, null, null);
        }

        private static Match surface(ConstructionSurface surface,
                ConstructionSurface.SurfaceSlot slot) {
            return new Match(null, null, surface, slot);
        }
    }
}
