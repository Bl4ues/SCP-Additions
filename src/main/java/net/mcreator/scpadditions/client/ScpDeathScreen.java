package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.save.SaveDifficultyPolicy;
import org.lwjgl.glfw.GLFW;

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
    private static final int[] BACKGROUND_REDS = {
            0xFF3A050A,
            0xFF26030A,
            0xFF4A0810,
            0xFF300611
    };

    private final Component causeOfDeath;
    private final boolean hardcoreMode;
    private final long openedAt = Util.getMillis();
    private final long visualSeed = openedAt ^ System.nanoTime();
    private Button loadButton;
    private Button menuButton;
    private Button hardcoreActionButton;
    private boolean menuConfirmationArmed;

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
        hardcoreActionButton = null;
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
        if (hardcoreMode) {
            for (Button button : buttons) {
                if (button != menuButton) {
                    hardcoreActionButton = button;
                    break;
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND);

        float reveal = revealProgress();
        if (reveal <= 0.0F) return;

        drawLivingRedBackground(graphics, reveal);
        drawRedVignette(graphics, reveal);
        int slide = Math.round((1.0F - reveal) * 14.0F);
        int x = cardX();
        int y = cardY() + slide;
        int cardWidth = cardWidth();
        int cardHeight = cardHeight();
        positionButtons(x, y);

        int cardColor = alpha(CARD, reveal);
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
        int separatorY = buttonSeparatorY(y);
        for (var line : minecraft.font.split(ScpFonts.roboto(causeOfDeath),
                maxTextWidth)) {
            graphics.drawString(minecraft.font, line, x + 22, lineY,
                    alpha(TEXT, reveal), false);
            lineY += minecraft.font.lineHeight + 3;
            if (lineY > separatorY - 14) break;
        }

        graphics.fill(x + 16, separatorY,
                x + cardWidth - 16, separatorY + 1,
                alpha(BORDER, reveal * 0.7F));
        drawButton(graphics, loadButton, mouseX, mouseY, reveal);
        drawButton(graphics, menuButton, mouseX, mouseY, reveal);
        if (hardcoreMode) {
            drawButton(graphics, hardcoreActionButton,
                    mouseX, mouseY, reveal);
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
                x + Math.min(112, Math.max(78, rowWidth / 4)), y + 2,
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
                ? menuConfirmationArmed && button == menuButton
                        ? DANGER_BRIGHT
                        : hovered ? ACCENT_BRIGHT : ACCENT
                : 0xFF33383D;
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

    /**
     * Slow, seeded red fields made from overlapping softened scanline ellipses.
     * The wobble changes independently per field, so the background never feels
     * like a static radial vignette while remaining dark enough not to compete
     * with the termination report.
     */
    private void drawLivingRedBackground(GuiGraphics graphics, float reveal) {
        double seconds = Math.max(0L, Util.getMillis() - openedAt) / 1000.0D;
        for (int blob = 0; blob < BACKGROUND_REDS.length; blob++) {
            double phase = seedPhase(blob);
            double speed = 0.055D + blob * 0.013D;
            int centerX = (int) Math.round(width * (0.50D
                    + 0.34D * Math.sin(seconds * speed + phase)));
            int centerY = (int) Math.round(height * (0.50D
                    + 0.30D * Math.cos(seconds * (speed * 0.83D)
                    + phase * 1.31D)));
            int radiusX = Math.max(90, (int) Math.round(width
                    * (0.22D + 0.035D * Math.sin(phase * 1.7D))));
            int radiusY = Math.max(70, (int) Math.round(height
                    * (0.25D + 0.040D * Math.cos(phase * 1.4D))));
            drawSoftBlob(graphics, centerX, centerY, radiusX, radiusY,
                    BACKGROUND_REDS[blob], reveal, seconds, phase, blob);
        }
    }

    private void drawSoftBlob(GuiGraphics graphics, int centerX, int centerY,
            int radiusX, int radiusY, int color, float reveal,
            double seconds, double phase, int blobIndex) {
        int layers = 7;
        int slices = 20;
        for (int layer = 0; layer < layers; layer++) {
            float scale = 1.0F - layer * 0.085F;
            float layerAlpha = 0.012F + layer * 0.0022F;
            int scaledY = Math.max(8, Math.round(radiusY * scale));
            float sliceHeight = scaledY * 2.0F / slices;

            for (int slice = 0; slice < slices; slice++) {
                double normalizedY = -1.0D
                        + 2.0D * (slice + 0.5D) / slices;
                double envelope = Math.sqrt(Math.max(0.0D,
                        1.0D - normalizedY * normalizedY));
                double wobble = 1.0D
                        + 0.11D * Math.sin(normalizedY * 4.2D + phase
                        + seconds * (0.13D + blobIndex * 0.018D))
                        + 0.055D * Math.sin(normalizedY * 7.3D
                        + phase * 1.63D - seconds * 0.09D);
                double driftX = radiusX * 0.065D
                        * Math.sin(normalizedY * 2.7D + phase * 0.71D
                        + seconds * 0.10D);
                int halfWidth = Math.max(1, (int) Math.round(radiusX
                        * scale * envelope * wobble));
                int left = (int) Math.round(centerX + driftX) - halfWidth;
                int right = (int) Math.round(centerX + driftX) + halfWidth;
                int top = (int) Math.round(centerY - scaledY
                        + slice * sliceHeight);
                int bottom = Math.max(top + 1,
                        (int) Math.ceil(top + sliceHeight + 1.0F));
                float edgeFade = (float) (0.48D + envelope * 0.52D);
                graphics.fill(left, top, right, bottom,
                        alpha(color, reveal * layerAlpha * edgeFade));
            }
        }
    }

    private double seedPhase(int index) {
        long mixed = visualSeed
                ^ (0x9E3779B97F4A7C15L * (index + 1L));
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        long positive = mixed & Long.MAX_VALUE;
        return (positive % 100000L) / 100000.0D * Math.PI * 2.0D;
    }

    private void drawRedVignette(GuiGraphics graphics, float reveal) {
        graphics.fill(0, 0, width, height, alpha(0x251A0205, reveal));
        int bands = 9;
        int band = Math.max(10, Math.min(width, height) / 38);
        for (int i = 0; i < bands; i++) {
            float strength = reveal * (bands - i) / (float) bands;
            int color = alpha(0x210E0103, strength);
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
        int menuY = y + cardHeight() - 48;
        int upperY = menuY - 38;

        if (loadButton != null) {
            loadButton.setX(buttonX);
            loadButton.setY(upperY);
            loadButton.setWidth(buttonWidth);
            loadButton.setHeight(30);
        }
        if (hardcoreActionButton != null) {
            hardcoreActionButton.setX(buttonX);
            hardcoreActionButton.setY(upperY);
            hardcoreActionButton.setWidth(buttonWidth);
            hardcoreActionButton.setHeight(30);
        }
        if (menuButton != null) {
            menuButton.setX(buttonX);
            menuButton.setY(menuY);
            menuButton.setWidth(buttonWidth);
            menuButton.setHeight(30);
        }
    }

    private int buttonSeparatorY(int y) {
        return y + cardHeight() - 102;
    }

    private int cardWidth() {
        return Mth.clamp(Math.round(width * 0.52F), 380, 560);
    }

    private int cardHeight() {
        return Mth.clamp(height - 36, 330, 430);
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

    private boolean blackoutActive() {
        return Util.getMillis() - openedAt < BLACKOUT_MS;
    }

    private void handleMenuButtonPress() {
        if (menuButton == null || !menuButton.active) return;
        menuButton.playDownSound(Minecraft.getInstance().getSoundManager());
        if (!menuConfirmationArmed) {
            menuConfirmationArmed = true;
            menuButton.setMessage(Component.literal("Confirm"));
            return;
        }

        // Run the real vanilla callback so saving/disconnect behavior remains
        // compatible. Vanilla inserts a ConfirmScreen; immediately activate its
        // Title Screen choice so that intermediate screen never gets rendered.
        menuButton.onPress();
        Minecraft minecraft = Minecraft.getInstance();
        Screen opened = minecraft.screen;
        if (!(opened instanceof ConfirmScreen confirm)) return;

        String titleScreen = Component.translatable(
                "deathScreen.titleScreen").getString();
        Button affirmative = null;
        for (var child : confirm.children()) {
            if (child instanceof Button button && button.active
                    && titleScreen.equals(button.getMessage().getString())) {
                affirmative = button;
                break;
            }
        }
        if (affirmative == null) {
            for (var child : confirm.children()) {
                if (child instanceof Button button && button.active) {
                    affirmative = button;
                    break;
                }
            }
        }
        if (affirmative != null) affirmative.onPress();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (blackoutActive()) return true;
        if (menuButton != null && menuButton.visible && menuButton.active
                && menuButton.isMouseOver(mouseX, mouseY)) {
            handleMenuButtonPress();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (blackoutActive()) return true;
        if (menuButton != null && menuButton.isFocused()
                && (keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_SPACE)) {
            handleMenuButtonPress();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static int alpha(int color, float alpha) {
        int source = color >>> 24;
        int a = Mth.clamp(Math.round(source * Mth.clamp(alpha, 0.0F, 1.0F)),
                0, 255);
        return a << 24 | color & 0x00FFFFFF;
    }
}
