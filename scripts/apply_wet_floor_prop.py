from __future__ import annotations

from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one match in {path}, found {count}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


def write(path: str, content: str) -> None:
    file = Path(path)
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_text(content, encoding="utf-8")


# Register the GeckoLib block, item and block entity in the existing facility module.
replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    '''    public static final RegistryObject<Block> FIRE_EXTINGUISHER = registerBlock(
            "fire_extinguisher", FireExtinguisherBlock::new, true);
    public static final RegistryObject<Block> SIGN_SUPPORT = registerBlock("sign_support", SignSupportBlock::new, true);
''',
    '''    public static final RegistryObject<Block> FIRE_EXTINGUISHER = registerBlock(
            "fire_extinguisher", FireExtinguisherBlock::new, true);
    public static final RegistryObject<Block> WET_FLOOR = registerBlock(
            "wet_floor", WetFloorBlock::new, true);
    public static final RegistryObject<Block> SIGN_SUPPORT = registerBlock("sign_support", SignSupportBlock::new, true);
''')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    '''    public static final RegistryObject<BlockEntityType<FacilitySignBlockEntity>>
            FACILITY_SIGN_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
                    "facility_sign", () -> BlockEntityType.Builder.of(
                            FacilitySignBlockEntity::new,
                            CORE_ROOM_SIGN.get(), DOOR_SIGN.get()).build(null));
''',
    '''    public static final RegistryObject<BlockEntityType<WetFloorBlockEntity>>
            WET_FLOOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
                    "wet_floor", () -> BlockEntityType.Builder.of(
                            WetFloorBlockEntity::new, WET_FLOOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<FacilitySignBlockEntity>>
            FACILITY_SIGN_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
                    "facility_sign", () -> BlockEntityType.Builder.of(
                            FacilitySignBlockEntity::new,
                            CORE_ROOM_SIGN.get(), DOOR_SIGN.get()).build(null));
''')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    '''        addFacilityCreativeItem(props, "fire_extinguisher");
        addFacilityCreativeItem(props, "tv");
''',
    '''        addFacilityCreativeItem(props, "fire_extinguisher");
        addFacilityCreativeItem(props, "wet_floor");
        addFacilityCreativeItem(props, "tv");
''')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    '''        RegistryObject<Item> item = ITEMS.register(path,
                () -> isDecorativeProp(path)
                        ? new DecorativePropBlockItem(block.get(), new Item.Properties())
                        : new BlockItem(block.get(), new Item.Properties()));
''',
    '''        RegistryObject<Item> item = ITEMS.register(path,
                () -> "wet_floor".equals(path)
                        ? new WetFloorBlockItem(block.get(), new Item.Properties())
                        : isDecorativeProp(path)
                        ? new DecorativePropBlockItem(block.get(), new Item.Properties())
                        : new BlockItem(block.get(), new Item.Properties()));
''')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    '''                || "fire_extinguisher".equals(path)
                || "trashbin".equals(path);
''',
    '''                || "fire_extinguisher".equals(path)
                || "wet_floor".equals(path)
                || "trashbin".equals(path);
''')

# Register the block entity renderer and its alpha-capable render layer.
replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityClientRenderEvents.java",
    '''import net.mcreator.scpadditions.client.FacilitySignBlockEntityRenderer;
''',
    '''import net.mcreator.scpadditions.client.FacilitySignBlockEntityRenderer;
import net.mcreator.scpadditions.client.WetFloorBlockEntityRenderer;
''')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityClientRenderEvents.java",
    '''            ItemBlockRenderTypes.setRenderLayer(
                    FacilityModule.EMERGENCY_BUTTON.get(), RenderType.cutout());
''',
    '''            ItemBlockRenderTypes.setRenderLayer(
                    FacilityModule.EMERGENCY_BUTTON.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    FacilityModule.WET_FLOOR.get(), RenderType.cutout());
''')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityClientRenderEvents.java",
    '''        event.registerBlockEntityRenderer(
                FacilityModule.FACILITY_SIGN_BLOCK_ENTITY.get(),
                FacilitySignBlockEntityRenderer::new);
''',
    '''        event.registerBlockEntityRenderer(
                FacilityModule.FACILITY_SIGN_BLOCK_ENTITY.get(),
                FacilitySignBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                FacilityModule.WET_FLOOR_BLOCK_ENTITY.get(),
                WetFloorBlockEntityRenderer::new);
''')

# Give the uploaded geometry a stable identifier while preserving its authored
# logical 32x32 UV resolution over the 128x128 source image.
replace_once(
    "src/main/resources/assets/scp_additions/geo/block/wet_floor.geo.json",
    '"identifier": "geometry.unknown"',
    '"identifier": "geometry.wet_floor"')

write(
    "src/main/java/net/mcreator/scpadditions/facility/WetFloorBlock.java",
    '''package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** Decorative GeckoLib prop with eight player-relative placement angles. */
public final class WetFloorBlock extends BaseEntityBlock {
    public static final IntegerProperty ROTATION = IntegerProperty.create(
            "rotation", 0, 7);

    private static final VoxelShape NORTH_SOUTH = Block.box(
            5.0D, 0.0D, 1.0D, 11.0D, 16.0D, 15.0D);
    private static final VoxelShape EAST_WEST = Block.box(
            1.0D, 0.0D, 5.0D, 15.0D, 16.0D, 11.0D);
    private static final VoxelShape NORTH_WEST_TO_SOUTH_EAST = diagonalShape(true);
    private static final VoxelShape NORTH_EAST_TO_SOUTH_WEST = diagonalShape(false);

    public WetFloorBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.WOOD)
                .strength(1.2F)
                .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int rotation = Mth.floor(context.getRotation() / 45.0F + 0.5F) & 7;
        BlockState state = defaultBlockState().setValue(ROTATION, rotation);
        return state.canSurvive(context.getLevel(), context.getClickedPos())
                ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below,
                Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction,
            BlockState neighbourState, LevelAccessor level, BlockPos pos,
            BlockPos neighbourPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbourState, level, pos,
                neighbourPos);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        int offset = switch (rotation) {
            case CLOCKWISE_90 -> 2;
            case CLOCKWISE_180 -> 4;
            case COUNTERCLOCKWISE_90 -> 6;
            default -> 0;
        };
        return state.setValue(ROTATION,
                (state.getValue(ROTATION) + offset) & 7);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        int rotation = state.getValue(ROTATION);
        return switch (mirror) {
            case LEFT_RIGHT -> state.setValue(ROTATION, (8 - rotation) & 7);
            case FRONT_BACK -> state.setValue(ROTATION,
                    (4 - rotation + 8) & 7);
            default -> state;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WetFloorBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
            BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level,
            BlockPos pos, PathComputationType type) {
        return false;
    }

    private static VoxelShape shapeFor(BlockState state) {
        return switch (state.getValue(ROTATION) & 3) {
            case 0 -> NORTH_SOUTH;
            case 1 -> NORTH_WEST_TO_SOUTH_EAST;
            case 2 -> EAST_WEST;
            default -> NORTH_EAST_TO_SOUTH_WEST;
        };
    }

    private static VoxelShape diagonalShape(boolean descending) {
        VoxelShape result = Shapes.empty();
        for (int index = 0; index < 4; index++) {
            double minX = 1.5D + index * 3.0D;
            double minZ = descending
                    ? 1.5D + index * 3.0D
                    : 10.5D - index * 3.0D;
            result = Shapes.or(result, Block.box(minX, 0.0D, minZ,
                    minX + 4.0D, 16.0D, minZ + 4.0D));
        }
        return result.optimize();
    }
}
''')

write(
    "src/main/java/net/mcreator/scpadditions/facility/WetFloorBlockEntity.java",
    '''package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Static GeckoLib block entity. It intentionally has no animation controller. */
public final class WetFloorBlockEntity extends BlockEntity
        implements GeoBlockEntity {
    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public WetFloorBlockEntity(BlockPos pos, BlockState state) {
        super(FacilityModule.WET_FLOOR_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        // The model is static. GeckoLib is used for its unrestricted geometry.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.0D);
    }
}
''')

write(
    "src/main/java/net/mcreator/scpadditions/facility/WetFloorBlockItem.java",
    '''package net.mcreator.scpadditions.facility;

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
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/** Block item using the same GeckoLib geometry as the placed prop. */
public final class WetFloorBlockItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public WetFloorBlockItem(Block block, Properties properties) {
        super(block, properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
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
        tooltip.add(Component.translatable(
                "tooltip.scp_additions.decorative_prop")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        // Static item model.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
''')

write(
    "src/main/java/net/mcreator/scpadditions/client/WetFloorGeoModel.java",
    '''package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

/** Shared model resources for the placed block and its block item. */
public final class WetFloorGeoModel<T extends GeoAnimatable>
        extends GeoModel<T> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpAdditionsMod.MODID, "geo/block/wet_floor.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/block/wet_floor.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpAdditionsMod.MODID,
            "animations/block/wet_floor.animation.json");

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ANIMATION;
    }
}
''')

write(
    "src/main/java/net/mcreator/scpadditions/client/WetFloorBlockEntityRenderer.java",
    '''package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.scpadditions.facility.WetFloorBlock;
import net.mcreator.scpadditions.facility.WetFloorBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Applies the block's eight-state rotation around the center of the cell. */
public final class WetFloorBlockEntityRenderer
        extends GeoBlockRenderer<WetFloorBlockEntity> {
    public WetFloorBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
        super(new WetFloorGeoModel<>());
    }

    @Override
    public void render(WetFloorBlockEntity blockEntity, float partialTick,
            PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        poseStack.pushPose();
        if (state.hasProperty(WetFloorBlock.ROTATION)) {
            poseStack.translate(0.5D, 0.0D, 0.5D);
            poseStack.mulPose(Axis.YP.rotationDegrees(
                    -state.getValue(WetFloorBlock.ROTATION) * 45.0F));
            poseStack.translate(-0.5D, 0.0D, -0.5D);
        }
        super.render(blockEntity, partialTick, poseStack, bufferSource,
                packedLight, packedOverlay);
        poseStack.popPose();
    }
}
''')

write(
    "src/main/java/net/mcreator/scpadditions/client/WetFloorItemRenderer.java",
    '''package net.mcreator.scpadditions.client;

import net.mcreator.scpadditions.facility.WetFloorBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** GeckoLib item renderer for the Wet Floor Sign block item. */
public final class WetFloorItemRenderer
        extends GeoItemRenderer<WetFloorBlockItem> {
    public WetFloorItemRenderer() {
        super(new WetFloorGeoModel<>());
    }
}
''')

write(
    "src/main/resources/assets/scp_additions/animations/block/wet_floor.animation.json",
    '''{
  "format_version": "1.8.0",
  "animations": {}
}
''')

write(
    "src/main/resources/assets/scp_additions/blockstates/wet_floor.json",
    '''{
  "multipart": [
    {
      "apply": {
        "model": "scp_additions:block/wet_floor"
      }
    }
  ]
}
''')

write(
    "src/main/resources/assets/scp_additions/models/block/wet_floor.json",
    '''{
  "parent": "minecraft:block/block",
  "textures": {
    "particle": "scp_additions:block/wet_floor"
  }
}
''')

write(
    "src/main/resources/assets/scp_additions/models/item/wet_floor.json",
    '''{
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
      "translation": [0, 3, 0],
      "scale": [0.32, 0.32, 0.32]
    },
    "fixed": {
      "rotation": [0, 180, 0],
      "scale": [0.55, 0.55, 0.55]
    },
    "thirdperson_righthand": {
      "rotation": [65, 45, 0],
      "translation": [0, 2.5, 0],
      "scale": [0.42, 0.42, 0.42]
    },
    "thirdperson_lefthand": {
      "rotation": [65, 225, 0],
      "translation": [0, 2.5, 0],
      "scale": [0.42, 0.42, 0.42]
    },
    "firstperson_righthand": {
      "rotation": [0, 45, 0],
      "translation": [0, 0, 0],
      "scale": [0.48, 0.48, 0.48]
    },
    "firstperson_lefthand": {
      "rotation": [0, 225, 0],
      "translation": [0, 0, 0],
      "scale": [0.48, 0.48, 0.48]
    }
  }
}
''')

write(
    "src/main/resources/data/scp_additions/loot_tables/blocks/wet_floor.json",
    '''{
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

# Add the English display name without rewriting the entire large language file.
lang_path = Path("src/main/resources/assets/scp_additions/lang/en_us.json")
lang = lang_path.read_text(encoding="utf-8")
translation = '"block.scp_additions.wet_floor": "Wet Floor Sign"'
if translation not in lang:
    stripped = lang.rstrip()
    if not stripped.endswith("}"):
        raise RuntimeError("en_us.json does not end with a JSON object")
    body = stripped[:-1].rstrip()
    separator = "," if not body.endswith("{") else ""
    lang_path.write_text(
            body + separator + "\n  " + translation + "\n}\n",
            encoding="utf-8")

replace_once(
    "CHANGELOG.md",
    '''- Added decorative Emergency Button and Fire Extinguisher facility props;
''',
    '''- Added decorative Emergency Button, Fire Extinguisher, and eight-direction Wet Floor Sign facility props;
''')

# Basic source-asset validation before Gradle does the expensive part.
for required in [
    "src/main/resources/assets/scp_additions/geo/block/wet_floor.geo.json",
    "src/main/resources/assets/scp_additions/textures/block/wet_floor.png",
    "src/main/resources/assets/scp_additions/textures/block/wet_floor_s.png",
]:
    if not Path(required).is_file():
        raise RuntimeError(f"Missing required uploaded asset: {required}")

print("Applied Wet Floor Sign GeckoLib integration")
