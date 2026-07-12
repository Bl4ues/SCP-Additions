package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
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
 * Internal mirrored geometry for the Unity door buttons.
 *
 * The public items remain button_closed and button_locked. These registry
 * entries exist only so the same item can place a visually correct left- or
 * right-offset model depending on the half of the wall that was clicked.
 */
public final class MirroredDoorButtons {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpAdditionsMod.MODID);

    public static final RegistryObject<Block> BUTTON_LOCKED = register("button_locked_mirrored", State.LOCKED);
    public static final RegistryObject<Block> BUTTON_CLOSED = register("button_closed_mirrored", State.CLOSED);
    public static final RegistryObject<Block> BUTTON_OPENING = register("button_opening_mirrored", State.OPENING);
    public static final RegistryObject<Block> BUTTON_OPEN = register("button_open_mirrored", State.OPEN);
    public static final RegistryObject<Block> BUTTON_CLOSING = register("button_closing_mirrored", State.CLOSING);

    private MirroredDoorButtons() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private static RegistryObject<Block> register(String path, State state) {
        return BLOCKS.register(path, () -> new MirroredDoorButtonBlock(state));
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

    private static final class MirroredDoorButtonBlock extends HorizontalDirectionalBlock {
        private final State state;

        private MirroredDoorButtonBlock(State state) {
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
            return defaultBlockState().setValue(FACING,
                    context.getHorizontalDirection().getOpposite());
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
                case NORTH -> Block.box(16.9D, -2.66D, 14.2D, 20.2D, 2.64D, 16.0D);
                case EAST -> Block.box(0.0D, -2.66D, 16.9D, 1.8D, 2.64D, 20.2D);
                case SOUTH -> Block.box(-4.2D, -2.66D, 0.0D, -0.9D, 2.64D, 1.8D);
                case WEST -> Block.box(14.2D, -2.66D, -4.2D, 16.0D, 2.64D, -0.9D);
                default -> Shapes.empty();
            };
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
