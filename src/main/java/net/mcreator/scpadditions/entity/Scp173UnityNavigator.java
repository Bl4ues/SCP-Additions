package net.mcreator.scpadditions.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
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
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.facility.FacilityModule;

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
import java.util.WeakHashMap;

/**
 * Stable pursuit/navigation authority for SCP-173.
 *
 * The old implementation let Scp173Entity's vanilla-derived strategic route
 * move first and only invoked Scp173MovementController when that movement
 * stalled. A legal but strategically bad sideways step was therefore accepted,
 * allowing path recalculation to alternate sides of narrow openings every tick.
 * This navigator owns pursuit movement completely: one persistent route, real
 * collision geometry, corner-safe diagonals and clearance-biased A*.
 */
public final class Scp173UnityNavigator {
    private static final double SEARCH_RANGE = 96.0D;
    private static final double SEARCH_RANGE_SQR = SEARCH_RANGE * SEARCH_RANGE;
    private static final double STOP_DISTANCE = 0.72D;
    private static final double DIRECT_STEP = 1.20D;
    private static final double BLINK_STEP = 0.95D;
    private static final double AUTOMATIC_BLINK_DISTANCE = 6.0D;
    private static final double MIN_USEFUL_STEP = 0.035D;
    private static final double NODE_REACHED_SQR = 0.11D * 0.11D;

    // Quarter-block nodes are deliberate. A 0.82-block-wide statue has only
    // ~0.09 blocks of clearance per side in a one-block doorway. Half-block
    // navigation can represent the doorway centre, but is too coarse to make a
    // stable approach to it from an arbitrary offset.
    private static final double GRID_STEP = 0.25D;
    private static final double LOCAL_SEARCH_RADIUS = 24.0D;
    private static final int LOCAL_SEARCH_RADIUS_GRID =
            (int) Math.round(LOCAL_SEARCH_RADIUS / GRID_STEP);
    private static final int MAX_SEARCH_NODES = 16000;
    private static final int MAX_ROUTE_NODES = 384;
    private static final int MAX_VANILLA_PATH_NODES = 128;

    private static final long ROUTE_LIFETIME_TICKS = 120L;
    private static final long ROUTE_REPLAN_DELAY_TICKS = 3L;
    private static final double TARGET_SHIFT_REPLAN_SQR = 2.25D * 2.25D;
    private static final int LOOKAHEAD_NODES = 28;

    // Route planning intentionally asks for slightly more space than the
    // physical hitbox. This is what turns a merely collision-free diagonal into
    // a centred doorway approach instead of grazing the frame and oscillating.
    private static final double EDGE_CLEARANCE_MARGIN = 0.010D;
    private static final double SMOOTH_CLEARANCE_MARGIN = 0.045D;
    private static final double DIRECT_CLEARANCE_MARGIN = 0.035D;
    private static final double ROUTE_RELEASE_MARGIN = 0.060D;

    private static final double SWEEP_SAMPLE_DISTANCE = 0.03125D;
    private static final double PLANNING_SAMPLE_DISTANCE = 0.0625D;
    private static final double MAX_UP_STEP = 1.05D;
    private static final double MAX_DOWN_STEP = 1.00D;
    private static final double COLLISION_EPSILON = 1.0E-6D;
    private static final double PENETRATION_EPSILON = 1.0E-5D;

    private static final int[][] GRID_DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private static final TagKey<Block> SOFT_OBSTACLES = TagKey.create(
            Registries.BLOCK, new ResourceLocation(ScpAdditionsMod.MODID,
                    "scp_173_soft_obstacles"));
    private static final TagKey<Block> HARD_OBSTACLES = TagKey.create(
            Registries.BLOCK, new ResourceLocation(ScpAdditionsMod.MODID,
                    "scp_173_hard_obstacles"));

    private static final Map<Scp173Entity, NavigationState> STATES =
            new WeakHashMap<>();

    private static final Method OBSERVATION_LOCKED = privateMethod(
            "isObservationLocked");
    private static final Method CONSUME_AUTOMATIC_BLINK = privateMethod(
            "consumeAutomaticBlinkTravel", Player.class, double.class);
    private static final Field AUTOMATIC_BLINK_REMAINING = privateField(
            "automaticBlinkTravelRemaining");
    private static final Field SCRAPING = privateField("SCRAPING");
    private static boolean reflectionWarningLogged;

    private Scp173UnityNavigator() {
    }

    /** Called instead of Scp173MovementController's legacy repair pass. */
    public static void tick(ServerLevel level, Scp173Entity statue) {
        if (level == null || statue == null || !statue.isAlive()
                || statue.isRemoved()) {
            if (statue != null) STATES.remove(statue);
            return;
        }

        NavigationState state = STATES.computeIfAbsent(statue,
                ignored -> new NavigationState());

        if (!ScpAdditionsModulesConfig.get().scp173.enabled
                || !statue.isActivated()) {
            state.clearAll();
            setScraping(statue, false);
            stopAtCurrentPosition(statue);
            return;
        }

        // Scp173Entity already applies its frozen vertical physics before this
        // pass. Do not restore a tick-start position here; simply refuse pursuit
        // motion while any authoritative observer lock exists.
        if (isObservationLocked(statue)) {
            setScraping(statue, false);
            stopAtCurrentPosition(statue);
            state.stallTicks = 0;
            return;
        }

        LivingEntity target = resolveStableTarget(level, statue, state);
        if (target == null) {
            state.clearAll();
            setScraping(statue, false);
            stopAtCurrentPosition(statue);
            return;
        }

        // Contact attacks remain owned by Scp173Entity so damage type, cooldown,
        // observation checks and multiplayer behavior stay in one place.
        if (statue.doHurtTarget(target)) {
            setScraping(statue, false);
            stopAtCurrentPosition(statue);
            return;
        }

        double maximumStep = maximumStep(statue, target);
        if (maximumStep <= MIN_USEFUL_STEP) {
            setScraping(statue, false);
            stopAtCurrentPosition(statue);
            return;
        }

        Vec3 step = chooseStep(level, statue, target, state, maximumStep);
        if (!isUseful(step)) {
            setScraping(statue, false);
            stopAtCurrentPosition(statue);
            return;
        }

        Vec3 start = statue.position();
        if (!isClearSweep(level, statue, start, step,
                SWEEP_SAMPLE_DISTANCE, 0.0D)) {
            state.noteStall(level.getGameTime());
            setScraping(statue, false);
            stopAtCurrentPosition(statue);
            return;
        }

        faceMovement(statue, step);
        statue.setPos(start.x + step.x, start.y + step.y,
                start.z + step.z);
        setScraping(statue, true);
        consumeBlinkBudget(statue, target, step.length());
        state.noteProgress(statue.position());
        stopMotionButKeepScraping(statue);
        statue.doHurtTarget(target);
    }

    private static LivingEntity resolveStableTarget(ServerLevel level,
            Scp173Entity statue, NavigationState state) {
        LivingEntity current = statue.getTarget();

        // Keep an already selected player. Blink packets deliberately set the
        // target before the entity tick, and constantly selecting whichever
        // player is a few centimetres nearer would make multiplayer routes
        // thrash just as badly as the old doorway logic.
        if (current instanceof Player player && isValidPlayer(player)
                && statue.distanceToSqr(player) <= SEARCH_RANGE_SQR) {
            state.prepareTarget(player);
            return player;
        }

        AABB area = statue.getBoundingBox().inflate(SEARCH_RANGE);
        Player nearestPlayer = null;
        double nearestPlayerDistance = Double.MAX_VALUE;
        for (Player player : level.getEntitiesOfClass(Player.class, area,
                Scp173UnityNavigator::isValidPlayer)) {
            double distance = statue.distanceToSqr(player);
            if (distance <= SEARCH_RANGE_SQR
                    && distance < nearestPlayerDistance) {
                nearestPlayer = player;
                nearestPlayerDistance = distance;
            }
        }
        if (nearestPlayer != null) {
            if (current == null || !current.getUUID().equals(
                    nearestPlayer.getUUID())) {
                statue.setTarget(nearestPlayer);
            }
            state.prepareTarget(nearestPlayer);
            return nearestPlayer;
        }

        if (isValidTarget(current)
                && statue.distanceToSqr(current) <= SEARCH_RANGE_SQR) {
            state.prepareTarget(current);
            return current;
        }

        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class, area,
                entity -> entity != statue && isValidTarget(entity))) {
            double distance = statue.distanceToSqr(candidate);
            if (distance <= SEARCH_RANGE_SQR && distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        if (best != null) statue.setTarget(best);
        state.prepareTarget(best);
        return best;
    }

    private static Vec3 chooseStep(ServerLevel level, Scp173Entity statue,
            LivingEntity target, NavigationState state, double maximumStep) {
        long gameTime = level.getGameTime();
        state.prepareTarget(target);
        state.advanceReachedNodes(statue.position());

        boolean targetShifted = state.routeTargetPosition == null
                || horizontalDistanceSqr(state.routeTargetPosition,
                        target.position()) >= TARGET_SHIFT_REPLAN_SQR;
        if (targetShifted && state.hasRoute()) {
            state.clearRoute();
            state.nextPlanGameTime = gameTime;
        }

        Vec3 direct = directStep(statue, target, maximumStep);
        if (!isUseful(direct)) return Vec3.ZERO;

        // Once committed to a detour, do not discard it just because a marginal
        // diagonal happens to be collision-free on this exact tick. Require a
        // wider corridor before returning to direct pursuit.
        if (state.hasRoute()) {
            if (isClearSweep(level, statue, statue.position(), direct,
                    PLANNING_SAMPLE_DISTANCE, ROUTE_RELEASE_MARGIN)) {
                state.clearRoute();
                state.routeTargetPosition = target.position();
                state.stallTicks = 0;
                return direct;
            }

            Vec3 routed = routeStep(level, statue, state, maximumStep);
            if (isUseful(routed)) {
                state.stallTicks = 0;
                return routed;
            }

            // Never improvise an X-only/Z-only sidestep. That was the visible
            // doorway dance. A blocked committed route is invalidated and
            // replanned from the current position instead.
            state.noteStall(gameTime);
            state.clearRoute();
            state.nextPlanGameTime = gameTime;
        }

        if (isClearSweep(level, statue, statue.position(), direct,
                PLANNING_SAMPLE_DISTANCE, DIRECT_CLEARANCE_MARGIN)) {
            state.routeTargetPosition = target.position();
            state.stallTicks = 0;
            return direct;
        }

        if (gameTime >= state.nextPlanGameTime) {
            List<Vec3> route = findCollisionRoute(level, statue, target);
            state.nextPlanGameTime = gameTime + ROUTE_REPLAN_DELAY_TICKS;
            if (!route.isEmpty()) {
                state.installRoute(route, target, gameTime);
                Vec3 routed = routeStep(level, statue, state, maximumStep);
                if (isUseful(routed)) {
                    state.stallTicks = 0;
                    return routed;
                }
                state.clearRoute();
            }
        }

        // Stairs and unusual vertical transitions are left to Minecraft's path
        // topology, but the resulting waypoint still has to pass our swept
        // collision test. It therefore cannot cut through a closed facility
        // door or teleport across a corner.
        Vec3 vanilla = vanillaPathStep(level, statue, target, maximumStep);
        if (isUseful(vanilla)) return vanilla;

        // If the target is behind a genuinely closed/solid barrier, move only
        // along the safe prefix toward a staging point. No sideways guesswork.
        return largestClearStep(level, statue, direct,
                SWEEP_SAMPLE_DISTANCE);
    }

    private static Vec3 directStep(Scp173Entity statue, LivingEntity target,
            double maximumStep) {
        Vec3 delta = target.position().subtract(statue.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        double distance = horizontal.length();
        if (distance <= STOP_DISTANCE) return Vec3.ZERO;
        double travel = Math.min(maximumStep, distance - STOP_DISTANCE);
        if (travel <= MIN_USEFUL_STEP) return Vec3.ZERO;
        return horizontal.scale(1.0D / distance).scale(travel);
    }

    private static Vec3 routeStep(ServerLevel level, Scp173Entity statue,
            NavigationState state, double maximumStep) {
        state.advanceReachedNodes(statue.position());
        if (!state.hasRoute()) return Vec3.ZERO;

        int chosenIndex = state.routeIndex;
        int farthest = Math.min(state.route.size() - 1,
                state.routeIndex + LOOKAHEAD_NODES);
        for (int index = farthest; index > state.routeIndex; index--) {
            Vec3 delta = state.route.get(index).subtract(statue.position());
            if (isClearSweep(level, statue, statue.position(), delta,
                    PLANNING_SAMPLE_DISTANCE, SMOOTH_CLEARANCE_MARGIN)) {
                chosenIndex = index;
                break;
            }
        }

        Vec3 waypoint = state.route.get(chosenIndex);
        Vec3 step = stepTowardWaypoint(level, statue, waypoint, maximumStep);
        if (!isUseful(step)) return Vec3.ZERO;
        state.routeIndex = chosenIndex;
        return step;
    }

    /**
     * Quarter-grid A* over the statue's real collision volume. The search
     * penalises low-clearance cells and forbids diagonal corner cutting, so a
     * one-block opening naturally produces a centred approach instead of an
     * alternating left/right sequence of technically valid positions.
     */
    private static List<Vec3> findCollisionRoute(ServerLevel level,
            Scp173Entity statue, LivingEntity target) {
        Vec3 start = statue.position();
        double baseY = start.y;
        int centerX = (int) Math.round(start.x / GRID_STEP);
        int centerZ = (int) Math.round(start.z / GRID_STEP);

        PriorityQueue<OpenNode> open = new PriorityQueue<>();
        Map<GridNode, Double> gScore = new HashMap<>();
        Map<GridNode, GridNode> parent = new HashMap<>();
        Map<GridNode, Double> clearanceCosts = new HashMap<>();
        Set<GridNode> closed = new HashSet<>();

        for (GridNode seed : seedNodes(start)) {
            Vec3 seedPosition = seed.position(baseY);
            Vec3 toSeed = seedPosition.subtract(start);
            if (!canStandAt(level, statue, seedPosition)
                    || !isClearSweep(level, statue, start, toSeed,
                    PLANNING_SAMPLE_DISTANCE, EDGE_CLEARANCE_MARGIN)) {
                continue;
            }
            double g = horizontalDistance(start, seedPosition)
                    + clearancePenalty(level, statue, seedPosition,
                    clearanceCosts, seed);
            gScore.put(seed, g);
            parent.put(seed, null);
            open.add(new OpenNode(seed,
                    g + heuristic(seedPosition, target.position())));
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
                    || (currentHeuristic <= 4.0D
                    && hasClearApproach(level, statue, currentPosition,
                    target))) {
                goal = current;
                break;
            }

            for (int[] direction : GRID_DIRECTIONS) {
                GridNode neighbor = new GridNode(current.x()
                        + direction[0], current.z() + direction[1]);
                if (Math.abs(neighbor.x() - centerX)
                        > LOCAL_SEARCH_RADIUS_GRID
                        || Math.abs(neighbor.z() - centerZ)
                        > LOCAL_SEARCH_RADIUS_GRID
                        || closed.contains(neighbor)) {
                    continue;
                }

                Vec3 neighborPosition = neighbor.position(baseY);
                if (!canStandAt(level, statue, neighborPosition)) continue;

                boolean diagonal = direction[0] != 0 && direction[1] != 0;
                if (diagonal && !diagonalIsCornerSafe(level, statue,
                        current, currentPosition, direction, baseY)) {
                    continue;
                }

                Vec3 edge = neighborPosition.subtract(currentPosition);
                if (!isClearSweep(level, statue, currentPosition, edge,
                        PLANNING_SAMPLE_DISTANCE, EDGE_CLEARANCE_MARGIN)) {
                    continue;
                }

                double edgeCost = diagonal
                        ? Math.sqrt(2.0D) * GRID_STEP : GRID_STEP;
                double turnCost = turnPenalty(parent.get(current), current,
                        direction);
                double tentative = gScore.getOrDefault(current,
                        Double.POSITIVE_INFINITY)
                        + edgeCost + turnCost
                        + clearancePenalty(level, statue, neighborPosition,
                        clearanceCosts, neighbor);
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
                || bestHeuristic > startHeuristic - 0.20D) {
            return Collections.emptyList();
        }

        List<Vec3> reversed = new ArrayList<>();
        GridNode cursor = destination;
        int safety = 0;
        while (cursor != null && safety++ < MAX_ROUTE_NODES) {
            reversed.add(cursor.position(baseY));
            cursor = parent.get(cursor);
        }
        Collections.reverse(reversed);
        while (!reversed.isEmpty()
                && horizontalDistanceSqr(reversed.get(0), start)
                <= NODE_REACHED_SQR) {
            reversed.remove(0);
        }
        return smoothRoute(level, statue, start, reversed);
    }

    private static boolean diagonalIsCornerSafe(ServerLevel level,
            Scp173Entity statue, GridNode current, Vec3 currentPosition,
            int[] direction, double baseY) {
        GridNode sideX = new GridNode(current.x() + direction[0],
                current.z());
        GridNode sideZ = new GridNode(current.x(),
                current.z() + direction[1]);
        Vec3 sideXPosition = sideX.position(baseY);
        Vec3 sideZPosition = sideZ.position(baseY);
        return canStandAt(level, statue, sideXPosition)
                && canStandAt(level, statue, sideZPosition)
                && isClearSweep(level, statue, currentPosition,
                sideXPosition.subtract(currentPosition),
                PLANNING_SAMPLE_DISTANCE, EDGE_CLEARANCE_MARGIN)
                && isClearSweep(level, statue, currentPosition,
                sideZPosition.subtract(currentPosition),
                PLANNING_SAMPLE_DISTANCE, EDGE_CLEARANCE_MARGIN);
    }

    private static double turnPenalty(GridNode previous, GridNode current,
            int[] direction) {
        if (previous == null || current == null) return 0.0D;
        int previousX = Integer.signum(current.x() - previous.x());
        int previousZ = Integer.signum(current.z() - previous.z());
        if (previousX == direction[0] && previousZ == direction[1]) {
            return 0.0D;
        }
        // A small turn cost suppresses equal-length zig-zag routes without
        // overpowering the actual shortest-path/clearance terms.
        return 0.045D;
    }

    private static double clearancePenalty(ServerLevel level,
            Scp173Entity statue, Vec3 position,
            Map<GridNode, Double> cache, GridNode node) {
        Double cached = cache.get(node);
        if (cached != null) return cached;

        AABB box = boxAt(statue, position);
        double cost;
        if (hardCollisionVolume(level, statue,
                box.inflate(0.045D, 0.0D, 0.045D)) > COLLISION_EPSILON) {
            cost = 0.55D;
        } else if (hardCollisionVolume(level, statue,
                box.inflate(0.075D, 0.0D, 0.075D)) > COLLISION_EPSILON) {
            cost = 0.26D;
        } else if (hardCollisionVolume(level, statue,
                box.inflate(0.110D, 0.0D, 0.110D)) > COLLISION_EPSILON) {
            cost = 0.10D;
        } else {
            cost = 0.0D;
        }
        cache.put(node, cost);
        return cost;
    }

    private static List<GridNode> seedNodes(Vec3 start) {
        int floorX = Mth.floor(start.x / GRID_STEP);
        int floorZ = Mth.floor(start.z / GRID_STEP);
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
            int farthest = Math.min(raw.size() - 1, index + LOOKAHEAD_NODES);
            for (int candidate = farthest; candidate > index; candidate--) {
                Vec3 delta = raw.get(candidate).subtract(anchor);
                if (isClearSweep(level, statue, anchor, delta,
                        PLANNING_SAMPLE_DISTANCE, SMOOTH_CLEARANCE_MARGIN)) {
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

    private static boolean hasClearApproach(ServerLevel level,
            Scp173Entity statue, Vec3 from, LivingEntity target) {
        Vec3 delta = target.position().subtract(from);
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        double distance = horizontal.length();
        if (distance <= STOP_DISTANCE) return true;
        Vec3 travel = horizontal.scale(1.0D / distance)
                .scale(distance - STOP_DISTANCE);
        return isClearSweep(level, statue, from, travel,
                PLANNING_SAMPLE_DISTANCE, DIRECT_CLEARANCE_MARGIN);
    }

    private static Vec3 vanillaPathStep(ServerLevel level,
            Scp173Entity statue, LivingEntity target, double maximumStep) {
        Path path = statue.getNavigation().createPath(target, 0);
        if (path == null || path.isDone()) return Vec3.ZERO;

        int safety = 0;
        while (!path.isDone() && safety++ < MAX_VANILLA_PATH_NODES) {
            Vec3 waypoint = path.getNextEntityPos(statue);
            path.advance();
            if (horizontalDistanceSqr(statue.position(), waypoint)
                    <= NODE_REACHED_SQR) {
                continue;
            }
            Vec3 step = stepTowardWaypoint(level, statue, waypoint,
                    maximumStep);
            if (isUseful(step)) return step;
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
                sampleDistance, 0.0D)) {
            return desired;
        }

        double low = 0.0D;
        double high = 1.0D;
        for (int attempt = 0; attempt < 12; attempt++) {
            double middle = (low + high) * 0.5D;
            if (isClearSweep(level, statue, statue.position(),
                    desired.scale(middle), sampleDistance, 0.0D)) {
                low = middle;
            } else {
                high = middle;
            }
        }
        Vec3 shortened = desired.scale(low);
        return isUseful(shortened) ? shortened : Vec3.ZERO;
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

    private static boolean isClearSweep(Level level, Scp173Entity statue,
            Vec3 start, Vec3 movement, double sampleDistance,
            double horizontalMargin) {
        if (movement == null) return false;
        double length = movement.length();
        if (length <= 0.001D) return true;

        AABB startBox = boxAt(statue, start);
        if (horizontalMargin > 0.0D) {
            startBox = startBox.inflate(horizontalMargin, 0.0D,
                    horizontalMargin);
        }
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
                    BlockState state = level.getBlockState(pos);
                    VoxelShape shape = state.getCollisionShape(level, pos,
                            context);
                    if (shape.isEmpty() || isSoftObstacle(level, pos,
                            state, shape)) {
                        continue;
                    }
                    for (AABB localPart : shape.toAabbs()) {
                        volume += intersectionVolume(box,
                                localPart.move(x, y, z));
                    }
                }
            }
        }
        return volume;
    }

    private static boolean isSoftObstacle(Level level, BlockPos pos,
            BlockState state, VoxelShape shape) {
        // Facility doors are frame-animated blocks. Their ordinary collision
        // classification can lag the visually open/passable state, so use the
        // facility's own passability decision as the navigation authority.
        if (FacilityModule.isDoorPassable(state)) return true;
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

    private static void setScraping(Scp173Entity statue, boolean value) {
        if (SCRAPING == null) return;
        try {
            @SuppressWarnings("unchecked")
            EntityDataAccessor<Boolean> accessor =
                    (EntityDataAccessor<Boolean>) SCRAPING.get(null);
            statue.getEntityData().set(accessor, value);
        } catch (ReflectiveOperationException exception) {
            warnReflection(exception);
        }
    }

    private static void faceMovement(Scp173Entity statue, Vec3 movement) {
        if (movement.x * movement.x + movement.z * movement.z <= 1.0E-8D) {
            return;
        }
        float yaw = (float) (Mth.atan2(movement.z, movement.x)
                * Mth.RAD_TO_DEG) - 90.0F;
        statue.setYRot(Mth.wrapDegrees(yaw));
    }

    private static void stopAtCurrentPosition(Scp173Entity statue) {
        statue.getNavigation().stop();
        statue.getMoveControl().setWantedPosition(statue.getX(),
                statue.getY(), statue.getZ(), 0.0D);
        statue.setDeltaMovement(Vec3.ZERO);
    }

    private static void stopMotionButKeepScraping(Scp173Entity statue) {
        statue.getNavigation().stop();
        statue.getMoveControl().setWantedPosition(statue.getX(),
                statue.getY(), statue.getZ(), 0.0D);
        statue.setDeltaMovement(Vec3.ZERO);
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

    private static double heuristic(Vec3 first, Vec3 second) {
        return horizontalDistance(first, second);
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return Math.sqrt(x * x + z * z);
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        if (first == null || second == null) return Double.POSITIVE_INFINITY;
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    private static Method privateMethod(String name, Class<?>... parameters) {
        try {
            Method method = Scp173Entity.class.getDeclaredMethod(name,
                    parameters);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static Field privateField(String name) {
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
                "SCP-173 Unity navigator lost internal entity access",
                exception);
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

    private static final class NavigationState {
        private final List<Vec3> route = new ArrayList<>();
        private int routeIndex;
        private UUID targetId;
        private Vec3 routeTargetPosition;
        private long routeExpiresGameTime = Long.MIN_VALUE;
        private long nextPlanGameTime;
        private int stallTicks;
        private Vec3 lastProgressPosition;

        private void prepareTarget(LivingEntity target) {
            UUID nextId = target == null ? null : target.getUUID();
            if (nextId == null) {
                clearAll();
                return;
            }
            if (!nextId.equals(targetId)) {
                clearAll();
                targetId = nextId;
                routeTargetPosition = target.position();
            }
        }

        private boolean hasRoute() {
            return routeIndex < route.size()
                    && routeExpiresGameTime != Long.MIN_VALUE;
        }

        private void installRoute(List<Vec3> nodes, LivingEntity target,
                long gameTime) {
            route.clear();
            route.addAll(nodes);
            routeIndex = 0;
            targetId = target.getUUID();
            routeTargetPosition = target.position();
            routeExpiresGameTime = gameTime + ROUTE_LIFETIME_TICKS;
            stallTicks = 0;
        }

        private void advanceReachedNodes(Vec3 position) {
            while (routeIndex < route.size()
                    && horizontalDistanceSqr(position, route.get(routeIndex))
                    <= NODE_REACHED_SQR) {
                routeIndex++;
            }
            if (routeIndex >= route.size()) clearRoute();
        }

        private void noteProgress(Vec3 position) {
            if (lastProgressPosition == null
                    || horizontalDistanceSqr(lastProgressPosition, position)
                    > MIN_USEFUL_STEP * MIN_USEFUL_STEP) {
                lastProgressPosition = position;
                stallTicks = 0;
            }
        }

        private void noteStall(long gameTime) {
            stallTicks++;
            if (stallTicks >= 2) nextPlanGameTime = gameTime;
        }

        private void clearRoute() {
            route.clear();
            routeIndex = 0;
            routeExpiresGameTime = Long.MIN_VALUE;
        }

        private void clearAll() {
            clearRoute();
            targetId = null;
            routeTargetPosition = null;
            nextPlanGameTime = 0L;
            stallTicks = 0;
            lastProgressPosition = null;
        }
    }
}
