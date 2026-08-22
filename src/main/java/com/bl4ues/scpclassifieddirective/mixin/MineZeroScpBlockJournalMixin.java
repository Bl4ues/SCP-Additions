package com.bl4ues.scpclassifieddirective.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.bl4ues.scpclassifieddirective.compat.MineZeroScpCheckpoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Journals the original state before any server-side SCP: Classified Directive block mutation. */
@Mixin(Level.class)
public abstract class MineZeroScpBlockJournalMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), require = 0)
    private void scpClassifiedDirective$journalFourArgSetBlock(BlockPos pos,
            BlockState replacement, int flags, int recursionLeft,
            CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof ServerLevel serverLevel) {
            MineZeroScpCheckpoint.recordBlockBeforeChange(serverLevel, pos,
                    replacement);
        }
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            at = @At("HEAD"), require = 0)
    private void scpClassifiedDirective$journalThreeArgSetBlock(BlockPos pos,
            BlockState replacement, int flags,
            CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof ServerLevel serverLevel) {
            MineZeroScpCheckpoint.recordBlockBeforeChange(serverLevel, pos,
                    replacement);
        }
    }
}
