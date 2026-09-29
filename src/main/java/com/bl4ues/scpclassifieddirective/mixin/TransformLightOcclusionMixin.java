package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionClientBridge;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives parametric Surface blocks real vanilla light opacity without placing
 * technical collision blocks into the world grid.
 *
 * Collision already comes from the transformed spatial index. Lighting follows
 * the same rule: the underlying vanilla BlockState keeps its own opacity and a
 * Surface crossing this world cell can only increase it.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class TransformLightOcclusionMixin {
    @Inject(
            method = "getLightBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"), cancellable = true)
    private void scpClassifiedDirective$mergeSurfaceLightBlock(
            BlockGetter level, BlockPos pos,
            CallbackInfoReturnable<Integer> cir) {
        int transformed = transformedLightBlock(level, pos);
        if (transformed > cir.getReturnValue()) {
            cir.setReturnValue(transformed);
        }
    }

    @Inject(
            method = "propagatesSkylightDown(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("RETURN"), cancellable = true)
    private void scpClassifiedDirective$blockSurfaceSkylight(
            BlockGetter level, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && transformedLightBlock(level, pos) > 0) {
            cir.setReturnValue(false);
        }
    }

    private static int transformedLightBlock(BlockGetter level, BlockPos pos) {
        if (!(level instanceof Level world) || pos == null) return 0;
        return world.isClientSide
                ? TransformConstructionClientBridge.lightBlock(pos)
                : TransformConstructionManager.proxyLightBlock(level, pos);
    }
}
