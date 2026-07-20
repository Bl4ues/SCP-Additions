package net.mcreator.scpadditions.fabric;

import java.lang.reflect.Method;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

public final class FabricItemHandlers {
    private FabricItemHandlers() {}
    public static IItemHandler find(ItemStack stack) { return null; }
    public static IItemHandler find(Entity entity) {
        if (entity instanceof Player player) return new SidedInvWrapper(player.getInventory(), null);
        return entity instanceof Container container ? new SidedInvWrapper(container, null) : null;
    }
    public static IItemHandler find(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return null;
        try {
            Method method = blockEntity.getClass().getMethod("getItemHandler", Direction.class);
            return (IItemHandler) method.invoke(blockEntity, side);
        } catch (ReflectiveOperationException ignored) {
            return blockEntity instanceof Container container ? new SidedInvWrapper(container, side) : null;
        }
    }
}
