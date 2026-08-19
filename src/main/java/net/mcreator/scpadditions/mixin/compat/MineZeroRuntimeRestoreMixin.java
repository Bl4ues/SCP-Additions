package net.mcreator.scpadditions.mixin.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.mcreator.scpadditions.compat.MineZeroRuntimeRestoreSupport;
import net.mcreator.scpadditions.compat.MineZeroScpCheckpoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Completes MineZero rollback for SCP Additions runtime-only state. */
@Mixin(value = MineZeroScpCheckpoint.class, remap = false)
public abstract class MineZeroRuntimeRestoreMixin {
    @Inject(method = "capture", at = @At("TAIL"))
    private static void scpAdditions$captureRuntimeState(
            MinecraftServer server, CallbackInfo callback) {
        MineZeroRuntimeRestoreSupport.captureCarriages(server);
    }

    @Inject(method = "restoreBlockDiff", at = @At("TAIL"))
    private static void scpAdditions$rehydrateRestoredBlocks(
            MinecraftServer server, CompoundTag diff, CallbackInfo callback) {
        MineZeroRuntimeRestoreSupport.rehydrateBlockDiff(server, diff);
    }

    @Inject(method = "restore", at = @At("TAIL"))
    private static void scpAdditions$restoreRuntimeState(
            MinecraftServer server, CallbackInfo callback) {
        MineZeroRuntimeRestoreSupport.restoreCarriages(server);
    }
}
