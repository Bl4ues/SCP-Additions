package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformWallFixturePlacement;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Selection;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SelectionType;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import com.bl4ues.scpclassifieddirective.inventory.context.ContextInteractionRegistry;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderLevels;
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

    // Group placement and use share one nearest-target arbitration with
        // curved surfaces in TransformSurfacePlacementClient. The former
        // HIGHEST-priority handler here pre-empted it and could steal a
        // click from a nearer Surface (or duplicate an interaction).

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
        GridTarget target = trace(player, false, state -> true);
        return target == null ? null : target.group().id();
    }

    static BlockState findAimedPayloadState(LocalPlayer player) {
        GridTarget target = trace(player, false,
                state -> state != null && !state.isAir());
        return target == null ? null : target.state();
    }

    static PayloadTarget findPayloadTarget(LocalPlayer player) {
        return findPayloadTarget(player, true);
    }

    static PayloadTarget findBreakTarget(LocalPlayer player) {
        return findPayloadTarget(player, false);
    }

    static PayloadTarget findContextTarget(LocalPlayer player) {
        GridTarget target = trace(player, false, state ->
                state != null && !state.isAir()
                        && !ContextInteractionRegistry.getBlockRules(
                                state.getBlock()).isEmpty(), true);
        if (target == null) return null;
        return new PayloadTarget(target.group(), target.cell(),
                target.visualCell(), target.state(), target.hit().face(),
                target.worldHit(), target.hit().distance());
    }

    private static PayloadTarget findPayloadTarget(LocalPlayer player,
            boolean interactiveOnly) {
        // Interaction follows the authored local 1x1 cell, exactly like the
        // construction grid. Tiny vanilla selection shapes (buttons, readers,
        // levers) are a visual/collision detail and must not make a rotated
        // payload effectively impossible to click.
        GridTarget target = trace(player, false, state ->
                state != null && !state.isAir()
                        && (!interactiveOnly || interactive(state)), true);
        if (target == null) return null;
        return new PayloadTarget(target.group(), target.cell(),
                target.visualCell(), target.state(), target.hit().face(),
                target.worldHit(), target.hit().distance());
    }

    static Target findTarget(LocalPlayer player) {
        GridTarget target = trace(player, false, state -> true);
        if (target == null) return null;
        return new Target(target.group(), target.cell(),
                target.hit().face(), target.worldHit());
    }

    /**
     * Raycasts transformed construction exactly like a small local block world:
     * project the ray once into each group's frame, walk logical cells with a
     * 3-D DDA, and only then test the authored block shape when requested.
     *
     * Cost follows ray length (<= 32 blocks), not group size. More importantly,
     * no world-space proxy/AABB is allowed to decide which local cell was hit.
     */
    private static GridTarget trace(LocalPlayer player, boolean payloadShape,
            java.util.function.Predicate<BlockState> accepted) {
        return trace(player, payloadShape, accepted, false);
    }

    private static GridTarget trace(LocalPlayer player, boolean payloadShape,
            java.util.function.Predicate<BlockState> accepted,
            boolean preferShiftedFixtures) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || minecraft.level == null) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 worldRay = player.getViewVector(1.0F).normalize();
        double limit = MAX_DISTANCE;
        BlockPos vanillaBlocker = null;
        double vanillaDistance = Double.POSITIVE_INFINITY;

        HitResult vanilla = minecraft.hitResult;
        if (vanilla instanceof BlockHitResult blockHit
                && vanilla.getType() == HitResult.Type.BLOCK
                && !minecraft.level.getBlockState(blockHit.getBlockPos()).is(
                        com.bl4ues.scpclassifieddirective.facility.transform
                                .TransformConstructionModule.getProxy())) {
            vanillaBlocker = blockHit.getBlockPos();
            vanillaDistance = eye.distanceTo(blockHit.getLocation());
        }

        GridTarget best = null;
        double bestDistance = limit + 1.0D;
        for (TransformGroup group : TransformConstructionClientState.groups(
                minecraft.level.dimension().location())) {
            Vec3 localEye = TransformMath.worldToLocal(group.origin(), eye,
                    group.rotationX(), group.rotationY(), group.rotationZ());
            Vec3 localRay = TransformMath.inverseRotate(worldRay,
                    group.rotationX(), group.rotationY(), group.rotationZ())
                    .normalize();
            GridTarget candidate = traceGroup(group, localEye, localRay,
                    Math.min(limit, bestDistance), payloadShape, accepted, eye,
                    worldRay, preferShiftedFixtures);
            if (candidate == null) continue;

            // Main-level geometry occludes unrelated transformed grids, but a
            // transformed local cell is allowed to coexist with that exact
            // vanilla world cell. This is the important sub-level rule: do not
            // let the parent's axis-aligned BlockPos steal a hit that belongs
            // to the logical grid occupying the same physical space.
            if (vanillaBlocker != null
                    && candidate.hit().distance() > vanillaDistance + 1.0E-4D
                    && (!TransformConstructionClientState
                            .groupTouchesWorldCell(group.id(), vanillaBlocker)
                    || candidate.hit().distance() > vanillaDistance + 1.80D)) {
                continue;
            }
            if (candidate.hit().distance() < bestDistance) {
                bestDistance = candidate.hit().distance();
                best = candidate;
            }
        }
        return best;
    }

    private static GridTarget traceGroup(TransformGroup group, Vec3 localEye,
            Vec3 localRay, double limit, boolean payloadShape,
            java.util.function.Predicate<BlockState> accepted,
            Vec3 worldEye, Vec3 worldRay, boolean preferShiftedFixtures) {
        TransformGroup.GridPos cell = nearestCell(localEye);
        double travelled = 0.0D;

        // A 32-block unit ray crosses at most ~56 cell planes diagonally.
        // 128 leaves plenty of numerical headroom without an unbounded walk.
        for (int step = 0; step < 128 && travelled <= limit + EPSILON; step++) {
            VisualPayload visual = payloadAtVisualCell(group, cell,
                    accepted, preferShiftedFixtures);
            if (visual != null) {
                Hit hit = payloadShape
                        ? intersectState(localEye, localRay, cell,
                                visual.state(), false)
                        : intersect(localEye, localRay, cell);
                if (hit != null && hit.distance() >= -EPSILON
                        && hit.distance() <= limit + EPSILON) {
                    Vec3 worldHit = worldEye.add(
                            worldRay.scale(hit.distance()));
                    return new GridTarget(group, visual.anchor(), cell,
                            visual.state(), hit, worldHit);
                }
            }

            double tx = nextBoundary(localEye.x, localRay.x, cell.x());
            double ty = nextBoundary(localEye.y, localRay.y, cell.y());
            double tz = nextBoundary(localEye.z, localRay.z, cell.z());
            double next = Math.min(tx, Math.min(ty, tz));
            if (!Double.isFinite(next) || next > limit + EPSILON) break;

            int dx = Math.abs(tx - next) <= 1.0E-7D
                    ? (localRay.x >= 0.0D ? 1 : -1) : 0;
            int dy = Math.abs(ty - next) <= 1.0E-7D
                    ? (localRay.y >= 0.0D ? 1 : -1) : 0;
            int dz = Math.abs(tz - next) <= 1.0E-7D
                    ? (localRay.z >= 0.0D ? 1 : -1) : 0;
            if (dx == 0 && dy == 0 && dz == 0) break;
            cell = cell.offset(dx, dy, dz);
            travelled = Math.max(travelled + EPSILON, next);
        }
        return null;
    }

    private static VisualPayload payloadAtVisualCell(
            TransformGroup group, TransformGroup.GridPos visualCell,
            java.util.function.Predicate<BlockState> accepted,
            boolean preferShiftedFixtures) {
        if (preferShiftedFixtures) {
            VisualPayload shifted = shiftedPayloadAtVisualCell(
                    group, visualCell, accepted);
            if (shifted != null) return shifted;
        }

        BlockState direct = group.cells().get(visualCell);
        if (direct != null && accepted.test(direct)
                && TransformWallFixturePlacement.visualShift(direct) == null) {
            return new VisualPayload(visualCell, direct);
        }

        return preferShiftedFixtures ? null
                : shiftedPayloadAtVisualCell(group, visualCell, accepted);
    }

    private static VisualPayload shiftedPayloadAtVisualCell(
            TransformGroup group, TransformGroup.GridPos visualCell,
            java.util.function.Predicate<BlockState> accepted) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            TransformGroup.GridPos anchor = visualCell.offset(
                    -direction.getStepX(), 0, -direction.getStepZ());
            BlockState state = group.cells().get(anchor);
            if (state == null || !accepted.test(state)) continue;
            if (TransformWallFixturePlacement.visualCell(anchor, state)
                    .equals(visualCell)) {
                return new VisualPayload(anchor, state);
            }
        }
        return null;
    }

    private static double nextBoundary(double origin, double direction,
            int cell) {
        if (Math.abs(direction) < EPSILON) {
            return Double.POSITIVE_INFINITY;
        }
        double boundary = direction > 0.0D ? cell + 0.5D : cell - 0.5D;
        double value = (boundary - origin) / direction;
        if (value < -EPSILON) return Double.POSITIVE_INFINITY;
        return Math.max(0.0D, value);
    }

    private static boolean interactive(BlockState state) {
        return state != null && (state.getBlock() instanceof ButtonBlock
                || state.getBlock() instanceof LeverBlock
                || TransformWallFixturePlacement.isDoorButton(state)
                || KeycardReaderLevels.describe(state) != null
                || FacilityModule.isFacilityDoor(state));
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

    private record VisualPayload(TransformGroup.GridPos anchor,
            BlockState state) {
    }

    private record GridTarget(TransformGroup group,
            TransformGroup.GridPos cell, TransformGroup.GridPos visualCell,
            BlockState state, Hit hit, Vec3 worldHit) {
    }

    static record Target(TransformGroup group, TransformGroup.GridPos source,
            Direction face, Vec3 worldHit) {
        TransformGroup.GridPos adjacentCell() {
            BlockState sourceState = group.cells().get(source);
            if (sourceState == null || sourceState.isAir()) return source;
            // The fixture's saved controller cell may sit beside its rendered
            // panel. A click on its visible face must extend the VISIBLE grid
            // cell, never the hidden controller address.
            TransformGroup.GridPos visual =
                    TransformWallFixturePlacement.visualCell(source, sourceState);
            return visual.offset(face.getStepX(), face.getStepY(),
                    face.getStepZ());
        }
    }

    static record PayloadTarget(TransformGroup group,
            TransformGroup.GridPos cell, TransformGroup.GridPos visualCell,
            BlockState state, Direction face, Vec3 worldHit,
            double distance) {
    }
}
