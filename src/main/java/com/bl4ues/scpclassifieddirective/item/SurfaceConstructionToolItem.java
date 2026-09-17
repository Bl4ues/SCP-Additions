package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
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

/** Creative authoring tool for editable curved/tilted construction surfaces. */
public final class SurfaceConstructionToolItem extends Item {
    public SurfaceConstructionToolItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return context.getPlayer() != null && context.getPlayer().isCreative()
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!TransformConstructionManager.canEdit(player)) {
            return InteractionResult.FAIL;
        }
        TransformConstructionManager.selectSurfacePoint(player,
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
        if (player instanceof ServerPlayer serverPlayer) {
            TransformConstructionManager.cancelSurface(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Surface Construction Tool");
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(
                "Authors resizable, tiltable and curved block surfaces.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Right-click twice: define the wall baseline; it starts 3 blocks high.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Left-click the surface: select the nearest corner or center handle.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "G + X/Y/Z: hold Attack and drag a handle along that axis.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Corner handles tilt/resize the wall; the center handle bends it.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Shift while dragging snaps to the 1/16-block authoring grid.")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal(
                "Static blocks bend with the surface; equipment attaches rigidly.")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal(
                "V while aiming at a placed surface block toggles Deform / Rigid.")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal(
                "Shift + right-click air: cancel the pending baseline.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
