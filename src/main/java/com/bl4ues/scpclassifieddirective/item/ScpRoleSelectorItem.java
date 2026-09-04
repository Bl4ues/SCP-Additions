package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.facility.Scp079RoleSelection;
import com.bl4ues.scpclassifieddirective.network.ScpRoleSelectorNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Placeholder item appearance backed by the real playable SCP selector UI. */
public final class ScpRoleSelectorItem extends Item {
    public ScpRoleSelectorItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
            InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (!Scp079RoleSelection.canOpenSelector(serverPlayer)) {
                return InteractionResultHolder.fail(stack);
            }
            ScpRoleSelectorNetwork.openSelector(serverPlayer);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, true);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("SCP Role Selector [Placeholder]");
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(
                "Temporary admin item appearance; playable role selection is functional.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Right-click: open the Playable SCP Selector")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "SCP-079 currently requires a registered physical computer in this dimension.")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal(
                "While controlling SCP-079: sneak + right-click to reopen the selector.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
