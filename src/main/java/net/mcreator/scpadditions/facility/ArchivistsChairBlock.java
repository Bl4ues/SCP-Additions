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
    // GeckoLib mirrors Bedrock's model X axis when baking cubes. The chair's
    // authored base is also rotated -17.5 degrees, while its swivelling upper
    // assembly adds another -27.5 degrees around the same horizontal pivot.
    private static final double MODEL_PIVOT_X = 6.0D;
    private static final double MODEL_PIVOT_Z = 0.0D;
    private static final double BASE_YAW = -17.5D;
    private static final double UPPER_YAW = -45.0D;

    private static final VoxelShape NORTH = Shapes.or(
            // Five-star base footprint, following the root bone.
            rotatedModelBox(0.20D, 0.00D, -6.50D,
                    12.65D, 3.65D, 6.50D, BASE_YAW, 7),
            // Central pedestal.
            rotatedModelBox(4.80D, 3.45D, -1.20D,
                    7.20D, 10.25D, 1.20D, BASE_YAW, 2),
            // Seat and immediate frame, following the swivelling upper bone.
            rotatedModelBox(-0.60D, 9.95D, -6.65D,
                    12.65D, 12.30D, 6.65D, UPPER_YAW, 8),
            // Broad backrest envelope.
            rotatedModelBox(-2.10D, 10.20D, -8.10D,
                    7.10D, 22.80D, 1.10D, UPPER_YAW, 6))
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
        // GeoBlockRenderer uses the same quarter-turn convention: EAST is
        // -90 degrees, SOUTH is 180, and WEST is +90 from NORTH.
        return switch (facing) {
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    /**
     * Builds a stepped approximation of an arbitrarily rotated model-space
     * rectangle. VoxelShapes remain axis-aligned, so thin strips keep the
     * collision close to the visible diagonal chair instead of inflating one
     * enormous bounding box around it.
     */
    private static VoxelShape rotatedModelBox(double minX, double minY,
            double minZ, double maxX, double maxY, double maxZ,
            double degrees, int slices) {
        int count = Math.max(1, slices);
        double step = (maxX - minX) / count;
        VoxelShape result = Shapes.empty();
        for (int index = 0; index < count; index++) {
            double sliceMinX = minX + step * index;
            double sliceMaxX = index == count - 1
                    ? maxX : minX + step * (index + 1);
            double[] bounds = rotatedBounds(sliceMinX, minZ,
                    sliceMaxX, maxZ, degrees);
            result = Shapes.or(result, modelBox(bounds[0], minY, bounds[1],
                    bounds[2], maxY, bounds[3]));
        }
        return result.optimize();
    }

    private static double[] rotatedBounds(double minX, double minZ,
            double maxX, double maxZ, double degrees) {
        double radians = Math.toRadians(degrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        double[] xs = {minX, minX, maxX, maxX};
        double[] zs = {minZ, maxZ, minZ, maxZ};
        double outMinX = Double.POSITIVE_INFINITY;
        double outMinZ = Double.POSITIVE_INFINITY;
        double outMaxX = Double.NEGATIVE_INFINITY;
        double outMaxZ = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < 4; index++) {
            double dx = xs[index] - MODEL_PIVOT_X;
            double dz = zs[index] - MODEL_PIVOT_Z;
            double x = MODEL_PIVOT_X + dx * cosine - dz * sine;
            double z = MODEL_PIVOT_Z + dx * sine + dz * cosine;
            outMinX = Math.min(outMinX, x);
            outMinZ = Math.min(outMinZ, z);
            outMaxX = Math.max(outMaxX, x);
            outMaxZ = Math.max(outMaxZ, z);
        }
        return new double[] {outMinX, outMinZ, outMaxX, outMaxZ};
    }

    private static VoxelShape modelBox(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        // GeckoLib's BakedModelFactory maps Bedrock X as -(origin + size)
        // and GeoBlockRenderer then translates the model to the block centre.
        // In Block.box's 0..16 coordinate space this is exactly X = 8 - modelX,
        // while Z remains Z = 8 + modelZ.
        return box(8.0D - maxX, minY, 8.0D + minZ,
                8.0D - minX, maxY, 8.0D + maxZ);
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
