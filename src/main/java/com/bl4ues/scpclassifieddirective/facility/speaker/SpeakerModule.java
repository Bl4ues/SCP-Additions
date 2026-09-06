package com.bl4ues.scpclassifieddirective.facility.speaker;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.SpeakerAudioClient;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.List;

/** Wall-mounted voice endpoint shared by SCP-079 and physical Intercoms. */
public final class SpeakerModule {
    public static final String PATH = "speaker";
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(
            ForgeRegistries.SOUND_EVENTS, ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<SpeakerBlock> BLOCK =
            BLOCKS.register(PATH, SpeakerBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register(PATH,
            () -> new SpeakerItem(BLOCK.get()));
    public static final RegistryObject<BlockEntityType<SpeakerBlockEntity>>
            BLOCK_ENTITY = BLOCK_ENTITIES.register(PATH, () ->
                    BlockEntityType.Builder.of(SpeakerBlockEntity::new,
                            BLOCK.get()).build(null));
    public static final RegistryObject<SoundEvent> ON = sound("speaker_on");
    public static final RegistryObject<SoundEvent> OFF = sound("speaker_off");
    public static final RegistryObject<SoundEvent> LOOP = sound("speaker_loop");

    private SpeakerModule() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        SOUNDS.register(bus);
    }

    private static RegistryObject<SoundEvent> sound(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path)));
    }

    public static final class SpeakerBlock extends BaseEntityBlock {
        // Authored model occupies X 4.25..11.75, Y 3..13 and Z 15..16.
        private static final VoxelShape NORTH = Block.box(
                4.25D, 3.0D, 15.0D, 11.75D, 13.0D, 16.0D);

        private SpeakerBlock() {
            super(BlockBehaviour.Properties.of()
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(ACTIVE, false));
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new SpeakerBlockEntity(pos, state);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                BlockState state, BlockEntityType<T> type) {
            return level.isClientSide
                    ? createTickerHelper(type, BLOCK_ENTITY.get(),
                            SpeakerBlockEntity::clientTick)
                    : null;
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, ACTIVE);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction face = context.getClickedFace();
            if (face.getAxis().isVertical()) return null;
            BlockState state = defaultBlockState().setValue(FACING, face);
            return state.canSurvive(context.getLevel(), context.getClickedPos())
                    ? state : null;
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level,
                BlockPos pos) {
            Direction facing = state.getValue(FACING);
            BlockPos support = pos.relative(facing.getOpposite());
            return level.getBlockState(support).isFaceSturdy(level, support,
                    facing);
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighborState, LevelAccessor level, BlockPos pos,
                BlockPos neighborPos) {
            if (direction == state.getValue(FACING).getOpposite()
                    && !state.canSurvive(level, pos)) {
                return Blocks.AIR.defaultBlockState();
            }
            return super.updateShape(state, direction, neighborState, level,
                    pos, neighborPos);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return rotateNorthShape(NORTH, state.getValue(FACING));
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return getShape(state, level, pos, context);
        }

        @Override
        public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
                BlockPos pos) {
            return Shapes.empty();
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
            if (!level.isClientSide && level instanceof ServerLevel server
                    && !oldState.is(this)) {
                FacilitySpeakerRegistry.register(server, pos);
            }
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (!level.isClientSide && level instanceof ServerLevel server
                    && newState.getBlock() != this) {
                FacilitySpeakerRegistry.unregister(server, pos);
                SpeakerBroadcastManager.removeEndpoint(server, pos);
            }
            super.onRemove(state, level, pos, newState, moving);
        }
    }

    public static final class SpeakerBlockEntity extends BlockEntity {
        private SpeakerBlockEntity(BlockPos pos, BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state);
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel server) {
                FacilitySpeakerRegistry.register(server, worldPosition);
                if (getBlockState().getValue(ACTIVE)
                        && !SpeakerBroadcastManager.isEndpointActive(server,
                        worldPosition)) {
                    server.setBlock(worldPosition,
                            getBlockState().setValue(ACTIVE, false),
                            Block.UPDATE_CLIENTS);
                }
            }
        }

        private static void clientTick(Level level, BlockPos pos,
                BlockState state, SpeakerBlockEntity speaker) {
            SpeakerAudioClient.update(level, pos, state.getValue(ACTIVE));
        }
    }

    private static final class SpeakerItem extends BlockItem {
        private SpeakerItem(Block block) {
            super(block, new Item.Properties());
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal(
                            "Broadcasts from the Intercom or Camera Operators")
                    .withStyle(ChatFormatting.GRAY));
            super.appendHoverText(stack, level, tooltip, flag);
        }
    }

    private static VoxelShape rotateNorthShape(VoxelShape source,
            Direction facing) {
        if (facing == Direction.NORTH) return source;
        VoxelShape current = source;
        int turns = switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        for (int turn = 0; turn < turns; turn++) {
            VoxelShape[] rotated = {Shapes.empty()};
            current.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    rotated[0] = Shapes.or(rotated[0], Shapes.box(
                            1.0D - maxZ, minY, minX,
                            1.0D - minZ, maxY, maxX)));
            current = rotated[0].optimize();
        }
        return current;
    }
}
