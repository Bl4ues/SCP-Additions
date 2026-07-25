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
    private static final int CORE_TEXT = 0x252A2D;
    private static final int CORE_OUTLINE = 0x92999D;
    private static final int DOOR_TEXT = 0xF5F7F8;
    private static final float FONT_BASELINE_HEIGHT = 9.0F;
    private static final float MODEL_PIXEL = 1.0F / 16.0F;
    private static final float CORE_TEXT_SCALE = 0.0068F;
    private static final float DOOR_TEXT_SCALE = 0.0040F;
    private static final float DOOR_NUMBER_SCALE = 0.0075F;
    private static final float CORE_OUTLINE_OFFSET = 0.30F;
    private static final float CORE_FILL_DEPTH_OFFSET = 0.10F;
    private static final float CORE_BASELINE_Y =
            -FONT_BASELINE_HEIGHT / 2.0F + 1.30F;
    private static final float DOOR_NUMBER_BASELINE_Y =
            -FONT_BASELINE_HEIGHT / 2.0F + 1.0F;
    private static final float DOOR_TEXT_BASELINE_Y =
            -FONT_BASELINE_HEIGHT / 2.0F + 1.55F;
    private static final float DOOR_TEXT_RIGHT_SHIFT =
            -0.25F * MODEL_PIXEL;
    private static final float[][] OUTLINE_DIRECTIONS = {
            {-1.0F, -1.0F}, {0.0F, -1.0F}, {1.0F, -1.0F},
            {-1.0F, 0.0F},                    {1.0F, 0.0F},
            {-1.0F, 1.0F},  {0.0F, 1.0F},  {1.0F, 1.0F}
    };

    /*
     * These rectangles come directly from line_1..3 and number_1..3 in the
     * supplied Blockbench models. Those elements are authoring guides, not
     * visible model geometry. Keeping the values here makes that distinction
     * explicit and lets the renderer place text inside the intended areas.
     */
    private static final TextArea[] CORE_LINES = {
            new TextArea(-10.0F, 13.75F, 10.0F, 17.25F, 15.49F),
            new TextArea(-10.0F, 7.75F, 10.0F, 11.25F, 15.49F),
            new TextArea(-10.0F, 1.75F, 10.0F, 5.25F, 15.49F)
    };
    private static final TextArea[] DOOR_LINES = {
            new TextArea(-0.9F, 2.25F, 14.5F, 3.75F, 14.30F),
            new TextArea(-0.9F, 0.25F, 14.5F, 1.75F, 14.30F),
            new TextArea(-0.9F, -1.75F, 14.5F, -0.25F, 14.30F)
    };
    private static final TextArea[] DOOR_NUMBERS = {
            new TextArea(15.0F, 2.25F, 17.0F, 3.75F, 14.30F),
            new TextArea(15.0F, 0.25F, 17.0F, 1.75F, 14.30F),
            new TextArea(15.0F, -1.75F, 17.0F, -0.25F, 14.30F)
    };

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
        for (int row = 0; row < FacilitySignData.ENTRY_COUNT; row++) {
            String value = FacilitySignData.cleanText(
                    FacilitySignBlock.SignType.CORE_ROOM,
                    entries.get(row).text());
            if (value.isEmpty()) continue;
            renderOutlinedCentered(ScpFonts.liberationSans(value),
                    CORE_LINES[row], poseStack, buffer, packedLight);
        }
    }

    private void renderDoor(List<FacilitySignData.Entry> entries,
            PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();
        poseStack.translate(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-22.5F));
        poseStack.translate(-1.0F / 16.0F, -1.0F / 16.0F, -1.0F / 16.0F);

        for (int row = 0; row < FacilitySignData.ENTRY_COUNT; row++) {
            FacilitySignData.Entry entry = FacilitySignData.sanitize(
                    FacilitySignBlock.SignType.DOOR, entries.get(row));
            if (!entry.number().isEmpty()) {
                renderFullBrightCentered(
                        ScpFonts.doorSignNumbers(entry.number()),
                        DOOR_NUMBERS[row], poseStack, buffer);
            }
            if (!entry.text().isEmpty()) {
                renderFullBrightLeft(
                        ScpFonts.anonymousPro(entry.text()),
                        DOOR_LINES[row], poseStack, buffer);
            }
        }
        poseStack.popPose();
    }

    private void renderOutlinedCentered(Component component, TextArea area,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        FormattedCharSequence sequence = component.getVisualOrderText();
        float width = Math.max(1.0F, font.width(sequence));
        float x = -width / 2.0F;

        poseStack.pushPose();
        poseStack.translate(area.centerX(), area.centerY(), area.z());
        faceTextTowardSignFront(poseStack);
        poseStack.scale(CORE_TEXT_SCALE, -CORE_TEXT_SCALE, CORE_TEXT_SCALE);

        /*
         * Font.drawInBatch8xOutline uses a full font pixel of separation.
         * At this small world scale that border overwhelms the dark glyph.
         * Sub-pixel samples preserve the Unity-style edge without turning
         * the whole label into a bright, heavy stroke.
         */
        for (float[] direction : OUTLINE_DIRECTIONS) {
            font.drawInBatch(sequence,
                    x + direction[0] * CORE_OUTLINE_OFFSET,
                    CORE_BASELINE_Y
                            + direction[1] * CORE_OUTLINE_OFFSET,
                    CORE_OUTLINE, false, poseStack.last().pose(), buffer,
                    Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
        }
        /*
         * Keep the dark fill a fraction of a font pixel in front of the
         * outline. The world-space separation is below one model pixel, but
         * it gives the depth buffer an unambiguous ordering and removes the
         * speckling caused by two differently colored coplanar glyph layers.
         */
        poseStack.translate(0.0F, 0.0F, CORE_FILL_DEPTH_OFFSET);
        font.drawInBatch(sequence, x, CORE_BASELINE_Y, CORE_TEXT, false,
                poseStack.last().pose(), buffer,
                Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
        poseStack.popPose();
    }

    private void renderFullBrightCentered(Component component, TextArea area,
            PoseStack poseStack, MultiBufferSource buffer) {
        FormattedCharSequence sequence = component.getVisualOrderText();
        float width = Math.max(1.0F, font.width(sequence));
        renderFullBright(sequence, -width / 2.0F,
                area.centerX(), area.centerY(), area.z(),
                DOOR_NUMBER_SCALE, DOOR_NUMBER_BASELINE_Y,
                poseStack, buffer);
    }

    private void renderFullBrightLeft(Component component, TextArea area,
            PoseStack poseStack, MultiBufferSource buffer) {
        FormattedCharSequence sequence = component.getVisualOrderText();
        renderFullBright(sequence, 0.0F,
                area.right() + DOOR_TEXT_RIGHT_SHIFT,
                area.centerY(), area.z(),
                DOOR_TEXT_SCALE, DOOR_TEXT_BASELINE_Y,
                poseStack, buffer);
    }

    private void renderFullBright(FormattedCharSequence sequence, float x,
            float originX, float originY, float z, float scale,
            float baselineY,
            PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();
        poseStack.translate(originX, originY, z);
        faceTextTowardSignFront(poseStack);
        poseStack.scale(scale, -scale, scale);
        font.drawInBatch(sequence, x, baselineY,
                DOOR_TEXT, false, poseStack.last().pose(), buffer,
                Font.DisplayMode.POLYGON_OFFSET, 0,
                LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static void faceTextTowardSignFront(PoseStack poseStack) {
        /*
         * Font glyph quads face the opposite direction from the north faces
         * used by the supplied Blockbench models. Rotating around the text's
         * own origin changes only which side is visible: its anchor, scale,
         * guide-derived position and Door Sign inclination remain unchanged.
         */
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    private static float rotationDegrees(Direction direction) {
        /*
         * Blockstate JSON rotations use the baked-model convention. PoseStack
         * uses the opposite sign around Y, so east/west must be inverted or
         * the text lands on the opposite side of an otherwise correctly
         * rotated model.
         */
        return switch (direction) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    private record TextArea(float minX, float minY, float maxX, float maxY,
            float surfaceZ) {
        private float right() {
            return maxX * MODEL_PIXEL;
        }

        private float centerX() {
            return (minX + maxX) * 0.5F * MODEL_PIXEL;
        }

        private float centerY() {
            return (minY + maxY) * 0.5F * MODEL_PIXEL;
        }

        private float z() {
            return surfaceZ * MODEL_PIXEL;
        }

    }
}
