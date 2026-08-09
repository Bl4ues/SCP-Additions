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
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.ModList;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ui.ClientPreferencesMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * SCP Additions title presentation. The class intentionally remains a
 * TitleScreen so Forge and other mods can inject their normal title-menu
 * buttons; those buttons are collected into Extras instead of being discarded.
 */
public final class CustomMainMenuScreen extends TitleScreen {
    private static final ResourceLocation MENU_OVERLAY = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/menu_background.png");
    private static final ResourceLocation FALLBACK_BACKGROUND = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/menu/loading_screen.png");
    private static final ResourceLocation SPINNER_OUTER = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/menu/loading_1.png");
    private static final ResourceLocation SPINNER_INNER = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/menu/loading_2.png");
    private static final ResourceLocation CONFIG_LOGO = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/logo.png");

    private static final ResourceLocation[] BACKGROUND_CANDIDATES = {
            new ResourceLocation(ScpAdditionsMod.MODID,
                    "textures/screens/placeholder_background_1.png"),
            new ResourceLocation(ScpAdditionsMod.MODID,
                    "textures/screens/placeholder_background_2.png"),
            new ResourceLocation(ScpAdditionsMod.MODID,
                    "textures/screens/placeholder_background_3.png"),
            new ResourceLocation(ScpAdditionsMod.MODID,
                    "textures/screens/placeholder_background_4.png")
    };

    private static final int REFERENCE_WIDTH = 1920;
    private static final int REFERENCE_HEIGHT = 1080;
    private static final int SPINNER_TEXTURE_SIZE = 512;

    private static final int TEXT = 0xFFF5F6F7;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;
    private static final int PANEL = 0xC70B0E12;
    private static final int BUTTON_BASE = 0x7A0B0E12;
    private static final int BUTTON_HOVER = 0xB5161B22;

    private static final long BACKGROUND_HOLD_MS = 18_000L;
    private static final long BACKGROUND_FADE_MS = 2_400L;
    private static final long SCREEN_TRANSITION_MS = 260L;
    private static final long OPEN_FADE_MS = 520L;

    private static final Set<String> PRIMARY_KEYS = Set.of(
            "menu.singleplayer",
            "menu.multiplayer",
            "menu.options",
            "menu.quit");
    private static final String REALMS_KEY = "menu.online";

    private static final List<String> CHANGELOG_HIGHLIGHTS = List.of(
            "SCP-106",
            "Reworked SCP-079 facility control",
            "Core Room Elevator",
            "Reworked survival systems",
            "Custom HUD, inventory and presentation"
    );

    private final Map<String, AbstractButton> sourceButtons = new HashMap<>();
    private final Set<AbstractButton> capturedSources =
            java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<MenuTextButton> primaryButtons = new ArrayList<>();
    private final List<MenuTextButton> extraButtons = new ArrayList<>();
    private final List<ResourceLocation> backgrounds = new ArrayList<>();

    private boolean extrasOpen;
    private boolean extrasBuilt;
    private float extrasProgress;
    private float hoverBoost;
    private float spinnerOuterAngle;
    private float spinnerInnerAngle;
    private long lastFrameAt;
    private long openedAt;
    private long backgroundStartedAt;
    private int backgroundOffset;

    private long transitionStartedAt = -1L;
    private Runnable pendingTransition;

    public CustomMainMenuScreen() {
        super();
    }

    @Override
    protected void init() {
        super.init();

        primaryButtons.clear();
        extraButtons.clear();
        sourceButtons.clear();
        capturedSources.clear();
        backgrounds.clear();
        extrasOpen = false;
        extrasBuilt = false;
        extrasProgress = 0.0F;
        hoverBoost = 0.0F;

        long now = Util.getMillis();
        openedAt = now;
        lastFrameAt = now;
        backgroundStartedAt = now;
        transitionStartedAt = -1L;
        pendingTransition = null;

        refreshAvailableBackgrounds();
        captureVanillaSources();
        hideSourceWidgets();
        buildPrimaryButtons();
    }

    @Override
    public void tick() {
        super.tick();
        if (!ClientModulePreferences.customMainMenuEnabled()) {
            Minecraft.getInstance().setScreen(new TitleScreen());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        if (!ClientModulePreferences.customMainMenuEnabled()) {
            return;
        }

        ensureExtrasBuilt();
        hideSourceWidgets();

        long now = Util.getMillis();
        float deltaSeconds = Math.min(0.10F,
                Math.max(0.0F, (now - lastFrameAt) / 1000.0F));
        lastFrameAt = now;

        updateAnimations(mouseX, mouseY, deltaSeconds, now);

        drawSlideshow(graphics, now);
        drawMenuOverlay(graphics);
        drawSpinner(graphics);
        drawBranding(graphics);
        drawWhatsNew(graphics);

        for (MenuTextButton button : primaryButtons) {
            button.render(graphics, mouseX, mouseY, partialTick);
        }
        renderExtraButtons(graphics, mouseX, mouseY, partialTick);

        drawTransition(graphics, now);
        drawOpeningFade(graphics, now);
    }

    private void captureVanillaSources() {
        for (GuiEventListener listener :
                new ArrayList<>(this.children())) {
            if (!(listener instanceof AbstractButton button)) continue;
            String key = translationKey(button.getMessage());
            if (PRIMARY_KEYS.contains(key) || REALMS_KEY.equals(key)) {
                sourceButtons.putIfAbsent(key, button);
                capturedSources.add(button);
            }
        }
    }

    private void ensureExtrasBuilt() {
        if (extrasBuilt) return;

        AbstractButton realms = sourceButtons.get(REALMS_KEY);
        if (realms != null) {
            addExtraSource(realms);
        }

        for (GuiEventListener listener :
                new ArrayList<>(this.children())) {
            if (!(listener instanceof AbstractButton button)
                    || button instanceof MenuTextButton
                    || capturedSources.contains(button)) {
                continue;
            }

            String key = translationKey(button.getMessage());
            String text = button.getMessage().getString().trim();
            if (PRIMARY_KEYS.contains(key)) {
                sourceButtons.putIfAbsent(key, button);
                capturedSources.add(button);
                continue;
            }
            if (REALMS_KEY.equals(key)) {
                sourceButtons.putIfAbsent(key, button);
                capturedSources.add(button);
                addExtraSource(button);
                continue;
            }
            if (text.isBlank() || isCopyrightButton(text)) {
                continue;
            }
            addExtraSource(button);
        }

        extrasBuilt = true;
        hideSourceWidgets();
    }

    private static boolean isCopyrightButton(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        return normalized.contains("copyright")
                && normalized.contains("mojang");
    }

    private void addExtraSource(AbstractButton source) {
        if (extraButtons.stream().anyMatch(
                button -> button.source == source)) {
            return;
        }
        capturedSources.add(source);

        MenuTextButton button = new MenuTextButton(
                0, 0, 190, 24,
                ScpFonts.roboto(source.getMessage()),
                () -> beginScreenTransition(source::onPress),
                null, source);
        button.visible = false;
        extraButtons.add(this.addRenderableWidget(button));
    }

    private void hideSourceWidgets() {
        for (GuiEventListener listener :
                new ArrayList<>(this.children())) {
            if (listener instanceof AbstractWidget widget
                    && !(widget instanceof MenuTextButton)) {
                widget.visible = false;
            }
        }
    }

    private void buildPrimaryButtons() {
        int left = Math.max(34, Math.round(this.width * 0.055F));
        int buttonWidth = Mth.clamp(Math.round(this.width * 0.235F),
                184, 274);
        int buttonHeight = Mth.clamp(Math.round(this.height * 0.052F),
                24, 31);
        int gap = Math.max(5, Math.round(this.height * 0.012F));
        int y = Math.round(this.height * 0.315F);

        y = addPrimarySource(left, y, buttonWidth, buttonHeight,
                "menu.singleplayer", "Singleplayer", gap);
        y = addPrimarySource(left, y, buttonWidth, buttonHeight,
                "menu.multiplayer", "Multiplayer", gap);
        y = addPrimary(left, y, buttonWidth, buttonHeight,
                ScpFonts.roboto("Configuration Center"),
                () -> beginScreenTransition(this::openConfigurationCenter),
                CONFIG_LOGO, null, gap);
        y = addPrimarySource(left, y, buttonWidth, buttonHeight,
                "menu.options", "Options", gap);
        y = addPrimary(left, y, buttonWidth, buttonHeight,
                ScpFonts.roboto("Extras"), this::toggleExtras,
                null, null, gap + 5);
        addPrimarySource(left, y, buttonWidth, buttonHeight,
                "menu.quit", "Quit", gap);
    }

    private int addPrimarySource(int x, int y, int width, int height,
            String key, String fallback, int gap) {
        AbstractButton source = sourceButtons.get(key);
        MenuTextButton button = new MenuTextButton(
                x, y, width, height, labelFor(key, fallback),
                sourceAction(key), null, source);
        button.active = source != null && source.active;
        primaryButtons.add(this.addRenderableWidget(button));
        return y + height + gap;
    }

    private int addPrimary(int x, int y, int width, int height,
            Component label, Runnable action, ResourceLocation icon,
            AbstractButton source, int gap) {
        MenuTextButton button = new MenuTextButton(
                x, y, width, height, label, action, icon, source);
        primaryButtons.add(this.addRenderableWidget(button));
        return y + height + gap;
    }

    private Component labelFor(String key, String fallback) {
        AbstractButton source = sourceButtons.get(key);
        return ScpFonts.roboto(source == null
                ? Component.literal(fallback) : source.getMessage());
    }

    private Runnable sourceAction(String key) {
        return () -> {
            AbstractButton source = sourceButtons.get(key);
            if (source != null && source.active) {
                beginScreenTransition(source::onPress);
            }
        };
    }

    private void openConfigurationCenter() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(ClientPreferencesMenu.open(minecraft, this));
    }

    private void toggleExtras() {
        if (transitionStartedAt >= 0L) return;
        extrasOpen = !extrasOpen;
    }

    private void beginScreenTransition(Runnable action) {
        if (action == null || transitionStartedAt >= 0L) return;
        transitionStartedAt = Util.getMillis();
        pendingTransition = action;
        setMenuActive(false);
    }

    private void setMenuActive(boolean active) {
        for (MenuTextButton button : primaryButtons) {
            button.active = active;
        }
        for (MenuTextButton button : extraButtons) {
            button.active = active && extrasOpen;
        }
    }

    private void refreshAvailableBackgrounds() {
        Minecraft minecraft = Minecraft.getInstance();
        for (ResourceLocation candidate : BACKGROUND_CANDIDATES) {
            if (minecraft.getResourceManager()
                    .getResource(candidate).isPresent()) {
                backgrounds.add(candidate);
            }
        }
        if (backgrounds.isEmpty()) {
            backgrounds.add(FALLBACK_BACKGROUND);
        }
        backgroundOffset = backgrounds.size() <= 1 ? 0
                : ThreadLocalRandom.current().nextInt(backgrounds.size());
    }

    private void updateAnimations(int mouseX, int mouseY,
            float deltaSeconds, long now) {
        float extrasTarget = extrasOpen ? 1.0F : 0.0F;
        extrasProgress = approach(extrasProgress, extrasTarget,
                deltaSeconds * 6.6F);

        boolean hovered = false;
        for (MenuTextButton button : primaryButtons) {
            if (button.source != null && transitionStartedAt < 0L) {
                button.active = button.source.active;
            }
            hovered |= button.active && button.isMouseOver(mouseX, mouseY);
        }
        for (MenuTextButton button : extraButtons) {
            if (extrasProgress > 0.05F) {
                hovered |= button.isMouseOver(mouseX, mouseY);
            }
        }

        float hoverTarget = hovered || Math.abs(extrasTarget - extrasProgress) > 0.01F
                ? 1.0F : 0.0F;
        hoverBoost = approach(hoverBoost, hoverTarget,
                deltaSeconds * 5.5F);

        float transitionBoost = transitionStartedAt >= 0L ? 1.0F : 0.0F;
        float degreesPerSecond = 4.5F
                + 24.0F * hoverBoost
                + 42.0F * transitionBoost;
        spinnerOuterAngle = wrapAngle(
                spinnerOuterAngle + degreesPerSecond * deltaSeconds);
        spinnerInnerAngle = wrapAngle(
                spinnerInnerAngle - degreesPerSecond * 0.86F * deltaSeconds);

        if (transitionStartedAt >= 0L
                && now - transitionStartedAt >= SCREEN_TRANSITION_MS) {
            Runnable action = pendingTransition;
            pendingTransition = null;
            transitionStartedAt = -1L;
            if (action != null) {
                action.run();
            }
        }
    }

    private void drawSlideshow(GuiGraphics graphics, long now) {
        if (backgrounds.isEmpty()) {
            graphics.fill(0, 0, this.width, this.height, 0xFF090C11);
            return;
        }

        if (backgrounds.size() == 1) {
            drawCoverTexture(graphics, backgrounds.get(0), 1.0F);
            return;
        }

        long cycle = BACKGROUND_HOLD_MS;
        long elapsed = Math.max(0L, now - backgroundStartedAt);
        long slot = elapsed / cycle;
        long within = elapsed % cycle;
        int current = (int) ((backgroundOffset + slot) % backgrounds.size());
        int next = (current + 1) % backgrounds.size();

        drawCoverTexture(graphics, backgrounds.get(current), 1.0F);
        if (within > cycle - BACKGROUND_FADE_MS) {
            float progress = (within - (cycle - BACKGROUND_FADE_MS))
                    / (float) BACKGROUND_FADE_MS;
            drawCoverTexture(graphics, backgrounds.get(next),
                    smootherStep(progress));
        }
    }

    private void drawMenuOverlay(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getResourceManager().getResource(MENU_OVERLAY).isPresent()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(MENU_OVERLAY, 0, 0, this.width, this.height,
                    0.0F, 0.0F, REFERENCE_WIDTH, REFERENCE_HEIGHT,
                    REFERENCE_WIDTH, REFERENCE_HEIGHT);
            RenderSystem.disableBlend();
            return;
        }

        // Temporary fallback while the authored translucent overlay is absent.
        int opaqueEnd = Math.round(this.width * 0.52F);
        int fadeEnd = Math.round(this.width * 0.68F);
        graphics.fill(0, 0, opaqueEnd, this.height, 0xE80A0D12);
        int steps = 18;
        for (int step = 0; step < steps; step++) {
            float t = step / (float) steps;
            int alpha = Math.round(Mth.lerp(t, 0xE8, 0x00));
            int x1 = Math.round(Mth.lerp(step / (float) steps,
                    opaqueEnd, fadeEnd));
            int x2 = Math.round(Mth.lerp((step + 1) / (float) steps,
                    opaqueEnd, fadeEnd));
            graphics.fill(x1, 0, x2 + 1, this.height, alpha << 24);
        }
    }

    private void drawSpinner(GuiGraphics graphics) {
        int size = Mth.clamp(Math.round(this.height * 0.61F), 210, 390);
        int centerX = 0;
        int centerY = Math.round(this.height * 0.59F);

        drawRotatedTexture(graphics, SPINNER_OUTER, centerX, centerY,
                size, spinnerOuterAngle, 0.17F);
        drawRotatedTexture(graphics, SPINNER_INNER, centerX, centerY,
                size, spinnerInnerAngle, 0.13F);
    }

    private void drawRotatedTexture(GuiGraphics graphics,
            ResourceLocation texture, int centerX, int centerY, int size,
            float angle, float alpha) {
        if (!Minecraft.getInstance().getResourceManager()
                .getResource(texture).isPresent()) {
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
                SPINNER_TEXTURE_SIZE, SPINNER_TEXTURE_SIZE,
                SPINNER_TEXTURE_SIZE, SPINNER_TEXTURE_SIZE);
        graphics.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private void drawBranding(GuiGraphics graphics) {
        Font font = Minecraft.getInstance().font;
        int left = Math.max(34, Math.round(this.width * 0.055F));
        int top = Math.max(28, Math.round(this.height * 0.072F));
        int logoHeight = Mth.clamp(Math.round(this.height * 0.105F), 43, 66);
        int logoWidth = Math.round(logoHeight * (960.0F / 832.0F));

        int titleX = left;
        if (Minecraft.getInstance().getResourceManager()
                .getResource(CONFIG_LOGO).isPresent()) {
            RenderSystem.enableBlend();
            graphics.blit(CONFIG_LOGO, left, top, logoWidth, logoHeight,
                    0.0F, 0.0F, 960, 832, 960, 832);
            RenderSystem.disableBlend();
            titleX += logoWidth + 12;
        }

        float titleScale = this.height < 420 ? 1.22F : 1.48F;
        drawScaledText(graphics, font,
                ScpFonts.montserrat("SCP ADDITIONS"),
                titleX, top + 2, titleScale, TEXT);

        String version = modVersion();
        drawScaledText(graphics, font,
                ScpFonts.titillium("VERSION " + version),
                titleX, top + Math.round(17 * titleScale),
                0.90F, ACCENT_BRIGHT);
    }

    private void drawWhatsNew(GuiGraphics graphics) {
        if (this.width < 720 || this.height < 380) return;

        Font font = Minecraft.getInstance().font;
        int panelWidth = Mth.clamp(Math.round(this.width * 0.30F), 248, 390);
        int panelHeight = Mth.clamp(Math.round(this.height * 0.285F), 132, 176);
        int x = this.width - panelWidth
                - Math.max(24, Math.round(this.width * 0.034F));
        int y = this.height - panelHeight
                - Math.max(26, Math.round(this.height * 0.055F));

        graphics.fill(x, y, x + panelWidth, y + panelHeight, PANEL);
        graphics.fill(x, y, x + panelWidth, y + 2, ACCENT);

        graphics.drawString(font, ScpFonts.montserrat("WHAT'S NEW"),
                x + 15, y + 13, TEXT, false);
        String version = majorVersion(modVersion());
        graphics.drawString(font, ScpFonts.titillium("VERSION " + version),
                x + 15, y + 29, ACCENT_BRIGHT, false);

        int lineY = y + 51;
        int availableWidth = panelWidth - 34;
        for (String highlight : CHANGELOG_HIGHLIGHTS) {
            if (lineY + font.lineHeight > y + panelHeight - 10) break;
            graphics.fill(x + 15, lineY + 4, x + 18, lineY + 7, ACCENT);
            Component text = ScpFonts.roboto(highlight);
            String compact = compactToWidth(font, text.getString(),
                    availableWidth);
            graphics.drawString(font, ScpFonts.roboto(compact),
                    x + 25, lineY, TEXT, false);
            lineY += 19;
        }
    }

    private void renderExtraButtons(GuiGraphics graphics, int mouseX,
            int mouseY, float partialTick) {
        if (extraButtons.isEmpty()) return;

        int primaryRight = 0;
        int primaryTop = Math.round(this.height * 0.315F);
        for (MenuTextButton button : primaryButtons) {
            primaryRight = Math.max(primaryRight,
                    button.getX() + button.getWidth());
        }

        int extraX = primaryRight + 14;
        int extraY = primaryTop;
        int extraWidth = Mth.clamp(Math.round(this.width * 0.195F),
                150, 218);
        int extraHeight = Mth.clamp(Math.round(this.height * 0.045F),
                22, 27);
        int gap = 5;

        int slide = Math.round((1.0F - smootherStep(extrasProgress)) * 28.0F);
        int alpha = Math.round(255.0F * extrasProgress);

        for (MenuTextButton button : extraButtons) {
            button.setX(extraX - slide);
            button.setY(extraY);
            button.setWidth(extraWidth);
            button.setHeight(extraHeight);
            button.visible = extrasProgress > 0.02F;
            button.active = extrasOpen && extrasProgress > 0.78F
                    && transitionStartedAt < 0L
                    && (button.source == null || button.source.active);
            button.renderAlpha = alpha / 255.0F;
            if (button.visible) {
                button.render(graphics, mouseX, mouseY, partialTick);
            }
            extraY += extraHeight + gap;
        }
    }

    private void drawTransition(GuiGraphics graphics, long now) {
        if (transitionStartedAt < 0L) return;
        float progress = Mth.clamp((now - transitionStartedAt)
                / (float) SCREEN_TRANSITION_MS, 0.0F, 1.0F);
        int alpha = Math.round(190.0F * smootherStep(progress));
        graphics.fill(0, 0, this.width, this.height, alpha << 24);
    }

    private void drawOpeningFade(GuiGraphics graphics, long now) {
        float progress = Mth.clamp((now - openedAt)
                / (float) OPEN_FADE_MS, 0.0F, 1.0F);
        if (progress >= 1.0F) return;
        int alpha = Math.round(220.0F * (1.0F - smootherStep(progress)));
        graphics.fill(0, 0, this.width, this.height, alpha << 24);
    }

    private void drawCoverTexture(GuiGraphics graphics,
            ResourceLocation texture, float alpha) {
        if (!Minecraft.getInstance().getResourceManager()
                .getResource(texture).isPresent()) {
            graphics.fill(0, 0, this.width, this.height, 0xFF090C11);
            return;
        }

        float screenAspect = this.width / (float) Math.max(1, this.height);
        float textureAspect = REFERENCE_WIDTH / (float) REFERENCE_HEIGHT;

        float u = 0.0F;
        float v = 0.0F;
        float regionWidth = REFERENCE_WIDTH;
        float regionHeight = REFERENCE_HEIGHT;

        if (screenAspect > textureAspect) {
            regionHeight = REFERENCE_WIDTH / screenAspect;
            v = (REFERENCE_HEIGHT - regionHeight) * 0.5F;
        } else if (screenAspect < textureAspect) {
            regionWidth = REFERENCE_HEIGHT * screenAspect;
            u = (REFERENCE_WIDTH - regionWidth) * 0.5F;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F,
                Mth.clamp(alpha, 0.0F, 1.0F));
        graphics.blit(texture, 0, 0, this.width, this.height,
                u, v, Math.round(regionWidth), Math.round(regionHeight),
                REFERENCE_WIDTH, REFERENCE_HEIGHT);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawScaledText(GuiGraphics graphics, Font font,
            Component text, float x, float y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static String compactToWidth(Font font, String text,
            int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int suffixWidth = font.width(suffix);
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            String candidate = result.toString() + text.charAt(index);
            if (font.width(candidate) + suffixWidth > maxWidth) break;
            result.append(text.charAt(index));
        }
        return result.append(suffix).toString();
    }

    private static String modVersion() {
        return ModList.get().getModContainerById(ScpAdditionsMod.MODID)
                .map(container -> container.getModInfo()
                        .getVersion().toString())
                .orElse("4.0.0");
    }

    private static String majorVersion(String version) {
        if (version == null || version.isBlank()) return "4.0";
        String[] parts = version.split("\\.");
        return parts.length >= 2 ? parts[0] + "." + parts[1] : version;
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

    private static float approach(float current, float target,
            float amount) {
        if (current < target) return Math.min(target, current + amount);
        if (current > target) return Math.max(target, current - amount);
        return current;
    }

    private static float wrapAngle(float angle) {
        float wrapped = angle % 360.0F;
        return wrapped < 0.0F ? wrapped + 360.0F : wrapped;
    }

    private final class MenuTextButton extends AbstractButton {
        private final Runnable action;
        private final ResourceLocation icon;
        private final AbstractButton source;
        private float hoverProgress;
        private long hoverUpdatedAt = Util.getMillis();
        private float renderAlpha = 1.0F;

        private MenuTextButton(int x, int y, int width, int height,
                Component message, Runnable action, ResourceLocation icon,
                AbstractButton source) {
            super(x, y, width, height, message);
            this.action = action;
            this.icon = icon;
            this.source = source;
        }

        @Override
        public void onPress() {
            if (this.active && this.action != null) {
                this.action.run();
            }
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

            float stateAlpha = this.active ? renderAlpha : renderAlpha * 0.48F;
            int baseAlpha = Math.round(0x7A * stateAlpha);
            int hoverAlpha = Math.round(0xB5 * stateAlpha);
            int backgroundAlpha = Math.round(
                    Mth.lerp(eased, baseAlpha, hoverAlpha));
            int backgroundRgb = eased > 0.001F
                    ? (BUTTON_HOVER & 0x00FFFFFF)
                    : (BUTTON_BASE & 0x00FFFFFF);
            graphics.fill(this.getX(), this.getY(),
                    this.getX() + this.getWidth(),
                    this.getY() + this.getHeight(),
                    (backgroundAlpha << 24) | backgroundRgb);

            int accentWidth = Math.max(2, Math.round(2.0F + eased * 2.0F));
            int accentAlpha = Math.round(255.0F * stateAlpha);
            graphics.fill(this.getX(), this.getY(),
                    this.getX() + accentWidth,
                    this.getY() + this.getHeight(),
                    (accentAlpha << 24) | (ACCENT & 0x00FFFFFF));

            int textColor = withAlpha(
                    eased > 0.45F ? ACCENT_BRIGHT : TEXT, stateAlpha);
            int iconX = this.getX() + 10 + Math.round(eased * 4.0F);
            int textX = iconX;

            if (icon != null
                    && Minecraft.getInstance().getResourceManager()
                    .getResource(icon).isPresent()) {
                int iconHeight = Math.max(14, this.getHeight() - 10);
                int iconWidth = Math.round(iconHeight * (960.0F / 832.0F));
                int iconY = this.getY()
                        + (this.getHeight() - iconHeight) / 2;
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, stateAlpha);
                graphics.blit(icon, iconX, iconY, iconWidth, iconHeight,
                        0.0F, 0.0F, 960, 832, 960, 832);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.disableBlend();
                textX += iconWidth + 7;
            }

            Font font = Minecraft.getInstance().font;
            int textY = this.getY()
                    + (this.getHeight() - font.lineHeight) / 2;
            graphics.drawString(font, this.getMessage(),
                    textX, textY, textColor, false);
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
