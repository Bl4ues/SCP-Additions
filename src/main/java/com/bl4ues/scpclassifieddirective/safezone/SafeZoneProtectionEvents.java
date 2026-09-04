package com.bl4ues.scpclassifieddirective.safezone;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.entity.Scp106Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp939Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

/** Enforces Safe Zones at spawn, targeting and physical-boundary layers. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SafeZoneProtectionEvents {
    private static final double REPLACEMENT_TARGET_RANGE = 64.0D;
    private static final double OUTSIDE_EPSILON = 0.075D;
    private static final Map<Mob, Vec3> LAST_OUTSIDE_POSITION =
            new WeakHashMap<>();

    private SafeZoneProtectionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpawnPlacement(
            MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getEntityType().getCategory() != MobCategory.MONSTER) return;
        ServerLevel level = event.getLevel().getLevel();
        if (SafeZoneManager.contains(level, event.getPos())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpawnPosition(MobSpawnEvent.PositionCheck event) {
        if (!isHostile(event.getEntity())) return;
        ServerLevel level = event.getLevel().getLevel();
        BlockPos pos = BlockPos.containing(event.getX(), event.getY(),
                event.getZ());
        if (SafeZoneManager.contains(level, pos)
                || SafeZoneManager.intersects(level,
                        event.getEntity().getBoundingBox())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof Mob mob)
                || !isHostile(mob)) {
            return;
        }
        if (SafeZoneManager.intersects(level, mob.getBoundingBox())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTargetChange(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !isHostile(mob)
                || !(event.getNewTarget() instanceof Player player)
                || !SafeZoneManager.isInside(player)) {
            return;
        }
        event.setNewTarget(nearestReplacement(mob));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level)
                || !isHostile(mob) || !mob.isAlive()) {
            return;
        }

        boolean inside = SafeZoneManager.intersects(level,
                mob.getBoundingBox());
        if (!inside) {
            LAST_OUTSIDE_POSITION.put(mob, mob.position());
            if (mob.getTarget() instanceof Player player
                    && SafeZoneManager.isInside(player)) {
                handleProtectedTarget(mob);
            }
            return;
        }

        Vec3 safePosition = LAST_OUTSIDE_POSITION.get(mob);
        if (safePosition == null) {
            safePosition = nearestOutsidePosition(level, mob);
        }
        if (safePosition != null) {
            mob.teleportTo(safePosition.x, safePosition.y, safePosition.z);
            mob.setDeltaMovement(Vec3.ZERO);
            mob.getNavigation().stop();
        }

        ServerPlayer replacement = nearestReplacement(mob);
        if (replacement != null) {
            if (mob instanceof Scp939Entity scp939) {
                scp939.cancelSafeZoneRetreat();
            }
            mob.setTarget(replacement);
            return;
        }
        retreatOrDiscard(mob);
    }

    private static void handleProtectedTarget(Mob mob) {
        ServerPlayer replacement = nearestReplacement(mob);
        if (replacement != null) {
            mob.setTarget(replacement);
            return;
        }
        mob.setTarget(null);
        retreatOrDiscard(mob);
    }

    private static void retreatOrDiscard(Mob mob) {
        if (mob instanceof Scp106Entity scp106) {
            scp106.retreatFromSafeZone();
        } else if (mob instanceof Scp173Entity scp173) {
            scp173.beginSafeZoneRetreat();
        } else if (mob instanceof Scp939Entity scp939) {
            scp939.beginSafeZoneRetreat();
        } else {
            mob.discard();
        }
    }

    private static ServerPlayer nearestReplacement(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return null;
        ServerPlayer nearest = null;
        double bestDistance = REPLACEMENT_TARGET_RANGE
                * REPLACEMENT_TARGET_RANGE;
        for (ServerPlayer player : level.players()) {
            if (!isEligible(player)) continue;
            double distance = mob.distanceToSqr(player);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private static boolean isEligible(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isRemoved()
                && !player.isCreative() && !player.isSpectator()
                && !SafeZoneManager.isInside(player);
    }

    private static boolean isHostile(Entity entity) {
        return entity != null
                && entity.getType().getCategory() == MobCategory.MONSTER;
    }

    private static Vec3 nearestOutsidePosition(ServerLevel level, Mob mob) {
        SafeZone zone = SafeZoneManager.findIntersecting(level,
                mob.getBoundingBox());
        if (zone == null) return null;
        AABB bounds = zone.bounds();
        AABB entityBounds = mob.getBoundingBox();
        double halfWidth = entityBounds.getXsize() * 0.5D;
        double left = bounds.minX - halfWidth - OUTSIDE_EPSILON;
        double right = bounds.maxX + halfWidth + OUTSIDE_EPSILON;
        double front = bounds.minZ - halfWidth - OUTSIDE_EPSILON;
        double back = bounds.maxZ + halfWidth + OUTSIDE_EPSILON;

        Vec3 current = mob.position();
        Vec3 best = new Vec3(left, current.y, current.z);
        double bestDistance = current.distanceToSqr(best);
        Vec3[] candidates = {
                new Vec3(right, current.y, current.z),
                new Vec3(current.x, current.y, front),
                new Vec3(current.x, current.y, back)
        };
        for (Vec3 candidate : candidates) {
            double distance = current.distanceToSqr(candidate);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }
}
