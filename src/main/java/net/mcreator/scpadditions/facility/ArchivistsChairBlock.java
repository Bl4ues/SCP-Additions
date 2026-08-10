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
 * The Blockbench model is intentionally offset from the block origin. The
 * collision below is a baked approximation of the actual rotated seat,
 * backrest, arm pieces, pedestal, five spokes and feet rather than a centered
 * generic chair box.
 */
public final class ArchivistsChairBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final VoxelShape NORTH = Shapes.or(
            box(3.372D, 10.050D, -6.570D, 8.628D, 11.420D, -3.942D),
            box(0.744D, 10.050D, -3.942D, 11.256D, 11.420D, -1.314D),
            box(-0.570D, 10.050D, -1.314D, 12.570D, 11.420D, 1.314D),
            box(0.744D, 10.050D, 1.314D, 11.256D, 11.420D, 3.942D),
            box(3.372D, 10.050D, 3.942D, 8.628D, 11.420D, 6.570D),
            box(-0.835D, 10.298D, -2.550D, 2.722D, 13.026D, 1.007D),
            box(-1.137D, 12.718D, -2.852D, 2.420D, 15.446D, 0.705D),
            box(-1.439D, 15.139D, -3.154D, 2.118D, 17.867D, 0.403D),
            box(-1.740D, 17.560D, -3.455D, 1.816D, 20.288D, 0.101D),
            box(-2.042D, 19.980D, -3.757D, 1.515D, 22.708D, -0.200D),
            box(1.308D, 10.298D, -4.692D, 4.864D, 13.026D, -1.136D),
            box(1.006D, 12.718D, -4.994D, 4.562D, 15.446D, -1.438D),
            box(0.704D, 15.139D, -5.296D, 4.261D, 17.867D, -1.739D),
            box(0.402D, 17.560D, -5.598D, 3.959D, 20.288D, -2.041D),
            box(0.100D, 19.980D, -5.900D, 3.657D, 22.708D, -2.343D),
            box(3.450D, 10.298D, -6.835D, 7.007D, 13.026D, -3.278D),
            box(3.148D, 12.718D, -7.137D, 6.705D, 15.446D, -3.580D),
            box(2.846D, 15.139D, -7.439D, 6.403D, 17.867D, -3.882D),
            box(2.545D, 17.560D, -7.740D, 6.101D, 20.288D, -4.184D),
            box(2.243D, 19.980D, -8.042D, 5.800D, 22.708D, -4.485D),
            box(5.358D, 10.260D, -7.218D, 7.875D, 14.420D, -4.701D),
            box(7.715D, 10.260D, -4.861D, 10.232D, 14.420D, -2.344D),
            box(10.072D, 10.260D, -2.504D, 12.589D, 14.420D, 0.013D),
            box(-1.218D, 10.260D, -0.642D, 1.299D, 14.420D, 1.875D),
            box(1.139D, 10.260D, 1.715D, 3.656D, 14.420D, 4.232D),
            box(3.496D, 10.260D, 4.072D, 6.013D, 14.420D, 6.589D),
            box(3.725D, 1.770D, -2.275D, 8.275D, 3.620D, 2.275D),
            box(4.847D, 3.560D, -1.153D, 7.153D, 6.910D, 1.153D),
            box(5.161D, 6.810D, -0.839D, 6.839D, 10.160D, 0.839D),
            box(7.358D, 1.509D, -0.439D, 9.266D, 2.780D, 0.844D),
            box(8.927D, 1.175D, -0.302D, 10.836D, 2.445D, 0.981D),
            box(10.497D, 0.840D, -0.165D, 12.406D, 2.110D, 1.119D),
            box(10.990D, 0.000D, -0.299D, 12.601D, 1.383D, 1.313D),
            box(3.039D, 1.509D, -2.306D, 5.159D, 2.780D, -0.356D),
            box(1.748D, 1.175D, -3.210D, 3.868D, 2.445D, -1.260D),
            box(0.458D, 0.840D, -4.114D, 2.577D, 2.110D, -2.164D),
            box(0.207D, 0.000D, -4.364D, 2.262D, 1.383D, -2.310D),
            box(3.172D, 1.509D, 0.484D, 5.272D, 2.780D, 2.499D),
            box(1.965D, 1.175D, 1.497D, 4.065D, 2.445D, 3.512D),
            box(0.758D, 0.840D, 2.510D, 2.858D, 2.110D, 4.525D),
            box(0.505D, 0.000D, 2.701D, 2.582D, 1.383D, 4.778D),
            box(5.958D, 1.509D, -3.222D, 7.630D, 2.780D, -1.139D),
            box(6.497D, 1.175D, -4.703D, 8.169D, 2.445D, -2.620D),
            box(7.036D, 0.840D, -6.184D, 8.708D, 2.110D, -4.101D),
            box(7.042D, 0.000D, -6.414D, 8.937D, 1.383D, -4.519D),
            box(5.891D, 1.509D, 1.182D, 7.505D, 2.780D, 3.245D),
            box(6.365D, 1.175D, 2.685D, 7.979D, 2.445D, 4.748D),
            box(6.839D, 0.840D, 4.187D, 8.453D, 2.110D, 6.251D),
            box(6.821D, 0.000D, 4.620D, 8.678D, 1.383D, 6.476D))
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
        return switch (facing) {
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> NORTH;
        };
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
