package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionStartupState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.level.ChunkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents transformed proxy indexing from hijacking Minecraft's synchronous
 * spawn-region preparation. Once ServerStartedEvent fires, the normal manager
 * behavior is restored without changing authored construction data.
 */
@Mixin(value = TransformConstructionManager.class, remap = false)
public abstract class TransformConstructionStartupMixin {
    @Inject(method = "onChunkLoad", at = @At("HEAD"), cancellable = true,
            remap = false)
    private static void scpClassifiedDirective$deferChunkProxyRepair(
            ChunkEvent.Load event, CallbackInfo callback) {
        if (event.getLevel() instanceof ServerLevel level
                && !TransformConstructionStartupState.isReady(level.getServer())) {
            callback.cancel();
        }
    }

    @Inject(method = "proxySelectionShape", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpClassifiedDirective$deferSelectionIndex(
            BlockGetter getter, BlockPos pos,
            CallbackInfoReturnable<VoxelShape> callback) {
        if (getter instanceof ServerLevel level
                && !TransformConstructionStartupState.isReady(level.getServer())) {
            callback.setReturnValue(Shapes.empty());
        }
    }

    @Inject(method = "proxyCollisionShape", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpClassifiedDirective$deferCollisionIndex(
            BlockGetter getter, BlockPos pos,
            CallbackInfoReturnable<VoxelShape> callback) {
        if (getter instanceof ServerLevel level
                && !TransformConstructionStartupState.isReady(level.getServer())) {
            callback.setReturnValue(Shapes.empty());
        }
    }

    @Inject(method = "offGridCollisionShape", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpClassifiedDirective$deferOffGridCollisionIndex(
            BlockGetter getter, BlockPos pos,
            CallbackInfoReturnable<VoxelShape> callback) {
        if (getter instanceof ServerLevel level
                && !TransformConstructionStartupState.isReady(level.getServer())) {
            callback.setReturnValue(Shapes.empty());
        }
    }

    @Inject(method = "proxyLight", at = @At("HEAD"), cancellable = true,
            remap = false)
    private static void scpClassifiedDirective$deferLightIndex(
            BlockGetter getter, BlockPos pos,
            CallbackInfoReturnable<Integer> callback) {
        if (getter instanceof ServerLevel level
                && !TransformConstructionStartupState.isReady(level.getServer())) {
            callback.setReturnValue(0);
        }
    }
}
