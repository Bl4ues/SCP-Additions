package com.bl4ues.scpinventory.event;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.item.ScpItemEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public final class PlayerStaminaEvents {

    private static final float MAX_STAMINA = 100.0F;
    private static final float STAMINA_DRAIN_PER_TICK = MAX_STAMINA / (5.0F * 20.0F);
    private static final float STAMINA_REGEN_PER_TICK = MAX_STAMINA / (5.0F * 20.0F);
    private static final int REGEN_DELAY_TICKS = 20;
    private static final int EXHAUSTED_LOCK_TICKS = 8;
    private static final double MOVING_THRESHOLD_SQR = 0.0004D;

    private static final Map<UUID, Float> STAMINA = new HashMap<>();
    private static final Map<UUID, Integer> REGEN_DELAY = new HashMap<>();
    private static final Map<UUID, Integer> SPRINT_LOCK = new HashMap<>();

    private PlayerStaminaEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTickStart(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        UUID id = player.getUUID();
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        float stamina = STAMINA.getOrDefault(id, MAX_STAMINA);
        int lockTicks = SPRINT_LOCK.getOrDefault(id, 0);
        if (stamina <= 0.0F || lockTicks > 0 || ScpItemEffects.hasNoStaminaModifierEquipped(player)) {
            forceStopSprint(player);
            if (lockTicks > 0) {
                SPRINT_LOCK.put(id, lockTicks - 1);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTickEnd(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        UUID id = player.getUUID();
        if (player.isCreative() || player.isSpectator()) {
            STAMINA.put(id, MAX_STAMINA);
            REGEN_DELAY.put(id, 0);
            SPRINT_LOCK.remove(id);
            return;
        }

        if (ScpItemEffects.hasNoStaminaModifierEquipped(player)) {
            STAMINA.put(id, 0.0F);
            REGEN_DELAY.put(id, REGEN_DELAY_TICKS);
            SPRINT_LOCK.put(id, EXHAUSTED_LOCK_TICKS);
            forceStopSprint(player);
            return;
        }

        float stamina = STAMINA.getOrDefault(id, MAX_STAMINA);
        int regenDelay = REGEN_DELAY.getOrDefault(id, 0);
        boolean moving = isMovingHorizontally(player);
        boolean sprinting = player.isSprinting();

        if (moving && sprinting) {
            if (stamina > 0.0F) {
                stamina = Math.max(0.0F, stamina - STAMINA_DRAIN_PER_TICK);
            }
            regenDelay = REGEN_DELAY_TICKS;
            if (stamina <= 0.0F) {
                stamina = 0.0F;
                SPRINT_LOCK.put(id, EXHAUSTED_LOCK_TICKS);
                forceStopSprint(player);
            }
        } else {
            if (stamina <= 0.0F) {
                stamina = 0.0F;
                forceStopSprint(player);
            }

            if (regenDelay > 0) {
                regenDelay--;
            } else if (stamina < MAX_STAMINA) {
                stamina = Math.min(MAX_STAMINA, stamina + STAMINA_REGEN_PER_TICK);
                if (stamina > 0.0F) {
                    SPRINT_LOCK.remove(id);
                }
            }
        }

        STAMINA.put(id, stamina);
        REGEN_DELAY.put(id, regenDelay);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        STAMINA.remove(id);
        REGEN_DELAY.remove(id);
        SPRINT_LOCK.remove(id);
    }

    private static boolean isMovingHorizontally(ServerPlayer player) {
        Vec3 delta = player.getDeltaMovement();
        return (delta.x * delta.x + delta.z * delta.z) > MOVING_THRESHOLD_SQR;
    }

    private static void forceStopSprint(ServerPlayer player) {
        player.setSprinting(false);
    }
}
