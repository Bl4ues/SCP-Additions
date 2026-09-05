package com.bl4ues.scpclassifieddirective.facility.surveillance;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Functional placeholder for the future animated GeckoLib surveillance camera.
 * Keep the public registry ID stable when replacing the temporary block/model.
 */
public final class SurveillanceCameraPlaceholderModule {
    public static final String PATH = "surveillance_camera";
    /** Full manual range: 180 degrees horizontal, centered on the mount. */
    public static final float MANUAL_YAW_LIMIT = 90.0F;
    /** Full manual range: 90 degrees vertical. */
    public static final float MANUAL_MIN_PITCH = -45.0F;
    public static final float MANUAL_MAX_PITCH = 45.0F;
    /** Target half-range for the future idle sweep when nobody controls it. */
    public static final float IDLE_SWEEP_YAW_LIMIT = 50.0F;

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<SurveillanceCameraBlock> BLOCK =
            BLOCKS.register(PATH, SurveillanceCameraBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register(PATH,
            () -> new SurveillanceCameraItem(BLOCK.get(), new Item.Properties()));

    private SurveillanceCameraPlaceholderModule() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public static final class SurveillanceCameraBlock
            extends HorizontalDirectionalBlock {
        private static final VoxelShape NORTH = Shapes.or(
                Block.box(4.0D, 4.0D, 13.0D, 12.0D, 12.0D, 16.0D),
                Block.box(3.0D, 5.0D, 5.0D, 13.0D, 11.0D, 14.0D),
                Block.box(5.0D, 6.0D, 1.0D, 11.0D, 10.0D, 5.0D));
        private static final VoxelShape SOUTH = Shapes.or(
                Block.box(4.0D, 4.0D, 0.0D, 12.0D, 12.0D, 3.0D),
                Block.box(3.0D, 5.0D, 2.0D, 13.0D, 11.0D, 11.0D),
                Block.box(5.0D, 6.0D, 11.0D, 11.0D, 10.0D, 15.0D));
        private static final VoxelShape EAST = Shapes.or(
                Block.box(0.0D, 4.0D, 4.0D, 3.0D, 12.0D, 12.0D),
                Block.box(2.0D, 5.0D, 3.0D, 11.0D, 11.0D, 13.0D),
                Block.box(11.0D, 6.0D, 5.0D, 15.0D, 10.0D, 11.0D));
        private static final VoxelShape WEST = Shapes.or(
                Block.box(13.0D, 4.0D, 4.0D, 16.0D, 12.0D, 12.0D),
                Block.box(5.0D, 5.0D, 3.0D, 14.0D, 11.0D, 13.0D),
                Block.box(1.0D, 6.0D, 5.0D, 5.0D, 10.0D, 11.0D));

        private SurveillanceCameraBlock() {
            super(BlockBehaviour.Properties.of()
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());
            registerDefaultState(stateDefinition.any().setValue(FACING,
                    Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING,
                    context.getHorizontalDirection().getOpposite());
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return switch (state.getValue(FACING)) {
                case SOUTH -> SOUTH;
                case EAST -> EAST;
                case WEST -> WEST;
                default -> NORTH;
            };
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
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (!level.isClientSide && level instanceof ServerLevel server) {
                registerCamera(server, pos, state);
            }
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (!level.isClientSide && level instanceof ServerLevel server
                    && newState.getBlock() != this) {
                FacilitySurveillanceRegistry.unregister(server,
                        cameraId(server, pos));
            }
            super.onRemove(state, level, pos, newState, moving);
        }

        private static void registerCamera(ServerLevel level, BlockPos pos,
                BlockState state) {
            Direction facing = state.getValue(FACING);
            Vec3 eye = Vec3.atCenterOf(pos).add(
                    facing.getStepX() * 0.58D,
                    0.20D,
                    facing.getStepZ() * 0.58D);
            String name = "Camera " + pos.getX() + ", " + pos.getY()
                    + ", " + pos.getZ();
            FacilitySurveillanceRegistry.register(level, cameraId(level, pos),
                    pos, eye, name, facing.toYRot(), 0.0F,
                    MANUAL_YAW_LIMIT, MANUAL_MIN_PITCH,
                    MANUAL_MAX_PITCH, 2.5F);
        }

        private static UUID cameraId(ServerLevel level, BlockPos pos) {
            String key = ScpClassifiedDirectiveMod.MODID + ":camera:"
                    + level.dimension().location() + ":" + pos.asLong();
            return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static final class SurveillanceCameraItem extends BlockItem {
        private SurveillanceCameraItem(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.literal("Surveillance Camera [Placeholder]");
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal(
                    "Functional Equipment - Surveillance Network")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(
                    "Temporary model; registers automatically to its mapped room.")
                    .withStyle(ChatFormatting.DARK_GRAY));
            super.appendHoverText(stack, level, tooltip, flag);
        }
    }
}
