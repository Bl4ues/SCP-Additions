package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.network.ScpRoleSelectorNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Animated administrative selector for playable SCP roles. */
public final class ScpRoleSelectorScreen extends Screen {
    private static final int PREVIEW_SOURCE_WIDTH = 1920;
    private static final int PREVIEW_SOURCE_HEIGHT = 1080;
    private static final ResourceLocation SCP_079_PREVIEW = resource(
            "textures/screens/menu/loading_images/scp079_loading.png");
    private static final ResourceLocation EUCLID_ICON = resource(
            "textures/gui/euclid.png");

    private static final RoleCard SCP_079 = new RoleCard(
            "SCP-079", "Old AI", "EUCLID",
            "FACILITY CONTROL / NETWORK INTELLIGENCE",
            SCP_079_PREVIEW, 1.58F, EUCLID_ICON,
            List.of(
                    new Ability("FACILITY SURVEILLANCE",
                            "Enter mapped surveillance cameras and switch between room feeds."),
                    new Ability("DOOR CONTROL",
                            "Aim at supported doors to open, close, or temporarily deny access."),
                    new Ability("TESLA SUPPRESSION",
                            "Aim at a Tesla Gate to suppress it using the shared processing budget."),
                    new Ability("PROCESSING POWER",
                            "Remote actions consume AP; powered Auxiliary units restore it over time."),
                    new Ability("LOCAL HOST",
                            "Without network access, remain anchored to SCP-079's physical computer.")));

    private final boolean scp079Active;
    private RoleCard selected;
    private long openedAt;
    private long lastFrame;
    private float cardHover;
    private float confirmHover;
    private float detailProgress;
    private boolean leaveConfirmation;

    private ScpRoleSelectorScreen(boolean scp079Active) {
        super(ScpFonts.titillium("Anomaly Database"));
        this.scp079Active = scp079Active;
        this.selected = scp079Active ? SCP_079 : null;
    }

    public static void open(boolean scp079Active) {
        Minecraft.getInstance().setScreen(new ScpRoleSelectorScreen(scp079Active));
    }

    @Override
    protected void init() {
        openedAt = Util.getMillis();
        lastFrame = openedAt;
        detailProgress = selected == null ? 0.0F : 1.0F;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        long now = Util.getMillis();
        float dt = Mth.clamp((now - lastFrame) / 1000.0F, 0.0F, 0.05F);
        lastFrame = now;

        Layout layout = animatedLayout(now);
        boolean cardHovered = !leaveConfirmation
                && inside(mouseX, mouseY, cardX(layout), cardY(layout),
                cardW(layout), cardH(layout));
        boolean confirmHovered = !leaveConfirmation
                && inside(mouseX, mouseY, confirmX(layout), confirmY(layout),
                confirmW(layout), confirmH())
                && (selected != null || scp079Active);
        cardHover = approach(cardHover, cardHovered ? 1.0F : 0.0F,
                dt * 9.0F);
        confirmHover = approach(confirmHover, confirmHovered ? 1.0F : 0.0F,
                dt * 11.0F);
        detailProgress = approach(detailProgress,
                selected == null ? 0.0F : 1.0F, dt * 7.0F);

        renderBackdrop(graphics, now);
        renderPanel(graphics, layout);
        renderHeader(graphics, layout);
        renderCurrentForm(graphics, layout);
        renderCard(graphics, layout, cardHovered, now);
        renderDetails(graphics, layout, now);
        renderClose(graphics, layout, mouseX, mouseY);
        if (leaveConfirmation) {
            renderLeaveConfirmation(graphics, mouseX, mouseY);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Layout animatedLayout(long now) {
        Layout layout = layout();
        float open = easeOutCubic(Mth.clamp((now - openedAt) / 230.0F,
                0.0F, 1.0F));
        return new Layout(layout.x,
                layout.y + Math.round((1.0F - open) * 14.0F),
                layout.w, layout.h, layout.leftW);
    }

    private void renderBackdrop(GuiGraphics graphics, long now) {
        graphics.fill(0, 0, width, height, 0xF2050D13);
        for (int x = 0; x < width; x += 28) {
            graphics.fill(x, 0, x + 1, height, 0x160E6077);
        }
        for (int y = 0; y < height; y += 28) {
            graphics.fill(0, y, width, y + 1, 0x160E6077);
        }
        int sweep = (int) ((now / 9L) % Math.max(1, height + 80)) - 40;
        graphics.fill(0, sweep, width, sweep + 2, 0x163EC7E8);
        for (int y = 0; y < height; y += 4) {
            graphics.fill(0, y, width, y + 1, 0x09000000);
        }
    }

    private void renderPanel(GuiGraphics graphics, Layout layout) {
        graphics.fill(layout.x, layout.y, layout.x + layout.w,
                layout.y + layout.h, 0xEC081720);
        border(graphics, layout.x, layout.y, layout.w, layout.h,
                0xFF31596A);
        graphics.fill(layout.x + layout.leftW, layout.y + 78,
                layout.x + layout.leftW + 1, layout.y + layout.h - 18,
                0xFF244958);
    }

    private void renderHeader(GuiGraphics graphics, Layout layout) {
        drawScaled(graphics, font, ScpFonts.montserrat("ANOMALY DATABASE"),
                layout.x + 22, layout.y + 17, 1.72F, 0xFFE9F8FF);
        drawScaled(graphics, font,
                ScpFonts.roboto("ADMINISTRATIVE ROLE ASSIGNMENT / SCP: CLASSIFIED DIRECTIVE"),
                layout.x + 23, layout.y + 43, 0.92F, 0xFF6F9DAE);
        graphics.fill(layout.x + 18, layout.y + 65,
                layout.x + layout.w - 18, layout.y + 66, 0xFF244958);
    }

    private void renderCurrentForm(GuiGraphics graphics, Layout layout) {
        int x = layout.x + 20;
        int y = layout.y + 82;
        graphics.drawString(font, ScpFonts.roboto("CURRENT FORM"), x, y,
                0xFF6F9DAE, false);
        String current = scp079Active ? "SCP-079" : "PERSONNEL";
        int color = scp079Active ? 0xFFE5BD55 : 0xFFBDEEFF;
        drawScaled(graphics, font, ScpFonts.montserrat(current),
                x, y + 14, 1.02F, color);
    }

    private void renderCard(GuiGraphics graphics, Layout layout,
            boolean hovered, long now) {
        int x = cardX(layout);
        int y = cardY(layout);
        int w = cardW(layout);
        int h = cardH(layout);
        float scale = 1.0F + cardHover * 0.022F;
        graphics.pose().pushPose();
        graphics.pose().translate(x + w * 0.5F, y + h * 0.5F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-(x + w * 0.5F), -(y + h * 0.5F), 0.0F);

        boolean chosen = selected == SCP_079;
        int fill = chosen ? 0xE01B3440
                : hovered ? 0xD918303B : 0xC912242D;
        int line = chosen ? 0xFFE5BD55
                : hovered ? 0xFF9ADFF4 : 0xFF416A79;
        graphics.fill(x, y, x + w, y + h, fill);
        border(graphics, x, y, w, h, line);

        int previewX = x + 12;
        int previewY = y + 12;
        int previewW = w - 24;
        int previewH = Math.max(62, Math.min(104, h - 82));
        graphics.fill(previewX, previewY, previewX + previewW,
                previewY + previewH, 0xFF020709);
        if (!drawCenteredPreview(graphics, SCP_079, previewX, previewY,
                previewW, previewH)) {
            graphics.drawCenteredString(font, ScpFonts.montserrat("079"),
                    previewX + previewW / 2,
                    previewY + previewH / 2 - font.lineHeight / 2,
                    0xFFDCECF0);
        }
        graphics.fill(previewX, previewY + previewH - 2,
                previewX + previewW, previewY + previewH, 0xA42F8399);
        int sweep = previewY + (int) ((now / 22L) % Math.max(1, previewH));
        graphics.fill(previewX, sweep, previewX + previewW,
                Math.min(previewY + previewH, sweep + 1), 0x203DD4F4);
        border(graphics, previewX, previewY, previewW, previewH,
                0xFF376270);

        int infoY = y + h - 58;
        graphics.drawString(font, ScpFonts.montserrat(SCP_079.name),
                x + 12, infoY, 0xFFFFFFFF, false);
        graphics.drawString(font, ScpFonts.roboto(SCP_079.nickname),
                x + 12, infoY + 15, 0xFF9CC9D7, false);
        drawClassInline(graphics, SCP_079, x + 12, infoY + 30, 17);

        String hint = chosen ? "SELECTED"
                : hovered ? "CLICK TO SELECT" : "AVAILABLE";
        int hintColor = chosen ? 0xFFE5BD55 : 0xFF6F9DAE;
        graphics.drawString(font, ScpFonts.roboto(hint),
                x + w - font.width(ScpFonts.roboto(hint)) - 12,
                infoY + 32, hintColor, false);
        graphics.pose().popPose();
    }

    private void renderDetails(GuiGraphics graphics, Layout layout, long now) {
        int baseX = layout.x + layout.leftW + 24;
        int x = baseX + Math.round((1.0F - detailProgress) * 16.0F);
        int y = layout.y + 84;
        int right = layout.x + layout.w - 22;

        if (selected == null) {
            drawScaled(graphics, font, ScpFonts.montserrat("SELECT AN SCP"),
                    baseX, y + 4, 1.12F, 0xFF9CC9D7);
            graphics.drawString(font,
                    ScpFonts.roboto("Choose an available anomaly to review its playable role."),
                    baseX, y + 28, 0xFF557A88, false);
            return;
        }

        int classW = Math.min(152, Math.max(118, (right - x) / 3));
        renderContainmentClass(graphics, selected,
                right - classW, y - 3, classW, 46);

        drawScaled(graphics, font, ScpFonts.montserrat(selected.name),
                x, y, 1.48F, 0xFFFFFFFF);
        graphics.drawString(font,
                ScpFonts.roboto("\"" + selected.nickname + "\""),
                x, y + 23, 0xFF9CC9D7, false);
        graphics.drawString(font, ScpFonts.roboto(selected.category),
                x, y + 40, 0xFF6F9DAE, false);

        int abilitiesTitleY = y + 67;
        drawScaled(graphics, font, ScpFonts.montserrat("ABILITIES"),
                x, abilitiesTitleY, 1.02F, 0xFFE9F8FF);
        graphics.fill(x, abilitiesTitleY + 18, right,
                abilitiesTitleY + 19, 0xFF244958);

        int abilitiesY = abilitiesTitleY + 29;
        int noteY = confirmY(layout) - 47;
        int available = Math.max(120, noteY - abilitiesY - 7);
        int rowH = Mth.clamp(available / selected.abilities.size(), 27, 38);
        int ay = abilitiesY;
        for (Ability ability : selected.abilities) {
            int markerY = ay + 4;
            graphics.fill(x, markerY, x + 4, markerY + 4, 0xFF4EC4E3);
            graphics.drawString(font, ScpFonts.roboto(ability.name),
                    x + 12, ay, 0xFFCBEFFA, false);
            if (rowH >= 31) {
                graphics.drawString(font, ScpFonts.roboto(ability.description),
                        x + 12, ay + 13, 0xFF789EAC, false);
            }
            graphics.fill(x + 12, ay + rowH - 5, right,
                    ay + rowH - 4, 0x40244958);
            ay += rowH;
        }

        graphics.fill(x, noteY, right, noteY + 34, 0xA818292F);
        graphics.fill(x, noteY, x + 3, noteY + 34, 0xFFE5BD55);
        graphics.drawString(font,
                ScpFonts.roboto("NETWORK ACCESS REQUIRES AUXILIARY POWER"),
                x + 10, noteY + 6, 0xFFE5BD55, false);
        graphics.drawString(font,
                ScpFonts.roboto("Without it, SCP-079 remains at the local physical host."),
                x + 10, noteY + 19, 0xFF7797A2, false);

        renderConfirm(graphics, layout, now);
    }

    private void renderContainmentClass(GuiGraphics graphics, RoleCard card,
            int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xA5162025);
        border(graphics, x, y, w, h, 0xFF806821);
        int iconSize = Math.min(34, h - 10);
        drawIcon(graphics, card.containmentIcon, x + 7,
                y + (h - iconSize) / 2, iconSize);
        int textX = x + iconSize + 14;
        drawScaled(graphics, font, ScpFonts.roboto("CONTAINMENT CLASS"),
                textX, y + 8, 0.67F, 0xFF9A8C62);
        drawScaled(graphics, font, ScpFonts.montserrat(card.objectClass),
                textX, y + 23, 0.93F, 0xFFFFDF78);
    }

    private void renderConfirm(GuiGraphics graphics, Layout layout, long now) {
        int x = confirmX(layout);
        int y = confirmY(layout);
        int w = confirmW(layout);
        int h = confirmH();
        boolean enabled = selected != null || scp079Active;
        int fill = !enabled ? 0xA5111A1F
                : blend(0xD51B4657, 0xE52C6A7E, confirmHover);
        int line = !enabled ? 0xFF34434A
                : blend(0xFF5D9CAF, 0xFFE5F8FF, confirmHover);
        graphics.fill(x, y, x + w, y + h, fill);
        border(graphics, x, y, w, h, line);
        String label = scp079Active ? "LEAVE SCP ROLE"
                : selected == null ? "SELECT AN SCP"
                : "ASSUME SCP-079 ROLE";
        int textColor = enabled ? 0xFFFFFFFF : 0xFF66757B;
        drawCenteredScaled(graphics, font, ScpFonts.roboto(label),
                x + w / 2.0F, y + h / 2.0F, 1.08F, textColor);

        if (enabled) {
            float pulse = (float) (0.5D + 0.5D * Math.sin(now * 0.006D));
            int px = x + 10 + Math.round(pulse * 5.0F);
            graphics.fill(px, y + h - 3, x + w - 10, y + h - 2,
                    0x845EC8E5);
        }
    }

    private void renderClose(GuiGraphics graphics, Layout layout,
            int mouseX, int mouseY) {
        int x = layout.x + layout.w - 32;
        int y = layout.y + 16;
        boolean hover = !leaveConfirmation
                && inside(mouseX, mouseY, x - 6, y - 5, 22, 22);
        graphics.drawString(font, ScpFonts.montserrat("X"), x, y,
                hover ? 0xFFFFFFFF : 0xFF7599A6, false);
    }

    private void renderLeaveConfirmation(GuiGraphics graphics,
            int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0xB8000000);
        int w = Math.min(330, width - 36);
        int h = 128;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        graphics.fill(x, y, x + w, y + h, 0xF20A1820);
        border(graphics, x, y, w, h, 0xFF5C8493);
        drawCenteredScaled(graphics, font,
                ScpFonts.montserrat("LEAVE SCP ROLE?"),
                x + w / 2.0F, y + 24, 1.12F, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                ScpFonts.roboto("Your original player state will be restored."),
                x + w / 2, y + 48, 0xFF8EAFBA);

        int gap = 10;
        int buttonW = (w - 38 - gap) / 2;
        int buttonY = y + h - 39;
        int cancelX = x + 19;
        int leaveX = cancelX + buttonW + gap;
        boolean cancelHover = inside(mouseX, mouseY,
                cancelX, buttonY, buttonW, 25);
        boolean leaveHover = inside(mouseX, mouseY,
                leaveX, buttonY, buttonW, 25);
        drawModalButton(graphics, cancelX, buttonY, buttonW, 25,
                "CANCEL", cancelHover, false);
        drawModalButton(graphics, leaveX, buttonY, buttonW, 25,
                "LEAVE ROLE", leaveHover, true);
    }

    private void drawModalButton(GuiGraphics graphics, int x, int y,
            int w, int h, String label, boolean hovered, boolean danger) {
        int fill = danger
                ? hovered ? 0xE0642828 : 0xD53D2022
                : hovered ? 0xE52A5868 : 0xD518303A;
        int line = danger ? 0xFFE28A7F : 0xFF75B7CC;
        graphics.fill(x, y, x + w, y + h, fill);
        border(graphics, x, y, w, h, line);
        drawCenteredScaled(graphics, font, ScpFonts.roboto(label),
                x + w / 2.0F, y + h / 2.0F, 0.96F, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (leaveConfirmation) {
            return handleLeaveConfirmationClick(mouseX, mouseY);
        }
        Layout layout = animatedLayout(Util.getMillis());
        int closeX = layout.x + layout.w - 40;
        if (inside(mouseX, mouseY, closeX, layout.y + 9, 30, 30)) {
            onClose();
            return true;
        }
        if (inside(mouseX, mouseY, cardX(layout), cardY(layout),
                cardW(layout), cardH(layout))) {
            selected = SCP_079;
            detailProgress = 0.0F;
            return true;
        }
        if (inside(mouseX, mouseY, confirmX(layout), confirmY(layout),
                confirmW(layout), confirmH())
                && (selected != null || scp079Active)) {
            if (scp079Active) leaveConfirmation = true;
            else confirmSelection();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleLeaveConfirmationClick(double mouseX,
            double mouseY) {
        int w = Math.min(330, width - 36);
        int h = 128;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int gap = 10;
        int buttonW = (w - 38 - gap) / 2;
        int buttonY = y + h - 39;
        int cancelX = x + 19;
        int leaveX = cancelX + buttonW + gap;
        if (inside(mouseX, mouseY, cancelX, buttonY, buttonW, 25)) {
            leaveConfirmation = false;
            return true;
        }
        if (inside(mouseX, mouseY, leaveX, buttonY, buttonW, 25)) {
            requestLeaveRole();
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (leaveConfirmation) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                leaveConfirmation = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER
                    || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                requestLeaveRole();
                return true;
            }
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && (selected != null || scp079Active)) {
            if (scp079Active) leaveConfirmation = true;
            else confirmSelection();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void confirmSelection() {
        if (selected == SCP_079) {
            ScpRoleSelectorNetwork.requestRole(ScpRoleSelectorNetwork.Role.SCP_079);
        }
        Minecraft.getInstance().setScreen(null);
    }

    private void requestLeaveRole() {
        ScpRoleSelectorNetwork.requestRole(ScpRoleSelectorNetwork.Role.HUMAN);
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean drawCenteredPreview(GuiGraphics graphics, RoleCard card,
            int x, int y, int w, int h) {
        if (card.preview == null || w <= 0 || h <= 0
                || Minecraft.getInstance().getResourceManager()
                .getResource(card.preview).isEmpty()) return false;

        float zoom = Math.max(1.0F, card.previewZoom);
        float sourceW = PREVIEW_SOURCE_WIDTH / zoom;
        float sourceH = PREVIEW_SOURCE_HEIGHT / zoom;
        float destinationAspect = w / (float) h;
        if (sourceW / sourceH > destinationAspect) {
            sourceW = sourceH * destinationAspect;
        } else {
            sourceH = sourceW / destinationAspect;
        }
        int sw = Math.max(1, Math.round(sourceW));
        int sh = Math.max(1, Math.round(sourceH));
        int sx = Math.max(0, (PREVIEW_SOURCE_WIDTH - sw) / 2);
        int sy = Math.max(0, (PREVIEW_SOURCE_HEIGHT - sh) / 2);
        graphics.blit(card.preview, x, y, w, h,
                sx, sy, sw, sh,
                PREVIEW_SOURCE_WIDTH, PREVIEW_SOURCE_HEIGHT);
        return true;
    }

    private void drawClassInline(GuiGraphics graphics, RoleCard card,
            int x, int y, int iconSize) {
        drawIcon(graphics, card.containmentIcon, x, y, iconSize);
        graphics.drawString(font, ScpFonts.roboto(card.objectClass),
                x + iconSize + 6, y + Math.max(1, (iconSize - font.lineHeight) / 2),
                0xFFE5BD55, false);
    }

    private void drawIcon(GuiGraphics graphics, ResourceLocation texture,
            int x, int y, int size) {
        if (texture == null || Minecraft.getInstance().getResourceManager()
                .getResource(texture).isEmpty()) return;
        float scale = size / 128.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(texture, 0, 0, 0.0F, 0.0F,
                128, 128, 128, 128);
        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private Layout layout() {
        int w = Math.min(920, Math.max(520, width - 32));
        int h = Math.min(500, Math.max(330, height - 32));
        w = Math.min(w, width - 12);
        h = Math.min(h, height - 12);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int leftW = Math.min(265, Math.max(205, w * 31 / 100));
        return new Layout(x, y, w, h, leftW);
    }

    private int cardX(Layout layout) { return layout.x + 20; }
    private int cardY(Layout layout) { return layout.y + 120; }
    private int cardW(Layout layout) { return layout.leftW - 40; }
    private int cardH(Layout layout) { return Math.min(218, layout.h - 158); }
    private int confirmX(Layout layout) { return layout.x + layout.leftW + 24; }
    private int confirmW(Layout layout) { return layout.w - layout.leftW - 46; }
    private int confirmY(Layout layout) { return layout.y + layout.h - 54; }
    private int confirmH() { return 40; }

    private static void drawScaled(GuiGraphics graphics, Font font,
            Component text, float x, float y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static void drawCenteredScaled(GuiGraphics graphics, Font font,
            Component text, float centerX, float centerY, float scale,
            int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, -font.width(text) / 2,
                -font.lineHeight / 2, color, false);
        graphics.pose().popPose();
    }

    private static float approach(float current, float target, float amount) {
        return current + (target - current) * Mth.clamp(amount, 0.0F, 1.0F);
    }

    private static float easeOutCubic(float value) {
        float inv = 1.0F - value;
        return 1.0F - inv * inv * inv;
    }

    private static boolean inside(double mouseX, double mouseY,
            int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static void border(GuiGraphics graphics, int x, int y,
            int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static int blend(int from, int to, float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        int a = Math.round(Mth.lerp(t, (from >>> 24) & 255, (to >>> 24) & 255));
        int r = Math.round(Mth.lerp(t, (from >>> 16) & 255, (to >>> 16) & 255));
        int g = Math.round(Mth.lerp(t, (from >>> 8) & 255, (to >>> 8) & 255));
        int b = Math.round(Mth.lerp(t, from & 255, to & 255));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
    }

    private record Layout(int x, int y, int w, int h, int leftW) {
    }

    private record Ability(String name, String description) {
    }

    private record RoleCard(String name, String nickname, String objectClass,
            String category, ResourceLocation preview, float previewZoom,
            ResourceLocation containmentIcon, List<Ability> abilities) {
    }
}
