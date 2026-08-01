package net.mcreator.scpadditions.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.BlinkClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps SCP-173's server position, collision and rendered pose under one strict
 * rule: observed statues do not move, and every unobserved snap must traverse a
 * completely collision-free segment. Two END passes validate both the entity's
 * own movement and the later strategic-pursuit repair pass.
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

    /** Client render lock that rejects late server interpolation while watched. */
    @Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
    public static final class ClientVisualLock {
        private static final Map<UUID, VisualSnapshot> LOCKS = new HashMap<>();
        private static final Method CLIENT_OBSERVED = method(
                "isClientObservedByLocalPlayer");

        private ClientVisualLock() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            net.minecraft.client.Minecraft minecraft =
                    net.minecraft.client.Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                LOCKS.clear();
                return;
            }

            Set<UUID> present = new HashSet<>();
            for (Scp173Entity statue : minecraft.level.getEntitiesOfClass(
                    Scp173Entity.class,
                    minecraft.player.getBoundingBox().inflate(128.0D))) {
                present.add(statue.getUUID());
                enforce(statue);
            }
            LOCKS.keySet().removeIf(id -> !present.contains(id));
        }

        @SubscribeEvent
        public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
            if (event.getEntity() instanceof Scp173Entity statue) {
                enforce(statue);
            }
        }

        private static void enforce(Scp173Entity statue) {
            boolean observed = isLocallyObserved(statue);
            if (!observed) {
                LOCKS.remove(statue.getUUID());
                return;
            }

            VisualSnapshot lock = LOCKS.computeIfAbsent(statue.getUUID(),
                    ignored -> new VisualSnapshot(statue.getX(), statue.getY(),
                            statue.getZ(), statue.getYRot()));
            statue.absMoveTo(lock.x(), lock.y(), lock.z(), lock.yaw(), 0.0F);
            statue.setDeltaMovement(Vec3.ZERO);
        }

        private static boolean isLocallyObserved(Scp173Entity statue) {
            if (BlinkClient.isBlinkClosedLocally()) return false;
            if (CLIENT_OBSERVED != null) {
                try {
                    return (boolean) CLIENT_OBSERVED.invoke(statue);
                } catch (ReflectiveOperationException exception) {
                    warnReflection(exception);
                }
            }
            net.minecraft.client.Minecraft minecraft =
                    net.minecraft.client.Minecraft.getInstance();
            return minecraft.player != null
                    && statue.isObservedBy(minecraft.player);
        }

        private record VisualSnapshot(double x, double y, double z,
                                      float yaw) {
        }
    }
}
