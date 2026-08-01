package net.mcreator.scpadditions.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps SCP-173's server position and collision under one strict rule: observed
 * statues do not move, and every unobserved snap must traverse a completely
 * collision-free segment. Two END passes validate both the entity's own motion
 * and the later strategic-pursuit repair pass.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class Scp173MovementConsistencyController {
    private static final double MOVEMENT_EPSILON_SQR = 0.001D * 0.001D;
    private static final double SWEEP_SAMPLE_DISTANCE = 0.03125D;

    private static final Map<UUID, ServerSnapshot> SERVER_SNAPSHOTS =
            new HashMap<>();
    private static final Method OBSERVATION_LOCKED = method(
            "isObservationLocked");
    private static final Method HARD_STOP = method("hardStopLocalMovement");
    private static final Field SCRAPING = field("SCRAPING");
    private static boolean reflectionWarningLogged;

    private Scp173MovementConsistencyController() {
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
            SERVER_SNAPSHOTS.put(statue.getUUID(),
                    new ServerSnapshot(statue.getX(), statue.getY(),
                            statue.getZ(), statue.getYRot(),
                            isObservationLocked(statue)));
        }
        SERVER_SNAPSHOTS.keySet().removeIf(id -> !present.contains(id));
    }

    /**
     * Runs before the normal-priority strategic controller. If the entity's own
     * tick tunneled across a thin door, restore it so pathfinding receives an
     * honest stalled position and can choose the doorway instead.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLevelTickEndBeforeStrategy(
            TickEvent.LevelTickEvent event) {
        validateLevelEnd(event);
    }

    /** Validates any corrective movement performed by later END-tick handlers. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLevelTickEndAfterStrategy(
            TickEvent.LevelTickEvent event) {
        validateLevelEnd(event);
    }

    private static void validateLevelEnd(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)) {
            return;
        }

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Scp173Entity statue)) continue;
            ServerSnapshot snapshot = SERVER_SNAPSHOTS.get(statue.getUUID());
            if (snapshot == null || !statue.isAlive() || statue.isRemoved()) {
                continue;
            }

            Vec3 start = snapshot.position();
            Vec3 end = statue.position();
            Vec3 movement = end.subtract(start);
            boolean moved = movement.lengthSqr() > MOVEMENT_EPSILON_SQR;
            boolean observed = snapshot.observedAtStart()
                    || isObservationLocked(statue);
            boolean crossedCollision = moved
                    && !isClearSweep(level, statue, start, movement);

            if (observed || crossedCollision) {
                restore(statue, snapshot);
                continue;
            }
            if (!moved) setScraping(statue, false);
        }
    }

    private static boolean isClearSweep(ServerLevel level,
            Scp173Entity statue, Vec3 start, Vec3 movement) {
        double length = movement.length();
        if (length <= 0.001D) return true;

        int samples = Math.max(1, (int) Math.ceil(length
                / SWEEP_SAMPLE_DISTANCE));
        Vec3 current = statue.position();
        for (int sample = 1; sample <= samples; sample++) {
            Vec3 position = start.add(movement.scale(
                    sample / (double) samples));
            Vec3 offset = position.subtract(current);
            if (!level.noCollision(statue,
                    statue.getBoundingBox().move(offset))) {
                return false;
            }
        }
        return true;
    }

    private static void restore(Scp173Entity statue,
            ServerSnapshot snapshot) {
        statue.absMoveTo(snapshot.x(), snapshot.y(), snapshot.z(),
                snapshot.yaw(), 0.0F);
        statue.getNavigation().stop();
        statue.getMoveControl().setWantedPosition(snapshot.x(), snapshot.y(),
                snapshot.z(), 0.0D);
        statue.setDeltaMovement(Vec3.ZERO);
        setScraping(statue, false);
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
                "SCP-173 movement consistency guard lost internal access",
                exception);
    }

    private record ServerSnapshot(double x, double y, double z, float yaw,
                                  boolean observedAtStart) {
        private Vec3 position() {
            return new Vec3(x, y, z);
        }
    }
}
