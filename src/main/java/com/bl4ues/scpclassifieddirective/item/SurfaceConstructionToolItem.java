package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceAuthoringManager;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceAuthoringState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Creative authoring tool for editable curved/tilted construction surfaces. */
public final class SurfaceConstructionToolItem extends Item {
    public SurfaceConstructionToolItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isCreative()) return InteractionResult.FAIL;
        if (!(player instanceof ServerPlayer serverPlayer)) {
            if (context.getLevel().isClientSide) {
                TransformSurfaceAuthoringState.select(context.getClickLocation());
            }
            return InteractionResult.SUCCESS;
        }
        if (!TransformConstructionManager.canEdit(serverPlayer)) {
            return InteractionResult.FAIL;
        }
        TransformSurfaceAuthoringManager.selectPoint(serverPlayer,
                context.getClickLocation());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
            InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !player.isCreative()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) TransformSurfaceAuthoringState.clear();
        if (player instanceof ServerPlayer serverPlayer) {
            TransformSurfaceAuthoringManager.cancel(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos,
            Player player) {
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Surface Construction Tool");
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(
                "Builds editable straight, tilted and curved construction surfaces.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Practical controls and the current authoring step are shown on-screen.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
