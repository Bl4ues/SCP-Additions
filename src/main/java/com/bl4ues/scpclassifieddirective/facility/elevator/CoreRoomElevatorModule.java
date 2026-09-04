package com.bl4ues.scpclassifieddirective.facility.elevator;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.UnifiedReaderItems;
import com.bl4ues.scpclassifieddirective.network.ScpEntityNetwork;
import com.bl4ues.scpclassifieddirective.safezone.DiscoveryMusicManager;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Complete registry and block-side implementation for the modular Core Room elevator. */
public final class CoreRoomElevatorModule {
    public static final int STATION_HEIGHT_BLOCKS = 3;
    public static final int MIN_FLOOR_SPACING = 8;
    public static final int MAX_FLOOR_SPACING = 32;
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty GENERATED = BooleanProperty.create("generated");
    private static final ThreadLocal<Boolean> MUTATING_PARTS =
            ThreadLocal.withInitial(() -> false);

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<Block> STATION = BLOCKS.register(
            "core_room_elevator_station", StationBlock::new);
    public static final RegistryObject<Block> PULLEY = BLOCKS.register(
            "core_room_elevator_pulley", PulleyBlock::new);
    public static final RegistryObject<Block> BEAMS = BLOCKS.register(
            "core_room_elevator_beams", BeamBlock::new);
    public static final RegistryObject<Block> FLOOR = BLOCKS.register(
            "core_room_floor", CoreRoomFloorBlock::new);
    public static final RegistryObject<Block> STRUCTURE_PART = BLOCKS.register(
            "core_room_elevator_structure_part", StructurePartBlock::new);
    public static final RegistryObject<Block> BEAM_STRUCTURE_PART = BLOCKS.register(
            "core_room_elevator_beam_part", BeamStructurePartBlock::new);

    public static final RegistryObject<Item> STATION_ITEM = ITEMS.register(
            "core_room_elevator_station", () -> new ElevatorBlockItem(
                    STATION.get(), "tooltip.scp_classified_directive.core_room_elevator_station"));
    public static final RegistryObject<Item> PULLEY_ITEM = ITEMS.register(
            "core_room_elevator_pulley", () -> new PulleyBlockItem(
                    PULLEY.get(), "tooltip.scp_classified_directive.core_room_elevator_pulley"));
    public static final RegistryObject<Item> BEAMS_ITEM = ITEMS.register(
            "core_room_elevator_beams", () -> new ElevatorBlockItem(
                    BEAMS.get(), "tooltip.scp_classified_directive.core_room_elevator_beams"));
    public static final RegistryObject<Item> FLOOR_ITEM = ITEMS.register(
            "core_room_floor", () -> new CoreRoomBlockItem(FLOOR.get()));

    public static final RegistryObject<BlockEntityType<StationBlockEntity>> STATION_BE =
            BLOCK_ENTITIES.register("core_room_elevator_station", () ->
                    BlockEntityType.Builder.of(StationBlockEntity::new,
                            STATION.get()).build(null));
    public static final RegistryObject<BlockEntityType<PulleyBlockEntity>> PULLEY_BE =
            BLOCK_ENTITIES.register("core_room_elevator_pulley", () ->
                    BlockEntityType.Builder.of(PulleyBlockEntity::new,
                            PULLEY.get()).build(null));
    public static final RegistryObject<BlockEntityType<StructurePartBlockEntity>> PART_BE =
            BLOCK_ENTITIES.register("core_room_elevator_structure_part", () ->
                    BlockEntityType.Builder.of(StructurePartBlockEntity::new,
                            STRUCTURE_PART.get(), BEAM_STRUCTURE_PART.get())
                            .build(null));

    public static final RegistryObject<SoundEvent> ELEVATOR_DOOR_CLOSE =
            sound("elevator_door_close");
    public static final RegistryObject<SoundEvent> STATION_CLOSE =
            sound("station_close");
    public static final RegistryObject<SoundEvent> ELEVATOR_MOVING =
            sound("elevator_moving");
    public static final RegistryObject<SoundEvent> ELEVATOR_DOOR_OPEN =
            sound("elevator_door_open");
    public static final RegistryObject<SoundEvent> ELEVATOR_CABIN_LOOP =
            sound("elevator_cabin_loop");
    public static final RegistryObject<SoundEvent> ELEVATOR_BUTTON_PRESS =
            sound("elevator_button_press");
    public static final RegistryObject<SoundEvent> ELEVATOR_BUTTON_ACCEPT =
            sound("elevator_button_accept");
    public static final RegistryObject<SoundEvent> ZONE_SPLASH =
            sound("zone_splash");

    public static final RegistryObject<EntityType<CoreRoomElevatorCarriageEntity>> CARRIAGE =
            ENTITIES.register("core_room_elevator_carriage", () -> EntityType.Builder
                    .<CoreRoomElevatorCarriageEntity>of(
                            CoreRoomElevatorCarriageEntity::new, MobCategory.MISC)
                    .sized(1.75F, 3.35F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("core_room_elevator_carriage"));

    private CoreRoomElevatorModule() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        SOUNDS.register(bus);
    }

    private static RegistryObject<SoundEvent> sound(String path) {
        ResourceLocation id = new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
        return SOUNDS.register(path,
                () -> SoundEvent.createVariableRangeEvent(id));
    }

    public enum StructureKind implements StringRepresentable {
        STATION("station"),
        PULLEY("pulley"),
        BEAMS("beams");

        private final String serialized;

        StructureKind(String serialized) {
            this.serialized = serialized;
        }

        @Override
        public String getSerializedName() {
            return serialized;
        }

        public static StructureKind fromName(String name) {
            if ("pulley".equals(name)) return PULLEY;
            if ("beams".equals(name)) return BEAMS;
            return STATION;
        }
    }

    public enum DoorVisualState {
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }

    public record LocalCell(int x, int y, int z) {
    }

    private static List<LocalCell> stationCells() {
        List<LocalCell> cells = new ArrayList<>();
        for (int y = 0; y < 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -2; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    cells.add(new LocalCell(x, y, z));
                }
            }
        }
        return cells;
    }

    private static List<LocalCell> pulleyCells() {
        List<LocalCell> cells = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                cells.add(new LocalCell(x, 0, z));
            }
        }
        return cells;
    }

    private static List<LocalCell> beamCells() {
        List<LocalCell> cells = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                cells.add(new LocalCell(x, 0, z));
            }
        }
        return cells;
    }

    public static BlockPos rotateOffset(Direction facing, int x, int y, int z) {
        return switch (facing) {
            case SOUTH -> new BlockPos(-x, y, -z);
            case EAST -> new BlockPos(-z, y, x);
            case WEST -> new BlockPos(z, y, -x);
            default -> new BlockPos(x, y, z);
        };
    }

    private static boolean placeParts(ServerLevel level, BlockPos master,
            Direction facing, StructureKind kind, List<LocalCell> cells,
            @Nullable Player placer) {
        Set<BlockPos> replaceableGeneratedBeams = new HashSet<>();
        for (LocalCell cell : cells) {
            BlockPos rotated = rotateOffset(facing, cell.x(), cell.y(), cell.z());
            BlockPos target = master.offset(rotated);
            BlockState existing = level.getBlockState(target);
            if (existing.canBeReplaced()) continue;
            if ((existing.is(STRUCTURE_PART.get())
                    || existing.is(BEAM_STRUCTURE_PART.get()))
                    && level.getBlockEntity(target) instanceof StructurePartBlockEntity part) {
                if (master.equals(part.masterPos()) && kind == part.kind()) {
                    continue;
                }
                BlockState owner = level.getBlockState(part.masterPos());
                if (part.kind() == StructureKind.BEAMS
                        && owner.is(BEAMS.get())
                        && owner.getValue(GENERATED)) {
                    replaceableGeneratedBeams.add(part.masterPos());
                    continue;
                }
            }
            if (placer != null) {
                placer.sendSystemMessage(Component.translatable(
                        "message.scp_classified_directive.elevator_space_blocked")
                        .withStyle(ChatFormatting.RED));
            }
            return false;
        }

        replaceableGeneratedBeams.forEach(pos -> level.removeBlock(pos, false));
        boolean previous = MUTATING_PARTS.get();
        MUTATING_PARTS.set(true);
        try {
            for (LocalCell cell : cells) {
                BlockPos rotated = rotateOffset(facing, cell.x(), cell.y(), cell.z());
                BlockPos target = master.offset(rotated);
                Block partBlock = kind == StructureKind.BEAMS
                        ? BEAM_STRUCTURE_PART.get() : STRUCTURE_PART.get();
                level.setBlock(target, partBlock.defaultBlockState(), 3);
                if (level.getBlockEntity(target) instanceof StructurePartBlockEntity part) {
                    part.configure(master, kind, cell.x(), cell.y(), cell.z());
                }
            }
        } finally {
            MUTATING_PARTS.set(previous);
        }
        return true;
    }

    public static boolean placeBeamParts(ServerLevel level, BlockPos master,
            Direction facing, @Nullable Player placer) {
        return placeParts(level, master, facing, StructureKind.BEAMS,
                beamCells(), placer);
    }

    public static void removeParts(ServerLevel level, BlockPos master,
            Direction facing, StructureKind kind) {
        List<LocalCell> cells = switch (kind) {
            case STATION -> stationCells();
            case PULLEY -> pulleyCells();
            case BEAMS -> beamCells();
        };
        boolean previous = MUTATING_PARTS.get();
        MUTATING_PARTS.set(true);
        try {
            for (LocalCell cell : cells) {
                BlockPos rotated = rotateOffset(facing, cell.x(), cell.y(), cell.z());
                BlockPos target = master.offset(rotated);
                if (level.getBlockEntity(target) instanceof StructurePartBlockEntity part
                        && part.kind() == kind && master.equals(part.masterPos())) {
                    level.removeBlock(target, false);
                }
            }
        } finally {
            MUTATING_PARTS.set(previous);
        }
    }

    static boolean isMutatingParts() {
        return MUTATING_PARTS.get();
    }

    public static class CoreRoomBlockItem extends BlockItem {
        protected CoreRoomBlockItem(Block block) {
            super(block, new Item.Properties());
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(
                    "tooltip.scp_classified_directive.core_room")
                    .withStyle(ChatFormatting.BLUE));
            super.appendHoverText(stack, level, tooltip, flag);
        }
    }

    public static class ElevatorBlockItem extends CoreRoomBlockItem {
        private final String tooltipKey;

        protected ElevatorBlockItem(Block block, String tooltipKey) {
            super(block);
            this.tooltipKey = tooltipKey;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltip, flag);
            tooltip.add(Component.translatable(tooltipKey)
                    .withStyle(ChatFormatting.GRAY));
            if (getBlock() == STATION.get()) {
                tooltip.add(Component.literal(
                        "Use a Screwdriver to edit this floor's arrival display.")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private static final class PulleyBlockItem extends ElevatorBlockItem {
        private PulleyBlockItem(Block block, String tooltipKey) {
            super(block, tooltipKey);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            BlockPlaceContext placement = new BlockPlaceContext(context);
            if (CoreRoomElevatorManager.findPulleyFacing(placement.getLevel(),
                    placement.getClickedPos()) == null) {
                if (!placement.getLevel().isClientSide
                        && placement.getPlayer() != null) {
                    placement.getPlayer().sendSystemMessage(Component.translatable(
                            "message.scp_classified_directive.elevator_place_station_first")
                            .withStyle(ChatFormatting.YELLOW));
                }
                return InteractionResult.FAIL;
            }
            return super.useOn(context);
        }
    }

    public static final class StationBlock extends BaseEntityBlock {
        private StationBlock() {
            super(BlockBehaviour.Properties.of().strength(5.0F, 15.0F)
                    .sound(SoundType.METAL).noOcclusion());
            registerDefaultState(stateDefinition.any().setValue(FACING,
                    Direction.NORTH));
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING,
                    context.getHorizontalDirection().getOpposite());
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                @Nullable LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (!(level instanceof ServerLevel serverLevel)) return;
            Player player = placer instanceof Player found ? found : null;
            Direction facing = state.getValue(FACING);

            BlockPos snapped = CoreRoomElevatorManager.findStationSnap(
                    serverLevel, pos, facing);
            if (!snapped.equals(pos)) {
                BlockState occupied = serverLevel.getBlockState(snapped);
                if (occupied.is(BEAMS.get()) && occupied.getValue(GENERATED)) {
                    serverLevel.removeBlock(snapped, false);
                } else if (!occupied.canBeReplaced()) {
                    if (player != null) {
                        player.sendSystemMessage(Component.translatable(
                                "message.scp_classified_directive.elevator_space_blocked")
                                .withStyle(ChatFormatting.RED));
                    }
                    serverLevel.destroyBlock(pos, true, placer);
                    return;
                }
                serverLevel.removeBlock(pos, false);
                serverLevel.setBlock(snapped, state, Block.UPDATE_ALL);
                pos = snapped;
            }

            if (!CoreRoomElevatorManager.isValidStationPlacement(
                    serverLevel, pos, facing)) {
                if (player != null) {
                    player.sendSystemMessage(Component.translatable(
                            "message.scp_classified_directive.elevator_floor_spacing",
                            MIN_FLOOR_SPACING, MAX_FLOOR_SPACING)
                            .withStyle(ChatFormatting.RED));
                }
                serverLevel.destroyBlock(pos, true, placer);
                return;
            }
            if (!placeParts(serverLevel, pos, facing, StructureKind.STATION,
                    stationCells(), player)) {
                serverLevel.destroyBlock(pos, true, placer);
                return;
            }
            CoreRoomElevatorManager.rebuildColumn(serverLevel, pos, player);
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
                Direction facing = state.getValue(FACING);
                removeParts(serverLevel, pos, facing, StructureKind.STATION);
                ScpClassifiedDirectiveMod.queueServerWork(1,
                        () -> CoreRoomElevatorManager.rebuildColumn(
                                serverLevel, pos, null));
            }
            super.onRemove(state, level, pos, newState, moving);
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (player.getItemInHand(hand).is(
                    UnifiedReaderItems.SCREWDRIVER.get())) {
                if (!level.isClientSide
                        && player instanceof ServerPlayer serverPlayer
                        && level.getBlockEntity(pos)
                        instanceof StationBlockEntity station) {
                    ScpEntityNetwork.openElevatorArrivalEditor(
                            serverPlayer, pos, station.arrivalDisplay());
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            Vec3 upButton = CoreRoomElevatorGeometry.stationButtonWorld(
            pos, state.getValue(FACING), true);
    Vec3 downButton = CoreRoomElevatorGeometry.stationButtonWorld(
            pos, state.getValue(FACING), false);
    double upDistance = hit.getLocation().distanceToSqr(upButton);
    double downDistance = hit.getLocation().distanceToSqr(downButton);
            if (Math.min(upDistance, downDistance) > 0.20D * 0.20D) {
                return InteractionResult.PASS;
            }
            Vec3 selectedButton = upDistance <= downDistance
                    ? upButton : downButton;
            Direction facing = state.getValue(FACING);
            Vec3 outward = new Vec3(facing.getStepX(), 0.0D,
                    facing.getStepZ());
            if (player.getEyePosition().subtract(selectedButton)
                    .dot(outward) <= 0.02D) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer serverPlayer)
                    || !(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.PASS;
            }
            ElevatorFoundation.TravelDirection direction =
                    upDistance <= downDistance
                    ? ElevatorFoundation.TravelDirection.UP
                    : ElevatorFoundation.TravelDirection.DOWN;
            return handleContextInteraction(serverLevel, pos, serverPlayer,
                    direction == ElevatorFoundation.TravelDirection.UP
                            ? "elevator_station_up" : "elevator_station_down");
        }

        public InteractionResult handleContextInteraction(ServerLevel level,
                BlockPos pos, ServerPlayer player, String actionKey) {
            ElevatorFoundation.TravelDirection direction = actionKey != null
                    && actionKey.endsWith("up")
                    ? ElevatorFoundation.TravelDirection.UP
                    : ElevatorFoundation.TravelDirection.DOWN;
            BlockState state = level.getBlockState(pos);
            Vec3 button = CoreRoomElevatorGeometry.stationButtonWorld(pos,
                    state.getValue(FACING),
                    direction == ElevatorFoundation.TravelDirection.UP);
            level.playSound(null, button.x, button.y, button.z,
                    ELEVATOR_BUTTON_PRESS.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    1.0F, 1.0F);
            boolean accepted = CoreRoomElevatorManager.requestFromStation(
                    level, pos, direction, player);
            if (accepted) {
                level.playSound(null, button.x, button.y, button.z,
                        ELEVATOR_BUTTON_ACCEPT.get(),
                        net.minecraft.sounds.SoundSource.BLOCKS,
                        1.0F, 1.0F);
            }
            return accepted ? InteractionResult.CONSUME
                    : InteractionResult.FAIL;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            boolean gateSolid = !(level.getBlockEntity(pos)
                    instanceof StationBlockEntity station)
                    || station.isGateCollisionSolid();
            return CoreRoomElevatorGeometry.stationCellShape(
                    state.getValue(FACING), 0, 0, 0, gateSolid);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            boolean gateSolid = !(level.getBlockEntity(pos)
                    instanceof StationBlockEntity station)
                    || station.isGateCollisionSolid();
            return CoreRoomElevatorGeometry.stationCellShape(
                    state.getValue(FACING), 0, 0, 0, gateSolid);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new StationBlockEntity(pos, state);
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                BlockState state, BlockEntityType<T> type) {
            return createTickerHelper(type, STATION_BE.get(),
                    StationBlockEntity::tick);
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
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

    public static final class PulleyBlock extends BaseEntityBlock {
        private PulleyBlock() {
            super(BlockBehaviour.Properties.of().strength(6.0F, 18.0F)
                    .sound(SoundType.METAL).noOcclusion());
            registerDefaultState(stateDefinition.any().setValue(FACING,
                    Direction.NORTH));
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction stationFacing = CoreRoomElevatorManager.findPulleyFacing(
                    context.getLevel(), context.getClickedPos());
            return stationFacing == null ? null
                    : defaultBlockState().setValue(FACING, stationFacing);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                @Nullable LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (!(level instanceof ServerLevel serverLevel)) return;
            Player player = placer instanceof Player found ? found : null;
            Direction facing = state.getValue(FACING);
            if (!placeParts(serverLevel, pos, facing, StructureKind.PULLEY,
                    pulleyCells(), player)) {
                serverLevel.destroyBlock(pos, true, placer);
                return;
            }
            CoreRoomElevatorManager.rebuildColumn(serverLevel, pos, player);
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
                Direction facing = state.getValue(FACING);
                removeParts(serverLevel, pos, facing, StructureKind.PULLEY);
                ScpClassifiedDirectiveMod.queueServerWork(1,
                        () -> CoreRoomElevatorManager.rebuildColumn(
                                serverLevel, pos, null));
            }
            super.onRemove(state, level, pos, newState, moving);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return CoreRoomElevatorGeometry.pulleySelectionCellShape();
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return CoreRoomElevatorGeometry.pulleyCellShape(
                    state.getValue(FACING), 0, 0, 0);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new PulleyBlockEntity(pos, state);
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                BlockState state, BlockEntityType<T> type) {
            return createTickerHelper(type, PULLEY_BE.get(),
                    PulleyBlockEntity::tick);
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
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

    public static final class BeamBlock extends HorizontalDirectionalBlock {
        private BeamBlock() {
            super(BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F)
                    .sound(SoundType.METAL).noOcclusion());
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(GENERATED, false));
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState()
                    .setValue(FACING,
                            context.getHorizontalDirection().getOpposite())
                    .setValue(GENERATED, false);
        }

        @Override
        public boolean canBeReplaced(BlockState state,
                BlockPlaceContext context) {
            ItemStack held = context.getItemInHand();
            if (state.getValue(GENERATED)
                    && (held.is(STATION_ITEM.get())
                    || held.is(PULLEY_ITEM.get()))) {
                return true;
            }
            return super.canBeReplaced(state, context);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                @Nullable LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (!(level instanceof ServerLevel serverLevel)) return;
            Player player = placer instanceof Player found ? found : null;
            if (!placeBeamParts(serverLevel, pos, state.getValue(FACING), player)) {
                serverLevel.destroyBlock(pos, true, placer);
            }
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
                removeParts(serverLevel, pos, state.getValue(FACING),
                        StructureKind.BEAMS);
                if (state.getValue(GENERATED)
                        && !CoreRoomElevatorManager.isRebuilding()) {
                    ScpClassifiedDirectiveMod.queueServerWork(1,
                            () -> CoreRoomElevatorManager.rebuildColumn(
                                    serverLevel, pos, null));
                }
            }
            super.onRemove(state, level, pos, newState, moving);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return CoreRoomElevatorGeometry.beamCellShape(
                    state.getValue(FACING), 0, 0, 0);
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, GENERATED);
        }
    }

    public static final class CoreRoomFloorBlock
            extends HorizontalDirectionalBlock {
        private static final VoxelShape FLOOR_SHAPE = Block.box(
                0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 16.0D);

        private CoreRoomFloorBlock() {
            super(BlockBehaviour.Properties.of().strength(4.0F, 12.0F)
                    .sound(SoundType.METAL).noOcclusion());
            registerDefaultState(stateDefinition.any().setValue(FACING,
                    Direction.NORTH));
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING,
                    context.getHorizontalDirection().getOpposite());
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return FLOOR_SHAPE;
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return FLOOR_SHAPE;
        }

        @Override
        public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
                BlockPos pos) {
            return Shapes.empty();
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
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

    public static class StructurePartBlock extends BaseEntityBlock {
        private StructurePartBlock() {
            this(BlockBehaviour.Properties.of().strength(5.0F, 15.0F)
                    .sound(SoundType.METAL).noOcclusion());
        }

        protected StructurePartBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new StructurePartBlockEntity(pos, state);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return getCollisionShape(state, level, pos, context);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            if (!(level.getBlockEntity(pos) instanceof StructurePartBlockEntity part)) {
                return Shapes.empty();
            }
            BlockState masterState = level.getBlockState(part.masterPos());
            if (!masterState.hasProperty(FACING)) return Shapes.empty();
            Direction facing = masterState.getValue(FACING);
            if (part.kind() == StructureKind.PULLEY) {
                return CoreRoomElevatorGeometry.pulleyCellShape(facing,
                        part.localX(), part.localY(), part.localZ());
            }
            if (part.kind() == StructureKind.BEAMS) {
                return CoreRoomElevatorGeometry.beamCellShape(facing,
                        part.localX(), part.localY(), part.localZ());
            }
            boolean gateSolid = !(level.getBlockEntity(part.masterPos())
                    instanceof StationBlockEntity station)
                    || station.isGateCollisionSolid();
            return CoreRoomElevatorGeometry.stationCellShape(facing,
                    part.localX(), part.localY(), part.localZ(), gateSolid);
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (!(level.getBlockEntity(pos) instanceof StructurePartBlockEntity part)) {
                return InteractionResult.PASS;
            }
            BlockState masterState = level.getBlockState(part.masterPos());
            if (masterState.getBlock() instanceof StationBlock station) {
                BlockHitResult redirected = new BlockHitResult(hit.getLocation(),
                        hit.getDirection(), part.masterPos(), hit.isInside());
                return station.use(masterState, level, part.masterPos(), player,
                        hand, redirected);
            }
            return InteractionResult.PASS;
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos,
                BlockState state, Player player) {
            if (!level.isClientSide
                    && level.getBlockEntity(pos) instanceof StructurePartBlockEntity part) {
                BlockPos master = part.masterPos();
                if (!master.equals(pos)) {
                    level.destroyBlock(master, !player.isCreative(), player);
                }
            }
            super.playerWillDestroy(level, pos, state, player);
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (!state.is(newState.getBlock()) && !isMutatingParts()
                    && level instanceof ServerLevel serverLevel
                    && level.getBlockEntity(pos) instanceof StructurePartBlockEntity part) {
                BlockPos master = part.masterPos();
                ScpClassifiedDirectiveMod.queueServerWork(1, () -> {
                    BlockState masterState = serverLevel.getBlockState(master);
                    if (masterState.is(STATION.get()) || masterState.is(PULLEY.get())
                            || masterState.is(BEAMS.get())) {
                        serverLevel.destroyBlock(master, true);
                    }
                });
            }
            super.onRemove(state, level, pos, newState, moving);
        }

        @Override
        public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos,
                BlockState state) {
            if (level.getBlockEntity(pos) instanceof StructurePartBlockEntity part) {
                return new ItemStack(switch (part.kind()) {
                    case PULLEY -> PULLEY_ITEM.get();
                    case BEAMS -> net.minecraft.world.item.Items.AIR;
                    case STATION -> STATION_ITEM.get();
                });
            }
            return ItemStack.EMPTY;
        }
    }

    private static final class BeamStructurePartBlock
            extends StructurePartBlock {
        private BeamStructurePartBlock() {
            super(BlockBehaviour.Properties.of()
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.METAL).noOcclusion());
        }
    }

    public static final class StructurePartBlockEntity extends BlockEntity {
        private BlockPos masterPos = BlockPos.ZERO;
        private StructureKind kind = StructureKind.STATION;
        private int localX;
        private int localY;
        private int localZ;

        public StructurePartBlockEntity(BlockPos pos, BlockState state) {
            super(PART_BE.get(), pos, state);
        }

        public void configure(BlockPos masterPos, StructureKind kind,
                int localX, int localY, int localZ) {
            this.masterPos = masterPos.immutable();
            this.kind = kind;
            this.localX = localX;
            this.localY = localY;
            this.localZ = localZ;
            setChanged();
    if (level != null) {
        level.sendBlockUpdated(worldPosition, getBlockState(),
                getBlockState(), 3);
    }
        }

        public BlockPos masterPos() { return masterPos; }
        public StructureKind kind() { return kind; }
        public int localX() { return localX; }
        public int localY() { return localY; }
        public int localZ() { return localZ; }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putLong("Master", masterPos.asLong());
            tag.putString("Kind", kind.getSerializedName());
            tag.putInt("LocalX", localX);
            tag.putInt("LocalY", localY);
            tag.putInt("LocalZ", localZ);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            masterPos = BlockPos.of(tag.getLong("Master"));
            kind = StructureKind.fromName(tag.getString("Kind"));
            localX = tag.getInt("LocalX");
            localY = tag.getInt("LocalY");
            localZ = tag.getInt("LocalZ");
        }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
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
    }

    public static final class StationBlockEntity extends BlockEntity
            implements GeoBlockEntity {
        private static final RawAnimation CLOSED_ANIM = RawAnimation.begin()
                .thenLoop(ElevatorAssets.STATION_CLOSED);
        private static final RawAnimation OPENING_ANIM = RawAnimation.begin()
                .thenPlay(ElevatorAssets.STATION_OPENING);
        private static final RawAnimation OPEN_ANIM = RawAnimation.begin()
                .thenLoop(ElevatorAssets.STATION_OPEN);
        private static final RawAnimation CLOSING_ANIM = RawAnimation.begin()
                .thenPlay(ElevatorAssets.STATION_CLOSING);

        private static final int DOOR_TICKS = 15;
        private static final int COLLISION_THRESHOLD = DOOR_TICKS / 2;

        private final AnimatableInstanceCache cache =
                GeckoLibUtil.createInstanceCache(this);
        private DoorVisualState doorState = DoorVisualState.CLOSED;
        private int doorTicks = DOOR_TICKS;
        private boolean initialized;
        private ElevatorArrivalDisplayData arrivalDisplay =
                ElevatorArrivalDisplayData.NONE;

        public StationBlockEntity(BlockPos pos, BlockState state) {
            super(STATION_BE.get(), pos, state);
        }

        public static void tick(Level level, BlockPos pos, BlockState state,
                StationBlockEntity blockEntity) {
            if (level.isClientSide) {
                blockEntity.advanceDoorClock();
                return;
            }
            if (!(level instanceof ServerLevel serverLevel)) return;
            if (!blockEntity.initialized) {
                blockEntity.initialized = true;
                CoreRoomElevatorManager.rebuildColumn(serverLevel, pos, null);
            }
            DiscoveryMusicManager.checkCoreRoomStation(serverLevel, pos);
            DoorVisualState next = CoreRoomElevatorManager
                    .visualStateForStation(serverLevel, pos);
            blockEntity.setDoorState(next);
            blockEntity.advanceDoorClock();
        }

        public DoorVisualState doorState() {
            return doorState;
        }

        public ElevatorArrivalDisplayData arrivalDisplay() {
            return arrivalDisplay;
        }

        public void setArrivalDisplay(
                ElevatorArrivalDisplayData data) {
            arrivalDisplay = data == null
                    ? ElevatorArrivalDisplayData.NONE : data;
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(),
                        getBlockState(), Block.UPDATE_ALL);
            }
        }

        public boolean isGateCollisionSolid() {
            return switch (doorState) {
                case OPEN -> false;
                case OPENING -> doorTicks < COLLISION_THRESHOLD;
                case CLOSING -> doorTicks >= COLLISION_THRESHOLD;
                default -> true;
            };
        }

        private void advanceDoorClock() {
            if ((doorState == DoorVisualState.OPENING
                    || doorState == DoorVisualState.CLOSING)
                    && doorTicks < DOOR_TICKS) {
                doorTicks++;
            }
        }

        private void setDoorState(DoorVisualState state) {
            if (state == doorState) return;
            doorState = state;
            doorTicks = 0;
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(),
                        getBlockState(), 3);
            }
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, "station_door", 0,
                    state -> state.setAndContinue(switch (doorState) {
                        case OPENING -> OPENING_ANIM;
                        case OPEN -> OPEN_ANIM;
                        case CLOSING -> CLOSING_ANIM;
                        default -> CLOSED_ANIM;
                    })));
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putByte("DoorState", (byte) doorState.ordinal());
            tag.putInt("DoorTicks", doorTicks);
            if (arrivalDisplay.enabled()) {
                tag.put("ArrivalDisplay", arrivalDisplay.save());
            }
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            int value = tag.getByte("DoorState");
            doorState = value >= 0 && value < DoorVisualState.values().length
                    ? DoorVisualState.values()[value] : DoorVisualState.CLOSED;
            doorTicks = tag.contains("DoorTicks")
                    ? Mth.clamp(tag.getInt("DoorTicks"), 0, DOOR_TICKS)
                    : DOOR_TICKS;
            arrivalDisplay = tag.contains("ArrivalDisplay", Tag.TAG_COMPOUND)
                    ? ElevatorArrivalDisplayData.load(
                            tag.getCompound("ArrivalDisplay"))
                    : ElevatorArrivalDisplayData.NONE;
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
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
    }

    public static final class PulleyBlockEntity extends BlockEntity
            implements GeoBlockEntity {
        private final AnimatableInstanceCache cache =
                GeckoLibUtil.createInstanceCache(this);
        private boolean initialized;

        public PulleyBlockEntity(BlockPos pos, BlockState state) {
            super(PULLEY_BE.get(), pos, state);
        }

        public static void tick(Level level, BlockPos pos, BlockState state,
                PulleyBlockEntity blockEntity) {
            if (level instanceof ServerLevel serverLevel
                    && !blockEntity.initialized) {
                blockEntity.initialized = true;
                CoreRoomElevatorManager.rebuildColumn(serverLevel, pos, null);
            }
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }
    }
}
