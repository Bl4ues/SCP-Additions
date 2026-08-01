package net.mcreator.scpadditions.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gives reachable players absolute pursuit priority before SCP-173's normal
 * entity tick. A newly opened direct corridor invalidates any remembered detour
 * immediately, so the statue cannot remain committed to a stale plan while a
 * blinking player is standing on an unobstructed route.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class Scp173PlayerPriorityController {
    private static final double SEARCH_RANGE = 96.0D;
    private static final double SEARCH_RANGE_SQR = SEARCH_RANGE * SEARCH_RANGE;
    private static final double DIRECT_CORRIDOR_STEP = 0.03125D;
    private static final double DIRECT_CORRIDOR_MAX_HEIGHT_DELTA = 0.80D;
    private static final double CONTACT_MARGIN = 0.82D;

    private static final Map<UUID, PriorityState> STATES = new HashMap<>();
    private static final Method CLEAR_STRATEGIC_ROUTE = method(
            "clearStrategicRoute");
    private static boolean reflectionWarningLogged;

    private Scp173PlayerPriorityController() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START
                || !(event.level instanceof ServerLevel level)) {
            return;
        }

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Scp173Entity statue) {
                prioritizePlayer(level, statue);
            }
        }
    }

    /**
     * Runs in the same server task that receives the blink input. This removes
     * the old one-tick loophole where a statue targeting something else ignored
     * the newly vulnerable player until its next ordinary target evaluation.
     */
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
            boolean directCorridor = hasClearDirectCorridor(statue, player);
            if (!directCorridor && !hasReachablePath(statue, player)) {
                continue;
            }

            forcePlayerTarget(statue, player, directCorridor);
        }
    }

    private static void prioritizePlayer(ServerLevel level,
            Scp173Entity statue) {
        PriorityState state = STATES.computeIfAbsent(statue.getUUID(),
                ignored -> new PriorityState());

        if (!ScpAdditionsModulesConfig.get().scp173.enabled
                || !statue.isAlive() || statue.isRemoved()
                || !statue.isActivated()) {
            state.reset();
            return;
        }

        Player directPlayer = null;
        double directDistance = Double.MAX_VALUE;
        Player reachablePlayer = null;
        double reachableDistance = Double.MAX_VALUE;

        for (Player player : level.players()) {
            if (!isValidPlayer(player)) continue;
            double distance = statue.distanceToSqr(player);
            if (distance > SEARCH_RANGE_SQR) continue;

            if (hasClearDirectCorridor(statue, player)) {
                if (distance < directDistance) {
                    directDistance = distance;
                    directPlayer = player;
                }
                continue;
            }

            if (distance < reachableDistance
                    && hasReachablePath(statue, player)) {
                reachableDistance = distance;
                reachablePlayer = player;
            }
        }

        Player chosen = directPlayer != null ? directPlayer : reachablePlayer;
        if (chosen == null) {
            state.directCorridor = false;
            return;
        }

        forcePlayerTarget(statue, chosen, chosen == directPlayer);
    }

    private static void forcePlayerTarget(Scp173Entity statue, Player chosen,
            boolean directCorridor) {
        PriorityState state = STATES.computeIfAbsent(statue.getUUID(),
                ignored -> new PriorityState());
        LivingEntity previous = statue.getTarget();
        boolean targetChanged = previous == null
                || !previous.getUUID().equals(chosen.getUUID());
        boolean directRouteOpened = directCorridor
                && (!state.directCorridor
                || state.targetId == null
                || !state.targetId.equals(chosen.getUUID()));

        if (targetChanged) statue.setTarget(chosen);
        if (targetChanged || directRouteOpened) {
            clearRememberedRoute(statue);
            statue.getNavigation().stop();
        }

        state.targetId = chosen.getUUID();
        state.directCorridor = directCorridor;
    }

    private static boolean hasReachablePath(Scp173Entity statue,
            Player player) {
        Path path = statue.getNavigation().createPath(player, 0);
        return path != null && path.canReach();
    }

    private static boolean hasClearDirectCorridor(Scp173Entity statue,
            Player player) {
        Vec3 from = statue.position();
        Vec3 to = player.position();
        if (Math.abs(to.y - from.y) > DIRECT_CORRIDOR_MAX_HEIGHT_DELTA) {
            return false;
        }

        Vec3 horizontal = new Vec3(to.x - from.x, 0.0D, to.z - from.z);
        double distance = horizontal.length();
        if (distance <= CONTACT_MARGIN) return true;
        Vec3 direction = horizontal.scale(1.0D / distance);
        double travel = Math.max(0.0D, distance - CONTACT_MARGIN);

        for (double offset = DIRECT_CORRIDOR_STEP;
                offset <= travel; offset += DIRECT_CORRIDOR_STEP) {
            Vec3 movement = direction.scale(offset);
            if (!statue.level().noCollision(statue,
                    statue.getBoundingBox().move(movement))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidPlayer(Player player) {
        return player != null && player.isAlive()
                && !player.isCreative() && !player.isSpectator();
    }

    private static void clearRememberedRoute(Scp173Entity statue) {
        if (CLEAR_STRATEGIC_ROUTE == null) return;
        try {
            CLEAR_STRATEGIC_ROUTE.invoke(statue);
        } catch (ReflectiveOperationException exception) {
            if (!reflectionWarningLogged) {
                reflectionWarningLogged = true;
                ScpAdditionsMod.LOGGER.warn(
                        "Could not clear SCP-173's stale pursuit route",
                        exception);
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

    private static final class PriorityState {
        private UUID targetId;
        private boolean directCorridor;

        private void reset() {
            targetId = null;
            directCorridor = false;
        }
    }
}
