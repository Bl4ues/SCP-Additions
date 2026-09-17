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
            if (!FacilityMappingManager.detachCamera(serverPlayer,
                    context.getClickedPos())) {
                FacilityMappingManager.openEditor(serverPlayer,
                        context.getClickedPos());
            }
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
                "Authors room floors, precise outlines and camera associations.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Contextual builder controls are shown on-screen while held.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
