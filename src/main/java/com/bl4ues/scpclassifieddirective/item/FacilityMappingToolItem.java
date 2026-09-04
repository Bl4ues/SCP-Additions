package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
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

import java.util.List;

/** Creative map-making tool that authors logical room floors for surveillance. */
public final class FacilityMappingToolItem extends Item {
    public FacilityMappingToolItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return player != null && player.isCreative()
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!FacilityMappingManager.canEdit(serverPlayer)) {
            return InteractionResult.FAIL;
        }
        if (serverPlayer.isShiftKeyDown()) {
            FacilityMappingManager.openEditor(serverPlayer,
                    context.getClickedPos());
        } else {
            FacilityMappingManager.completeSelection(serverPlayer,
                    context.getClickedPos());
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
            InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !player.isCreative()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            FacilityMappingManager.cancelSelection(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack,
                level.isClientSide);
    }

    @Override
    public boolean canAttackBlock(
            net.minecraft.world.level.block.state.BlockState state,
            Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Facility Mapping Tool");
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(
                "Defines room floors used by facility surveillance maps.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Only select the floor area of the room; height is not needed.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Overlapping selections are added to the same mapped room.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Left-click: select the first floor corner")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click: add the selected floor area")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Shift + right-click: edit a room or cancel in air")
                .withStyle(ChatFormatting.GRAY));
    }
}
