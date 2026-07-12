package net.mcreator.scpadditions.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * One public item for a legacy left/right keycard-reader block pair.
 *
 * The reader models intentionally live one block to the side of their logical
 * redstone block. This item moves that logical block to the opposite side of
 * the clicked visual position, so the model appears exactly where the player
 * clicked while the old redstone architecture and registry IDs remain intact.
 */
public final class OffsetKeycardReaderItem extends BlockItem {
    private final Supplier<? extends Block> rightBlock;
    private final Supplier<? extends BlockItem> rightItem;

    public OffsetKeycardReaderItem(Block leftBlock, Supplier<? extends Block> rightBlock,
            Supplier<? extends BlockItem> rightItem, Item.Properties properties) {
        super(leftBlock, properties);
        this.rightBlock = rightBlock;
        this.rightItem = rightItem;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null || context.getClickedFace().getAxis() == Direction.Axis.Y) {
            return InteractionResult.FAIL;
        }

        BlockPlaceContext originalPlacement = new BlockPlaceContext(context);
        BlockPos visualPosition = originalPlacement.getClickedPos();

        // Matches the FACING value used by the existing reader block classes.
        Direction readerFacing = context.getPlayer().getDirection().getOpposite();
        Direction screenLeft = readerFacing.getClockWise();

        Vec3 clickedBlockCenter = Vec3.atCenterOf(context.getClickedPos());
        Vec3 offsetFromCenter = context.getClickLocation().subtract(clickedBlockCenter);
        double leftCoordinate = offsetFromCenter.x * screenLeft.getStepX()
                + offsetFromCenter.z * screenLeft.getStepZ();

        boolean clickedLeftHalf = leftCoordinate >= 0.0D;

        // Right model is offset right from its logical block, so its logical
        // block goes left. The left model follows the inverse rule.
        Direction logicalShift = clickedLeftHalf ? screenLeft : screenLeft.getOpposite();
        BlockPos logicalPosition = visualPosition.relative(logicalShift);

        BlockHitResult shiftedHit = new BlockHitResult(
                Vec3.atCenterOf(logicalPosition),
                context.getClickedFace(),
                logicalPosition,
                false
        );
        BlockPlaceContext shiftedPlacement = new BlockPlaceContext(
                context.getLevel(),
                context.getPlayer(),
                context.getHand(),
                context.getItemInHand(),
                shiftedHit
        );

        // Never let BlockPlaceContext silently move the logical block one more
        // block forward when the intended anchor is occupied.
        if (!shiftedPlacement.getClickedPos().equals(logicalPosition)) {
            return InteractionResult.FAIL;
        }

        if (clickedLeftHalf) {
            return rightItem.get().place(shiftedPlacement);
        }
        return super.place(shiftedPlacement);
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.scp_additions.keycard_reader_placement"));
    }

    /**
     * Make Block#asItem resolve both legacy block variants to the one public
     * item. This also unifies pick-block and every existing normal/accept/wrong
     * reader drop without rewriting all 36 legacy block classes.
     */
    @Override
    public void registerBlocks(Map<Block, Item> blockToItemMap, Item item) {
        super.registerBlocks(blockToItemMap, item);
        blockToItemMap.put(rightBlock.get(), item);
    }
}
