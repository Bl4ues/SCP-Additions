package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Creative authoring tool for rigid local grids that may leave vanilla axes. */
public final class OffGridConstructionToolItem extends Item {
    public OffGridConstructionToolItem() {
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
        TransformConstructionManager.createGroup(player, context.getClickLocation(),
                context.getClickedFace());
        return InteractionResult.CONSUME;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Off-Grid Construction Tool");
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(
                "Creates a movable local 1x1x1 construction grid outside vanilla alignment.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Right-click a surface: create an off-grid cell.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Place blocks on transformed cells to extend their local grid.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Left-click a transformed cell: select its local grid.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "G/R: move or rotate; X/Y/Z selects the world-space axis.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Move: hold Attack and drag the axis, or use the mouse wheel.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Rotate: mouse wheel in 5 degree steps; Shift uses 15 degrees.")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal(
                "Shift while moving snaps to the 1/16-block authoring grid.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
