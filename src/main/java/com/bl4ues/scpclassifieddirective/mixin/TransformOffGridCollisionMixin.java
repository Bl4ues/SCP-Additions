package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionClientBridge;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
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

        VoxelShape offGrid = world.isClientSide
                ? TransformConstructionClientBridge.offGridCollision(pos)
                : TransformConstructionManager.offGridCollisionShape(level, pos);
        if (offGrid == null || offGrid.isEmpty()) return;

        VoxelShape vanilla = cir.getReturnValue();
        if (vanilla == null || vanilla.isEmpty()) {
            cir.setReturnValue(offGrid);
            return;
        }

        VoxelShape free = Shapes.joinUnoptimized(offGrid, vanilla,
                BooleanOp.ONLY_FIRST);
        if (!free.isEmpty()) {
            cir.setReturnValue(Shapes.or(vanilla, free).optimize());
        }
    }
}
