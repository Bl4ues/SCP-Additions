package com.bl4ues.scpclassifieddirective.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import com.bl4ues.scpclassifieddirective.compat.MineZeroCompatibility;
import com.bl4ues.scpclassifieddirective.compat.MineZeroSaveSafety;
import com.bl4ues.scpclassifieddirective.event.SaveGameSoundEvents;
import com.bl4ues.scpclassifieddirective.save.SaveGameContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures vanilla, command and modded respawn-point writes in one pipeline. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSaveTrackingMixin {
    @Inject(method = "setRespawnPosition", at = @At("HEAD"), cancellable = true)
    private void scpClassifiedDirective$rejectUnsafeMineZeroSave(
            ResourceKey<Level> dimension, BlockPos position, float angle,
            boolean forced, boolean sendMessage, CallbackInfo callback) {
        if (position == null) return;

        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.server.getWorldData().isHardcore()) {
            SaveGameSoundEvents.showSaveBlocked(player);
            callback.cancel();
            return;
        }

        if (!MineZeroCompatibility.enabled()
                || MineZeroCompatibility.restoring()) return;

        if (player.connection == null
                || MineZeroSaveSafety.canSave(player.server)) return;

        SaveGameSoundEvents.showSaveBlocked(player);
        callback.cancel();
    }

    @Inject(method = "setRespawnPosition", at = @At("TAIL"))
    private void scpClassifiedDirective$trackSave(ResourceKey<Level> dimension,
            BlockPos position, float angle, boolean forced,
            boolean sendMessage, CallbackInfo callback) {
        if (position == null) return;
        SaveGameSoundEvents.recordRespawnPointSet(
                (ServerPlayer) (Object) this, SaveGameContext.current());
    }
}
