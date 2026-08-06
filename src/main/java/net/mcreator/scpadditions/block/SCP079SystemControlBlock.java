package net.mcreator.scpadditions.block;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.mcreator.scpadditions.block.entity.SystemTerminalBlockEntity;
import net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** ARC-Site-48 SCiPNET facility diagnostic terminal. */
public class SCP079SystemControlBlock extends BaseEntityBlock
        implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED =
            BlockStateProperties.WATERLOGGED;
    private static final Map<Long, Long> TERMINAL_LOOP_NEXT_TICK =
            new HashMap<>();

    public SCP079SystemControlBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                .strength(30.0F, 100.0F).noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SystemTerminalBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos,
            RandomSource random) {
        super.animateTick(state, level, pos, random);
        long key = pos.asLong();
        long gameTime = level.getGameTime();
        long nextLoop = TERMINAL_LOOP_NEXT_TICK.getOrDefault(key, 0L);
        if (gameTime < nextLoop) return;

        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(
                new ResourceLocation("scp_additions", "terminalloop"));
        if (sound != null) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D, sound, SoundSource.BLOCKS,
                    0.4F, 1.0F, false);
        }
        TERMINAL_LOOP_NEXT_TICK.put(key, gameTime + 160L);
        if (TERMINAL_LOOP_NEXT_TICK.size() > 512) {
            TERMINAL_LOOP_NEXT_TICK.clear();
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level instanceof ServerLevel
                && player instanceof ServerPlayer serverPlayer) {
            ScpEntityNetwork.openFacilityDiagnostics(serverPlayer,
                    Scp079FacilityAccessManager.performDiagnosticScan(
                            serverPlayer), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // The visual assembly extends beyond its host block, but a single block
    // cannot provide a sensible multi-block collision without helper blocks.
    // Keep the selectable/collidable area tightly inside the host block and
    // approximate only the central monitor and keyboard/base assembly.
    private static final VoxelShape MODEL_SHAPE_NORTH = Shapes.or(
            Block.box(5, 5, 8, 16, 15, 15),
            Block.box(3, 1, 3, 16, 6, 11));
    private static final VoxelShape MODEL_SHAPE_EAST = Shapes.or(
            Block.box(1, 5, 5, 8, 15, 16),
            Block.box(5, 1, 3, 13, 6, 16));
    private static final VoxelShape MODEL_SHAPE_SOUTH = Shapes.or(
            Block.box(0, 5, 1, 11, 15, 8),
            Block.box(0, 1, 5, 13, 6, 13));
    private static final VoxelShape MODEL_SHAPE_WEST = Shapes.or(
            Block.box(8, 5, 0, 15, 15, 11),
            Block.box(3, 1, 0, 11, 6, 13));

    private static VoxelShape modelShape(Direction facing) {
        return switch (facing) {
            case EAST -> MODEL_SHAPE_EAST;
            case SOUTH -> MODEL_SHAPE_SOUTH;
            case WEST -> MODEL_SHAPE_WEST;
            default -> MODEL_SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return modelShape(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return modelShape(state.getValue(FACING));
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("SCiPNET diagnostic terminal."));
        tooltip.add(Component.literal("Telemetry and remote cache control."));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,
            BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING,
                        context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED,
                        context.getLevel().getFluidState(
                                context.getClickedPos()).getType()
                                == Fluids.WATER);
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
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction,
            BlockState neighbor, LevelAccessor level, BlockPos pos,
            BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER,
                    Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbor, level, pos,
                neighborPos);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state,
            BlockGetter level, BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level,
            BlockPos pos) {
        return 0;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
            BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state,
            LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(this));
    }
}
