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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * One public item for every legacy left/right keycard-reader block variant.
 * The clicked wall face, not player yaw, defines both orientation and which
 * lateral half was selected.
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
        if (context.getPlayer() == null
                || context.getClickedFace().getAxis() == Direction.Axis.Y) {
            return InteractionResult.FAIL;
        }

        BlockPlaceContext originalPlacement = new BlockPlaceContext(context);
        BlockPos visualPosition = originalPlacement.getClickedPos();

        Direction readerFacing = context.getClickedFace();
        Direction screenLeft = readerFacing.getClockWise();

        Vec3 clickedBlockCenter = Vec3.atCenterOf(context.getClickedPos());
        Vec3 offsetFromCenter = context.getClickLocation().subtract(clickedBlockCenter);
        double leftCoordinate = offsetFromCenter.x * screenLeft.getStepX()
                + offsetFromCenter.z * screenLeft.getStepZ();
        boolean clickedLeftHalf = leftCoordinate >= 0.0D;

        // Preserve the existing model-to-logical-anchor mapping; only the basis
        // used to evaluate left/right has changed from player yaw to wall face.
        Direction logicalShift = clickedLeftHalf
                ? screenLeft : screenLeft.getOpposite();
        BlockPos logicalPosition = visualPosition.relative(logicalShift);

        BlockHitResult shiftedHit = new BlockHitResult(
                Vec3.atCenterOf(logicalPosition),
                readerFacing,
                logicalPosition,
                false);
        BlockPlaceContext shiftedPlacement = new BlockPlaceContext(
                context.getLevel(),
                context.getPlayer(),
                context.getHand(),
                context.getItemInHand(),
                shiftedHit);

        if (!shiftedPlacement.getClickedPos().equals(logicalPosition)) {
            return InteractionResult.FAIL;
        }

        InteractionResult result = clickedLeftHalf
                ? rightPlacementItem.get().place(shiftedPlacement)
                : super.place(shiftedPlacement);

        // The legacy block classes still derive FACING from player yaw. Correct
        // the placed state immediately so diagonal placement cannot rotate it.
        if (!context.getLevel().isClientSide && result.consumesAction()) {
            BlockState placed = context.getLevel().getBlockState(logicalPosition);
            if (placed.hasProperty(HorizontalDirectionalBlock.FACING)
                    && placed.getValue(HorizontalDirectionalBlock.FACING) != readerFacing) {
                context.getLevel().setBlock(logicalPosition,
                        placed.setValue(HorizontalDirectionalBlock.FACING, readerFacing),
                        Block.UPDATE_ALL);
            }
        }

        return result;
    }

    @Override
    public String getDescriptionId() {
        return "item.scp_additions.keycard_reader";
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(
                "tooltip.scp_additions.keycard_reader_placement")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.scp_additions.keycard_reader_configure")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void registerBlocks(Map<Block, Item> blockToItemMap, Item item) {
        super.registerBlocks(blockToItemMap, item);
        blockToItemMap.put(rightPlacementBlock.get(), item);
        for (Supplier<? extends Block> readerBlock : canonicalReaderBlocks) {
            blockToItemMap.put(readerBlock.get(), item);
        }
    }
}
