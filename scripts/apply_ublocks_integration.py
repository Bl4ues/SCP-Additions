from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"Expected text not found in {path}: {old[:120]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


ublocks_module = r'''package net.mcreator.scpadditions.facility;

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
    public static final RegistryObject<Block> BENCH = directional(
            "bench", DirectionalShape.BENCH, SoundType.METAL);
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
            return shape == DirectionalShape.BENCH
                    ? shape.outline(state.getValue(FACING)) : Shapes.empty();
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return Collections.singletonList(new ItemStack(this));
        }
    }

    private enum DirectionalShape {
        BENCH,
        FLOOR_DECAL,
        WALL_DECOR,
        VENT;

        private VoxelShape outline(Direction facing) {
            return switch (this) {
                case BENCH -> facing.getAxis() == Direction.Axis.X
                        ? Block.box(2.0D, 0.0D, 0.0D, 14.0D, 9.0D, 16.0D)
                        : Block.box(0.0D, 0.0D, 2.0D, 16.0D, 9.0D, 14.0D);
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
'''

ublocks_client = r'''package net.mcreator.scpadditions.facility;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class UBlocksClientEvents {
    private UBlocksClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> UBlocksModule.cutoutBlocks().forEach(block ->
                ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout())));
    }
}
'''

Path("src/main/java/net/mcreator/scpadditions/facility/UBlocksModule.java").write_text(
        ublocks_module, encoding="utf-8")
Path("src/main/java/net/mcreator/scpadditions/facility/UBlocksClientEvents.java").write_text(
        ublocks_client, encoding="utf-8")

replace_once(
    "src/main/java/net/mcreator/scpadditions/ScpAdditionsMod.java",
    "import net.mcreator.scpadditions.facility.FacilityModule;\n",
    "import net.mcreator.scpadditions.facility.FacilityModule;\n"
    "import net.mcreator.scpadditions.facility.UBlocksModule;\n",
)
replace_once(
    "src/main/java/net/mcreator/scpadditions/ScpAdditionsMod.java",
    "\t\tScpAdditionsModTabs.REGISTRY.register(bus);\n\t\tFacilityModule.register(bus);\n",
    "\t\tScpAdditionsModTabs.REGISTRY.register(bus);\n"
    "\t\tUBlocksModule.register(bus);\n"
    "\t\tFacilityModule.register(bus);\n",
)

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    ".displayItems((parameters, output) -> CREATIVE_ITEMS.forEach(item -> output.accept(item.get())))",
    ".displayItems((parameters, output) -> {\n"
    "                        UBlocksModule.creativeItems().forEach(item -> output.accept(item.get()));\n"
    "                        CREATIVE_ITEMS.forEach(item -> output.accept(item.get()));\n"
    "                    })",
)

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityLegacyMappings.java",
    "        for (MissingMappingsEvent.Mapping<Item> mapping :\n"
    "                event.getMappings(ForgeRegistries.Keys.ITEMS, FacilityModule.LEGACY_MODID)) {",
    "        for (MissingMappingsEvent.Mapping<Block> mapping :\n"
    "                event.getMappings(ForgeRegistries.Keys.BLOCKS, UBlocksModule.LEGACY_MODID)) {\n"
    "            RegistryObject<Block> replacement = UBlocksModule.blockByPath(mapping.getKey().getPath());\n"
    "            if (replacement != null && replacement.isPresent()) {\n"
    "                mapping.remap(replacement.get());\n"
    "            }\n"
    "        }\n\n"
    "        for (MissingMappingsEvent.Mapping<Item> mapping :\n"
    "                event.getMappings(ForgeRegistries.Keys.ITEMS, FacilityModule.LEGACY_MODID)) {",
)
replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityLegacyMappings.java",
    "        }\n    }\n}",
    "        }\n\n"
    "        for (MissingMappingsEvent.Mapping<Item> mapping :\n"
    "                event.getMappings(ForgeRegistries.Keys.ITEMS, UBlocksModule.LEGACY_MODID)) {\n"
    "            RegistryObject<Item> replacement = UBlocksModule.itemByPath(mapping.getKey().getPath());\n"
    "            if (replacement != null && replacement.isPresent()) {\n"
    "                mapping.remap(replacement.get());\n"
    "            }\n"
    "        }\n"
    "    }\n}",
)

build_file = "build.gradle"
insert_marker = "        // Copy loot tables, recipes and other namespace-owned data into the new\n"
ublock_copy = '''        // SCP UBlocks follows the same consolidation rule: registry-facing
        // blockstates and item models are copied into scp_additions, while the
        // original namespace remains the library for block/custom models and textures.
        def ublocksAssets = file("$buildDir/resources/main/assets/scp_ublocks")
        if (ublocksAssets.exists()) {
            project.copy {
                from new File(ublocksAssets, 'blockstates')
                into new File(additionsAssets, 'blockstates')
            }
            project.copy {
                from new File(ublocksAssets, 'models/item')
                into new File(additionsAssets, 'models/item')
            }
        }

'''
replace_once(build_file, insert_marker, ublock_copy + insert_marker)
replace_once(
    build_file,
    "        def legacyLangFile = file('src/main/resources/assets/scp_unity_extra_blocks/lang/en_us.json')\n",
    "        def legacyLangFile = file('src/main/resources/assets/scp_unity_extra_blocks/lang/en_us.json')\n"
    "        def ublocksLangFile = file('src/main/resources/assets/scp_ublocks/lang/en_us.json')\n",
)
lang_marker = "            if (langPatchFile.exists()) {\n"
ublock_lang = '''            if (ublocksLangFile.exists()) {
                def ublocksLang = parser.parse(ublocksLangFile)
                ublocksLang.each { key, value ->
                    if (key.startsWith('block.scp_ublocks.')) {
                        baseLang[key.replace('block.scp_ublocks.', 'block.scp_additions.')] = value
                    }
                    if (key.startsWith('item.scp_ublocks.')) {
                        baseLang[key.replace('item.scp_ublocks.', 'item.scp_additions.')] = value
                    }
                }
            }

'''
replace_once(build_file, lang_marker, ublock_lang + lang_marker)
replace_once(
    build_file,
    "        ['scp_additions', 'scpinventory', 'scp_unity_extra_blocks'].each { namespace ->",
    "        ['scp_additions', 'scpinventory', 'scp_unity_extra_blocks', 'scp_ublocks'].each { namespace ->",
)

replace_once(
    "src/main/java/net/mcreator/scpadditions/init/UnifiedReaderItems.java",
    "     * Texture/model intentionally deferred. Shift-right-clicking a reader with\n",
    "     * Shift-right-clicking a reader with\n",
)

screwdriver_model = '''{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "scp_additions:item/screwdriver"
  }
}
'''
model_path = Path("src/main/resources/assets/scp_additions/models/item/screwdriver.json")
model_path.parent.mkdir(parents=True, exist_ok=True)
model_path.write_text(screwdriver_model, encoding="utf-8")

print("SCP UBlocks registry, resources, tab ordering and screwdriver model patched.")
