package net.mcreator.scpadditions.facility;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

/** Two-cell decorative table authored for the Tesla Gate terminal area. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TeslaGateTerminalTableModule {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpAdditionsMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpAdditionsMod.MODID);

    public static final RegistryObject<Block> BLOCK = BLOCKS.register(
            "tesla_gate_terminal_table", TerminalTableBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register(
            "tesla_gate_terminal_table", () -> new TerminalTableItem(
                    BLOCK.get(), new Item.Properties()));

    private TeslaGateTerminalTableModule() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    @SubscribeEvent
    public static void addToFacilityTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().location().equals(new ResourceLocation(
                ScpAdditionsMod.MODID, "scp_unity_blocks"))) {
            event.accept(ITEM.get());
        }
    }

    public static final class TerminalTableBlock extends HorizontalDirectionalBlock {
        public static final EnumProperty<Part> PART = EnumProperty.create(
                "part", Part.class);

        private static final EnumMap<Direction, VoxelShape> SHAPES =
                buildShapes();

        private TerminalTableBlock() {
            super(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1.0F, 1200.0F)
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(PART, Part.CONTROLLER));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, PART);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction facing = context.getHorizontalDirection().getOpposite();
            BlockPos controller = context.getClickedPos();
            BlockPos extension = extensionPos(controller, facing);
            if (!context.getLevel().getWorldBorder().isWithinBounds(extension)
                    || !context.getLevel().getBlockState(extension)
                    .canBeReplaced(context)) {
                return null;
            }
            return defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(PART, Part.CONTROLLER);
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (level.isClientSide || oldState.getBlock() == this
                    || state.getValue(PART) != Part.CONTROLLER) {
                return;
            }

            Direction facing = state.getValue(FACING);
            BlockPos extension = extensionPos(pos, facing);
            BlockState existing = level.getBlockState(extension);
            if (!existing.canBeReplaced()) {
                level.destroyBlock(pos, true);
                return;
            }

            level.setBlock(extension, defaultBlockState()
                            .setValue(FACING, facing)
                            .setValue(PART, Part.EXTENSION),
                    Block.UPDATE_ALL);
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (state.getBlock() != newState.getBlock() && !level.isClientSide) {
                Direction facing = state.getValue(FACING);
                BlockPos counterpart = state.getValue(PART) == Part.CONTROLLER
                        ? extensionPos(pos, facing)
                        : controllerPos(pos, facing);
                BlockState other = level.getBlockState(counterpart);
                if (other.getBlock() == this
                        && other.getValue(FACING) == facing
                        && other.getValue(PART) != state.getValue(PART)) {
                    level.destroyBlock(counterpart, false);
                }
            }
            super.onRemove(state, level, pos, newState, moving);
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighborState, LevelAccessor level, BlockPos pos,
                BlockPos neighborPos) {
            Direction facing = state.getValue(FACING);
            BlockPos expected = state.getValue(PART) == Part.CONTROLLER
                    ? extensionPos(pos, facing)
                    : controllerPos(pos, facing);
            if (neighborPos.equals(expected)) {
                BlockState counterpart = level.getBlockState(expected);
                Part expectedPart = state.getValue(PART) == Part.CONTROLLER
                        ? Part.EXTENSION : Part.CONTROLLER;
                if (counterpart.getBlock() != this
                        || counterpart.getValue(FACING) != facing
                        || counterpart.getValue(PART) != expectedPart) {
                    return Blocks.AIR.defaultBlockState();
                }
            }
            return super.updateShape(state, direction, neighborState, level,
                    pos, neighborPos);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return SHAPES.getOrDefault(state.getValue(FACING), Shapes.block());
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
        public boolean isPathfindable(BlockState state, BlockGetter level,
                BlockPos pos, PathComputationType type) {
            return false;
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
            return state.getValue(PART) == Part.CONTROLLER
                    ? Collections.singletonList(new ItemStack(ITEM.get()))
                    : Collections.emptyList();
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target,
                BlockGetter level, BlockPos pos,
                net.minecraft.world.entity.player.Player player) {
            return new ItemStack(ITEM.get());
        }

        private static BlockPos extensionPos(BlockPos controller,
                Direction facing) {
            return controller.relative(facing.getCounterClockWise());
        }

        private static BlockPos controllerPos(BlockPos extension,
                Direction facing) {
            return extension.relative(facing.getClockWise());
        }

        private static EnumMap<Direction, VoxelShape> buildShapes() {
            VoxelShape north = Shapes.or(
                    box(0.0D, 0.0D, 9.0D, 16.0D, 16.0D, 16.0D),
                    box(0.0D, 13.0D, 0.0D, 16.0D, 16.0D, 9.0D),
                    box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 9.0D),
                    box(0.0D, 6.0D, 7.0D, 16.0D, 9.5D, 9.0D))
                    .optimize();
            EnumMap<Direction, VoxelShape> shapes =
                    new EnumMap<>(Direction.class);
            shapes.put(Direction.NORTH, north);
            shapes.put(Direction.EAST, rotateY(north, 1));
            shapes.put(Direction.SOUTH, rotateY(north, 2));
            shapes.put(Direction.WEST, rotateY(north, 3));
            return shapes;
        }

        private static VoxelShape rotateY(VoxelShape source,
                int quarterTurns) {
            VoxelShape current = source;
            for (int turn = 0; turn < quarterTurns; turn++) {
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

    public enum Part implements StringRepresentable {
        CONTROLLER("controller"),
        EXTENSION("extension");

        private final String serializedName;

        Part(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    private static final class TerminalTableItem extends BlockItem {
        private TerminalTableItem(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.literal("Tesla Gate Terminal Table");
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(
                    "tooltip.scp_additions.decorative_prop")
                    .withStyle(ChatFormatting.GRAY));
            super.appendHoverText(stack, level, tooltip, flag);
        }
    }
}
