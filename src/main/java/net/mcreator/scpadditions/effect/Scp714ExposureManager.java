package net.mcreator.scpadditions.effect;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;
import com.bl4ues.scpadditions.compat.TickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

/** Server-authoritative exhaustion, immobilization and coma cycle for SCP-714. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class Scp714ExposureManager {
    public static final int FADE_DURATION_TICKS = 120 * 20;
    public static final int COMA_GRACE_TICKS = 5 * 20;
    public static final int DEATH_TICKS =
            FADE_DURATION_TICKS + COMA_GRACE_TICKS;

    private static final int TIRED_WARNING_TICKS = 90 * 20;
    private static final int SLEEP_WARNING_TICKS = 110 * 20;
    private static final int SYNC_INTERVAL_TICKS = 5;
    private static final String EXPOSURE_TAG = "Scp714ExposureTicks";
    private static final ResourceKey<DamageType> COMA_DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(ScpAdditionsMod.MODID, "scp_714_coma"));

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

        if (!Scp714ProtectionAccess.isProtected(player)) {
            clear(player, true);
            return;
        }

        int previousTicks = getExposureTicks(player);
        int exposureTicks = Math.min(DEATH_TICKS, previousTicks + 1);
        setExposureTicks(player, exposureTicks);

        if (exposureTicks == TIRED_WARNING_TICKS) {
            showActionBar(player,
                    "message.scp_additions.scp_714.tired");
        } else if (exposureTicks == SLEEP_WARNING_TICKS) {
            showActionBar(player,
                    "message.scp_additions.scp_714.sleepy");
        } else if (exposureTicks == FADE_DURATION_TICKS) {
            showActionBar(player,
                    "message.scp_additions.scp_714.coma_warning");
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
            player.invulnerableTime = 0;
            player.hurt(comaDamageSource(player), Float.MAX_VALUE);
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
        return Mth.clamp(net.mcreator.scpadditions.fabric.FabricPersistentData.get(player).getInt(EXPOSURE_TAG),
                0, DEATH_TICKS);
    }

    public static float getExposureProgress(ServerPlayer player) {
        return Mth.clamp(getExposureTicks(player)
                / (float) FADE_DURATION_TICKS, 0.0F, 1.0F);
    }

    public static double getMovementMultiplier(ServerPlayer player) {
        if (player == null || !Scp714ProtectionAccess.isProtected(player)) {
            return 1.0D;
        }
        return Mth.clamp(1.0D - getExposureTicks(player)
                / (double) FADE_DURATION_TICKS, 0.0D, 1.0D);
    }

    public static boolean isImmobilized(ServerPlayer player) {
        return player != null
                && Scp714ProtectionAccess.isProtected(player)
                && getExposureTicks(player) >= FADE_DURATION_TICKS;
    }

    private static void setExposureTicks(ServerPlayer player, int ticks) {
        CompoundTag data = net.mcreator.scpadditions.fabric.FabricPersistentData.get(player);
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
        boolean hadExposure = getExposureTicks(player) > 0;
        setExposureTicks(player, 0);
        if (syncClient && hadExposure) {
            ScpEntityNetwork.syncScp714Exposure(player,
                    false, 0.0F, false);
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
