package com.bl4ues.scpclassifieddirective.stealth;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import com.bl4ues.scpclassifieddirective.network.StealthNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Server-authoritative crouch/crawl controller with a client-side presentation
 * state for smooth eye-height changes.
 *
 * <p>The physical hitbox intentionally remains on Minecraft's canonical
 * CROUCHING and SWIMMING dimensions. Interpolating the collision box itself
 * would make client prediction and server collision disagree. Only the camera
 * travels continuously between the canonical eye heights.</p>
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AdvancedCrouchController {
    public static final float STANDING_EYE_HEIGHT = 1.62F;
    public static final float CROUCHING_EYE_HEIGHT = 1.27F;
    public static final float CRAWLING_EYE_HEIGHT = 0.40F;

    private static final double PROBE_DISTANCE = 0.46D;
    private static final double COLLISION_EPSILON = 1.0E-5D;
    private static final float CROUCH_DOWN_STEP = 0.070F;
    private static final float STAND_UP_STEP = 0.050F;
    private static final float CRAWL_DOWN_STEP = 0.155F;
    private static final float CRAWL_UP_STEP = 0.105F;

    private static final Map<Player, EyeState> EYE_STATES = new WeakHashMap<>();
    private static volatile boolean clientModuleEnabled = true;

    private AdvancedCrouchController() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player == null) return;

        boolean enabled;
        if (player.level().isClientSide) {
            enabled = clientModuleEnabled;
        } else {
            enabled = ScpClassifiedDirectiveModulesConfig.get().stealth.enabled;
            if (player instanceof ServerPlayer serverPlayer) {
                StealthNetwork.syncModuleState(serverPlayer, enabled);
            }
        }

        if (enabled) applyAutomaticLowPose(player);
        updateEyeState(player, enabled);
    }

    public static void setClientModuleEnabled(boolean enabled) {
        clientModuleEnabled = enabled;
    }

    public static boolean clientModuleEnabled() {
        return clientModuleEnabled;
    }

    /** Used by the client-only Player mixin. */
    public static float smoothEyeHeight(Player player, Pose requestedPose,
            float vanillaHeight) {
        if (player == null || !player.level().isClientSide
                || !clientModuleEnabled) {
            return vanillaHeight;
        }
        EyeState state = EYE_STATES.get(player);
        if (state == null) return vanillaHeight;

        // Only replace the three locomotion poses owned by this system. Sleeping,
        // fall-flying and other special poses must retain their vanilla camera.
        if (requestedPose != Pose.STANDING && requestedPose != Pose.CROUCHING
                && requestedPose != Pose.SWIMMING) {
            return vanillaHeight;
        }
        return state.height;
    }

    public static boolean isLowCrawling(Player player) {
        return player != null && player.getPose() == Pose.SWIMMING
                && !player.isSwimming() && !player.isFallFlying();
    }

    private static void applyAutomaticLowPose(Player player) {
        if (!canOwnPose(player)) return;

        boolean crouchHeld = player.isShiftKeyDown();
        boolean canStandHere = canFit(player, Pose.STANDING, Vec3.ZERO);
        boolean canCrouchHere = canFit(player, Pose.CROUCHING, Vec3.ZERO);
        boolean canCrawlHere = canFit(player, Pose.SWIMMING, Vec3.ZERO);
        boolean alreadyCrawling = isLowCrawling(player);

        boolean forcedLow = alreadyCrawling && !canCrouchHere && canCrawlHere;
        boolean enteringLowGap = crouchHeld && canCrawlHere
                && lowGapImmediatelyAhead(player);

        Pose desired = null;
        if (forcedLow || enteringLowGap) {
            desired = Pose.SWIMMING;
        } else if (crouchHeld) {
            desired = Pose.CROUCHING;
        } else if (alreadyCrawling && !canStandHere) {
            desired = canCrouchHere ? Pose.CROUCHING : Pose.SWIMMING;
        }

        if (desired != null && player.getPose() != desired) {
            player.setPose(desired);
            player.refreshDimensions();
        }
    }

    private static boolean lowGapImmediatelyAhead(Player player) {
        Vec3 horizontal = player.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() < 0.0025D) {
            Vec3 look = player.getLookAngle();
            horizontal = new Vec3(look.x, 0.0D, look.z);
        }
        if (horizontal.lengthSqr() < 1.0E-6D) return false;
        Vec3 offset = horizontal.normalize().scale(PROBE_DISTANCE);
        return !canFit(player, Pose.CROUCHING, offset)
                && canFit(player, Pose.SWIMMING, offset);
    }

    private static boolean canOwnPose(Player player) {
        return !player.isSpectator()
                && !player.isPassenger()
                && !player.isSleeping()
                && !player.isFallFlying()
                && !player.isAutoSpinAttack()
                && !player.isSwimming()
                && !player.isInWater();
    }

    private static boolean canFit(Player player, Pose pose, Vec3 offset) {
        EntityDimensions dimensions = player.getDimensions(pose);
        double halfWidth = dimensions.width * 0.5D;
        double x = player.getX() + offset.x;
        double y = player.getY() + offset.y;
        double z = player.getZ() + offset.z;
        AABB box = new AABB(x - halfWidth, y + COLLISION_EPSILON,
                z - halfWidth, x + halfWidth,
                y + dimensions.height - COLLISION_EPSILON, z + halfWidth);
        return player.level().noCollision(player, box);
    }

    private static void updateEyeState(Player player, boolean enabled) {
        float vanillaTarget = eyeHeightFor(player.getPose());
        EyeState state = EYE_STATES.computeIfAbsent(player,
                ignored -> new EyeState(vanillaTarget));
        if (!enabled) {
            state.height = vanillaTarget;
            return;
        }

        float target = vanillaTarget;
        float step;
        if (target < state.height) {
            step = target <= CRAWLING_EYE_HEIGHT + 0.01F
                    ? CRAWL_DOWN_STEP : CROUCH_DOWN_STEP;
        } else {
            step = state.height < CROUCHING_EYE_HEIGHT - 0.01F
                    ? CRAWL_UP_STEP : STAND_UP_STEP;
        }
        state.height = approach(state.height, target, step);
    }

    private static float eyeHeightFor(Pose pose) {
        if (pose == Pose.SWIMMING) return CRAWLING_EYE_HEIGHT;
        if (pose == Pose.CROUCHING) return CROUCHING_EYE_HEIGHT;
        return STANDING_EYE_HEIGHT;
    }

    private static float approach(float value, float target, float step) {
        if (value < target) return Math.min(target, value + step);
        if (value > target) return Math.max(target, value - step);
        return value;
    }

    private static final class EyeState {
        private float height;

        private EyeState(float height) {
            this.height = height;
        }
    }
}
