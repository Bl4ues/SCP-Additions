package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.save.SaveDifficultyPolicy;

import java.util.ArrayList;
import java.util.List;

/** SCP Additions presentation layered over vanilla DeathScreen behavior. */
public final class ScpDeathScreen extends DeathScreen {
    private static final long BLACKOUT_MS = 1000L;
    private static final long REVEAL_MS = 620L;

    private static final int BACKGROUND = 0xFF030405;
    private static final int CARD = 0xF20B0D10;
    private static final int CARD_ALT = 0xD5121519;
    private static final int BORDER = 0xFF394047;
    private static final int TEXT = 0xFFF3F4F5;
    private static final int MUTED = 0xFF9EA4AA;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE5CC72;
    private static final int DANGER = 0xFF8E242B;
    private static final int DANGER_BRIGHT = 0xFFD45C62;

    private final Component causeOfDeath;
    private final boolean hardcoreMode;
    private final long openedAt = Util.getMillis();
    private Button loadButton;
    private Button menuButton;

    public ScpDeathScreen(Component causeOfDeath, boolean hardcore) {
        super(causeOfDeath, hardcore);
        this.causeOfDeath = causeOfDeath == null
                ? Component.literal("Unknown cause of death.") : causeOfDeath;
        this.hardcoreMode = hardcore;
    }

    @Override
    protected void init() {
        super.init();
        identifyButtons();
        positionButtons(cardX(), cardY());
    }

    private void identifyButtons() {
        loadButton = null;
        menuButton = null;
        List<Button> buttons = new ArrayList<>();
        for (var child : children()) {
            if (child instanceof Button button) buttons.add(button);
        }

        String respawn = Component.translatable("deathScreen.respawn").getString();
        String title = Component.translatable("deathScreen.titleScreen").getString();
        for (Button button : buttons) {
            String label = button.getMessage().getString();
            if (!hardcoreMode && respawn.equals(label)) {
                loadButton = button;
                button.setMessage(Component.literal("Load Game"));
            } else if (title.equals(label)) {
                menuButton = button;
                button.setMessage(Component.literal("Main Menu"));
            }
        }

        // Translation packs or mods can replace the literal button text while
        // preserving the vanilla order. Keep the callbacks rather than cloning
        // them so respawn-altering mods remain compatible with this screen.
        if (loadButton == null && !hardcoreMode && !buttons.isEmpty()) {
            loadButton = buttons.get(0);
            loadButton.setMessage(Component.literal("Load Game"));
        }
        if (menuButton == null && buttons.size() >= 2) {
            menuButton = buttons.get(buttons.size() - 1);
            menuButton.setMessage(Component.literal("Main Menu"));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND);

        float reveal = revealProgress();
        if (reveal <= 0.0F) return;

        drawRedVignette(graphics, reveal);
        int slide = Math.round((1.0F - reveal) * 14.0F);
        int x = cardX();
        int y = cardY() + slide;
        int cardWidth = cardWidth();
        int cardHeight = cardHeight();
        positionButtons(x, y);

        int cardColor = alpha(CARD, reveal);
        int altColor = alpha(CARD_ALT, reveal);
        int borderColor = alpha(BORDER, reveal);
        int accentColor = alpha(DANGER_BRIGHT, reveal);

        graphics.fill(x, y, x + cardWidth, y + cardHeight, cardColor);
        graphics.fill(x, y, x + 4, y + cardHeight, accentColor);
        graphics.fill(x, y, x + cardWidth, y + 1, borderColor);
        graphics.fill(x, y + cardHeight - 1, x + cardWidth,
                y + cardHeight, borderColor);
        graphics.fill(x + 4, y + 58, x + cardWidth, y + 59,
                alpha(DANGER, reveal * 0.75F));

        Minecraft minecraft = Minecraft.getInstance();
        graphics.drawString(minecraft.font, ScpFonts.montserrat("YOU DIED"),
                x + 22, y + 18, alpha(TEXT, reveal), false);
        graphics.drawString(minecraft.font,
                ScpFonts.titillium("PERSONNEL TERMINATION REPORT"),
                x + 22, y + 39, alpha(DANGER_BRIGHT, reveal), false);

        String designation = minecraft.player == null ? "Unknown"
                : minecraft.player.getName().getString();
        String difficulty = minecraft.level == null ? "Euclid"
                : SaveDifficultyPolicy.displayName(
                        minecraft.level.getDifficulty());
        String save = SaveGameClientState.lastMethod().displayName();

        drawRow(graphics, "DESIGNATION", designation, x + 22, y + 78,
                cardWidth - 44, reveal);
        drawRow(graphics, "DIFFICULTY", difficulty, x + 22, y + 103,
                cardWidth - 44, reveal);
        drawRow(graphics, "SAVE", save, x + 22, y + 128,
                cardWidth - 44, reveal);

        int causeY = y + 166;
        graphics.drawString(minecraft.font, ScpFonts.titillium("CAUSE OF DEATH"),
                x + 22, causeY, alpha(DANGER_BRIGHT, reveal), false);
        graphics.fill(x + 22, causeY + 17, x + cardWidth - 22, causeY + 18,
                alpha(BORDER, reveal));
        int lineY = causeY + 27;
        int maxTextWidth = cardWidth - 44;
        for (var line : minecraft.font.split(ScpFonts.roboto(causeOfDeath),
                maxTextWidth)) {
            graphics.drawString(minecraft.font, line, x + 22, lineY,
                    alpha(TEXT, reveal), false);
            lineY += minecraft.font.lineHeight + 3;
            if (lineY > y + cardHeight - 92) break;
        }

        graphics.fill(x + 16, y + cardHeight - 82,
                x + cardWidth - 16, y + cardHeight - 81,
                alpha(BORDER, reveal * 0.7F));
        drawButton(graphics, loadButton, mouseX, mouseY, reveal);
        drawButton(graphics, menuButton, mouseX, mouseY, reveal);

        // Hardcore's first vanilla action is Spectate World rather than Respawn.
        // Preserve it instead of lying by relabelling it as Load Game.
        if (hardcoreMode) {
            for (var child : children()) {
                if (child instanceof Button button && button != menuButton) {
                    drawButton(graphics, button, mouseX, mouseY, reveal);
                    break;
                }
            }
        }
    }

    private void drawRow(GuiGraphics graphics, String label, String value,
            int x, int y, int rowWidth, float reveal) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.fill(x, y - 4, x + rowWidth, y + 17,
                alpha(CARD_ALT, reveal * 0.56F));
        graphics.drawString(minecraft.font, ScpFonts.titillium(label),
                x + 7, y + 2, alpha(MUTED, reveal), false);
        graphics.drawString(minecraft.font, ScpFonts.roboto(value),
                x + Math.min(116, Math.max(82, rowWidth / 4)), y + 2,
                alpha(TEXT, reveal), false);
    }

    private void drawButton(GuiGraphics graphics, Button button,
            int mouseX, int mouseY, float reveal) {
        if (button == null || !button.visible) return;
        boolean hovered = button.active && button.isMouseOver(mouseX, mouseY);
        int x = button.getX();
        int y = button.getY();
        int right = x + button.getWidth();
        int bottom = y + button.getHeight();
        int background = button.active
                ? hovered ? 0xD21A2026 : 0xB80E1216 : 0x80101417;
        int border = button.active
                ? hovered ? ACCENT_BRIGHT : BORDER : 0xFF24292E;
        int stripe = button.active
                ? hovered ? ACCENT_BRIGHT : ACCENT : 0xFF33383D;
        int text = button.active ? TEXT : MUTED;

        graphics.fill(x, y, right, bottom, alpha(background, reveal));
        graphics.fill(x, y, right, y + 1, alpha(border, reveal));
        graphics.fill(x, bottom - 1, right, bottom, alpha(border, reveal));
        graphics.fill(x, y, x + 4, bottom, alpha(stripe, reveal));
        Component label = ScpFonts.roboto(button.getMessage());
        int textX = x + (button.getWidth()
                - Minecraft.getInstance().font.width(label)) / 2;
        int textY = y + (button.getHeight()
                - Minecraft.getInstance().font.lineHeight) / 2 + 1;
        graphics.drawString(Minecraft.getInstance().font, label,
                textX, textY, alpha(text, reveal), false);
    }

    private void drawRedVignette(GuiGraphics graphics, float reveal) {
        graphics.fill(0, 0, width, height, alpha(0x301A0205, reveal));
        int bands = 8;
        int band = Math.max(10, Math.min(width, height) / 36);
        for (int i = 0; i < bands; i++) {
            float strength = reveal * (bands - i) / (float) bands;
            int color = alpha(0x240E0103, strength);
            int inset = i * band;
            graphics.fill(inset, inset, width - inset, inset + band, color);
            graphics.fill(inset, height - inset - band,
                    width - inset, height - inset, color);
            graphics.fill(inset, inset, inset + band,
                    height - inset, color);
            graphics.fill(width - inset - band, inset,
                    width - inset, height - inset, color);
        }
    }

    private void positionButtons(int x, int y) {
        int buttonWidth = cardWidth() - 44;
        int buttonX = x + 22;
        int firstY = y + cardHeight() - 70;
        if (loadButton != null) {
            loadButton.setX(buttonX);
            loadButton.setY(firstY - 34);
            loadButton.setWidth(buttonWidth);
            loadButton.setHeight(28);
        }
        if (menuButton != null) {
            menuButton.setX(buttonX);
            menuButton.setY(firstY);
            menuButton.setWidth(buttonWidth);
            menuButton.setHeight(28);
        }
    }

    private int cardWidth() {
        return Mth.clamp(width - 80, 360, 620);
    }

    private int cardHeight() {
        return Mth.clamp(height - 70, 300, 390);
    }

    private int cardX() {
        return (width - cardWidth()) / 2;
    }

    private int cardY() {
        return (height - cardHeight()) / 2;
    }

    private float revealProgress() {
        long elapsed = Util.getMillis() - openedAt;
        if (elapsed <= BLACKOUT_MS) return 0.0F;
        float t = Mth.clamp((elapsed - BLACKOUT_MS) / (float) REVEAL_MS,
                0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Util.getMillis() - openedAt < BLACKOUT_MS) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static int alpha(int color, float alpha) {
        int source = color >>> 24;
        int a = Mth.clamp(Math.round(source * Mth.clamp(alpha, 0.0F, 1.0F)),
                0, 255);
        return a << 24 | color & 0x00FFFFFF;
    }
}
