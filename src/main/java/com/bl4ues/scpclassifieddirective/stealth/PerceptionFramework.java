package com.bl4ues.scpclassifieddirective.stealth;

import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Global visual-perception framework for hostile AI.
 *
 * <p>Vanilla goals still decide who a mob wants to attack. This service only
 * decides when a visual target has accumulated enough evidence to be acquired.
 * It intentionally exposes modifiers and per-player overrides so smoke,
 * lockers, anomalous invisibility and future sensory systems can participate
 * without replacing the AI layer again.</p>
 */
public final class PerceptionFramework {
    private static final Map<Mob, Observation> OBSERVATIONS = new WeakHashMap<>();
    private static final Map<Player, Double> VISIBILITY_OVERRIDES = new WeakHashMap<>();
    private static final List<VisibilityModifier> MODIFIERS =
            new CopyOnWriteArrayList<>();
    private static final long OBSERVATION_FORGET_TICKS = 20L;

    private PerceptionFramework() {
    }

    public static boolean canAcquire(Mob observer, Player target) {
        if (observer == null || target == null || !target.isAlive()
                || target.isCreative() || target.isSpectator()) {
            forget(observer, target);
            return false;
        }

        ScpClassifiedDirectiveModulesConfig.Stealth settings =
                ScpClassifiedDirectiveModulesConfig.get().stealth;
        if (!settings.enabled) return true;

        ScpClassifiedDirectiveModulesConfig.PerceptionRule rule = ruleFor(observer);
        if (rule != null && rule.omniscient) return true;
        if (rule != null && rule.blind) {
            forget(observer, target);
            return false;
        }

        // A mob that was directly attacked has no need to politely pretend it
        // did not notice the attacker for another stealth acquisition window.
        LivingEntity lastAttacker = observer.getLastHurtByMob();
        if (lastAttacker == target) return true;

        if (!observer.getSensing().hasLineOfSight(target)) {
            forget(observer, target);
            return false;
        }

        double visibility = visibility(observer, target, rule, settings);
        if (visibility <= 0.0D) {
            forget(observer, target);
            return false;
        }

        double distance = observer.distanceTo(target);
        double followRange = Math.max(1.0D,
                observer.getAttributeValue(Attributes.FOLLOW_RANGE));
        double rangeMultiplier = rule == null ? 1.0D : rule.rangeMultiplier;
        double visualRange = followRange * visibility * rangeMultiplier;
        visualRange = Math.max(settings.minimumCloseRange, visualRange);
        if (distance > visualRange) {
            forget(observer, target);
            return false;
        }

        // At near-total darkness, being somewhere inside a mob's vanilla 360°
        // target scan is not enough. The player must cross its forward field or
        // approach the close-detection radius.
        int light = target.level().getMaxLocalRawBrightness(target.blockPosition());
        boolean nightVision = rule != null && rule.nightVision;
        if (!nightVision && light <= 1 && distance > settings.minimumCloseRange) {
            var toward = target.getEyePosition().subtract(observer.getEyePosition());
            if (toward.lengthSqr() > 1.0E-5D) {
                double facing = observer.getViewVector(1.0F).normalize()
                        .dot(toward.normalize());
                if (facing < 0.15D) {
                    forget(observer, target);
                    return false;
                }
            }
        }

        double delayMultiplier = rule == null ? 1.0D
                : rule.acquireDelayMultiplier;
        int requiredTicks = (int) Math.ceil((1.0D - visibility)
                * settings.maxAcquireDelayTicks * delayMultiplier);
        if (requiredTicks <= 0) {
            forget(observer, target);
            return true;
        }

        long now = observer.level().getGameTime();
        Observation observation = OBSERVATIONS.get(observer);
        UUID targetId = target.getUUID();
        if (observation == null || !targetId.equals(observation.targetId)
                || now - observation.lastSeenTick > OBSERVATION_FORGET_TICKS) {
            observation = new Observation(targetId, now, now);
            OBSERVATIONS.put(observer, observation);
            return false;
        }
        observation.lastSeenTick = now;
        return now - observation.firstSeenTick >= requiredTicks;
    }

    public static double visibility(Mob observer, Player target) {
        ScpClassifiedDirectiveModulesConfig.Stealth settings =
                ScpClassifiedDirectiveModulesConfig.get().stealth;
        return visibility(observer, target, ruleFor(observer), settings);
    }

    private static double visibility(Mob observer, Player target,
            ScpClassifiedDirectiveModulesConfig.PerceptionRule rule,
            ScpClassifiedDirectiveModulesConfig.Stealth settings) {
        double posture;
        if (AdvancedCrouchController.isLowCrawling(target)) {
            posture = settings.crawlingVisibility;
        } else if (target.isCrouching()) {
            posture = settings.crouchingVisibility;
        } else {
            posture = settings.standingVisibility;
        }

        double value = posture;
        boolean nightVision = rule != null && rule.nightVision;
        if (!nightVision) {
            int light = Math.max(0, Math.min(15,
                    target.level().getMaxLocalRawBrightness(target.blockPosition())));
            double normalizedLight = light / 15.0D;
            double lightFactor = settings.darknessFloor
                    + (1.0D - settings.darknessFloor)
                    * Math.pow(normalizedLight, 0.65D);
            value *= lightFactor;
        }

        if (target.isInvisible()) value *= 0.10D;
        if (rule != null) value *= rule.visibilityMultiplier;

        synchronized (VISIBILITY_OVERRIDES) {
            Double override = VISIBILITY_OVERRIDES.get(target);
            if (override != null) value = override;
        }
        for (VisibilityModifier modifier : MODIFIERS) {
            try {
                value = modifier.modify(observer, target, value);
            } catch (RuntimeException ignored) {
                // One optional integration must never disable perception globally.
            }
        }
        return clamp01(value);
    }

    public static ScpClassifiedDirectiveModulesConfig.PerceptionRule ruleFor(
            Mob observer) {
        if (observer == null) return null;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(observer.getType());
        if (id == null) return null;
        for (ScpClassifiedDirectiveModulesConfig.PerceptionRule rule :
                ScpClassifiedDirectiveModulesConfig.get().stealth.perceptionRules) {
            if (rule == null || rule.entity == null) continue;
            ResourceLocation configured = ResourceLocation.tryParse(rule.entity);
            if (id.equals(configured)) return rule;
        }
        return null;
    }

    /** Temporary absolute visibility, useful for lockers or anomalous hiding. */
    public static void setVisibilityOverride(Player player, double visibility) {
        if (player == null) return;
        synchronized (VISIBILITY_OVERRIDES) {
            VISIBILITY_OVERRIDES.put(player, clamp01(visibility));
        }
    }

    public static void clearVisibilityOverride(Player player) {
        if (player == null) return;
        synchronized (VISIBILITY_OVERRIDES) {
            VISIBILITY_OVERRIDES.remove(player);
        }
    }

    /** Future smoke/equipment/SCP integrations can register multiplicative logic. */
    public static void registerModifier(VisibilityModifier modifier) {
        if (modifier != null && !MODIFIERS.contains(modifier)) MODIFIERS.add(modifier);
    }

    public static void unregisterModifier(VisibilityModifier modifier) {
        MODIFIERS.remove(modifier);
    }

    private static void forget(Mob observer, Player target) {
        if (observer == null) return;
        Observation observation = OBSERVATIONS.get(observer);
        if (observation == null || target == null
                || target.getUUID().equals(observation.targetId)) {
            OBSERVATIONS.remove(observer);
        }
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    @FunctionalInterface
    public interface VisibilityModifier {
        double modify(Mob observer, Player target, double currentVisibility);
    }

    private static final class Observation {
        private final UUID targetId;
        private final long firstSeenTick;
        private long lastSeenTick;

        private Observation(UUID targetId, long firstSeenTick,
                long lastSeenTick) {
            this.targetId = targetId;
            this.firstSeenTick = firstSeenTick;
            this.lastSeenTick = lastSeenTick;
        }
    }
}
