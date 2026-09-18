package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
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
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
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
import java.util.UUID;

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
        if (player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        Selection selected = TransformConstructionClientState.selection();
        if (selected != null && selected.type() == SelectionType.SURFACE) {
            // Surface placement/editing owns the use click while a surface is
            // selected. Do not let an unrelated rigid group behind it steal it.
            return;
        }

        if (player.getMainHandItem().getItem() instanceof BlockItem) {
            if (!player.isCreative()) return;
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
            return;
        }

        // Runtime interaction is also local-space. This is deliberately
        // independent from minecraft.hitResult: a rotated button may share a
        // vanilla BlockPos with its mounting wall and may have no proxy there.
        PayloadTarget payload = findPayloadTarget(player);
        if (payload == null || !interactive(payload.state())) return;
        TransformConstructionNetwork.useGroupCell(payload.group().id(),
                payload.cell());
        event.setCanceled(true);
    }

    private static TransformGroup.GridPos nearestCell(Vec3 local) {
        return new TransformGroup.GridPos(
                (int) Math.floor(local.x + 0.5D),
                (int) Math.floor(local.y + 0.5D),
                (int) Math.floor(local.z + 0.5D));
    }

    /**
     * Editor picking uses the authored local 1x1 cells rather than proxy or
     * payload bounding boxes. This keeps an Off-Grid group selectable even when
     * its world cell is occupied by a normal block and therefore has no proxy.
     */
    static UUID findAimedGroup(LocalPlayer player) {
        if (player == null) return null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 worldRay = player.getViewVector(1.0F).normalize();
        double limit = MAX_DISTANCE;

        HitResult vanilla = minecraft.hitResult;
        if (vanilla instanceof BlockHitResult blockHit
                && vanilla.getType() == HitResult.Type.BLOCK) {
            // Allow the transformed local cell sharing the clicked vanilla block
            // to remain selectable, but never pick through a whole wall.
            limit = Math.min(limit,
                    eye.distanceTo(blockHit.getLocation()) + 0.90D);
        }

        UUID bestId = null;
        double bestDistance = limit + 1.0D;
        for (TransformGroup group : TransformConstructionClientState.groups(
                minecraft.level.dimension().location())) {
            Vec3 localEye = TransformMath.worldToLocal(group.origin(), eye,
                    group.rotationX(), group.rotationY(), group.rotationZ());
            Vec3 localRay = TransformMath.inverseRotate(worldRay,
                    group.rotationX(), group.rotationY(), group.rotationZ())
                    .normalize();
            for (TransformGroup.GridPos cell : group.cells().keySet()) {
                Hit hit = intersect(localEye, localRay, cell);
                if (hit == null || hit.distance() < 0.0D
                        || hit.distance() > limit
                        || hit.distance() >= bestDistance) continue;
                bestDistance = hit.distance();
                bestId = group.id();
            }
        }
        return bestId;
    }

    static BlockState findAimedPayloadState(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || minecraft.level == null) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 worldRay = player.getViewVector(1.0F).normalize();
        double limit = MAX_DISTANCE;

        HitResult vanilla = minecraft.hitResult;
        if (vanilla instanceof BlockHitResult blockHit
                && vanilla.getType() == HitResult.Type.BLOCK
                && !minecraft.level.getBlockState(blockHit.getBlockPos()).is(
                        com.bl4ues.scpclassifieddirective.facility.transform
                                .TransformConstructionModule.getProxy())) {
            limit = Math.min(limit,
                    eye.distanceTo(blockHit.getLocation()) + 0.08D);
        }

        BlockState bestState = null;
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
                BlockState state = entry.getValue();
                if (state == null || state.isAir()) continue;
                Hit hit = intersect(localEye, localRay, entry.getKey());
                if (hit == null || hit.distance() < 0.0D
                        || hit.distance() > limit
                        || hit.distance() >= bestDistance) continue;
                bestDistance = hit.distance();
                bestState = state;
            }
        }
        return bestState;
    }

    private static PayloadTarget findPayloadTarget(LocalPlayer player) {
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
            // The transformed payload may sit just in front of / inside the
            // clicked vanilla wall. Permit that shared-cell epsilon, but never
            // raycast through unrelated solid geometry.
            limit = Math.min(limit, eye.distanceTo(blockHit.getLocation()) + 0.08D);
        }

        PayloadTarget best = null;
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
                BlockState state = entry.getValue();
                if (state == null || state.isAir() || !interactive(state)) {
                    continue;
                }
                Hit hit = intersect(localEye, localRay, entry.getKey());
                if (hit == null || hit.distance() < 0.0D
                        || hit.distance() > limit
                        || hit.distance() >= bestDistance) continue;
                bestDistance = hit.distance();
                best = new PayloadTarget(group, entry.getKey(), state,
                        hit.face(), eye.add(worldRay.scale(hit.distance())),
                        hit.distance());
            }
        }
        return best;
    }

    private static boolean interactive(BlockState state) {
        return state != null && (state.getBlock() instanceof ButtonBlock
                || state.getBlock() instanceof LeverBlock
                || FacilityModule.isFacilityDoor(state));
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
                // Placement authors against the clean local 1x1x1 grid
                // cell, not the payload's visual/collision AABB. Wall-mounted
                // blocks, doors and thin controls therefore resolve the same
                // local face a vanilla block would expose before the group's
                // transform is applied.
                Hit hit = intersect(localEye, localRay, entry.getKey());
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

    private record PayloadTarget(TransformGroup group,
            TransformGroup.GridPos cell, BlockState state, Direction face,
            Vec3 worldHit, double distance) {
    }
}
