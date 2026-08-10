package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** SCP Additions advancement toast presentation. */
public final class CustomAdvancementToastClient {
    public static final int WIDTH = 232;
    public static final int HEIGHT = 68;

    private static final int PANEL = 0xE80B0E12;
    private static final int PANEL_INNER = 0xD412161C;
    private static final int ICON_PANEL = 0xF00A0D12;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;
    private static final int TEXT = 0xFFF5F6F7;
    private static final int MUTED = 0xFF9DA5AF;
    private static final int BORDER = 0x70414A56;
    private static final int TRACK = 0x663D4652;
    private static final int TASK = 0xFF79D58B;
    private static final int GOAL = 0xFFC99B18;
    private static final int CHALLENGE = 0xFFFF7373;

    private static final long ENTER_MS = 620L;
    private static final long EXIT_START_MS = 5300L;
    private static final long LIFETIME_MS = 6050L;
    private static final int LOGO_TEXTURE_SIZE = 512;
    private static final ResourceLocation LOGO_OUTER = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/menu/loading_1.png");
    private static final ResourceLocation LOGO_INNER = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/menu/loading_2.png");
    private static final AtomicBoolean SUPPRESS_NEXT_VANILLA_TRANSITION =
            new AtomicBoolean(false);

    private CustomAdvancementToastClient() {
    }

    public static Toast.Visibility render(GuiGraphics graphics,
            ToastComponent toastComponent, DisplayInfo display, long age) {
        float enter = smootherStep(age / (float) ENTER_MS);
        float exit = age <= EXIT_START_MS ? 0.0F : smootherStep(
                (age - EXIT_START_MS) / (float) (LIFETIME_MS - EXIT_START_MS));
        float alpha = Mth.clamp(enter * (1.0F - exit), 0.0F, 1.0F);
        float slide = (1.0F - enter) * 46.0F + exit * 22.0F;

        graphics.pose().pushPose();
        graphics.pose().translate(slide, 0.0F, 0.0F);
        drawLogoPair(graphics, age, alpha);
        drawCard(graphics, toastComponent.getMinecraft().font,
                display, alpha);
        graphics.pose().popPose();

        return age >= LIFETIME_MS
                ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    public static void armVanillaTransitionSoundSuppression() {
        SUPPRESS_NEXT_VANILLA_TRANSITION.set(true);
    }

    public static boolean consumeVanillaTransitionSoundSuppression() {
        return SUPPRESS_NEXT_VANILLA_TRANSITION.getAndSet(false);
    }

    private static void drawCard(GuiGraphics graphics, Font font,
            DisplayInfo display, float alpha) {
        int left = 8;
        int top = 8;
        int right = WIDTH - 5;
        int bottom = HEIGHT - 6;
        int panel = withAlpha(PANEL, alpha);
        int inner = withAlpha(PANEL_INNER, alpha);
        int accent = withAlpha(ACCENT, alpha);
        int accentBright = withAlpha(ACCENT_BRIGHT, alpha);
        int border = withAlpha(BORDER, alpha);

        graphics.fill(left, top, right, bottom, panel);
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, inner);
        graphics.fill(left, top, right, top + 2, accent);
        graphics.fill(left, top, left + 2, bottom, accent);
        graphics.fill(left + 2, bottom - 1, right, bottom, border);
        graphics.fill(right - 1, top + 2, right, bottom, border);
        graphics.fill(left + 2, top + 2, left + 8, top + 3, accentBright);

        int iconLeft = left + 8;
        int iconTop = top + 9;
        int iconSize = 40;
        graphics.fill(iconLeft, iconTop, iconLeft + iconSize,
                iconTop + iconSize, withAlpha(ICON_PANEL, alpha));
        graphics.fill(iconLeft, iconTop, iconLeft + iconSize,
                iconTop + 1, border);
        graphics.fill(iconLeft, iconTop, iconLeft + 1,
                iconTop + iconSize, border);
        graphics.fill(iconLeft + iconSize - 1, iconTop,
                iconLeft + iconSize, iconTop + iconSize, border);
        graphics.fill(iconLeft, iconTop + iconSize - 1,
                iconLeft + iconSize, iconTop + iconSize, border);

        renderIcon(graphics, display.getIcon(), iconLeft + 8,
                iconTop + 8, alpha);

        int textLeft = iconLeft + iconSize + 10;
        int textRight = right - 9;
        Component eyebrow = ScpFonts.roboto("ADVANCEMENT  //  UNLOCKED");
        graphics.drawString(font, eyebrow, textLeft, top + 8,
                withAlpha(MUTED, alpha), false);

        List<FormattedCharSequence> title = font.split(
                ScpFonts.roboto(display.getTitle().getString()),
                Math.max(45, textRight - textLeft));
        int titleY = top + 20;
        for (int index = 0; index < Math.min(2, title.size()); index++) {
            graphics.drawString(font, title.get(index), textLeft,
                    titleY + index * (font.lineHeight + 1),
                    withAlpha(TEXT, alpha), false);
        }

        drawRarity(graphics, font, display.getFrame(), textLeft,
                bottom - 11, textRight, alpha);
    }

    private static void renderIcon(GuiGraphics graphics, ItemStack stack,
            int x, int y, float alpha) {
        if (stack == null || stack.isEmpty()) return;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 40.0F);
        graphics.pose().scale(1.5F, 1.5F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.renderItem(stack, 0, 0);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }

    private static void drawRarity(GuiGraphics graphics, Font font,
            FrameType frame, int left, int y, int right, float alpha) {
        int filled;
        int color;
        String label;
        if (frame == FrameType.CHALLENGE) {
            filled = 5;
            color = CHALLENGE;
            label = "CHALLENGE";
        } else if (frame == FrameType.GOAL) {
            filled = 3;
            color = GOAL;
            label = "GOAL";
        } else {
            filled = 1;
            color = TASK;
            label = "TASK";
        }

        Component type = ScpFonts.roboto(label);
        int muted = withAlpha(MUTED, alpha * 0.86F);
        graphics.drawString(font, type, left, y - 1, muted, false);

        int barWidth = 9;
        int barHeight = 3;
        int gap = 2;
        int total = barWidth * 5 + gap * 4;
        int barLeft = right - total;
        for (int index = 0; index < 5; index++) {
            int x = barLeft + index * (barWidth + gap);
            graphics.fill(x, y + 2, x + barWidth, y + 2 + barHeight,
                    index < filled ? withAlpha(color, alpha)
                            : withAlpha(TRACK, alpha * 0.72F));
        }
    }

    private static void drawLogoPair(GuiGraphics graphics, long age,
            float cardAlpha) {
        float arrival = smootherStep(age / (float) ENTER_MS);
        float cx = Mth.lerp(arrival, WIDTH + 94.0F, WIDTH + 28.0F);
        float cy = Mth.lerp(arrival, -78.0F, -20.0F);
        float size = Mth.lerp(arrival, 122.0F, 106.0F);
        float seconds = Math.max(0.0F, age / 1000.0F);
        float fastBurst = 780.0F * (1.0F - (float) Math.exp(-2.10F * seconds));
        float slowSpin = 42.0F * seconds;
        float outerAngle = fastBurst + slowSpin;
        float innerAngle = -(fastBurst + slowSpin) * 1.08F;
        float logoAlpha = cardAlpha * Mth.lerp(arrival, 0.32F, 0.19F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, logoAlpha);
        drawRotatedTexture(graphics, LOGO_OUTER, cx, cy,
                Math.round(size), outerAngle);
        drawRotatedTexture(graphics, LOGO_INNER, cx, cy,
                Math.round(size), innerAngle);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawRotatedTexture(GuiGraphics graphics,
            ResourceLocation texture, float centerX, float centerY,
            int size, float angleDegrees) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angleDegrees));
        int half = size / 2;
        graphics.blit(texture, -half, -half, size, size,
                0.0F, 0.0F, LOGO_TEXTURE_SIZE, LOGO_TEXTURE_SIZE,
                LOGO_TEXTURE_SIZE, LOGO_TEXTURE_SIZE);
        graphics.pose().popPose();
    }

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static int withAlpha(int color, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
