package net.mcreator.scpadditions.effect;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.equipment.HazmatSuitAccess;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Protects a complete sealed Hazmat Suit from externally applied potion effects.
 *
 * <p>Forge's per-effect applicability event does not expose the effect source.
 * Splash impacts are therefore marked immediately before vanilla applies their
 * contents, while active area-effect clouds are detected spatially. This keeps
 * command effects, internal SCP effects, and deliberately granted buffs working
 * unless their own system explicitly checks sealed protection.</p>
 */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class HazmatExternalEffectEvents {
    private static final double SPLASH_RADIUS = 4.25D;
    private static final double CLOUD_SEARCH_RADIUS = 8.0D;
    private static final Map<UUID, Long> SPLASH_PROTECTION_TICKS =
            new HashMap<>();

    private HazmatExternalEffectEvents() {
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownPotion potion)
                || potion.level().isClientSide) {
            return;
        }

        Vec3 impact = event.getRayTraceResult().getLocation();
        AABB affectedArea = new AABB(impact, impact).inflate(SPLASH_RADIUS);
        long protectedThrough = potion.level().getGameTime() + 1L;
        for (ServerPlayer player : potion.level().getEntitiesOfClass(
                ServerPlayer.class, affectedArea,
                candidate -> HazmatSuitAccess.providesSealedProtection(candidate))) {
            SPLASH_PROTECTION_TICKS.put(player.getUUID(), protectedThrough);
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player
                && isProtectedExternalPotionContext(player)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onPotionDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !isProtectedExternalPotionContext(player)) {
            return;
        }

        if (event.getSource().is(DamageTypes.INDIRECT_MAGIC)
                || event.getSource().is(DamageTypes.MAGIC)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPotionHealing(LivingHealEvent event) {
        if (event.getEntity() instanceof Player player
                && isProtectedExternalPotionContext(player)) {
            event.setAmount(0.0F);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SPLASH_PROTECTION_TICKS.remove(event.getEntity().getUUID());
    }

    private static boolean isProtectedExternalPotionContext(Player player) {
        if (player == null
                || !HazmatSuitAccess.providesSealedProtection(player)) {
            return false;
        }

        long currentTick = player.level().getGameTime();
        long markedThrough = SPLASH_PROTECTION_TICKS.getOrDefault(
                player.getUUID(), Long.MIN_VALUE);
        if (markedThrough >= currentTick) {
            return true;
        }
        SPLASH_PROTECTION_TICKS.remove(player.getUUID());
        return isInsideActiveEffectCloud(player);
    }

    private static boolean isInsideActiveEffectCloud(Player player) {
        AABB search = player.getBoundingBox().inflate(CLOUD_SEARCH_RADIUS);
        for (AreaEffectCloud cloud : player.level().getEntitiesOfClass(
                AreaEffectCloud.class, search, AreaEffectCloud::isAlive)) {
            double radius = Math.max(0.0D, cloud.getRadius()) + 1.0D;
            if (cloud.distanceToSqr(player) <= radius * radius) {
                return true;
            }
        }
        return false;
    }
}
