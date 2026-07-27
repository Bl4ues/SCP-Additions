package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

/** Client-only replacement and filtering for player combat audio. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class PlayerDamageAudioClient {
    private static final String PLAYER_HURT_PREFIX = "entity.player.hurt";
    private static final String PLAYER_DEATH_PREFIX = "entity.player.death";
    private static final String PLAYER_ATTACK_PREFIX = "entity.player.attack.";
    private static final double LOCAL_PLAYER_SOUND_RADIUS_SQ = 9.0D;
    private static final long MOB_IMPACT_WINDOW_NANOS = 300_000_000L;
    private static volatile long suppressMobImpactUntilNanos = Long.MIN_VALUE;

    private PlayerDamageAudioClient() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !event.getEntity().getUUID().equals(
                minecraft.player.getUUID())) {
            return;
        }
        if (event.getTarget() instanceof LivingEntity
                && !(event.getTarget() instanceof Player)) {
            suppressMobImpactUntilNanos = System.nanoTime()
                    + MOB_IMPACT_WINDOW_NANOS;
        }
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance original = event.getOriginalSound();
        if (original == null) return;
        ResourceLocation location = original.getLocation();
        if (location == null) return;

        boolean minecraftSound = "minecraft".equals(location.getNamespace());
        boolean localPlayerSound = isLocalPlayerSound(original);
        String path = location.getPath();

        if (InventoryModuleRuntimeState.replacePlayerHurtSoundsForClient()
                && minecraftSound && localPlayerSound) {
            if (path.startsWith(PLAYER_HURT_PREFIX)) {
                event.setSound(localHurtReplacement(original));
                return;
            }
            if (path.startsWith(PLAYER_DEATH_PREFIX)) {
                event.setSound(null);
                return;
            }
        }

        if (InventoryModuleRuntimeState.muteNonPlayerHitSoundsForClient()
                && minecraftSound && localPlayerSound
                && path.startsWith(PLAYER_ATTACK_PREFIX)
                && isNonPlayerMobImpact()) {
            event.setSound(null);
        }
    }

    private static SoundInstance localHurtReplacement(SoundInstance original) {
        RandomSource random = RandomSource.create();
        float pitch = Mth.clamp(original.getPitch()
                * (0.96F + random.nextFloat() * 0.08F), 0.5F, 2.0F);
        return new SimpleSoundInstance(
                ScpAdditionsModSounds.PLAYER_HURT.get().getLocation(),
                original.getSource(), original.getVolume(), pitch, random,
                false, 0, SoundInstance.Attenuation.NONE,
                0.0D, 0.0D, 0.0D, true);
    }

    private static boolean isLocalPlayerSound(SoundInstance sound) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;
        if (sound.isRelative()) return true;
        double dx = sound.getX() - minecraft.player.getX();
        double dy = sound.getY() - minecraft.player.getY();
        double dz = sound.getZ() - minecraft.player.getZ();
        return dx * dx + dy * dy + dz * dz
                <= LOCAL_PLAYER_SOUND_RADIUS_SQ;
    }

    private static boolean isNonPlayerMobImpact() {
        Minecraft minecraft = Minecraft.getInstance();
        if (System.nanoTime() <= suppressMobImpactUntilNanos) return true;
        return minecraft.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity
                && !(hit.getEntity() instanceof Player);
    }
}
