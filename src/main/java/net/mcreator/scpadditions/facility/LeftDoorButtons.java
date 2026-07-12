package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Collections;
import java.util.List;

/**
 * Authored left-side geometry for the Unity door panels.
 *
 * The public items remain button_closed and button_locked. These blocks are
 * internal state variants which use the Blockbench model supplied for the left
 * side instead of mathematically reflecting the right-side model.
 */
public final class LeftDoorButtons {
    private static final int TRANSITION_TICKS = 21;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpAdditionsMod.MODID);

    public static final RegistryObject<Block> BUTTON_LOCKED = register("button_locked_left", State.LOCKED);
    public static final RegistryObject<Block> BUTTON_CLOSED = register("button_closed_left", State.CLOSED);
    public static final RegistryObject<Block> BUTTON_OPENING = register("button_opening_left", State.OPENING);
    public static final RegistryObject<Block> BUTTON_OPEN = register("button_open_left", State.OPEN);
    public static final RegistryObject<Block> BUTTON_CLOSING = register("button_closing_left", State.CLOSING);

    private LeftDoorButtons() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private static RegistryObject<Block> register(String path, State state) {
        return BLOCKS.register(path, () -> new LeftDoorButtonBlock(state));
    }

    public static boolean isAny(Block block) {
        return stateOf(block) != null;
    }

    public static boolean isFunctional(Block block) {
        State state = stateOf(block);
        return state != null && state != State.LOCKED;
    }

    public static boolean isLocked(Block block) {
        return stateOf(block) == State.LOCKED;
    }

    public static State stateOf(Block block) {
        if (block == BUTTON_LOCKED.get()) return State.LOCKED;
        if (block == BUTTON_CLOSED.get()) return State.CLOSED;
        if (block == BUTTON_OPENING.get()) return State.OPENING;
        if (block == BUTTON_OPEN.get()) return State.OPEN;
        if (block == BUTTON_CLOSING.get()) return State.CLOSING;
        return null;
    }

    public static Block blockFor(State state) {
        return switch (state) {
            case LOCKED -> BUTTON_LOCKED.get();
            case CLOSED -> BUTTON_CLOSED.get();
            case OPENING -> BUTTON_OPENING.get();
            case OPEN -> BUTTON_OPEN.get();
            case CLOSING -> BUTTON_CLOSING.get();
        };
    }

    public enum State {
        LOCKED,
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }

    private static final class LeftDoorButtonBlock extends HorizontalDirectionalBlock {
        private final State state;

        private LeftDoorButtonBlock(State state) {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F)
                    .noCollission()
                    .noOcclusion()
                    .isRedstoneConductor((blockState, level, pos) -> false));
            this.state = state;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction face = context.getClickedFace();
            Direction facing = face.getAxis().isHorizontal()
                    ? face : context.getHorizontalDirection().getOpposite();
            return defaultBlockState().setValue(FACING, facing);
        }

        @Override
        public BlockState rotate(BlockState blockState, Rotation rotation) {
            return blockState.setValue(FACING, rotation.rotate(blockState.getValue(FACING)));
        }

        @Override
        public BlockState mirror(BlockState blockState, Mirror mirror) {
            return blockState.rotate(mirror.getRotation(blockState.getValue(FACING)));
        }

        @Override
        public VoxelShape getShape(BlockState blockState, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return switch (blockState.getValue(FACING)) {
                case NORTH -> Block.box(14.80D, -0.70D, -1.80D, 20.20D, 5.00D, 0.10D);
                case EAST -> Block.box(15.90D, -0.70D, 14.80D, 17.80D, 5.00D, 20.20D);
                case SOUTH -> Block.box(-4.20D, -0.70D, 15.90D, 1.20D, 5.00D, 17.80D);
                case WEST -> Block.box(-1.80D, -0.70D, -4.20D, 0.10D, 5.00D, 1.20D);
                default -> Shapes.empty();
            };
        }

        @Override
        public InteractionResult use(BlockState blockState, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (state != State.CLOSED && state != State.OPEN) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide && level instanceof ServerLevel server) {
                DoorButtonIndependentInteractionEvents.activateButton(server, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public void onPlace(BlockState blockState, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(blockState, level, pos, oldState, moving);
            if (!level.isClientSide
                    && (state == State.OPENING || state == State.CLOSING)) {
                level.scheduleTick(pos, this, TRANSITION_TICKS);
            }
        }

        @Override
        public void tick(BlockState blockState, ServerLevel level,
                BlockPos pos, RandomSource random) {
            State endpoint = state == State.OPENING ? State.OPEN
                    : state == State.CLOSING ? State.CLOSED : null;
            if (endpoint == null || level.getBlockState(pos).getBlock() != this) {
                return;
            }
            level.setBlock(pos, blockFor(endpoint).defaultBlockState()
                    .setValue(FACING, blockState.getValue(FACING)), Block.UPDATE_ALL);
        }

        @Override
        public boolean isSignalSource(BlockState blockState) {
            return state == State.OPENING || state == State.OPEN;
        }

        @Override
        public int getSignal(BlockState blockState, BlockGetter level,
                BlockPos pos, Direction direction) {
            return isSignalSource(blockState) ? 15 : 0;
        }

        @Override
        public int getDirectSignal(BlockState blockState, BlockGetter level,
                BlockPos pos, Direction direction) {
            return getSignal(blockState, level, pos, direction);
        }

        @Override
        public List<ItemStack> getDrops(BlockState blockState, LootParams.Builder builder) {
            Block drop = state == State.LOCKED
                    ? FacilityModule.BUTTON_LOCKED.get()
                    : FacilityModule.BUTTON_CLOSED.get();
            return Collections.singletonList(new ItemStack(drop));
        }

        @Override
        public ItemStack getCloneItemStack(BlockState blockState, HitResult target,
                BlockGetter level, BlockPos pos, Player player) {
            return new ItemStack(state == State.LOCKED
                    ? FacilityModule.BUTTON_LOCKED.get()
                    : FacilityModule.BUTTON_CLOSED.get());
        }
    }
}
