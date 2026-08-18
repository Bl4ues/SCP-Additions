package net.mcreator.scpadditions.mixin.compat;

import net.minecraftforge.event.TickEvent;
import net.mcreator.scpadditions.compat.MineZeroCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** MineZero checkpoints are created only by explicit SCP Additions save events. */
@Pseudo
@Mixin(targets = "boomcow.minezero.event.CheckpointTicker", remap = false)
public abstract class MineZeroCheckpointTickerMixin {
    @Inject(method = "onServerTick", at = @At("HEAD"), cancellable = true,
            require = 0)
    private static void scpAdditions$disableAutomaticCheckpointTicker(
            TickEvent.ServerTickEvent event, CallbackInfo callback) {
        if (MineZeroCompatibility.enabled()) callback.cancel();
    }
}
