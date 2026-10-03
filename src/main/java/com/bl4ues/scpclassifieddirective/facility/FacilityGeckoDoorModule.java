package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.FacilityGeckoDoorClient;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
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
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * State-driven GeckoLib replacement for the old MCreator-era manual doors.
 *
 * <p>Each door is one block for its entire lifetime. OPENING/CLOSING are block
 * states, not registry IDs, so animation, persistence, transformed construction
 * and facility mapping all observe one stable object.</p>
 */
public final class FacilityGeckoDoorModule {
    public static final DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Phase> STAGE =
            EnumProperty.create("stage", Phase.class);

    public static final int OPENING_TICKS = 30;
    public static final int CLOSING_TICKS = 21;
    public static final double INTERACTION_RANGE = 2.0D;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<Block> FACILITY_DOOR =
            BLOCKS.register("facility_door",
                    () -> new DoorBlock(Family.FACILITY));
    public static final RegistryObject<Block> LOGISTICS_DOOR_LEFT =
            BLOCKS.register("logistics_door_left",
                    () -> new DoorBlock(Family.LOGISTICS_LEFT));
    public static final RegistryObject<Block> LOGISTICS_DOOR_RIGHT =
            BLOCKS.register("logistics_door_right",
                    () -> new DoorBlock(Family.LOGISTICS_RIGHT));
    public static final RegistryObject<Block> OFFICE_DOOR =
            BLOCKS.register("office_door",
                    () -> new DoorBlock(Family.OFFICE));
    public static final RegistryObject<Block> BATHROOM_DOOR =
            BLOCKS.register("bathroom_door",
                    () -> new DoorBlock(Family.BATHROOM));
    public static final RegistryObject<Block> WORKSHOP_DOOR =
            BLOCKS.register("workshop_door",
                    () -> new DoorBlock(Family.WORKSHOP));

    public static final RegistryObject<Item> FACILITY_DOOR_ITEM =
            ITEMS.register("facility_door",
                    () -> new DoorItem(FACILITY_DOOR.get(), Family.FACILITY));
    public static final RegistryObject<Item> LOGISTICS_DOOR_ITEM =
            ITEMS.register("logistics_door",
                    () -> new LogisticsDoorItem(LOGISTICS_DOOR_LEFT.get()));
    public static final RegistryObject<Item> OFFICE_DOOR_ITEM =
            ITEMS.register("office_door",
                    () -> new DoorItem(OFFICE_DOOR.get(), Family.OFFICE));
    public static final RegistryObject<Item> BATHROOM_DOOR_ITEM =
            ITEMS.register("bathroom_door",
                    () -> new DoorItem(BATHROOM_DOOR.get(), Family.BATHROOM));
    public static final RegistryObject<Item> WORKSHOP_DOOR_ITEM =
            ITEMS.register("workshop_door",
                    () -> new DoorItem(WORKSHOP_DOOR.get(), Family.WORKSHOP));

    public static final RegistryObject<BlockEntityType<DoorBlockEntity>>
            BLOCK_ENTITY = BLOCK_ENTITIES.register("facility_gecko_door",
                    () -> BlockEntityType.Builder.of(DoorBlockEntity::new,
                            FACILITY_DOOR.get(), LOGISTICS_DOOR_LEFT.get(),
                            LOGISTICS_DOOR_RIGHT.get(), OFFICE_DOOR.get(),
                            BATHROOM_DOOR.get(), WORKSHOP_DOOR.get())
                            .build(null));

    private FacilityGeckoDoorModule() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    public enum Phase implements StringRepresentable {
        CLOSED("closed"),
        OPENING("opening"),
        OPEN("open"),
        CLOSING("closing");

        private final String serialized;

        Phase(String serialized) {
            this.serialized = serialized;
        }

        @Override
        public String getSerializedName() {
            return serialized;
        }
    }

    public enum Family {
        FACILITY("facility", "normal", "unity_door_open",
                "unity_door_close"),
        LOGISTICS_LEFT("logistics_left", "left_logistics",
                "unity_door_open", "unity_door_close"),
        LOGISTICS_RIGHT("logistics_right", "right_logistics",
                "unity_door_open", "unity_door_close"),
        OFFICE("office", "office", "unity_office_open",
                "unity_office_close"),
        BATHROOM("bathroom", "bathroom", "unity_bath_open",
                "unity_bath_close"),
        WORKSHOP("workshop", "workshop", "unity_door_open",
                "unity_door_close");

        private final String id;
        private final String shapeId;
        private final String openingSound;
        private final String closingSound;

        Family(String id, String shapeId, String openingSound,
                String closingSound) {
            this.id = id;
            this.shapeId = shapeId;
            this.openingSound = openingSound;
            this.closingSound = closingSound;
        }

        public String id() {
            return id;
        }

        public String shapeId() {
            return shapeId;
        }

        private String sound(boolean opening) {
            return opening ? openingSound : closingSound;
        }
    }

    @Nullable
    public static Family family(BlockState state) {
        return state != null && state.getBlock() instanceof DoorBlock door
                ? door.family : null;
    }

    public static boolean isDoor(BlockState state) {
        return family(state) != null;
    }

    public static boolean isDoorBlock(Block block) {
        return block instanceof DoorBlock;
    }

    public static Phase phase(BlockState state) {
        return isDoor(state) && state.hasProperty(STAGE)
                ? state.getValue(STAGE) : Phase.CLOSED;
    }

    public static boolean isPassable(BlockState state) {
        // Preserve the old manual-door collision contract: the doorway only
        // becomes passable once the opening animation has fully completed,
        // and becomes solid again as soon as closing begins.
        return isDoor(state) && phase(state) == Phase.OPEN;
    }

    public static boolean isInteractable(BlockState state) {
        Phase phase = phase(state);
        return isDoor(state)
                && (phase == Phase.CLOSED || phase == Phase.OPEN);
    }

    public static boolean isWindowed(BlockState state) {
        Family family = family(state);
        return family == Family.FACILITY || family == Family.OFFICE;
    }

    public static int transitionTicks(boolean opening) {
        return opening ? OPENING_TICKS : CLOSING_TICKS;
    }

    public static BlockState beginTransition(BlockState state,
            boolean opening) {
        if (!isInteractable(state)) return state;
        Phase wanted = opening ? Phase.OPENING : Phase.CLOSING;
        Phase current = phase(state);
        if (opening && current != Phase.CLOSED) return state;
        if (!opening && current != Phase.OPEN) return state;
        return state.setValue(STAGE, wanted);
    }

    public static BlockState finishTransition(BlockState state,
            boolean opening) {
        if (!isDoor(state)) return state;
        return state.setValue(STAGE, opening ? Phase.OPEN : Phase.CLOSED);
    }

    public static void playTransitionSound(ServerLevel level, Vec3 position,
            Family family, boolean opening) {
        if (level == null || position == null || family == null) return;
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(
                new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                        family.sound(opening)));
        if (sound != null) {
            level.playSound(null, position.x, position.y, position.z, sound,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /**
     * Server-authoritative Context Interaction path. The client has already
     * selected the physical handle, so do not synthesize a vanilla hit against
     * an animated handle that may currently sit outside its owning block.
     */
    public static InteractionResult handleContextInteraction(ServerLevel level,
            BlockPos pos, BlockState state) {
        return handleContextInteraction(level, pos, state, false);
    }

    public static InteractionResult handleContextInteraction(ServerLevel level,
            BlockPos pos, BlockState state, boolean singleDoorOnly) {
        if (level == null || pos == null || state == null
                || !(state.getBlock() instanceof DoorBlock door)) {
            return InteractionResult.PASS;
        }
        return door.trigger(level, pos, state, singleDoorOnly);
    }

    public static Item itemFor(Family family) {
        return switch (family) {
            case FACILITY -> FACILITY_DOOR_ITEM.get();
            case LOGISTICS_LEFT, LOGISTICS_RIGHT -> LOGISTICS_DOOR_ITEM.get();
            case OFFICE -> OFFICE_DOOR_ITEM.get();
            case BATHROOM -> BATHROOM_DOOR_ITEM.get();
            case WORKSHOP -> WORKSHOP_DOOR_ITEM.get();
        };
    }

    public static final class DoorBlock extends BaseEntityBlock {
        private final Family family;

        private DoorBlock(Family family) {
            super(BlockBehaviour.Properties.of()
                    .sound(SoundType.WOOD)
                    .strength(1.0F, 10.0F)
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false));
            this.family = family;
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(STAGE, Phase.CLOSED));
        }

        public Family family() {
            return family;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new DoorBlockEntity(pos, state);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, STAGE);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState()
                    .setValue(FACING,
                            context.getHorizontalDirection().getOpposite())
                    .setValue(STAGE, Phase.CLOSED);
        }

        @Override
        public InteractionResult use(BlockState state, Level level,
                BlockPos pos, Player player, InteractionHand hand,
                BlockHitResult hit) {
            if (!isInteractable(state)) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (!(level instanceof ServerLevel server)) {
                return InteractionResult.PASS;
            }
            return trigger(server, pos, state,
                    player.isShiftKeyDown());
        }

        private InteractionResult trigger(ServerLevel level, BlockPos pos,
                BlockState state, boolean singleDoorOnly) {
            if (!isInteractable(state)) return InteractionResult.PASS;

            boolean opening = phase(state) == Phase.CLOSED;
            LinkedDoor linked = !singleDoorOnly
                    && ScpClassifiedDirectiveModulesConfig.get()
                            .doubleDoors.enabled
                    ? matchingLogisticsDoor(level, pos, state)
                    : null;

            if (!startTransition(level, pos, state, opening)) {
                return InteractionResult.CONSUME;
            }
            if (linked != null) {
                linked.door().startTransition(level, linked.pos(),
                        linked.state(), opening);
            }
            return InteractionResult.CONSUME;
        }

        private boolean startTransition(ServerLevel level, BlockPos pos,
                BlockState state, boolean opening) {
            BlockState next = beginTransition(state, opening);
            if (next == state) return false;

            level.setBlock(pos, next, Block.UPDATE_ALL);
            Scp079ActivityPingManager.emitDoorAt(level,
                    Vec3.atCenterOf(pos));
            playTransitionSound(level, Vec3.atCenterOf(pos), family,
                    opening);
            level.scheduleTick(pos, this, transitionTicks(opening));
            return true;
        }

        @Nullable
        private LinkedDoor matchingLogisticsDoor(ServerLevel level,
                BlockPos pos, BlockState state) {
            Family currentFamily = FacilityGeckoDoorModule.family(state);
            if (currentFamily != Family.LOGISTICS_LEFT
                    && currentFamily != Family.LOGISTICS_RIGHT) {
                return null;
            }

            Direction facing = state.getValue(FACING);
            Direction towardPartner = currentFamily == Family.LOGISTICS_LEFT
                    ? facing.getCounterClockWise()
                    : facing.getClockWise();
            BlockPos partnerPos = pos.relative(towardPartner);
            BlockState partnerState = level.getBlockState(partnerPos);
            Family expected = currentFamily == Family.LOGISTICS_LEFT
                    ? Family.LOGISTICS_RIGHT : Family.LOGISTICS_LEFT;

            if (FacilityGeckoDoorModule.family(partnerState) != expected
                    || !partnerState.hasProperty(FACING)
                    || partnerState.getValue(FACING) != facing
                    || phase(partnerState) != phase(state)
                    || !isInteractable(partnerState)
                    || !(partnerState.getBlock()
                    instanceof DoorBlock partnerDoor)) {
                return null;
            }
            return new LinkedDoor(partnerPos, partnerState, partnerDoor);
        }

        @Override
        public void tick(BlockState state, ServerLevel level, BlockPos pos,
                RandomSource random) {
            Phase phase = phase(state);
            if (phase != Phase.OPENING && phase != Phase.CLOSING) return;
            boolean opening = phase == Phase.OPENING;
            level.setBlock(pos, finishTransition(state, opening),
                    Block.UPDATE_ALL);
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (level instanceof ServerLevel server
                    && !(oldState.getBlock() instanceof DoorBlock)) {
                Scp079FacilityAccessManager.registerDoor(server, pos);
            }
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (level instanceof ServerLevel server
                    && !(newState.getBlock() instanceof DoorBlock)) {
                Scp079FacilityAccessManager.unregisterDoor(server, pos);
            }
            super.onRemove(state, level, pos, newState, moving);
        }

        private VoxelShape physicalShape(BlockState state) {
            return FacilityDoorShapes.shape(family.shapeId(),
                    isPassable(state), state.getValue(FACING));
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            // The handle is a Context Interaction highlight, not a fake
            // selection box. Selection follows only the real door body/frame.
            return physicalShape(state);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state,
                BlockGetter level, BlockPos pos, CollisionContext context) {
            return physicalShape(state);
        }

        @Override
        public VoxelShape getOcclusionShape(BlockState state,
                BlockGetter level, BlockPos pos) {
            return Shapes.empty();
        }

        @Override
        public boolean propagatesSkylightDown(BlockState state,
                BlockGetter level, BlockPos pos) {
            return true;
        }

        @Override
        public int getLightBlock(BlockState state, BlockGetter level,
                BlockPos pos) {
            return 0;
        }

        @Override
        public boolean isPathfindable(BlockState state, BlockGetter level,
                BlockPos pos, PathComputationType type) {
            return isPassable(state);
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
            return Collections.singletonList(new ItemStack(itemFor(family)));
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target,
                BlockGetter level, BlockPos pos, Player player) {
            return new ItemStack(itemFor(family));
        }
    }

    private record LinkedDoor(BlockPos pos, BlockState state,
            DoorBlock door) {
    }

    public static final class DoorBlockEntity extends BlockEntity
            implements GeoBlockEntity {
        private static final RawAnimation CLOSED =
                RawAnimation.begin().thenLoop("closed");
        private static final RawAnimation OPENING =
                RawAnimation.begin().thenPlay("opening");
        private static final RawAnimation OPEN =
                RawAnimation.begin().thenLoop("open");
        private static final RawAnimation CLOSING =
                RawAnimation.begin().thenPlay("closing");

        private final AnimatableInstanceCache cache =
                GeckoLibUtil.createInstanceCache(this);

        public DoorBlockEntity(BlockPos pos, BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state);
        }

        public Family family() {
            Family family = FacilityGeckoDoorModule.family(getBlockState());
            return family == null ? Family.FACILITY : family;
        }

        public Phase phase() {
            return FacilityGeckoDoorModule.phase(getBlockState());
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, "door", 0,
                    state -> state.setAndContinue(switch (phase()) {
                        case CLOSED -> CLOSED;
                        case OPENING -> OPENING;
                        case OPEN -> OPEN;
                        case CLOSING -> CLOSING;
                    })));
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition).inflate(2.0D, 1.5D, 2.0D);
        }
    }

    public static class DoorItem extends BlockItem implements GeoItem {
        private final Family family;
        private final AnimatableInstanceCache cache =
                GeckoLibUtil.createInstanceCache(this);

        protected DoorItem(Block block, Family family) {
            super(block, new Item.Properties());
            this.family = family;
        }

        public Family family() {
            return family;
        }

        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private FacilityGeckoDoorClient.ItemRenderer renderer;

                @Override
                public @NotNull net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
                        getCustomRenderer() {
                    if (renderer == null) {
                        renderer = new FacilityGeckoDoorClient.ItemRenderer();
                    }
                    return renderer;
                }
            });
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            // Inventory/hand representation is always the closed pose.
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }
    }

    private static final class LogisticsDoorItem extends DoorItem {
        private LogisticsDoorItem(Block block) {
            super(block, Family.LOGISTICS_LEFT);
        }

        @Nullable
        @Override
        protected BlockState getPlacementState(BlockPlaceContext context) {
            // Choose the authored Left/Right variant from the player's view,
            // exactly like choosing which side of a vanilla doorway owns the
            // hinge. The block itself still computes its normal facing from
            // the placement context afterwards.
            Direction view = context.getHorizontalDirection();
            Vec3 hit = context.getClickLocation();
            BlockPos pos = context.getClickedPos();
            double dx = hit.x - (pos.getX() + 0.5D);
            double dz = hit.z - (pos.getZ() + 0.5D);
            Direction screenRight = view.getClockWise();
            double side = dx * screenRight.getStepX()
                    + dz * screenRight.getStepZ();
            DoorBlock selected = (DoorBlock) (side < 0.0D
                    ? LOGISTICS_DOOR_LEFT.get()
                    : LOGISTICS_DOOR_RIGHT.get());
            return selected.getStateForPlacement(context);
        }
    }
}
