package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Decorative Archivist's Chair.
 *
 * GeckoLib/Blockbench geometry is authored around a horizontal model origin,
 * while Block.box uses the block's north-west corner as zero. The collision
 * therefore uses independent X/Z offsets before it is rotated with the block
 * state. Four practical volumes follow the base, pedestal, seat and backrest
 * without turning every wheel and spoke into its own collision box.
 */
public final class ArchivistsChairBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final double MODEL_ORIGIN_X = 10.0D;
    private static final double MODEL_ORIGIN_Z = 8.0D;

    private static final VoxelShape NORTH = Shapes.or(
            // Five-star base footprint.
            modelBox(0.20D, 0.00D, -6.50D, 12.65D, 3.65D, 6.50D),
            // Central pedestal.
            modelBox(4.80D, 3.45D, -1.20D, 7.20D, 10.25D, 1.20D),
            // Seat and immediate frame.
            modelBox(-0.60D, 9.95D, -6.65D, 12.65D, 12.30D, 6.65D),
            // Broad backrest envelope.
            modelBox(-2.10D, 10.20D, -8.10D, 7.10D, 22.80D, 1.10D))
            .optimize();
    private static final VoxelShape EAST = rotateY(NORTH, 1);
    private static final VoxelShape SOUTH = rotateY(NORTH, 2);
    private static final VoxelShape WEST = rotateY(NORTH, 3);

    public ArchivistsChairBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .strength(1.0F, 10.0F)
                .noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArchivistsChairBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level,
            BlockPos pos, PathComputationType type) {
        return false;
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
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.singletonList(
                new ItemStack(FacilityModule.ARCHIVISTS_CHAIR_ITEM.get()));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target,
            BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(FacilityModule.ARCHIVISTS_CHAIR_ITEM.get());
    }

    private static VoxelShape shapeFor(Direction facing) {
        // GeckoLib renders this authored model with the opposite
        // horizontal forward convention from the vanilla block-facing
        // collision transform. Mirror the facing by 180 degrees so the
        // collision sits on the visible chair instead of across the
        // placement origin.
        return switch (facing) {
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            default -> SOUTH;
        };
    }

    private static VoxelShape modelBox(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        return box(minX + MODEL_ORIGIN_X, minY, minZ + MODEL_ORIGIN_Z,
                maxX + MODEL_ORIGIN_X, maxY, maxZ + MODEL_ORIGIN_Z);
    }

    private static VoxelShape rotateY(VoxelShape source, int quarterTurns) {
        VoxelShape current = source;
        for (int turn = 0; turn < quarterTurns; turn++) {
            VoxelShape previous = current;
            VoxelShape[] rotated = {Shapes.empty()};
            previous.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    rotated[0] = Shapes.or(rotated[0], Shapes.box(
                            1.0D - maxZ, minY, minX,
                            1.0D - minZ, maxY, maxX)));
            current = rotated[0].optimize();
        }
        return current;
    }
}
