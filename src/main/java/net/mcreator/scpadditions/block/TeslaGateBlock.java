package net.mcreator.scpadditions.block;

import net.minecraft.world.item.Item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.mcreator.scpadditions.procedures.TeslaGateUpdateTickProcedure;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;

import java.util.Collections;
import java.util.List;

public class TeslaGateBlock extends Block implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public TeslaGateBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .strength(30.0F, 100.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK)
                .hasPostProcess((state, level, pos) -> true)
                .emissiveRendering((state, level, pos) -> true)
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return TeslaGateShapeHelper.shape(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos placementPos = context.getClickedPos();
        BlockPos controllerPos = placementPos.above();
        Direction facing = context.getHorizontalDirection().getOpposite();

        if (!context.getLevel().getBlockState(placementPos.below())
                .isFaceSturdy(context.getLevel(), placementPos.below(), Direction.UP)
                || !TeslaGateStructure.canPlace(context.getLevel(), controllerPos, facing)) {
            return null;
        }

        boolean waterlogged = context.getLevel().getFluidState(placementPos).getType() == Fluids.WATER;
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(WATERLOGGED, waterlogged);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return player.getMainHandItem().isCorrectToolForDrops(state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> original = super.getDrops(state, builder);
        return original.isEmpty()
                ? Collections.singletonList(new ItemStack(this))
                : original;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
            boolean moving) {
        if (tryRaiseOnPlacement(state, level, pos, oldState, moving)) {
            return;
        }

        super.onPlace(state, level, pos, oldState, moving);
        level.scheduleTick(pos, this, 10);
        TeslaGateUpdateTickProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
    }

    private boolean tryRaiseOnPlacement(BlockState state, Level level, BlockPos pos,
            BlockState oldState, boolean moving) {
        if (moving || oldState.getBlock() == this || level.isClientSide()) {
            return false;
        }

        // The second onPlace call is the raised controller being installed above
        // the temporary placement block. It must initialize normally, not rise again.
        if (level.getBlockState(pos.below()).getBlock() == this) {
            return false;
        }

        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return false;
        }

        Direction facing = state.getValue(FACING);
        BlockPos raisedPos = pos.above();
        if (!TeslaGateStructure.canPlace(level, raisedPos, facing)) {
            return false;
        }

        level.setBlock(raisedPos, state, Block.UPDATE_ALL);
        level.setBlock(pos,
                state.getValue(WATERLOGGED)
                        ? Blocks.WATER.defaultBlockState()
                        : Blocks.AIR.defaultBlockState(),
                Block.UPDATE_ALL);

        if (!TeslaGateStructure.placeCollisionParts(level, raisedPos, facing)) {
            level.destroyBlock(raisedPos, true);
        }
        return true;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        TeslaGateUpdateTickProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
        if (level.getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEON)
                || level.getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE)) {
            level.scheduleTick(pos, this, 10);
        }
    }
}
