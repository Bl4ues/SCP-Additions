package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.block.Scp714PlacedBlock;
import com.bl4ues.scpclassifieddirective.facility.ObjectContainmentUnitModule;
import com.bl4ues.scpclassifieddirective.init.Scp714Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

/** Public SCP-714 jade ring item. Its equipped effects are handled centrally. */
public final class Scp714Item extends Item {
    private static final Component SUBTITLE = Component.literal("The Jaded Ring");

    public Scp714Item() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(SUBTITLE);
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        boolean containmentPlacement = context.getClickedFace() == Direction.UP
                && level.getBlockState(clickedPos).is(
                        ObjectContainmentUnitModule.UNIT.get())
                && level.getBlockEntity(clickedPos)
                        instanceof ObjectContainmentUnitModule.UnitBlockEntity unit
                && unit.isOpenForAccess();
        if (!player.isShiftKeyDown() && !containmentPlacement) {
            return InteractionResult.PASS;
        }

        BlockPos placePos = clickedPos.relative(context.getClickedFace());
        if (!level.getBlockState(placePos).isAir()) {
            return InteractionResult.FAIL;
        }

        BlockState state = Scp714Items.SCP_714_PLACED.get().defaultBlockState()
                .setValue(Scp714PlacedBlock.FACING,
                        player.getDirection().getOpposite());
        if (!state.canSurvive(level, placePos)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!level.setBlock(placePos, state, 11)) {
            return InteractionResult.FAIL;
        }

        if (!player.isCreative()) {
            context.getItemInHand().shrink(1);
        }

        SoundType soundType = state.getSoundType(level, placePos, player);
        level.playSound(null, placePos, soundType.getPlaceSound(),
                SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F,
                soundType.getPitch() * 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_PLACE, placePos);
        return InteractionResult.CONSUME;
    }
}
