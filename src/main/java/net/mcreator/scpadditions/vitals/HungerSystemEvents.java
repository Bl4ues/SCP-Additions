package net.mcreator.scpadditions.vitals;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Replaces hunger with health-oriented food and delayed natural regeneration.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HungerSystemEvents {
    private static final int NORMAL_REGEN_DELAY_TICKS = 15 * 20;
    private static final int SATURATION_REGEN_DELAY_TICKS = 5 * 20;
    private static final int NORMAL_REGEN_INTERVAL_TICKS = 6 * 20;
    private static final int SATURATION_REGEN_INTERVAL_TICKS = 4 * 20;
    private static final float REGEN_AMOUNT = 1.0F;
    private static final Map<UUID, RegenerationState> REGENERATION =
            new HashMap<>();

    private HungerSystemEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.isCreative() || event.player.isSpectator()) {
            if (!event.player.level().isClientSide) {
                REGENERATION.remove(event.player.getUUID());
            }
            return;
        }

        boolean disabled = event.player.level().isClientSide
                ? InventoryModuleRuntimeState.hungerDisabledForClient()
                : ScpAdditionsModulesConfig.get().hunger.disabled;
        if (!disabled) {
            if (!event.player.level().isClientSide) {
                REGENERATION.remove(event.player.getUUID());
            }
            return;
        }

        normalizeFoodData(event.player.getFoodData(),
                event.player.getHealth() < event.player.getMaxHealth());
        if (event.phase == TickEvent.Phase.END
                && event.player instanceof ServerPlayer player) {
            tickRegeneration(player);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (event.getAmount() <= 0.0F
                || !(event.getEntity() instanceof ServerPlayer player)
                || !ScpAdditionsModulesConfig.get().hunger.disabled) {
            return;
        }
        REGENERATION.computeIfAbsent(player.getUUID(),
                ignored -> new RegenerationState()).reset();
    }

    @SubscribeEvent
    public static void onFoodFinished(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            healFromFood(player, event.getItem());
        }
    }

    public static void healFromFood(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()
                || player.isCreative() || player.isSpectator()
                || !ScpAdditionsModulesConfig.get().hunger.disabled) {
            return;
        }

        FoodProperties food = stack.getFoodProperties(player);
        if (food == null || food.getNutrition() <= 0) return;

        player.heal(food.getNutrition());
        normalizeFoodData(player.getFoodData(),
                player.getHealth() < player.getMaxHealth());
    }

    private static void normalizeFoodData(FoodData foodData,
            boolean allowFoodUse) {
        foodData.setFoodLevel(allowFoodUse ? 19 : 20);
        foodData.setSaturation(0.0F);
        foodData.setExhaustion(0.0F);
        foodData.tickTimer = 0;
    }

    private static void tickRegeneration(ServerPlayer player) {
        RegenerationState state = REGENERATION.computeIfAbsent(
                player.getUUID(), ignored -> new RegenerationState());
        state.ticksSinceDamage = Math.min(Integer.MAX_VALUE - 1,
                state.ticksSinceDamage + 1);

        if (!player.isAlive()
                || player.getHealth() >= player.getMaxHealth()
                || player.hasEffect(MobEffects.HUNGER)
                || !player.level().getGameRules().getBoolean(
                        GameRules.RULE_NATURAL_REGENERATION)) {
            return;
        }

        boolean saturated = player.hasEffect(MobEffects.SATURATION);
        int delay = saturated
                ? SATURATION_REGEN_DELAY_TICKS
                : NORMAL_REGEN_DELAY_TICKS;
        if (state.ticksSinceDamage < delay) return;

        int interval = saturated
                ? SATURATION_REGEN_INTERVAL_TICKS
                : NORMAL_REGEN_INTERVAL_TICKS;
        if (state.lastHealTick < delay
                || state.ticksSinceDamage - state.lastHealTick >= interval) {
            player.heal(REGEN_AMOUNT);
            state.lastHealTick = state.ticksSinceDamage;
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            REGENERATION.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        REGENERATION.remove(event.getEntity().getUUID());
    }

    private static final class RegenerationState {
        private int ticksSinceDamage;
        private int lastHealTick = -1;

        private void reset() {
            ticksSinceDamage = 0;
            lastHealTick = -1;
        }
    }
}
