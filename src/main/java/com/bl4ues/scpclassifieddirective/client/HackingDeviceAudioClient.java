package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079SpeakerCueSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Client-only low-tech playback for the device's borrowed SCP-079 cues. */
public final class HackingDeviceAudioClient {
    private static final ResourceLocation ATTACH_CUE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "scp079_1");
    private static final ResourceLocation DETACH_CUE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "scp079_2");

    private HackingDeviceAudioClient() {
    }

    public static void playAttachmentCue(BlockPos pos, boolean attached) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || pos == null) return;
        ResourceLocation cue = attached ? ATTACH_CUE : DETACH_CUE;
        minecraft.getSoundManager().play(new Scp079SpeakerCueSoundInstance(
                cue,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                0.62F,
                1.0F));
    }
}
