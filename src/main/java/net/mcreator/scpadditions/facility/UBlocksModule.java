package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Registry bridge for the older SCP UBlocks resource pack.
 *
 * Registry IDs are owned by scp_additions. The scp_ublocks namespace remains
 * only as a model, texture, sound and translation library.
 */
public final class UBlocksModule {
    public static final String MODID = ScpAdditionsMod.MODID;
    public static final String LEGACY_MODID = "scp_ublocks";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    private static final List<RegistryObject<Item>> CREATIVE_ITEMS = new ArrayList<>();
    private static final List<RegistryObject<Block>> CUTOUT_BLOCKS = new ArrayList<>();

    // Sector 1 structural set.
    public static final RegistryObject<Block> SL_1_FLOOR_1 = structure("sl_1_floor_1");
    public static final RegistryObject<Block> SL_1_FLOOR_2 = structure("sl_1_floor_2");
    public static final RegistryObject<Block> SL1_WALL_BOT = structure("sl1_wall_bot");
    public static final RegistryObject<Block> SL1_WALL_MID = structure("sl1_wall_mid");
    public static final RegistryObject<Block> SL_1_WALL_TOP = structure("sl_1_wall_top");

    // Sector 1 directional decoration.
    public static final RegistryObject<Block> SL_1_FLOOR_DETAIL_SMALL = directional(
            "sl_1_floor_detail_small", DirectionalShape.FLOOR_DECAL, SoundType.STONE);
    public static final RegistryObject<Block> SL_1_FLOOR_DETAIL_BIG = directional(
            "sl_1_floor_detail_big", DirectionalShape.FLOOR_DECAL, SoundType.STONE);
    public static final RegistryObject<Block> SL_1_WALL_DETAIL_1_BOT = directional(
            "sl_1_wall_detail_1_bot", DirectionalShape.WALL_DECOR, SoundType.STONE);
    public static final RegistryObject<Block> SL_1_WALL_DETAIL_1_MID = directional(
            "sl_1_wall_detail_1_mid", DirectionalShape.WALL_DECOR, SoundType.STONE);
    public static final RegistryObject<Block> SL_1_WALL_DETAIL_1_TOP = directional(
            "sl_1_wall_detail_1_top", DirectionalShape.WALL_DECOR, SoundType.STONE);
    public static final RegistryObject<Block> SL_1_WALL_DETAIL_2 = directional(
            "sl_1_wall_detail_2", DirectionalShape.WALL_DECOR, SoundType.STONE);

    // Sector 2 structural set.
    public static final RegistryObject<Block> SL_2_FLOOR = structure("sl_2_floor");
    public static final RegistryObject<Block> SL_2_WALL_BOT = structure("sl_2_wall_bot");
    public static final RegistryObject<Block> SL_2_WALL_MID = structure("sl_2_wall_mid");
    public static final RegistryObject<Block> SL_2_WALL_TOP = structure("sl_2_wall_top");

    // Props.
    public static final RegistryObject<Block> VENT_OPEN = directional(
            "vent_open", DirectionalShape.VENT, SoundType.METAL);

    private UBlocksModule() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    /** Items are deliberately returned in registration order for tab ordering. */
    public static List<RegistryObject<Item>> creativeItems() {
        return Collections.unmodifiableList(CREATIVE_ITEMS);
    }

    public static List<RegistryObject<Block>> cutoutBlocks() {
        return Collections.unmodifiableList(CUTOUT_BLOCKS);
    }

    public static RegistryObject<Block> blockByPath(String path) {
        return BLOCKS.getEntries().stream()
                .filter(entry -> entry.getId().getPath().equals(path))
                .findFirst()
                .orElse(null);
    }

    public static RegistryObject<Item> itemByPath(String path) {
        return ITEMS.getEntries().stream()
                .filter(entry -> entry.getId().getPath().equals(path))
                .findFirst()
                .orElse(null);
    }

    private static RegistryObject<Block> structure(String path) {
        return registerBlock(path, UBlockStructureBlock::new, false);
    }

    private static RegistryObject<Block> directional(String path, DirectionalShape shape, SoundType sound) {
        return registerBlock(path, () -> new UBlockDirectionalBlock(shape, sound), true);
    }

    private static RegistryObject<Block> registerBlock(String path,
            Supplier<? extends Block> factory, boolean cutout) {
        RegistryObject<Block> block = BLOCKS.register(path, factory);
        RegistryObject<Item> item = ITEMS.register(path,
                () -> new BlockItem(block.get(), new Item.Properties()));
        CREATIVE_ITEMS.add(item);
        if (cutout) {
            CUTOUT_BLOCKS.add(block);
        }
        return block;
    }

    private static final class UBlockStructureBlock extends Block {
        private UBlockStructureBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(1.5F, 10.0F));
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            List<ItemStack> original = super.getDrops(state, builder);
            return original.isEmpty() ? Collections.singletonList(new ItemStack(this)) : original;
        }
    }

    private static final class UBlockDirectionalBlock extends HorizontalDirectionalBlock {
        private final DirectionalShape shape;

        private UBlockDirectionalBlock(DirectionalShape shape, SoundType sound) {
            super(BlockBehaviour.Properties.of().sound(sound).strength(1.5F, 10.0F)
                    .noOcclusion().isRedstoneConductor((state, level, pos) -> false));
            this.shape = shape;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING,
                    context.getHorizontalDirection().getOpposite());
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
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return shape.outline(state.getValue(FACING));
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return Collections.singletonList(new ItemStack(this));
        }
    }

    private enum DirectionalShape {
        FLOOR_DECAL,
        WALL_DECOR,
        VENT;

        private VoxelShape outline(Direction facing) {
            return switch (this) {
                case FLOOR_DECAL -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, 0.5D, 16.0D);
                case WALL_DECOR, VENT -> switch (facing) {
                    case NORTH -> Block.box(0.0D, 0.0D, 14.5D, 16.0D, 16.0D, 16.0D);
                    case EAST -> Block.box(0.0D, 0.0D, 0.0D, 1.5D, 16.0D, 16.0D);
                    case SOUTH -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.5D);
                    case WEST -> Block.box(14.5D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
                    default -> Shapes.block();
                };
            };
        }
    }
}
