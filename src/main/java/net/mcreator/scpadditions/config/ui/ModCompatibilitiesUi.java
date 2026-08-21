package net.mcreator.scpadditions.config.ui;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.MineZeroCompatibilityClientState;
import net.mcreator.scpadditions.client.ModIntegrationLogoClient;
import net.mcreator.scpadditions.client.Scp939ClientEvents;
import net.mcreator.scpadditions.client.Scp939VoiceClientPreferences;
import net.mcreator.scpadditions.client.SimpleVoiceChatCompatibilityClientState;
import net.mcreator.scpadditions.compat.SimpleVoiceChatPresence;

import java.util.Map;
import java.util.WeakHashMap;

/** Entry point and controls for optional mod integrations. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class ModCompatibilitiesUi {
    private static final String HOME_SCREEN =
            "net.mcreator.scpadditions.config.ui.ConfigCenterClient$HomeScreen";
    private static final String LABEL = "Mod Integrations";
    private static final String MINEZERO_ID = "minezero";
    private static final int INTEGRATION_ICON_SIZE = 26;
    private static final int INTEGRATION_ICON_LEFT = 8;
    private static final int INTEGRATION_ICON_GAP = 10;
    private static final int INTEGRATION_ROW_START = 50;
    private static final int INTEGRATION_ROW_STRIDE = 78;
    private static final int INTEGRATION_DESCRIPTION_OFFSET = 44;
    private static final int MIMICRY_BUTTON_GAP = 8;
    private static final Map<Screen, Button> BUTTONS = new WeakHashMap<>();

    private ModCompatibilitiesUi() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInitPost(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!isHome(screen) || find(screen) != null) return;
        Button button = Button.builder(ScpFonts.roboto(LABEL), ignored ->
                Minecraft.getInstance().setScreen(
                        new ModCompatibilitiesScreen(screen)))
                .bounds(0, 0, 200, 36).build();
        event.addListener(button);
        BUTTONS.put(screen, button);
        arrange(screen, button);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!isHome(screen)) return;
        Button button = find(screen);
        if (button != null) arrange(screen, button);
    }

    private static boolean isHome(Screen screen) {
        return screen != null && HOME_SCREEN.equals(screen.getClass().getName());
    }

    private static Button find(Screen screen) {
        Button cached = BUTTONS.get(screen);
        if (cached != null && screen.children().contains(cached)) return cached;
        for (var child : screen.children()) {
            if (child instanceof Button button
                    && LABEL.equals(button.getMessage().getString())) {
                BUTTONS.put(screen, button);
                return button;
            }
        }
        return null;
    }

    private static void arrange(Screen screen, Button button) {
        int navWidth = clamp(Math.round(screen.width * 0.245F), 320, 410);
        int infoWidth = clamp(Math.round(screen.width * 0.245F), 300, 430);
        int gap = clamp(Math.round(screen.width * 0.014F), 16, 24);
        int total = navWidth + gap + infoWidth;
        int preferred = Math.round(screen.width * 0.365F);
        int navX = clamp(Math.max(preferred, screen.width - total - 54),
                20, Math.max(20, screen.width - total - 20));
        int startY = Math.max(132, Math.round(screen.height * 0.225F));
        int rowHeight = clamp(Math.round(screen.height * 0.060F), 34, 46);
        int rowGap = clamp(Math.round(screen.height * 0.012F), 7, 10);
        button.setX(navX);
        button.setY(startY + 5 * (rowHeight + rowGap));
        button.setWidth(navWidth);
        button.setHeight(rowHeight);
        button.visible = true;
        button.active = true;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class ModCompatibilitiesScreen extends Screen {
        private final Screen parent;
        private Button mineZeroButton;
        private Button voiceChatButton;
        private Button mimicryButton;
        private Button backButton;

        private ModCompatibilitiesScreen(Screen parent) {
            super(Component.literal(LABEL));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int panelWidth = panelWidth();
            int x = ConfigCenterVisuals.contentLeft(width, panelWidth);
            int panelHeight = panelHeight();
            int y = Math.max(18, (height - panelHeight) / 2);

            mineZeroButton = addRenderableWidget(Button.builder(
                    Component.empty(), ignored -> {
                        MineZeroCompatibilityClientState.toggle();
                        refreshMineZeroButton();
                    }).bounds(x + 24, y + INTEGRATION_ROW_START,
                            panelWidth - 48, 36).build());

            int voiceRowWidth = panelWidth - 48;
            int mimicryWidth = Math.min(154,
                    Math.max(124, voiceRowWidth / 3));
            int voiceWidth = voiceRowWidth - mimicryWidth
                    - MIMICRY_BUTTON_GAP;
            int voiceY = y + INTEGRATION_ROW_START + INTEGRATION_ROW_STRIDE;
            voiceChatButton = addRenderableWidget(Button.builder(
                    Component.empty(), ignored -> {
                        SimpleVoiceChatCompatibilityClientState.toggle();
                        refreshVoiceChatButton();
                    }).bounds(x + 24, voiceY, voiceWidth, 36).build());

            mimicryButton = addRenderableWidget(Button.builder(
                    Component.empty(), ignored -> {
                        boolean next = !Scp939VoiceClientPreferences
                                .allowMimicry();
                        Scp939ClientEvents.setMimicConsent(next);
                        refreshMimicryButton();
                    }).bounds(x + 24 + voiceWidth + MIMICRY_BUTTON_GAP,
                            voiceY, mimicryWidth, 36).build());

            backButton = addRenderableWidget(Button.builder(
                    ScpFonts.roboto("Back"),
                    ignored -> Minecraft.getInstance().setScreen(parent))
                    .bounds(x + panelWidth - 126, y + panelHeight - 44,
                            104, 28).build());

            refreshMineZeroButton();
            refreshVoiceChatButton();
            refreshMimicryButton();
            MineZeroCompatibilityClientState.query();
            SimpleVoiceChatCompatibilityClientState.query();
        }

        private int panelWidth() {
            return Math.min(760, Math.max(440, width - 48));
        }

        private int panelHeight() {
            return Math.min(440, Math.max(280, height - 70));
        }

        private void refreshMineZeroButton() {
            if (mineZeroButton == null) return;
            if (!MineZeroCompatibilityClientState.known()) {
                mineZeroButton.setMessage(Component.literal(
                        "MineZero — Detecting..."));
                mineZeroButton.active = false;
                return;
            }
            if (!MineZeroCompatibilityClientState.installed()) {
                mineZeroButton.setMessage(Component.literal(
                        "MineZero — NOT INSTALLED"));
                mineZeroButton.active = false;
                return;
            }
            mineZeroButton.setMessage(Component.literal("MineZero: "
                    + (MineZeroCompatibilityClientState.enabled()
                    ? "ON" : "OFF")));
            mineZeroButton.active = MineZeroCompatibilityClientState.canEdit();
        }

        private void refreshVoiceChatButton() {
            if (voiceChatButton == null) return;
            if (!SimpleVoiceChatCompatibilityClientState.known()) {
                voiceChatButton.setMessage(Component.literal(
                        "Simple Voice Chat — Detecting..."));
                voiceChatButton.active = false;
                return;
            }
            if (!SimpleVoiceChatCompatibilityClientState.installed()) {
                voiceChatButton.setMessage(Component.literal(
                        "Simple Voice Chat — NOT INSTALLED"));
                voiceChatButton.active = false;
                return;
            }
            voiceChatButton.setMessage(Component.literal(
                    "Simple Voice Chat: "
                            + (SimpleVoiceChatCompatibilityClientState.enabled()
                            ? "ON" : "OFF")));
            voiceChatButton.active =
                    SimpleVoiceChatCompatibilityClientState.canEdit();
        }

        private void refreshMimicryButton() {
            if (mimicryButton == null) return;
            boolean installed = SimpleVoiceChatPresence.installed();
            boolean configured = Scp939VoiceClientPreferences.setupComplete();
            boolean enabled = configured
                    && Scp939VoiceClientPreferences.allowMimicry();
            mimicryButton.setMessage(Component.literal(installed
                    ? "939 Mimicry: " + (enabled ? "ON" : "OFF")
                    : "939 Mimicry — N/A"));
            mimicryButton.active = installed;
        }

        @Override
        public void renderBackground(GuiGraphics graphics) {
            ConfigCenterVisuals.renderBackdrop(this, graphics, 0, 0);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            refreshMineZeroButton();
            refreshVoiceChatButton();
            refreshMimicryButton();
            renderBackground(graphics);
            int panelWidth = panelWidth();
            int x = ConfigCenterVisuals.contentLeft(width, panelWidth);
            int panelHeight = panelHeight();
            int y = Math.max(18, (height - panelHeight) / 2);

            ConfigCenterVisuals.drawPanel(graphics, font, x, y, panelWidth,
                    panelHeight, LABEL);

            int textX = x + 24 + ConfigCenterVisuals.contentOffsetX();
            drawDescription(graphics, textX,
                    y + INTEGRATION_ROW_START
                            + INTEGRATION_DESCRIPTION_OFFSET,
                    panelWidth - 48,
                    "Uses SCP Additions saves as MineZero checkpoints and replaces automatic Return by Death with the death/spectate flow. Team wipes require a vote before rollback.");
            drawDescription(graphics, textX,
                    y + INTEGRATION_ROW_START + INTEGRATION_ROW_STRIDE
                            + INTEGRATION_DESCRIPTION_OFFSET,
                    panelWidth - 48,
                    "Provides SCP Additions voice features. SCP-939 Mimicry separately controls whether short consenting voice fragments may be reused from memory by SCP-939; fragments expire and are never written to disk.");

            drawIntegrationButton(graphics, mineZeroButton, MINEZERO_ID,
                    MineZeroCompatibilityClientState.installed(),
                    mouseX, mouseY);
            drawIntegrationButton(graphics, voiceChatButton,
                    SimpleVoiceChatPresence.MOD_ID,
                    SimpleVoiceChatCompatibilityClientState.installed(),
                    mouseX, mouseY);
            drawMimicryButton(graphics, mouseX, mouseY);

            if (backButton != null) {
                ConfigCenterVisuals.drawButton(graphics, font, backButton,
                        ScpFonts.roboto("Back"), mouseX, mouseY);
            }
        }

        private void drawDescription(GuiGraphics graphics, int x, int y,
                int width, String description) {
            int lineY = y;
            for (var line : font.split(ScpFonts.roboto(description), width)) {
                graphics.drawString(font, line, x, lineY,
                        ConfigCenterVisuals.fadeColor(
                                ConfigCenterVisuals.MUTED), false);
                lineY += font.lineHeight + 3;
            }
        }

        private void drawMimicryButton(GuiGraphics graphics,
                int mouseX, int mouseY) {
            if (mimicryButton == null) return;
            ConfigCenterVisuals.drawButton(graphics, font, mimicryButton,
                    Component.empty(), mouseX, mouseY);

            String plain = mimicryButton.getMessage().getString();
            int left = mimicryButton.getX()
                    + ConfigCenterVisuals.contentOffsetX() + 10;
            int right = mimicryButton.getX()
                    + ConfigCenterVisuals.contentOffsetX()
                    + mimicryButton.getWidth() - 10;
            int textY = mimicryButton.getY()
                    + Math.max(1,
                    (mimicryButton.getHeight() - font.lineHeight) / 2);
            boolean hovered = mimicryButton.active
                    && (mimicryButton.isMouseOver(mouseX, mouseY)
                    || mimicryButton.isFocused());
            int textColor = !mimicryButton.active
                    ? ConfigCenterVisuals.MUTED
                    : hovered ? ConfigCenterVisuals.ACCENT_BRIGHT
                    : ConfigCenterVisuals.TEXT;

            int stateLength = plain.endsWith(": ON") ? 2
                    : plain.endsWith(": OFF") ? 3 : 0;
            if (stateLength <= 0) {
                graphics.drawString(font,
                        ScpFonts.roboto(fitIntegrationText(plain,
                                Math.max(12, right - left))),
                        left, textY,
                        ConfigCenterVisuals.fadeColor(textColor), false);
                return;
            }

            String prefix = plain.substring(0,
                    plain.length() - stateLength).trim();
            if (prefix.endsWith(":")) {
                prefix = prefix.substring(0, prefix.length() - 1).trim();
            }
            String state = plain.substring(plain.length() - stateLength);
            int stateWidth = font.width(ScpFonts.roboto(state));
            int prefixWidth = Math.max(18,
                    right - stateWidth - 12 - left);
            graphics.drawString(font,
                    ScpFonts.roboto(fitIntegrationText(prefix, prefixWidth)),
                    left, textY,
                    ConfigCenterVisuals.fadeColor(textColor), false);
            graphics.drawString(font, ScpFonts.roboto(state),
                    right - stateWidth, textY,
                    ConfigCenterVisuals.fadeColor("ON".equals(state)
                            ? ConfigCenterVisuals.GREEN
                            : ConfigCenterVisuals.RED), false);
        }

        private void drawIntegrationButton(GuiGraphics graphics,
                Button button, String modId, boolean installed,
                int mouseX, int mouseY) {
            if (button == null) return;

            ConfigCenterVisuals.drawButton(graphics, font, button,
                    Component.empty(), mouseX, mouseY);

            int left = button.getX() + ConfigCenterVisuals.contentOffsetX();
            int right = left + button.getWidth();
            int textX = left + INTEGRATION_ICON_LEFT + INTEGRATION_ICON_SIZE
                    + INTEGRATION_ICON_GAP;
            int textY = button.getY() + Math.max(1,
                    (button.getHeight() - font.lineHeight) / 2);
            boolean hovered = button.active
                    && (button.isMouseOver(mouseX, mouseY)
                    || button.isFocused());
            int textColor = !button.active ? ConfigCenterVisuals.MUTED
                    : hovered ? ConfigCenterVisuals.ACCENT_BRIGHT
                    : ConfigCenterVisuals.TEXT;

            String plain = button.getMessage().getString();
            int stateLength = plain.endsWith(": ON") ? 2
                    : plain.endsWith(": OFF") ? 3 : 0;
            if (stateLength > 0 && button.getWidth() >= 155) {
                String prefix = plain.substring(0,
                        plain.length() - stateLength).trim();
                if (prefix.endsWith(":")) {
                    prefix = prefix.substring(0,
                            prefix.length() - 1).trim();
                }
                String state = plain.substring(plain.length() - stateLength);
                int stateWidth = font.width(ScpFonts.roboto(state));
                int maxPrefix = Math.max(20,
                        right - 28 - stateWidth - textX);
                graphics.drawString(font,
                        ScpFonts.roboto(fitIntegrationText(prefix, maxPrefix)),
                        textX, textY,
                        ConfigCenterVisuals.fadeColor(textColor), false);
                graphics.drawString(font, ScpFonts.roboto(state),
                        right - 14 - stateWidth, textY,
                        ConfigCenterVisuals.fadeColor(!button.active
                                ? ConfigCenterVisuals.MUTED
                                : "ON".equals(state)
                                ? ConfigCenterVisuals.GREEN
                                : ConfigCenterVisuals.RED), false);
            } else {
                int maxWidth = Math.max(12, right - 12 - textX);
                graphics.drawString(font,
                        ScpFonts.roboto(fitIntegrationText(plain, maxWidth)),
                        textX, textY,
                        ConfigCenterVisuals.fadeColor(textColor), false);
            }

            if (!installed) return;
            ResourceLocation logo = ModIntegrationLogoClient.logo(modId);
            if (logo == null) return;
            int iconX = left + INTEGRATION_ICON_LEFT;
            int iconY = button.getY()
                    + (button.getHeight() - INTEGRATION_ICON_SIZE) / 2;
            graphics.blit(logo, iconX, iconY,
                    INTEGRATION_ICON_SIZE, INTEGRATION_ICON_SIZE,
                    0.0F, 0.0F,
                    INTEGRATION_ICON_SIZE, INTEGRATION_ICON_SIZE,
                    INTEGRATION_ICON_SIZE, INTEGRATION_ICON_SIZE);
        }

        private String fitIntegrationText(String value, int maxWidth) {
            if (font.width(ScpFonts.roboto(value)) <= maxWidth) return value;
            String suffix = "...";
            int suffixWidth = font.width(ScpFonts.roboto(suffix));
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                String candidate = out.toString() + value.charAt(i);
                if (font.width(ScpFonts.roboto(candidate)) + suffixWidth
                        > maxWidth) {
                    break;
                }
                out.append(value.charAt(i));
            }
            return out + suffix;
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreen(parent);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
