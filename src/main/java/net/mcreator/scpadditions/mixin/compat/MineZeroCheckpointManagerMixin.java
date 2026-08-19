package net.mcreator.scpadditions.mixin.compat;

import net.minecraft.server.level.ServerPlayer;
import net.mcreator.scpadditions.compat.MineZeroCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Under compatibility, MineZero may only create or restore checkpoints through
 * the SCP Additions save/death pipeline. This blocks its flute, commands and
 * any future direct calls from bypassing save-safety and cooperative voting.
 */
@Pseudo
@Mixin(targets = "boomcow.minezero.checkpoint.CheckpointManager", remap = false)
public abstract class MineZeroCheckpointManagerMixin {
    @Inject(method = "setCheckpoint", at = @At("HEAD"), cancellable = true,
            require = 0)
    private static void scpAdditions$guardCheckpointCreation(
            ServerPlayer player, CallbackInfo callback) {
        if (!MineZeroCompatibility.enabled()
                || MineZeroCompatibility.creatingCheckpoint()) return;
        callback.cancel();
    }

    @Inject(method = "restoreCheckpoint", at = @At("HEAD"), cancellable = true,
            require = 0)
    private static void scpAdditions$guardCheckpointRestore(
            ServerPlayer player, CallbackInfo callback) {
        if (!MineZeroCompatibility.enabled()
                || MineZeroCompatibility.restoring()) return;
        callback.cancel();
    }
}
