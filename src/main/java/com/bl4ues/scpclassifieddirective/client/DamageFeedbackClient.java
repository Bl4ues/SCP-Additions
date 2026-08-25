package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModParticleTypes;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import com.bl4ues.scpclassifieddirective.vitals.BleedingDamage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Client-only damage presentation. Direct hits retain the normal hurt reaction;
 * damage-over-time pulses become quiet and stable while still reducing health.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DamageFeedbackClient {
    private static final float HEALTH_EPSILON = 1.0E-3F;
    private static final int BLOOD_COLOR = 0x880808;
    private static final int WITHER_COLOR = 0x2B0606;
    private static final int SCP_1079_COLOR = 0xF5A2E4;
    private static final double TWO_PI = Math.PI * 2.0D;
    private static final double LOCAL_SOUND_RADIUS_SQ = 1.0D;

    /** Reserved now so future SCP-1079 damage automatically gets its authored color. */
    public static final ResourceKey<DamageType> SCP_1079_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE,
                    new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                            "scp_1079"));

    private static UUID trackedPlayerId;
    private static float previousHealth = Float.NaN;

    private DamageFeedbackClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            reset();
            return;
        }

        UUID playerId = player.getUUID();
        float health = player.getHealth();
        if (!playerId.equals(trackedPlayerId) || Float.isNaN(previousHealth)) {
            trackedPlayerId = playerId;
            previousHealth = health;
            return;
        }

        if (!ClientModulePreferences.contextualDamageFeedbackEnabled()) {
            previousHealth = health;
            return;
        }

        DamageSource source = player.getLastDamageSource();
        boolean continuous = isContinuousDamage(player, source);
        if (continuous) {
            // hurtTime drives vanilla camera hurt tilt and several local hit
            // presentation paths. The authoritative health is untouched.
            player.hurtTime = 0;
        }

        boolean lostHealth = health + HEALTH_EPSILON < previousHealth;
        if (lostHealth && !Scp572ClientEffects.isHeldBy(player)) {
            spawnSplatter(level, player, source, continuous);
        }
        previousHealth = health;
    }

    /**
     * Suppress only the local hurt cue for continuous damage. Direct damage is
     * intentionally left to vanilla or PlayerDamageAudioClient unchanged.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlaySound(PlaySoundEvent event) {
        if (!ClientModulePreferences.contextualDamageFeedbackEnabled()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Scp572ClientEffects.isHeldBy(player)) return;
        DamageSource source = player.getLastDamageSource();
        if (!isContinuousDamage(player, source)) return;

        SoundInstance sound = event.getOriginalSound();
        if (sound == null || !isLocalPlayerSound(sound, player)) return;
        ResourceLocation id = sound.getLocation();
        if (id == null) return;

        String namespace = id.getNamespace();
        String path = id.getPath();
        boolean vanillaHurt = "minecraft".equals(namespace)
                && path.startsWith("entity.player.hurt");
        boolean customHurt = ScpClassifiedDirectiveMod.MODID.equals(namespace)
                && ("player_hurt".equals(path)
                || "voice_profile_b_hurt".equals(path));
        if (vanillaHurt || customHurt) event.setSound(null);
    }

    public static boolean isContinuousDamage(LocalPlayer player,
            DamageSource source) {
        if (player == null || source == null) return false;
        if (source.is(BleedingDamage.TYPE) || source.is(SCP_1079_DAMAGE)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.HOT_FLOOR)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.CRAMMING)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.DRAGON_BREATH)
                || source.is(DamageTypes.DRY_OUT)
                || source.is(DamageTypes.FREEZE)) {
            return true;
        }
        // Poison ticks use generic MAGIC. Restrict that key to an actually
        // poisoned player so direct magical attacks keep direct-hit feedback.
        return source.is(DamageTypes.MAGIC) && player.hasEffect(MobEffects.POISON);
    }

    private static void spawnSplatter(ClientLevel level, LocalPlayer player,
            DamageSource source, boolean continuous) {
        SplatterStyle style = styleFor(player, source, continuous);
        if (style == null) return;

        BlockPos support = findFullBlockBelow(level, player);
        if (support == null) return;

        double jitterX = (player.getRandom().nextDouble() - 0.5D) * 0.48D;
        double jitterZ = (player.getRandom().nextDouble() - 0.5D) * 0.48D;
        double x = Mth.clamp(player.getX() + jitterX,
                support.getX() + 0.12D, support.getX() + 0.88D);
        double z = Mth.clamp(player.getZ() + jitterZ,
                support.getZ() + 0.12D, support.getZ() + 0.88D);
        double y = support.getY() + 1.003D;

        double rotation = player.getRandom().nextDouble() * TWO_PI;
        double variation = 0.90D + player.getRandom().nextDouble() * 0.20D;
        double size = style.size * variation;
        level.addParticle(ScpClassifiedDirectiveModParticleTypes
                        .DAMAGE_SPLATTER.get(),
                x, y, z, size, style.color, rotation);

        float pitch = 0.97F + player.getRandom().nextFloat() * 0.06F;
        float volume = (float) Mth.clamp(0.35D + size * 0.75D,
                0.35D, 0.90D);
        level.playLocalSound(x, y, z,
                ScpClassifiedDirectiveModSounds.DAMAGE_SPLATTER.get(),
                SoundSource.PLAYERS, volume, pitch, false);
    }

    private static SplatterStyle styleFor(LocalPlayer player,
            DamageSource source, boolean continuous) {
        if (source != null && source.is(DamageTypes.MAGIC)
                && player.hasEffect(MobEffects.POISON)) {
            return null;
        }

        float maxHealth = Math.max(1.0F, player.getMaxHealth());
        float healthRatio = Mth.clamp(player.getHealth() / maxHealth,
                0.0F, 1.0F);
        float missing = 1.0F - healthRatio;

        if (source != null && source.is(BleedingDamage.TYPE)) {
            return new SplatterStyle(BLOOD_COLOR,
                    0.22D + missing * 0.34D);
        }
        if (source != null && source.is(SCP_1079_DAMAGE)) {
            return new SplatterStyle(SCP_1079_COLOR,
                    continuous ? 0.16D + missing * 0.08D
                            : 0.50D + missing * 0.16D);
        }
        if (source != null && source.is(DamageTypes.WITHER)) {
            return new SplatterStyle(WITHER_COLOR,
                    0.15D + missing * 0.08D);
        }
        if (continuous) {
            return new SplatterStyle(BLOOD_COLOR,
                    0.14D + missing * 0.07D);
        }
        return new SplatterStyle(BLOOD_COLOR,
                0.24D + missing * 0.09D);
    }

    private static BlockPos findFullBlockBelow(ClientLevel level,
            LocalPlayer player) {
        int x = Mth.floor(player.getX());
        int z = Mth.floor(player.getZ());
        int startY = Mth.floor(player.getBoundingBox().minY - 0.01D);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int offset = 0; offset <= 3; offset++) {
            mutable.set(x, startY - offset, z);
            BlockState state = level.getBlockState(mutable);
            if (state.isAir()) continue;
            VoxelShape collision = state.getCollisionShape(level, mutable);
            if (collision.isEmpty()) continue;
            if (Block.isShapeFullBlock(collision)) {
                return mutable.immutable();
            }
            // A slab, stair, carpet or other partial collision shape is the
            // actual surface. Do not tunnel through it to paint the full block
            // hidden underneath.
            return null;
        }
        return null;
    }

    private static boolean isLocalPlayerSound(SoundInstance sound,
            LocalPlayer player) {
        if (sound.isRelative()) return true;
        double dx = sound.getX() - player.getX();
        double dy = sound.getY() - player.getY();
        double dz = sound.getZ() - player.getZ();
        return dx * dx + dy * dy + dz * dz <= LOCAL_SOUND_RADIUS_SQ;
    }

    private static void reset() {
        trackedPlayerId = null;
        previousHealth = Float.NaN;
    }

    private record SplatterStyle(int color, double size) {
    }
}
