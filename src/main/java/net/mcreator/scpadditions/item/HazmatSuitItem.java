package net.mcreator.scpadditions.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.mcreator.scpadditions.equipment.HazmatSuitManager;

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
    public int getUseDuration(ItemStack stack) {
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
}
