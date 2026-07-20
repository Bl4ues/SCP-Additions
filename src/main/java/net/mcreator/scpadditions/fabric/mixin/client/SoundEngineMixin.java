package net.mcreator.scpadditions.fabric.mixin.client;

import java.util.Map;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
abstract class SoundEngineMixin {
    @Shadow @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Inject(
            method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V",
            at = @At("RETURN"))
    private void scpAdditions$registerStartedSound(
            SoundInstance sound,
            CallbackInfo callback) {
        ChannelAccess.ChannelHandle handle = instanceToChannel.get(sound);
        if (handle == null) return;

        SoundEngine engine = (SoundEngine) (Object) this;
        handle.execute(channel -> {
            Sound resolved = sound.getSound();
            if (resolved != null && resolved.shouldStream()) {
                NeoForge.EVENT_BUS.post(
                        new PlayStreamingSourceEvent(engine, channel, sound));
            } else {
                NeoForge.EVENT_BUS.post(
                        new PlaySoundSourceEvent(engine, channel, sound));
            }
        });
    }
}
