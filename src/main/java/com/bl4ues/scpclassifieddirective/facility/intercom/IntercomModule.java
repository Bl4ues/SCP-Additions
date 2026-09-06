package com.bl4ues.scpclassifieddirective.facility.intercom;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.IntercomAudioClient;
import com.bl4ues.scpclassifieddirective.client.IntercomClient;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoom;
import com.bl4ues.scpclassifieddirective.facility.speaker.FacilitySpeakerRegistry;
import com.bl4ues.scpclassifieddirective.facility.speaker.SpeakerBroadcastManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
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
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/** Desk-mounted facility Intercom that captures a small local acoustic radius. */
public final class IntercomModule {
    public static final String PATH = "intercom";
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final double CAPTURE_RADIUS = 5.0D;
    public static final double USER_RADIUS = 2.0D;

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(
            ForgeRegistries.SOUND_EVENTS, ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<IntercomBlock> BLOCK =
            BLOCKS.register(PATH, IntercomBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register(PATH,
            () -> new IntercomItem(BLOCK.get()));
    public static final RegistryObject<BlockEntityType<IntercomBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register(PATH, () -> BlockEntityType.Builder.of(
                    IntercomBlockEntity::new, BLOCK.get()).build(null));

    public static final RegistryObject<SoundEvent> ON = sound("intercom_on");
    public static final RegistryObject<SoundEvent> OFF = sound("intercom_off");
    public static final RegistryObject<SoundEvent> LOOP = sound("intercom_loop");

    private IntercomModule() {
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

    /** Authored push-button centre in the canonical NORTH orientation. */
    public static Vec3 buttonAnchor(BlockPos pos, BlockState state) {
        return localToWorld(pos, state, 12.25D / 16.0D,
                6.85D / 16.0D, 5.0D / 16.0D);
    }

    /** Acoustic origin follows the lapel microphone head rather than block centre. */
    public static Vec3 microphonePosition(BlockPos pos, BlockState state) {
        return localToWorld(pos, state, 4.0D / 16.0D,
                13.0D / 16.0D, 4.5D / 16.0D);
    }

    private static Vec3 localToWorld(BlockPos pos, BlockState state,
            double x, double y, double z) {
        Vec3 local = new Vec3(x - 0.5D, y - 0.5D, z - 0.5D);
        Direction facing = state.hasProperty(FACING)
                ? state.getValue(FACING) : Direction.NORTH;
        Vec3 rotated = switch (facing) {
            case SOUTH -> new Vec3(-local.x, local.y, -local.z);
            case EAST -> new Vec3(-local.z, local.y, local.x);
            case WEST -> new Vec3(local.z, local.y, -local.x);
            default -> local;
        };
        return Vec3.atCenterOf(pos).add(rotated);
    }

    public static final class IntercomBlock extends BaseEntityBlock {
        // Collision remains the rectangular console body. Selection extends
        // over the authored flexible microphone as requested, so aiming at the
        // visible upper part still outlines/selects the complete Intercom.
        private static final VoxelShape BODY_NORTH = Block.box(
                1.0D, 0.0D, 2.0D, 15.0D, 7.0D, 14.0D);
        private static final VoxelShape OUTLINE_NORTH = Block.box(
                -0.1D, 0.0D, 2.0D, 16.1D, 19.0D, 14.0D);

        private IntercomBlock() {
            super(BlockBehaviour.Properties.of()
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(ACTIVE, false));
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new IntercomBlockEntity(pos, state);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                BlockState state, BlockEntityType<T> type) {
            return level.isClientSide
                    ? createTickerHelper(type, BLOCK_ENTITY.get(),
                            IntercomBlockEntity::clientTick)
                    : createTickerHelper(type, BLOCK_ENTITY.get(),
                            IntercomBlockEntity::serverTick);
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, ACTIVE);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = defaultBlockState().setValue(FACING,
                    context.getHorizontalDirection().getOpposite());
            return state.canSurvive(context.getLevel(), context.getClickedPos())
                    ? state : null;
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level,
                BlockPos pos) {
            BlockPos below = pos.below();
            return level.getBlockState(below).isFaceSturdy(level, below,
                    Direction.UP);
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighborState, LevelAccessor level, BlockPos pos,
                BlockPos neighborPos) {
            if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
                return Blocks.AIR.defaultBlockState();
            }
            return super.updateShape(state, direction, neighborState, level,
                    pos, neighborPos);
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer serverPlayer)
                    || !(level.getBlockEntity(pos) instanceof IntercomBlockEntity intercom)) {
                return InteractionResult.PASS;
            }
            return intercom.toggle(serverPlayer)
                    ? InteractionResult.CONSUME : InteractionResult.PASS;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return rotateNorthShape(OUTLINE_NORTH, state.getValue(FACING));
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return rotateNorthShape(BODY_NORTH, state.getValue(FACING));
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
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (!level.isClientSide && level instanceof ServerLevel server
                    && newState.getBlock() != this) {
                SpeakerBroadcastManager.stopIntercom(server, pos);
            }
            super.onRemove(state, level, pos, newState, moving);
        }
    }

    public static final class IntercomBlockEntity extends BlockEntity
            implements GeoBlockEntity {
        private static final RawAnimation IDLE_OFF = RawAnimation.begin()
                .thenLoop("idle_off");
        private static final RawAnimation IDLE_ON = RawAnimation.begin()
                .thenLoop("idle_on");
        private static final RawAnimation TURN_ON = RawAnimation.begin()
                .thenPlay("turn_on");
        private static final RawAnimation TURN_OFF = RawAnimation.begin()
                .thenPlay("turn_off");

        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);
        private int endpointRefreshTicks;

        public IntercomBlockEntity(BlockPos pos, BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state);
        }

        public boolean toggle(ServerPlayer player) {
            if (!(level instanceof ServerLevel server)) return false;
            if (getBlockState().getValue(ACTIVE)) {
                deactivate(server, true);
                return true;
            }
            return activate(server, player);
        }

        private boolean activate(ServerLevel server, ServerPlayer player) {
            FacilityRoom room = FacilityMappingManager.roomForPosition(server,
                    worldPosition);
            if (room == null) return false;
            List<FacilitySpeakerRegistry.SpeakerEndpoint> endpoints =
                    FacilitySpeakerRegistry.speakersForRoom(server, room.id());
            if (!SpeakerBroadcastManager.startIntercom(server, worldPosition,
                    player, endpoints)) {
                return false;
            }

            BlockState state = getBlockState();
            server.setBlock(worldPosition, state.setValue(ACTIVE, true),
                    Block.UPDATE_CLIENTS);
            Vec3 microphone = microphonePosition(worldPosition, state);
            server.playSound(null, microphone.x, microphone.y, microphone.z,
                    ON.get(), SoundSource.BLOCKS, 0.32F, 1.0F);
            triggerAnim("intercom", "turn_on");
            endpointRefreshTicks = 0;
            return true;
        }

        private void deactivate(ServerLevel server, boolean playCue) {
            SpeakerBroadcastManager.stopIntercom(server, worldPosition);
            BlockState state = getBlockState();
            if (!state.getValue(ACTIVE)) return;
            server.setBlock(worldPosition, state.setValue(ACTIVE, false),
                    Block.UPDATE_CLIENTS);
            if (playCue) {
                Vec3 microphone = microphonePosition(worldPosition, state);
                server.playSound(null, microphone.x, microphone.y, microphone.z,
                        OFF.get(), SoundSource.BLOCKS, 0.30F, 1.0F);
            }
            triggerAnim("intercom", "turn_off");
        }

        private static void serverTick(Level level, BlockPos pos,
                BlockState state, IntercomBlockEntity intercom) {
            if (!(level instanceof ServerLevel server)
                    || !state.getValue(ACTIVE)) return;

            if (!intercom.hasNearbyUser(server)) {
                intercom.deactivate(server, true);
                return;
            }

            if (++intercom.endpointRefreshTicks < 20) return;
            intercom.endpointRefreshTicks = 0;
            FacilityRoom room = FacilityMappingManager.roomForPosition(server, pos);
            List<FacilitySpeakerRegistry.SpeakerEndpoint> endpoints = room == null
                    ? List.of()
                    : FacilitySpeakerRegistry.speakersForRoom(server, room.id());
            if (!SpeakerBroadcastManager.refreshIntercom(server, pos, endpoints)) {
                intercom.deactivate(server, true);
            }
        }

        private boolean hasNearbyUser(ServerLevel server) {
            AABB area = new AABB(worldPosition).inflate(USER_RADIUS);
            Vec3 centre = Vec3.atCenterOf(worldPosition);
            return !server.getEntitiesOfClass(Player.class, area, player ->
                    player.isAlive() && !player.isSpectator()
                            && player.position().distanceToSqr(centre)
                            <= USER_RADIUS * USER_RADIUS).isEmpty();
        }

        private static void clientTick(Level level, BlockPos pos,
                BlockState state, IntercomBlockEntity intercom) {
            IntercomAudioClient.update(level, pos, state.getValue(ACTIVE));
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel server
                    && getBlockState().getValue(ACTIVE)) {
                server.setBlock(worldPosition,
                        getBlockState().setValue(ACTIVE, false),
                        Block.UPDATE_CLIENTS);
            }
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            AnimationController<IntercomBlockEntity> controller =
                    new AnimationController<>(this, "intercom", 0, state ->
                            state.setAndContinue(getBlockState().getValue(ACTIVE)
                                    ? IDLE_ON : IDLE_OFF));
            controller.triggerableAnim("turn_on", TURN_ON);
            controller.triggerableAnim("turn_off", TURN_OFF);
            controllers.add(controller);
        }

        @Override
        public double getTick(Object blockEntity) {
            return level == null ? 0.0D : level.getGameTime();
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return animationCache;
        }
    }

    public static final class IntercomItem extends BlockItem implements GeoItem {
        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);

        private IntercomItem(Block block) {
            super(block, new Item.Properties());
            SingletonGeoAnimatable.registerSyncedAnimatable(this);
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.literal("Intercom");
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal("Broadcasts to Facility Speakers")
                    .withStyle(ChatFormatting.GRAY));
            super.appendHoverText(stack, level, tooltip, flag);
        }

        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private IntercomClient.ItemRenderer renderer;

                @Override
                public @NotNull net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
                        getCustomRenderer() {
                    if (renderer == null) renderer = new IntercomClient.ItemRenderer();
                    return renderer;
                }
            });
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            // Inventory representation uses the authored default/off pose.
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
