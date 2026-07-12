package net.mcreator.scpadditions.item;

import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * One public item for every legacy left/right keycard-reader block variant.
 *
 * Readers still use the old offset logical block because it carries the
 * redstone implementation. The item shifts that logical block so the visible
 * reader appears exactly on the half of the wall the player clicked.
 */
public final class OffsetKeycardReaderItem extends BlockItem {
    private final Supplier<? extends Block> rightPlacementBlock;
    private final Supplier<? extends BlockItem> rightPlacementItem;
    private final List<Supplier<? extends Block>> canonicalReaderBlocks;

    public OffsetKeycardReaderItem(Block leftPlacementBlock,
            Supplier<? extends Block> rightPlacementBlock,
            Supplier<? extends BlockItem> rightPlacementItem,
            List<Supplier<? extends Block>> canonicalReaderBlocks,
            Item.Properties properties) {
        super(leftPlacementBlock, properties);
        this.rightPlacementBlock = rightPlacementBlock;
        this.rightPlacementItem = rightPlacementItem;
        this.canonicalReaderBlocks = List.copyOf(canonicalReaderBlocks);
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
            return rightPlacementItem.get().place(shiftedPlacement);
        }
        return super.place(shiftedPlacement);
    }

    @Override
    public String getDescriptionId() {
        return "item.scp_additions.keycard_reader";
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.scp_additions.keycard_reader_placement").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scp_additions.keycard_reader_configure").withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * Make Block#asItem resolve every normal reader level and side to the one
     * public item. Accept/wrong states already drop their corresponding normal
     * block, so they inherit this mapping too.
     */
    @Override
    public void registerBlocks(Map<Block, Item> blockToItemMap, Item item) {
        super.registerBlocks(blockToItemMap, item);
        blockToItemMap.put(rightPlacementBlock.get(), item);
        for (Supplier<? extends Block> readerBlock : canonicalReaderBlocks) {
            blockToItemMap.put(readerBlock.get(), item);
        }
    }

    @Override
    public void removeFromBlockToItemMap(Map<Block, Item> blockToItemMap, Item item) {
        super.removeFromBlockToItemMap(blockToItemMap, item);
        if (blockToItemMap.get(rightPlacementBlock.get()) == item) {
            blockToItemMap.remove(rightPlacementBlock.get());
        }
        for (Supplier<? extends Block> readerBlock : canonicalReaderBlocks) {
            if (blockToItemMap.get(readerBlock.get()) == item) {
                blockToItemMap.remove(readerBlock.get());
            }
        }
    }
}
