package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.mcreator.scpadditions.document.DocumentData;

/** Renders the official 1365x2048 document template in previews and the Codex. */
public final class DocumentRenderer {
    public static final int PAGE_WIDTH = 1365;
    public static final int PAGE_HEIGHT = 2048;

    private static final ResourceLocation TEMPLATE = new ResourceLocation(
            "scp_additions", "textures/gui/document_template.png");
    private static final ResourceLocation PICTURE_FRAME = new ResourceLocation(
            "scp_additions", "textures/gui/picture.png");

    // The supplied coordinates were measured on a 1536x2304 canvas. The
    // exported template is exactly 8/9 of that size on both axes.
    private static final int HEADER_LEFT_X = scaled(462);
    private static final int HEADER_RIGHT_X = scaled(1034);
    private static final int HEADER_ROW_1_Y = scaled(105);
    private static final int HEADER_ROW_2_Y = scaled(217);
    private static final int HEADER_ROW_3_Y = scaled(329);

    private static final int BODY_X = scaled(54);
    private static final int BODY_Y = scaled(522);
    private static final int BODY_RIGHT = scaled(1439);
    private static final int BODY_BOTTOM = scaled(2031);

    private static final int PHOTO_X = scaled(864);
    private static final int PHOTO_Y = scaled(478);
    private static final int PHOTO_RIGHT = scaled(1471);
    private static final int PHOTO_BOTTOM = scaled(1084);
    private static final int CAPTION_TOP = PHOTO_BOTTOM;
    private static final int CAPTION_BOTTOM = scaled(1145);

    private static final float HEADER_SCALE = 3.2F;
    private static final float BODY_SCALE = 2.75F;
    private static final int BODY_LINE_HEIGHT = 35;
    private static final int TEXT_COLOR = 0xFF303030;

    private DocumentRenderer() {
    }

    public static void render(GuiGraphics graphics, ItemStack stack,
                              int areaX, int areaY, int areaWidth,
                              int areaHeight) {
        DocumentData.State state = DocumentData.read(stack);
        int[] fitted = fitRect(PAGE_WIDTH, PAGE_HEIGHT, areaWidth, areaHeight);
        int pageX = areaX + (areaWidth - fitted[0]) / 2;
        int pageY = areaY + (areaHeight - fitted[1]) / 2;
        float scale = fitted[0] / (float) PAGE_WIDTH;

        graphics.enableScissor(areaX, areaY, areaX + areaWidth,
                areaY + areaHeight);
        graphics.pose().pushPose();
        graphics.pose().translate(pageX, pageY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);

        setTextureFiltering(TEMPLATE, true);
        graphics.blit(TEMPLATE, 0, 0, PAGE_WIDTH, PAGE_HEIGHT,
                0.0F, 0.0F, PAGE_WIDTH, PAGE_HEIGHT,
                PAGE_WIDTH, PAGE_HEIGHT);
        setTextureFiltering(TEMPLATE, false);

        boolean hasPhoto = !state.photoKey().isBlank()
                && state.photoWidth() > 0 && state.photoHeight() > 0;
        if (hasPhoto) drawPhoto(graphics, state);

        drawHeader(graphics, state.header1(), state.value1(), HEADER_ROW_1_Y);
        drawHeader(graphics, state.header2(), state.value2(), HEADER_ROW_2_Y);
        drawHeader(graphics, state.header3(), state.value3(), HEADER_ROW_3_Y);

        int fullBodyWidth = Math.max(1, BODY_RIGHT - BODY_X);
        MarkdownTextRenderer.render(graphics, state.body(), BODY_X, BODY_Y,
                BODY_BOTTOM, BODY_LINE_HEIGHT, BODY_SCALE, y -> {
                    if (hasPhoto && y < CAPTION_BOTTOM + 20) {
                        return Math.max(80, PHOTO_X - BODY_X - 28);
                    }
                    return fullBodyWidth;
                }, TEXT_COLOR);

        graphics.pose().popPose();
        graphics.disableScissor();
    }

    private static void drawHeader(GuiGraphics graphics, String label,
                                   String value, int y) {
        if (label != null && !label.isBlank()) {
            Component text = ScpFonts.roboto(label)
                    .withStyle(style -> style.withBold(true));
            drawScaledText(graphics, text, HEADER_LEFT_X + 8, y,
                    TEXT_COLOR, HEADER_SCALE);
        }
        if (value != null && !value.isBlank()) {
            Component text = ScpFonts.roboto(value);
            int width = Math.round(Minecraft.getInstance().font.width(text)
                    * HEADER_SCALE);
            drawScaledText(graphics, text, HEADER_RIGHT_X - 8 - width, y,
                    TEXT_COLOR, HEADER_SCALE);
        }
    }

    private static void drawPhoto(GuiGraphics graphics,
                                  DocumentData.State state) {
        ResourceLocation photo = CodexAssetClient.getTexture(state.photoKey())
                .orElse(null);
        int frameW = PHOTO_RIGHT - PHOTO_X;
        int frameH = PHOTO_BOTTOM - PHOTO_Y;
        if (photo != null) {
            int sourceW = state.photoWidth();
            int sourceH = state.photoHeight();
            float targetAspect = frameW / (float) frameH;
            float sourceAspect = sourceW / (float) sourceH;
            int cropW = sourceW;
            int cropH = sourceH;
            int u = 0;
            int v = 0;
            if (sourceAspect > targetAspect) {
                cropW = Math.max(1, Math.round(sourceH * targetAspect));
                u = Math.max(0, (sourceW - cropW) / 2);
            } else if (sourceAspect < targetAspect) {
                cropH = Math.max(1, Math.round(sourceW / targetAspect));
                v = Math.max(0, (sourceH - cropH) / 2);
            }
            setTextureFiltering(photo, true);
            graphics.blit(photo, PHOTO_X, PHOTO_Y, frameW, frameH,
                    (float) u, (float) v, cropW, cropH, sourceW, sourceH);
            setTextureFiltering(photo, false);
        }

        setTextureFiltering(PICTURE_FRAME, true);
        graphics.blit(PICTURE_FRAME, 0, 0, PAGE_WIDTH, PAGE_HEIGHT,
                0.0F, 0.0F, PAGE_WIDTH, PAGE_HEIGHT,
                PAGE_WIDTH, PAGE_HEIGHT);
        setTextureFiltering(PICTURE_FRAME, false);

        if (!state.caption().isBlank()) {
            Component caption = ScpFonts.roboto(state.caption())
                    .withStyle(style -> style.withItalic(true));
            float captionScale = 2.15F;
            int width = Math.round(Minecraft.getInstance().font.width(caption)
                    * captionScale);
            int x = PHOTO_X + Math.max(8, (frameW - width) / 2);
            int y = CAPTION_TOP + Math.max(3,
                    (CAPTION_BOTTOM - CAPTION_TOP
                            - Math.round(Minecraft.getInstance().font.lineHeight
                            * captionScale)) / 2);
            drawScaledText(graphics, caption, x, y, TEXT_COLOR,
                    captionScale);
        }
    }

    private static void drawScaledText(GuiGraphics graphics,
                                       Component component, int x, int y,
                                       int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(Minecraft.getInstance().font, component,
                0, 0, color, false);
        graphics.pose().popPose();
    }

    private static void setTextureFiltering(ResourceLocation texture,
                                            boolean blur) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.getTextureManager().getTexture(texture)
                    .setFilter(blur, false);
        }
    }

    private static int scaled(int original) {
        return Math.round(original * (8.0F / 9.0F));
    }

    private static int[] fitRect(int sourceWidth, int sourceHeight,
                                 int maxWidth, int maxHeight) {
        float scale = Math.min(maxWidth / (float) sourceWidth,
                maxHeight / (float) sourceHeight);
        return new int[]{Math.max(1, Math.round(sourceWidth * scale)),
                Math.max(1, Math.round(sourceHeight * scale))};
    }
}
