package com.bl4ues.scpclassifieddirective.facility.surveillance;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.CeilingCameraClient;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079ProcessingManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079RoomInteractionPolicy;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
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
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/** Ceiling-mounted dome surveillance camera sharing SCP-079's CCTV backend. */
public final class CeilingCameraModule {
    public static final String PATH = "ceiling_camera";

    /** North is the authored forward axis; the dome can rotate a full circle. */
    public static final float BASE_YAW = Direction.NORTH.toYRot();
    public static final float MANUAL_YAW_LIMIT = 180.0F;
    /** Vanilla pitch convention: negative is up, positive is down. */
    public static final float MANUAL_MIN_PITCH = -50.0F;
    public static final float MANUAL_MAX_PITCH = 90.0F;

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<CeilingCameraBlock> BLOCK =
            BLOCKS.register(PATH, CeilingCameraBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register(PATH,
            () -> new CeilingCameraItem(BLOCK.get()));
    public static final RegistryObject<BlockEntityType<CeilingCameraBlockEntity>>
            BLOCK_ENTITY = BLOCK_ENTITIES.register(PATH, () ->
                    BlockEntityType.Builder.of(CeilingCameraBlockEntity::new,
                            BLOCK.get()).build(null));

    private CeilingCameraModule() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    public static Vec3 eyePosition(BlockPos pos, BlockState state) {
        return CeilingCameraViewGeometry.baseEye(pos);
    }

    public static boolean isCamera(BlockState state) {
        return state != null && state.is(BLOCK.get());
    }

    public static final class CeilingCameraBlock extends BaseEntityBlock {
        // The dome occupies only the top few pixels of its block space. Keep the
        // selection slightly generous without creating a dangling full block.
        private static final VoxelShape SHAPE = Block.box(
                5.0D, 13.0D, 5.0D, 11.0D, 16.0D, 11.0D);

        private CeilingCameraBlock() {
            super(BlockBehaviour.Properties.of()
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CeilingCameraBlockEntity(pos, state);
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
                            CeilingCameraBlockEntity::clientTick)
                    : createTickerHelper(type, BLOCK_ENTITY.get(),
                            CeilingCameraBlockEntity::serverTick);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            // It is a ceiling camera in the literal sense, not a human exercise
            // in finding increasingly inventive walls to stick it to.
            if (context.getClickedFace() != Direction.DOWN) return null;
            BlockState state = defaultBlockState();
            return state.canSurvive(context.getLevel(), context.getClickedPos())
                    ? state : null;
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level,
                BlockPos pos) {
            BlockPos supportPos = pos.above();
            return level.getBlockState(supportPos).isFaceSturdy(level,
                    supportPos, Direction.DOWN);
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighborState, LevelAccessor level, BlockPos pos,
                BlockPos neighborPos) {
            if (direction == Direction.UP && !state.canSurvive(level, pos)) {
                return Blocks.AIR.defaultBlockState();
            }
            return super.updateShape(state, direction, neighborState, level,
                    pos, neighborPos);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
                BlockPos pos) {
            return Shapes.empty();
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
                        SurveillanceCameraPlaceholderModule.cameraId(server, pos));
            }
            super.onRemove(state, level, pos, newState, moving);
        }

        private static void registerCamera(ServerLevel level, BlockPos pos,
                BlockState state) {
            String name = "Ceiling Camera " + pos.getX() + ", " + pos.getY()
                    + ", " + pos.getZ();
            FacilitySurveillanceRegistry.register(level,
                    SurveillanceCameraPlaceholderModule.cameraId(level, pos),
                    pos, eyePosition(pos, state), name,
                    BASE_YAW, CeilingCameraViewGeometry.DEFAULT_DOWN_PITCH,
                    MANUAL_YAW_LIMIT, MANUAL_MIN_PITCH,
                    MANUAL_MAX_PITCH, 2.5F);
        }
    }

    public static final class CeilingCameraBlockEntity extends BlockEntity
            implements GeoBlockEntity {
        private static final int IDLE_CYCLE_TICKS = 180;
        private static final float MANUAL_YAW_SPEED = 6.0F;
        private static final float MANUAL_PITCH_SPEED = 4.5F;
        private static final float IDLE_YAW_SPEED = 1.35F;
        private static final float IDLE_PITCH_SPEED = 0.95F;
        private static final float IDLE_MIN_PITCH = 24.0F;
        private static final float IDLE_MAX_PITCH = 82.0F;
        private static final float TRACKING_SYNC_EPSILON = 0.35F;
        private static final long YAW_SALT = 0x9E3779B97F4A7C15L;
        private static final long PITCH_SALT = 0xD1B54A32D192ED03L;
        private static final Map<ServerLevel, RoomSnapshotCache> ROOM_SNAPSHOT_CACHE =
                new WeakHashMap<>();

        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);

        private boolean controlled;
        private float targetYaw;
        private float targetPitch = CeilingCameraViewGeometry.DEFAULT_DOWN_PITCH;

        private float previousVisualYaw;
        private float visualYaw;
        private float previousVisualPitch =
                CeilingCameraViewGeometry.DEFAULT_DOWN_PITCH;
        private float visualPitch = CeilingCameraViewGeometry.DEFAULT_DOWN_PITCH;
        private boolean visualInitialized;
        private boolean visuallyMoving;

        public CeilingCameraBlockEntity(BlockPos pos, BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state);
        }

        private static void serverTick(Level level, BlockPos pos,
                BlockState state, CeilingCameraBlockEntity camera) {
            if (!(level instanceof ServerLevel server)) return;
            ServerPlayer controller = Scp079PlayableManager.controller(
                    server.getServer());
            boolean controllerHere = controller != null
                    && controller.level().dimension().equals(server.dimension());
            boolean autonomousAccess = Scp079ProcessingManager.isActive(server)
                    && Scp079FacilityAccessManager.hasFacilityAccess(server);
            boolean autonomousWatching = controller == null && autonomousAccess;
            boolean needsDefinition = controllerHere || autonomousWatching;
            FacilityCameraDefinition definition = needsDefinition
                    ? FacilitySurveillanceRegistry.camera(server,
                            SurveillanceCameraPlaceholderModule.cameraId(server, pos))
                    : null;

            boolean operatorControl = false;
            boolean directed = false;
            float wantedYaw = camera.targetYaw;
            float wantedPitch = camera.targetPitch;

            if (controllerHere && definition != null
                    && Scp079PlayableManager.isCameraMode(controller)
                    && controller.position().distanceToSqr(
                            definition.eyePosition()) <= 0.36D) {
                operatorControl = true;
                directed = true;
                wantedYaw = Mth.clamp(Mth.wrapDegrees(
                                controller.getYRot() - BASE_YAW),
                        -MANUAL_YAW_LIMIT, MANUAL_YAW_LIMIT);
                wantedPitch = Mth.clamp(controller.getXRot(),
                        MANUAL_MIN_PITCH, MANUAL_MAX_PITCH);
            } else if (autonomousWatching && definition != null) {
                ServerPlayer target = trackingTarget(server, definition, null);
                if (target != null) {
                    directed = true;
                    Vec3 delta = target.getEyePosition()
                            .subtract(definition.eyePosition());
                    double horizontal = Math.sqrt(delta.x * delta.x
                            + delta.z * delta.z);
                    float worldYaw = (float) Math.toDegrees(
                            Math.atan2(-delta.x, delta.z));
                    float worldPitch = (float) -Math.toDegrees(
                            Math.atan2(delta.y, horizontal));
                    wantedYaw = Mth.clamp(Mth.wrapDegrees(
                                    worldYaw - BASE_YAW),
                            -MANUAL_YAW_LIMIT, MANUAL_YAW_LIMIT);
                    wantedPitch = Mth.clamp(worldPitch,
                            MANUAL_MIN_PITCH, MANUAL_MAX_PITCH);
                }
            }

            boolean changed = directed != camera.controlled;
            if (directed) {
                float epsilon = operatorControl ? 0.08F : TRACKING_SYNC_EPSILON;
                changed |= Math.abs(Mth.wrapDegrees(
                        wantedYaw - camera.targetYaw)) > epsilon;
                changed |= Math.abs(wantedPitch - camera.targetPitch) > epsilon;
            }
            if (!changed) return;

            camera.controlled = directed;
            if (directed) {
                camera.targetYaw = wantedYaw;
                camera.targetPitch = wantedPitch;
            }
            camera.setChanged();
            server.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        private static ServerPlayer trackingTarget(ServerLevel level,
                FacilityCameraDefinition camera, @Nullable ServerPlayer controller) {
            List<FacilityRoomSnapshot> rooms = roomSnapshots(level);
            FacilityRoomSnapshot cameraRoom = roomForCamera(rooms, camera);
            if (cameraRoom == null) return null;

            ServerPlayer closest = null;
            double closestDistance = Double.MAX_VALUE;
            for (ServerPlayer player : level.players()) {
                if (player == controller || !player.isAlive()
                        || player.isSpectator()
                        || !cameraRoom.containsColumn(player.blockPosition())) {
                    continue;
                }
                double distance = player.position().distanceToSqr(
                        camera.eyePosition());
                if (distance < closestDistance) {
                    closest = player;
                    closestDistance = distance;
                }
            }
            return closest;
        }

        private static List<FacilityRoomSnapshot> roomSnapshots(ServerLevel level) {
            long tick = level.getGameTime();
            RoomSnapshotCache cached = ROOM_SNAPSHOT_CACHE.get(level);
            if (cached != null && cached.tick == tick) return cached.rooms;
            List<FacilityRoomSnapshot> rooms = FacilityMappingManager
                    .roomSnapshots(level);
            ROOM_SNAPSHOT_CACHE.put(level, new RoomSnapshotCache(tick, rooms));
            return rooms;
        }

        private static FacilityRoomSnapshot roomForCamera(
                List<FacilityRoomSnapshot> rooms,
                FacilityCameraDefinition camera) {
            BlockPos eye = BlockPos.containing(camera.eyePosition());
            for (FacilityRoomSnapshot room : rooms) {
                if (room.containsColumn(eye)) return room;
            }
            for (FacilityRoomSnapshot room : rooms) {
                if (Scp079RoomInteractionPolicy.withinExpandedFloor(room,
                        camera.anchorPos(), 1)) return room;
            }
            return null;
        }

        private static void clientTick(Level level, BlockPos pos,
                BlockState state, CeilingCameraBlockEntity camera) {
            float wantedYaw = camera.controlled
                    ? camera.targetYaw : idleYaw(level.getGameTime(), pos);
            float wantedPitch = camera.controlled
                    ? camera.targetPitch : idlePitch(level.getGameTime(), pos);

            if (!camera.visualInitialized) {
                camera.visualInitialized = true;
                camera.visualYaw = 0.0F;
                camera.previousVisualYaw = 0.0F;
                camera.visualPitch = CeilingCameraViewGeometry.DEFAULT_DOWN_PITCH;
                camera.previousVisualPitch = camera.visualPitch;
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
            long seed = idleSeed(gameTime, pos) ^ YAW_SALT;
            return -180.0F + randomUnit(seed) * 360.0F;
        }

        private static float idlePitch(long gameTime, BlockPos pos) {
            long seed = idleSeed(gameTime, pos) ^ PITCH_SALT;
            return Mth.lerp(randomUnit(seed), IDLE_MIN_PITCH, IDLE_MAX_PITCH);
        }

        private static long idleSeed(long gameTime, BlockPos pos) {
            long positionSeed = mix64(pos.asLong());
            int offset = (int) Math.floorMod(positionSeed, IDLE_CYCLE_TICKS);
            long sequence = Math.floorDiv(gameTime + offset, IDLE_CYCLE_TICKS);
            return mix64(pos.asLong() ^ sequence * YAW_SALT);
        }

        private static float randomUnit(long seed) {
            long mixed = mix64(seed);
            return (float) ((mixed >>> 11) * 0x1.0p-53);
        }

        private static long mix64(long value) {
            value ^= value >>> 33;
            value *= 0xff51afd7ed558ccdL;
            value ^= value >>> 33;
            value *= 0xc4ceb9fe1a85ec53L;
            value ^= value >>> 33;
            return value;
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
            targetPitch = tag.contains("TargetPitch")
                    ? Mth.clamp(tag.getFloat("TargetPitch"),
                            MANUAL_MIN_PITCH, MANUAL_MAX_PITCH)
                    : CeilingCameraViewGeometry.DEFAULT_DOWN_PITCH;
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
            return new AABB(worldPosition).inflate(1.0D);
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            // The dome eye is driven procedurally by the renderer.
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return animationCache;
        }

        private record RoomSnapshotCache(long tick,
                List<FacilityRoomSnapshot> rooms) { }
    }

    public static final class CeilingCameraItem extends BlockItem implements GeoItem {
        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);

        private CeilingCameraItem(Block block) {
            super(block, new Item.Properties());
            SingletonGeoAnimatable.registerSyncedAnimatable(this);
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.literal("Ceiling Camera");
        }

        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private CeilingCameraClient.ItemRenderer renderer;

                @Override
                public @NotNull net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
                        getCustomRenderer() {
                    if (renderer == null) {
                        renderer = new CeilingCameraClient.ItemRenderer();
                    }
                    return renderer;
                }
            });
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal(
                    "Automatically associates with mapped facility rooms.")
                    .withStyle(ChatFormatting.GRAY));
            super.appendHoverText(stack, level, tooltip, flag);
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            // Inventory representation keeps the authored downward-facing eye.
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return animationCache;
        }
    }
}
