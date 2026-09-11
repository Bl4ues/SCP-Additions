package com.bl4ues.scpclassifieddirective.facility.blastdoor;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.BlastDoorClient;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager;
import com.bl4ues.scpclassifieddirective.facility.StructurePlacementFeedback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Redstone-operated, multiblock Blast Door. */
public final class BlastDoorModule {
    public static final String PATH = "blast_door";
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Phase> PHASE =
            EnumProperty.create("phase", Phase.class);
    public static final IntegerProperty SIDE =
            IntegerProperty.create("side", 0, 6);
    public static final IntegerProperty HEIGHT =
            IntegerProperty.create("height", 0, 3);

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<BlastDoorBlock> BLOCK =
            BLOCKS.register(PATH, BlastDoorBlock::new);
    public static final RegistryObject<BlastDoorPartBlock> PART =
            BLOCKS.register(PATH + "_part", BlastDoorPartBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register(PATH,
            () -> new BlastDoorItem(BLOCK.get()));
    public static final RegistryObject<BlockEntityType<BlastDoorBlockEntity>>
            BLOCK_ENTITY = BLOCK_ENTITIES.register(PATH,
            () -> BlockEntityType.Builder.of(BlastDoorBlockEntity::new,
                    BLOCK.get()).build(null));
    public static final RegistryObject<SoundEvent> OPEN_SOUND =
            sound("blast_door_open");
    public static final RegistryObject<SoundEvent> CLOSE_SOUND =
            sound("blast_door_close");

    private BlastDoorModule() {
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

    public static boolean isController(BlockState state) {
        return state != null && state.is(BLOCK.get());
    }

    public static boolean isPart(BlockState state) {
        return state != null && state.is(PART.get());
    }

    public static boolean isStructureState(BlockState state) {
        return isController(state) || isPart(state);
    }

    @Nullable
    public static BlockPos controllerPosition(BlockGetter level, BlockPos pos,
            BlockState state) {
        if (isController(state)) return pos.immutable();
        if (!isPart(state)) return null;
        BlockPos controller = BlastDoorStructure.controllerPosition(pos, state);
        return isController(level.getBlockState(controller))
                ? controller : null;
    }

    public static boolean isPassableState(BlockState state) {
        return isController(state)
                && (state.getValue(PHASE) == Phase.OPEN
                || state.getValue(PHASE) == Phase.CLOSING);
    }

    public static boolean hasRedstoneConnection(Level level,
            BlockPos controllerPos) {
        return BlastDoorStructure.hasRedstoneConnection(level, controllerPos);
    }

    public static boolean setRemoteOpen(ServerLevel level,
            BlockPos controllerPos, boolean open) {
        BlockState state = level.getBlockState(controllerPos);
        if (!isController(state)
                || !(level.getBlockEntity(controllerPos)
                instanceof BlastDoorBlockEntity door)
                || !BlastDoorStructure.hasRedstoneConnection(
                        level, controllerPos)) {
            return false;
        }
        return door.setRemoteCommand(level, open);
    }

    public static boolean isOpenOrOpening(Level level, BlockPos controllerPos) {
        if (level.getBlockEntity(controllerPos)
                instanceof BlastDoorBlockEntity door) {
            return door.phase == Phase.OPEN || door.phase == Phase.OPENING;
        }
        BlockState state = level.getBlockState(controllerPos);
        return isController(state)
                && (state.getValue(PHASE) == Phase.OPEN
                || state.getValue(PHASE) == Phase.OPENING);
    }

    public enum Phase implements StringRepresentable {
        CLOSED("closed"),
        OPENING("opening"),
        OPEN("open"),
        CLOSING("closing");

        private final String serializedName;

        Phase(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public static final class BlastDoorBlock extends BaseEntityBlock
            implements SimpleWaterloggedBlock {
        private BlastDoorBlock() {
            super(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(37.5F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(WATERLOGGED, false)
                    .setValue(PHASE, Phase.CLOSED));
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BlastDoorBlockEntity(pos, state);
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide ? null : createTickerHelper(type,
                    BLOCK_ENTITY.get(), BlastDoorBlockEntity::serverTick);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockPos pos = context.getClickedPos();
            Direction facing = context.getHorizontalDirection().getOpposite();
            BlockPos support = pos.below();
            if (!context.getLevel().getBlockState(support).isFaceSturdy(
                    context.getLevel(), support, Direction.UP)
                    || !BlastDoorStructure.canPlace(context.getLevel(), pos,
                    facing)) {
                return null;
            }
            return defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(WATERLOGGED,
                            context.getLevel().getFluidState(pos).getType()
                                    == Fluids.WATER);
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (level.isClientSide || oldState.is(this)) return;
            Direction facing = state.getValue(FACING);
            if (!BlastDoorStructure.placeParts(level, pos, facing)) {
                level.destroyBlock(pos, true);
                return;
            }
            if (level instanceof ServerLevel server) {
                Scp079FacilityAccessManager.registerDoor(server, pos);
                if (level.getBlockEntity(pos)
                        instanceof BlastDoorBlockEntity door) {
                    door.refreshImmediately(server);
                }
            }
        }

        @Override
        public void neighborChanged(BlockState state, Level level, BlockPos pos,
                Block neighborBlock, BlockPos neighborPos, boolean moving) {
            super.neighborChanged(state, level, pos, neighborBlock, neighborPos,
                    moving);
            if (level instanceof ServerLevel server
                    && level.getBlockEntity(pos)
                    instanceof BlastDoorBlockEntity door) {
                door.refreshImmediately(server);
            }
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return BlastDoorStructure.shapeAt(level, pos, state);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return BlastDoorStructure.shapeAt(level, pos, state);
        }

        @Override
        public VoxelShape getVisualShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return BlastDoorStructure.shapeAt(level, pos, state);
        }

        @Override
        public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
                BlockPos pos) {
            return Shapes.empty();
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, WATERLOGGED, PHASE);
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
                    ? Fluids.WATER.getSource(false)
                    : super.getFluidState(state);
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
        public boolean canHarvestBlock(BlockState state, BlockGetter level,
                BlockPos pos, Player player) {
            return player.getInventory().getSelected().getItem()
                    instanceof PickaxeItem pickaxe
                    && pickaxe.getTier().getLevel() >= 1;
        }

        @Override
        public List<ItemStack> getDrops(BlockState state,
                LootParams.Builder builder) {
            return Collections.singletonList(new ItemStack(ITEM.get()));
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target,
                BlockGetter level, BlockPos pos, Player player) {
            return new ItemStack(ITEM.get());
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (!level.isClientSide && !newState.is(this)) {
                BlastDoorStructure.removeParts(level, pos, state);
                if (level instanceof ServerLevel server) {
                    Scp079FacilityAccessManager.unregisterDoor(server, pos);
                }
            }
            super.onRemove(state, level, pos, newState, moving);
        }
    }

    public static final class BlastDoorPartBlock extends Block
            implements SimpleWaterloggedBlock {
        private BlastDoorPartBlock() {
            super(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(37.5F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(SIDE, 3)
                    .setValue(HEIGHT, 0)
                    .setValue(WATERLOGGED, false));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, SIDE, HEIGHT, WATERLOGGED);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return BlastDoorStructure.shapeAt(level, pos, state);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return BlastDoorStructure.shapeAt(level, pos, state);
        }

        @Override
        public VoxelShape getVisualShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return BlastDoorStructure.shapeAt(level, pos, state);
        }

        @Override
        public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
                BlockPos pos) {
            return Shapes.empty();
        }

        @Override
        public FluidState getFluidState(BlockState state) {
            return state.getValue(WATERLOGGED)
                    ? Fluids.WATER.getSource(false)
                    : super.getFluidState(state);
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
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (!level.isClientSide) level.scheduleTick(pos, this, 40);
        }

        @Override
        public void neighborChanged(BlockState state, Level level, BlockPos pos,
                Block neighborBlock, BlockPos neighborPos, boolean moving) {
            super.neighborChanged(state, level, pos, neighborBlock, neighborPos,
                    moving);
            if (!(level instanceof ServerLevel server)) return;
            BlockPos controller = BlastDoorStructure.controllerPosition(pos,
                    state);
            if (level.getBlockEntity(controller)
                    instanceof BlastDoorBlockEntity door) {
                door.refreshImmediately(server);
            }
        }

        @Override
        public void tick(BlockState state, ServerLevel level, BlockPos pos,
                net.minecraft.util.RandomSource random) {
            if (!BlastDoorStructure.isValidPart(level, pos, state)) {
                BlastDoorStructure.clearBlock(level, pos, state);
                return;
            }
            BlockPos controller = BlastDoorStructure.controllerPosition(pos,
                    state);
            BlockState controllerState = level.getBlockState(controller);
            BlastDoorStructure.ensureParts(level, controller,
                    controllerState.getValue(FACING));
            level.scheduleTick(pos, this, 40);
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
        public List<ItemStack> getDrops(BlockState state,
                LootParams.Builder builder) {
            return Collections.emptyList();
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target,
                BlockGetter level, BlockPos pos, Player player) {
            return new ItemStack(ITEM.get());
        }
    }

    public static final class BlastDoorBlockEntity extends BlockEntity
            implements GeoBlockEntity {
        public static final int TRANSITION_TICKS = 40;
        private static final double MAX_LIFT_PIXELS = 33.0D;
        private static final RawAnimation CLOSED_ANIMATION =
                RawAnimation.begin().thenLoop("closed");
        private static final RawAnimation OPENING_ANIMATION =
                RawAnimation.begin().thenPlay("opening").thenLoop("open");
        private static final RawAnimation OPEN_ANIMATION =
                RawAnimation.begin().thenLoop("open");
        private static final RawAnimation CLOSING_ANIMATION =
                RawAnimation.begin().thenPlay("closing").thenLoop("closed");

        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);
        private Phase phase;
        private long phaseStarted;
        private boolean initializedPower;
        private boolean lastPhysicalPower;
        private boolean remoteOverride;
        private boolean remoteOpen;

        public BlastDoorBlockEntity(BlockPos pos, BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state);
            phase = state.hasProperty(PHASE)
                    ? state.getValue(PHASE) : Phase.CLOSED;
        }

        private static void serverTick(Level level, BlockPos pos,
                BlockState state, BlastDoorBlockEntity door) {
            if (!(level instanceof ServerLevel server)) return;
            door.evaluate(server);
        }

        private void evaluate(ServerLevel server) {
            boolean physicalPower = BlastDoorStructure.hasNeighborSignal(
                    server, worldPosition);
            boolean connected = BlastDoorStructure.hasRedstoneConnection(
                    server, worldPosition);

            if (!initializedPower) {
                initializedPower = true;
                lastPhysicalPower = physicalPower;
            } else if (physicalPower != lastPhysicalPower) {
                remoteOverride = false;
            }
            if (remoteOverride && !connected) remoteOverride = false;
            lastPhysicalPower = physicalPower;

            boolean desiredOpen = remoteOverride ? remoteOpen : physicalPower;
            long elapsed = Math.max(0L,
                    server.getGameTime() - phaseStarted);
            switch (phase) {
                case CLOSED -> {
                    if (desiredOpen) begin(server, Phase.OPENING, true);
                }
                case OPENING -> {
                    if (elapsed >= TRANSITION_TICKS) {
                        begin(server, desiredOpen
                                ? Phase.OPEN : Phase.CLOSING, !desiredOpen);
                    }
                }
                case OPEN -> {
                    if (!desiredOpen) begin(server, Phase.CLOSING, true);
                }
                case CLOSING -> {
                    if (elapsed >= TRANSITION_TICKS) {
                        begin(server, desiredOpen
                                ? Phase.OPENING : Phase.CLOSED, desiredOpen);
                    }
                }
            }
        }

        public void refreshImmediately(ServerLevel server) {
            evaluate(server);
        }

        private boolean setRemoteCommand(ServerLevel server, boolean open) {
            if (remoteOverride && remoteOpen == open) return true;
            remoteOverride = true;
            remoteOpen = open;
            setChanged();
            sync();
            evaluate(server);
            return true;
        }

        private void begin(ServerLevel server, Phase next,
                boolean playTransitionSound) {
            phase = next;
            phaseStarted = server.getGameTime();
            BlockState state = getBlockState();
            if (state.hasProperty(PHASE)
                    && state.getValue(PHASE) != next) {
                server.setBlock(worldPosition, state.setValue(PHASE, next),
                        Block.UPDATE_CLIENTS);
            }
            if (playTransitionSound) {
                if (next == Phase.OPENING) {
                    server.playSound(null, worldPosition, OPEN_SOUND.get(),
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                } else if (next == Phase.CLOSING) {
                    server.playSound(null, worldPosition, CLOSE_SOUND.get(),
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
            setChanged();
            sync();
        }

        public double doorLiftPixels() {
            long now = level == null ? phaseStarted : level.getGameTime();
            double t = Math.max(0.0D, Math.min(1.0D,
                    (now - phaseStarted) / (double) TRANSITION_TICKS));
            double progress = mechanicalProgress(t);
            return switch (phase) {
                case CLOSED -> 0.0D;
                case OPEN -> MAX_LIFT_PIXELS;
                case OPENING -> MAX_LIFT_PIXELS * progress;
                case CLOSING -> MAX_LIFT_PIXELS * (1.0D - progress);
            };
        }

        /**
         * Short motor ramp-up, long constant travel and controlled braking.
         * This is deliberately non-elastic: a blast door should look like
         * several hundred kilograms of metal, not an agitated slime block.
         */
        private static double mechanicalProgress(double x) {
            final double ramp = 0.18D;
            final double normalization = 1.0D - ramp;
            if (x <= 0.0D) return 0.0D;
            if (x >= 1.0D) return 1.0D;
            if (x < ramp) {
                return (x * x) / (2.0D * ramp * normalization);
            }
            if (x > 1.0D - ramp) {
                double remaining = 1.0D - x;
                return 1.0D - (remaining * remaining)
                        / (2.0D * ramp * normalization);
            }
            return (ramp * 0.5D + x - ramp) / normalization;
        }

        private software.bernie.geckolib.core.object.PlayState animate(
                software.bernie.geckolib.core.animation.AnimationState<
                        BlastDoorBlockEntity> state) {
            Phase current = getBlockState().hasProperty(PHASE)
                    ? getBlockState().getValue(PHASE) : phase;
            RawAnimation animation = switch (current) {
                case CLOSED -> CLOSED_ANIMATION;
                case OPENING -> OPENING_ANIMATION;
                case OPEN -> OPEN_ANIMATION;
                case CLOSING -> CLOSING_ANIMATION;
            };
            return state.setAndContinue(animation);
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, "blast_door", 0,
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
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putString("BlastDoorPhase", phase.getSerializedName());
            tag.putLong("BlastDoorPhaseStarted", phaseStarted);
            tag.putBoolean("BlastDoorPowerInitialized", initializedPower);
            tag.putBoolean("BlastDoorLastPower", lastPhysicalPower);
            tag.putBoolean("BlastDoorRemoteOverride", remoteOverride);
            tag.putBoolean("BlastDoorRemoteOpen", remoteOpen);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            phase = parsePhase(tag.getString("BlastDoorPhase"));
            phaseStarted = tag.getLong("BlastDoorPhaseStarted");
            initializedPower = tag.getBoolean("BlastDoorPowerInitialized");
            lastPhysicalPower = tag.getBoolean("BlastDoorLastPower");
            remoteOverride = tag.getBoolean("BlastDoorRemoteOverride");
            remoteOpen = tag.getBoolean("BlastDoorRemoteOpen");
        }

        private static Phase parsePhase(String value) {
            for (Phase candidate : Phase.values()) {
                if (candidate.getSerializedName().equals(value)) {
                    return candidate;
                }
            }
            return Phase.CLOSED;
        }

        private void sync() {
            if (level == null) return;
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state,
                    Block.UPDATE_CLIENTS);
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public void handleUpdateTag(CompoundTag tag) {
            load(tag);
        }

        @Nullable
        @Override
        public ClientboundBlockEntityDataPacket getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public void onDataPacket(Connection net,
                ClientboundBlockEntityDataPacket packet) {
            CompoundTag tag = packet.getTag();
            if (tag != null) load(tag);
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition).inflate(4.0D, 6.0D, 2.0D);
        }
    }

    public static final class BlastDoorItem extends BlockItem
            implements GeoItem {
        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);

        private BlastDoorItem(Block block) {
            super(block, new Item.Properties());
            SingletonGeoAnimatable.registerSyncedAnimatable(this);
        }

        @Override
        public InteractionResult place(BlockPlaceContext context) {
            BlockPos pos = context.getClickedPos();
            Direction facing = context.getHorizontalDirection().getOpposite();
            BlockPos support = pos.below();
            if (context.getLevel().getBlockState(support).isFaceSturdy(
                    context.getLevel(), support, Direction.UP)) {
                List<BlockPos> blockers =
                        BlastDoorStructure.collectObstructions(
                                context.getLevel(), pos, facing);
                if (!blockers.isEmpty()) {
                    StructurePlacementFeedback.reportBlocked(context, blockers);
                    return InteractionResult.FAIL;
                }
            }
            return super.place(context);
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.literal("Blast Door");
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal("Heavy redstone-operated security door")
                    .withStyle(ChatFormatting.GRAY));
            super.appendHoverText(stack, level, tooltip, flag);
        }

        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private BlastDoorClient.ItemRenderer renderer;

                @Override
                public @NotNull net.minecraft.client.renderer.
                        BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (renderer == null) {
                        renderer = new BlastDoorClient.ItemRenderer();
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
}
