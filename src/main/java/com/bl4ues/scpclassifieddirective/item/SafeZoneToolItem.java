package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.safezone.SafeZoneManager;
import net.minecraft.ChatFormatting;
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

/** Creative operator tool used to define and edit persistent Safe Zones. */
public final class SafeZoneToolItem extends Item {
    public SafeZoneToolItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return player != null && player.canUseGameMasterBlocks()
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!SafeZoneManager.canEdit(serverPlayer)) {
            return InteractionResult.FAIL;
        }

        if (serverPlayer.isShiftKeyDown()) {
            SafeZoneManager.openEditor(serverPlayer,
                    context.getClickedPos());
        } else {
            SafeZoneManager.completeSelection(serverPlayer,
                    context.getClickedPos());
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
            InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !player.canUseGameMasterBlocks()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            SafeZoneManager.cancelSelection(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack,
                level.isClientSide);
    }

    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state,
            Level level, net.minecraft.core.BlockPos pos, Player player) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Left-click: select the first corner")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click: create the Safe Zone")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Shift + right-click: edit a zone or cancel in air")
                .withStyle(ChatFormatting.GRAY));
    }
}
