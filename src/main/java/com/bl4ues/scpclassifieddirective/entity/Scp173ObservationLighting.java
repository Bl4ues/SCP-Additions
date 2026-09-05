package com.bl4ues.scpclassifieddirective.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

/**
 * Shared visibility gate for SCP-173 observation. Geometry still decides whether
 * an observer is actually looking at the statue; this class decides whether
 * there is enough light, or an explicit vision aid, for that look to count.
 */
public final class Scp173ObservationLighting {
    public static final int MIN_OBSERVATION_LIGHT = 5;

    private static final List<BiPredicate<LivingEntity, Scp173Entity>>
            VISION_ASSISTS = new CopyOnWriteArrayList<>();

    private Scp173ObservationLighting() {
    }

    /** Extension point for future flashlights, goggles and other active vision. */
    public static void registerVisionAssist(
            BiPredicate<LivingEntity, Scp173Entity> assist) {
        if (assist != null) VISION_ASSISTS.add(assist);
    }

    public static boolean canObserve(Scp173Entity statue,
            LivingEntity observer) {
        return statue != null && observer != null
                && (ambientLight(statue) >= MIN_OBSERVATION_LIGHT
                || hasIndependentVisionAssist(observer, statue));
    }

    public static boolean hasIndependentVisionAssist(LivingEntity observer,
            Scp173Entity statue) {
        if (observer == null || statue == null) return false;
        if (observer.hasEffect(MobEffects.NIGHT_VISION)) return true;
        for (BiPredicate<LivingEntity, Scp173Entity> assist : VISION_ASSISTS) {
            try {
                if (assist.test(observer, statue)) return true;
            } catch (RuntimeException ignored) {
                // A compatibility vision provider must never break SCP-173 AI.
            }
        }
        return false;
    }

    /**
     * Use Minecraft's local raw brightness so outdoor moonlight follows the
     * normal day/night sky-darkening rules instead of treating open sky as day.
     */
    public static int ambientLight(Scp173Entity statue) {
        if (statue == null || statue.level() == null) return 15;
        double x = statue.getX();
        double z = statue.getZ();
        double bottom = statue.getBoundingBox().minY + 0.12D;
        double middle = (statue.getBoundingBox().minY
                + statue.getBoundingBox().maxY) * 0.5D;
        double top = statue.getBoundingBox().maxY - 0.12D;
        int feet = statue.level().getMaxLocalRawBrightness(
                BlockPos.containing(x, bottom, z));
        int body = statue.level().getMaxLocalRawBrightness(
                BlockPos.containing(x, middle, z));
        int head = statue.level().getMaxLocalRawBrightness(
                BlockPos.containing(x, top, z));
        return Math.max(feet, Math.max(body, head));
    }
}
