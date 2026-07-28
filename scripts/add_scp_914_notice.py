from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Expected block not found in {path}: {old[:100]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    '    public static final RegistryObject<Block> SIGN_SUPPORT = registerBlock("sign_support", ScpSignSupportBlock::new, true);\n',
    '    public static final RegistryObject<Block> SIGN_SUPPORT = registerBlock("sign_support", ScpSignSupportBlock::new, true);\n'
    '    public static final RegistryObject<Block> SCP_914_USAGE_NOTICE = registerBlock(\n'
    '            "scp_914_usage_notice", Scp914UsageNoticeBlock::new, true);\n')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    '    public static final RegistryObject<BlockEntityType<ScpSignSupportBlockEntity>>\n'
    '            SCP_SIGN_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(\n'
    '                    "scp_sign_support", () -> BlockEntityType.Builder.of(\n'
    '                            ScpSignSupportBlockEntity::new,\n'
    '                            SIGN_SUPPORT.get()).build(null));\n',
    '    public static final RegistryObject<BlockEntityType<ScpSignSupportBlockEntity>>\n'
    '            SCP_SIGN_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(\n'
    '                    "scp_sign_support", () -> BlockEntityType.Builder.of(\n'
    '                            ScpSignSupportBlockEntity::new,\n'
    '                            SIGN_SUPPORT.get()).build(null));\n'
    '    public static final RegistryObject<BlockEntityType<Scp914UsageNoticeBlockEntity>>\n'
    '            SCP_914_NOTICE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(\n'
    '                    "scp_914_usage_notice", () -> BlockEntityType.Builder.of(\n'
    '                            Scp914UsageNoticeBlockEntity::new,\n'
    '                            SCP_914_USAGE_NOTICE.get()).build(null));\n')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    '        addFacilityCreativeItem(props, "water_faucet");\n'
    '        addFacilityCreativeItem(props, "tv");\n',
    '        addFacilityCreativeItem(props, "water_faucet");\n'
    '        addFacilityCreativeItem(props, "scp_914_usage_notice");\n'
    '        addFacilityCreativeItem(props, "tv");\n')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    '                || "water_faucet".equals(path)\n'
    '                || "trashbin".equals(path);\n',
    '                || "water_faucet".equals(path)\n'
    '                || "scp_914_usage_notice".equals(path)\n'
    '                || "trashbin".equals(path);\n')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityLargePropStructure.java",
    '    public enum Kind {\n'
    '        SIGN_SUPPORT,\n'
    '        TV\n'
    '    }\n',
    '    public enum Kind {\n'
    '        SIGN_SUPPORT,\n'
    '        SCP_914_NOTICE,\n'
    '        TV\n'
    '    }\n')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityLargePropStructure.java",
    '    public static Block controllerBlock(Kind kind) {\n'
    '        return kind == Kind.SIGN_SUPPORT\n'
    '                ? FacilityModule.SIGN_SUPPORT.get()\n'
    '                : FacilityModule.TV.get();\n'
    '    }\n',
    '    public static Block controllerBlock(Kind kind) {\n'
    '        return switch (kind) {\n'
    '            case SIGN_SUPPORT -> FacilityModule.SIGN_SUPPORT.get();\n'
    '            case SCP_914_NOTICE -> FacilityModule.SCP_914_USAGE_NOTICE.get();\n'
    '            case TV -> FacilityModule.TV.get();\n'
    '        };\n'
    '    }\n')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityLargePropStructure.java",
    '    private static Direction controllerFacing(BlockState state, Kind kind) {\n'
    '        return kind == Kind.SIGN_SUPPORT\n'
    '                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)\n'
    '                : state.getValue(FacilityPropPartBlock.FACING);\n'
    '    }\n',
    '    private static Direction controllerFacing(BlockState state, Kind kind) {\n'
    '        return kind == Kind.TV\n'
    '                ? state.getValue(FacilityPropPartBlock.FACING)\n'
    '                : state.getValue(BlockStateProperties.HORIZONTAL_FACING);\n'
    '    }\n')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityLargePropStructure.java",
    '        for (Map.Entry<Direction, VoxelShape> entry :\n'
    '                signShapes.entrySet()) {\n'
    '            split(result, Kind.SIGN_SUPPORT, entry.getKey(),\n'
    '                    entry.getValue());\n'
    '        }\n',
    '        for (Map.Entry<Direction, VoxelShape> entry :\n'
    '                signShapes.entrySet()) {\n'
    '            split(result, Kind.SIGN_SUPPORT, entry.getKey(),\n'
    '                    entry.getValue());\n'
    '            split(result, Kind.SCP_914_NOTICE, entry.getKey(),\n'
    '                    entry.getValue());\n'
    '        }\n')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityPropPartBlock.java",
    '        SIGN_FAR("sign_far",\n'
    '                FacilityLargePropStructure.Kind.SIGN_SUPPORT, 1, -1),\n'
    '        TV_LEFT_LOWER',
    '        SIGN_FAR("sign_far",\n'
    '                FacilityLargePropStructure.Kind.SIGN_SUPPORT, 1, -1),\n'
    '        NOTICE_NEAR("notice_near",\n'
    '                FacilityLargePropStructure.Kind.SCP_914_NOTICE, 0, -1),\n'
    '        NOTICE_FAR("notice_far",\n'
    '                FacilityLargePropStructure.Kind.SCP_914_NOTICE, 1, -1),\n'
    '        TV_LEFT_LOWER')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityClientRenderEvents.java",
    'import net.mcreator.scpadditions.client.ScpSignSupportBlockEntityRenderer;\n',
    'import net.mcreator.scpadditions.client.Scp914UsageNoticeBlockEntityRenderer;\n'
    'import net.mcreator.scpadditions.client.ScpSignSupportBlockEntityRenderer;\n')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityClientRenderEvents.java",
    '            ItemBlockRenderTypes.setRenderLayer(\n'
    '                    FacilityModule.DOOR_SIGN.get(), RenderType.cutout());\n',
    '            ItemBlockRenderTypes.setRenderLayer(\n'
    '                    FacilityModule.DOOR_SIGN.get(), RenderType.cutout());\n'
    '            ItemBlockRenderTypes.setRenderLayer(\n'
    '                    FacilityModule.SCP_914_USAGE_NOTICE.get(),\n'
    '                    RenderType.translucent());\n')

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityClientRenderEvents.java",
    '        event.registerBlockEntityRenderer(\n'
    '                FacilityModule.SCP_SIGN_BLOCK_ENTITY.get(),\n'
    '                ScpSignSupportBlockEntityRenderer::new);\n',
    '        event.registerBlockEntityRenderer(\n'
    '                FacilityModule.SCP_SIGN_BLOCK_ENTITY.get(),\n'
    '                ScpSignSupportBlockEntityRenderer::new);\n'
    '        event.registerBlockEntityRenderer(\n'
    '                FacilityModule.SCP_914_NOTICE_BLOCK_ENTITY.get(),\n'
    '                Scp914UsageNoticeBlockEntityRenderer::new);\n')

replace_once(
    "src/main/resources/assets/scp_additions/lang/en_us_3_0.json",
    '  "block.scp_additions.wet_floor": "Wet Floor Sign",\n',
    '  "block.scp_additions.wet_floor": "Wet Floor Sign",\n'
    '  "block.scp_additions.scp_914_usage_notice": "SCP-914 Usage Notice",\n')

replace_once(
    "CHANGELOG.md",
    '- Added decorative Emergency Button, Fire Extinguisher, Wet Floor Sign, and Non-potable Water Faucet facility props;\n',
    '- Added decorative Emergency Button, Fire Extinguisher, Wet Floor Sign, Non-potable Water Faucet, and SCP-914 Usage Notice facility props;\n')

Path("src/main/java/net/mcreator/scpadditions/facility/Scp914UsageNoticeBlock.java").write_text(r'''package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Static decorative SCP-914 notice using the Sign Support frame. */
public final class Scp914UsageNoticeBlock extends BaseEntityBlock
        implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED =
            BlockStateProperties.WATERLOGGED;

    public Scp914UsageNoticeBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.GLASS)
                .strength(1.0F, 10.0F).noOcclusion().randomTicks()
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace.getAxis() == Direction.Axis.Y) return null;
        if (!FacilityLargePropStructure.canPlace(context.getLevel(),
                context.getClickedPos(),
                FacilityLargePropStructure.Kind.SCP_914_NOTICE, clickedFace)) {
            return null;
        }
        boolean waterlogged = context.getLevel().getFluidState(
                context.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState().setValue(FACING, clickedFace)
                .setValue(WATERLOGGED, waterlogged);
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
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction,
            BlockState neighbor, LevelAccessor level, BlockPos pos,
            BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER,
                    Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbor, level, pos,
                neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return FacilityLargePropStructure.controllerShape(
                FacilityLargePropStructure.Kind.SCP_914_NOTICE,
                state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Scp914UsageNoticeBlockEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
            BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (level.isClientSide || oldState.getBlock() == this) return;
        Direction facing = state.getValue(FACING);
        if (!FacilityLargePropStructure.placeParts(level, pos,
                FacilityLargePropStructure.Kind.SCP_914_NOTICE, facing)) {
            level.destroyBlock(pos, true);
            return;
        }
        level.scheduleTick(pos, this, 1);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random) {
        FacilityLargePropStructure.ensureParts(level, pos,
                FacilityLargePropStructure.Kind.SCP_914_NOTICE,
                state.getValue(FACING));
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random) {
        FacilityLargePropStructure.ensureParts(level, pos,
                FacilityLargePropStructure.Kind.SCP_914_NOTICE,
                state.getValue(FACING));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide) {
            FacilityLargePropStructure.removeParts(level, pos,
                    FacilityLargePropStructure.Kind.SCP_914_NOTICE,
                    state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state,
            LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(this));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target,
            BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(this);
    }
}
''', encoding="utf-8")

Path("src/main/java/net/mcreator/scpadditions/facility/Scp914UsageNoticeBlockEntity.java").write_text(r'''package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Render-only block entity for the static SCP-914 usage notice. */
public final class Scp914UsageNoticeBlockEntity extends BlockEntity {
    public Scp914UsageNoticeBlockEntity(BlockPos pos, BlockState state) {
        super(FacilityModule.SCP_914_NOTICE_BLOCK_ENTITY.get(), pos, state);
    }
}
''', encoding="utf-8")

Path("src/main/java/net/mcreator/scpadditions/client/Scp914UsageNoticeBlockEntityRenderer.java").write_text(r'''package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.Scp914UsageNoticeBlock;
import net.mcreator.scpadditions.facility.Scp914UsageNoticeBlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws the completed SCP-914 notice directly behind the support glass. */
public final class Scp914UsageNoticeBlockEntityRenderer
        implements BlockEntityRenderer<Scp914UsageNoticeBlockEntity> {
    private static final ResourceLocation NOTICE = new ResourceLocation(
            ScpAdditionsMod.MODID,
            "textures/screens/scpsign/914-notice.png");
    private static final float PANEL_MIN_X = 8.2F / 16.0F;
    private static final float PANEL_MAX_X = 23.7F / 16.0F;
    private static final float PANEL_MIN_Y = -12.85F / 16.0F;
    private static final float PANEL_MAX_Y = -3.15F / 16.0F;
    private static final float IMAGE_Z = 15.86F / 16.0F;

    public Scp914UsageNoticeBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Scp914UsageNoticeBlockEntity notice, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            int packedOverlay) {
        BlockState state = notice.getBlockState();
        if (!(state.getBlock() instanceof Scp914UsageNoticeBlock)) return;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees(
                state.getValue(Scp914UsageNoticeBlock.FACING))));
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        VertexConsumer consumer = buffer.getBuffer(
                RenderType.entityTranslucent(NOTICE));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        vertex(consumer, matrix, normal, PANEL_MAX_X, PANEL_MAX_Y, 0.0F, 0.0F,
                packedLight);
        vertex(consumer, matrix, normal, PANEL_MAX_X, PANEL_MIN_Y, 0.0F, 1.0F,
                packedLight);
        vertex(consumer, matrix, normal, PANEL_MIN_X, PANEL_MIN_Y, 1.0F, 1.0F,
                packedLight);
        vertex(consumer, matrix, normal, PANEL_MIN_X, PANEL_MAX_Y, 1.0F, 0.0F,
                packedLight);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
            Matrix3f normal, float x, float y, float u, float v,
            int packedLight) {
        consumer.vertex(matrix, x, y, IMAGE_Z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, -1.0F)
                .endVertex();
    }

    private static float rotationDegrees(Direction direction) {
        return switch (direction) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }
}
''', encoding="utf-8")

resources = {
    "src/main/resources/assets/scp_additions/blockstates/scp_914_usage_notice.json": '''{
  "multipart": [
    {"when": {"facing": "north"}, "apply": {"model": "scp_additions:block/scp_914_usage_notice"}},
    {"when": {"facing": "east"}, "apply": {"model": "scp_additions:block/scp_914_usage_notice", "y": 90}},
    {"when": {"facing": "south"}, "apply": {"model": "scp_additions:block/scp_914_usage_notice", "y": 180}},
    {"when": {"facing": "west"}, "apply": {"model": "scp_additions:block/scp_914_usage_notice", "y": 270}}
  ]
}
''',
    "src/main/resources/assets/scp_additions/models/block/scp_914_usage_notice.json": '''{
  "parent": "scp_unity_extra_blocks:block/sign_support"
}
''',
    "src/main/resources/assets/scp_additions/models/item/scp_914_usage_notice.json": '''{
  "parent": "scp_additions:block/scp_914_usage_notice"
}
'''
}
for path, content in resources.items():
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")
