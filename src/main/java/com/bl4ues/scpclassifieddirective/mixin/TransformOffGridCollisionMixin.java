package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionClientBridge;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds only the free part of rigid Off-Grid collision to an occupied vanilla
 * cell. The ordinary block keeps its own shape and Surface construction is
 * deliberately excluded from this path.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class TransformOffGridCollisionMixin {
    @Inject(
            method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("RETURN"), cancellable = true)
    private void scpClassifiedDirective$mergeOffGridCollision(
            BlockGetter level, BlockPos pos, CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir) {
        if (!(level instanceof Level world) || pos == null) return;
        BlockState self = (BlockState) (Object) this;
        if (self.is(com.bl4ues.scpclassifieddirective.facility.transform
                .TransformConstructionModule.getProxy())) {
            // Proxy collision already is the complete transformed shape.
            return;
        }

        VoxelShape vanilla = cir.getReturnValue();
        // Most facility walls/floors are full cubes. A transformed contribution
        // cannot add any collision inside this BlockPos, so avoid even touching
        // the transformed spatial index on this extremely hot path.
        if (vanilla == Shapes.block()) return;

        VoxelShape transformed = world.isClientSide
                ? TransformConstructionClientBridge.transformedCollision(pos)
                : TransformConstructionManager.transformedCollisionShape(
                        level, pos);
        if (transformed == null || transformed.isEmpty()) return;

        if (vanilla == null || vanilla.isEmpty()) {
            cir.setReturnValue(transformed);
            return;
        }
        cir.setReturnValue(Shapes.joinUnoptimized(vanilla, transformed,
                BooleanOp.OR));
    }
}
