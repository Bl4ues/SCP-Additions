package com.bl4ues.scpclassifieddirective.facility.surveillance;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.SurveillanceCameraClient;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
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
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Wall-mounted surveillance camera used by SCP-079 and the shared facility
 * surveillance backend. The historical class name is retained so existing
 * integrations keep compiling, but the old placeholder renderer is gone.
 */
public final class SurveillanceCameraPlaceholderModule {
    public static final String PATH = "surveillance_camera";
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Manual operator envelope: 180 degrees horizontal in total. */
    public static final float MANUAL_YAW_LIMIT = 90.0F;
    /** Manual operator envelope: 90 degrees vertical in total. */
    public static final float MANUAL_MIN_PITCH = -45.0F;
    public static final float MANUAL_MAX_PITCH = 45.0F;
    /** Idle scan reaches fifty degrees either side of the mounted direction. */
    public static final float IDLE_SWEEP_YAW_LIMIT = 50.0F;

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<SurveillanceCameraBlock> BLOCK =
            BLOCKS.register(PATH, SurveillanceCameraBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register(PATH,
            () -> new SurveillanceCameraItem(BLOCK.get()));
    public static final RegistryObject<BlockEntityType<SurveillanceCameraBlockEntity>>
            BLOCK_ENTITY = BLOCK_ENTITIES.register(PATH, () ->
                    BlockEntityType.Builder.of(SurveillanceCameraBlockEntity::new,
                            BLOCK.get()).build(null));

    private SurveillanceCameraPlaceholderModule() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    public static Vec3 eyePosition(BlockPos pos, BlockState state) {
        Direction facing = state.hasProperty(FACING)
                ? state.getValue(FACING) : Direction.NORTH;
        // The authored lens sits almost directly above the block centre while
        // the bracket extends backwards into the wall. Keep the viewpoint just
        // in front of the lens to avoid clipping into the model itself.
        return Vec3.atCenterOf(pos).add(
                facing.getStepX() * 0.10D,
                0.03D,
                facing.getStepZ() * 0.10D);
    }

    static UUID cameraId(ServerLevel level, BlockPos pos) {
        String key = ScpClassifiedDirectiveMod.MODID + ":camera:"
                + level.dimension().location() + ":" + pos.asLong();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    public static final class SurveillanceCameraBlock extends BaseEntityBlock {
        // Canonical NORTH shape. The authored camera is intentionally narrow;
        // selection remains slightly generous so a wall-mounted prop is not a
        // pixel-hunting exercise.
        private static final VoxelShape NORTH = Block.box(
                6.5D, 4.5D, 7.0D, 9.5D, 10.0D, 16.0D);

        private SurveillanceCameraBlock() {
            super(BlockBehaviour.Properties.of()
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());
            registerDefaultState(stateDefinition.any().setValue(FACING,
                    Direction.NORTH));
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new SurveillanceCameraBlockEntity(pos, state);
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
                            SurveillanceCameraBlockEntity::clientTick)
                    : createTickerHelper(type, BLOCK_ENTITY.get(),
                            SurveillanceCameraBlockEntity::serverTick);
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
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
        public boolean canSurvive(BlockState state, BlockGetter level,
                BlockPos pos) {
            Direction facing = state.getValue(FACING);
            BlockPos supportPos = pos.relative(facing.getOpposite());
            return level.getBlockState(supportPos).isFaceSturdy(level,
                    supportPos, facing);
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
            String name = "Camera " + pos.getX() + ", " + pos.getY()
                    + ", " + pos.getZ();
            FacilitySurveillanceRegistry.register(level, cameraId(level, pos),
                    pos, eyePosition(pos, state), name, facing.toYRot(), 0.0F,
                    MANUAL_YAW_LIMIT, MANUAL_MIN_PITCH,
                    MANUAL_MAX_PITCH, 2.5F);
        }
    }

    public static final class SurveillanceCameraBlockEntity extends BlockEntity
            implements GeoBlockEntity {
        private static final int IDLE_CYCLE_TICKS = 300;
        private static final int IDLE_PAUSE_TICKS = 20;
        private static final int IDLE_TRAVEL_TICKS = 120;
        private static final float MANUAL_YAW_SPEED = 6.0F;
        private static final float MANUAL_PITCH_SPEED = 4.5F;
        private static final float IDLE_YAW_SPEED = 1.15F;
        private static final float IDLE_PITCH_SPEED = 1.0F;

        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);

        private boolean controlled;
        private float targetYaw;
        private float targetPitch;

        private float previousVisualYaw;
        private float visualYaw;
        private float previousVisualPitch;
        private float visualPitch;
        private boolean visualInitialized;
        private boolean visuallyMoving;

        public SurveillanceCameraBlockEntity(BlockPos pos, BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state);
        }

        private static void serverTick(Level level, BlockPos pos,
                BlockState state, SurveillanceCameraBlockEntity camera) {
            if (!(level instanceof ServerLevel server)) return;
            ServerPlayer controller = Scp079PlayableManager.controller(
                    server.getServer());
            boolean activeControl = false;
            float wantedYaw = camera.targetYaw;
            float wantedPitch = camera.targetPitch;

            if (controller != null && Scp079PlayableManager.isCameraMode(controller)
                    && controller.level().dimension().equals(server.dimension())) {
                FacilityCameraDefinition definition = FacilitySurveillanceRegistry.camera(
                        server, cameraId(server, pos));
                if (definition != null
                        && controller.position().distanceToSqr(
                        definition.eyePosition()) <= 0.36D) {
                    activeControl = true;
                    Direction facing = state.getValue(FACING);
                    wantedYaw = Mth.clamp(Mth.wrapDegrees(
                                    controller.getYRot() - facing.toYRot()),
                            -MANUAL_YAW_LIMIT, MANUAL_YAW_LIMIT);
                    wantedPitch = Mth.clamp(controller.getXRot(),
                            MANUAL_MIN_PITCH, MANUAL_MAX_PITCH);
                }
            }

            boolean changed = activeControl != camera.controlled;
            if (activeControl) {
                changed |= Math.abs(Mth.wrapDegrees(
                        wantedYaw - camera.targetYaw)) > 0.08F;
                changed |= Math.abs(wantedPitch - camera.targetPitch) > 0.08F;
            }
            if (!changed) return;

            camera.controlled = activeControl;
            if (activeControl) {
                camera.targetYaw = wantedYaw;
                camera.targetPitch = wantedPitch;
            }
            camera.setChanged();
            server.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        private static void clientTick(Level level, BlockPos pos,
                BlockState state, SurveillanceCameraBlockEntity camera) {
            float wantedYaw = camera.controlled
                    ? camera.targetYaw : idleYaw(level.getGameTime(), pos);
            float wantedPitch = camera.controlled ? camera.targetPitch : 0.0F;

            if (!camera.visualInitialized) {
                camera.visualInitialized = true;
                camera.visualYaw = wantedYaw;
                camera.previousVisualYaw = wantedYaw;
                camera.visualPitch = wantedPitch;
                camera.previousVisualPitch = wantedPitch;
                camera.visuallyMoving = false;
                return;
            }

            camera.previousVisualYaw = camera.visualYaw;
            camera.previousVisualPitch = camera.visualPitch;
            float yawSpeed = camera.controlled
                    ? MANUAL_YAW_SPEED : IDLE_YAW_SPEED;
            float pitchSpeed = camera.controlled
                    ? MANUAL_PITCH_SPEED : IDLE_PITCH_SPEED;
            camera.visualYaw = Mth.approachDegrees(camera.visualYaw,
                    wantedYaw, yawSpeed);
            camera.visualPitch = Mth.approach(camera.visualPitch,
                    wantedPitch, pitchSpeed);
            camera.visuallyMoving = Math.abs(Mth.wrapDegrees(
                    camera.visualYaw - camera.previousVisualYaw)) > 0.01F
                    || Math.abs(camera.visualPitch
                    - camera.previousVisualPitch) > 0.01F;
        }

        private static float idleYaw(long gameTime, BlockPos pos) {
            int offset = Math.floorMod((int) (pos.asLong()
                    ^ (pos.asLong() >>> 32)), IDLE_CYCLE_TICKS);
            int phase = Math.floorMod((int) (gameTime + offset),
                    IDLE_CYCLE_TICKS);
            if (phase < IDLE_PAUSE_TICKS) {
                return -IDLE_SWEEP_YAW_LIMIT;
            }
            int firstEnd = IDLE_PAUSE_TICKS + IDLE_TRAVEL_TICKS;
            if (phase < firstEnd) {
                float t = (phase - IDLE_PAUSE_TICKS)
                        / (float) IDLE_TRAVEL_TICKS;
                return Mth.lerp(t, -IDLE_SWEEP_YAW_LIMIT,
                        IDLE_SWEEP_YAW_LIMIT);
            }
            int secondPauseEnd = firstEnd + IDLE_PAUSE_TICKS;
            if (phase < secondPauseEnd) {
                return IDLE_SWEEP_YAW_LIMIT;
            }
            int secondEnd = secondPauseEnd + IDLE_TRAVEL_TICKS;
            if (phase < secondEnd) {
                float t = (phase - secondPauseEnd)
                        / (float) IDLE_TRAVEL_TICKS;
                return Mth.lerp(t, IDLE_SWEEP_YAW_LIMIT,
                        -IDLE_SWEEP_YAW_LIMIT);
            }
            return -IDLE_SWEEP_YAW_LIMIT;
        }

        public float visualYaw(float partialTick) {
            return Mth.rotLerp(partialTick, previousVisualYaw, visualYaw);
        }

        public float visualPitch(float partialTick) {
            return Mth.lerp(partialTick, previousVisualPitch, visualPitch);
        }

        public boolean isVisuallyMoving() {
            return visuallyMoving;
        }

        public boolean isControlled() {
            return controlled;
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putBoolean("Controlled", controlled);
            tag.putFloat("TargetYaw", targetYaw);
            tag.putFloat("TargetPitch", targetPitch);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            controlled = tag.getBoolean("Controlled");
            targetYaw = Mth.clamp(tag.getFloat("TargetYaw"),
                    -MANUAL_YAW_LIMIT, MANUAL_YAW_LIMIT);
            targetPitch = Mth.clamp(tag.getFloat("TargetPitch"),
                    MANUAL_MIN_PITCH, MANUAL_MAX_PITCH);
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public ClientboundBlockEntityDataPacket getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public void onDataPacket(Connection connection,
                ClientboundBlockEntityDataPacket packet) {
            CompoundTag tag = packet.getTag();
            if (tag != null) load(tag);
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition).inflate(0.75D);
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            // Yaw and pitch are driven procedurally by the authored bones.
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return animationCache;
        }
    }

    public static final class SurveillanceCameraItem extends BlockItem
            implements GeoItem {
        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);

        private SurveillanceCameraItem(Block block) {
            super(block, new Item.Properties());
            SingletonGeoAnimatable.registerSyncedAnimatable(this);
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.literal("Surveillance Camera");
        }

        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private SurveillanceCameraClient.ItemRenderer renderer;

                @Override
                public @NotNull net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
                        getCustomRenderer() {
                    if (renderer == null) {
                        renderer = new SurveillanceCameraClient.ItemRenderer();
                    }
                    return renderer;
                }
            });
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal(
                    "Functional Equipment - Surveillance Network")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(
                    "Automatically associates with mapped facility rooms.")
                    .withStyle(ChatFormatting.DARK_GRAY));
            super.appendHoverText(stack, level, tooltip, flag);
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            // Inventory representation uses the static authored pose.
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
