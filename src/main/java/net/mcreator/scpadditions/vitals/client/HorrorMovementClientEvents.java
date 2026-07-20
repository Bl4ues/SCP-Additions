package net.mcreator.scpadditions.vitals.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.Scp714ClientState;
import net.mcreator.scpadditions.equipment.HazmatSuitAccess;
import net.mcreator.scpadditions.vitals.HorrorMovementNetwork;
import net.mcreator.scpadditions.vitals.VitalsModule;

/** Sends sprint-input changes and corrects custom movement-speed FOV. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class HorrorMovementClientEvents {
    private static final double VANILLA_WALK_SPEED = 0.100D;
    private static final double HORROR_WALK_SPEED = 0.055D;
    private static final double HORROR_SPRINT_BASE_SPEED = 0.110D;
    private static final double HAZMAT_VANILLA_WALK_SPEED = 0.075D;
    private static final double HAZMAT_VANILLA_SPRINT_BASE_SPEED = 0.050D;
    private static final double HAZMAT_HORROR_WALK_SPEED = 0.04125D;
    private static final double HAZMAT_HORROR_SPRINT_BASE_SPEED = 0.055D;
    private static final double EPSILON = 0.0001D;
    private static final float SCP_714_FOV_SMOOTHING = 0.12F;

    private static boolean lastRequestedSprint;
    private static float smoothedScp714FovProgress;

    private HorrorMovementClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            lastRequestedSprint = false;
            smoothedScp714FovProgress = 0.0F;
            return;
        }

        float targetFovProgress = Scp714ClientState.isActive()
                ? Scp714ClientState.getTargetProgress() : 0.0F;
        smoothedScp714FovProgress = Mth.lerp(SCP_714_FOV_SMOOTHING,
                smoothedScp714FovProgress, targetFovProgress);
        if (Math.abs(smoothedScp714FovProgress - targetFovProgress) < 0.0005F) {
            smoothedScp714FovProgress = targetFovProgress;
        }

        boolean requestedSprint = VitalsModule.horrorMovementEnabled()
                && minecraft.screen == null
                && !player.isCreative()
                && !player.isSpectator()
                && minecraft.options.keySprint.isDown()
                && player.input.forwardImpulse > 0.0F
                && !player.isCrouching()
                && !player.isPassenger()
                && !player.getAbilities().flying
                && player.getFoodData().getFoodLevel() > 6
                && PlayerVitalsClient.canSprint();

        if (requestedSprint != lastRequestedSprint) {
            HorrorMovementNetwork.sendSprintInput(requestedSprint);
            lastRequestedSprint = requestedSprint;
        }
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        boolean hazmat = HazmatSuitAccess.isFullyEquipped(player);
        boolean scp714 = Scp714ClientState.isActive()
                && !player.isCreative() && !player.isSpectator();
        if (player.isSpectator()
                || (!VitalsModule.horrorMovementEnabled() && !hazmat && !scp714)) {
            return;
        }

        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }

        double base = movement.getBaseValue();
        boolean managedBase = approximately(base, HORROR_WALK_SPEED)
                || approximately(base, HORROR_SPRINT_BASE_SPEED)
                || approximately(base, HAZMAT_VANILLA_WALK_SPEED)
                || approximately(base, HAZMAT_VANILLA_SPRINT_BASE_SPEED)
                || approximately(base, HAZMAT_HORROR_WALK_SPEED)
                || approximately(base, HAZMAT_HORROR_SPRINT_BASE_SPEED);
        if (scp714) {
            double maximumManagedBase = maximumManagedBase(player, hazmat);
            managedBase |= base >= -EPSILON
                    && base <= maximumManagedBase + EPSILON;
        }
        if (!managedBase || base < -EPSILON) {
            return;
        }

        double abilityWalkSpeed = player.getAbilities().getWalkingSpeed();
        if (abilityWalkSpeed <= EPSILON) {
            return;
        }

        double currentValue = movement.getValue();
        double currentMovementFactor =
                (currentValue / abilityWalkSpeed + 1.0D) / 2.0D;
        if (Math.abs(currentMovementFactor) <= EPSILON) {
            return;
        }

        double vanillaEquivalentValue;
        if (scp714) {
            double modifierScale = base > EPSILON
                    ? Mth.clamp(currentValue / base, 0.0D, 4.0D) : 1.0D;
            double smoothMovementMultiplier = Mth.clamp(
                    1.0D - smoothedScp714FovProgress, 0.0D, 1.0D);
            vanillaEquivalentValue = VANILLA_WALK_SPEED
                    * smoothMovementMultiplier * modifierScale;
        } else {
            if (base <= EPSILON) {
                return;
            }
            vanillaEquivalentValue =
                    currentValue * (VANILLA_WALK_SPEED / base);
        }

        double vanillaMovementFactor =
                (vanillaEquivalentValue / abilityWalkSpeed + 1.0D) / 2.0D;
        float correctedRaw = (float) (event.getFovModifier()
                / currentMovementFactor * vanillaMovementFactor);

        float accessibilityScale = Minecraft.getInstance().options
                .fovEffectScale().get().floatValue();
        event.setNewFovModifier(Mth.lerp(
                accessibilityScale, 1.0F, correctedRaw));
    }

    private static double maximumManagedBase(Player player, boolean hazmat) {
        boolean horrorMovement = VitalsModule.horrorMovementEnabled()
                && !player.isCreative();
        if (horrorMovement) {
            return hazmat ? HAZMAT_HORROR_SPRINT_BASE_SPEED
                    : HORROR_SPRINT_BASE_SPEED;
        }
        return hazmat ? HAZMAT_VANILLA_WALK_SPEED : VANILLA_WALK_SPEED;
    }

    private static boolean approximately(double left, double right) {
        return Math.abs(left - right) <= EPSILON;
    }
}
