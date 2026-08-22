package com.bl4ues.scpclassifieddirective.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
 * One server-side movement authority for SCP-173. The entity's own movement is
 * accepted when it is legal. A stalled movement is repaired with a local route
 * built from the real collision geometry instead of Minecraft's block-cell
 * classification, which is frequently too coarse for multi-block props,
 * checkpoints, gates, and modded decorative blocks.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID)
public final class Scp173MovementController {
    private static final double SEARCH_RANGE = 96.0D;
    private static final double SEARCH_RANGE_SQR = SEARCH_RANGE * SEARCH_RANGE;
    private static final double MOVEMENT_EPSILON_SQR = 0.001D * 0.001D;
    private static final double MIN_USEFUL_STEP = 0.035D;
    private static final double DIRECT_STEP = 1.20D;
    private static final double BLINK_STEP = 0.95D;
    private static final double AUTOMATIC_BLINK_DISTANCE = 6.0D;
    private static final double STOP_DISTANCE = 0.72D;
    private static final double NODE_REACHED_SQR = 0.38D * 0.38D;
    private static final double SWEEP_SAMPLE_DISTANCE = 0.03125D;
    private static final double CORRIDOR_SAMPLE_DISTANCE = 0.125D;
    private static final double GRID_STEP = 0.50D;
    private static final double LOCAL_SEARCH_RADIUS = 24.0D;
    private static final int LOCAL_SEARCH_RADIUS_GRID =
            (int) Math.round(LOCAL_SEARCH_RADIUS / GRID_STEP);
    private static final int MAX_LOCAL_SEARCH_NODES = 2600;
    private static final int MAX_PATH_NODES = 128;
    private static final int ROUTE_LIFETIME_TICKS = 40;
    private static final int ROUTE_REPLAN_DELAY_TICKS = 8;
    private static final double ROUTE_TARGET_SHIFT_SQR = 2.0D * 2.0D;
    private static final double MAX_UP_STEP = 1.05D;
    private static final double MAX_DOWN_STEP = 1.00D;
    private static final double COLLISION_EPSILON = 1.0E-6D;
    private static final double PENETRATION_EPSILON = 1.0E-5D;

    private static final int[][] GRID_DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    /**
     * Datapacks may explicitly classify blocks. The generic shape heuristic is
     * used only when neither tag contains the block.
     */
    private static final TagKey<Block> SOFT_OBSTACLES = TagKey.create(
            Registries.BLOCK, new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                    "scp_173_soft_obstacles"));
    private static final TagKey<Block> HARD_OBSTACLES = TagKey.create(
            Registries.BLOCK, new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                    "scp_173_hard_obstacles"));

    private static final Map<UUID, MovementState> STATES = new HashMap<>();

    private static final Method OBSERVATION_LOCKED = method(
            "isObservationLocked");
    private static final Method HARD_STOP = method("hardStopLocalMovement");
    private static final Method SET_MANUAL_YAW = method(
            "setManualYaw", float.class);
    private static final Method CLEAR_STRATEGIC_ROUTE = method(
            "clearStrategicRoute");
    private static final Method CONSUME_AUTOMATIC_BLINK = method(
            "consumeAutomaticBlinkTravel", Player.class, double.class);
    private static final Field AUTOMATIC_BLINK_REMAINING = field(
            "automaticBlinkTravelRemaining");
    private static final Field SCRAPING = field("SCRAPING");
    private static boolean reflectionWarningLogged;

    private Scp173MovementController() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLevelTickStart(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START
                || !(event.level instanceof ServerLevel level)) {
            return;
        }

        Set<UUID> present = new HashSet<>();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Scp173Entity statue)
                    || !statue.isAlive() || statue.isRemoved()) {
                continue;
            }

            present.add(statue.getUUID());
            MovementState state = STATES.computeIfAbsent(statue.getUUID(),
                    ignored -> new MovementState());
            state.dimension = level.dimension();
            state.snapshot = new Snapshot(statue.getX(), statue.getY(),
                    statue.getZ(), statue.getYRot(),
                    isObservationLocked(statue));

            if (statue.isActivated()
                    && ScpClassifiedDirectiveModulesConfig.get().scp173.enabled) {
                prioritizeBestTarget(level, statue);
            }
        }

        STATES.entrySet().removeIf(entry ->
                entry.getValue().dimension != null
                        && entry.getValue().dimension.equals(level.dimension())
                        && !present.contains(entry.getKey()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLevelTickEnd(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)) {
            return;
        }

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Scp173Entity statue) {
                validateAndRepair(level, statue);
            }
        }
    }

    /** Called from the blink packet before SCP-173 handles that blink. */
    public static void prioritizeBlinkingPlayer(ServerPlayer player) {
        if (!isValidPlayer(player)
                || !ScpClassifiedDirectiveModulesConfig.get().scp173.enabled) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(SEARCH_RANGE);
        for (Scp173Entity statue : player.serverLevel().getEntitiesOfClass(
                Scp173Entity.class, area,
                entity -> entity.isAlive() && entity.isActivated()
                        && entity.distanceToSqr(player)
                        <= SEARCH_RANGE_SQR)) {
            setTarget(statue, player);
        }
    }

    private static void validateAndRepair(ServerLevel level,
            Scp173Entity statue) {
        MovementState state = STATES.get(statue.getUUID());
        Snapshot snapshot = state == null ? null : state.snapshot;
        if (snapshot == null || !statue.isAlive() || statue.isRemoved()) {
            return;
        }

        if (!ScpClassifiedDirectiveModulesConfig.get().scp173.enabled
                || !statue.isActivated()) {
            setScraping(statue, false);
            state.clearRoute();
            return;
        }

        Vec3 start = snapshot.position();
        Vec3 movement = statue.position().subtract(start);
        boolean moved = movement.lengthSqr() > MOVEMENT_EPSILON_SQR;
        boolean observed = snapshot.observedAtStart()
                || isObservationLocked(statue);

        if (observed) {
            restore(statue, snapshot);
            return;
        }

        if (moved && !isClearSweep(level, statue, start, movement,
                SWEEP_SAMPLE_DISTANCE)) {
            restore(statue, snapshot);
            moved = false;
        }

        // Native movement remains authoritative when it produced one legal,
        // server-side displacement. The repair path never adds a second step.
        if (moved) {
            state.advanceReachedNodes(statue.position());
            return;
        }

        LivingEntity target = prioritizeBestTarget(level, statue);
        if (target == null) {
            setScraping(statue, false);
            stopAtCurrentPosition(statue);
            state.clearRoute();
            return;
        }

        double maximumStep = maximumStep(statue, target);
        if (maximumStep <= MIN_USEFUL_STEP) {
            setScraping(statue, false);
            stopAtCurrentPosition(statue);
            return;
        }

        Vec3 step = chooseRepairStep(level, statue, target, state,
                maximumStep);
        if (!isUseful(step)) {
            setScraping(statue, false);
            stopAtCurrentPosition(statue);
            return;
        }

        Vec3 before = statue.position();
        applyStep(level, statue, target, step);
        double travelled = statue.position().distanceTo(before);
        if (travelled <= MIN_USEFUL_STEP) {
            setScraping(statue, false);
            stopAtCurrentPosition(statue);
            return;
        }

        setScraping(statue, true);
        consumeBlinkBudget(statue, target, travelled);
        state.advanceReachedNodes(statue.position());
    }

    /** Players always outrank configured non-player targets. */
    private static LivingEntity prioritizeBestTarget(ServerLevel level,
            Scp173Entity statue) {
        Player nearestPlayer = null;
        double nearestPlayerDistance = Double.MAX_VALUE;
        AABB area = statue.getBoundingBox().inflate(SEARCH_RANGE);

        for (Player player : level.getEntitiesOfClass(Player.class, area,
                Scp173MovementController::isValidPlayer)) {
            double distance = statue.distanceToSqr(player);
            if (distance <= SEARCH_RANGE_SQR
                    && distance < nearestPlayerDistance) {
                nearestPlayerDistance = distance;
                nearestPlayer = player;
            }
        }

        if (nearestPlayer != null) {
            setTarget(statue, nearestPlayer);
            return nearestPlayer;
        }

        LivingEntity current = statue.getTarget();
        if (isValidTarget(current)) return current;

        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class, area,
                entity -> entity != statue && isValidTarget(entity))) {
            double distance = statue.distanceToSqr(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        if (best != null) setTarget(statue, best);
        return best;
    }

    private static void setTarget(Scp173Entity statue, LivingEntity target) {
        LivingEntity previous = statue.getTarget();
        if (previous != null && previous.getUUID().equals(target.getUUID())) {
            return;
        }
        statue.setTarget(target);
        statue.getNavigation().stop();
        clearStrategicRoute(statue);
        MovementState state = STATES.get(statue.getUUID());
        if (state != null) state.clearRoute();
    }

    private static Vec3 chooseRepairStep(ServerLevel level,
            Scp173Entity statue, LivingEntity target, MovementState state,
            double maximumStep) {
        Vec3 direct = directStep(statue, target, maximumStep);
        if (isUseful(direct)
                && isClearSweep(level, statue, statue.position(), direct,
                SWEEP_SAMPLE_DISTANCE)) {
            state.clearRoute();
            return direct;
        }

        prepareRouteForTarget(state, target);
        Vec3 cached = cachedRouteStep(level, statue, state, maximumStep);
        if (isUseful(cached)) return cached;

        long gameTime = level.getGameTime();
        boolean targetShifted = state.routeTargetPosition == null
                || state.routeTargetPosition.distanceToSqr(target.position())
                >= ROUTE_TARGET_SHIFT_SQR;
        if (gameTime >= state.nextRouteSearchGameTime
                && (state.route.isEmpty()
                || gameTime > state.routeExpiresGameTime
                || targetShifted)) {
            List<Vec3> localRoute = findLocalCollisionRoute(level, statue,
                    target);
            state.nextRouteSearchGameTime = gameTime
                    + ROUTE_REPLAN_DELAY_TICKS;
            if (!localRoute.isEmpty()) {
                state.route.clear();
                state.route.addAll(localRoute);
                state.routeIndex = 0;
                state.routeTarget = target.getUUID();
                state.routeTargetPosition = target.position();
                state.routeExpiresGameTime = gameTime
                        + ROUTE_LIFETIME_TICKS;
                cached = cachedRouteStep(level, statue, state, maximumStep);
                if (isUseful(cached)) return cached;
            }
        }

        // Keep vanilla navigation as a vertical/stair fallback. The local route
        // intentionally stays on one floor because facility gates and props are
        // horizontal obstacles, while vanilla already understands stairs.
        Path vanillaPath = statue.getNavigation().createPath(target, 0);
        Vec3 vanillaStep = firstVanillaPathStep(level, statue, vanillaPath,
                maximumStep);
        if (isUseful(vanillaStep)) return vanillaStep;

        // A closed door or a completely sealed structure still has a useful
        // staging point. Advance only through the collision-free segment.
        return largestClearStep(level, statue, direct,
                SWEEP_SAMPLE_DISTANCE);
    }

    private static Vec3 directStep(Scp173Entity statue,
            LivingEntity target, double maximumStep) {
        Vec3 delta = target.position().subtract(statue.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        double distance = horizontal.length();
        if (distance <= STOP_DISTANCE) return Vec3.ZERO;
        double travel = Math.min(maximumStep, distance - STOP_DISTANCE);
        if (travel <= MIN_USEFUL_STEP) return Vec3.ZERO;
        return horizontal.scale(1.0D / distance).scale(travel);
    }

    private static void prepareRouteForTarget(MovementState state,
            LivingEntity target) {
        if (target.getUUID().equals(state.routeTarget)) return;
        state.clearRoute();
        state.routeTarget = target.getUUID();
        state.routeTargetPosition = target.position();
    }

    private static Vec3 cachedRouteStep(ServerLevel level,
            Scp173Entity statue, MovementState state, double maximumStep) {
        state.advanceReachedNodes(statue.position());
        if (state.routeIndex >= state.route.size()) return Vec3.ZERO;

        // Skip unnecessary grid corners whenever the real collision geometry
        // allows a straighter segment. This prevents a visible left-right weave.
        int chosenIndex = state.routeIndex;
        int farthest = Math.min(state.route.size() - 1,
                state.routeIndex + 8);
        for (int index = farthest; index > state.routeIndex; index--) {
            Vec3 delta = state.route.get(index).subtract(statue.position());
            if (isClearSweep(level, statue, statue.position(), delta,
                    CORRIDOR_SAMPLE_DISTANCE)) {
                chosenIndex = index;
                break;
            }
        }

        Vec3 waypoint = state.route.get(chosenIndex);
        Vec3 step = stepTowardWaypoint(level, statue, waypoint,
                maximumStep);
        if (!isUseful(step)) {
            state.clearRoute();
            return Vec3.ZERO;
        }
        state.routeIndex = chosenIndex;
        return step;
    }

    /**
     * Fine-grid A* using actual AABB/voxel intersections. Unlike vanilla's
     * block-node evaluator, a half-cell opening through a multi-block model is
     * represented as an opening rather than a completely blocked block.
     */
    private static List<Vec3> findLocalCollisionRoute(ServerLevel level,
            Scp173Entity statue, LivingEntity target) {
        Vec3 start = statue.position();
        double baseY = start.y;
        int centerX2 = (int) Math.round(start.x * 2.0D);
        int centerZ2 = (int) Math.round(start.z * 2.0D);

        PriorityQueue<OpenNode> open = new PriorityQueue<>();
        Map<GridNode, Double> gScore = new HashMap<>();
        Map<GridNode, GridNode> parent = new HashMap<>();
        Set<GridNode> closed = new HashSet<>();

        for (GridNode seed : seedNodes(start)) {
            Vec3 seedPosition = seed.position(baseY);
            Vec3 toSeed = seedPosition.subtract(start);
            if (!canStandAt(level, statue, seedPosition)
                    || !isClearSweep(level, statue, start, toSeed,
                    CORRIDOR_SAMPLE_DISTANCE)) {
                continue;
            }
            double g = horizontalDistance(start, seedPosition);
            gScore.put(seed, g);
            parent.put(seed, null);
            open.add(new OpenNode(seed, g + heuristic(seedPosition,
                    target.position())));
        }

        if (open.isEmpty()) return Collections.emptyList();

        GridNode best = open.peek().node();
        double startHeuristic = heuristic(start, target.position());
        double bestHeuristic = startHeuristic;
        GridNode goal = null;
        int expanded = 0;

        while (!open.isEmpty() && expanded++ < MAX_LOCAL_SEARCH_NODES) {
            OpenNode queued = open.poll();
            GridNode current = queued.node();
            if (!closed.add(current)) continue;

            Vec3 currentPosition = current.position(baseY);
            double currentHeuristic = heuristic(currentPosition,
                    target.position());
            if (currentHeuristic < bestHeuristic) {
                bestHeuristic = currentHeuristic;
                best = current;
            }

            if (currentHeuristic <= 1.25D
                    || (currentHeuristic <= 4.0D
                    && hasClearApproachFrom(level, statue, currentPosition,
                    target))) {
                goal = current;
                break;
            }

            for (int[] direction : GRID_DIRECTIONS) {
                GridNode neighbor = new GridNode(current.x2()
                        + direction[0], current.z2() + direction[1]);
                if (Math.abs(neighbor.x2() - centerX2)
                        > LOCAL_SEARCH_RADIUS_GRID
                        || Math.abs(neighbor.z2() - centerZ2)
                        > LOCAL_SEARCH_RADIUS_GRID
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
                        Double.POSITIVE_INFINITY) + edgeCost;
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
                || bestHeuristic > startHeuristic - 0.50D) {
            return Collections.emptyList();
        }

        List<Vec3> reversed = new ArrayList<>();
        GridNode cursor = destination;
        while (cursor != null) {
            reversed.add(cursor.position(baseY));
            cursor = parent.get(cursor);
        }
        Collections.reverse(reversed);
        while (!reversed.isEmpty()
                && reversed.get(0).distanceToSqr(start)
                <= NODE_REACHED_SQR) {
            reversed.remove(0);
        }
        return smoothRoute(level, statue, start, reversed);
    }

    private static List<GridNode> seedNodes(Vec3 start) {
        int floorX = Mth.floor(start.x * 2.0D);
        int floorZ = Mth.floor(start.z * 2.0D);
        Set<GridNode> unique = new HashSet<>();
        unique.add(new GridNode(floorX, floorZ));
        unique.add(new GridNode(floorX + 1, floorZ));
        unique.add(new GridNode(floorX, floorZ + 1));
        unique.add(new GridNode(floorX + 1, floorZ + 1));
        List<GridNode> result = new ArrayList<>(unique);
        result.sort((first, second) -> Double.compare(
                first.position(start.y).distanceToSqr(start),
                second.position(start.y).distanceToSqr(start)));
        return result;
    }

    private static List<Vec3> smoothRoute(ServerLevel level,
            Scp173Entity statue, Vec3 start, List<Vec3> raw) {
        if (raw.size() < 2) return raw;
        List<Vec3> smooth = new ArrayList<>();
        Vec3 anchor = start;
        int index = 0;
        while (index < raw.size()) {
            int chosen = index;
            for (int candidate = Math.min(raw.size() - 1, index + 12);
                    candidate > index; candidate--) {
                Vec3 delta = raw.get(candidate).subtract(anchor);
                if (isClearSweep(level, statue, anchor, delta,
                        CORRIDOR_SAMPLE_DISTANCE)) {
                    chosen = candidate;
                    break;
                }
            }
            Vec3 waypoint = raw.get(chosen);
            smooth.add(waypoint);
            anchor = waypoint;
            index = chosen + 1;
        }
        return smooth;
    }

    private static boolean hasClearApproachFrom(ServerLevel level,
            Scp173Entity statue, Vec3 from, LivingEntity target) {
        Vec3 delta = target.position().subtract(from);
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        double distance = horizontal.length();
        if (distance <= STOP_DISTANCE) return true;
        Vec3 travel = horizontal.scale(1.0D / distance)
                .scale(distance - STOP_DISTANCE);
        return isClearSweep(level, statue, from, travel,
                CORRIDOR_SAMPLE_DISTANCE);
    }

    private static Vec3 firstVanillaPathStep(ServerLevel level,
            Scp173Entity statue, Path path, double maximumStep) {
        if (path == null || path.isDone()) return Vec3.ZERO;

        int safety = 0;
        while (!path.isDone() && safety++ < MAX_PATH_NODES) {
            Vec3 waypoint = path.getNextEntityPos(statue);
            path.advance();
            if (statue.position().distanceToSqr(waypoint)
                    <= NODE_REACHED_SQR) {
                continue;
            }
            return stepTowardWaypoint(level, statue, waypoint,
                    maximumStep);
        }
        return Vec3.ZERO;
    }

    private static Vec3 stepTowardWaypoint(ServerLevel level,
            Scp173Entity statue, Vec3 waypoint, double maximumStep) {
        Vec3 delta = waypoint.subtract(statue.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        double horizontalLength = horizontal.length();

        if (horizontalLength <= 0.001D) {
            return largestClearStep(level, statue, new Vec3(0.0D,
                    Mth.clamp(delta.y, -MAX_DOWN_STEP, MAX_UP_STEP), 0.0D),
                    SWEEP_SAMPLE_DISTANCE);
        }

        double horizontalTravel = Math.min(maximumStep, horizontalLength);
        double vertical = horizontalLength <= maximumStep * 1.25D
                ? Mth.clamp(delta.y, -MAX_DOWN_STEP, MAX_UP_STEP) : 0.0D;
        Vec3 desired = horizontal.scale(1.0D / horizontalLength)
                .scale(horizontalTravel).add(0.0D, vertical, 0.0D);
        return largestClearStep(level, statue, desired,
                SWEEP_SAMPLE_DISTANCE);
    }

    private static Vec3 largestClearStep(ServerLevel level,
            Scp173Entity statue, Vec3 desired, double sampleDistance) {
        if (!isUseful(desired)) return Vec3.ZERO;
        if (isClearSweep(level, statue, statue.position(), desired,
                sampleDistance)) {
            return desired;
        }

        double low = 0.0D;
        double high = 1.0D;
        for (int attempt = 0; attempt < 12; attempt++) {
            double middle = (low + high) * 0.5D;
            if (isClearSweep(level, statue, statue.position(),
                    desired.scale(middle), sampleDistance)) {
                low = middle;
            } else {
                high = middle;
            }
        }

        Vec3 shortened = desired.scale(low);
        if (isUseful(shortened)) return shortened;

        Vec3 xOnly = new Vec3(desired.x, desired.y, 0.0D);
        Vec3 zOnly = new Vec3(0.0D, desired.y, desired.z);
        Vec3 first = Math.abs(desired.x) >= Math.abs(desired.z)
                ? xOnly : zOnly;
        Vec3 second = first == xOnly ? zOnly : xOnly;
        if (isUseful(first) && isClearSweep(level, statue,
                statue.position(), first, sampleDistance)) return first;
        if (isUseful(second) && isClearSweep(level, statue,
                statue.position(), second, sampleDistance)) return second;
        return Vec3.ZERO;
    }

    private static boolean canStandAt(ServerLevel level,
            Scp173Entity statue, Vec3 position) {
        AABB box = boxAt(statue, position);
        if (hardCollisionVolume(level, statue, box) > COLLISION_EPSILON) {
            return false;
        }
        if (statue.isInWater()) return true;

        double inset = Math.min(0.08D, statue.getBbWidth() * 0.15D);
        AABB supportProbe = new AABB(box.minX + inset,
                box.minY - 0.09D, box.minZ + inset,
                box.maxX - inset, box.minY + 0.01D,
                box.maxZ - inset);
        return hardCollisionVolume(level, statue, supportProbe)
                > COLLISION_EPSILON;
    }

    /**
     * Samples the complete segment. If an old world already left 173 slightly
     * embedded, movement is allowed only while penetration never increases,
     * letting it escape without granting a route through a wall.
     */
    private static boolean isClearSweep(Level level, Scp173Entity statue,
            Vec3 start, Vec3 movement, double sampleDistance) {
        if (movement == null) return false;
        double length = movement.length();
        if (length <= 0.001D) return true;

        AABB startBox = boxAt(statue, start);
        double previousPenetration = hardCollisionVolume(level, statue,
                startBox);
        boolean escaping = previousPenetration > COLLISION_EPSILON;
        int samples = Math.max(1,
                (int) Math.ceil(length / sampleDistance));

        for (int sample = 1; sample <= samples; sample++) {
            AABB box = startBox.move(movement.scale(
                    sample / (double) samples));
            double penetration = hardCollisionVolume(level, statue, box);
            if (!escaping && penetration > COLLISION_EPSILON) return false;
            if (escaping) {
                if (penetration > previousPenetration
                        + PENETRATION_EPSILON) {
                    return false;
                }
                if (penetration <= COLLISION_EPSILON) escaping = false;
            }
            previousPenetration = penetration;
        }
        return true;
    }

    private static AABB boxAt(Scp173Entity statue, Vec3 position) {
        return statue.getBoundingBox().move(
                position.x - statue.getX(),
                position.y - statue.getY(),
                position.z - statue.getZ());
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
                    BlockState blockState = level.getBlockState(pos);
                    VoxelShape shape = blockState.getCollisionShape(level,
                            pos, context);
                    if (shape.isEmpty()
                            || isSoftObstacle(level, pos, blockState, shape)) {
                        continue;
                    }
                    for (AABB localPart : shape.toAabbs()) {
                        AABB part = localPart.move(x, y, z);
                        volume += intersectionVolume(box, part);
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

        // Low clutter is stepped over. Full-width horizontal pieces such as
        // pressure plates are therefore harmless without making vertical doors
        // or wall panels passable.
        if (height <= 0.25D) return true;
        if (height > 1.25D) return false;

        // A thin shape spanning almost an entire cell is a barrier plane: doors,
        // fences, wall panels and similar modded blocks stay authoritative.
        boolean barrierPlane = height >= 0.75D
                && (widthX >= 0.90D || widthZ >= 0.90D);
        if (barrierPlane) return false;

        // Compact, low-volume props do not dictate navigation. Datapack tags
        // above remain the explicit override for unusual third-party blocks.
        return volume <= 0.32D && widthX <= 0.80D && widthZ <= 0.80D;
    }

    private static double intersectionVolume(AABB first, AABB second) {
        double x = Math.max(0.0D,
                Math.min(first.maxX, second.maxX)
                        - Math.max(first.minX, second.minX));
        double y = Math.max(0.0D,
                Math.min(first.maxY, second.maxY)
                        - Math.max(first.minY, second.minY));
        double z = Math.max(0.0D,
                Math.min(first.maxZ, second.maxZ)
                        - Math.max(first.minZ, second.minZ));
        return x * y * z;
    }

    private static void applyStep(ServerLevel level, Scp173Entity statue,
            LivingEntity target, Vec3 step) {
        if (!isClearSweep(level, statue, statue.position(), step,
                SWEEP_SAMPLE_DISTANCE)) {
            return;
        }

        Vec3 toTarget = target.position().subtract(statue.position());
        if (toTarget.x * toTarget.x + toTarget.z * toTarget.z
                > 0.000001D) {
            setManualYaw(statue, (float) (Mth.atan2(toTarget.z, toTarget.x)
                    * Mth.RAD_TO_DEG) - 90.0F);
        }

        statue.setPos(statue.getX() + step.x,
                statue.getY() + step.y, statue.getZ() + step.z);
        stopAtCurrentPosition(statue);
    }

    private static void restore(Scp173Entity statue, Snapshot snapshot) {
        statue.absMoveTo(snapshot.x(), snapshot.y(), snapshot.z(),
                snapshot.yaw(), 0.0F);
        setScraping(statue, false);
        stopAtCurrentPosition(statue);
    }

    private static void stopAtCurrentPosition(Scp173Entity statue) {
        statue.getNavigation().stop();
        statue.getMoveControl().setWantedPosition(statue.getX(),
                statue.getY(), statue.getZ(), 0.0D);
        statue.setDeltaMovement(Vec3.ZERO);
        if (HARD_STOP != null) {
            try {
                HARD_STOP.invoke(statue);
            } catch (ReflectiveOperationException exception) {
                warnReflection(exception);
            }
        }
    }

    private static boolean isObservationLocked(Scp173Entity statue) {
        if (OBSERVATION_LOCKED != null) {
            try {
                return (boolean) OBSERVATION_LOCKED.invoke(statue);
            } catch (ReflectiveOperationException exception) {
                warnReflection(exception);
            }
        }
        for (Player player : statue.level().players()) {
            if (statue.isObservedBy(player)) return true;
        }
        return false;
    }

    private static double maximumStep(Scp173Entity statue,
            LivingEntity target) {
        if (!(target instanceof Player player)
                || !BlinkServerState.isBlinkClosed(player)) {
            return DIRECT_STEP;
        }
        if (BlinkServerState.isManualBlink(player)) return BLINK_STEP;

        double remaining = automaticBlinkRemaining(statue, player);
        if (remaining <= MIN_USEFUL_STEP) return 0.0D;
        return Math.min(BLINK_STEP, remaining);
    }

    @SuppressWarnings("unchecked")
    private static double automaticBlinkRemaining(Scp173Entity statue,
            Player player) {
        if (AUTOMATIC_BLINK_REMAINING == null) {
            return AUTOMATIC_BLINK_DISTANCE;
        }
        try {
            Map<UUID, Double> remaining = (Map<UUID, Double>)
                    AUTOMATIC_BLINK_REMAINING.get(statue);
            return remaining.getOrDefault(player.getUUID(),
                    AUTOMATIC_BLINK_DISTANCE);
        } catch (ReflectiveOperationException exception) {
            warnReflection(exception);
            return 0.0D;
        }
    }

    private static void consumeBlinkBudget(Scp173Entity statue,
            LivingEntity target, double distance) {
        if (!(target instanceof Player player)
                || !BlinkServerState.isBlinkClosed(player)
                || BlinkServerState.isManualBlink(player)
                || CONSUME_AUTOMATIC_BLINK == null) {
            return;
        }
        try {
            CONSUME_AUTOMATIC_BLINK.invoke(statue, player, distance);
        } catch (ReflectiveOperationException exception) {
            warnReflection(exception);
        }
    }

    private static boolean isValidPlayer(Player player) {
        return player != null && player.isAlive()
                && !player.isCreative() && !player.isSpectator();
    }

    private static boolean isValidTarget(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()) {
            return false;
        }
        if (entity instanceof Player player) return isValidPlayer(player);
        return Scp173TargetConfig.isConfiguredTarget(entity);
    }

    private static boolean isUseful(Vec3 movement) {
        return movement != null && movement.lengthSqr()
                > MIN_USEFUL_STEP * MIN_USEFUL_STEP;
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return Math.sqrt(x * x + z * z);
    }

    private static double heuristic(Vec3 position, Vec3 target) {
        return horizontalDistance(position, target);
    }

    private static void clearStrategicRoute(Scp173Entity statue) {
        if (CLEAR_STRATEGIC_ROUTE == null) return;
        try {
            CLEAR_STRATEGIC_ROUTE.invoke(statue);
        } catch (ReflectiveOperationException exception) {
            warnReflection(exception);
        }
    }

    private static void setManualYaw(Scp173Entity statue, float yaw) {
        if (SET_MANUAL_YAW == null) return;
        try {
            SET_MANUAL_YAW.invoke(statue, yaw);
        } catch (ReflectiveOperationException exception) {
            warnReflection(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setScraping(Scp173Entity statue, boolean value) {
        if (SCRAPING == null) return;
        try {
            EntityDataAccessor<Boolean> accessor =
                    (EntityDataAccessor<Boolean>) SCRAPING.get(null);
            statue.getEntityData().set(accessor, value);
        } catch (ReflectiveOperationException exception) {
            warnReflection(exception);
        }
    }

    private static Method method(String name, Class<?>... parameters) {
        try {
            Method method = Scp173Entity.class.getDeclaredMethod(
                    name, parameters);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
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
        ScpClassifiedDirectiveMod.LOGGER.warn(
                "SCP-173 movement controller lost internal access",
                exception);
    }

    private record GridNode(int x2, int z2) {
        private Vec3 position(double y) {
            return new Vec3(x2 * GRID_STEP, y, z2 * GRID_STEP);
        }
    }

    private record OpenNode(GridNode node, double score)
            implements Comparable<OpenNode> {
        @Override
        public int compareTo(OpenNode other) {
            return Double.compare(score, other.score);
        }
    }

    private record Snapshot(double x, double y, double z, float yaw,
                            boolean observedAtStart) {
        private Vec3 position() {
            return new Vec3(x, y, z);
        }
    }

    private static final class MovementState {
        private ResourceKey<Level> dimension;
        private Snapshot snapshot;
        private final List<Vec3> route = new ArrayList<>();
        private int routeIndex;
        private UUID routeTarget;
        private Vec3 routeTargetPosition;
        private long routeExpiresGameTime = Long.MIN_VALUE;
        private long nextRouteSearchGameTime;

        private void advanceReachedNodes(Vec3 position) {
            while (routeIndex < route.size()
                    && route.get(routeIndex).distanceToSqr(position)
                    <= NODE_REACHED_SQR) {
                routeIndex++;
            }
            if (routeIndex >= route.size()) clearRoute();
        }

        private void clearRoute() {
            route.clear();
            routeIndex = 0;
            routeTarget = null;
            routeTargetPosition = null;
            routeExpiresGameTime = Long.MIN_VALUE;
        }
    }
}
