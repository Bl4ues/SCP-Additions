package com.bl4ues.scpclassifieddirective.anomalous;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.List;

public final class Item006Block extends Block {
    private static final VoxelShape SHAPE =
            box(4.75D, 0.0D, 5.0D, 11.0D, 4.0D, 10.75D);

    public Item006Block() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.AMETHYST)
                .strength(1.5F, 3.0F)
                .lightLevel(state -> 12)
                .noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false));
    }

    @Override
    public MutableComponent getName() {
        return Component.literal("Item #006, Vol. I");
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("Glowing Rock").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            net.minecraft.core.BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            net.minecraft.core.BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> original = super.getDrops(state, builder);
        if (!original.isEmpty()) return original;
        return Collections.singletonList(new ItemStack(this));
    }
}
