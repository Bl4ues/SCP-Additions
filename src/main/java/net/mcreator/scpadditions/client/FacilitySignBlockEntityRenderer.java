package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.scpadditions.facility.FacilitySignBlock;
import net.mcreator.scpadditions.facility.FacilitySignBlockEntity;
import net.mcreator.scpadditions.facility.FacilitySignData;

import java.util.List;

public final class FacilitySignBlockEntityRenderer
        implements BlockEntityRenderer<FacilitySignBlockEntity> {
    private static final int CORE_TEXT = 0x3B4247;
    private static final int CORE_OUTLINE = 0xF3F5F6;
    private static final int DOOR_TEXT = 0xF5F7F8;
    private static final float FONT_HEIGHT = 9.0F;

    private final Font font;

    public FacilitySignBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(FacilitySignBlockEntity sign, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            int packedOverlay) {
        BlockState state = sign.getBlockState();
        if (!(state.getBlock() instanceof FacilitySignBlock block)) return;
        List<FacilitySignData.Entry> entries = sign.entries();

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                rotationDegrees(state.getValue(FacilitySignBlock.FACING))));
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        if (block.type() == FacilitySignBlock.SignType.CORE_ROOM) {
            renderCoreRoom(entries, poseStack, buffer, packedLight);
        } else {
            renderDoor(entries, poseStack, buffer);
        }
        poseStack.popPose();
    }

    private void renderCoreRoom(List<FacilitySignData.Entry> entries,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Centers of line_1, line_2 and line_3 in the supplied model.
        float[] centersY = {15.5F, 9.5F, 3.5F};
        for (int row = 0; row < FacilitySignData.ENTRY_COUNT; row++) {
            String value = FacilitySignData.cleanText(
                    FacilitySignBlock.SignType.CORE_ROOM,
                    entries.get(row).text());
            if (value.isEmpty()) continue;
            Component component = ScpFonts.liberationSans(value);
            renderOutlinedCentered(component,
                    0.0F,
                    centersY[row] / 16.0F,
                    15.47F / 16.0F,
                    20.0F / 16.0F,
                    3.1F / 16.0F,
                    poseStack, buffer, packedLight);
        }
    }

    private void renderDoor(List<FacilitySignData.Entry> entries,
            PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();
        poseStack.translate(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-22.5F));
        poseStack.translate(-1.0F / 16.0F, -1.0F / 16.0F, -1.0F / 16.0F);

        float[] centersY = {3.0F, 1.0F, -1.0F};
        for (int row = 0; row < FacilitySignData.ENTRY_COUNT; row++) {
            FacilitySignData.Entry entry = FacilitySignData.sanitize(
                    FacilitySignBlock.SignType.DOOR, entries.get(row));
            if (!entry.number().isEmpty()) {
                renderFullBrightCentered(
                        ScpFonts.doorSignNumbers(entry.number()),
                        16.0F / 16.0F,
                        centersY[row] / 16.0F,
                        14.27F / 16.0F,
                        1.8F / 16.0F,
                        1.3F / 16.0F,
                        poseStack, buffer);
            }
            if (!entry.text().isEmpty()) {
                renderFullBrightLeft(
                        ScpFonts.anonymousPro(entry.text()),
                        -0.65F / 16.0F,
                        centersY[row] / 16.0F,
                        14.27F / 16.0F,
                        14.9F / 16.0F,
                        1.3F / 16.0F,
                        poseStack, buffer);
            }
        }
        poseStack.popPose();
    }

    private void renderOutlinedCentered(Component component, float centerX,
            float centerY, float z, float maxWidth, float maxHeight,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        FormattedCharSequence sequence = component.getVisualOrderText();
        float width = Math.max(1.0F, font.width(sequence));
        float scale = Math.min(maxWidth / width, maxHeight / FONT_HEIGHT);

        poseStack.pushPose();
        poseStack.translate(centerX, centerY, z);
        poseStack.scale(scale, -scale, scale);
        font.drawInBatch8xOutline(sequence, -width / 2.0F,
                -FONT_HEIGHT / 2.0F, CORE_TEXT, CORE_OUTLINE,
                poseStack.last().pose(), buffer, packedLight);
        poseStack.popPose();
    }

    private void renderFullBrightCentered(Component component, float centerX,
            float centerY, float z, float maxWidth, float maxHeight,
            PoseStack poseStack, MultiBufferSource buffer) {
        FormattedCharSequence sequence = component.getVisualOrderText();
        float width = Math.max(1.0F, font.width(sequence));
        renderFullBright(sequence, -width / 2.0F, centerX, centerY, z,
                Math.min(maxWidth / width, maxHeight / FONT_HEIGHT),
                poseStack, buffer);
    }

    private void renderFullBrightLeft(Component component, float leftX,
            float centerY, float z, float maxWidth, float maxHeight,
            PoseStack poseStack, MultiBufferSource buffer) {
        FormattedCharSequence sequence = component.getVisualOrderText();
        float width = Math.max(1.0F, font.width(sequence));
        renderFullBright(sequence, 0.0F, leftX, centerY, z,
                Math.min(maxWidth / width, maxHeight / FONT_HEIGHT),
                poseStack, buffer);
    }

    private void renderFullBright(FormattedCharSequence sequence, float x,
            float originX, float originY, float z, float scale,
            PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();
        poseStack.translate(originX, originY, z);
        poseStack.scale(scale, -scale, scale);
        font.drawInBatch(sequence, x, -FONT_HEIGHT / 2.0F,
                DOOR_TEXT, false, poseStack.last().pose(), buffer,
                Font.DisplayMode.POLYGON_OFFSET, 0,
                LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static float rotationDegrees(Direction direction) {
        return switch (direction) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }
}
