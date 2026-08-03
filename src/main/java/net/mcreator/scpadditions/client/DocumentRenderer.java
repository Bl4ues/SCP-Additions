package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.mcreator.scpadditions.document.DocumentData;

/** Renders the official document template in previews and the Codex. */
public final class DocumentRenderer {
    public static final int PAGE_WIDTH = 1497;
    public static final int PAGE_HEIGHT = 2246;

    private static final ResourceLocation TEMPLATE = new ResourceLocation(
            "scp_additions", "textures/gui/document_template.png");
    private static final ResourceLocation PICTURE_FRAME = new ResourceLocation(
            "scp_additions", "textures/gui/picture.png");

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
    private static final float BODY_SCALE = 3.58F;
    private static final int BODY_LINE_HEIGHT = 39;
    private static final int TEXT_COLOR = 0xFF303030;

    private DocumentRenderer() {
    }

    public static void render(GuiGraphics graphics, ItemStack stack,
                              int areaX, int areaY,
                              int areaWidth, int areaHeight) {
        DocumentData.State state = DocumentData.read(stack);
        int[] fitted = fitRect(PAGE_WIDTH, PAGE_HEIGHT, areaWidth, areaHeight);
        int pageX = areaX + (areaWidth - fitted[0]) / 2;
        int pageY = areaY + (areaHeight - fitted[1]) / 2;
        float pageScale = fitted[0] / (float) PAGE_WIDTH;

        graphics.enableScissor(areaX, areaY,
                areaX + areaWidth, areaY + areaHeight);
        graphics.pose().pushPose();
        graphics.pose().translate(pageX, pageY, 0.0F);
        graphics.pose().scale(pageScale, pageScale, 1.0F);

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
        MarkdownTextRenderer.render(graphics, state.body(),
                BODY_X, BODY_Y, BODY_BOTTOM,
                BODY_LINE_HEIGHT, BODY_SCALE, y -> {
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
            drawScaledText(graphics, ScpFonts.roboto(label),
                    HEADER_LEFT_X, y, TEXT_COLOR, HEADER_SCALE, true);
        }
        if (value != null && !value.isBlank()) {
            Component text = ScpFonts.roboto(value);
            int width = Math.round(Minecraft.getInstance().font.width(text)
                    * HEADER_SCALE);
            drawScaledText(graphics, text, HEADER_RIGHT_X - width, y,
                    TEXT_COLOR, HEADER_SCALE, false);
        }
    }

    private static void drawPhoto(GuiGraphics graphics,
                                  DocumentData.State state) {
        ResourceLocation photo = CodexAssetClient.getTexture(state.photoKey())
                .orElse(null);
        int frameWidth = PHOTO_RIGHT - PHOTO_X;
        int frameHeight = PHOTO_RENDER_BOTTOM - PHOTO_Y;

        if (photo != null) {
            int sourceWidth = state.photoWidth();
            int sourceHeight = state.photoHeight();
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

            setTextureFiltering(photo, true);
            graphics.blit(photo, PHOTO_X, PHOTO_Y, frameWidth, frameHeight,
                    (float) u, (float) v, cropWidth, cropHeight,
                    sourceWidth, sourceHeight);
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
            float captionScale = 3.15F;
            int width = Math.round(Minecraft.getInstance().font.width(caption)
                    * captionScale);
            int maxWidth = frameWidth - 28;
            float fittedScale = captionScale;
            if (width > maxWidth && width > 0) {
                fittedScale *= maxWidth / (float) width;
                width = maxWidth;
            }
            int x = PHOTO_X + (frameWidth - width) / 2;
            int textHeight = Math.round(
                    Minecraft.getInstance().font.lineHeight * fittedScale);
            int y = CAPTION_TOP
                    + (CAPTION_BOTTOM - CAPTION_TOP - textHeight) / 2 + 1;
            drawScaledText(graphics, caption, x, y, TEXT_COLOR,
                    fittedScale, false);
        }
    }

    private static void drawScaledText(GuiGraphics graphics,
                                       Component component,
                                       int x, int y, int color,
                                       float scale, boolean bold) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(Minecraft.getInstance().font,
                component, 0, 0, color, false);
        if (bold) {
            graphics.pose().translate(0.34F, 0.0F, 0.01F);
            graphics.drawString(Minecraft.getInstance().font,
                    component, 0, 0, color, false);
        }
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

    private static int[] fitRect(int sourceWidth, int sourceHeight,
                                 int maxWidth, int maxHeight) {
        float scale = Math.min(maxWidth / (float) sourceWidth,
                maxHeight / (float) sourceHeight);
        return new int[]{Math.max(1, Math.round(sourceWidth * scale)),
                Math.max(1, Math.round(sourceHeight * scale))};
    }
}
