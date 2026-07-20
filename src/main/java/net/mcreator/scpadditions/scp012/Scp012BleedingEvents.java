package net.mcreator.scpadditions.scp012;

import net.minecraft.core.registries.BuiltInRegistries;

import java.util.function.Supplier;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import com.bl4ues.scpadditions.compat.TickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModMobEffects;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent post-exposure blood loss and authored bleeding cues. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = EventBusSubscriber.Bus.GAME)
public final class Scp012BleedingEvents {
    private static final String BLEEDING_TAG = "ScpAdditionsScp012Bleeding";
    private static final int DAMAGE_INTERVAL_TICKS = 40;
    private static final float DAMAGE_PER_INTERVAL = 1.0F;
    private static final Map<UUID, BleedState> STATES = new HashMap<>();

    private Scp012BleedingEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        UUID id = player.getUUID();
        boolean marked = net.mcreator.scpadditions.fabric.FabricPersistentData.get(player).getBoolean(BLEEDING_TAG);
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            clear(player, true);
            return;
        }
        if (!marked) {
            STATES.remove(id);
            return;
        }

        // SCP-714 does not cure a physical wound. The bleeding state remains
        // until the player receives real healing from any source.
        if (!player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ScpAdditionsModMobEffects.BLEEDING.get()))) {
            player.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ScpAdditionsModMobEffects.BLEEDING.get()),
                    Integer.MAX_VALUE, 0, false, false, true));
        }

        BleedState state = STATES.computeIfAbsent(id, ignored -> new BleedState());
        state.ticks++;
        if (state.ticks % DAMAGE_INTERVAL_TICKS != 0) return;

        float before = player.getHealth();
        player.hurt(Scp012Damage.source(player.serverLevel()),
                DAMAGE_PER_INTERVAL);
        float lost = Math.max(0.0F, before - player.getHealth());
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
                || !net.mcreator.scpadditions.fabric.FabricPersistentData.get(player).getBoolean(BLEEDING_TAG)) {
            return;
        }
        clear(player, true);
    }

    private static void playBleedCue(ServerPlayer player) {
        @SuppressWarnings("unchecked")
        Supplier<SoundEvent>[] sounds = new Supplier[]{
                ScpAdditionsModSounds.SCP012_BLEED_1,
                ScpAdditionsModSounds.SCP012_BLEED_2,
                ScpAdditionsModSounds.SCP012_BLEED_3
        };
        Supplier<SoundEvent> selected = sounds[
                player.getRandom().nextInt(sounds.length)];
        float pitch = 0.94F + player.getRandom().nextFloat() * 0.12F;
        player.playNotifySound(selected.get(), SoundSource.PLAYERS,
                0.9F, pitch);
    }

    private static void clear(ServerPlayer player, boolean removeEffect) {
        STATES.remove(player.getUUID());
        net.mcreator.scpadditions.fabric.FabricPersistentData.get(player).remove(BLEEDING_TAG);
        if (removeEffect) {
            player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ScpAdditionsModMobEffects.BLEEDING.get()));
        }
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

    private static final class BleedState {
        private int ticks;
        private int nextMilestone = 1;
        private float damageTaken;
    }
}
