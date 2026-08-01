package net.mcreator.scpadditions.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Supplies a second, conservative pursuit pass after entity ticks. It acts only
 * when SCP-173 had a valid movement opportunity but made no progress, allowing
 * it to retain a route, approach a newly closed door, and reacquire its target
 * without adding a second normal movement step.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class Scp173StrategicPursuitController {
    private static final double SEARCH_RANGE = 48.0D;
    private static final double MOVEMENT_EPSILON_SQR = 0.035D * 0.035D;
    private static final double NODE_REACHED_SQR = 0.58D * 0.58D;
    private static final double TARGET_REPLAN_SQR = 1.5D * 1.5D;
    private static final double ROUTE_FIRST_NODE_DIFFERENCE_SQR = 0.45D * 0.45D;
    private static final double ROUTE_ENDPOINT_IMPROVEMENT_SQR = 0.65D;
    private static final double MIN_USEFUL_STEP = 0.055D;
    private static final double ASSISTED_STEP = 0.72D;
    private static final double MANUAL_BLINK_ASSISTED_STEP = 0.58D;
    private static final int REPLAN_INTERVAL_TICKS = 4;
    private static final int ROUTE_MEMORY_TICKS = 100;
    private static final int MAX_ROUTE_NODES = 128;

    private static final Map<UUID, PursuitState> STATES = new HashMap<>();

    private static final Method OBSERVATION_LOCKED = method(
            "isObservationLocked");
    private static final Method SNAP_MOVE = method("snapMove", Vec3.class);
    private static final Method SET_MANUAL_YAW = method(
            "setManualYaw", float.class);
    private static final Method HARD_STOP = method("hardStopLocalMovement");
    private static final Field SCRAPING = field("SCRAPING");
    private static boolean reflectionWarningLogged;

    private Scp173StrategicPursuitController() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)) {
            return;
        }

        List<Scp173Entity> statues = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Scp173Entity statue) statues.add(statue);
        }

        Set<UUID> present = new HashSet<>();
        for (Scp173Entity statue : statues) {
            present.add(statue.getUUID());
            update(level, statue);
        }
        STATES.entrySet().removeIf(entry -> !present.contains(entry.getKey())
                && entry.getValue().lastLevel == level.dimension());
    }

    private static void update(ServerLevel level, Scp173Entity statue) {
        PursuitState state = STATES.computeIfAbsent(statue.getUUID(),
                ignored -> new PursuitState());
        state.lastLevel = level.dimension();

        Vec3 current = statue.position();
        if (state.lastPosition == null) {
            state.lastPosition = current;
        }

        if (!ScpAdditionsModulesConfig.get().scp173.enabled
                || !statue.isAlive() || statue.isRemoved()
                || !statue.isActivated()) {
            state.reset(current);
            return;
        }

        LivingEntity target = resolveTarget(level, statue);
        if (target == null) {
            state.reset(current);
            return;
        }

        prepareTarget(state, target);
        long gameTime = level.getGameTime();
        boolean targetShifted = state.lastTargetPosition == null
                || horizontalDistanceSqr(state.lastTargetPosition,
                        target.position()) >= TARGET_REPLAN_SQR;
        if (targetShifted || gameTime >= state.nextReplanGameTime) {
            considerFreshRoute(statue, target, state, gameTime);
        }

        boolean movedNormally = current.distanceToSqr(state.lastPosition)
                > MOVEMENT_EPSILON_SQR;
        state.lastPosition = current;
        state.lastTargetPosition = target.position();
        if (movedNormally) {
            state.stallAttempts = 0;
            trimReachedNodes(statue.position(), state);
            return;
        }

        if (isObservationLocked(statue)) {
            return;
        }

        if (target instanceof Player player
                && BlinkServerState.isBlinkClosed(player)
                && !BlinkServerState.isManualBlink(player)) {
            // Automatic blinks already have a hard travel budget inside the
            // entity. Never let the corrective pass spend beyond that budget.
            return;
        }

        state.stallAttempts++;
        if (state.stallAttempts >= 2
                || gameTime >= state.nextReplanGameTime) {
            considerFreshRoute(statue, target, state, gameTime);
        }

        double maximumStep = target instanceof Player player
                && BlinkServerState.isBlinkClosed(player)
                ? MANUAL_BLINK_ASSISTED_STEP : ASSISTED_STEP;
        Vec3 step = stepAlongRememberedRoute(statue, state, maximumStep);
        if (step.lengthSqr() <= MIN_USEFUL_STEP * MIN_USEFUL_STEP) {
            // Even without a complete path, advance to the last physically
            // reachable point before the obstruction instead of stopping in the
            // middle of the previous room.
            step = largestClearStep(statue,
                    horizontalDirection(statue.position(), target.position()),
                    maximumStep);
        }

        if (step.lengthSqr() <= MIN_USEFUL_STEP * MIN_USEFUL_STEP) {
            return;
        }

        applyStrategicStep(statue, step);
        state.lastPosition = statue.position();
        state.stallAttempts = 0;
        state.routeExpiresGameTime = gameTime + ROUTE_MEMORY_TICKS;
        trimReachedNodes(statue.position(), state);
    }

    private static LivingEntity resolveTarget(ServerLevel level,
            Scp173Entity statue) {
        LivingEntity current = statue.getTarget();
        if (isValidTarget(current)) return current;

        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        AABB area = statue.getBoundingBox().inflate(SEARCH_RANGE);
        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class, area,
                entity -> entity != statue && isValidTarget(entity))) {
            double distance = statue.distanceToSqr(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        if (best != null) statue.setTarget(best);
        return best;
    }

    private static boolean isValidTarget(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return Scp173TargetConfig.isConfiguredTarget(entity);
    }

    private static void prepareTarget(PursuitState state,
            LivingEntity target) {
        if (target.getUUID().equals(state.targetId)) return;
        state.route.clear();
        state.routeIndex = 0;
        state.targetId = target.getUUID();
        state.lastTargetPosition = target.position();
        state.routeEndpointDistanceSqr = Double.POSITIVE_INFINITY;
        state.routeExpiresGameTime = Long.MIN_VALUE;
        state.nextReplanGameTime = 0L;
        state.stallAttempts = 0;
    }

    private static void considerFreshRoute(Scp173Entity statue,
            LivingEntity target, PursuitState state, long gameTime) {
        state.nextReplanGameTime = gameTime + REPLAN_INTERVAL_TICKS;
        Path path = statue.getNavigation().createPath(target, 0);
        List<Vec3> fresh = capturePath(statue, path);
        trimReachedNodes(statue.position(), fresh);
        if (fresh.isEmpty()) return;

        Vec3 endpoint = fresh.get(fresh.size() - 1);
        double endpointDistance = horizontalDistanceSqr(endpoint,
                target.position());
        boolean currentUsable = hasUsableRoute(state, gameTime);
        boolean currentBlocked = currentUsable
                && !hasClearStepToCurrentNode(statue, state);
        boolean clearlyBetter = !currentUsable
                || endpointDistance + ROUTE_ENDPOINT_IMPROVEMENT_SQR
                < state.routeEndpointDistanceSqr;
        boolean usefulAlternative = state.stallAttempts > 0
                && (currentBlocked || firstNodeDiffers(fresh, state));

        if (!clearlyBetter && !usefulAlternative) return;

        state.route.clear();
        state.route.addAll(fresh);
        state.routeIndex = 0;
        state.routeEndpointDistanceSqr = endpointDistance;
        state.routeExpiresGameTime = gameTime + ROUTE_MEMORY_TICKS;
    }

    private static List<Vec3> capturePath(Scp173Entity statue, Path path) {
        List<Vec3> nodes = new ArrayList<>();
        if (path == null || path.isDone()) return nodes;

        Vec3 previous = null;
        int safety = 0;
        while (!path.isDone() && safety++ < MAX_ROUTE_NODES) {
            Vec3 node = path.getNextEntityPos(statue);
            if (previous == null
                    || horizontalDistanceSqr(previous, node) > 0.01D) {
                nodes.add(node);
                previous = node;
            }
            path.advance();
        }
        return nodes;
    }

    private static Vec3 stepAlongRememberedRoute(Scp173Entity statue,
            PursuitState state, double maximumStep) {
        trimReachedNodes(statue.position(), state);
        if (state.routeIndex >= state.route.size()) return Vec3.ZERO;
        Vec3 direction = horizontalDirection(statue.position(),
                state.route.get(state.routeIndex));
        return largestClearStep(statue, direction, maximumStep);
    }

    private static Vec3 largestClearStep(Scp173Entity statue,
            Vec3 normalizedDirection, double maximumStep) {
        if (normalizedDirection.lengthSqr() <= 0.000001D
                || maximumStep <= MIN_USEFUL_STEP) {
            return Vec3.ZERO;
        }

        Vec3 full = normalizedDirection.scale(maximumStep);
        if (canMoveBy(statue, full)) return full;

        double low = 0.0D;
        double high = maximumStep;
        for (int attempt = 0; attempt < 9; attempt++) {
            double middle = (low + high) * 0.5D;
            Vec3 candidate = normalizedDirection.scale(middle);
            if (canMoveBy(statue, candidate)) low = middle;
            else high = middle;
        }
        return low >= MIN_USEFUL_STEP
                ? normalizedDirection.scale(low) : Vec3.ZERO;
    }

    private static boolean canMoveBy(Scp173Entity statue, Vec3 step) {
        return step != null && step.lengthSqr() > 0.000001D
                && statue.level().noCollision(statue,
                        statue.getBoundingBox().move(step));
    }

    private static boolean hasClearStepToCurrentNode(Scp173Entity statue,
            PursuitState state) {
        if (state.routeIndex >= state.route.size()) return false;
        Vec3 direction = horizontalDirection(statue.position(),
                state.route.get(state.routeIndex));
        return largestClearStep(statue, direction, ASSISTED_STEP)
                .lengthSqr() > MIN_USEFUL_STEP * MIN_USEFUL_STEP;
    }

    private static boolean firstNodeDiffers(List<Vec3> fresh,
            PursuitState state) {
        if (fresh.isEmpty() || state.routeIndex >= state.route.size()) {
            return true;
        }
        return horizontalDistanceSqr(fresh.get(0),
                state.route.get(state.routeIndex))
                >= ROUTE_FIRST_NODE_DIFFERENCE_SQR;
    }

    private static boolean hasUsableRoute(PursuitState state,
            long gameTime) {
        return gameTime <= state.routeExpiresGameTime
                && state.routeIndex < state.route.size();
    }

    private static void trimReachedNodes(Vec3 position,
            PursuitState state) {
        while (state.routeIndex < state.route.size()
                && horizontalDistanceSqr(position,
                        state.route.get(state.routeIndex))
                <= NODE_REACHED_SQR) {
            state.routeIndex++;
        }
    }

    private static void trimReachedNodes(Vec3 position, List<Vec3> nodes) {
        while (!nodes.isEmpty()
                && horizontalDistanceSqr(position, nodes.get(0))
                <= NODE_REACHED_SQR) {
            nodes.remove(0);
        }
    }

    private static Vec3 horizontalDirection(Vec3 from, Vec3 to) {
        if (from == null || to == null) return Vec3.ZERO;
        Vec3 horizontal = new Vec3(to.x - from.x, 0.0D, to.z - from.z);
        double length = horizontal.length();
        return length <= 0.000001D
                ? Vec3.ZERO : horizontal.scale(1.0D / length);
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        if (first == null || second == null) return Double.POSITIVE_INFINITY;
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
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

    @SuppressWarnings("unchecked")
    private static void applyStrategicStep(Scp173Entity statue, Vec3 step) {
        try {
            if (SCRAPING != null) {
                EntityDataAccessor<Boolean> accessor =
                        (EntityDataAccessor<Boolean>) SCRAPING.get(null);
                statue.getEntityData().set(accessor, true);
            }
            float yaw = (float) (Mth.atan2(step.z, step.x)
                    * Mth.RAD_TO_DEG) - 90.0F;
            if (SET_MANUAL_YAW != null) SET_MANUAL_YAW.invoke(statue, yaw);
            if (SNAP_MOVE != null) SNAP_MOVE.invoke(statue, step);
            else statue.setPos(statue.getX() + step.x,
                    statue.getY() + step.y, statue.getZ() + step.z);
            statue.getNavigation().stop();
            if (HARD_STOP != null) HARD_STOP.invoke(statue);
            else statue.setDeltaMovement(Vec3.ZERO);
        } catch (ReflectiveOperationException exception) {
            warnReflection(exception);
            if (canMoveBy(statue, step)) {
                statue.setPos(statue.getX() + step.x,
                        statue.getY() + step.y, statue.getZ() + step.z);
                statue.setDeltaMovement(Vec3.ZERO);
            }
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
        ScpAdditionsMod.LOGGER.warn(
                "SCP-173 strategic pursuit could not access movement internals",
                exception);
    }

    private static final class PursuitState {
        private final List<Vec3> route = new ArrayList<>();
        private net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>
                lastLevel;
        private UUID targetId;
        private Vec3 lastPosition;
        private Vec3 lastTargetPosition;
        private int routeIndex;
        private int stallAttempts;
        private long routeExpiresGameTime = Long.MIN_VALUE;
        private long nextReplanGameTime;
        private double routeEndpointDistanceSqr = Double.POSITIVE_INFINITY;

        private void reset(Vec3 position) {
            route.clear();
            targetId = null;
            lastPosition = position;
            lastTargetPosition = null;
            routeIndex = 0;
            stallAttempts = 0;
            routeExpiresGameTime = Long.MIN_VALUE;
            nextReplanGameTime = 0L;
            routeEndpointDistanceSqr = Double.POSITIVE_INFINITY;
        }
    }
}
