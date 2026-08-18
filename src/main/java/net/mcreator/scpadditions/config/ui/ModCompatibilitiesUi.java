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

import java.util.Map;
import java.util.WeakHashMap;

/** Empty compatibility hub prepared for opt-in integrations added after 4.0. */
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

        private ModCompatibilitiesScreen(Screen parent) {
            super(Component.literal(LABEL));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int panelWidth = Math.min(720, Math.max(360, width - 48));
            int x = ConfigCenterVisuals.contentLeft(width, panelWidth);
            int panelHeight = Math.min(330, Math.max(220, height - 90));
            int y = Math.max(28, (height - panelHeight) / 2);
            addRenderableWidget(Button.builder(ScpFonts.roboto("Back"),
                    ignored -> Minecraft.getInstance().setScreen(parent))
                    .bounds(x + panelWidth - 126, y + panelHeight - 46,
                            104, 28).build());
        }

        @Override
        public void renderBackground(GuiGraphics graphics) {
            ConfigCenterVisuals.renderBackdrop(this, graphics, 0, 0);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            renderBackground(graphics);
            int panelWidth = Math.min(720, Math.max(360, width - 48));
            int x = ConfigCenterVisuals.contentLeft(width, panelWidth);
            int panelHeight = Math.min(330, Math.max(220, height - 90));
            int y = Math.max(28, (height - panelHeight) / 2);

            ConfigCenterVisuals.drawPanel(graphics, font, x, y, panelWidth,
                    panelHeight, LABEL);
            int textX = x + 24 + ConfigCenterVisuals.contentOffsetX();
            int textY = y + 58;
            graphics.drawString(font,
                    ScpFonts.titillium("DETECTED INTEGRATIONS"),
                    textX, textY, ConfigCenterVisuals.fadeColor(
                            ConfigCenterVisuals.ACCENT_BRIGHT), false);
            textY += 30;
            for (var line : font.split(ScpFonts.roboto(
                    "Compatibility modules will appear here when supported mods are detected. "
                            + "Unavailable integrations will remain visible but disabled, so their state is explicit."),
                    panelWidth - 48)) {
                graphics.drawString(font, line, textX, textY,
                        ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.MUTED),
                        false);
                textY += font.lineHeight + 4;
            }
            textY += 16;
            graphics.drawString(font,
                    ScpFonts.roboto("No compatibility modules are registered yet."),
                    textX, textY,
                    ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.MUTED),
                    false);

            super.render(graphics, mouseX, mouseY, partialTick);
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
