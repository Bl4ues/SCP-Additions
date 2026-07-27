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
    private static final String PLAYER_ATTACK_PREFIX = "entity.player.attack.";
    private static long suppressMobImpactUntilTick = Long.MIN_VALUE;

    private PlayerDamageAudioClient() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || event.getEntity() != minecraft.player) {
            return;
        }
        if (event.getTarget() instanceof LivingEntity
                && !(event.getTarget() instanceof Player)) {
            suppressMobImpactUntilTick = minecraft.level.getGameTime() + 4L;
        }
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance original = event.getOriginalSound();
        if (original == null) return;
        ResourceLocation location = original.getLocation();
        if (location == null) return;

        if (InventoryModuleRuntimeState.replacePlayerHurtSoundsForClient()
                && "minecraft".equals(location.getNamespace())
                && location.getPath().startsWith(PLAYER_HURT_PREFIX)) {
            RandomSource random = RandomSource.create();
            float pitch = Mth.clamp(original.getPitch()
                    * (0.96F + random.nextFloat() * 0.08F), 0.5F, 2.0F);
            event.setSound(new SimpleSoundInstance(
                    ScpAdditionsModSounds.PLAYER_HURT.get().getLocation(),
                    original.getSource(), original.getVolume(), pitch, random,
                    original.isLooping(), original.getDelay(),
                    original.getAttenuation(), original.getX(), original.getY(),
                    original.getZ(), original.isRelative()));
            return;
        }

        if (InventoryModuleRuntimeState.muteNonPlayerHitSoundsForClient()
                && "minecraft".equals(location.getNamespace())
                && location.getPath().startsWith(PLAYER_ATTACK_PREFIX)
                && isNonPlayerMobImpact()) {
            event.setSound(null);
        }
    }

    private static boolean isNonPlayerMobImpact() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return false;
        if (minecraft.level.getGameTime() <= suppressMobImpactUntilTick) {
            return true;
        }
        return minecraft.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity
                && !(hit.getEntity() instanceof Player);
    }
}
