package com.bl4ues.scpclassifieddirective.block;

import com.bl4ues.scpclassifieddirective.block.entity.Scp914BlockEntity;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Module;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Structure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Single controller/render block for the rebuilt SCP-914. */
public final class Scp914Block extends BaseEntityBlock implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape CONTROLLER_SHAPE = Shapes.block();
    private static final double WIND_KEY_USE_RADIUS_SQR = 0.22D * 0.22D;

    public Scp914Block() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(6.0F, 1200.0F)
                .sound(SoundType.METAL)
                .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING,
                rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return CONTROLLER_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return CONTROLLER_SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        Vec3 windKey = Scp914Structure.windKeyAnchor(pos,
                state.getValue(FACING));
        if (hit.getLocation().distanceToSqr(windKey)
                > WIND_KEY_USE_RADIUS_SQR) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof Scp914BlockEntity machine)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        return machine.beginRefining()
                ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Scp914Structure.placeHelpers(level, pos, state.getValue(FACING));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            Scp914Structure.clearHelpers(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Scp914BlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
            BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,
                Scp914Module.SCP_914_BLOCK_ENTITY.get(),
                Scp914BlockEntity::serverTick);
    }
}
