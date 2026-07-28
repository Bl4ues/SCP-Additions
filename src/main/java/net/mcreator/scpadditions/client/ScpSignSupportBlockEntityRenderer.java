package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.ScpSignData;
import net.mcreator.scpadditions.facility.ScpSignHazards;
import net.mcreator.scpadditions.facility.ScpSignSupportBlock;
import net.mcreator.scpadditions.facility.ScpSignSupportBlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Renders the configurable paper directly behind the Sign Support glass. */
public final class ScpSignSupportBlockEntityRenderer
        implements BlockEntityRenderer<ScpSignSupportBlockEntity> {
    private static final ResourceLocation BASE = new ResourceLocation(
            ScpAdditionsMod.MODID,
            "textures/screens/scpsign/scp_sign_base.png");

    private static final float IMAGE_WIDTH = 1024.0F;
    private static final float IMAGE_HEIGHT = 640.0F;
    private static final float PANEL_MIN_X = 8.2F / 16.0F;
    private static final float PANEL_MAX_X = 23.7F / 16.0F;
    private static final float PANEL_MIN_Y = -12.85F / 16.0F;
    private static final float PANEL_MAX_Y = -3.15F / 16.0F;
    private static final float PANEL_WIDTH = PANEL_MAX_X - PANEL_MIN_X;
    private static final float PANEL_HEIGHT = PANEL_MAX_Y - PANEL_MIN_Y;
    private static final float BASE_Z = 15.86F / 16.0F;
    private static final float CONTENT_Z = 15.83F / 16.0F;
    private static final float FONT_HEIGHT = 9.0F;
    private static final int TEXT_COLOR = 0xFF000000;

    private static final ImageArea CLEARANCE =
            new ImageArea(783.0F, 83.0F, 57.0F, 40.0F);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64.0F, 273.0F, 355.0F, 48.0F);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65.0F, 351.0F, 354.0F, 27.0F);
    private static final ImageArea ANOMALY =
            new ImageArea(589.0F, 298.0F, 235.0F, 15.0F);
    private static final ImageArea[] HAZARDS = {
            new ImageArea(473.0F, 375.0F, 167.0F, 164.0F),
            new ImageArea(622.0F, 375.0F, 166.0F, 164.0F),
            new ImageArea(771.0F, 375.0F, 167.0F, 164.0F)
    };

    private final Font font;

    public ScpSignSupportBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
        font = context.getFont();
    }

    @Override
    public void render(ScpSignSupportBlockEntity sign, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            int packedOverlay) {
        BlockState state = sign.getBlockState();
        if (!(state.getBlock() instanceof ScpSignSupportBlock)) return;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees(
                state.getValue(ScpSignSupportBlock.FACING))));
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        renderImage(BASE, new ImageArea(0.0F, 0.0F,
                IMAGE_WIDTH, IMAGE_HEIGHT), BASE_Z,
                poseStack, buffer, packedLight);

        ScpSignData data = sign.data();
        for (int slot = 0; slot < ScpSignData.HAZARD_SLOTS; slot++) {
            ScpSignHazards.Option option = ScpSignHazards.option(
                    data.hazards().get(slot));
            ResourceLocation texture = option.texture();
            if (!resourceExists(texture)) {
                texture = ScpSignHazards.NONE.texture();
            }
            if (resourceExists(texture)) {
                renderImage(texture, HAZARDS[slot], CONTENT_Z,
                        poseStack, buffer, packedLight);
            }
        }

        renderText(String.format("%02d", data.clearanceLevel()), CLEARANCE,
                true, poseStack, buffer, packedLight);
        renderText(data.scpLabel(), SCP_NUMBER, false,
                poseStack, buffer, packedLight);
        renderText(data.containmentLabel(), CONTAINMENT, false,
                poseStack, buffer, packedLight);
        renderText(data.anomalyLabel(), ANOMALY, true,
                poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    private void renderText(String value, ImageArea area, boolean centered,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Component component = ScpFonts.scpSign(value);
        FormattedCharSequence sequence = component.getVisualOrderText();
        float textWidth = Math.max(1.0F, font.width(sequence));
        float scale = Math.min(
                area.width() * PANEL_WIDTH / IMAGE_WIDTH / textWidth,
                area.height() * PANEL_HEIGHT / IMAGE_HEIGHT / FONT_HEIGHT);
        float imageX = centered ? area.x() + area.width() * 0.5F : area.x();

        poseStack.pushPose();
        poseStack.translate(panelX(imageX), panelY(area.y()), CONTENT_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(scale, -scale, scale);
        font.drawInBatch(sequence, centered ? -textWidth * 0.5F : 0.0F,
                0.0F, TEXT_COLOR, false, poseStack.last().pose(), buffer,
                Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderImage(ResourceLocation texture, ImageArea area,
            float z, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight) {
        float left = panelX(area.x());
        float right = panelX(area.x() + area.width());
        float top = panelY(area.y());
        float bottom = panelY(area.y() + area.height());

        VertexConsumer consumer = buffer.getBuffer(
                RenderType.entityTranslucent(texture));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        vertex(consumer, matrix, normal, left, top, z,
                0.0F, 0.0F, packedLight);
        vertex(consumer, matrix, normal, left, bottom, z,
                0.0F, 1.0F, packedLight);
        vertex(consumer, matrix, normal, right, bottom, z,
                1.0F, 1.0F, packedLight);
        vertex(consumer, matrix, normal, right, top, z,
                1.0F, 0.0F, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
            Matrix3f normal, float x, float y, float z, float u, float v,
            int packedLight) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, -1.0F)
                .endVertex();
    }

    private static boolean resourceExists(ResourceLocation texture) {
        return Minecraft.getInstance().getResourceManager()
                .getResource(texture).isPresent();
    }

    private static float panelX(float imageX) {
        return PANEL_MAX_X - imageX / IMAGE_WIDTH * PANEL_WIDTH;
    }

    private static float panelY(float imageY) {
        return PANEL_MAX_Y - imageY / IMAGE_HEIGHT * PANEL_HEIGHT;
    }

    private static float rotationDegrees(Direction direction) {
        return switch (direction) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    private record ImageArea(float x, float y, float width, float height) {
    }
}
