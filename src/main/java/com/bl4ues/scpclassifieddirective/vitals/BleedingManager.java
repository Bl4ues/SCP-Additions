package com.bl4ues.scpclassifieddirective.vitals;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModMobEffects;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared persistent bleeding condition used by SCP-012, SCP-939 and future
 * physical wounds. Any real healing closes the wound, matching the previous
 * SCP-012 behavior, while damage arrives in irregular pulses instead of a
 * metronomic fixed tick.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BleedingManager {
    public static final String BLEEDING_TAG =
            "ScpClassifiedDirectiveBleeding";
    private static final String LEGACY_SCP_012_TAG =
            "ScpClassifiedDirectiveScp012Bleeding";

    private static final int INITIAL_DELAY_MIN_TICKS = 25;
    private static final int INITIAL_DELAY_VARIATION_TICKS = 36;
    private static final int INTERVAL_MIN_TICKS = 45;
    private static final int INTERVAL_VARIATION_TICKS = 56;
    private static final float DAMAGE_MIN = 0.50F;
    private static final float DAMAGE_VARIATION = 1.25F;

    private static final Map<UUID, BleedState> STATES = new HashMap<>();

    private BleedingManager() {
    }

    /** Applies or refreshes persistent bleeding without resetting its progress. */
    public static void apply(ServerPlayer player) {
        if (player == null || !player.isAlive()
                || player.isCreative() || player.isSpectator()) {
            return;
        }
        migrateLegacyTag(player);
        player.getPersistentData().putBoolean(BLEEDING_TAG, true);
        ensureEffect(player);
        STATES.computeIfAbsent(player.getUUID(), ignored ->
                new BleedState(initialDelay(player)));
    }

    public static boolean isBleeding(ServerPlayer player) {
        if (player == null) return false;
        return player.getPersistentData().getBoolean(BLEEDING_TAG)
                || player.getPersistentData().getBoolean(LEGACY_SCP_012_TAG);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        migrateLegacyTag(player);
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            clear(player, true);
            return;
        }

        /*
         * The MobEffect is also intentionally usable through vanilla commands.
         * Previously /effect give only painted the HUD icon red: the irregular
         * damage state lived behind a separate persistent flag that commands
         * never set. Treat a present Bleeding effect as a real wound and adopt
         * it into the same manager used by SCP-012/SCP-939.
         */
        if (!player.getPersistentData().getBoolean(BLEEDING_TAG)) {
            if (player.hasEffect(ScpClassifiedDirectiveModMobEffects.BLEEDING.get())) {
                player.getPersistentData().putBoolean(BLEEDING_TAG, true);
                STATES.computeIfAbsent(player.getUUID(), ignored ->
                        new BleedState(initialDelay(player)));
            } else {
                STATES.remove(player.getUUID());
                return;
            }
        }

        ensureEffect(player);
        BleedState state = STATES.computeIfAbsent(player.getUUID(), ignored ->
                new BleedState(initialDelay(player)));
        if (--state.ticksUntilDamage > 0) return;

        float before = player.getHealth();
        float damage = DAMAGE_MIN
                + player.getRandom().nextFloat() * DAMAGE_VARIATION;
        player.hurt(BleedingDamage.source(player.serverLevel()), damage);
        float lost = Math.max(0.0F, before - player.getHealth());
        state.ticksUntilDamage = nextInterval(player);
        if (lost <= 0.0F) return;

        state.damageTaken += lost;
        float milestoneSize = Math.max(1.0F, player.getMaxHealth()) * 0.20F;
        while (state.damageTaken >= milestoneSize * state.nextMilestone) {
            playBleedCue(player);
            state.nextMilestone++;
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getAmount() <= 0.0F
                || !(event.getEntity() instanceof ServerPlayer player)
                || !isBleeding(player)) {
            return;
        }
        clear(player, true);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player, true);
        }
    }

    private static void ensureEffect(ServerPlayer player) {
        if (!player.hasEffect(ScpClassifiedDirectiveModMobEffects.BLEEDING.get())) {
            player.addEffect(new MobEffectInstance(
                    ScpClassifiedDirectiveModMobEffects.BLEEDING.get(),
                    Integer.MAX_VALUE, 0, false, false, true));
        }
    }

    private static int initialDelay(ServerPlayer player) {
        return INITIAL_DELAY_MIN_TICKS
                + player.getRandom().nextInt(INITIAL_DELAY_VARIATION_TICKS);
    }

    private static int nextInterval(ServerPlayer player) {
        return INTERVAL_MIN_TICKS
                + player.getRandom().nextInt(INTERVAL_VARIATION_TICKS);
    }

    private static void migrateLegacyTag(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(LEGACY_SCP_012_TAG)) return;
        player.getPersistentData().remove(LEGACY_SCP_012_TAG);
        player.getPersistentData().putBoolean(BLEEDING_TAG, true);
    }

    private static void playBleedCue(ServerPlayer player) {
        @SuppressWarnings("unchecked")
        RegistryObject<SoundEvent>[] sounds = new RegistryObject[]{
                ScpClassifiedDirectiveModSounds.SCP012_BLEED_1,
                ScpClassifiedDirectiveModSounds.SCP012_BLEED_2,
                ScpClassifiedDirectiveModSounds.SCP012_BLEED_3
        };
        RegistryObject<SoundEvent> selected = sounds[
                player.getRandom().nextInt(sounds.length)];
        float pitch = 0.94F + player.getRandom().nextFloat() * 0.12F;
        player.playNotifySound(selected.get(), SoundSource.PLAYERS,
                0.9F, pitch);
    }

    private static void clear(ServerPlayer player, boolean removeEffect) {
        STATES.remove(player.getUUID());
        player.getPersistentData().remove(BLEEDING_TAG);
        player.getPersistentData().remove(LEGACY_SCP_012_TAG);
        if (removeEffect) {
            player.removeEffect(ScpClassifiedDirectiveModMobEffects.BLEEDING.get());
        }
    }

    private static final class BleedState {
        private int ticksUntilDamage;
        private int nextMilestone = 1;
        private float damageTaken;

        private BleedState(int ticksUntilDamage) {
            this.ticksUntilDamage = ticksUntilDamage;
        }
    }
}
