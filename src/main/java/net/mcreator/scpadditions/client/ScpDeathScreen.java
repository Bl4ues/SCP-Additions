package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.mixin.client.DeathScreenInvoker;
import net.mcreator.scpadditions.save.SaveDifficultyPolicy;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** SCP Additions presentation layered over vanilla DeathScreen behavior. */
public final class ScpDeathScreen extends DeathScreen {
    private static final long BLACKOUT_MS = 1000L;
    private static final long REVEAL_MS = 620L;
    private static final long MUFFLE_RAMP_MS = 3400L;
    private static final float FULL_DEATH_MUFFLE = 0.94F;
    private static final float SPECTATE_MUFFLE = 0.47F;

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

    private static final int METABALL_COUNT = 18;
    private static final int METABALL_CELL = 7;

    private final Component causeOfDeath;
    private final boolean hardcoreMode;
    private final boolean mineZeroMode;
    private final long openedAt = Util.getMillis();
    private final long visualSeed = openedAt ^ System.nanoTime();

    private Button loadButton;
    private Button menuButton;
    private Button hardcoreActionButton;
    private Button mineZeroPrimaryButton;
    private Button normalSpectateButton;
    private Button previousSpectateButton;
    private Button nextSpectateButton;

    private boolean menuConfirmationArmed;
    private boolean mineZeroRestoreStarted;
    private boolean normalSpectating;
    private long normalSpectateChangedAt = openedAt;

    public ScpDeathScreen(Component causeOfDeath, boolean hardcore) {
        this(causeOfDeath, hardcore, false);
    }

    private ScpDeathScreen(Component causeOfDeath, boolean hardcore,
            boolean mineZeroMode) {
        super(causeOfDeath, hardcore);
        this.causeOfDeath = causeOfDeath == null
                ? Component.literal("Unknown cause of death.") : causeOfDeath;
        this.hardcoreMode = hardcore;
        this.mineZeroMode = mineZeroMode;
    }

    public static ScpDeathScreen mineZero(Component causeOfDeath) {
        return new ScpDeathScreen(causeOfDeath, false, true);
    }

    @Override
    protected void init() {
        super.init();
        identifyButtons();

        if (mineZeroMode) {
            if (loadButton != null) {
                loadButton.visible = false;
                loadButton.active = false;
            }
            mineZeroPrimaryButton = addRenderableWidget(Button.builder(
                    Component.literal("Spectate"), ignored ->
                            handleMineZeroPrimary()).bounds(0, 0, 200, 28).build());
        } else if (!hardcoreMode) {
            normalSpectateButton = addRenderableWidget(Button.builder(
                    Component.literal("Spectate"), ignored ->
                            toggleNormalSpectate()).bounds(0, 0, 100, 28).build());
        }

        previousSpectateButton = addRenderableWidget(Button.builder(
                Component.literal("<"), ignored -> cycleSpectatedPlayer(-1))
                .bounds(0, 0, 30, 26).build());
        nextSpectateButton = addRenderableWidget(Button.builder(
                Component.literal(">"), ignored -> cycleSpectatedPlayer(1))
                .bounds(0, 0, 30, 26).build());
        previousSpectateButton.visible = false;
        nextSpectateButton.visible = false;
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

    /** Target intensity consumed by the shared OpenAL low-pass mixer. */
    public float requestedDeathMuffleStrength() {
        long age = Math.max(0L, Util.getMillis() - openedAt);
        float progress = smootherStep(Mth.clamp(
                age / (float) MUFFLE_RAMP_MS, 0.0F, 1.0F));
        float target = isSpectating() ? SPECTATE_MUFFLE : FULL_DEATH_MUFFLE;
        return target * progress;
    }

    public void beginMineZeroRestore() {
        mineZeroRestoreStarted = true;
        if (mineZeroPrimaryButton != null) mineZeroPrimaryButton.active = false;
        if (menuButton != null) menuButton.active = false;
        if (previousSpectateButton != null) previousSpectateButton.active = false;
        if (nextSpectateButton != null) nextSpectateButton.active = false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        boolean spectating = isSpectating();
        Viewport viewport = viewport();

        if (spectating) drawSpectateMask(graphics, viewport);
        else graphics.fill(0, 0, width, height, BACKGROUND);

        float reveal = revealProgress();
        if (reveal <= 0.0F) return;

        drawLivingRedBackground(graphics, reveal, spectating ? viewport : null);
        drawRedVignette(graphics, reveal, spectating ? viewport : null);

        int slide = Math.round((1.0F - reveal) * 14.0F);
        int centeredX = cardX();
        int leftX = Math.max(20, Math.round(width * 0.035F));
        float layout = spectateLayoutProgress();
        int x = Mth.lerpInt(layout, centeredX, leftX);
        int y = cardY() + slide;
        int cardWidth = cardWidth();
        int cardHeight = cardHeight();
        positionButtons(x, y);
        updateSpectateWidgets(viewport);

        float restoreZoom = mineZeroMode && mineZeroRestoreStarted
                ? MineZeroClientState.restoreZoomProgress() : 0.0F;
        float restoreScale = 1.0F + restoreZoom * 0.13F;
        float restoreAlpha = 1.0F - restoreZoom * 0.40F;

        graphics.pose().pushPose();
        if (restoreZoom > 0.0F) {
            float centerX = x + cardWidth / 2.0F;
            float centerY = y + cardHeight / 2.0F;
            graphics.pose().translate(centerX, centerY, 0.0F);
            graphics.pose().scale(restoreScale, restoreScale, 1.0F);
            graphics.pose().translate(-centerX, -centerY, 0.0F);
        }
        drawReportCard(graphics, mouseX, mouseY, reveal * restoreAlpha,
                x, y, cardWidth, cardHeight);
        graphics.pose().popPose();

        if (spectating) drawSpectateFrame(graphics, viewport, reveal);
    }

    private void drawReportCard(GuiGraphics graphics, int mouseX, int mouseY,
            float reveal, int x, int y, int cardWidth, int cardHeight) {
        int cardColor = alpha(CARD, reveal);
        int borderColor = alpha(BORDER, reveal);
        int accentColor = alpha(DANGER_BRIGHT, reveal);

        graphics.fill(x, y, x + cardWidth, y + cardHeight, cardColor);
        graphics.fill(x, y, x + 4, y + cardHeight, accentColor);
        graphics.fill(x, y, x + cardWidth, y + 1, borderColor);
        graphics.fill(x, y + cardHeight - 1, x + cardWidth,
                y + cardHeight, borderColor);
        graphics.fill(x + 4, y + 56, x + cardWidth, y + 57,
                alpha(DANGER, reveal * 0.75F));

        Minecraft minecraft = Minecraft.getInstance();
        graphics.drawString(minecraft.font, ScpFonts.montserrat("YOU DIED"),
                x + 20, y + 16, alpha(TEXT, reveal), false);
        graphics.drawString(minecraft.font,
                ScpFonts.titillium("PERSONNEL TERMINATION REPORT"),
                x + 20, y + 37, alpha(DANGER_BRIGHT, reveal), false);

        String designation = minecraft.player == null ? "Unknown"
                : minecraft.player.getName().getString();
        String difficulty = minecraft.level == null ? "Euclid"
                : SaveDifficultyPolicy.displayName(minecraft.level.getDifficulty());
        String save = SaveGameClientState.lastMethod().displayName();

        drawRow(graphics, "DESIGNATION", designation, x + 20, y + 74,
                cardWidth - 40, reveal);
        drawRow(graphics, "DIFFICULTY", difficulty, x + 20, y + 98,
                cardWidth - 40, reveal);
        drawRow(graphics, "SAVE", save, x + 20, y + 122,
                cardWidth - 40, reveal);

        int causeY = y + 156;
        graphics.drawString(minecraft.font, ScpFonts.titillium("CAUSE OF DEATH"),
                x + 20, causeY, alpha(DANGER_BRIGHT, reveal), false);
        graphics.fill(x + 20, causeY + 17, x + cardWidth - 20, causeY + 18,
                alpha(BORDER, reveal));
        int lineY = causeY + 27;
        int separatorY = buttonSeparatorY(y);
        for (var line : minecraft.font.split(ScpFonts.roboto(causeOfDeath),
                cardWidth - 40)) {
            graphics.drawString(minecraft.font, line, x + 20, lineY,
                    alpha(TEXT, reveal), false);
            lineY += minecraft.font.lineHeight + 3;
            if (lineY > separatorY - 29) break;
        }

        if (mineZeroMode && MineZeroClientState.allDead()) {
            String vote = "ROLLBACK VOTE  " + MineZeroClientState.votes()
                    + " / " + MineZeroClientState.requiredVotes();
            Component text = ScpFonts.titillium(vote);
            int voteX = x + (cardWidth - minecraft.font.width(text)) / 2;
            graphics.drawString(minecraft.font, text, voteX,
                    separatorY - 18, alpha(MUTED, reveal), false);
        }

        graphics.fill(x + 14, separatorY,
                x + cardWidth - 14, separatorY + 1,
                alpha(BORDER, reveal * 0.7F));

        if (mineZeroMode) {
            drawButton(graphics, mineZeroPrimaryButton, mouseX, mouseY, reveal);
        } else if (hardcoreMode) {
            drawButton(graphics, hardcoreActionButton, mouseX, mouseY, reveal);
        } else {
            drawButton(graphics, loadButton, mouseX, mouseY, reveal);
            drawButton(graphics, normalSpectateButton, mouseX, mouseY, reveal);
        }
        drawButton(graphics, menuButton, mouseX, mouseY, reveal);
    }

    private void drawRow(GuiGraphics graphics, String label, String value,
            int x, int y, int rowWidth, float reveal) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.fill(x, y - 4, x + rowWidth, y + 17,
                alpha(CARD_ALT, reveal * 0.56F));
        graphics.drawString(minecraft.font, ScpFonts.titillium(label),
                x + 7, y + 2, alpha(MUTED, reveal), false);
        graphics.drawString(minecraft.font, ScpFonts.roboto(value),
                x + Math.min(102, Math.max(74, rowWidth / 3)), y + 2,
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
                        ? DANGER_BRIGHT : hovered ? ACCENT_BRIGHT : ACCENT
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

    /** Organic metaball field distributed across the entire dead background. */
    private void drawLivingRedBackground(GuiGraphics graphics, float reveal,
            Viewport protectedViewport) {
        double seconds = Math.max(0L, Util.getMillis() - openedAt) / 1000.0D;
        int cell = Math.max(6, METABALL_CELL);

        double[] cx = new double[METABALL_COUNT];
        double[] cy = new double[METABALL_COUNT];
        double[] rx = new double[METABALL_COUNT];
        double[] ry = new double[METABALL_COUNT];
        double[] bias = new double[METABALL_COUNT];
        for (int i = 0; i < METABALL_COUNT; i++) {
            double p1 = seedPhase(i);
            double p2 = seedPhase(i + 37);
            double speed = 0.045D + (i % 6) * 0.011D;
            cx[i] = width * (0.50D
                    + 0.48D * Math.sin(seconds * speed + p1)
                    + 0.08D * Math.sin(seconds * speed * 2.13D + p2));
            cy[i] = height * (0.50D
                    + 0.46D * Math.cos(seconds * speed * 0.79D + p2)
                    + 0.09D * Math.sin(seconds * speed * 1.61D + p1));
            double pulse = 1.0D
                    + 0.18D * Math.sin(seconds * (0.17D + i * 0.009D) + p1)
                    + 0.07D * Math.sin(seconds * 0.41D + p2);
            rx[i] = width * (0.095D + (i % 5) * 0.011D) * pulse;
            ry[i] = height * (0.135D + (i % 4) * 0.014D)
                    * (1.70D - pulse * 0.48D);
            bias[i] = (i % 5) / 4.0D;
        }

        for (int y = 0; y < height; y += cell) {
            for (int x = 0; x < width; x += cell) {
                if (protectedViewport != null
                        && protectedViewport.containsCell(x, y, cell)) continue;

                double px = x + cell * 0.5D;
                double py = y + cell * 0.5D;
                double globalWarpX = Math.sin(py * 0.017D + seconds * 0.29D) * 0.16D;
                double globalWarpY = Math.sin(px * 0.013D - seconds * 0.23D) * 0.13D;
                double field = 0.0D;
                double colorBias = 0.0D;

                for (int i = 0; i < METABALL_COUNT; i++) {
                    double dx = (px - cx[i]) / Math.max(1.0D, rx[i]) + globalWarpX;
                    double dy = (py - cy[i]) / Math.max(1.0D, ry[i]) + globalWarpY;
                    double d2 = dx * dx + dy * dy;
                    if (d2 > 5.5D) continue;
                    double inv = 1.0D / (1.0D + d2 * 2.35D);
                    double contribution = inv * inv;
                    field += contribution;
                    colorBias += contribution * bias[i];
                }

                if (field < 0.045D) continue;
                double edge = Mth.clamp((field - 0.045D) / 0.74D,
                        0.0D, 1.0D);
                double breathing = 0.82D + 0.18D * Math.sin(
                        seconds * 0.31D + x * 0.005D - y * 0.004D);
                float opacity = (float) (reveal * breathing
                        * (0.035D + edge * 0.165D));
                int colorMix = Mth.clamp((int) Math.round(
                        24.0D * colorBias / Math.max(0.001D, field)), 0, 24);
                int red = 42 + colorMix;
                int green = 3 + colorMix / 6;
                int blue = 8 + colorMix / 3;
                int color = 0xFF000000 | red << 16 | green << 8 | blue;
                graphics.fill(x, y, Math.min(width, x + cell + 1),
                        Math.min(height, y + cell + 1), alpha(color, opacity));
            }
        }
    }

    private double seedPhase(int index) {
        long mixed = visualSeed ^ (0x9E3779B97F4A7C15L * (index + 1L));
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        long positive = mixed & Long.MAX_VALUE;
        return (positive % 100000L) / 100000.0D * Math.PI * 2.0D;
    }

    private void drawRedVignette(GuiGraphics graphics, float reveal,
            Viewport protectedViewport) {
        if (protectedViewport == null) {
            graphics.fill(0, 0, width, height, alpha(0x181A0205, reveal));
        }
        int bands = 9;
        int band = Math.max(10, Math.min(width, height) / 38);
        for (int i = 0; i < bands; i++) {
            float strength = reveal * (bands - i) / (float) bands;
            int color = alpha(0x210E0103, strength);
            int inset = i * band;
            graphics.fill(inset, inset, width - inset, inset + band, color);
            graphics.fill(inset, height - inset - band,
                    width - inset, height - inset, color);
            graphics.fill(inset, inset, inset + band, height - inset, color);
            graphics.fill(width - inset - band, inset,
                    width - inset, height - inset, color);
        }
    }

    private void drawSpectateMask(GuiGraphics graphics, Viewport viewport) {
        graphics.fill(0, 0, width, viewport.top(), BACKGROUND);
        graphics.fill(0, viewport.bottom(), width, height, BACKGROUND);
        graphics.fill(0, viewport.top(), viewport.left(), viewport.bottom(), BACKGROUND);
        graphics.fill(viewport.right(), viewport.top(), width, viewport.bottom(), BACKGROUND);
        graphics.fill(viewport.left(), viewport.top(), viewport.right(),
                viewport.bottom(), 0x24020304);
    }

    private void drawSpectateFrame(GuiGraphics graphics, Viewport viewport,
            float reveal) {
        drawCameraInterference(graphics, viewport, reveal);

        int border = alpha(BORDER, reveal);
        int accent = alpha(DANGER_BRIGHT, reveal * 0.82F);
        graphics.fill(viewport.left() - 2, viewport.top() - 2,
                viewport.right() + 2, viewport.top(), border);
        graphics.fill(viewport.left() - 2, viewport.bottom(),
                viewport.right() + 2, viewport.bottom() + 2, border);
        graphics.fill(viewport.left() - 2, viewport.top(),
                viewport.left(), viewport.bottom(), accent);
        graphics.fill(viewport.right(), viewport.top(),
                viewport.right() + 2, viewport.bottom(), border);

        Minecraft minecraft = Minecraft.getInstance();
        Component label = ScpFonts.titillium("LIVE PERSONNEL FEED");
        Component name = ScpFonts.roboto(MineZeroSpectateClient.targetName());
        graphics.drawString(minecraft.font, label, viewport.left(),
                viewport.top() - 18, alpha(DANGER_BRIGHT, reveal), false);
        int nameX = viewport.left()
                + (viewport.width() - minecraft.font.width(name)) / 2;
        graphics.drawString(minecraft.font, name, nameX,
                viewport.bottom() + 10, alpha(TEXT, reveal), false);
        drawButton(graphics, previousSpectateButton,
                Integer.MIN_VALUE, Integer.MIN_VALUE, reveal);
        drawButton(graphics, nextSpectateButton,
                Integer.MIN_VALUE, Integer.MIN_VALUE, reveal);
    }

    private void drawCameraInterference(GuiGraphics graphics,
            Viewport viewport, float reveal) {
        long now = Util.getMillis();
        float burst = MineZeroSpectateClient.switchInterference();

        // Persistent low-level scan noise.
        for (int y = viewport.top() + 1; y < viewport.bottom(); y += 4) {
            int wave = (int) ((y + now / 35L) & 7L);
            int a = 7 + (wave == 0 ? 5 : 0);
            graphics.fill(viewport.left(), y, viewport.right(), y + 1,
                    alpha(0xFFB9C0C5, reveal * a / 255.0F));
        }

        long frame = now / 52L;
        int strips = 10 + Math.round(burst * 22.0F);
        for (int i = 0; i < strips; i++) {
            long hash = interferenceHash(frame + i * 37L);
            int feedHeight = Math.max(1, viewport.height());
            int y = viewport.top() + Math.floorMod((int) hash, feedHeight);
            int h = 1 + Math.floorMod((int) (hash >>> 11), 3 + Math.round(burst * 7));
            int inset = Math.floorMod((int) (hash >>> 19), Math.max(1, viewport.width() / 5));
            int rightInset = Math.floorMod((int) (hash >>> 27), Math.max(1, viewport.width() / 6));
            int a = 8 + Math.round(burst * 82.0F);
            int tint = ((hash >>> 34) & 1L) == 0L ? 0x00D8DDE0 : 0x008B2028;
            graphics.fill(viewport.left() + inset, y,
                    Math.max(viewport.left() + inset + 1, viewport.right() - rightInset),
                    Math.min(viewport.bottom(), y + h),
                    Mth.clamp(a, 0, 120) << 24 | tint);
        }

        if (burst > 0.0F) {
            int dark = Mth.clamp(Math.round(burst * 82.0F), 0, 82);
            graphics.fill(viewport.left(), viewport.top(), viewport.right(),
                    viewport.bottom(), dark << 24 | 0x00020507);
        }
    }

    private static long interferenceHash(long value) {
        long x = value + 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    private void updateSpectateWidgets(Viewport viewport) {
        boolean spectating = isSpectating();
        boolean targets = MineZeroSpectateClient.hasTargets();

        if (mineZeroMode && mineZeroPrimaryButton != null) {
            boolean allDead = MineZeroClientState.allDead();
            if (allDead) {
                mineZeroPrimaryButton.setMessage(Component.literal("Load Game"));
                mineZeroPrimaryButton.active = !MineZeroClientState.restoring();
            } else if (spectating) {
                mineZeroPrimaryButton.setMessage(Component.literal("Spectating"));
                mineZeroPrimaryButton.active = false;
            } else {
                mineZeroPrimaryButton.setMessage(Component.literal("Spectate"));
                mineZeroPrimaryButton.active = MineZeroClientState.livingPlayers() > 0;
            }
        }

        if (!mineZeroMode && normalSpectateButton != null) {
            if (normalSpectating && !targets) stopNormalSpectate();
            normalSpectateButton.visible = targets;
            normalSpectateButton.active = targets;
            normalSpectateButton.setMessage(Component.literal(
                    normalSpectating ? "Return" : "Spectate"));
        }

        boolean arrows = isSpectating() && targets;
        previousSpectateButton.visible = arrows;
        previousSpectateButton.active = arrows;
        nextSpectateButton.visible = arrows;
        nextSpectateButton.active = arrows;
        previousSpectateButton.setX(viewport.left() + 8);
        previousSpectateButton.setY(viewport.bottom() + 4);
        nextSpectateButton.setX(viewport.right() - 38);
        nextSpectateButton.setY(viewport.bottom() + 4);
    }

    private void handleMineZeroPrimary() {
        if (!mineZeroMode || MineZeroClientState.restoring()) return;
        if (MineZeroClientState.allDead()) MineZeroClientState.voteToRestore();
        else MineZeroClientState.startSpectating();
    }

    private void toggleNormalSpectate() {
        if (mineZeroMode || hardcoreMode) return;
        if (normalSpectating) {
            stopNormalSpectate();
            return;
        }
        MineZeroSpectateClient.start();
        if (!MineZeroSpectateClient.active()) return;
        normalSpectating = true;
        normalSpectateChangedAt = Util.getMillis();
    }

    private void stopNormalSpectate() {
        if (!normalSpectating && !MineZeroSpectateClient.active()) return;
        normalSpectating = false;
        normalSpectateChangedAt = Util.getMillis();
        MineZeroSpectateClient.stop();
    }

    private void cycleSpectatedPlayer(int direction) {
        if (mineZeroMode) MineZeroClientState.cycleSpectatedPlayer(direction);
        else if (normalSpectating) MineZeroSpectateClient.cycle(direction);
    }

    private boolean isSpectating() {
        return mineZeroMode ? MineZeroClientState.spectating()
                : normalSpectating && MineZeroSpectateClient.active();
    }

    private float spectateLayoutProgress() {
        if (mineZeroMode) return MineZeroClientState.spectateLayoutProgress();
        long elapsed = Math.max(0L, Util.getMillis() - normalSpectateChangedAt);
        float t = Mth.clamp(elapsed / 520.0F, 0.0F, 1.0F);
        t = t * t * (3.0F - 2.0F * t);
        return normalSpectating ? t : 1.0F - t;
    }

    private void positionButtons(int x, int y) {
        int fullWidth = cardWidth() - 40;
        int buttonX = x + 20;
        int menuY = y + cardHeight() - 42;
        int upperY = menuY - 35;
        int buttonHeight = 28;

        if (mineZeroMode) {
            placeButton(mineZeroPrimaryButton, buttonX, upperY,
                    fullWidth, buttonHeight, true);
        } else if (hardcoreMode) {
            placeButton(hardcoreActionButton, buttonX, upperY,
                    fullWidth, buttonHeight, true);
        } else {
            boolean showSpectate = normalSpectateButton != null
                    && MineZeroSpectateClient.hasTargets();
            if (showSpectate) {
                int half = (fullWidth - 8) / 2;
                placeButton(loadButton, buttonX, upperY, half, buttonHeight, true);
                placeButton(normalSpectateButton, buttonX + half + 8, upperY,
                        fullWidth - half - 8, buttonHeight, true);
            } else {
                placeButton(loadButton, buttonX, upperY,
                        fullWidth, buttonHeight, true);
                if (normalSpectateButton != null) normalSpectateButton.visible = false;
            }
        }
        placeButton(menuButton, buttonX, menuY, fullWidth, buttonHeight, true);
    }

    private static void placeButton(Button button, int x, int y,
            int width, int height, boolean visible) {
        if (button == null) return;
        button.setX(x);
        button.setY(y);
        button.setWidth(width);
        button.setHeight(height);
        button.visible = visible;
    }

    private int buttonSeparatorY(int y) {
        return y + cardHeight() - 91;
    }

    private int cardWidth() {
        if (width < 700) return Mth.clamp(Math.round(width * 0.52F), 320, 410);
        return Mth.clamp(Math.round(width * 0.42F), 340, 470);
    }

    private int cardHeight() {
        return Mth.clamp(Math.round(height * 0.78F), 320, 370);
    }

    private int cardX() {
        return (width - cardWidth()) / 2;
    }

    private int cardY() {
        return (height - cardHeight()) / 2;
    }

    private Viewport viewport() {
        int left = Math.max(width / 2 + 28, Math.round(width * 0.53F));
        int right = Math.max(left + 120, width - 28);
        int top = Math.max(46, Math.round(height * 0.095F));
        int bottom = Math.max(top + 100, height - 64);
        return new Viewport(left, top, right, bottom);
    }

    private float revealProgress() {
        long elapsed = Util.getMillis() - openedAt;
        if (elapsed <= BLACKOUT_MS) return 0.0F;
        return smootherStep(Mth.clamp(
                (elapsed - BLACKOUT_MS) / (float) REVEAL_MS,
                0.0F, 1.0F));
    }

    private boolean blackoutActive() {
        return Util.getMillis() - openedAt < BLACKOUT_MS;
    }

    private void prepareNormalLoadGame() {
        stopNormalSpectate();
        SaveGameClientState.suppressForLoadGame();
        EnterSoundClient.play();
        MineZeroRestoreVisualClient.start();
    }

    private void handleMenuButtonPress() {
        if (menuButton == null || !menuButton.active) return;
        menuButton.playDownSound(Minecraft.getInstance().getSoundManager());
        if (!menuConfirmationArmed) {
            menuConfirmationArmed = true;
            menuButton.setMessage(Component.literal("Confirm"));
            return;
        }
        stopAnySpectate();
        ((DeathScreenInvoker) (Object) this).scpAdditions$exitToTitleScreen();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (blackoutActive()) return true;
        if (menuButton != null && menuButton.visible && menuButton.active
                && menuButton.isMouseOver(mouseX, mouseY)) {
            handleMenuButtonPress();
            return true;
        }
        if (!mineZeroMode && loadButton != null && loadButton.visible
                && loadButton.active && loadButton.isMouseOver(mouseX, mouseY)) {
            prepareNormalLoadGame();
            loadButton.playDownSound(Minecraft.getInstance().getSoundManager());
            loadButton.onPress();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
            double dragX, double dragY) {
        if (isSpectating() && button == 0 && viewport().contains(mouseX, mouseY)) {
            if (mineZeroMode) MineZeroClientState.orbit(dragX, dragY);
            else MineZeroSpectateClient.orbit(dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (blackoutActive()) return true;
        boolean activate = keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_SPACE;
        if (menuButton != null && menuButton.isFocused() && activate) {
            handleMenuButtonPress();
            return true;
        }
        if (!mineZeroMode && loadButton != null && loadButton.isFocused()
                && activate) {
            prepareNormalLoadGame();
            loadButton.playDownSound(Minecraft.getInstance().getSoundManager());
            loadButton.onPress();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        stopAnySpectate();
        super.onClose();
    }

    @Override
    public void removed() {
        stopAnySpectate();
        super.removed();
    }

    private void stopAnySpectate() {
        if (mineZeroMode) MineZeroClientState.stopSpectating();
        else stopNormalSpectate();
    }

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static int alpha(int color, float alpha) {
        int source = color >>> 24;
        int a = Mth.clamp(Math.round(source * Mth.clamp(alpha, 0.0F, 1.0F)),
                0, 255);
        return a << 24 | color & 0x00FFFFFF;
    }

    private record Viewport(int left, int top, int right, int bottom) {
        int width() { return Math.max(0, right - left); }
        int height() { return Math.max(0, bottom - top); }

        boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }

        boolean containsCell(int x, int y, int size) {
            return x + size > left && x < right && y + size > top && y < bottom;
        }
    }
}
