package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceAuthoringMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceAuthoringState;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.Vec3;

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
        if (!TransformConstructionManager.canEdit(player)) {
            return InteractionResult.FAIL;
        }
        if (context.getLevel().isClientSide) {
            Vec3 point = TransformSurfaceAuthoringMath.resolve(player,
                    context.getClickLocation(),
                    TransformSurfaceAuthoringState.start(),
                    TransformSurfaceAuthoringState.end(),
                    player.isShiftKeyDown());
            TransformSurfaceAuthoringState.select(point);
            TransformConstructionNetwork.authorSurfacePoint(point);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
            InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isCreative()) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) {
            Vec3 point = TransformSurfaceAuthoringMath.resolve(player, null,
                    TransformSurfaceAuthoringState.start(),
                    TransformSurfaceAuthoringState.end(),
                    player.isShiftKeyDown());
            TransformSurfaceAuthoringState.select(point);
            TransformConstructionNetwork.authorSurfacePoint(point);
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
        tooltip.add(Component.literal(
                "Press B to link edges from two existing Surfaces into a parent-anchored curved plane.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
