package net.mcreator.scpadditions.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.mcreator.scpadditions.equipment.HazmatSuitManager;

import java.util.List;

/** Public single-item representation of the complete Hazmat Suit. */
public final class HazmatSuitItem extends Item {
    public HazmatSuitItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
            InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!HazmatSuitManager.canBeginEquip(player)) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                HazmatSuitManager.explainBlockedEquip(serverPlayer);
            }
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            HazmatSuitManager.beginEquip(serverPlayer);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return HazmatSuitManager.EQUIP_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level,
            LivingEntity livingEntity, int timeCharged) {
        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            HazmatSuitManager.cancelEquip(player);
        }
        super.releaseUsing(stack, level, livingEntity, timeCharged);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level,
            LivingEntity livingEntity) {
        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            if (HazmatSuitManager.completeEquip(player)
                    && !player.isCreative()) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip.scp_additions.hazmat_suit.sealed")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.scp_additions.hazmat_suit.armor")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.scp_additions.hazmat_suit.equip")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(
                "tooltip.scp_additions.hazmat_suit.remove")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
