package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
abstract class BlockItemPlaceMixin {
    @Inject(
            method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN"))
    private void scpAdditions$afterBlockPlaced(
            BlockPlaceContext context,
            CallbackInfoReturnable<InteractionResult> callback) {
        if (!callback.getReturnValue().consumesAction()) return;
        Level level = context.getLevel();
        if (level.isClientSide()) return;
        BlockPos pos = context.getClickedPos();
        NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(
                level, pos, level.getBlockState(pos), context.getPlayer()));
    }
}
