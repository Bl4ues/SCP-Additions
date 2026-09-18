package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Selection;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SelectionType;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

/**
 * Precise local-space placement for rigid transformed groups. This bypasses
 * the axis-aligned vanilla proxy hit result, so blocks attached to an existing
 * off-grid block inherit the same TransformGroup and its exact local face.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformGroupPlacementClient {
    private static final double MAX_DISTANCE = 32.0D;
    private static final double EPSILON = 1.0E-7D;

    private TransformGroupPlacementClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteraction(
            InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null
                || !player.isCreative()
                || !(player.getMainHandItem().getItem() instanceof BlockItem)) {
            return;
        }
        Selection selected = TransformConstructionClientState.selection();
        if (selected != null && selected.type() == SelectionType.SURFACE) {
            // Surface placement owns the use click while a surface is selected.
            // Do not let an unrelated off-grid group behind it steal the block.
            return;
        }

        Target target = findTarget(player);
        if (target == null) return;
        Vec3 localHit = TransformMath.worldToLocal(target.group().origin(),
                target.worldHit(), target.group().rotationX(),
                target.group().rotationY(), target.group().rotationZ());
        Vec3 outside = localHit.add(
                Vec3.atLowerCornerOf(target.face().getNormal()).scale(0.501D));
        TransformGroup.GridPos next = nearestCell(outside);
        TransformConstructionNetwork.placeGroupBlock(target.group().id(),
                target.source(), next, target.face(), target.worldHit());
        event.setCanceled(true);
    }

    private static TransformGroup.GridPos nearestCell(Vec3 local) {
        return new TransformGroup.GridPos(
                (int) Math.floor(local.x + 0.5D),
                (int) Math.floor(local.y + 0.5D),
                (int) Math.floor(local.z + 0.5D));
    }

    private static Target findTarget(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 eye = player.getEyePosition();
        Vec3 worldRay = player.getViewVector(1.0F).normalize();

        double limit = MAX_DISTANCE;
        HitResult vanilla = minecraft.hitResult;
        if (vanilla instanceof BlockHitResult blockHit
                && vanilla.getType() == HitResult.Type.BLOCK
                && !minecraft.level.getBlockState(blockHit.getBlockPos())
                        .is(com.bl4ues.scpclassifieddirective.facility.transform
                                .TransformConstructionModule.getProxy())) {
            limit = Math.min(limit, eye.distanceTo(blockHit.getLocation()) + 0.04D);
        }

        Selection selected = TransformConstructionClientState.selection();
        Target best = null;
        double bestDistance = limit + 1.0D;
        for (TransformGroup group : TransformConstructionClientState.groups(
                minecraft.level.dimension().location())) {
            Vec3 localEye = TransformMath.worldToLocal(group.origin(), eye,
                    group.rotationX(), group.rotationY(), group.rotationZ());
            Vec3 localRay = TransformMath.inverseRotate(worldRay,
                    group.rotationX(), group.rotationY(), group.rotationZ())
                    .normalize();
            for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                    : group.cells().entrySet()) {
                boolean guideCell = entry.getValue().isAir()
                        && selected != null
                        && selected.type() == SelectionType.GROUP
                        && group.id().equals(selected.id());
                if (entry.getValue().isAir() && !guideCell) continue;
                Hit hit = intersectState(localEye, localRay,
                        entry.getKey(), entry.getValue(), guideCell);
                if (hit == null || hit.distance() < 0.0D
                        || hit.distance() > limit
                        || hit.distance() >= bestDistance) continue;
                bestDistance = hit.distance();
                Vec3 worldHit = eye.add(worldRay.scale(hit.distance()));
                best = new Target(group, entry.getKey(), hit.face(), worldHit);
            }
        }
        return best;
    }

    private static Hit intersectState(Vec3 origin, Vec3 ray,
            TransformGroup.GridPos cell, BlockState state, boolean guideCell) {
        if (guideCell || state == null || state.isAir()) {
            return intersect(origin, ray, new AABB(cell.x() - 0.5D,
                    cell.y() - 0.5D, cell.z() - 0.5D,
                    cell.x() + 0.5D, cell.y() + 0.5D,
                    cell.z() + 0.5D));
        }
        VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE,
                BlockPos.ZERO, CollisionContext.empty());
        List<AABB> boxes = shape.isEmpty()
                ? List.of(new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D))
                : shape.toAabbs();
        Hit best = null;
        for (AABB box : boxes) {
            AABB local = box.move(cell.x() - 0.5D,
                    cell.y() - 0.5D, cell.z() - 0.5D);
            Hit hit = intersect(origin, ray, local);
            if (hit != null && (best == null
                    || hit.distance() < best.distance())) best = hit;
        }
        return best;
    }

    private static Hit intersect(Vec3 origin, Vec3 ray,
            TransformGroup.GridPos cell) {
        return intersect(origin, ray, new AABB(cell.x() - 0.5D,
                cell.y() - 0.5D, cell.z() - 0.5D,
                cell.x() + 0.5D, cell.y() + 0.5D,
                cell.z() + 0.5D));
    }

    private static Hit intersect(Vec3 origin, Vec3 ray, AABB box) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        double tMin = Double.NEGATIVE_INFINITY;
        double tMax = Double.POSITIVE_INFINITY;
        Direction enter = null;

        AxisHit x = axis(origin.x, ray.x, minX, maxX,
                Direction.WEST, Direction.EAST);
        if (x == null) return null;
        if (x.near() > tMin) { tMin = x.near(); enter = x.nearFace(); }
        tMax = Math.min(tMax, x.far());

        AxisHit y = axis(origin.y, ray.y, minY, maxY,
                Direction.DOWN, Direction.UP);
        if (y == null) return null;
        if (y.near() > tMin) { tMin = y.near(); enter = y.nearFace(); }
        tMax = Math.min(tMax, y.far());

        AxisHit z = axis(origin.z, ray.z, minZ, maxZ,
                Direction.NORTH, Direction.SOUTH);
        if (z == null) return null;
        if (z.near() > tMin) { tMin = z.near(); enter = z.nearFace(); }
        tMax = Math.min(tMax, z.far());

        if (tMax + EPSILON < Math.max(tMin, 0.0D)) return null;
        double distance = tMin >= 0.0D ? tMin : tMax;
        if (!Double.isFinite(distance) || distance < 0.0D) return null;
        if (enter == null || tMin < 0.0D) {
            Vec3 point = origin.add(ray.scale(distance))
                    .subtract(box.getCenter());
            enter = dominant(point);
        }
        return new Hit(distance, enter);
    }

    private static AxisHit axis(double origin, double direction,
            double min, double max, Direction minFace, Direction maxFace) {
        if (Math.abs(direction) < EPSILON) {
            return origin >= min && origin <= max
                    ? new AxisHit(Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY, minFace) : null;
        }
        double a = (min - origin) / direction;
        double b = (max - origin) / direction;
        if (a <= b) return new AxisHit(a, b, minFace);
        return new AxisHit(b, a, maxFace);
    }

    private static Direction dominant(Vec3 point) {
        double ax = Math.abs(point.x);
        double ay = Math.abs(point.y);
        double az = Math.abs(point.z);
        if (ay >= ax && ay >= az) return point.y >= 0.0D
                ? Direction.UP : Direction.DOWN;
        if (ax >= az) return point.x >= 0.0D
                ? Direction.EAST : Direction.WEST;
        return point.z >= 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    private record AxisHit(double near, double far, Direction nearFace) {
    }

    private record Hit(double distance, Direction face) {
    }

    private record Target(TransformGroup group, TransformGroup.GridPos source,
            Direction face, Vec3 worldHit) {
    }
}
