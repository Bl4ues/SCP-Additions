from __future__ import annotations

import json
from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one match in {path}, found {count}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


def write(path: str, content: str) -> None:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content.rstrip() + "\n", encoding="utf-8")


write("src/main/java/net/mcreator/scpadditions/facility/WetFloorBlock.java", r'''
package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** Decorative folding caution sign with eight placement orientations. */
public final class WetFloorBlock extends BaseEntityBlock {
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 7);

    private static final VoxelShape NORTH_SOUTH = box(5.0D, 0.0D, 3.0D,
            11.0D, 16.0D, 13.0D);
    private static final VoxelShape EAST_WEST = box(3.0D, 0.0D, 5.0D,
            13.0D, 16.0D, 11.0D);
    private static final VoxelShape DIAGONAL = box(4.0D, 0.0D, 4.0D,
            12.0D, 16.0D, 12.0D);

    public WetFloorBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .strength(0.8F)
                .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WetFloorBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!canStandOn(context.getLevel(), context.getClickedPos())) return null;
        int rotation = Mth.floor((context.getRotation() + 180.0F + 22.5F) / 45.0F) & 7;
        return defaultBlockState().setValue(ROTATION, rotation);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelAccessor level, BlockPos pos) {
        return canStandOn(level, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
            LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.DOWN && !canStandOn(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        int rotation = state.getValue(ROTATION);
        if ((rotation & 1) != 0) return DIAGONAL;
        return (rotation & 2) == 0 ? NORTH_SOUTH : EAST_WEST;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        int offset = switch (rotation) {
            case CLOCKWISE_90 -> 2;
            case CLOCKWISE_180 -> 4;
            case COUNTERCLOCKWISE_90 -> 6;
            default -> 0;
        };
        return state.setValue(ROTATION, (state.getValue(ROTATION) + offset) & 7);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        int value = state.getValue(ROTATION);
        return switch (mirror) {
            case LEFT_RIGHT -> state.setValue(ROTATION, (-value) & 7);
            case FRONT_BACK -> state.setValue(ROTATION, (4 - value) & 7);
            default -> state;
        };
    }

    private static boolean canStandOn(LevelAccessor level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
''')

write("src/main/java/net/mcreator/scpadditions/facility/WetFloorBlockEntity.java", r'''
package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Static GeckoLib block entity used to render the authored angled geometry. */
public final class WetFloorBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public WetFloorBlockEntity(BlockPos pos, BlockState state) {
        super(FacilityModule.WET_FLOOR_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Intentionally static. Keeping a GeckoLib animatable allows future animation.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
''')

write("src/main/java/net/mcreator/scpadditions/facility/WetFloorBlockItem.java", r'''
package net.mcreator.scpadditions.facility;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.mcreator.scpadditions.client.WetFloorItemRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/** Block item that renders the same GeckoLib model in inventories and hands. */
public final class WetFloorBlockItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public WetFloorBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private WetFloorItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new WetFloorItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.scp_additions.decorative_prop")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No animation controller is required for this static prop.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
''')

write("src/main/java/net/mcreator/scpadditions/client/WetFloorGeoModel.java", r'''
package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.WetFloorBlock;
import net.mcreator.scpadditions.facility.WetFloorBlockEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public final class WetFloorGeoModel extends GeoModel<WetFloorBlockEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpAdditionsMod.MODID, "geo/block/wet_floor.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/block/wet_floor.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpAdditionsMod.MODID, "animations/block/wet_floor.animation.json");

    @Override
    public ResourceLocation getModelResource(WetFloorBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(WetFloorBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(WetFloorBlockEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(WetFloorBlockEntity animatable, long instanceId,
            AnimationState<WetFloorBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        CoreGeoBone root = getAnimationProcessor().getBone("root");
        if (root == null || !animatable.getBlockState().hasProperty(WetFloorBlock.ROTATION)) {
            return;
        }
        int rotation = animatable.getBlockState().getValue(WetFloorBlock.ROTATION);
        root.setRotY((float) (-rotation * Math.PI / 4.0D));
    }
}
''')

write("src/main/java/net/mcreator/scpadditions/client/WetFloorBlockEntityRenderer.java", r'''
package net.mcreator.scpadditions.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.mcreator.scpadditions.facility.WetFloorBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class WetFloorBlockEntityRenderer
        extends GeoBlockRenderer<WetFloorBlockEntity> {
    public WetFloorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new WetFloorGeoModel());
    }
}
''')

write("src/main/java/net/mcreator/scpadditions/client/WetFloorItemGeoModel.java", r'''
package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.WetFloorBlockItem;
import software.bernie.geckolib.model.GeoModel;

public final class WetFloorItemGeoModel extends GeoModel<WetFloorBlockItem> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpAdditionsMod.MODID, "geo/block/wet_floor.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/block/wet_floor.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpAdditionsMod.MODID, "animations/block/wet_floor.animation.json");

    @Override
    public ResourceLocation getModelResource(WetFloorBlockItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(WetFloorBlockItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(WetFloorBlockItem animatable) {
        return ANIMATION;
    }
}
''')

write("src/main/java/net/mcreator/scpadditions/client/WetFloorItemRenderer.java", r'''
package net.mcreator.scpadditions.client;

import net.mcreator.scpadditions.facility.WetFloorBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class WetFloorItemRenderer extends GeoItemRenderer<WetFloorBlockItem> {
    public WetFloorItemRenderer() {
        super(new WetFloorItemGeoModel());
    }
}
''')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    '''    public static final RegistryObject<Block> TV = registerBlock("tv", TvBlock::new, true);\n    public static final RegistryObject<Block> TRASHBIN = registerBlock("trashbin", TrashbinBlock::new, true);\n    public static final RegistryObject<Block> FACILITY_PROP_PART =\n''',
    '''    public static final RegistryObject<Block> TV = registerBlock("tv", TvBlock::new, true);\n    public static final RegistryObject<Block> TRASHBIN = registerBlock("trashbin", TrashbinBlock::new, true);\n    public static final RegistryObject<Block> WET_FLOOR = registerWetFloor();\n    public static final RegistryObject<Block> FACILITY_PROP_PART =\n''')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    '''    public static final RegistryObject<BlockEntityType<FacilitySignBlockEntity>>\n            FACILITY_SIGN_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(\n''',
    '''    public static final RegistryObject<BlockEntityType<WetFloorBlockEntity>>\n            WET_FLOOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(\n                    "wet_floor", () -> BlockEntityType.Builder.of(\n                            WetFloorBlockEntity::new, WET_FLOOR.get()).build(null));\n    public static final RegistryObject<BlockEntityType<FacilitySignBlockEntity>>\n            FACILITY_SIGN_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(\n''')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    '''        addFacilityCreativeItem(props, "fire_extinguisher");\n        addFacilityCreativeItem(props, "tv");\n''',
    '''        addFacilityCreativeItem(props, "fire_extinguisher");\n        addFacilityCreativeItem(props, "wet_floor");\n        addFacilityCreativeItem(props, "tv");\n''')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    '''    private static RegistryObject<Block> registerBlock(String path,\n            Supplier<? extends Block> factory, boolean publicItem) {\n''',
    '''    private static RegistryObject<Block> registerWetFloor() {\n        String path = "wet_floor";\n        RegistryObject<Block> block = BLOCKS.register(path, WetFloorBlock::new);\n        RegistryObject<Item> item = ITEMS.register(path,\n                () -> new WetFloorBlockItem(block.get(), new Item.Properties()));\n        BLOCKS_BY_PATH.put(path, block);\n        ITEMS_BY_PATH.put(path, item);\n        CREATIVE_ITEMS.add(item);\n        return block;\n    }\n\n    private static RegistryObject<Block> registerBlock(String path,\n            Supplier<? extends Block> factory, boolean publicItem) {\n''')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityClientRenderEvents.java",
    '''import net.mcreator.scpadditions.client.FacilitySignBlockEntityRenderer;\n''',
    '''import net.mcreator.scpadditions.client.FacilitySignBlockEntityRenderer;\nimport net.mcreator.scpadditions.client.WetFloorBlockEntityRenderer;\n''')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityClientRenderEvents.java",
    '''        event.registerBlockEntityRenderer(\n                FacilityModule.FACILITY_SIGN_BLOCK_ENTITY.get(),\n                FacilitySignBlockEntityRenderer::new);\n''',
    '''        event.registerBlockEntityRenderer(\n                FacilityModule.FACILITY_SIGN_BLOCK_ENTITY.get(),\n                FacilitySignBlockEntityRenderer::new);\n        event.registerBlockEntityRenderer(\n                FacilityModule.WET_FLOOR_BLOCK_ENTITY.get(),\n                WetFloorBlockEntityRenderer::new);\n''')

# Repair the Blockbench export metadata and create one shared rotation root.
geo_path = Path("src/main/resources/assets/scp_additions/geo/block/wet_floor.geo.json")
geo = json.loads(geo_path.read_text(encoding="utf-8"))
geometry = geo["minecraft:geometry"][0]
description = geometry["description"]
description["identifier"] = "geometry.wet_floor"
description["texture_width"] = 128
description["texture_height"] = 128
bones = geometry["bones"]
if not any(bone.get("name") == "root" for bone in bones):
    for bone in bones:
        if "parent" not in bone:
            bone["parent"] = "root"
    bones.insert(0, {"name": "root", "pivot": [0, 0, 0]})
geo_path.write_text(json.dumps(geo, indent=2) + "\n", encoding="utf-8")

write("src/main/resources/assets/scp_additions/animations/block/wet_floor.animation.json", '''
{
  "format_version": "1.8.0",
  "animations": {}
}
''')

write("src/main/resources/assets/scp_additions/blockstates/wet_floor.json", '''
{
  "multipart": [
    {
      "apply": {
        "model": "scp_additions:block/wet_floor_particle"
      }
    }
  ]
}
''')

write("src/main/resources/assets/scp_additions/models/block/wet_floor_particle.json", '''
{
  "parent": "minecraft:block/block",
  "textures": {
    "particle": "scp_additions:block/wet_floor"
  }
}
''')

write("src/main/resources/assets/scp_additions/models/item/wet_floor.json", '''
{
  "parent": "builtin/entity",
  "textures": {
    "particle": "scp_additions:block/wet_floor"
  },
  "display": {
    "gui": {
      "rotation": [30, 225, 0],
      "translation": [0, -1, 0],
      "scale": [0.72, 0.72, 0.72]
    },
    "ground": {
      "translation": [0, 2, 0],
      "scale": [0.5, 0.5, 0.5]
    },
    "fixed": {
      "rotation": [0, 180, 0],
      "scale": [0.75, 0.75, 0.75]
    },
    "thirdperson_righthand": {
      "rotation": [75, 45, 0],
      "translation": [0, 2.5, 0],
      "scale": [0.38, 0.38, 0.38]
    },
    "thirdperson_lefthand": {
      "rotation": [75, 225, 0],
      "translation": [0, 2.5, 0],
      "scale": [0.38, 0.38, 0.38]
    },
    "firstperson_righthand": {
      "rotation": [0, 45, 0],
      "translation": [1.13, 3.2, 1.13],
      "scale": [0.4, 0.4, 0.4]
    },
    "firstperson_lefthand": {
      "rotation": [0, 225, 0],
      "translation": [1.13, 3.2, 1.13],
      "scale": [0.4, 0.4, 0.4]
    }
  }
}
''')

write("src/main/resources/data/scp_additions/loot_tables/blocks/wet_floor.json", '''
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "scp_additions:wet_floor"
        }
      ],
      "conditions": [
        {
          "condition": "minecraft:survives_explosion"
        }
      ]
    }
  ]
}
''')

lang_path = Path("src/main/resources/assets/scp_additions/lang/en_us_3_0.json")
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang["block.scp_additions.wet_floor"] = "Wet Floor Sign"
lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

replace_once(
    "CHANGELOG.md",
    '''- Added decorative Emergency Button and Fire Extinguisher facility props;\n''',
    '''- Added decorative Emergency Button and Fire Extinguisher facility props;\n- Added the **Wet Floor Sign**, a GeckoLib-rendered decorative prop with eight placement rotations, model-aware collision, and a matching 3D inventory render;\n''')

print("Applied Wet Floor prop integration")
