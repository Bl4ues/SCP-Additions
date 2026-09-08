package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079SpeakerCueSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Client-only low-tech playback for the device's borrowed SCP-079 interface cues. */
public final class HackingDeviceAudioClient {
    private static final ResourceLocation LOW_CUE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "scp079_1");
    private static final ResourceLocation HIGH_CUE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "scp079_2");

    private HackingDeviceAudioClient() {
    }

    public static void playInterfaceCue(BlockPos pos, boolean high) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || pos == null) return;
        ResourceLocation cue = high ? HIGH_CUE : LOW_CUE;
        minecraft.getSoundManager().play(new Scp079SpeakerCueSoundInstance(
                cue,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                0.42F,
                high ? 0.82F : 0.72F));
    }
}
