package net.mcreator.scpadditions.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

/**
 * Supplies the existing SCP-173 movement code with a stable, collision-driven
 * route when the direct chase line is blocked. It does not move the statue and
 * therefore does not create a second movement authority; it only replaces the
 * coarse vanilla path nodes stored by Scp173Entity with centered local routes.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class Scp173SmartRoutePlanner {
    private static final double GRID_STEP = 0.25D;
    private static final double SEARCH_RADIUS = 16.0D;
    private static final int SEARCH_RADIUS_GRID =
            (int) Math.round(SEARCH_RADIUS / GRID_STEP);
    private static final int MAX_SEARCH_NODES = 5200;
    private static final int ROUTE_LIFETIME_TICKS = 36;
    private static final int ENTITY_ROUTE_LIFETIME_TICKS = 60;
    private static final int ENTITY_REPLAN_DELAY_TICKS = 20;
    private static final int STALL_REPLAN_TICKS = 2;
    private static final double TARGET_SHIFT_SQR = 2.0D * 2.0D;
    private static final double DIRECT_STEP = 1.20D;
    private static final double STOP_DISTANCE = 0.72D;
    private static final double REACHED_NODE_SQR = 0.55D * 0.55D;
    private static final double SWEEP_SAMPLE_DISTANCE = 0.03125D;
    private static final double CORRIDOR_SAMPLE_DISTANCE = 0.0625D;
    private static final double COLLISION_EPSILON = 1.0E-6D;
    private static final double CLEARANCE_MARGIN = 0.08D;

    private static final int[][] DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private static final TagKey<Block> SOFT_OBSTACLES = TagKey.create(
            Registries.BLOCK, new ResourceLocation(ScpAdditionsMod.MODID,
                    "scp_173_soft_obstacles"));
    private static final TagKey<Block> HARD_OBSTACLES = TagKey.create(
            Registries.BLOCK, new ResourceLocation(ScpAdditionsMod.MODID,
                    "scp_173_hard_obstacles"));

    private static final Map<UUID, PlannerState> STATES = new HashMap<>();

    private static final Field REMEMBERED_ROUTE_NODES = field(
            "rememberedRouteNodes");
    private static final Field REMEMBERED_ROUTE_INDEX = field(
            "rememberedRouteIndex");
    private static final Field REMEMBERED_ROUTE_TARGET = field(
            "rememberedRouteTarget");
    private static final Field REMEMBERED_ROUTE_TARGET_POSITION = field(
            "rememberedRouteTargetPosition");
    private static final Field REMEMBERED_ROUTE_EXPIRES_TICK = field(
            "rememberedRouteExpiresTick");
    private static final Field NEXT_ROUTE_REPLAN_TICK = field(
            "nextRouteReplanTick");
    private static final Field ROUTE_STALL_ATTEMPTS = field(
            "routeStallAttempts");
    private static final Field REMEMBERED_ROUTE_ENDPOINT_DISTANCE_SQR = field(
            "rememberedRouteEndpointDistanceSqr");
    private static boolean reflectionWarningLogged;

    private Scp173SmartRoutePlanner() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLevelTickStart(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START
                || !(event.level instanceof ServerLevel level)
                || !ScpAdditionsModulesConfig.get().scp173.enabled) {
            return;
        }

        Set<UUID> present = new HashSet<>();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Scp173Entity statue)
                    || !statue.isAlive() || statue.isRemoved()) {
                continue;
            }
            present.add(statue.getUUID());
            if (!statue.isActivated()) continue;

            LivingEntity target = statue.getTarget();
            if (!isValidTarget(target)
                    || Math.abs(target.getY() - statue.getY()) > 1.25D) {
                STATES.remove(statue.getUUID());
                continue;
            }

            Vec3 start = statue.position();
            Vec3 direct = directStep(start, target.position());
            if (direct.lengthSqr() <= 1.0E-6D
                    || isClearSweep(level, statue, start, direct,
                    SWEEP_SAMPLE_DISTANCE)) {
                STATES.remove(statue.getUUID());
                continue;
            }

            PlannerState state = STATES.computeIfAbsent(statue.getUUID(),
                    ignored -> new PlannerState());
            state.dimension = level.dimension();
            state.updateMotion(start);

            boolean targetShifted = state.targetPosition == null
                    || horizontalDistanceSqr(state.targetPosition,
                    target.position()) >= TARGET_SHIFT_SQR;
            boolean expired = level.getGameTime() > state.expiresGameTime;
            List<Vec3> remaining = remainingRoute(level, statue,
                    state.route, start);
            boolean blocked = remaining.isEmpty();
            boolean stalled = state.stallTicks >= STALL_REPLAN_TICKS;

            if (state.targetId == null
                    || !state.targetId.equals(target.getUUID())
                    || targetShifted || expired || blocked || stalled) {
                List<Vec3> route = findRoute(level, statue, target);
                state.route.clear();
                state.route.addAll(route);
                state.targetId = target.getUUID();
                state.targetPosition = target.position();
                state.expiresGameTime = level.getGameTime()
                        + ROUTE_LIFETIME_TICKS;
                state.stallTicks = 0;
                remaining = remainingRoute(level, statue, state.route, start);
            }

            if (!remaining.isEmpty()) {
                injectRoute(statue, target, remaining);
            }
        }

        STATES.entrySet().removeIf(entry ->
                entry.getValue().dimension != null
                        && entry.getValue().dimension.equals(level.dimension())
                        && !present.contains(entry.getKey()));
    }

    private static Vec3 directStep(Vec3 start, Vec3 target) {
        Vec3 horizontal = new Vec3(target.x - start.x, 0.0D,
                target.z - start.z);
        double distance = horizontal.length();
        if (distance <= STOP_DISTANCE) return Vec3.ZERO;
        double travel = Math.min(DIRECT_STEP, distance - STOP_DISTANCE);
        return travel <= 0.001D ? Vec3.ZERO
                : horizontal.scale(travel / distance);
    }

    private static List<Vec3> remainingRoute(ServerLevel level,
            Scp173Entity statue, List<Vec3> route, Vec3 current) {
        if (route.isEmpty()) return Collections.emptyList();
        int first = 0;
        while (first < route.size()
                && horizontalDistanceSqr(current, route.get(first))
                <= REACHED_NODE_SQR) {
            first++;
        }
        if (first >= route.size()) return Collections.emptyList();

        Vec3 toFirst = route.get(first).subtract(current);
        if (!isClearSweep(level, statue, current, toFirst,
                CORRIDOR_SAMPLE_DISTANCE)) {
            return Collections.emptyList();
        }
        return List.copyOf(route.subList(first, route.size()));
    }

    private static List<Vec3> findRoute(ServerLevel level,
            Scp173Entity statue, LivingEntity target) {
        Vec3 start = statue.position();
        double baseY = start.y;
        int centerX = (int) Math.round(start.x / GRID_STEP);
        int centerZ = (int) Math.round(start.z / GRID_STEP);

        PriorityQueue<OpenNode> open = new PriorityQueue<>();
        Map<GridNode, Double> gScore = new HashMap<>();
        Map<GridNode, GridNode> parent = new HashMap<>();
        Set<GridNode> closed = new HashSet<>();

        for (GridNode seed : seedNodes(start)) {
            Vec3 position = seed.position(baseY);
            if (!canStandAt(level, statue, position)
                    || !isClearSweep(level, statue, start,
                    position.subtract(start), CORRIDOR_SAMPLE_DISTANCE)) {
                continue;
            }
            double cost = horizontalDistance(start, position);
            gScore.put(seed, cost);
            parent.put(seed, null);
            open.add(new OpenNode(seed, cost
                    + heuristic(position, target.position())));
        }
        if (open.isEmpty()) return Collections.emptyList();

        GridNode best = open.peek().node();
        double startHeuristic = heuristic(start, target.position());
        double bestHeuristic = startHeuristic;
        GridNode goal = null;
        int expanded = 0;

        while (!open.isEmpty() && expanded++ < MAX_SEARCH_NODES) {
            GridNode current = open.poll().node();
            if (!closed.add(current)) continue;
            Vec3 currentPosition = current.position(baseY);
            double currentHeuristic = heuristic(currentPosition,
                    target.position());
            if (currentHeuristic < bestHeuristic) {
                bestHeuristic = currentHeuristic;
                best = current;
            }

            if (currentHeuristic <= 1.10D
                    || (currentHeuristic <= 5.0D
                    && hasClearApproach(level, statue, currentPosition,
                    target.position()))) {
                goal = current;
                break;
            }

            for (int[] direction : DIRECTIONS) {
                GridNode neighbor = new GridNode(current.x()
                        + direction[0], current.z() + direction[1]);
                if (Math.abs(neighbor.x() - centerX) > SEARCH_RADIUS_GRID
                        || Math.abs(neighbor.z() - centerZ)
                        > SEARCH_RADIUS_GRID
                        || closed.contains(neighbor)) {
                    continue;
                }

                Vec3 neighborPosition = neighbor.position(baseY);
                if (!canStandAt(level, statue, neighborPosition)) continue;
                Vec3 edge = neighborPosition.subtract(currentPosition);
                if (!isClearSweep(level, statue, currentPosition, edge,
                        CORRIDOR_SAMPLE_DISTANCE)) {
                    continue;
                }

                double edgeCost = direction[0] != 0 && direction[1] != 0
                        ? Math.sqrt(2.0D) * GRID_STEP : GRID_STEP;
                double tentative = gScore.getOrDefault(current,
                        Double.POSITIVE_INFINITY) + edgeCost
                        + clearancePenalty(level, statue, neighborPosition);
                if (tentative + 1.0E-7D
                        >= gScore.getOrDefault(neighbor,
                        Double.POSITIVE_INFINITY)) {
                    continue;
                }

                gScore.put(neighbor, tentative);
                parent.put(neighbor, current);
                open.add(new OpenNode(neighbor, tentative
                        + heuristic(neighborPosition, target.position())));
            }
        }

        GridNode destination = goal != null ? goal : best;
        if (destination == null
                || bestHeuristic > startHeuristic - 0.75D) {
            return Collections.emptyList();
        }

        List<Vec3> reversed = new ArrayList<>();
        for (GridNode cursor = destination; cursor != null;
                cursor = parent.get(cursor)) {
            reversed.add(cursor.position(baseY));
        }
        Collections.reverse(reversed);
        while (!reversed.isEmpty()
                && horizontalDistanceSqr(start, reversed.get(0))
                <= REACHED_NODE_SQR) {
            reversed.remove(0);
        }
        return smoothRoute(level, statue, start, reversed);
    }

    private static List<GridNode> seedNodes(Vec3 start) {
        int floorX = Mth.floor(start.x / GRID_STEP);
        int floorZ = Mth.floor(start.z / GRID_STEP);
        List<GridNode> result = new ArrayList<>(List.of(
                new GridNode(floorX, floorZ),
                new GridNode(floorX + 1, floorZ),
                new GridNode(floorX, floorZ + 1),
                new GridNode(floorX + 1, floorZ + 1)));
        result.sort((first, second) -> Double.compare(
                horizontalDistanceSqr(first.position(start.y), start),
                horizontalDistanceSqr(second.position(start.y), start)));
        return result;
    }

    private static List<Vec3> smoothRoute(ServerLevel level,
            Scp173Entity statue, Vec3 start, List<Vec3> raw) {
        if (raw.isEmpty()) return raw;
        List<Vec3> smooth = new ArrayList<>();
        Vec3 anchor = start;
        int index = 0;
        while (index < raw.size()) {
            int chosen = index;
            for (int candidate = Math.min(raw.size() - 1, index + 24);
                    candidate > index; candidate--) {
                Vec3 delta = raw.get(candidate).subtract(anchor);
                if (isClearSweep(level, statue, anchor, delta,
                        CORRIDOR_SAMPLE_DISTANCE)
                        && hasClearanceSweep(level, statue, anchor, delta,
                        CLEARANCE_MARGIN)) {
                    chosen = candidate;
                    break;
                }
            }
            if (chosen == index) {
                for (int candidate = Math.min(raw.size() - 1, index + 12);
                        candidate > index; candidate--) {
                    Vec3 delta = raw.get(candidate).subtract(anchor);
                    if (isClearSweep(level, statue, anchor, delta,
                            CORRIDOR_SAMPLE_DISTANCE)) {
                        chosen = candidate;
                        break;
                    }
                }
            }
            Vec3 waypoint = raw.get(chosen);
            smooth.add(waypoint);
            anchor = waypoint;
            index = chosen + 1;
        }
        return List.copyOf(smooth);
    }

    private static boolean hasClearApproach(ServerLevel level,
            Scp173Entity statue, Vec3 start, Vec3 target) {
        Vec3 horizontal = new Vec3(target.x - start.x, 0.0D,
                target.z - start.z);
        double distance = horizontal.length();
        if (distance <= STOP_DISTANCE) return true;
        return isClearSweep(level, statue, start,
                horizontal.scale((distance - STOP_DISTANCE) / distance),
                CORRIDOR_SAMPLE_DISTANCE);
    }

    private static double clearancePenalty(ServerLevel level,
            Scp173Entity statue, Vec3 position) {
        AABB box = boxAt(statue, position);
        double penalty = 0.0D;
        if (hardCollisionVolume(level, statue,
                expandHorizontal(box, 0.14D)) > COLLISION_EPSILON) {
            penalty += 0.18D;
        }
        if (hardCollisionVolume(level, statue,
                expandHorizontal(box, CLEARANCE_MARGIN))
                > COLLISION_EPSILON) {
            penalty += 0.62D;
        }
        return penalty;
    }

    private static boolean canStandAt(ServerLevel level,
            Scp173Entity statue, Vec3 position) {
        AABB box = boxAt(statue, position);
        if (hardCollisionVolume(level, statue, box) > COLLISION_EPSILON) {
            return false;
        }
        if (statue.isInWater()) return true;

        double inset = Math.min(0.08D, statue.getBbWidth() * 0.15D);
        AABB support = new AABB(box.minX + inset, box.minY - 0.09D,
                box.minZ + inset, box.maxX - inset, box.minY + 0.01D,
                box.maxZ - inset);
        return hardCollisionVolume(level, statue, support)
                > COLLISION_EPSILON;
    }

    private static boolean hasClearanceSweep(Level level,
            Scp173Entity statue, Vec3 start, Vec3 movement,
            double margin) {
        double length = movement.length();
        if (length <= 0.001D) return true;
        AABB startBox = expandHorizontal(boxAt(statue, start), margin);
        int samples = Math.max(1,
                (int) Math.ceil(length / CORRIDOR_SAMPLE_DISTANCE));
        for (int sample = 1; sample <= samples; sample++) {
            AABB box = startBox.move(movement.scale(
                    sample / (double) samples));
            if (hardCollisionVolume(level, statue, box)
                    > COLLISION_EPSILON) {
                return false;
            }
        }
        return true;
    }

    private static boolean isClearSweep(Level level, Scp173Entity statue,
            Vec3 start, Vec3 movement, double sampleDistance) {
        double length = movement.length();
        if (length <= 0.001D) return true;
        AABB startBox = boxAt(statue, start);
        int samples = Math.max(1,
                (int) Math.ceil(length / sampleDistance));
        for (int sample = 1; sample <= samples; sample++) {
            AABB box = startBox.move(movement.scale(
                    sample / (double) samples));
            if (hardCollisionVolume(level, statue, box)
                    > COLLISION_EPSILON) {
                return false;
            }
        }
        return true;
    }

    private static AABB boxAt(Scp173Entity statue, Vec3 position) {
        return statue.getBoundingBox().move(position.x - statue.getX(),
                position.y - statue.getY(), position.z - statue.getZ());
    }

    private static AABB expandHorizontal(AABB box, double margin) {
        return new AABB(box.minX - margin, box.minY, box.minZ - margin,
                box.maxX + margin, box.maxY, box.maxZ + margin);
    }

    private static double hardCollisionVolume(Level level,
            Scp173Entity statue, AABB box) {
        int minX = Mth.floor(box.minX + COLLISION_EPSILON);
        int maxX = Mth.floor(box.maxX - COLLISION_EPSILON);
        int minY = Mth.floor(box.minY + COLLISION_EPSILON);
        int maxY = Mth.floor(box.maxY - COLLISION_EPSILON);
        int minZ = Mth.floor(box.minZ + COLLISION_EPSILON);
        int maxZ = Mth.floor(box.maxZ - COLLISION_EPSILON);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        CollisionContext context = CollisionContext.of(statue);
        double volume = 0.0D;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    VoxelShape shape = state.getCollisionShape(level, pos,
                            context);
                    if (shape.isEmpty()
                            || isSoftObstacle(level, pos, state, shape)) {
                        continue;
                    }
                    for (AABB part : shape.toAabbs()) {
                        volume += intersectionVolume(box,
                                part.move(x, y, z));
                    }
                }
            }
        }
        return volume;
    }

    private static boolean isSoftObstacle(Level level, BlockPos pos,
            BlockState state, VoxelShape shape) {
        if (state.is(HARD_OBSTACLES)) return false;
        if (state.is(SOFT_OBSTACLES)) return true;
        if (state.getDestroySpeed(level, pos) < 0.0F || shape.isEmpty()) {
            return false;
        }

        AABB bounds = shape.bounds();
        double widthX = bounds.getXsize();
        double height = bounds.getYsize();
        double widthZ = bounds.getZsize();
        double volume = 0.0D;
        for (AABB part : shape.toAabbs()) {
            volume += part.getXsize() * part.getYsize()
                    * part.getZsize();
        }
        if (height <= 0.25D) return true;
        if (height > 1.25D) return false;
        boolean barrierPlane = height >= 0.75D
                && (widthX >= 0.90D || widthZ >= 0.90D);
        if (barrierPlane) return false;
        return volume <= 0.32D && widthX <= 0.80D && widthZ <= 0.80D;
    }

    private static double intersectionVolume(AABB first, AABB second) {
        double x = Math.max(0.0D, Math.min(first.maxX, second.maxX)
                - Math.max(first.minX, second.minX));
        double y = Math.max(0.0D, Math.min(first.maxY, second.maxY)
                - Math.max(first.minY, second.minY));
        double z = Math.max(0.0D, Math.min(first.maxZ, second.maxZ)
                - Math.max(first.minZ, second.minZ));
        return x * y * z;
    }

    @SuppressWarnings("unchecked")
    private static void injectRoute(Scp173Entity statue,
            LivingEntity target, List<Vec3> route) {
        if (!reflectionAvailable()) return;
        try {
            List<Vec3> entityRoute = (List<Vec3>)
                    REMEMBERED_ROUTE_NODES.get(statue);
            entityRoute.clear();
            entityRoute.addAll(route);
            REMEMBERED_ROUTE_INDEX.setInt(statue, 0);
            REMEMBERED_ROUTE_TARGET.set(statue, target.getUUID());
            REMEMBERED_ROUTE_TARGET_POSITION.set(statue, target.position());
            REMEMBERED_ROUTE_EXPIRES_TICK.setInt(statue,
                    statue.tickCount + ENTITY_ROUTE_LIFETIME_TICKS);
            NEXT_ROUTE_REPLAN_TICK.setInt(statue,
                    statue.tickCount + ENTITY_REPLAN_DELAY_TICKS);
            ROUTE_STALL_ATTEMPTS.setInt(statue, 0);
            REMEMBERED_ROUTE_ENDPOINT_DISTANCE_SQR.setDouble(statue,
                    horizontalDistanceSqr(route.get(route.size() - 1),
                            target.position()));
        } catch (ReflectiveOperationException exception) {
            warnReflection(exception);
        }
    }

    private static boolean reflectionAvailable() {
        return REMEMBERED_ROUTE_NODES != null
                && REMEMBERED_ROUTE_INDEX != null
                && REMEMBERED_ROUTE_TARGET != null
                && REMEMBERED_ROUTE_TARGET_POSITION != null
                && REMEMBERED_ROUTE_EXPIRES_TICK != null
                && NEXT_ROUTE_REPLAN_TICK != null
                && ROUTE_STALL_ATTEMPTS != null
                && REMEMBERED_ROUTE_ENDPOINT_DISTANCE_SQR != null;
    }

    private static Field field(String name) {
        try {
            Field field = Scp173Entity.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static void warnReflection(Exception exception) {
        if (reflectionWarningLogged) return;
        reflectionWarningLogged = true;
        ScpAdditionsMod.LOGGER.warn(
                "SCP-173 smart route planner lost internal access",
                exception);
    }

    private static boolean isValidTarget(LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return false;
        }
        return !(target instanceof Player player)
                || (!player.isCreative() && !player.isSpectator());
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        return Math.sqrt(horizontalDistanceSqr(first, second));
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    private static double heuristic(Vec3 position, Vec3 target) {
        return horizontalDistance(position, target);
    }

    private record GridNode(int x, int z) {
        private Vec3 position(double y) {
            return new Vec3(x * GRID_STEP, y, z * GRID_STEP);
        }
    }

    private record OpenNode(GridNode node, double score)
            implements Comparable<OpenNode> {
        @Override
        public int compareTo(OpenNode other) {
            return Double.compare(score, other.score);
        }
    }

    private static final class PlannerState {
        private ResourceKey<Level> dimension;
        private final List<Vec3> route = new ArrayList<>();
        private UUID targetId;
        private Vec3 targetPosition;
        private Vec3 lastPosition;
        private long expiresGameTime = Long.MIN_VALUE;
        private int stallTicks;

        private void updateMotion(Vec3 position) {
            if (lastPosition != null
                    && horizontalDistanceSqr(lastPosition, position)
                    <= 0.02D * 0.02D) {
                stallTicks++;
            } else {
                stallTicks = 0;
            }
            lastPosition = position;
        }
    }
}
