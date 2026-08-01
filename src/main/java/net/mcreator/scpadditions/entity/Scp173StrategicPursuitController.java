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
 * Repairs a stalled SCP-173 movement opportunity after the entity's own tick.
 * Fresh shortest-path nodes always outrank remembered detours, including during
 * automatic blinks. The corrective step consumes the same six-block automatic
 * blink budget instead of granting additional travel.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class Scp173StrategicPursuitController {
    private static final double SEARCH_RANGE = 48.0D;
    private static final double SEARCH_RANGE_SQR = SEARCH_RANGE * SEARCH_RANGE;
    private static final double MOVEMENT_EPSILON_SQR = 0.035D * 0.035D;
    private static final double NODE_REACHED_SQR = 0.52D * 0.52D;
    private static final double MIN_USEFUL_STEP = 0.045D;
    private static final double DIRECT_STEP = 1.20D;
    private static final double BLINK_STEP = 0.95D;
    private static final double AUTOMATIC_BLINK_DISTANCE = 6.0D;
    private static final double STOP_DISTANCE = 0.72D;
    private static final double SWEEP_SAMPLE_DISTANCE = 0.16D;
    private static final double MAX_UP_STEP = 1.05D;
    private static final double MAX_DOWN_STEP = 1.00D;
    private static final int ROUTE_MEMORY_TICKS = 100;
    private static final int MAX_ROUTE_NODES = 128;

    private static final Map<UUID, PursuitState> STATES = new HashMap<>();

    private static final Method OBSERVATION_LOCKED = method(
            "isObservationLocked");
    private static final Method SNAP_MOVE = method("snapMove", Vec3.class);
    private static final Method SET_MANUAL_YAW = method(
            "setManualYaw", float.class);
    private static final Method HARD_STOP = method("hardStopLocalMovement");
    private static final Method CONSUME_AUTOMATIC_BLINK = method(
            "consumeAutomaticBlinkTravel", Player.class, double.class);
    private static final Field AUTOMATIC_BLINK_REMAINING = field(
            "automaticBlinkTravelRemaining");
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
        if (state.lastPosition == null) state.lastPosition = current;

        if (!ScpAdditionsModulesConfig.get().scp173.enabled
                || !statue.isAlive() || statue.isRemoved()
                || !statue.isActivated()) {
            setScraping(statue, false);
            state.reset(current);
            return;
        }

        LivingEntity target = resolveTarget(level, statue);
        if (target == null) {
            setScraping(statue, false);
            state.reset(current);
            return;
        }

        prepareTarget(state, target);
        boolean movedNormally = current.distanceToSqr(state.lastPosition)
                > MOVEMENT_EPSILON_SQR;
        state.lastPosition = current;
        if (movedNormally) {
            state.routeExpiresGameTime = level.getGameTime()
                    + ROUTE_MEMORY_TICKS;
            trimReachedNodes(statue.position(), state);
            return;
        }

        if (isObservationLocked(statue)) {
            setScraping(statue, false);
            return;
        }

        double maximumStep = maximumStep(statue, target);
        if (maximumStep <= MIN_USEFUL_STEP) {
            setScraping(statue, false);
            return;
        }

        long gameTime = level.getGameTime();
        Vec3 step = shortestFreshPathStep(statue, target, state,
                maximumStep, gameTime);
        if (!isUseful(step)) {
            step = rememberedRouteStep(statue, state, maximumStep, gameTime);
        }
        if (!isUseful(step)) {
            step = directStagingStep(statue, target, maximumStep);
        }

        if (!isUseful(step)) {
            setScraping(statue, false);
            statue.getNavigation().stop();
            hardStop(statue);
            return;
        }

        Vec3 before = statue.position();
        applyStrategicStep(statue, step);
        double moved = statue.position().distanceTo(before);
        if (moved <= MIN_USEFUL_STEP) {
            setScraping(statue, false);
            state.lastPosition = statue.position();
            return;
        }

        setScraping(statue, true);
        consumeBlinkBudget(target, statue, moved);
        state.lastPosition = statue.position();
        state.routeExpiresGameTime = gameTime + ROUTE_MEMORY_TICKS;
        trimReachedNodes(statue.position(), state);
    }

    private static LivingEntity resolveTarget(ServerLevel level,
            Scp173Entity statue) {
        Player nearestPlayer = null;
        double nearestPlayerDistance = Double.MAX_VALUE;
        AABB area = statue.getBoundingBox().inflate(SEARCH_RANGE);
        for (Player player : level.getEntitiesOfClass(Player.class, area,
                Scp173StrategicPursuitController::isValidPlayer)) {
            double distance = statue.distanceToSqr(player);
            if (distance <= SEARCH_RANGE_SQR
                    && distance < nearestPlayerDistance) {
                nearestPlayerDistance = distance;
                nearestPlayer = player;
            }
        }
        if (nearestPlayer != null) {
            if (statue.getTarget() != nearestPlayer) {
                statue.setTarget(nearestPlayer);
            }
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
        if (best != null) statue.setTarget(best);
        return best;
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

    private static void prepareTarget(PursuitState state,
            LivingEntity target) {
        if (target.getUUID().equals(state.targetId)) return;
        state.route.clear();
        state.routeIndex = 0;
        state.targetId = target.getUUID();
        state.lastTargetPosition = target.position();
        state.routeExpiresGameTime = Long.MIN_VALUE;
    }

    /**
     * Rebuilds the path on every stalled opportunity. This is deliberate: an
     * opened door must beat every remembered plan immediately, not after a
     * replan cooldown or a collection of failed collision attempts.
     */
    private static Vec3 shortestFreshPathStep(Scp173Entity statue,
            LivingEntity target, PursuitState state, double maximumStep,
            long gameTime) {
        Path path = statue.getNavigation().createPath(target, 0);
        List<Vec3> fresh = capturePath(statue, path);
        trimReachedNodes(statue.position(), fresh);
        if (fresh.isEmpty()) return Vec3.ZERO;

        state.route.clear();
        state.route.addAll(fresh);
        state.routeIndex = 0;
        state.lastTargetPosition = target.position();
        state.routeExpiresGameTime = gameTime + ROUTE_MEMORY_TICKS;
        return stepTowardWaypoint(statue, fresh.get(0), maximumStep);
    }

    private static List<Vec3> capturePath(Scp173Entity statue, Path path) {
        List<Vec3> nodes = new ArrayList<>();
        if (path == null || path.isDone()) return nodes;

        Vec3 previous = null;
        int safety = 0;
        while (!path.isDone() && safety++ < MAX_ROUTE_NODES) {
            Vec3 node = path.getNextEntityPos(statue);
            if (previous == null
                    || horizontalDistanceSqr(previous, node) > 0.01D
                    || Math.abs(previous.y - node.y) > 0.01D) {
                nodes.add(node);
                previous = node;
            }
            path.advance();
        }
        return nodes;
    }

    private static Vec3 rememberedRouteStep(Scp173Entity statue,
            PursuitState state, double maximumStep, long gameTime) {
        if (gameTime > state.routeExpiresGameTime) return Vec3.ZERO;
        trimReachedNodes(statue.position(), state);
        if (state.routeIndex >= state.route.size()) return Vec3.ZERO;
        return stepTowardWaypoint(statue,
                state.route.get(state.routeIndex), maximumStep);
    }

    private static Vec3 directStagingStep(Scp173Entity statue,
            LivingEntity target, double maximumStep) {
        Vec3 delta = target.position().subtract(statue.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        double distance = horizontal.length();
        if (distance <= STOP_DISTANCE) return Vec3.ZERO;
        double travel = Math.min(maximumStep, distance - STOP_DISTANCE);
        if (travel <= MIN_USEFUL_STEP) return Vec3.ZERO;
        return largestClearStep(statue,
                horizontal.scale(1.0D / distance).scale(travel));
    }

    private static Vec3 stepTowardWaypoint(Scp173Entity statue,
            Vec3 waypoint, double maximumStep) {
        Vec3 delta = waypoint.subtract(statue.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        double horizontalLength = horizontal.length();
        if (horizontalLength <= 0.001D) {
            double vertical = Mth.clamp(delta.y, -MAX_DOWN_STEP, MAX_UP_STEP);
            Vec3 verticalStep = new Vec3(0.0D, vertical, 0.0D);
            return largestClearStep(statue, verticalStep);
        }

        double horizontalTravel = Math.min(maximumStep, horizontalLength);
        double vertical = horizontalLength <= maximumStep * 1.25D
                ? Mth.clamp(delta.y, -MAX_DOWN_STEP, MAX_UP_STEP) : 0.0D;
        Vec3 desired = horizontal.scale(1.0D / horizontalLength)
                .scale(horizontalTravel).add(0.0D, vertical, 0.0D);
        return largestClearStep(statue, desired);
    }

    private static Vec3 largestClearStep(Scp173Entity statue,
            Vec3 desired) {
        if (!isUseful(desired)) return Vec3.ZERO;
        if (canSweepBy(statue, desired)) return desired;

        double low = 0.0D;
        double high = 1.0D;
        for (int attempt = 0; attempt < 10; attempt++) {
            double middle = (low + high) * 0.5D;
            Vec3 candidate = desired.scale(middle);
            if (canSweepBy(statue, candidate)) low = middle;
            else high = middle;
        }
        if (low > 0.0D) {
            Vec3 shortened = desired.scale(low);
            if (isUseful(shortened)) return shortened;
        }

        // A diagonal approach can catch one edge of a narrow doorway. Preserve
        // the pathfinder's intent but align one axis at a time toward its node.
        Vec3 xOnly = new Vec3(desired.x, desired.y, 0.0D);
        Vec3 zOnly = new Vec3(0.0D, desired.y, desired.z);
        Vec3 first = Math.abs(desired.x) >= Math.abs(desired.z)
                ? xOnly : zOnly;
        Vec3 second = first == xOnly ? zOnly : xOnly;
        if (isUseful(first) && canSweepBy(statue, first)) return first;
        if (isUseful(second) && canSweepBy(statue, second)) return second;
        return Vec3.ZERO;
    }

    private static boolean canSweepBy(Scp173Entity statue, Vec3 step) {
        if (!isUseful(step)) return false;
        int samples = Math.max(1, (int) Math.ceil(step.length()
                / SWEEP_SAMPLE_DISTANCE));
        for (int sample = 1; sample <= samples; sample++) {
            Vec3 partial = step.scale(sample / (double) samples);
            if (!statue.level().noCollision(statue,
                    statue.getBoundingBox().move(partial))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isUseful(Vec3 step) {
        return step != null && step.lengthSqr()
                > MIN_USEFUL_STEP * MIN_USEFUL_STEP;
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
        if (AUTOMATIC_BLINK_REMAINING == null) return 0.0D;
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

    private static void consumeBlinkBudget(LivingEntity target,
            Scp173Entity statue, double distance) {
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

    private static void trimReachedNodes(Vec3 position,
            PursuitState state) {
        while (state.routeIndex < state.route.size()
                && distanceSqr(position,
                        state.route.get(state.routeIndex))
                <= NODE_REACHED_SQR) {
            state.routeIndex++;
        }
    }

    private static void trimReachedNodes(Vec3 position, List<Vec3> nodes) {
        while (!nodes.isEmpty()
                && distanceSqr(position, nodes.get(0)) <= NODE_REACHED_SQR) {
            nodes.remove(0);
        }
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        if (first == null || second == null) return Double.POSITIVE_INFINITY;
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    private static double distanceSqr(Vec3 first, Vec3 second) {
        return first == null || second == null
                ? Double.POSITIVE_INFINITY : first.distanceToSqr(second);
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

    private static void applyStrategicStep(Scp173Entity statue, Vec3 step) {
        try {
            float yaw = (float) (Mth.atan2(step.z, step.x)
                    * Mth.RAD_TO_DEG) - 90.0F;
            if (SET_MANUAL_YAW != null) SET_MANUAL_YAW.invoke(statue, yaw);
            if (SNAP_MOVE != null) SNAP_MOVE.invoke(statue, step);
            else statue.setPos(statue.getX() + step.x,
                    statue.getY() + step.y, statue.getZ() + step.z);
            statue.getNavigation().stop();
            hardStop(statue);
        } catch (ReflectiveOperationException exception) {
            warnReflection(exception);
            if (canSweepBy(statue, step)) {
                statue.setPos(statue.getX() + step.x,
                        statue.getY() + step.y, statue.getZ() + step.z);
                statue.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    private static void hardStop(Scp173Entity statue) {
        if (HARD_STOP != null) {
            try {
                HARD_STOP.invoke(statue);
                return;
            } catch (ReflectiveOperationException exception) {
                warnReflection(exception);
            }
        }
        statue.setDeltaMovement(Vec3.ZERO);
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
        private long routeExpiresGameTime = Long.MIN_VALUE;

        private void reset(Vec3 position) {
            route.clear();
            targetId = null;
            lastPosition = position;
            lastTargetPosition = null;
            routeIndex = 0;
            routeExpiresGameTime = Long.MIN_VALUE;
        }
    }
}
