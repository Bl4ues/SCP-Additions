package net.mcreator.scpadditions.vitals;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.equipment.HazmatSuitAccess;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative copy of the final SCP Inventory stamina controller. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID)
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
        if (event.phase != TickEvent.Phase.START
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        UUID id = player.getUUID();
        if (!VitalsModule.staminaEnabled()) {
            clear(id);
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        float stamina = STAMINA.getOrDefault(id, MAX_STAMINA);
        int lockTicks = SPRINT_LOCK.getOrDefault(id, 0);
        if (stamina <= 0.0F || lockTicks > 0
                || StaminaBlockerAccess.isBlocked(player)) {
            forceStopSprint(player);
            if (lockTicks > 0) {
                SPRINT_LOCK.put(id, lockTicks - 1);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTickEnd(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        UUID id = player.getUUID();
        if (!VitalsModule.staminaEnabled()) {
            clear(id);
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            STAMINA.put(id, MAX_STAMINA);
            REGEN_DELAY.put(id, 0);
            SPRINT_LOCK.remove(id);
            return;
        }

        if (StaminaBlockerAccess.isBlocked(player)) {
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
                float drain = STAMINA_DRAIN_PER_TICK
                        * HazmatSuitAccess.getStaminaDrainMultiplier(player);
                stamina = Math.max(0.0F, stamina - drain);
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
                stamina = Math.min(MAX_STAMINA,
                        stamina + STAMINA_REGEN_PER_TICK);
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
        clear(event.getEntity().getUUID());
    }

    public static boolean canSprint(ServerPlayer player) {
        if (player == null || !VitalsModule.staminaEnabled()
                || player.isCreative() || player.isSpectator()) {
            return true;
        }

        UUID id = player.getUUID();
        return STAMINA.getOrDefault(id, MAX_STAMINA) > 0.0F
                && SPRINT_LOCK.getOrDefault(id, 0) <= 0
                && !StaminaBlockerAccess.isBlocked(player);
    }

    private static boolean isMovingHorizontally(ServerPlayer player) {
        Vec3 movement = player.getDeltaMovement();
        return (movement.x * movement.x + movement.z * movement.z)
                > MOVING_THRESHOLD_SQR;
    }

    private static void forceStopSprint(ServerPlayer player) {
        player.setSprinting(false);
    }

    private static void clear(UUID id) {
        STAMINA.remove(id);
        REGEN_DELAY.remove(id);
        SPRINT_LOCK.remove(id);
    }
}
