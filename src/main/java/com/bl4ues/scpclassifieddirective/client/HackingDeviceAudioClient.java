package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079SpeakerCueSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Positional, narrow-band playback for Hacking Device UI feedback. The source
 * samples are the same 079select_* cues used by SCP-079's camera interface,
 * deliberately pitched down and routed through the facility-Speaker filter so
 * they sound like a cheap speaker inside the improvised handheld.
 */
public final class HackingDeviceAudioClient {
    private static final ResourceLocation NAVIGATE = id("079select_1");
    private static final ResourceLocation STATUS = id("079select_2");
    private static final ResourceLocation DENIED = id("079select_3");
    private static final ResourceLocation CONFIRM = id("079select_4");

    private HackingDeviceAudioClient() {
    }

    public static void playNavigate(BlockPos pos) {
        play(pos, NAVIGATE, 0.28F, 0.68F);
    }

    public static void playStatus(BlockPos pos) {
        play(pos, STATUS, 0.31F, 0.64F);
    }

    public static void playDenied(BlockPos pos) {
        play(pos, DENIED, 0.36F, 0.58F);
    }

    public static void playConfirm(BlockPos pos) {
        play(pos, CONFIRM, 0.34F, 0.66F);
    }

    public static void playBootPulse(BlockPos pos, int line) {
        if ((line & 1) == 0) playStatus(pos);
        else playNavigate(pos);
    }

    private static void play(BlockPos pos, ResourceLocation cue,
            float volume, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || pos == null) return;
        minecraft.getSoundManager().play(new Scp079SpeakerCueSoundInstance(
                cue,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                volume,
                pitch));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
    }
}
