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

/** Compact animated administrative selector for playable SCP roles. */
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
            SCP_079_PREVIEW, 2.45F, EUCLID_ICON,
            List.of(
                    new Ability("FACILITY SURVEILLANCE",
                            "Enter mapped surveillance cameras and switch between room feeds."),
                    new Ability("DOOR CONTROL",
                            "Aim at doors to open, close, or temporarily deny access."),
                    new Ability("TESLA SUPPRESSION",
                            "Aim at a Tesla Gate to suppress it."),
                    new Ability("PROCESSING POWER",
                            "Remote actions consume AP; powered Auxiliary units restore it over time.")));

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
        boolean cardHovered = !leaveConfirmation && inside(mouseX, mouseY,
                cardX(layout), cardY(layout), cardW(layout), cardH(layout));
        boolean confirmHovered = !leaveConfirmation && inside(mouseX, mouseY,
                confirmX(layout), confirmY(layout), confirmW(layout), confirmH())
                && (selected != null || scp079Active);
        cardHover = approach(cardHover, cardHovered ? 1.0F : 0.0F, dt * 9.0F);
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
        if (leaveConfirmation) renderLeaveConfirmation(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Layout animatedLayout(long now) {
        Layout layout = layout();
        float open = easeOutCubic(Mth.clamp((now - openedAt) / 230.0F, 0.0F, 1.0F));
        return new Layout(layout.x, layout.y + Math.round((1.0F - open) * 12.0F),
                layout.w, layout.h, layout.leftW);
    }

    private void renderBackdrop(GuiGraphics graphics, long now) {
        graphics.fill(0, 0, width, height, 0xF2050D13);
        for (int x = 0; x < width; x += 28) graphics.fill(x, 0, x + 1, height, 0x160E6077);
        for (int y = 0; y < height; y += 28) graphics.fill(0, y, width, y + 1, 0x160E6077);
        int sweep = (int) ((now / 9L) % Math.max(1, height + 80)) - 40;
        graphics.fill(0, sweep, width, sweep + 2, 0x163EC7E8);
        for (int y = 0; y < height; y += 4) graphics.fill(0, y, width, y + 1, 0x09000000);
    }

    private void renderPanel(GuiGraphics graphics, Layout layout) {
        graphics.fill(layout.x, layout.y, layout.x + layout.w,
                layout.y + layout.h, 0xEC081720);
        border(graphics, layout.x, layout.y, layout.w, layout.h, 0xFF31596A);
        graphics.fill(layout.x + layout.leftW, layout.y + 70,
                layout.x + layout.leftW + 1, layout.y + layout.h - 16,
                0xFF244958);
    }

    private void renderHeader(GuiGraphics graphics, Layout layout) {
        drawScaled(graphics, font, ScpFonts.montserrat("ANOMALY DATABASE"),
                layout.x + 20, layout.y + 15, 1.56F, 0xFFE9F8FF);
        drawScaled(graphics, font,
                ScpFonts.roboto("ADMINISTRATIVE ROLE ASSIGNMENT / SCP: CLASSIFIED DIRECTIVE"),
                layout.x + 21, layout.y + 39, 0.82F, 0xFF6F9DAE);
        graphics.fill(layout.x + 16, layout.y + 58,
                layout.x + layout.w - 16, layout.y + 59, 0xFF244958);
    }

    private void renderCurrentForm(GuiGraphics graphics, Layout layout) {
        int x = layout.x + 18;
        int y = layout.y + 73;
        graphics.drawString(font, ScpFonts.roboto("CURRENT FORM"), x, y,
                0xFF6F9DAE, false);
        String current = scp079Active ? "SCP-079" : "PERSONNEL";
        drawScaled(graphics, font, ScpFonts.montserrat(current), x, y + 13,
                0.95F, scp079Active ? 0xFFE5BD55 : 0xFFBDEEFF);
    }

    private void renderCard(GuiGraphics graphics, Layout layout,
            boolean hovered, long now) {
        int x = cardX(layout), y = cardY(layout), w = cardW(layout), h = cardH(layout);
        float scale = 1.0F + cardHover * 0.018F;
        graphics.pose().pushPose();
        graphics.pose().translate(x + w * 0.5F, y + h * 0.5F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-(x + w * 0.5F), -(y + h * 0.5F), 0.0F);
        boolean chosen = selected == SCP_079;
        graphics.fill(x, y, x + w, y + h,
                chosen ? 0xE01B3440 : hovered ? 0xD918303B : 0xC912242D);
        border(graphics, x, y, w, h,
                chosen ? 0xFFE5BD55 : hovered ? 0xFF9ADFF4 : 0xFF416A79);

        int px = x + 9, py = y + 9, pw = w - 18;
        int ph = Math.max(58, h - 69);
        graphics.enableScissor(px, py, px + pw, py + ph);
        if (!drawCenteredPreview(graphics, SCP_079, px, py, pw, ph)) {
            graphics.drawCenteredString(font, ScpFonts.montserrat("079"),
                    px + pw / 2, py + ph / 2 - 4, 0xFFDCECF0);
        }
        graphics.disableScissor();
        int sweep = py + (int) ((now / 20L) % Math.max(1, ph));
        graphics.fill(px, sweep, px + pw, Math.min(py + ph, sweep + 1), 0x203DD4F4);
        border(graphics, px, py, pw, ph, 0xFF376270);

        int infoY = y + h - 52;
        graphics.drawString(font, ScpFonts.montserrat(SCP_079.name),
                x + 10, infoY, 0xFFFFFFFF, false);
        graphics.drawString(font, ScpFonts.roboto(SCP_079.nickname),
                x + 10, infoY + 14, 0xFF9CC9D7, false);
        drawClassInline(graphics, SCP_079, x + 10, infoY + 27, 15);
        String hint = chosen ? "SELECTED" : hovered ? "SELECT" : "AVAILABLE";
        graphics.drawString(font, ScpFonts.roboto(hint),
                x + w - font.width(ScpFonts.roboto(hint)) - 9, infoY + 29,
                chosen ? 0xFFE5BD55 : 0xFF6F9DAE, false);
        graphics.pose().popPose();
    }

    private void renderDetails(GuiGraphics graphics, Layout layout, long now) {
        int baseX = layout.x + layout.leftW + 22;
        int x = baseX + Math.round((1.0F - detailProgress) * 14.0F);
        int y = layout.y + 78;
        int right = layout.x + layout.w - 20;
        if (selected == null) {
            drawScaled(graphics, font, ScpFonts.montserrat("SELECT AN SCP"),
                    baseX, y + 4, 1.08F, 0xFF9CC9D7);
            graphics.drawString(font,
                    ScpFonts.roboto("Choose an available anomaly to review its playable role."),
                    baseX, y + 28, 0xFF557A88, false);
            return;
        }

        int classW = Math.min(145, Math.max(114, (right - x) / 3));
        renderContainmentClass(graphics, selected, right - classW, y - 2, classW, 43);
        drawScaled(graphics, font, ScpFonts.montserrat(selected.name),
                x, y, 1.38F, 0xFFFFFFFF);
        graphics.drawString(font, ScpFonts.roboto("\"" + selected.nickname + "\""),
                x, y + 22, 0xFF9CC9D7, false);
        graphics.drawString(font, ScpFonts.roboto(selected.category),
                x, y + 38, 0xFF6F9DAE, false);

        int titleY = y + 66;
        drawScaled(graphics, font, ScpFonts.montserrat("ABILITIES"),
                x, titleY, 0.98F, 0xFFE9F8FF);
        graphics.fill(x, titleY + 18, right, titleY + 19, 0xFF244958);
        int ay = titleY + 30;
        int rowH = 43;
        for (Ability ability : selected.abilities) {
            graphics.fill(x, ay, x + 3, ay + 28, 0xFF4EC4E3);
            graphics.drawString(font, ScpFonts.roboto(ability.name),
                    x + 10, ay, 0xFFCBEFFA, false);
            graphics.drawString(font, ScpFonts.roboto(ability.description),
                    x + 10, ay + 14, 0xFF789EAC, false);
            ay += rowH;
        }

        int noteY = confirmY(layout) - 42;
        graphics.fill(x, noteY, right, noteY + 30, 0xA818292F);
        graphics.fill(x, noteY, x + 3, noteY + 30, 0xFFE5BD55);
        graphics.drawString(font,
                ScpFonts.roboto("NETWORK ACCESS REQUIRES AUXILIARY POWER"),
                x + 9, noteY + 5, 0xFFE5BD55, false);
        graphics.drawString(font,
                ScpFonts.roboto("Without it, SCP-079 remains at the physical host."),
                x + 9, noteY + 17, 0xFF7797A2, false);
        renderConfirm(graphics, layout, now);
    }

    private void renderContainmentClass(GuiGraphics graphics, RoleCard card,
            int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xA5162025);
        border(graphics, x, y, w, h, 0xFF806821);
        int iconSize = Math.min(31, h - 8);
        drawIcon(graphics, card.containmentIcon, x + 6,
                y + (h - iconSize) / 2, iconSize);
        int tx = x + iconSize + 12;
        drawScaled(graphics, font, ScpFonts.roboto("CONTAINMENT CLASS"),
                tx, y + 7, 0.62F, 0xFF9A8C62);
        drawScaled(graphics, font, ScpFonts.montserrat(card.objectClass),
                tx, y + 21, 0.88F, 0xFFFFDF78);
    }

    private void renderConfirm(GuiGraphics graphics, Layout layout, long now) {
        int x = confirmX(layout), y = confirmY(layout), w = confirmW(layout), h = confirmH();
        boolean enabled = selected != null || scp079Active;
        int fill = !enabled ? 0xA5111A1F : blend(0xD51B4657, 0xE52C6A7E, confirmHover);
        int line = !enabled ? 0xFF34434A : blend(0xFF5D9CAF, 0xFFE5F8FF, confirmHover);
        graphics.fill(x, y, x + w, y + h, fill);
        border(graphics, x, y, w, h, line);
        String label = scp079Active ? "LEAVE SCP ROLE"
                : selected == null ? "SELECT AN SCP" : "ASSUME SCP-079 ROLE";
        drawCenteredScaled(graphics, font, ScpFonts.roboto(label),
                x + w / 2.0F, y + h / 2.0F, 1.13F,
                enabled ? 0xFFFFFFFF : 0xFF66757B);
        if (enabled) {
            float pulse = (float) (0.5D + 0.5D * Math.sin(now * 0.006D));
            int px = x + 10 + Math.round(pulse * 5.0F);
            graphics.fill(px, y + h - 3, x + w - 10, y + h - 2, 0x845EC8E5);
        }
    }

    private void renderClose(GuiGraphics graphics, Layout layout,
            int mouseX, int mouseY) {
        int x = layout.x + layout.w - 29;
        int y = layout.y + 14;
        boolean hover = !leaveConfirmation && inside(mouseX, mouseY, x - 6, y - 5, 22, 22);
        graphics.drawString(font, ScpFonts.montserrat("X"), x, y,
                hover ? 0xFFFFFFFF : 0xFF7599A6, false);
    }

    private void renderLeaveConfirmation(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0xB8000000);
        int w = Math.min(330, width - 36), h = 128;
        int x = (width - w) / 2, y = (height - h) / 2;
        graphics.fill(x, y, x + w, y + h, 0xF20A1820);
        border(graphics, x, y, w, h, 0xFF5C8493);
        drawCenteredScaled(graphics, font, ScpFonts.montserrat("LEAVE SCP ROLE?"),
                x + w / 2.0F, y + 24, 1.12F, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                ScpFonts.roboto("Your original player state will be restored."),
                x + w / 2, y + 48, 0xFF8EAFBA);
        int gap = 10, buttonW = (w - 38 - gap) / 2, buttonY = y + h - 39;
        int cancelX = x + 19, leaveX = cancelX + buttonW + gap;
        drawModalButton(graphics, cancelX, buttonY, buttonW, 25, "CANCEL",
                inside(mouseX, mouseY, cancelX, buttonY, buttonW, 25), false);
        drawModalButton(graphics, leaveX, buttonY, buttonW, 25, "LEAVE ROLE",
                inside(mouseX, mouseY, leaveX, buttonY, buttonW, 25), true);
    }

    private void drawModalButton(GuiGraphics graphics, int x, int y,
            int w, int h, String label, boolean hovered, boolean danger) {
        int fill = danger ? (hovered ? 0xE0642828 : 0xD53D2022)
                : (hovered ? 0xE52A5868 : 0xD518303A);
        int line = danger ? 0xFFE28A7F : 0xFF75B7CC;
        graphics.fill(x, y, x + w, y + h, fill);
        border(graphics, x, y, w, h, line);
        drawCenteredScaled(graphics, font, ScpFonts.roboto(label),
                x + w / 2.0F, y + h / 2.0F, 0.96F, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(mouseX, mouseY, button);
        if (leaveConfirmation) return handleLeaveConfirmationClick(mouseX, mouseY);
        Layout layout = animatedLayout(Util.getMillis());
        if (inside(mouseX, mouseY, layout.x + layout.w - 37, layout.y + 8, 29, 29)) {
            onClose(); return true;
        }
        if (inside(mouseX, mouseY, cardX(layout), cardY(layout), cardW(layout), cardH(layout))) {
            selected = SCP_079; detailProgress = 0.0F; return true;
        }
        if (inside(mouseX, mouseY, confirmX(layout), confirmY(layout),
                confirmW(layout), confirmH()) && (selected != null || scp079Active)) {
            if (scp079Active) leaveConfirmation = true; else confirmSelection();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleLeaveConfirmationClick(double mouseX, double mouseY) {
        int w = Math.min(330, width - 36), h = 128;
        int x = (width - w) / 2, y = (height - h) / 2;
        int gap = 10, bw = (w - 38 - gap) / 2, by = y + h - 39;
        int cx = x + 19, lx = cx + bw + gap;
        if (inside(mouseX, mouseY, cx, by, bw, 25)) { leaveConfirmation = false; return true; }
        if (inside(mouseX, mouseY, lx, by, bw, 25)) { requestLeaveRole(); return true; }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (leaveConfirmation) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { leaveConfirmation = false; return true; }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                requestLeaveRole(); return true;
            }
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && (selected != null || scp079Active)) {
            if (scp079Active) leaveConfirmation = true; else confirmSelection();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void confirmSelection() {
        if (selected == SCP_079) ScpRoleSelectorNetwork.requestRole(ScpRoleSelectorNetwork.Role.SCP_079);
        Minecraft.getInstance().setScreen(null);
    }

    private void requestLeaveRole() {
        ScpRoleSelectorNetwork.requestRole(ScpRoleSelectorNetwork.Role.HUMAN);
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private boolean drawCenteredPreview(GuiGraphics graphics, RoleCard card,
            int x, int y, int w, int h) {
        if (card.preview == null || w <= 0 || h <= 0
                || Minecraft.getInstance().getResourceManager().getResource(card.preview).isEmpty()) return false;
        float zoom = Math.max(1.0F, card.previewZoom);
        float sourceW = PREVIEW_SOURCE_WIDTH / zoom;
        float sourceH = PREVIEW_SOURCE_HEIGHT / zoom;
        float aspect = w / (float) h;
        if (sourceW / sourceH > aspect) sourceW = sourceH * aspect;
        else sourceH = sourceW / aspect;
        int sw = Math.max(1, Math.round(sourceW));
        int sh = Math.max(1, Math.round(sourceH));
        int sx = Math.max(0, (PREVIEW_SOURCE_WIDTH - sw) / 2);
        int sy = Math.max(0, (PREVIEW_SOURCE_HEIGHT - sh) / 2);
        graphics.blit(card.preview, x, y, w, h, sx, sy, sw, sh,
                PREVIEW_SOURCE_WIDTH, PREVIEW_SOURCE_HEIGHT);
        return true;
    }

    private void drawClassInline(GuiGraphics graphics, RoleCard card,
            int x, int y, int iconSize) {
        drawIcon(graphics, card.containmentIcon, x, y, iconSize);
        graphics.drawString(font, ScpFonts.roboto(card.objectClass),
                x + iconSize + 5, y + Math.max(1, (iconSize - font.lineHeight) / 2),
                0xFFE5BD55, false);
    }

    private void drawIcon(GuiGraphics graphics, ResourceLocation texture,
            int x, int y, int size) {
        if (texture == null || Minecraft.getInstance().getResourceManager().getResource(texture).isEmpty()) return;
        float scale = size / 128.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(texture, 0, 0, 0.0F, 0.0F, 128, 128, 128, 128);
        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private Layout layout() {
        int w = Math.min(790, Math.max(560, width - 90));
        int h = Math.min(455, Math.max(340, height - 70));
        w = Math.min(w, width - 16);
        h = Math.min(h, height - 16);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int leftW = Math.min(205, Math.max(176, w * 25 / 100));
        return new Layout(x, y, w, h, leftW);
    }

    private int cardX(Layout l) { return l.x + 18; }
    private int cardY(Layout l) { return l.y + 108; }
    private int cardW(Layout l) { return l.leftW - 36; }
    private int cardH(Layout l) { return Math.min(174, l.h - 142); }
    private int confirmX(Layout l) { return l.x + l.leftW + 22; }
    private int confirmW(Layout l) { return l.w - l.leftW - 42; }
    private int confirmY(Layout l) { return l.y + l.h - 52; }
    private int confirmH() { return 38; }

    private static void drawScaled(GuiGraphics graphics, Font font,
            Component text, float x, float y, float scale, int color) {
        graphics.pose().pushPose(); graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false); graphics.pose().popPose();
    }

    private static void drawCenteredScaled(GuiGraphics graphics, Font font,
            Component text, float centerX, float centerY, float scale, int color) {
        graphics.pose().pushPose(); graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, -font.width(text) / 2,
                -font.lineHeight / 2, color, false); graphics.pose().popPose();
    }

    private static float approach(float current, float target, float amount) {
        return current + (target - current) * Mth.clamp(amount, 0.0F, 1.0F);
    }
    private static float easeOutCubic(float value) {
        float inv = 1.0F - value; return 1.0F - inv * inv * inv;
    }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
    private static void border(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c); g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c); g.fill(x + w - 1, y, x + w, y + h, c);
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

    private record Layout(int x, int y, int w, int h, int leftW) { }
    private record Ability(String name, String description) { }
    private record RoleCard(String name, String nickname, String objectClass,
            String category, ResourceLocation preview, float previewZoom,
            ResourceLocation containmentIcon, List<Ability> abilities) { }
}
