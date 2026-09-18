package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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
        if (context.getLevel().getBlockState(context.getClickedPos()).is(
                com.bl4ues.scpclassifieddirective.facility.transform
                        .TransformConstructionModule.getProxy())) {
            // Clicking an existing transformed object with the authoring tool is
            // an edit gesture, never an instruction to stack another empty grid
            // on top of it.
            return InteractionResult.CONSUME;
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.CONSUME;
        }
        // New grids start exactly centered in the adjacent vanilla cell. The
        // user gets a predictable 1x1x1 reference before applying any offset.
        TransformConstructionManager.createGroup(player, context.getClickedPos(),
                context.getClickedFace());
        player.getCooldowns().addCooldown(this, 4);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos,
            Player player) {
        // Attack is an editor/select input while this tool is held. Destructive
        // removal remains an explicit Delete action so selecting a transformed
        // cell can never accidentally erase its payload.
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Off-Grid Construction Tool");
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(
                "Builds rigid local block grids at arbitrary positions and angles.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(
                "Practical controls are shown on-screen while the tool is held.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
