package net.mcreator.scpadditions.config.ui;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.MineZeroCompatibilityClientState;

import java.util.Map;
import java.util.WeakHashMap;

/** Entry point and detected-mod controls for optional integrations. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class ModCompatibilitiesUi {
    private static final String HOME_SCREEN =
            "net.mcreator.scpadditions.config.ui.ConfigCenterClient$HomeScreen";
    private static final String LABEL = "Mod Compatibilities";
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
        private Button backButton;

        private ModCompatibilitiesScreen(Screen parent) {
            super(Component.literal(LABEL));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int panelWidth = Math.min(720, Math.max(420, width - 48));
            int x = ConfigCenterVisuals.contentLeft(width, panelWidth);
            int panelHeight = Math.min(360, Math.max(250, height - 90));
            int y = Math.max(28, (height - panelHeight) / 2);

            mineZeroButton = addRenderableWidget(Button.builder(
                    Component.empty(), ignored -> {
                        MineZeroCompatibilityClientState.toggle();
                        refreshMineZeroButton();
                    }).bounds(x + 24, y + 104, panelWidth - 48, 36).build());
            backButton = addRenderableWidget(Button.builder(
                    ScpFonts.roboto("Back"),
                    ignored -> Minecraft.getInstance().setScreen(parent))
                    .bounds(x + panelWidth - 126, y + panelHeight - 46,
                            104, 28).build());
            refreshMineZeroButton();
            MineZeroCompatibilityClientState.query();
        }

        private void refreshMineZeroButton() {
            if (mineZeroButton == null) return;
            if (!MineZeroCompatibilityClientState.known()) {
                mineZeroButton.setMessage(Component.literal("MineZero — Detecting..."));
                mineZeroButton.active = false;
                return;
            }
            if (!MineZeroCompatibilityClientState.installed()) {
                mineZeroButton.setMessage(Component.literal("MineZero — NOT INSTALLED"));
                mineZeroButton.active = false;
                return;
            }
            mineZeroButton.setMessage(Component.literal("MineZero Compatibility: "
                    + (MineZeroCompatibilityClientState.enabled() ? "ON" : "OFF")));
            mineZeroButton.active = MineZeroCompatibilityClientState.canEdit();
        }

        @Override
        public void renderBackground(GuiGraphics graphics) {
            ConfigCenterVisuals.renderBackdrop(this, graphics, 0, 0);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            refreshMineZeroButton();
            renderBackground(graphics);
            int panelWidth = Math.min(720, Math.max(420, width - 48));
            int x = ConfigCenterVisuals.contentLeft(width, panelWidth);
            int panelHeight = Math.min(360, Math.max(250, height - 90));
            int y = Math.max(28, (height - panelHeight) / 2);

            ConfigCenterVisuals.drawPanel(graphics, font, x, y, panelWidth,
                    panelHeight, LABEL);
            int textX = x + 24 + ConfigCenterVisuals.contentOffsetX();
            int textY = y + 58;
            graphics.drawString(font, ScpFonts.titillium("DETECTED INTEGRATIONS"),
                    textX, textY, ConfigCenterVisuals.fadeColor(
                            ConfigCenterVisuals.ACCENT_BRIGHT), false);

            textY += 29;
            String status;
            if (!MineZeroCompatibilityClientState.known()) {
                status = "Reading the server mod list...";
            } else if (!MineZeroCompatibilityClientState.installed()) {
                status = "MineZero was not detected on the server. The integration remains visible but disabled.";
            } else {
                status = "Replaces MineZero's immediate Return by Death with SCP Additions save points, cooperative death spectating, rollback voting, and SCP state restoration.";
            }
            for (var line : font.split(ScpFonts.roboto(status), panelWidth - 48)) {
                graphics.drawString(font, line, textX, textY,
                        ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.MUTED), false);
                textY += font.lineHeight + 3;
            }

            textY = y + 158;
            String ownership = MineZeroCompatibilityClientState.installed()
                    && !MineZeroCompatibilityClientState.canEdit()
                    ? "SERVER setting · Operator permission is required to change it."
                    : "SERVER setting · Enabled by default when MineZero is installed.";
            graphics.drawString(font, ScpFonts.roboto(ownership), textX, textY,
                    ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.MUTED), false);

            if (mineZeroButton != null) {
                ConfigCenterVisuals.drawButton(graphics, font, mineZeroButton,
                        ScpFonts.roboto(mineZeroButton.getMessage()), mouseX, mouseY);
            }
            if (backButton != null) {
                ConfigCenterVisuals.drawButton(graphics, font, backButton,
                        ScpFonts.roboto("Back"), mouseX, mouseY);
            }
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
