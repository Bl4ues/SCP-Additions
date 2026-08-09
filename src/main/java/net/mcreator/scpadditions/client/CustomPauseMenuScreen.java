package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Animated in-world pause presentation. Vanilla/Forge buttons are kept as the
 * authoritative actions and merely receive an SCP Additions presentation.
 */
public final class CustomPauseMenuScreen extends PauseScreen {
    private static final ResourceLocation LOGO_OUTER = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/menu/loading_1.png");
    private static final ResourceLocation LOGO_INNER = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/menu/loading_2.png");
    private static final int LOGO_TEXTURE_SIZE = 512;

    private static final int TEXT = 0xFFF5F6F7;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;
    private static final int BUTTON_BASE = 0x780B0E12;
    private static final int BUTTON_HOVER = 0xB5161B22;
    private static final int PAUSE_DIM = 0xA8000000;

    private static final long ENTER_MS = 620L;
    private static final long BUTTON_STAGGER_MS = 46L;
    private static final long BUTTON_ENTER_MS = 410L;
    private static final long EXIT_MS = 280L;

    private static final String RESUME_KEY = "menu.returnToGame";
    private static final String ADVANCEMENTS_KEY = "gui.advancements";
    private static final String STATISTICS_KEY = "gui.stats";
    private static final String LAN_KEY = "menu.shareToLan";
    private static final String OPTIONS_KEY = "menu.options";
    private static final String MODS_KEY = "fml.menu.mods";
    private static final String DISCONNECT_KEY = "menu.disconnect";

    private static final Set<String> KNOWN_KEYS = Set.of(
            RESUME_KEY, ADVANCEMENTS_KEY, STATISTICS_KEY, LAN_KEY,
            OPTIONS_KEY, MODS_KEY, DISCONNECT_KEY);

    private final Map<String, AbstractButton> sourceButtons = new HashMap<>();
    private final Set<AbstractButton> capturedSources =
            java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<PauseMenuButton> menuButtons = new ArrayList<>();
    private final List<AbstractButton> injectedSources = new ArrayList<>();

    private long openedAt;
    private long lastFrameAt;
    private long leavingAt = -1L;
    private Runnable pendingAction;
    private float logoOuterAngle;
    private float logoInnerAngle;
    private float hoverBoost;
    private boolean injectedCollected;

    public CustomPauseMenuScreen() {
        super(true);
    }

    @Override
    protected void init() {
        super.init();
        sourceButtons.clear();
        capturedSources.clear();
        menuButtons.clear();
        injectedSources.clear();
        injectedCollected = false;
        leavingAt = -1L;
        pendingAction = null;
        hoverBoost = 0.0F;
        logoOuterAngle = -420.0F;
        logoInnerAngle = 360.0F;

        openedAt = Util.getMillis();
        lastFrameAt = openedAt;

        captureKnownSources();
        hideSourceWidgets();
        buildKnownButtons();
    }

    @Override
    public void tick() {
        super.tick();
        if (!ClientModulePreferences.customPauseMenuEnabled()) {
            Minecraft.getInstance().setScreen(new PauseScreen(true));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        collectInjectedButtons();
        hideSourceWidgets();

        long now = Util.getMillis();
        float delta = Math.min(0.10F,
                Math.max(0.0F, (now - lastFrameAt) / 1000.0F));
        lastFrameAt = now;

        updateAnimation(mouseX, mouseY, delta, now);

        graphics.fill(0, 0, this.width, this.height, PAUSE_DIM);
        drawLogo(graphics, now);
        layoutAndRenderButtons(graphics, mouseX, mouseY, partialTick, now);

        if (leavingAt >= 0L) {
            float leave = Mth.clamp((now - leavingAt) / (float) EXIT_MS,
                    0.0F, 1.0F);
            graphics.fill(0, 0, this.width, this.height,
                    Math.round(72.0F * smootherStep(leave)) << 24);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && leavingAt < 0L) {
            AbstractButton resume = sourceButtons.get(RESUME_KEY);
            beginExit(resume != null ? resume::onPress
                    : () -> Minecraft.getInstance().setScreen(null));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void captureKnownSources() {
        for (GuiEventListener listener : new ArrayList<>(this.children())) {
            if (!(listener instanceof AbstractButton button)) continue;
            String key = translationKey(button.getMessage());
            if (KNOWN_KEYS.contains(key)) {
                sourceButtons.putIfAbsent(key, button);
                capturedSources.add(button);
            }
        }
    }

    private void collectInjectedButtons() {
        if (injectedCollected) return;

        boolean foundNew = false;
        for (GuiEventListener listener : new ArrayList<>(this.children())) {
            if (!(listener instanceof AbstractButton button)
                    || button instanceof PauseMenuButton
                    || capturedSources.contains(button)) {
                continue;
            }

            String key = translationKey(button.getMessage());
            if (KNOWN_KEYS.contains(key)) {
                sourceButtons.putIfAbsent(key, button);
                capturedSources.add(button);
                foundNew = true;
                continue;
            }

            String text = button.getMessage().getString().trim();
            if (text.isBlank() || isVanillaUtilityButton(text)) continue;
            capturedSources.add(button);
            injectedSources.add(button);
            foundNew = true;
        }

        if (foundNew) rebuildButtons();
        injectedCollected = true;
    }

    private static boolean isVanillaUtilityButton(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        return normalized.contains("feedback")
                || normalized.contains("report bugs")
                || normalized.contains("player reporting");
    }

    private void buildKnownButtons() {
        rebuildButtons();
    }

    private void rebuildButtons() {
        for (PauseMenuButton button : menuButtons) {
            this.removeWidget(button);
        }
        menuButtons.clear();

        addSourceButton(RESUME_KEY, "Resume");
        addSourceButton(ADVANCEMENTS_KEY, "Achievements");
        addSourceButton(STATISTICS_KEY, "Statistics");
        addSourceButton(LAN_KEY, "Open to LAN");

        AbstractButton options = sourceButtons.get(OPTIONS_KEY);
        if (options != null) {
            addButton(new PauseMenuButton(ScpFonts.roboto("Settings"),
                    () -> PauseMenuSettingsPanelClient.toggle(this),
                    options));
        }

        addSourceButton(MODS_KEY, "Mods");

        for (AbstractButton source : injectedSources) {
            addButton(new PauseMenuButton(ScpFonts.roboto(source.getMessage()),
                    () -> beginExit(source::onPress), source));
        }

        addSourceButton(DISCONNECT_KEY, "Main Menu");
    }

    private void addSourceButton(String key, String label) {
        AbstractButton source = sourceButtons.get(key);
        if (source == null) return;
        addButton(new PauseMenuButton(ScpFonts.roboto(label),
                () -> beginExit(source::onPress), source));
    }

    private void addButton(PauseMenuButton button) {
        menuButtons.add(this.addRenderableWidget(button));
    }

    private void hideSourceWidgets() {
        for (GuiEventListener listener : new ArrayList<>(this.children())) {
            if (listener instanceof AbstractWidget widget
                    && !(widget instanceof PauseMenuButton)) {
                widget.visible = false;
            }
        }
    }

    private void updateAnimation(int mouseX, int mouseY,
            float delta, long now) {
        boolean hovered = false;
        for (PauseMenuButton button : menuButtons) {
            hovered |= button.active && button.visible
                    && button.isMouseOver(mouseX, mouseY);
        }
        float hoverTarget = hovered ? 1.0F : 0.0F;
        hoverBoost = approach(hoverBoost, hoverTarget, delta * 5.8F);

        float leaveBoost = leavingAt >= 0L ? 1.0F : 0.0F;
        float speed = 5.0F + 26.0F * hoverBoost + 120.0F * leaveBoost;
        logoOuterAngle = wrapAngle(logoOuterAngle + speed * delta);
        logoInnerAngle = wrapAngle(logoInnerAngle - speed * 0.88F * delta);

        if (leavingAt >= 0L && now - leavingAt >= EXIT_MS) {
            Runnable action = pendingAction;
            pendingAction = null;
            leavingAt = -1L;
            if (action != null) action.run();
        }
    }

    private void drawLogo(GuiGraphics graphics, long now) {
        int size = Mth.clamp(Math.round(this.height * 0.82F), 290, 640);
        float enter = Mth.clamp((now - openedAt) / (float) ENTER_MS,
                0.0F, 1.0F);
        float eased = easeOutBack(enter);

        float leave = leavingAt < 0L ? 0.0F
                : smootherStep(Mth.clamp((now - leavingAt)
                / (float) EXIT_MS, 0.0F, 1.0F));

        int finalCenterX = Math.round(this.width * 0.055F);
        int startCenterX = -size / 2;
        int centerX = Math.round(Mth.lerp(eased,
                startCenterX, finalCenterX));
        centerX -= Math.round(leave * (finalCenterX + size / 2 + 40));
        int centerY = Math.round(this.height * 0.53F);

        float entrySpin = (1.0F - smootherStep(enter)) * -520.0F;
        drawRotatedTexture(graphics, LOGO_OUTER, centerX, centerY,
                size, logoOuterAngle + entrySpin, 0.23F);
        drawRotatedTexture(graphics, LOGO_INNER, centerX, centerY,
                size, logoInnerAngle - entrySpin * 0.82F, 0.17F);
    }

    private void layoutAndRenderButtons(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick, long now) {
        int count = menuButtons.size();
        if (count == 0) return;

        int width = Mth.clamp(Math.round(this.width * 0.28F), 225, 350);
        int height = Mth.clamp(Math.round(this.height * 0.057F), 28, 38);
        int gap = Math.max(6, Math.round(this.height * 0.011F));
        int total = count * height + Math.max(0, count - 1) * gap;
        int finalX = Math.max(46, Math.round(this.width * 0.105F));
        int startY = Math.max(48, (this.height - total) / 2);

        float leave = leavingAt < 0L ? 0.0F
                : smootherStep(Mth.clamp((now - leavingAt)
                / (float) EXIT_MS, 0.0F, 1.0F));

        for (int index = 0; index < count; index++) {
            PauseMenuButton button = menuButtons.get(index);
            long localStart = openedAt + index * BUTTON_STAGGER_MS;
            float progress = Mth.clamp((now - localStart)
                    / (float) BUTTON_ENTER_MS, 0.0F, 1.0F);
            float eased = easeOutBack(progress);

            int startX = -width - 36 - index * 8;
            int x = Math.round(Mth.lerp(eased, startX, finalX));
            x -= Math.round(leave * (finalX + width + 60));
            int y = startY + index * (height + gap);

            button.setX(x);
            button.setY(y);
            button.setWidth(width);
            button.setHeight(height);
            button.visible = progress > 0.02F;
            button.active = leavingAt < 0L && progress > 0.88F
                    && (button.source == null || button.source.active);
            button.renderAlpha = Mth.clamp(progress * 1.35F, 0.0F, 1.0F)
                    * (1.0F - leave);
            if (button.visible) {
                button.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        PauseMenuSettingsPanelClient.render(this, graphics,
                mouseX, mouseY, partialTick, now, finalX + width + 16,
                startY, width, height, gap);
    }

    private void beginExit(Runnable action) {
        if (action == null || leavingAt >= 0L) return;
        PauseMenuSettingsPanelClient.close(this);
        leavingAt = Util.getMillis();
        pendingAction = action;
        for (PauseMenuButton button : menuButtons) button.active = false;
    }

    private void drawRotatedTexture(GuiGraphics graphics,
            ResourceLocation texture, int centerX, int centerY,
            int size, float angle, float alpha) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getResourceManager().getResource(texture).isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        graphics.blit(texture, -size / 2, -size / 2,
                size, size, 0.0F, 0.0F,
                LOGO_TEXTURE_SIZE, LOGO_TEXTURE_SIZE,
                LOGO_TEXTURE_SIZE, LOGO_TEXTURE_SIZE);
        graphics.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static String translationKey(Component component) {
        if (component != null
                && component.getContents() instanceof TranslatableContents t) {
            return t.getKey();
        }
        return "";
    }

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static float easeOutBack(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F) - 1.0F;
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        return 1.0F + c3 * t * t * t + c1 * t * t;
    }

    private static float approach(float current, float target, float amount) {
        if (current < target) return Math.min(target, current + amount);
        if (current > target) return Math.max(target, current - amount);
        return current;
    }

    private static float wrapAngle(float angle) {
        float wrapped = angle % 360.0F;
        return wrapped < 0.0F ? wrapped + 360.0F : wrapped;
    }

    private final class PauseMenuButton extends AbstractButton {
        private final Runnable action;
        private final AbstractButton source;
        private float hoverProgress;
        private long hoverUpdatedAt = Util.getMillis();
        private float renderAlpha = 1.0F;

        private PauseMenuButton(Component message, Runnable action,
                AbstractButton source) {
            super(0, 0, 220, 30, message);
            this.action = action;
            this.source = source;
        }

        @Override
        public void onPress() {
            if (this.active && this.action != null) this.action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            long now = Util.getMillis();
            float delta = Math.min(0.10F,
                    Math.max(0.0F, (now - hoverUpdatedAt) / 1000.0F));
            hoverUpdatedAt = now;
            float target = this.isHoveredOrFocused() ? 1.0F : 0.0F;
            hoverProgress = approach(hoverProgress, target, delta * 8.0F);
            float eased = smootherStep(hoverProgress);

            float alpha = this.active ? renderAlpha : renderAlpha * 0.48F;
            int baseAlpha = Math.round(0x78 * alpha);
            int hoverAlpha = Math.round(0xB5 * alpha);
            int backgroundAlpha = Math.round(Mth.lerp(eased,
                    baseAlpha, hoverAlpha));
            int rgb = eased > 0.001F
                    ? (BUTTON_HOVER & 0x00FFFFFF)
                    : (BUTTON_BASE & 0x00FFFFFF);
            graphics.fill(this.getX(), this.getY(),
                    this.getX() + this.getWidth(),
                    this.getY() + this.getHeight(),
                    (backgroundAlpha << 24) | rgb);

            int accentAlpha = Math.round(255.0F * alpha);
            int accentWidth = Math.max(3, Math.round(3.0F + eased * 2.0F));
            graphics.fill(this.getX(), this.getY(),
                    this.getX() + accentWidth,
                    this.getY() + this.getHeight(),
                    (accentAlpha << 24) | (ACCENT & 0x00FFFFFF));

            Font font = Minecraft.getInstance().font;
            int color = withAlpha(eased > 0.45F
                    ? ACCENT_BRIGHT : TEXT, alpha);
            float textScale = 1.24F;
            float scaledHeight = font.lineHeight * textScale;
            float textY = this.getY()
                    + (this.getHeight() - scaledHeight) * 0.5F;
            graphics.pose().pushPose();
            graphics.pose().translate(this.getX() + 14
                    + Math.round(eased * 5.0F), textY, 0.0F);
            graphics.pose().scale(textScale, textScale, 1.0F);
            graphics.drawString(font, this.getMessage(), 0, 0,
                    color, false);
            graphics.pose().popPose();
        }

        @Override
        protected void updateWidgetNarration(
                NarrationElementOutput narration) {
            this.defaultButtonNarrationText(narration);
        }
    }

    private static int withAlpha(int color, float alpha) {
        int sourceAlpha = color >>> 24;
        int finalAlpha = Mth.clamp(Math.round(sourceAlpha
                * Mth.clamp(alpha, 0.0F, 1.0F)), 0, 255);
        return (finalAlpha << 24) | (color & 0x00FFFFFF);
    }
}
