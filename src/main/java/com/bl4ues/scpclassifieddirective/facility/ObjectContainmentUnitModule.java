package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.ObjectContainmentUnitClient;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.init.UnifiedReaderItems;
import com.bl4ues.scpclassifieddirective.integration.PlayerItemAccess;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderInteractionEvents;
import com.bl4ues.scpclassifieddirective.network.ScpEntityNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Functional two-block containment pedestal for small block-form SCPs.
 *
 * <p>The lower block owns the model and state. While the case is closed and
 * empty, an invisible reservation block occupies the cell above so ordinary
 * placement cannot sneak through the glass. If a block is inside when the lid
 * closes, the real block occupies that cell instead and protection is enforced
 * by interaction/break hooks until the lid has fully opened again.</p>
 */
public final class ObjectContainmentUnitModule {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final int DEFAULT_ACCESS_LEVEL = 1;
    private static final int TRANSITION_TICKS = 25;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(
            ForgeRegistries.SOUND_EVENTS, ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<Block> UNIT = BLOCKS.register(
            "object_containment_unit", UnitBlock::new);
    public static final RegistryObject<Block> RESERVATION = BLOCKS.register(
            "object_containment_unit_reservation", ReservationBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register(
            "object_containment_unit", () -> new UnitItem(UNIT.get()));
    public static final RegistryObject<BlockEntityType<UnitBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register("object_containment_unit", () ->
                    BlockEntityType.Builder.of(UnitBlockEntity::new, UNIT.get())
                            .build(null));

    public static final RegistryObject<SoundEvent> OPEN_SOUND = sound(
            "object_containment_unit_open");
    public static final RegistryObject<SoundEvent> CLOSE_SOUND = sound(
            "object_containment_unit_close");

    private ObjectContainmentUnitModule() {
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

    /** Returns the securing unit immediately below a contained block position. */
    @Nullable
    public static UnitBlockEntity securingUnit(LevelAccessor level,
            BlockPos containedPos) {
        if (level == null || containedPos == null) return null;
        BlockPos master = containedPos.below();
        if (!level.getBlockState(master).is(UNIT.get())) return null;
        BlockEntity blockEntity = level.getBlockEntity(master);
        if (blockEntity instanceof UnitBlockEntity unit && unit.isSecured()) {
            return unit;
        }
        return null;
    }

    public static boolean isProtectedContent(LevelAccessor level,
            BlockPos pos) {
        return securingUnit(level, pos) != null;
    }

    public static final class UnitBlock extends BaseEntityBlock {
        // Authored lower-body footprint plus the closed glass case. The upper
        // part intentionally exceeds 16 px so ray picking hits the case itself
        // rather than the protected block behind it.
        private static final VoxelShape BODY_NORTH = Shapes.or(
                box(5.0D, 0.0D, 5.0D, 11.0D, 4.0D, 11.0D),
                box(6.0D, 4.0D, 6.0D, 10.0D, 12.0D, 10.0D),
                box(5.0D, 12.0D, 4.0D, 11.0D, 15.0D, 12.0D),
                box(1.0D, 15.0D, 1.0D, 15.0D, 16.0D, 15.0D),
                box(15.5D, 12.5D, 5.5D, 20.0D, 15.0D, 11.0D))
                .optimize();
        private static final VoxelShape CLOSED_NORTH = Shapes.or(
                BODY_NORTH, box(1.0D, 16.0D, 1.0D, 15.0D, 25.75D, 15.0D))
                .optimize();

        public UnitBlock() {
            super(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(3.0F, 1200.0F)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any().setValue(FACING,
                    Direction.NORTH));
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new UnitBlockEntity(pos, state);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                BlockState state, BlockEntityType<T> type) {
            return level.isClientSide ? null : createTickerHelper(type,
                    BLOCK_ENTITY.get(), UnitBlockEntity::serverTick);
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockPos pos = context.getClickedPos();
            BlockPos upper = pos.above();
            if (!context.getLevel().getWorldBorder().isWithinBounds(upper)
                    || !context.getLevel().getBlockState(upper)
                    .canBeReplaced(context)) {
                return null;
            }
            return defaultBlockState().setValue(FACING,
                    context.getHorizontalDirection().getOpposite());
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (level.isClientSide || oldState.is(this)) return;
            placeReservationIfEmpty(level, pos);
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (!state.is(newState.getBlock()) && !level.isClientSide) {
                BlockPos upper = pos.above();
                if (level.getBlockState(upper).is(RESERVATION.get())) {
                    level.removeBlock(upper, false);
                }
            }
            super.onRemove(state, level, pos, newState, moving);
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (!KeycardReaderInteractionEvents.screwdriver(player).isEmpty()) {
                if (level.isClientSide) return InteractionResult.SUCCESS;
                if (player instanceof ServerPlayer serverPlayer
                        && level.getBlockEntity(pos) instanceof UnitBlockEntity unit) {
                    ScpEntityNetwork.openKeycardReaderScreen(serverPlayer, pos,
                            unit.requiredLevel());
                    KeycardReaderInteractionEvents.suppressNextInteraction(
                            serverPlayer, pos);
                    return InteractionResult.CONSUME;
                }
            }

            if (!(level.getBlockEntity(pos) instanceof UnitBlockEntity unit)) {
                return InteractionResult.PASS;
            }
            return unit.handleUse(player);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            boolean accessible = level.getBlockEntity(pos)
                    instanceof UnitBlockEntity unit && unit.isOpenForAccess();
            VoxelShape north = accessible ? BODY_NORTH : CLOSED_NORTH;
            return rotateNorthShape(north, state.getValue(FACING));
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
        public boolean propagatesSkylightDown(BlockState state, BlockGetter level,
                BlockPos pos) {
            return true;
        }

        @Override
        public int getLightBlock(BlockState state, BlockGetter level,
                BlockPos pos) {
            return 0;
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
            return Collections.singletonList(new ItemStack(ITEM.get()));
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target,
                BlockGetter level, BlockPos pos, Player player) {
            return new ItemStack(ITEM.get());
        }
    }

    public static final class ReservationBlock extends Block {
        private static final VoxelShape SHAPE = box(1.0D, 0.0D, 1.0D,
                15.0D, 10.0D, 15.0D);

        public ReservationBlock() {
            super(BlockBehaviour.Properties.of()
                    .strength(-1.0F, 3600000.0F)
                    .noOcclusion()
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
                    .isRedstoneConductor((state, level, pos) -> false));
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
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
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighbor, LevelAccessor level, BlockPos pos,
                BlockPos neighborPos) {
            if (direction == Direction.DOWN
                    && !level.getBlockState(pos.below()).is(UNIT.get())) {
                return Blocks.AIR.defaultBlockState();
            }
            return super.updateShape(state, direction, neighbor, level, pos,
                    neighborPos);
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

    public static final class UnitBlockEntity extends BlockEntity
            implements GeoBlockEntity {
        public enum Phase {
            CLOSED,
            OPENING,
            OPEN,
            CLOSING
        }

        private static final RawAnimation CLOSED_ANIMATION =
                RawAnimation.begin().thenLoop("closed");
        private static final RawAnimation OPENING_ANIMATION =
                RawAnimation.begin().thenPlay("opening");
        private static final RawAnimation OPEN_ANIMATION =
                RawAnimation.begin().thenLoop("open");
        private static final RawAnimation CLOSING_ANIMATION =
                RawAnimation.begin().thenPlay("closing");

        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);
        private Phase phase = Phase.CLOSED;
        private int transitionTicks;
        private int requiredLevel = DEFAULT_ACCESS_LEVEL;

        public UnitBlockEntity(BlockPos pos, BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state);
        }

        public Phase phase() {
            return phase;
        }

        public int requiredLevel() {
            return requiredLevel;
        }

        public void setRequiredLevel(int level) {
            int clamped = Math.max(1, Math.min(6, level));
            if (requiredLevel == clamped) return;
            requiredLevel = clamped;
            markUpdated();
        }

        public boolean isOpenForAccess() {
            return phase == Phase.OPEN;
        }

        public boolean isSecured() {
            return phase == Phase.CLOSED || phase == Phase.CLOSING
                    || phase == Phase.OPENING;
        }

        public boolean isTransitioning() {
            return phase == Phase.OPENING || phase == Phase.CLOSING;
        }

        public InteractionResult handleUse(Player player) {
            if (level == null || player == null) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (isTransitioning()) return InteractionResult.CONSUME;

            if (phase == Phase.OPEN) {
                startClosing();
                return InteractionResult.CONSUME;
            }

            int keycardLevel = highestKeycardLevel(player);
            boolean accepted = keycardLevel >= requiredLevel;
            playReaderSound(accepted);
            if (accepted) startOpening();
            return InteractionResult.CONSUME;
        }

        public static void serverTick(Level level, BlockPos pos,
                BlockState state, UnitBlockEntity unit) {
            if (level.isClientSide) return;

            if (unit.phase == Phase.CLOSED) {
                placeReservationIfEmpty(level, pos);
            } else if (unit.phase == Phase.OPEN) {
                removeReservation(level, pos);
            }

            if (!unit.isTransitioning()) return;
            unit.transitionTicks++;
            if (unit.transitionTicks < TRANSITION_TICKS) return;

            if (unit.phase == Phase.OPENING) {
                unit.phase = Phase.OPEN;
                removeReservation(level, pos);
            } else if (unit.phase == Phase.CLOSING) {
                unit.phase = Phase.CLOSED;
                placeReservationIfEmpty(level, pos);
            }
            unit.transitionTicks = 0;
            unit.markUpdated();
        }

        private void startOpening() {
            if (level == null || level.isClientSide) return;
            phase = Phase.OPENING;
            transitionTicks = 0;
            removeReservation(level, worldPosition);
            playUnitSound(OPEN_SOUND.get());
            markUpdated();
        }

        private void startClosing() {
            if (level == null || level.isClientSide) return;
            phase = Phase.CLOSING;
            transitionTicks = 0;
            placeReservationIfEmpty(level, worldPosition);
            playUnitSound(CLOSE_SOUND.get());
            markUpdated();
        }

        private void playReaderSound(boolean accepted) {
            if (level == null || level.isClientSide) return;
            ResourceLocation id = new ResourceLocation(
                    ScpClassifiedDirectiveMod.MODID,
                    accepted ? "accessgranted" : "accessdenied");
            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(id);
            if (sound != null) {
                level.playSound(null, worldPosition, sound,
                        SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
        }

        private void playUnitSound(SoundEvent sound) {
            if (level == null || level.isClientSide || sound == null) return;
            level.playSound(null, worldPosition, sound,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        private void markUpdated() {
            setChanged();
            if (level != null && !level.isClientSide) {
                BlockState state = getBlockState();
                level.sendBlockUpdated(worldPosition, state, state,
                        Block.UPDATE_ALL);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putString("Phase", phase.name().toLowerCase(Locale.ROOT));
            tag.putInt("TransitionTicks", transitionTicks);
            tag.putInt("RequiredLevel", requiredLevel);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            try {
                phase = Phase.valueOf(tag.getString("Phase")
                        .toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {
                phase = Phase.CLOSED;
            }
            transitionTicks = Math.max(0, Math.min(TRANSITION_TICKS - 1,
                    tag.getInt("TransitionTicks")));
            requiredLevel = tag.contains("RequiredLevel")
                    ? Math.max(1, Math.min(6, tag.getInt("RequiredLevel")))
                    : DEFAULT_ACCESS_LEVEL;
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
            return new AABB(worldPosition).inflate(1.5D).expandTowards(0, 1.0D, 0);
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, "containment_unit",
                    0, state -> state.setAndContinue(switch (phase) {
                        case CLOSED -> CLOSED_ANIMATION;
                        case OPENING -> OPENING_ANIMATION;
                        case OPEN -> OPEN_ANIMATION;
                        case CLOSING -> CLOSING_ANIMATION;
                    })));
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return animationCache;
        }
    }

    public static final class UnitItem extends BlockItem implements GeoItem {
        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);

        public UnitItem(Block block) {
            super(block, new Item.Properties().stacksTo(16));
            SingletonGeoAnimatable.registerSyncedAnimatable(this);
        }

        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private ObjectContainmentUnitClient.ItemRenderer renderer;

                @Override
                public @NotNull net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
                        getCustomRenderer() {
                    if (renderer == null) {
                        renderer = new ObjectContainmentUnitClient.ItemRenderer();
                    }
                    return renderer;
                }
            });
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal(
                    "Keycard-secured containment equipment")
                    .withStyle(ChatFormatting.GRAY));
            super.appendHoverText(stack, level, tooltip, flag);
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            // Inventory representation is always the authored closed pose.
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return animationCache;
        }
    }

    private static int highestKeycardLevel(Player player) {
        return PlayerItemAccess.highestLevel(player,
                ObjectContainmentUnitModule::keycardLevel);
    }

    private static int keycardLevel(ItemStack stack) {
        if (stack.is(ScpClassifiedDirectiveModItems.LEVEL_6_KEYCARD.get())) return 6;
        if (stack.is(ScpClassifiedDirectiveModItems.LEVEL_5_KEYCARD.get())) return 5;
        if (stack.is(ScpClassifiedDirectiveModItems.LEVEL_4_KEYCARD.get())) return 4;
        if (stack.is(ScpClassifiedDirectiveModItems.LEVEL_3_KEYCARD.get())) return 3;
        if (stack.is(ScpClassifiedDirectiveModItems.LEVEL_2_KEYCARD.get())) return 2;
        if (stack.is(ScpClassifiedDirectiveModItems.LEVEL_1_KEYCARD.get())) return 1;
        return 0;
    }

    private static void placeReservationIfEmpty(LevelAccessor level,
            BlockPos master) {
        BlockPos upper = master.above();
        BlockState state = level.getBlockState(upper);
        if (state.isAir() || state.canBeReplaced()) {
            level.setBlock(upper, RESERVATION.get().defaultBlockState(),
                    Block.UPDATE_ALL);
        }
    }

    private static void removeReservation(LevelAccessor level,
            BlockPos master) {
        BlockPos upper = master.above();
        if (level.getBlockState(upper).is(RESERVATION.get())) {
            level.removeBlock(upper, false);
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

    @Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ProtectionEvents {
        private ProtectionEvents() {
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onBreak(BlockEvent.BreakEvent event) {
            if (isProtectedContent(event.getLevel(), event.getPos())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
            if (!isProtectedContent(event.getLevel(), event.getPos())) return;
            event.setCanceled(true);
            event.setUseBlock(Event.Result.DENY);
            event.setUseItem(Event.Result.DENY);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
            if (!isProtectedContent(event.getLevel(), event.getPos())) return;
            event.setCanceled(true);
            event.setUseBlock(Event.Result.DENY);
            event.setUseItem(Event.Result.DENY);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }
}
