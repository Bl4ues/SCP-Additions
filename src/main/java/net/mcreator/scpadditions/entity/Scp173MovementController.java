package net.mcreator.scpadditions.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Single server-side authority around SCP-173's own movement implementation.
 * The entity remains the primary mover. This controller only rejects illegal
 * movement and repairs a genuinely stalled pursuit with one collision-swept
 * path step. It replaces the previous stack of independent controllers that
 * could alternately move, restore and visually pull the statue between poses.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class Scp173MovementController {
    private static final double SEARCH_RANGE = 96.0D;
    private static final double SEARCH_RANGE_SQR = SEARCH_RANGE * SEARCH_RANGE;
    private static final double MOVEMENT_EPSILON_SQR = 0.001D * 0.001D;
    private static final double MIN_USEFUL_STEP = 0.035D;
    private static final double DIRECT_STEP = 1.20D;
    private static final double BLINK_STEP = 0.95D;
    private static final double AUTOMATIC_BLINK_DISTANCE = 6.0D;
    private static final double STOP_DISTANCE = 0.72D;
    private static final double NODE_REACHED_SQR = 0.52D * 0.52D;
    private static final double SWEEP_SAMPLE_DISTANCE = 0.03125D;
    private static final double MAX_UP_STEP = 1.05D;
    private static final double MAX_DOWN_STEP = 1.00D;
    private static final int MAX_PATH_NODES = 128;

    private static final Map<UUID, Snapshot> SNAPSHOTS = new HashMap<>();

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
            SNAPSHOTS.put(statue.getUUID(), new Snapshot(level.dimension(),
                    statue.getX(), statue.getY(), statue.getZ(),
                    statue.getYRot(), isObservationLocked(statue)));

            if (statue.isActivated()
                    && ScpAdditionsModulesConfig.get().scp173.enabled) {
                prioritizeBestTarget(level, statue);
            }
        }

        SNAPSHOTS.entrySet().removeIf(entry ->
                entry.getValue().dimension().equals(level.dimension())
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

    /** Called from the blink packet before SCP-173 processes the blink itself. */
    public static void prioritizeBlinkingPlayer(ServerPlayer player) {
        if (!isValidPlayer(player)
                || !ScpAdditionsModulesConfig.get().scp173.enabled) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(SEARCH_RANGE);
        for (Scp173Entity statue : player.serverLevel().getEntitiesOfClass(
                Scp173Entity.class, area,
                entity -> entity.isAlive() && entity.isActivated()
                        && entity.distanceToSqr(player)
                        <= SEARCH_RANGE_SQR)) {
            Path path = statue.getNavigation().createPath(player, 0);
            if (!hasClearDirectCorridor(statue, player)
                    && path == null) {
                continue;
            }
            setTarget(statue, player);
        }
    }

    private static void validateAndRepair(ServerLevel level,
            Scp173Entity statue) {
        Snapshot snapshot = SNAPSHOTS.get(statue.getUUID());
        if (snapshot == null || !statue.isAlive() || statue.isRemoved()) {
            return;
        }

        if (!ScpAdditionsModulesConfig.get().scp173.enabled
                || !statue.isActivated()) {
            setScraping(statue, false);
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

        if (moved && !isClearSweep(level, statue, start, movement)) {
            restore(statue, snapshot);
            moved = false;
        }

        // The entity's native movement remains authoritative when it produced a
        // legal displacement. A repair step is used only after a real stall or
        // after an illegal tunnelling attempt was rolled back.
        if (moved) return;

        LivingEntity target = prioritizeBestTarget(level, statue);
        if (target == null) {
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

        Vec3 step = chooseRepairStep(statue, target, maximumStep);
        if (!isUseful(step)) {
            setScraping(statue, false);
            stopAtCurrentPosition(statue);
            return;
        }

        Vec3 before = statue.position();
        applyStep(statue, target, step);
        double travelled = statue.position().distanceTo(before);
        if (travelled <= MIN_USEFUL_STEP) {
            setScraping(statue, false);
            stopAtCurrentPosition(statue);
            return;
        }

        setScraping(statue, true);
        consumeBlinkBudget(statue, target, travelled);
    }

    private static LivingEntity prioritizeBestTarget(ServerLevel level,
            Scp173Entity statue) {
        Player directPlayer = null;
        double directDistance = Double.MAX_VALUE;
        Player reachablePlayer = null;
        double reachableDistance = Double.MAX_VALUE;
        AABB area = statue.getBoundingBox().inflate(SEARCH_RANGE);

        for (Player player : level.getEntitiesOfClass(Player.class, area,
                Scp173MovementController::isValidPlayer)) {
            double distance = statue.distanceToSqr(player);
            if (distance > SEARCH_RANGE_SQR) continue;

            if (hasClearDirectCorridor(statue, player)) {
                if (distance < directDistance) {
                    directDistance = distance;
                    directPlayer = player;
                }
                continue;
            }

            Path path = statue.getNavigation().createPath(player, 0);
            if (path != null && path.canReach()
                    && distance < reachableDistance) {
                reachableDistance = distance;
                reachablePlayer = player;
            }
        }

        Player playerChoice = directPlayer != null
                ? directPlayer : reachablePlayer;
        if (playerChoice != null) {
            setTarget(statue, playerChoice);
            return playerChoice;
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
    }

    private static Vec3 chooseRepairStep(Scp173Entity statue,
            LivingEntity target, double maximumStep) {
        Vec3 directDelta = target.position().subtract(statue.position());
        Vec3 directHorizontal = new Vec3(directDelta.x, 0.0D,
                directDelta.z);
        double directDistance = directHorizontal.length();
        if (directDistance <= STOP_DISTANCE) return Vec3.ZERO;

        double directTravel = Math.min(maximumStep,
                directDistance - STOP_DISTANCE);
        Vec3 direct = directHorizontal.scale(1.0D / directDistance)
                .scale(directTravel);
        if (canSweepBy(statue, direct)) return direct;

        Path path = statue.getNavigation().createPath(target, 0);
        Vec3 pathStep = firstPathStep(statue, path, maximumStep);
        if (isUseful(pathStep)) return pathStep;

        // Even a sealed door has a useful staging point. Move only through the
        // collision-free part of the direct segment and then wait at the frame.
        return largestClearStep(statue, direct);
    }

    private static Vec3 firstPathStep(Scp173Entity statue, Path path,
            double maximumStep) {
        if (path == null || path.isDone()) return Vec3.ZERO;

        int safety = 0;
        while (!path.isDone() && safety++ < MAX_PATH_NODES) {
            Vec3 waypoint = path.getNextEntityPos(statue);
            path.advance();
            if (distanceSqr(statue.position(), waypoint)
                    <= NODE_REACHED_SQR) {
                continue;
            }
            return stepTowardWaypoint(statue, waypoint, maximumStep);
        }
        return Vec3.ZERO;
    }

    private static Vec3 stepTowardWaypoint(Scp173Entity statue,
            Vec3 waypoint, double maximumStep) {
        Vec3 delta = waypoint.subtract(statue.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        double horizontalLength = horizontal.length();

        if (horizontalLength <= 0.001D) {
            return largestClearStep(statue, new Vec3(0.0D,
                    Mth.clamp(delta.y, -MAX_DOWN_STEP, MAX_UP_STEP), 0.0D));
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
        for (int attempt = 0; attempt < 12; attempt++) {
            double middle = (low + high) * 0.5D;
            if (canSweepBy(statue, desired.scale(middle))) low = middle;
            else high = middle;
        }

        Vec3 shortened = desired.scale(low);
        if (isUseful(shortened)) return shortened;

        // Path nodes can approach a one-block doorway diagonally. Aligning one
        // axis at a time lets the statue centre itself without crossing a frame.
        Vec3 xOnly = new Vec3(desired.x, desired.y, 0.0D);
        Vec3 zOnly = new Vec3(0.0D, desired.y, desired.z);
        Vec3 first = Math.abs(desired.x) >= Math.abs(desired.z)
                ? xOnly : zOnly;
        Vec3 second = first == xOnly ? zOnly : xOnly;
        if (isUseful(first) && canSweepBy(statue, first)) return first;
        if (isUseful(second) && canSweepBy(statue, second)) return second;
        return Vec3.ZERO;
    }

    private static boolean hasClearDirectCorridor(Scp173Entity statue,
            Player player) {
        Vec3 delta = player.position().subtract(statue.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        double distance = horizontal.length();
        if (distance <= STOP_DISTANCE) return true;
        if (Math.abs(delta.y) > MAX_UP_STEP) return false;
        Vec3 travel = horizontal.scale(1.0D / distance)
                .scale(Math.max(0.0D, distance - STOP_DISTANCE));
        return canSweepBy(statue, travel);
    }

    private static boolean canSweepBy(Scp173Entity statue, Vec3 movement) {
        if (!isUseful(movement)) return false;
        return isClearSweep(statue.level(), statue, statue.position(), movement);
    }

    private static boolean isClearSweep(Level level, Scp173Entity statue,
            Vec3 start, Vec3 movement) {
        double length = movement.length();
        if (length <= 0.001D) return true;

        Vec3 current = statue.position();
        AABB startBox = statue.getBoundingBox().move(start.subtract(current));
        int samples = Math.max(1, (int) Math.ceil(length
                / SWEEP_SAMPLE_DISTANCE));
        for (int sample = 1; sample <= samples; sample++) {
            Vec3 partial = movement.scale(sample / (double) samples);
            if (!level.noCollision(statue, startBox.move(partial))) {
                return false;
            }
        }
        return true;
    }

    private static void applyStep(Scp173Entity statue, LivingEntity target,
            Vec3 step) {
        if (!canSweepBy(statue, step)) return;

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

    private static double distanceSqr(Vec3 first, Vec3 second) {
        return first == null || second == null
                ? Double.POSITIVE_INFINITY : first.distanceToSqr(second);
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
        ScpAdditionsMod.LOGGER.warn(
                "SCP-173 movement controller lost internal access",
                exception);
    }

    private record Snapshot(ResourceKey<Level> dimension, double x, double y,
                            double z, float yaw, boolean observedAtStart) {
        private Vec3 position() {
            return new Vec3(x, y, z);
        }
    }
}
