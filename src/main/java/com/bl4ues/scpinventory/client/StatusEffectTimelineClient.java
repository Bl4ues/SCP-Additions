package com.bl4ues.scpinventory.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Tracks the original client-side duration of active effects for proportional UI bars. */
public final class StatusEffectTimelineClient {
    private static final int INFINITE_DURATION_THRESHOLD = 20 * 60 * 60 * 24;
    private static final Map<EffectKey, Integer> INITIAL_DURATIONS = new HashMap<>();

    private StatusEffectTimelineClient() {
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            INITIAL_DURATIONS.clear();
            return;
        }

        Set<EffectKey> active = new HashSet<>();
        for (MobEffectInstance effect : minecraft.player.getActiveEffects()) {
            EffectKey key = key(effect);
            if (key == null) continue;
            active.add(key);

            int duration = effect.getDuration();
            if (isFinite(duration) && duration > 0) {
                INITIAL_DURATIONS.merge(key, duration, Math::max);
            }
        }
        INITIAL_DURATIONS.keySet().removeIf(key -> !active.contains(key));
    }

    public static float getRemainingRatio(MobEffectInstance effect) {
        if (effect == null) return 0.0F;
        int duration = effect.getDuration();
        if (!isFinite(duration)) return 1.0F;
        if (duration <= 0) return 0.0F;

        EffectKey key = key(effect);
        int initial = key == null ? duration : INITIAL_DURATIONS.getOrDefault(key, duration);
        if (initial < duration) {
            initial = duration;
            if (key != null) INITIAL_DURATIONS.put(key, initial);
        }
        return Math.max(0.0F, Math.min(1.0F, duration / (float) Math.max(1, initial)));
    }

    private static boolean isFinite(int duration) {
        return duration >= 0 && duration <= INFINITE_DURATION_THRESHOLD;
    }

    private static EffectKey key(MobEffectInstance effect) {
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect());
        return id == null ? null : new EffectKey(id, effect.getAmplifier());
    }

    private record EffectKey(ResourceLocation id, int amplifier) {
    }
}
