package com.bl4ues.scpclassifieddirective.effect;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.compat.MineZeroCompatibility;
import com.bl4ues.scpclassifieddirective.compat.MineZeroDeathCoordinator;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import com.bl4ues.scpclassifieddirective.init.Scp714Items;
import com.bl4ues.scpclassifieddirective.inventory.ScpInventoryAccess;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpEquipmentSlot;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.bl4ues.scpclassifieddirective.network.ScpEntityNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingSwapItemsEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-authoritative exhaustion, immobilization and coma cycle for SCP-714. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID)
public final class Scp714ExposureManager {
    public static final int FADE_DURATION_TICKS = 120 * 20;
    public static final int COMA_GRACE_TICKS = 5 * 20;
    public static final int DEATH_TICKS =
            FADE_DURATION_TICKS + COMA_GRACE_TICKS;
    public static final float MAX_STAMINA_DRAIN_MULTIPLIER = 2.5F;

    private static final int TIRED_WARNING_TICKS = 90 * 20;
    private static final int SLEEP_WARNING_TICKS = 110 * 20;
    private static final int SYNC_INTERVAL_TICKS = 5;
    private static final double RESCUE_RANGE = 2.75D;
    private static final String EXPOSURE_TAG = "Scp714ExposureTicks";
    private static final String COMA_TAG = "Scp714Comatose";
    private static final String COMA_X_TAG = "Scp714ComaX";
    private static final String COMA_Z_TAG = "Scp714ComaZ";
    private static final String COMA_YAW_TAG = "Scp714ComaYaw";
    private static final String COMA_PITCH_TAG = "Scp714ComaPitch";
    private static final String COMA_SELECTED_TAG = "Scp714ComaSelected";
    public static final String REMOVE_INTERACTION_KEY =
            "scp714_remove_from_coma";
    private static final ResourceKey<DamageType> COMA_DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE,
                    new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                            "scp_714_coma"));

    private Scp714ExposureManager() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            clear(player, true);
            return;
        }

        if (isComatose(player)) {
            tickComa(player);
            return;
        }

        if (!Scp714ProtectionAccess.isProtected(player)) {
            clear(player, true);
            return;
        }

        int previousTicks = getExposureTicks(player);
        int exposureTicks = Math.min(DEATH_TICKS, previousTicks + 1);
        setExposureTicks(player, exposureTicks);

        if (exposureTicks == TIRED_WARNING_TICKS) {
            showActionBar(player,
                    "message.scp_classified_directive.scp_714.tired");
        } else if (exposureTicks == SLEEP_WARNING_TICKS) {
            showActionBar(player,
                    "message.scp_classified_directive.scp_714.sleepy");
        } else if (exposureTicks == FADE_DURATION_TICKS) {
            showActionBar(player,
                    "message.scp_classified_directive.scp_714.coma_warning");
        }

        boolean immobilized = exposureTicks >= FADE_DURATION_TICKS;
        if (immobilized) {
            freezeHorizontalMovement(player);
        }

        if (previousTicks == 0
                || exposureTicks % SYNC_INTERVAL_TICKS == 0
                || exposureTicks == TIRED_WARNING_TICKS
                || exposureTicks == SLEEP_WARNING_TICKS
                || exposureTicks == FADE_DURATION_TICKS) {
            sync(player, exposureTicks, immobilized);
        }

        if (exposureTicks >= DEATH_TICKS) {
            if (MineZeroCompatibility.enabled() && hasOtherLivingPlayer(player)) {
                enterComa(player);
            } else {
                applyFatalComa(player);
            }
        }
    }

    private static void tickComa(ServerPlayer player) {
        if (!MineZeroCompatibility.enabled()) {
            applyFatalComa(player);
            return;
        }
        // Once the coma starts it is a state of the victim, not a live check of
        // whether their client still reports SCP-714 in the expected slot. The
        // only legitimate recovery path is tryRemoveFromComa(), invoked by a
        // different living player. This prevents self-rescue through inventory
        // clicks, offhand swaps or stale mirror state.
        if (!hasOtherLivingPlayer(player)) {
            applyFatalComa(player);
            return;
        }

        maintainComaPose(player);
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        if (player.tickCount % SYNC_INTERVAL_TICKS == 0) {
            sync(player, DEATH_TICKS, true);
        }
    }

    private static void enterComa(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.putBoolean(COMA_TAG, true);
        data.putDouble(COMA_X_TAG, player.getX());
        data.putDouble(COMA_Z_TAG, player.getZ());
        data.putFloat(COMA_YAW_TAG, player.getYRot());
        data.putFloat(COMA_PITCH_TAG, player.getXRot());
        data.putInt(COMA_SELECTED_TAG, player.getInventory().selected);
        setExposureTicks(player, DEATH_TICKS);
        maintainComaPose(player);
        sync(player, DEATH_TICKS, true);
    }

    private static void maintainComaPose(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        double x = data.contains(COMA_X_TAG) ? data.getDouble(COMA_X_TAG)
                : player.getX();
        double z = data.contains(COMA_Z_TAG) ? data.getDouble(COMA_Z_TAG)
                : player.getZ();
        float yaw = data.contains(COMA_YAW_TAG) ? data.getFloat(COMA_YAW_TAG)
                : player.getYRot();
        float pitch = data.contains(COMA_PITCH_TAG) ? data.getFloat(COMA_PITCH_TAG)
                : player.getXRot();
        int selected = data.contains(COMA_SELECTED_TAG)
                ? data.getInt(COMA_SELECTED_TAG)
                : player.getInventory().selected;
        if (!data.contains(COMA_X_TAG)) data.putDouble(COMA_X_TAG, x);
        if (!data.contains(COMA_Z_TAG)) data.putDouble(COMA_Z_TAG, z);
        if (!data.contains(COMA_YAW_TAG)) data.putFloat(COMA_YAW_TAG, yaw);
        if (!data.contains(COMA_PITCH_TAG)) data.putFloat(COMA_PITCH_TAG, pitch);
        if (!data.contains(COMA_SELECTED_TAG)) {
            data.putInt(COMA_SELECTED_TAG, selected);
        }

        Vec3 motion = player.getDeltaMovement();
        player.setPos(x, player.getY(), z);
        player.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
        player.setYRot(yaw);
        player.setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
        player.setYHeadRot(yaw);
        player.getInventory().selected = Mth.clamp(selected, 0, 8);
        player.setSprinting(false);
        player.setShiftKeyDown(false);
        player.xxa = 0.0F;
        player.zza = 0.0F;
        player.setPose(Pose.SLEEPING);
        player.hurtMarked = true;
    }

    public static boolean isComatose(ServerPlayer player) {
        return player != null && player.getPersistentData().getBoolean(COMA_TAG);
    }

    /** True while SCP-714 has removed all normal player agency. */
    public static boolean isControlsLocked(ServerPlayer player) {
        return player != null && (isComatose(player)
                || getExposureTicks(player) >= FADE_DURATION_TICKS);
    }

    public static boolean tryRemoveFromComa(ServerPlayer rescuer,
            ServerPlayer victim) {
        if (rescuer == null || victim == null || rescuer == victim
                || !rescuer.isAlive() || rescuer.isSpectator()
                || isComatose(rescuer) || !isComatose(victim)
                || !MineZeroCompatibility.enabled()) {
            return false;
        }
        if (rescuer.distanceToSqr(victim) > RESCUE_RANGE * RESCUE_RANGE
                || !rescuer.hasLineOfSight(victim)
                || !canReceiveRing(rescuer)) {
            return false;
        }

        ItemStack ring = extractRing(victim);
        if (ring.isEmpty()) {
            return false;
        }
        if (!giveRing(rescuer, ring)) {
            rescuer.drop(ring, false);
        }
        clear(victim, true);
        return true;
    }

    private static boolean canReceiveRing(ServerPlayer player) {
        if (!ScpClassifiedDirectiveModulesConfig.get().inventory.enabled) {
            return player.getInventory().getFreeSlot() >= 0;
        }
        final boolean[] canReceive = {false};
        ScpInventoryAccess.get(player).ifPresent(inventory ->
                canReceive[0] = inventory.hasFreeMainSlots(1));
        return canReceive[0];
    }

    private static boolean giveRing(ServerPlayer player, ItemStack ring) {
        if (ring.isEmpty()) return false;
        if (!ScpClassifiedDirectiveModulesConfig.get().inventory.enabled) {
            return player.getInventory().add(ring);
        }
        final boolean[] added = {false};
        ScpInventoryAccess.get(player).ifPresent(inventory -> {
            if (inventory.addInventoryItem(ring)) {
                added[0] = true;
                ModNetwork.syncTo(player, inventory);
            }
        });
        return added[0];
    }

    private static ItemStack extractRing(ServerPlayer player) {
        final ItemStack[] extracted = {ItemStack.EMPTY};
        ScpInventoryAccess.get(player).ifPresent(inventory -> {
            for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
                ItemStack stack = inventory.getEquipment(slot);
                if (stack.is(Scp714Items.SCP_714.get())) {
                    extracted[0] = inventory.extractEquipment(slot);
                    ModNetwork.syncTo(player, inventory);
                    return;
                }
            }
            // Defensive fallback for old/stale clients that managed to move the
            // ring out of its equipment slot before the coma lock was applied.
            for (int i = 0; i < inventory.getMaxMainSlots(); i++) {
                ItemStack stack = inventory.getInventoryItem(i);
                if (stack.is(Scp714Items.SCP_714.get())) {
                    extracted[0] = inventory.extractInventoryItem(i);
                    ModNetwork.syncTo(player, inventory);
                    return;
                }
            }
        });
        if (!extracted[0].isEmpty()) {
            return extracted[0];
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.is(Scp714Items.SCP_714.get())) {
                ItemStack result = stack.copy();
                player.setItemSlot(slot, ItemStack.EMPTY);
                return result;
            }
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Scp714Items.SCP_714.get())) {
                ItemStack result = stack.copy();
                player.getInventory().setItem(i, ItemStack.EMPTY);
                player.getInventory().setChanged();
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean hasOtherLivingPlayer(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == player || !other.isAlive() || other.isSpectator()
                    || MineZeroDeathCoordinator.isLogicallyDead(other)
                    || isComatose(other)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static void applyFatalComa(ServerPlayer player) {
        leaveComaState(player);
        player.invulnerableTime = 0;
        player.hurt(comaDamageSource(player), Float.MAX_VALUE);
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && isControlsLocked(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && isControlsLocked(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player
                && isControlsLocked(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onSwapHands(LivingSwapItemsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && isControlsLocked(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            int ticks = getExposureTicks(player);
            sync(player, ticks, ticks >= FADE_DURATION_TICKS);
        }
    }

    public static int getExposureTicks(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return Mth.clamp(player.getPersistentData().getInt(EXPOSURE_TAG),
                0, DEATH_TICKS);
    }

    public static float getExposureProgress(ServerPlayer player) {
        return Mth.clamp(getExposureTicks(player)
                / (float) FADE_DURATION_TICKS, 0.0F, 1.0F);
    }

    public static float getStaminaDrainMultiplier(ServerPlayer player) {
        if (player == null || !Scp714ProtectionAccess.isProtected(player)) {
            return 1.0F;
        }
        return Mth.lerp(getExposureProgress(player), 1.0F,
                MAX_STAMINA_DRAIN_MULTIPLIER);
    }

    public static double getMovementMultiplier(ServerPlayer player) {
        if (player == null || !Scp714ProtectionAccess.isProtected(player)) {
            return 1.0D;
        }
        return Mth.clamp(1.0D - getExposureTicks(player)
                / (double) FADE_DURATION_TICKS, 0.0D, 1.0D);
    }

    public static boolean isImmobilized(ServerPlayer player) {
        return player != null && (isComatose(player)
                || Scp714ProtectionAccess.isProtected(player)
                && getExposureTicks(player) >= FADE_DURATION_TICKS);
    }

    private static void setExposureTicks(ServerPlayer player, int ticks) {
        CompoundTag data = player.getPersistentData();
        if (ticks <= 0) {
            data.remove(EXPOSURE_TAG);
        } else {
            data.putInt(EXPOSURE_TAG, Mth.clamp(ticks, 0, DEATH_TICKS));
        }
    }

    private static void clear(ServerPlayer player, boolean syncClient) {
        if (player == null) {
            return;
        }
        boolean hadExposure = getExposureTicks(player) > 0 || isComatose(player);
        leaveComaState(player);
        setExposureTicks(player, 0);
        if (syncClient && hadExposure) {
            ScpEntityNetwork.syncScp714Exposure(player,
                    false, 0.0F, false);
        }
    }

    private static void leaveComaState(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        boolean wasComatose = data.getBoolean(COMA_TAG);
        data.remove(COMA_TAG);
        data.remove(COMA_X_TAG);
        data.remove(COMA_Z_TAG);
        data.remove(COMA_YAW_TAG);
        data.remove(COMA_PITCH_TAG);
        data.remove(COMA_SELECTED_TAG);
        if (wasComatose && player.getPose() == Pose.SLEEPING) {
            player.setPose(Pose.STANDING);
            player.hurtMarked = true;
        }
    }

    private static void sync(ServerPlayer player, int exposureTicks,
            boolean immobilized) {
        ScpEntityNetwork.syncScp714Exposure(player, true,
                Mth.clamp(exposureTicks / (float) FADE_DURATION_TICKS,
                        0.0F, 1.0F), immobilized);
    }

    private static void freezeHorizontalMovement(ServerPlayer player) {
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(0.0D, motion.y, 0.0D);
        player.setSprinting(false);
        player.xxa = 0.0F;
        player.zza = 0.0F;
        player.hurtMarked = true;
    }

    private static void showActionBar(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    private static DamageSource comaDamageSource(ServerPlayer player) {
        return new DamageSource(player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(COMA_DAMAGE_TYPE));
    }
}
