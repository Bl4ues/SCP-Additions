package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.inventory.item.CodexDocumentDefinition;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemClassifier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.document.DocumentData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Renders the authored thin 3D Document model while allowing the north/front
 * face to show the actual visual contents carried by each document stack.
 */
public final class DocumentItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation BASE_TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/item/document.png");
    private static final ResourceLocation TEMPLATE_TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/gui/document_template.png");
    private static final ResourceLocation PICTURE_FRAME = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/gui/picture.png");

    private static final int PAGE_WIDTH = DocumentRenderer.PAGE_WIDTH;
    private static final int PAGE_HEIGHT = DocumentRenderer.PAGE_HEIGHT;

    private static final int HEADER_LEFT_X = 470;
    private static final int HEADER_RIGHT_X = 1026;
    private static final int HEADER_ROW_1_Y = 108;
    private static final int HEADER_ROW_2_Y = 220;
    private static final int HEADER_ROW_3_Y = 332;

    private static final int BODY_X = 72;
    private static final int BODY_Y = 529;
    private static final int BODY_RIGHT = 1421;
    private static final int BODY_BOTTOM = 2023;

    private static final int PHOTO_X = 864;
    private static final int PHOTO_Y = 478;
    private static final int PHOTO_RIGHT = 1471;
    private static final int PHOTO_BOTTOM = 1084;
    private static final int PHOTO_RENDER_BOTTOM = PHOTO_BOTTOM + 7;
    private static final int CAPTION_TOP = PHOTO_BOTTOM;
    private static final int CAPTION_BOTTOM = 1145;

    private static final float HEADER_SCALE = 4.0F;
    private static final float BODY_SCALE = 3.72F;
    private static final float CAPTION_SCALE = 3.55F;
    private static final int BODY_LINE_HEIGHT = 40;
    private static final int TEXT_COLOR = 0xFF303030;

    // Authored Blockbench cuboid, normalized from 0..16 model coordinates.
    private static final float X0 = 5.75F / 16.0F;
    private static final float X1 = 10.25F / 16.0F;
    private static final float Y0 = 3.25F / 16.0F;
    private static final float Y1 = 10.75F / 16.0F;
    private static final float Z0 = 8.25F / 16.0F;
    private static final float Z1 = 8.30F / 16.0F;

    private static final float PHOTO_DEPTH = 0.00030F;
    private static final float FRAME_DEPTH = 0.00055F;
    private static final float TEXT_DEPTH = 0.00085F;

    public DocumentItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
            PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        ResourceLocation frontTexture = resolveFrontTexture(stack);
        PoseStack.Pose pose = poseStack.last();

        VertexConsumer base = bufferSource.getBuffer(
                RenderType.entityCutoutNoCull(BASE_TEXTURE));

        // Every non-front face always keeps the authored Blank Document skin.
        east(base, pose, packedLight, packedOverlay,
                0.0F, 8.0F, 0.5F, 15.5F);
        south(base, pose, packedLight, packedOverlay,
                5.0F, 0.0F, 9.5F, 7.5F);
        west(base, pose, packedLight, packedOverlay,
                1.0F, 8.0F, 1.5F, 15.5F);
        up(base, pose, packedLight, packedOverlay,
                6.5F, 8.5F, 2.0F, 8.0F);
        down(base, pose, packedLight, packedOverlay,
                11.5F, 8.0F, 7.0F, 8.5F);

        if (frontTexture == null) {
            north(base, pose, packedLight, packedOverlay,
                    0.0F, 0.0F, 4.5F, 7.5F, false, Z0);
            return;
        }

        VertexConsumer front = bufferSource.getBuffer(
                RenderType.entityTranslucent(frontTexture));
        north(front, pose, packedLight, packedOverlay,
                0.0F, 0.0F, 16.0F, 16.0F, true, Z0);

        if (DocumentData.hasStructuredData(stack)) {
            DocumentData.State state = DocumentData.read(stack);
            if (hasTemplateAppearance(state)) {
                renderStructuredFront(state, poseStack, bufferSource,
                        packedLight, packedOverlay);
            }
        }
    }

    private static ResourceLocation resolveFrontTexture(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        if (DocumentData.hasStructuredData(stack)) {
            DocumentData.State state = DocumentData.read(stack);
            if (hasTemplateAppearance(state)) return TEMPLATE_TEXTURE;
            return null;
        }

        Optional<CodexDocumentDefinition> configured =
                ScpItemClassifier.getCodexDocument(stack);
        if (configured.isEmpty()) return null;

        CodexDocumentDefinition definition = configured.get();
        if (!definition.getWorldImageKey().isBlank()) {
            ResourceLocation worldTexture = CodexAssetClient
                    .getTexture(definition.getWorldImageKey()).orElse(null);
            if (worldTexture != null) return worldTexture;
        }
        return definition.getImageLocation().orElse(null);
    }

    private static void renderStructuredFront(DocumentData.State state,
            PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        boolean hasPhoto = !state.photoKey().isBlank()
                && state.photoWidth() > 0 && state.photoHeight() > 0;

        if (hasPhoto) {
            ResourceLocation photo = CodexAssetClient.getTexture(state.photoKey())
                    .orElse(null);
            if (photo != null) {
                renderPhoto(photo, state, poseStack.last(), bufferSource,
                        packedLight, packedOverlay);
            }

            VertexConsumer frame = bufferSource.getBuffer(
                    RenderType.entityTranslucent(PICTURE_FRAME));
            north(frame, poseStack.last(), packedLight, packedOverlay,
                    0.0F, 0.0F, 16.0F, 16.0F, true,
                    Z0 - FRAME_DEPTH);
        }

        renderStructuredText(state, hasPhoto, poseStack, bufferSource,
                packedLight);
    }

    private static void renderPhoto(ResourceLocation photo,
            DocumentData.State state, PoseStack.Pose pose,
            MultiBufferSource bufferSource, int packedLight,
            int packedOverlay) {
        int sourceWidth = Math.max(1, state.photoWidth());
        int sourceHeight = Math.max(1, state.photoHeight());
        int frameWidth = PHOTO_RIGHT - PHOTO_X;
        int frameHeight = PHOTO_RENDER_BOTTOM - PHOTO_Y;
        float targetAspect = frameWidth / (float) frameHeight;
        float sourceAspect = sourceWidth / (float) sourceHeight;
        int cropWidth = sourceWidth;
        int cropHeight = sourceHeight;
        int u = 0;
        int v = 0;

        if (sourceAspect > targetAspect) {
            cropWidth = Math.max(1,
                    (int) Math.ceil(sourceHeight * targetAspect));
            u = Math.max(0, (sourceWidth - cropWidth) / 2);
        } else if (sourceAspect < targetAspect) {
            cropHeight = Math.max(1,
                    (int) Math.ceil(sourceWidth / targetAspect));
            v = Math.max(0, (sourceHeight - cropHeight) / 2);
        }

        float u0 = u / (float) sourceWidth;
        float v0 = v / (float) sourceHeight;
        float u1 = (u + cropWidth) / (float) sourceWidth;
        float v1 = (v + cropHeight) / (float) sourceHeight;
        VertexConsumer out = bufferSource.getBuffer(
                RenderType.entityTranslucent(photo));
        frontPageRect(out, pose, packedLight, packedOverlay,
                PHOTO_X, PHOTO_Y, PHOTO_RIGHT, PHOTO_RENDER_BOTTOM,
                u0, v0, u1, v1, Z0 - PHOTO_DEPTH);
    }

    private static void renderStructuredText(DocumentData.State state,
            boolean hasPhoto, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        Font font = Minecraft.getInstance().font;

        poseStack.pushPose();
        /*
         * The north face is viewed with model +X running screen-right to
         * screen-left. Start at its visual top-left corner, turn font quads
         * toward the north face, and map the document's 1497x2246 page space
         * directly onto the authored 4.5x7.5 model-pixel front.
         */
        poseStack.translate(X1, Y1, Z0 - TEXT_DEPTH);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale((X1 - X0) / PAGE_WIDTH,
                -(Y1 - Y0) / PAGE_HEIGHT, 1.0F);

        drawHeader(font, state.header1(), state.value1(), HEADER_ROW_1_Y,
                poseStack, bufferSource, packedLight);
        drawHeader(font, state.header2(), state.value2(), HEADER_ROW_2_Y,
                poseStack, bufferSource, packedLight);
        drawHeader(font, state.header3(), state.value3(), HEADER_ROW_3_Y,
                poseStack, bufferSource, packedLight);

        List<String> bodyLines = wrapBody(font, state.body(), hasPhoto);
        int line = 0;
        for (String bodyLine : bodyLines) {
            int y = BODY_Y + line * BODY_LINE_HEIGHT;
            if (y + BODY_LINE_HEIGHT > BODY_BOTTOM) break;
            if (!bodyLine.isEmpty()) {
                drawScaled(font, ScpFonts.roboto(bodyLine).getVisualOrderText(),
                        BODY_X, y, BODY_SCALE, TEXT_COLOR,
                        poseStack, bufferSource, packedLight, false);
            }
            line++;
        }

        if (hasPhoto && !state.caption().isBlank()) {
            FormattedCharSequence caption = ScpFonts.roboto(state.caption())
                    .withStyle(style -> style.withItalic(true))
                    .getVisualOrderText();
            float fittedScale = CAPTION_SCALE;
            float width = font.width(caption) * fittedScale;
            float maxWidth = PHOTO_RIGHT - PHOTO_X - 28.0F;
            if (width > maxWidth && width > 0.0F) {
                fittedScale *= maxWidth / width;
                width = maxWidth;
            }
            float x = PHOTO_X + ((PHOTO_RIGHT - PHOTO_X) - width) / 2.0F;
            float textHeight = font.lineHeight * fittedScale;
            float y = CAPTION_TOP
                    + (CAPTION_BOTTOM - CAPTION_TOP - textHeight) / 2.0F + 6.0F;
            drawScaled(font, caption, x, y, fittedScale, TEXT_COLOR,
                    poseStack, bufferSource, packedLight, false);
        }

        poseStack.popPose();
    }

    private static void drawHeader(Font font, String label, String value,
            int y, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight) {
        if (label != null && !label.isBlank()) {
            drawScaled(font, ScpFonts.roboto(label).getVisualOrderText(),
                    HEADER_LEFT_X, y, HEADER_SCALE, TEXT_COLOR,
                    poseStack, bufferSource, packedLight, true);
        }
        if (value != null && !value.isBlank()) {
            FormattedCharSequence sequence =
                    ScpFonts.roboto(value).getVisualOrderText();
            float width = font.width(sequence) * HEADER_SCALE;
            drawScaled(font, sequence, HEADER_RIGHT_X - width, y,
                    HEADER_SCALE, TEXT_COLOR, poseStack, bufferSource,
                    packedLight, false);
        }
    }

    private static void drawScaled(Font font, FormattedCharSequence sequence,
            float x, float y, float scale, int color,
            PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, boolean bold) {
        poseStack.pushPose();
        poseStack.translate(x, y, 0.0F);
        poseStack.scale(scale, scale, 1.0F);
        font.drawInBatch(sequence, 0.0F, 0.0F, color, false,
                poseStack.last().pose(), bufferSource,
                Font.DisplayMode.NORMAL, 0, packedLight);
        if (bold) {
            poseStack.translate(0.34F, 0.0F, 0.01F);
            font.drawInBatch(sequence, 0.0F, 0.0F, color, false,
                    poseStack.last().pose(), bufferSource,
                    Font.DisplayMode.NORMAL, 0, packedLight);
        }
        poseStack.popPose();
    }

    private static List<String> wrapBody(Font font, String raw,
            boolean hasPhoto) {
        List<String> lines = new ArrayList<>();
        String text = stripMarkdown(raw);
        for (String paragraph : text.split("\\n", -1)) {
            if (paragraph.isBlank()) {
                lines.add("");
                continue;
            }

            String current = "";
            for (String word : paragraph.trim().split("\\s+")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                int maxPageWidth = bodyWidthForLine(lines.size(), hasPhoto);
                int maxFontWidth = Math.max(1,
                        Math.round(maxPageWidth / BODY_SCALE));
                if (current.isEmpty()
                        || font.width(ScpFonts.roboto(candidate)) <= maxFontWidth) {
                    current = candidate;
                } else {
                    lines.add(current);
                    current = word;
                }
            }
            if (!current.isEmpty()) lines.add(current);
        }
        return lines;
    }

    private static int bodyWidthForLine(int line, boolean hasPhoto) {
        int y = BODY_Y + line * BODY_LINE_HEIGHT;
        if (hasPhoto && y < CAPTION_BOTTOM + 20) {
            return Math.max(80, PHOTO_X - BODY_X - 28);
        }
        return Math.max(80, BODY_RIGHT - BODY_X);
    }

    private static String stripMarkdown(String value) {
        if (value == null || value.isBlank()) return "";
        return value.replace("**", "")
                .replace("__", "")
                .replace("~~", "")
                .replace("`", "")
                .replace("### ", "")
                .replace("## ", "")
                .replace("# ", "");
    }

    private static boolean hasTemplateAppearance(DocumentData.State state) {
        if (state == null) return false;
        if (state.template() != DocumentData.Template.BLANK_DOCUMENT) return true;
        return !blank(state.header1()) || !blank(state.value1())
                || !blank(state.header2()) || !blank(state.value2())
                || !blank(state.header3()) || !blank(state.value3())
                || !blank(state.body()) || !blank(state.photoKey())
                || !blank(state.caption());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static void north(VertexConsumer out, PoseStack.Pose pose,
            int light, int overlay, float u0, float v0, float u1, float v1,
            boolean fullTexture, float z) {
        /*
         * For a north-facing quad the viewer sees model +X on screen-left.
         * Assign the texture's right edge to X0 and left edge to X1 so the
         * authored page is readable instead of horizontally mirrored.
         */
        float atX0 = fullTexture ? 1.0F : uv(u1);
        float atX1 = fullTexture ? 0.0F : uv(u0);
        float top = fullTexture ? 0.0F : uv(v0);
        float bottom = fullTexture ? 1.0F : uv(v1);
        vertex(out, pose, X0, Y1, z, atX0, top, light, overlay, 0, 0, -1);
        vertex(out, pose, X0, Y0, z, atX0, bottom, light, overlay, 0, 0, -1);
        vertex(out, pose, X1, Y0, z, atX1, bottom, light, overlay, 0, 0, -1);
        vertex(out, pose, X1, Y1, z, atX1, top, light, overlay, 0, 0, -1);
    }

    private static void frontPageRect(VertexConsumer out, PoseStack.Pose pose,
            int light, int overlay, float pageLeft, float pageTop,
            float pageRight, float pageBottom,
            float u0, float v0, float u1, float v1, float z) {
        float faceWidth = X1 - X0;
        float faceHeight = Y1 - Y0;
        float visualLeftX = X1 - (pageLeft / PAGE_WIDTH) * faceWidth;
        float visualRightX = X1 - (pageRight / PAGE_WIDTH) * faceWidth;
        float topY = Y1 - (pageTop / PAGE_HEIGHT) * faceHeight;
        float bottomY = Y1 - (pageBottom / PAGE_HEIGHT) * faceHeight;

        vertex(out, pose, visualLeftX, topY, z,
                u0, v0, light, overlay, 0, 0, -1);
        vertex(out, pose, visualLeftX, bottomY, z,
                u0, v1, light, overlay, 0, 0, -1);
        vertex(out, pose, visualRightX, bottomY, z,
                u1, v1, light, overlay, 0, 0, -1);
        vertex(out, pose, visualRightX, topY, z,
                u1, v0, light, overlay, 0, 0, -1);
    }

    private static void south(VertexConsumer out, PoseStack.Pose pose,
            int light, int overlay, float u0, float v0, float u1, float v1) {
        vertex(out, pose, X1, Y1, Z1, uv(u0), uv(v0), light, overlay, 0, 0, 1);
        vertex(out, pose, X1, Y0, Z1, uv(u0), uv(v1), light, overlay, 0, 0, 1);
        vertex(out, pose, X0, Y0, Z1, uv(u1), uv(v1), light, overlay, 0, 0, 1);
        vertex(out, pose, X0, Y1, Z1, uv(u1), uv(v0), light, overlay, 0, 0, 1);
    }

    private static void east(VertexConsumer out, PoseStack.Pose pose,
            int light, int overlay, float u0, float v0, float u1, float v1) {
        vertex(out, pose, X1, Y1, Z0, uv(u0), uv(v0), light, overlay, 1, 0, 0);
        vertex(out, pose, X1, Y0, Z0, uv(u0), uv(v1), light, overlay, 1, 0, 0);
        vertex(out, pose, X1, Y0, Z1, uv(u1), uv(v1), light, overlay, 1, 0, 0);
        vertex(out, pose, X1, Y1, Z1, uv(u1), uv(v0), light, overlay, 1, 0, 0);
    }

    private static void west(VertexConsumer out, PoseStack.Pose pose,
            int light, int overlay, float u0, float v0, float u1, float v1) {
        vertex(out, pose, X0, Y1, Z1, uv(u0), uv(v0), light, overlay, -1, 0, 0);
        vertex(out, pose, X0, Y0, Z1, uv(u0), uv(v1), light, overlay, -1, 0, 0);
        vertex(out, pose, X0, Y0, Z0, uv(u1), uv(v1), light, overlay, -1, 0, 0);
        vertex(out, pose, X0, Y1, Z0, uv(u1), uv(v0), light, overlay, -1, 0, 0);
    }

    private static void up(VertexConsumer out, PoseStack.Pose pose,
            int light, int overlay, float u0, float v0, float u1, float v1) {
        vertex(out, pose, X0, Y1, Z1, uv(u0), uv(v0), light, overlay, 0, 1, 0);
        vertex(out, pose, X0, Y1, Z0, uv(u0), uv(v1), light, overlay, 0, 1, 0);
        vertex(out, pose, X1, Y1, Z0, uv(u1), uv(v1), light, overlay, 0, 1, 0);
        vertex(out, pose, X1, Y1, Z1, uv(u1), uv(v0), light, overlay, 0, 1, 0);
    }

    private static void down(VertexConsumer out, PoseStack.Pose pose,
            int light, int overlay, float u0, float v0, float u1, float v1) {
        vertex(out, pose, X0, Y0, Z0, uv(u0), uv(v0), light, overlay, 0, -1, 0);
        vertex(out, pose, X0, Y0, Z1, uv(u0), uv(v1), light, overlay, 0, -1, 0);
        vertex(out, pose, X1, Y0, Z1, uv(u1), uv(v1), light, overlay, 0, -1, 0);
        vertex(out, pose, X1, Y0, Z0, uv(u1), uv(v0), light, overlay, 0, -1, 0);
    }

    private static float uv(float modelUv) {
        return modelUv / 16.0F;
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose,
            float x, float y, float z, float u, float v,
            int light, int overlay, float nx, float ny, float nz) {
        out.vertex(pose.pose(), x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }
}
