package com.bl4ues.scpclassifieddirective.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.Collections;
import java.util.List;

/** Obtainable closed form of SCP-902. Rendering/animation is owned by the BE. */
public final class Scp902ClosedBlock extends Scp902BlockBase {
    public Scp902ClosedBlock() {
        super();
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("The Anticipation is Killing Me"));
    }

    @Override
    public List<ItemStack> getDrops(net.minecraft.world.level.block.state.BlockState state,
            LootParams.Builder builder) {
        List<ItemStack> vanilla = super.getDrops(state, builder);
        return vanilla.isEmpty()
                ? Collections.singletonList(new ItemStack(this)) : vanilla;
    }
}
