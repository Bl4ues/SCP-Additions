package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.intercom.IntercomWorldSoundBridge;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures the two authoritative ServerLevel positional-sound paths. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelIntercomSoundMixin {
    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"))
    private void scpclassifieddirective$capturePositionSound(Player excluded,
            double x, double y, double z, Holder<SoundEvent> sound,
            SoundSource source, float volume, float pitch, long seed,
            CallbackInfo ci) {
        if (sound == null || sound.value() == null) return;
        IntercomWorldSoundBridge.relayServerSound((ServerLevel) (Object) this,
                new Vec3(x, y, z), sound.value().getLocation(), volume, pitch);
    }

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"))
    private void scpclassifieddirective$captureEntitySound(Player excluded,
            Entity entity, Holder<SoundEvent> sound, SoundSource source,
            float volume, float pitch, long seed, CallbackInfo ci) {
        if (entity == null || sound == null || sound.value() == null) return;
        IntercomWorldSoundBridge.relayServerSound((ServerLevel) (Object) this,
                entity.position(), sound.value().getLocation(), volume, pitch);
    }
}
