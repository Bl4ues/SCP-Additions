package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.network.ScpRoleSelectorNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Animated admin-facing selector for playable SCP roles. The item icon is a
 * placeholder, but this screen is intentionally the real interaction surface so
 * future playable SCPs can be added without changing the operator workflow.
 */
public final class ScpRoleSelectorScreen extends Screen {
    private static final RoleCard SCP_079 = new RoleCard(
            "SCP-079", "Old AI", "EUCLID",
            "FACILITY CONTROL / NETWORK INTELLIGENCE",
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

    private ScpRoleSelectorScreen(boolean scp079Active) {
        super(ScpFonts.titillium("Playable SCP Selector"));
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

        Layout layout = layout();
        float open = easeOutCubic(Mth.clamp((now - openedAt) / 230.0F,
                0.0F, 1.0F));
        int panelY = layout.y + Math.round((1.0F - open) * 14.0F);
        layout = new Layout(layout.x, panelY, layout.w, layout.h,
                layout.leftW);

        boolean cardHovered = inside(mouseX, mouseY, cardX(layout),
                cardY(layout), cardW(layout), cardH(layout));
        boolean confirmHovered = inside(mouseX, mouseY, confirmX(layout),
                confirmY(layout), confirmW(layout), 30)
                && (selected != null || scp079Active);
        cardHover = approach(cardHover, cardHovered ? 1.0F : 0.0F,
                dt * 9.0F);
        confirmHover = approach(confirmHover, confirmHovered ? 1.0F : 0.0F,
                dt * 11.0F);
        detailProgress = approach(detailProgress, selected == null ? 0.0F : 1.0F,
                dt * 7.0F);

        renderBackdrop(graphics, now);
        renderPanel(graphics, layout);
        renderHeader(graphics, layout);
        renderCurrentForm(graphics, layout);
        renderCard(graphics, layout, cardHovered, now);
        renderDetails(graphics, layout, now);
        renderClose(graphics, layout, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
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
        graphics.fill(layout.x + layout.leftW, layout.y + 62,
                layout.x + layout.leftW + 1, layout.y + layout.h - 18,
                0xFF244958);
    }

    private void renderHeader(GuiGraphics graphics, Layout layout) {
        graphics.drawString(font,
                ScpFonts.montserratTitle("PLAYABLE ANOMALY DATABASE"),
                layout.x + 22, layout.y + 18, 0xFFE9F8FF, false);
        graphics.drawString(font,
                ScpFonts.roboto("ADMINISTRATIVE ROLE ASSIGNMENT / SCP: CLASSIFIED DIRECTIVE"),
                layout.x + 22, layout.y + 36, 0xFF6F9DAE, false);
        graphics.fill(layout.x + 18, layout.y + 54,
                layout.x + layout.w - 18, layout.y + 55, 0xFF244958);
    }

    private void renderCurrentForm(GuiGraphics graphics, Layout layout) {
        int x = layout.x + 20;
        int y = layout.y + 72;
        graphics.drawString(font, ScpFonts.roboto("CURRENT FORM"), x, y,
                0xFF6F9DAE, false);
        String current = scp079Active ? "SCP-079" : "PERSONNEL";
        int color = scp079Active ? 0xFFE5BD55 : 0xFFBDEEFF;
        graphics.drawString(font, ScpFonts.montserrat(current), x, y + 14,
                color, false);
    }

    private void renderCard(GuiGraphics graphics, Layout layout,
            boolean hovered, long now) {
        int x = cardX(layout);
        int y = cardY(layout);
        int w = cardW(layout);
        int h = cardH(layout);
        float scale = 1.0F + cardHover * 0.025F;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + w * 0.5F, y + h * 0.5F, 0.0F);
        pose.scale(scale, scale, 1.0F);
        pose.translate(-(x + w * 0.5F), -(y + h * 0.5F), 0.0F);

        boolean chosen = selected == SCP_079;
        int fill = chosen ? 0xE01B3440
                : hovered ? 0xD918303B : 0xC912242D;
        int line = chosen ? 0xFFE5BD55
                : hovered ? 0xFF9ADFF4 : 0xFF416A79;
        graphics.fill(x, y, x + w, y + h, fill);
        border(graphics, x, y, w, h, line);

        int previewX = x + 14;
        int previewY = y + 14;
        int previewW = w - 28;
        int previewH = Math.min(94, h - 86);
        graphics.fill(previewX, previewY, previewX + previewW,
                previewY + previewH, 0xFF020709);
        border(graphics, previewX, previewY, previewW, previewH,
                0xFF274E5D);
        int sweep = previewY + (int) ((now / 18L) % Math.max(1, previewH));
        graphics.fill(previewX + 1, sweep, previewX + previewW - 1,
                Math.min(previewY + previewH - 1, sweep + 2), 0x263DD4F4);
        graphics.drawCenteredString(font, ScpFonts.montserratTitle("079"),
                previewX + previewW / 2, previewY + previewH / 2 - 7,
                0xFFDCECF0);

        graphics.drawString(font, ScpFonts.montserrat(SCP_079.name),
                x + 14, y + h - 58, 0xFFFFFFFF, false);
        graphics.drawString(font, ScpFonts.roboto(SCP_079.nickname),
                x + 14, y + h - 43, 0xFF9CC9D7, false);
        graphics.drawString(font, ScpFonts.roboto(SCP_079.objectClass),
                x + 14, y + h - 26, 0xFFE5BD55, false);
        String hint = chosen ? "SELECTED" : hovered ? "CLICK TO SELECT" : "AVAILABLE";
        int hintColor = chosen ? 0xFFE5BD55 : 0xFF6F9DAE;
        graphics.drawString(font, ScpFonts.roboto(hint),
                x + w - font.width(hint) - 14, y + h - 26,
                hintColor, false);
        pose.popPose();
    }

    private void renderDetails(GuiGraphics graphics, Layout layout, long now) {
        int baseX = layout.x + layout.leftW + 28;
        int x = baseX + Math.round((1.0F - detailProgress) * 18.0F);
        int y = layout.y + 76;
        int right = layout.x + layout.w - 24;

        if (selected == null) {
            graphics.drawString(font, ScpFonts.montserrat("SELECT AN SCP"),
                    baseX, y + 10, 0xFF7EA9B9, false);
            graphics.drawString(font,
                    ScpFonts.roboto("Choose an available anomaly to review its playable abilities."),
                    baseX, y + 30, 0xFF557A88, false);
            return;
        }

        graphics.drawString(font, ScpFonts.montserratTitle(selected.name),
                x, y, 0xFFFFFFFF, false);
        graphics.drawString(font, ScpFonts.roboto("\"" + selected.nickname + "\""),
                x, y + 18, 0xFF9CC9D7, false);
        graphics.drawString(font, ScpFonts.roboto(selected.category),
                x, y + 36, 0xFF6F9DAE, false);

        int badgeW = 60;
        graphics.fill(right - badgeW, y, right, y + 18, 0xD53C3217);
        border(graphics, right - badgeW, y, badgeW, 18, 0xFFE5BD55);
        graphics.drawCenteredString(font, ScpFonts.roboto(selected.objectClass),
                right - badgeW / 2, y + 5, 0xFFFFDF78);

        graphics.drawString(font, ScpFonts.montserrat("ABILITIES"),
                x, y + 66, 0xFFE9F8FF, false);
        int ay = y + 84;
        for (Ability ability : selected.abilities) {
            graphics.fill(x, ay + 3, x + 3, ay + 28, 0xFF4EC4E3);
            graphics.drawString(font, ScpFonts.roboto(ability.name),
                    x + 10, ay, 0xFFBDEEFF, false);
            graphics.drawString(font, ScpFonts.roboto(ability.description),
                    x + 10, ay + 13, 0xFF789EAC, false);
            ay += 36;
        }

        int noteY = Math.min(confirmY(layout) - 42, ay + 4);
        graphics.fill(x, noteY, right, noteY + 30, 0xA818292F);
        graphics.drawString(font,
                ScpFonts.roboto("NETWORK ACCESS REQUIRES AUXILIARY POWER"),
                x + 9, noteY + 6, 0xFFE5BD55, false);
        graphics.drawString(font,
                ScpFonts.roboto("Without it, SCP-079 remains at the local physical host."),
                x + 9, noteY + 18, 0xFF7797A2, false);

        renderConfirm(graphics, layout, now);
    }

    private void renderConfirm(GuiGraphics graphics, Layout layout, long now) {
        int x = confirmX(layout);
        int y = confirmY(layout);
        int w = confirmW(layout);
        int h = 30;
        boolean enabled = selected != null || scp079Active;
        int fill = !enabled ? 0xA5111A1F
                : blend(0xD51B4657, 0xE52C6A7E, confirmHover);
        int line = !enabled ? 0xFF34434A
                : blend(0xFF5D9CAF, 0xFFE5F8FF, confirmHover);
        graphics.fill(x, y, x + w, y + h, fill);
        border(graphics, x, y, w, h, line);
        String label = scp079Active ? "RETURN TO HUMAN"
                : selected == null ? "SELECT AN SCP"
                : "CONFIRM TRANSFORMATION";
        int textColor = enabled ? 0xFFFFFFFF : 0xFF66757B;
        graphics.drawCenteredString(font, ScpFonts.montserrat(label),
                x + w / 2, y + 10, textColor);

        if (enabled) {
            float pulse = (float) (0.5D + 0.5D * Math.sin(now * 0.006D));
            int px = x + 7 + Math.round(pulse * 5.0F);
            graphics.fill(px, y + h - 3, x + w - 7, y + h - 2,
                    0x845EC8E5);
        }
    }

    private void renderClose(GuiGraphics graphics, Layout layout,
            int mouseX, int mouseY) {
        int x = layout.x + layout.w - 35;
        int y = layout.y + 17;
        boolean hover = inside(mouseX, mouseY, x - 5, y - 5, 22, 22);
        graphics.drawString(font, ScpFonts.montserrat("X"), x, y,
                hover ? 0xFFFFFFFF : 0xFF7599A6, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        Layout layout = layout();
        float open = easeOutCubic(Mth.clamp((Util.getMillis() - openedAt) / 230.0F,
                0.0F, 1.0F));
        layout = new Layout(layout.x,
                layout.y + Math.round((1.0F - open) * 14.0F),
                layout.w, layout.h, layout.leftW);

        int closeX = layout.x + layout.w - 40;
        if (inside(mouseX, mouseY, closeX, layout.y + 10, 30, 30)) {
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
                confirmW(layout), 30) && (selected != null || scp079Active)) {
            confirmSelection();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && (selected != null || scp079Active)) {
            confirmSelection();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void confirmSelection() {
        if (scp079Active) {
            ScpRoleSelectorNetwork.requestRole(ScpRoleSelectorNetwork.Role.HUMAN);
        } else if (selected == SCP_079) {
            ScpRoleSelectorNetwork.requestRole(ScpRoleSelectorNetwork.Role.SCP_079);
        }
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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
    private int cardY(Layout layout) { return layout.y + 112; }
    private int cardW(Layout layout) { return layout.leftW - 40; }
    private int cardH(Layout layout) { return Math.min(205, layout.h - 152); }
    private int confirmX(Layout layout) { return layout.x + layout.leftW + 28; }
    private int confirmW(Layout layout) { return layout.w - layout.leftW - 52; }
    private int confirmY(Layout layout) { return layout.y + layout.h - 48; }

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

    private record Layout(int x, int y, int w, int h, int leftW) {
    }

    private record Ability(String name, String description) {
    }

    private record RoleCard(String name, String nickname, String objectClass,
            String category, List<Ability> abilities) {
    }
}
