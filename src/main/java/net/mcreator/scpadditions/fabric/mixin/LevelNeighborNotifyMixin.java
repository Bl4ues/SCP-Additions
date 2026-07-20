package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
abstract class LevelNeighborNotifyMixin {
    @Inject(
            method = "updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V",
            at = @At("HEAD"))
    private void scpAdditions$neighborNotify(
            BlockPos pos,
            Block source,
            CallbackInfo callback) {
        Level level = (Level) (Object) this;
        NeoForge.EVENT_BUS.post(new BlockEvent.NeighborNotifyEvent(
                level, pos, level.getBlockState(pos)));
    }
}
