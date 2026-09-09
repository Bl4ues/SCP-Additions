package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderPhysicalGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Legacy reader blocks live at the wall cell while their authored models sit to
 * the left/right of it. Relocate authorization audio to the actual model body so
 * image and sound agree spatially.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class KeycardReaderSoundPositionClient {
    private KeycardReaderSoundPositionClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getOriginalSound();
        ClientLevel level = Minecraft.getInstance().level;
        if (sound == null || level == null || sound.isRelative()) return;

        ResourceLocation id = sound.getLocation();
        if (id == null || !ScpClassifiedDirectiveMod.MODID.equals(id.getNamespace())
                || !("accessgranted".equals(id.getPath())
                || "accessdenied".equals(id.getPath()))) {
            return;
        }

        BlockPos sourcePos = BlockPos.containing(sound.getX(), sound.getY(),
                sound.getZ());
        if (!level.isLoaded(sourcePos)) return;
        Vec3 physical = KeycardReaderPhysicalGeometry.soundPosition(sourcePos,
                level.getBlockState(sourcePos));
        if (physical == null) return;

        event.setSound(new SimpleSoundInstance(id, sound.getSource(), 1.0F, 1.0F,
                RandomSource.create(), false, 0,
                SoundInstance.Attenuation.LINEAR,
                physical.x, physical.y, physical.z, false));
    }
}
