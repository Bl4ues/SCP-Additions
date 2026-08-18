package net.mcreator.scpadditions.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.mcreator.scpadditions.compat.MineZeroCompatibility;
import net.mcreator.scpadditions.compat.MineZeroSaveSafety;
import net.mcreator.scpadditions.event.SaveGameSoundEvents;
import net.mcreator.scpadditions.save.SaveGameContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures vanilla, command and modded respawn-point writes in one pipeline. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSaveTrackingMixin {
    @Inject(method = "setRespawnPosition", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$rejectUnsafeMineZeroSave(
            ResourceKey<Level> dimension, BlockPos position, float angle,
            boolean forced, boolean sendMessage, CallbackInfo callback) {
        if (position == null || !MineZeroCompatibility.enabled()
                || MineZeroCompatibility.restoring()) return;

        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.connection == null
                || MineZeroSaveSafety.canSave(player.server)) return;

        player.displayClientMessage(
                Component.literal("You can't save right now"), true);
        callback.cancel();
    }

    @Inject(method = "setRespawnPosition", at = @At("TAIL"))
    private void scpAdditions$trackSave(ResourceKey<Level> dimension,
            BlockPos position, float angle, boolean forced,
            boolean sendMessage, CallbackInfo callback) {
        if (position == null) return;
        SaveGameSoundEvents.recordRespawnPointSet(
                (ServerPlayer) (Object) this, SaveGameContext.current());
    }
}
