package com.bl4ues.scpclassifieddirective.facility.alarm;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.DecontaminationStructure;
import com.bl4ues.scpclassifieddirective.client.AlarmAudioClient;
import com.bl4ues.scpclassifieddirective.client.AlarmClient;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorModule;
import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorStructure;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * Wall-mounted rotating facility alarm. Redstone is immediate; nearby electric
 * doors are resolved through the facility door registry rather than scanning
 * thousands of world blocks every tick.
 */
public final class AlarmModule {
    public static final String PATH = "alarm";
    public static final DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE =
            BooleanProperty.create("active");
    public static final int ACTIVE_LIGHT_LEVEL = 7;
    /**
     * The Alarm model spans Y 6.25..9.75 in its block. When attached to the
     * top row of a Blast Door, raise it just enough for the model top to meet
     * the top edge of its placement block instead of intersecting the frame.
     */
    public static final double BLAST_DOOR_TOP_MOUNT_Y_OFFSET =
            6.25D / 16.0D;

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS,
                    ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS,
                    ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<AlarmBlock> BLOCK =
            BLOCKS.register(PATH, AlarmBlock::new);
    public static final RegistryObject<Item> ITEM =
            ITEMS.register(PATH, () -> new AlarmItem(BLOCK.get()));
    public static final RegistryObject<BlockEntityType<AlarmBlockEntity>>
            BLOCK_ENTITY = BLOCK_ENTITIES.register(PATH,
                    () -> BlockEntityType.Builder.of(
                            AlarmBlockEntity::new, BLOCK.get()).build(null));
    public static final RegistryObject<SoundEvent> LOOP =
            SOUNDS.register(PATH, () -> SoundEvent.createFixedRangeEvent(
                    new ResourceLocation(ScpClassifiedDirectiveMod.MODID, PATH),
                    12.0F));

    private AlarmModule() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        SOUNDS.register(bus);
    }

    public static final class AlarmBlock extends BaseEntityBlock {
        // Authored NORTH model: X -1.75..1.75, Y 6.25..9.75 and
        // Z 4.75..8.0. Convert the compact body to a simple one-piece shape.
        private static final VoxelShape NORTH_SHAPE = Block.box(
                6.15D, 6.15D, 12.65D,
                9.85D, 9.85D, 16.0D);

        private AlarmBlock() {
            super(BlockBehaviour.Properties.of()
                    .strength(2.0F, 8.0F)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(ACTIVE)
                            ? ACTIVE_LIGHT_LEVEL : 0)
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(ACTIVE, false));
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new AlarmBlockEntity(pos, state);
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide
                    ? createTickerHelper(type, BLOCK_ENTITY.get(),
                            AlarmBlockEntity::clientTick)
                    : createTickerHelper(type, BLOCK_ENTITY.get(),
                            AlarmBlockEntity::serverTick);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            /*
             * Keep a tiny baked-model proxy in the chunk mesh while the actual
             * Alarm remains a GeckoLib block entity. Shader packs such as BSL
             * classify colored block light from terrain/block.properties data;
             * ENTITYBLOCK_ANIMATED supplied no terrain vertices, so
             * alarm:active=true could never reach that pipeline.
             */
            return RenderShape.MODEL;
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, ACTIVE);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction clicked = context.getClickedFace();
            if (clicked.getAxis() == Direction.Axis.Y) return null;
            BlockState state = defaultBlockState()
                    .setValue(FACING, clicked)
                    .setValue(ACTIVE, false);
            return state.canSurvive(context.getLevel(),
                    context.getClickedPos()) ? state : null;
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level,
                BlockPos pos) {
            Direction facing = state.getValue(FACING);
            BlockPos support = pos.relative(facing.getOpposite());
            BlockState supportState = level.getBlockState(support);
            return supportState.isFaceSturdy(level, support, facing)
                    || isBlastDoorTopSupport(level, support, supportState);
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighborState, LevelAccessor level, BlockPos pos,
                BlockPos neighborPos) {
            if (!state.canSurvive(level, pos)) {
                return Blocks.AIR.defaultBlockState();
            }
            return super.updateShape(state, direction, neighborState,
                    level, pos, neighborPos);
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (!level.isClientSide && level instanceof ServerLevel server
                    && level.getBlockEntity(pos)
                    instanceof AlarmBlockEntity alarm) {
                alarm.refresh(server, true);
            }
        }

        @Override
        public void neighborChanged(BlockState state, Level level,
                BlockPos pos, Block neighborBlock, BlockPos neighborPos,
                boolean moving) {
            super.neighborChanged(state, level, pos, neighborBlock,
                    neighborPos, moving);
            if (!level.isClientSide && level instanceof ServerLevel server
                    && level.getBlockEntity(pos)
                    instanceof AlarmBlockEntity alarm) {
                // Redstone changes should never wait for the periodic door pass.
                alarm.refresh(server, false);
            }
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            VoxelShape shape = rotateNorthShape(
                    NORTH_SHAPE, state.getValue(FACING));
            double offset = visualYOffset(level, pos, state);
            return offset == 0.0D ? shape : shape.move(0.0D, offset, 0.0D);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state,
                BlockGetter level, BlockPos pos, CollisionContext context) {
            return getShape(state, level, pos, context);
        }

        @Override
        public VoxelShape getVisualShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return Shapes.empty();
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
    }

    public static final class AlarmBlockEntity extends BlockEntity
            implements GeoBlockEntity {
        private static final RawAnimation OFF =
                RawAnimation.begin().thenLoop("off");
        private static final RawAnimation ON =
                RawAnimation.begin().thenLoop("on");
        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);

        // Client-only presentation clocks are primitives so the common block
        // entity never links client classes on a dedicated server.
        private boolean clientPhaseInitialized;
        private boolean clientPhaseActive;
        private long clientPhaseStartTick;

        public AlarmBlockEntity(BlockPos pos, BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state);
        }

        private static void serverTick(Level level, BlockPos pos,
                BlockState state, AlarmBlockEntity alarm) {
            if (!(level instanceof ServerLevel server)) return;
            // Six adjacent block checks are cheap enough to do every tick and
            // give the Alarm exact, immediate door behaviour without a broad
            // room-radius scan.
            alarm.applyActive(server, server.hasNeighborSignal(pos)
                    || alarm.hasAdjacentOpenElectricDoor(server));
        }

        private static void clientTick(Level level, BlockPos pos,
                BlockState state, AlarmBlockEntity alarm) {
            boolean active = state.getValue(ACTIVE);
            alarm.syncClientPhase(active);
            AlarmAudioClient.update(level, pos, active);
        }

        private void refresh(ServerLevel server,
                boolean ignoredRefreshDoors) {
            applyActive(server, server.hasNeighborSignal(worldPosition)
                    || hasAdjacentOpenElectricDoor(server));
        }

        /**
         * Door activation is deliberately literal: the Alarm must occupy a
         * block directly next to the electric door structure. No diagonal,
         * room-wide or several-block-away activation is accepted.
         */
        private boolean hasAdjacentOpenElectricDoor(ServerLevel server) {
            for (Direction direction : Direction.values()) {
                BlockPos doorPos = worldPosition.relative(direction);
                BlockState doorState = server.getBlockState(doorPos);

                if (DecontaminationStructure.isOwnedDoor(
                        server, doorPos, doorState)) {
                    continue;
                }

                if (FacilityModule.isElectricDoorOpenOrOpening(doorState)) {
                    return true;
                }

                BlockPos controller = null;
                if (BlastDoorModule.isController(doorState)) {
                    controller = doorPos;
                } else if (BlastDoorModule.isPart(doorState)
                        && BlastDoorStructure.isValidPart(
                                server, doorPos, doorState)) {
                    controller = BlastDoorStructure.controllerPosition(
                            doorPos, doorState);
                }

                if (controller != null
                        && BlastDoorModule.isOpenOrOpening(
                                server, controller)) {
                    return true;
                }
            }
            return false;
        }

        private void applyActive(ServerLevel server, boolean active) {
            BlockState state = getBlockState();
            if (!state.hasProperty(ACTIVE)
                    || state.getValue(ACTIVE) == active) {
                return;
            }
            server.setBlock(worldPosition, state.setValue(ACTIVE, active),
                    Block.UPDATE_ALL);
            setChanged();
        }

        public float projectionPhase(float partialTick) {
            boolean active = getBlockState().getValue(ACTIVE);
            syncClientPhase(active);
            if (!active || level == null) return 0.0F;
            double elapsed = level.getGameTime() + partialTick
                    - clientPhaseStartTick;
            double cycle = elapsed % 20.0D;
            if (cycle < 0.0D) cycle += 20.0D;
            return (float) (cycle / 20.0D);
        }

        private void syncClientPhase(boolean active) {
            if (level == null || !level.isClientSide) return;
            if (!clientPhaseInitialized || active != clientPhaseActive) {
                clientPhaseInitialized = true;
                clientPhaseActive = active;
                clientPhaseStartTick = level.getGameTime();
            }
        }

        private software.bernie.geckolib.core.object.PlayState animate(
                software.bernie.geckolib.core.animation.AnimationState<
                        AlarmBlockEntity> state) {
            return state.setAndContinue(getBlockState().getValue(ACTIVE)
                    ? ON : OFF);
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, "alarm", 0,
                    this::animate));
        }

        @Override
        public double getTick(Object blockEntity) {
            return level == null ? 0.0D : level.getGameTime();
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return animationCache;
        }

        @Override
        public AABB getRenderBoundingBox() {
            // Projected light can land several blocks from the physical lamp.
            return new AABB(worldPosition).inflate(5.0D);
        }
    }

    public static boolean isBlastDoorTopMounted(BlockGetter level,
            BlockPos pos, BlockState alarmState) {
        if (level == null || pos == null || alarmState == null
                || !alarmState.hasProperty(FACING)) {
            return false;
        }
        Direction facing = alarmState.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        BlockState supportState = level.getBlockState(support);
        return isBlastDoorTopSupport(level, support, supportState);
    }

    public static double visualYOffset(BlockGetter level, BlockPos pos,
            BlockState alarmState) {
        return isBlastDoorTopMounted(level, pos, alarmState)
                ? BLAST_DOOR_TOP_MOUNT_Y_OFFSET : 0.0D;
    }

    private static boolean isBlastDoorTopSupport(BlockGetter level,
            BlockPos support, BlockState supportState) {
        return BlastDoorModule.isPart(supportState)
                && supportState.hasProperty(BlastDoorModule.HEIGHT)
                && supportState.getValue(BlastDoorModule.HEIGHT)
                        == BlastDoorStructure.MAX_HEIGHT
                && BlastDoorStructure.isValidPart(
                        level, support, supportState);
    }

    public static final class AlarmItem extends BlockItem implements GeoItem {
        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);

        private AlarmItem(Block block) {
            super(block, new Item.Properties());
            SingletonGeoAnimatable.registerSyncedAnimatable(this);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(
                    "tooltip.scp_classified_directive.alarm")
                    .withStyle(ChatFormatting.GRAY));
            super.appendHoverText(stack, level, tooltip, flag);
        }

        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private AlarmClient.ItemRenderer renderer;

                @Override
                public @NotNull net.minecraft.client.renderer.
                        BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (renderer == null) {
                        renderer = new AlarmClient.ItemRenderer();
                    }
                    return renderer;
                }
            });
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return animationCache;
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
