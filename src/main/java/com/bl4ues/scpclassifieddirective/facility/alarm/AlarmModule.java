package com.bl4ues.scpclassifieddirective.facility.alarm;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.DecontaminationStructure;
import com.bl4ues.scpclassifieddirective.client.AlarmAudioClient;
import com.bl4ues.scpclassifieddirective.client.AlarmClient;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorModule;
import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorStructure;
import com.bl4ues.scpclassifieddirective.item.ScrewdriverItem;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
    /** Configuration survives save/load and is shared by normal and transformed Alarms. */
    public static final BooleanProperty SILENT =
            BooleanProperty.create("silent");
    /** 0/1/2 encode left-center-right and bottom-center-top. */
    public static final IntegerProperty MOUNT_X =
            IntegerProperty.create("mount_x", 0, 2);
    public static final IntegerProperty MOUNT_Y =
            IntegerProperty.create("mount_y", 0, 2);
    /** Relative helper-cell offsets use the same -1/0/+1 encoding. */
    public static final IntegerProperty PART_X =
            IntegerProperty.create("part_x", 0, 2);
    public static final IntegerProperty PART_Y =
            IntegerProperty.create("part_y", 0, 2);
    public static final int ACTIVE_LIGHT_LEVEL = 7;

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
    public static final RegistryObject<AlarmPartBlock> PART =
            BLOCKS.register(PATH + "_part", AlarmPartBlock::new);
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

    public static boolean isController(BlockState state) {
        return state != null && state.is(BLOCK.get());
    }

    public static boolean isPart(BlockState state) {
        return state != null && state.is(PART.get());
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
                    .setValue(ACTIVE, false)
                    .setValue(SILENT, false)
                    .setValue(MOUNT_X,
                            AlarmMountStructure.encodeSlot(
                                    AlarmMountStructure.CENTER))
                    .setValue(MOUNT_Y,
                            AlarmMountStructure.encodeSlot(
                                    AlarmMountStructure.CENTER)));
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
            // The Alarm is wholly rendered by its BlockEntityRenderer. Do not
            // keep a terrain proxy around just for shader-pack classification:
            // it adds another depth/render path and the feature no longer
            // depends on block.properties for its visual light.
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, ACTIVE, SILENT, MOUNT_X, MOUNT_Y);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction clicked = context.getClickedFace();
            if (clicked.getAxis() == Direction.Axis.Y) return null;

            int horizontal = AlarmMountStructure.quantize(
                    AlarmMountStructure.horizontalClick(context, clicked));
            int vertical = AlarmMountStructure.quantize(
                    AlarmMountStructure.verticalClick(context, clicked));
            BlockState state = defaultBlockState()
                    .setValue(FACING, clicked)
                    .setValue(ACTIVE, false)
                    .setValue(SILENT, false)
                    .setValue(MOUNT_X,
                            AlarmMountStructure.encodeSlot(horizontal))
                    .setValue(MOUNT_Y,
                            AlarmMountStructure.encodeSlot(vertical));

            BlockPos pos = context.getClickedPos();
            if (!AlarmMountStructure.blastDoorPlacementAllowed(
                    context.getLevel(), pos, state)) {
                return null;
            }
            return AlarmMountStructure.canPlace(
                    context.getLevel(), pos, state) ? state : null;
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level,
                BlockPos pos) {
            return AlarmMountStructure.canSurvive(level, pos, state);
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
        public InteractionResult use(BlockState state, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (!(player.getItemInHand(hand).getItem() instanceof ScrewdriverItem)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(SILENT,
                        !state.getValue(SILENT)), Block.UPDATE_CLIENTS);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (level.isClientSide || !(level instanceof ServerLevel server)) {
                return;
            }

            if (!oldState.is(this)
                    && !AlarmMountStructure.placeParts(level, pos, state)) {
                level.destroyBlock(pos, true);
                return;
            }

            if (level.getBlockEntity(pos)
                    instanceof AlarmBlockEntity alarm) {
                alarm.refresh(server, true);
            }
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (!level.isClientSide && !newState.is(this)) {
                AlarmMountStructure.removeParts(level, pos, state);
            }
            super.onRemove(state, level, pos, newState, moving);
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
            return mountedShape(state);
        }

        private static VoxelShape mountedShape(BlockState state) {
            VoxelShape shape = rotateNorthShape(
                    NORTH_SHAPE, state.getValue(FACING));
            Vec3 offset = AlarmMountStructure.visualOffset(state);
            return shape.move(offset.x, offset.y, offset.z);
        }

        private static VoxelShape shapeForPart(BlockGetter level,
                BlockPos partPos, BlockState partState) {
            if (!AlarmMountStructure.isValidPart(
                    level, partPos, partState)) {
                return Shapes.empty();
            }
            BlockPos controller = AlarmMountStructure.controllerPosition(
                    partPos, partState);
            BlockState controllerState = level.getBlockState(controller);
            VoxelShape inController = mountedShape(controllerState);
            /*
             * Do not clip the helper's shape to its own 1x1x1 cell. Edge and
             * corner mounts deliberately span two/four cells, and every one of
             * those cells must select the SAME physical Alarm. Translating the
             * controller shape into helper-local coordinates makes all cells
             * resolve to one identical world-space outline/collision volume.
             */
            return inController.move(
                    controller.getX() - partPos.getX(),
                    controller.getY() - partPos.getY(),
                    controller.getZ() - partPos.getZ());
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

    public static final class AlarmPartBlock extends Block {
        private AlarmPartBlock() {
            super(BlockBehaviour.Properties.of()
                    .strength(2.0F, 8.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .noLootTable()
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(PART_X,
                            AlarmMountStructure.encodeSlot(
                                    AlarmMountStructure.CENTER))
                    .setValue(PART_Y,
                            AlarmMountStructure.encodeSlot(
                                    AlarmMountStructure.POSITIVE)));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, PART_X, PART_Y);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return AlarmBlock.shapeForPart(level, pos, state);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state,
                BlockGetter level, BlockPos pos, CollisionContext context) {
            return getShape(state, level, pos, context);
        }

        @Override
        public VoxelShape getVisualShape(BlockState state,
                BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighborState, LevelAccessor level, BlockPos pos,
                BlockPos neighborPos) {
            if (!AlarmMountStructure.isValidPart(level, pos, state)) {
                return Blocks.AIR.defaultBlockState();
            }
            return super.updateShape(state, direction, neighborState,
                    level, pos, neighborPos);
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos,
                BlockState state, Player player) {
            if (!level.isClientSide
                    && AlarmMountStructure.isValidPart(level, pos, state)) {
                BlockPos controller =
                        AlarmMountStructure.controllerPosition(pos, state);
                level.destroyBlock(controller, !player.isCreative());
            }
            super.playerWillDestroy(level, pos, state, player);
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target,
                BlockGetter level, BlockPos pos, Player player) {
            return new ItemStack(ITEM.get());
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

            /*
             * Internal-development migration: Alarms placed on the old Blast
             * Door copycat row had no mount properties and therefore deserialize
             * at the center anchor. Center/lower anchors are now forbidden on
             * that strip, so such a state is unambiguously legacy and can be
             * promoted to the new top-center anchor without user intervention.
             */
            if (AlarmMountStructure.blastDoorController(
                    server, pos, state) != null
                    && AlarmMountStructure.decodeSlot(
                            state.getValue(MOUNT_Y))
                            != AlarmMountStructure.POSITIVE) {
                BlockState migrated = state.setValue(MOUNT_Y,
                        AlarmMountStructure.encodeSlot(
                                AlarmMountStructure.POSITIVE));
                if (AlarmMountStructure.canPlace(server, pos, migrated)) {
                    server.setBlock(pos, migrated, Block.UPDATE_ALL);
                    state = migrated;
                }
            }

            if (!AlarmMountStructure.canSurvive(server, pos, state)) {
                server.destroyBlock(pos, true);
                return;
            }

            // At most three helper cells are repaired. Door/redstone checks
            // then use the complete physical footprint of the mounted Alarm.
            AlarmMountStructure.ensureParts(server, pos, state);
            alarm.applyActive(server,
                    AlarmMountStructure.hasNeighborSignal(server, pos, state)
                    || alarm.hasAdjacentOpenElectricDoor(server));
        }

        private static void clientTick(Level level, BlockPos pos,
                BlockState state, AlarmBlockEntity alarm) {
            boolean active = state.getValue(ACTIVE);
            alarm.syncClientPhase(active);
            AlarmAudioClient.update(level, pos, active && !state.getValue(SILENT));
        }

        private void refresh(ServerLevel server,
                boolean ignoredRefreshDoors) {
            BlockState state = getBlockState();
            applyActive(server,
                    AlarmMountStructure.hasNeighborSignal(
                            server, worldPosition, state)
                    || hasAdjacentOpenElectricDoor(server));
        }

        /**
         * Door activation is deliberately literal: the Alarm must occupy a
         * block directly next to the electric door structure. No diagonal,
         * room-wide or several-block-away activation is accepted.
         */
        private boolean hasAdjacentOpenElectricDoor(ServerLevel server) {
            BlockState alarmState = getBlockState();

            /*
             * The Blast Door's fourth-height mounting strip is intentionally
             * real wall now, so the door controller is no longer one of the six
             * literal neighbour blocks. The mount system already knows exactly
             * which Blast Door owns that strip; use that relationship directly.
             * This keeps ordinary doors on the strict one-block adjacency rule
             * while restoring the intended "Alarm above this Blast Door"
             * behaviour without bringing back fake copycat geometry.
             */
            BlockPos mountedBlastDoor =
                    AlarmMountStructure.blastDoorController(
                            server, worldPosition, alarmState);
            if (mountedBlastDoor != null
                    && BlastDoorModule.isOpenOrOpening(
                            server, mountedBlastDoor)) {
                return true;
            }

            java.util.HashSet<BlockPos> checked = new java.util.HashSet<>();
            for (BlockPos occupied : AlarmMountStructure.occupiedPositions(
                    worldPosition, alarmState)) {
                for (Direction direction : Direction.values()) {
                    BlockPos doorPos = occupied.relative(direction);
                    if (!checked.add(doorPos)) continue;
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
        return blastDoorTopMountController(level, pos, alarmState) != null;
    }

    @Nullable
    public static BlockPos blastDoorTopMountController(BlockGetter level,
            BlockPos pos, BlockState alarmState) {
        return AlarmMountStructure.blastDoorController(
                level, pos, alarmState);
    }

    public static Vec3 visualOffset(BlockGetter level, BlockPos pos,
            BlockState alarmState) {
        return AlarmMountStructure.visualOffset(alarmState);
    }

    /** Compatibility shim while render code migrates to the full 3-D offset. */
    public static double visualYOffset(BlockGetter level, BlockPos pos,
            BlockState alarmState) {
        return visualOffset(level, pos, alarmState).y;
    }

    public static final class AlarmItem extends BlockItem implements GeoItem {
        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);

        private AlarmItem(Block block) {
            super(block, new Item.Properties());
            SingletonGeoAnimatable.registerSyncedAnimatable(this);
        }

        @Override
        public InteractionResult place(BlockPlaceContext context) {
            BlockState state = BLOCK.get().getStateForPlacement(context);
            if (state == null || !AlarmMountStructure.canPlace(
                    context.getLevel(), context.getClickedPos(), state)) {
                return InteractionResult.FAIL;
            }
            return super.place(context);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(
                    "tooltip.scp_classified_directive.alarm")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(
                    "Use a Screwdriver to toggle alarm sound.")
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
