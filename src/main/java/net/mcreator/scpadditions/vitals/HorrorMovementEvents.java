package net.mcreator.scpadditions.vitals;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.bl4ues.scpadditions.compat.TickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.effect.Scp714ExposureManager;
import net.mcreator.scpadditions.equipment.HazmatSuitAccess;

/** Server-authoritative horror movement controller with equipment penalties. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class HorrorMovementEvents {
    private static final double VANILLA_WALK_SPEED = 0.100D;
    private static final double HORROR_WALK_SPEED = 0.055D;
    private static final double HORROR_SPRINT_BASE_SPEED = 0.110D;
    private static final double HAZMAT_WALK_MULTIPLIER = 0.75D;
    private static final double HAZMAT_SPRINT_MULTIPLIER = 0.50D;
    private static final double EPSILON = 0.0001D;

    private HorrorMovementEvents() {
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (player.isSpectator()) {
            HorrorMovementNetwork.clear(player);
            applyMovementSpeed(player, VANILLA_WALK_SPEED);
            return;
        }

        boolean hazmat = HazmatSuitAccess.isFullyEquipped(player);
        double scp714Multiplier =
                Scp714ExposureManager.getMovementMultiplier(player);
        if (!VitalsModule.horrorMovementEnabled() || player.isCreative()) {
            HorrorMovementNetwork.clear(player);
            double speed = VANILLA_WALK_SPEED;
            if (hazmat) {
                speed *= player.isSprinting()
                        ? HAZMAT_SPRINT_MULTIPLIER
                        : HAZMAT_WALK_MULTIPLIER;
            }
            speed *= scp714Multiplier;
            if (speed <= EPSILON) {
                player.setSprinting(false);
            }
            applyMovementSpeed(player, speed);
            return;
        }

        boolean sprinting = HorrorMovementNetwork.isSprintRequested(player)
                && canUseHorrorSprint(player)
                && PlayerStaminaEvents.canSprint(player);

        if (player.isSprinting() != sprinting) {
            player.setSprinting(sprinting);
        }

        double speed = sprinting
                ? HORROR_SPRINT_BASE_SPEED
                : HORROR_WALK_SPEED;
        if (hazmat) {
            speed *= sprinting
                    ? HAZMAT_SPRINT_MULTIPLIER
                    : HAZMAT_WALK_MULTIPLIER;
        }
        speed *= scp714Multiplier;
        if (speed <= EPSILON) {
            player.setSprinting(false);
        }
        applyMovementSpeed(player, speed);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HorrorMovementNetwork.clear(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HorrorMovementNetwork.clear(player);
            boolean survivalHorrorMovement = VitalsModule.horrorMovementEnabled()
                    && !player.isCreative() && !player.isSpectator();
            applyMovementSpeed(player, survivalHorrorMovement
                    ? HORROR_WALK_SPEED : VANILLA_WALK_SPEED);
        }
    }

    private static boolean canUseHorrorSprint(ServerPlayer player) {
        return !player.isCrouching()
                && !player.isPassenger()
                && !player.isFallFlying()
                && !player.getAbilities().flying
                && player.getFoodData().getFoodLevel() > 6;
    }

    private static void applyMovementSpeed(ServerPlayer player, double speed) {
        AttributeInstance movementSpeed =
                player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null
                && Math.abs(movementSpeed.getBaseValue() - speed) > EPSILON) {
            movementSpeed.setBaseValue(speed);
        }
    }
}
