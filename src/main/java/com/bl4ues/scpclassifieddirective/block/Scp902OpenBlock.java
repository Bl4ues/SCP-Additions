package com.bl4ues.scpclassifieddirective.block;

import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

import java.util.Collections;
import java.util.List;

/** Runtime open form of SCP-902. It always yields the canonical closed item. */
public final class Scp902OpenBlock extends Scp902BlockBase {
    public Scp902OpenBlock() {
        super();
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("The Final Countdown"));
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> vanilla = super.getDrops(state, builder);
        return vanilla.isEmpty()
                ? Collections.singletonList(new ItemStack(
                        ScpClassifiedDirectiveModBlocks.SCP_902_CLOSED.get()))
                : vanilla;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target,
            BlockGetter level, net.minecraft.core.BlockPos pos, Player player) {
        return new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_902_CLOSED.get());
    }
}
