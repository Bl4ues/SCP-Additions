package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.facility.Scp079RoleSelection;
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

/**
 * Placeholder entry point for playable SCP roles. For now SCP-079 is the only
 * implemented role, so use simply toggles that role on the nearest registered
 * SCP-079 host in the current dimension.
 */
public final class ScpRoleSelectorItem extends Item {
    public ScpRoleSelectorItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
            InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            boolean changed = Scp079RoleSelection.toggle(serverPlayer);
            return changed ? InteractionResultHolder.success(stack)
                    : InteractionResultHolder.fail(stack);
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
                "Temporary Creative tool for testing playable SCP roles.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Right-click: assume/release SCP-079")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Uses the nearest registered SCP-079 computer in this dimension.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
