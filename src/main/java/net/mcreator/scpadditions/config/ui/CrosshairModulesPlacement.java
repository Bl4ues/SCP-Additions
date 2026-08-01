package net.mcreator.scpadditions.config.ui;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import com.bl4ues.scpinventory.client.ScpFonts;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/** Places Crosshair with the gameplay modules instead of the Configuration Center home screen. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CrosshairModulesPlacement {
    private static final String HOME_SCREEN =
            "net.mcreator.scpadditions.config.ui.ConfigCenterClient$HomeScreen";
    private static final String EXTENDED_MODULES_SCREEN =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";
    private static final String CROSSHAIR_SCREEN =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$CrosshairScreen";

    private static final int NAVY = 0xFF081022;
    private static final int NAVY_HOVER = 0xFF131E36;
    private static final int BORDER = 0xFF46536C;
    private static final int BORDER_HOVER = 0xFF73809A;
    private static final int ACCENT_SOFT = 0xFF8D711F;
    private static final int WHITE = 0xFFF7F8FC;

    private CrosshairModulesPlacement() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen == null) return;

        String screenClass = screen.getClass().getName();
        if (HOME_SCREEN.equals(screenClass)) {
            restoreCompactHomeLayout(event);
        } else if (EXTENDED_MODULES_SCREEN.equals(screenClass)
                && "General & Modules".equals(screen.getTitle().getString())) {
            addCrosshairGameplayEntry(event, screen);
        }
    }

    private static void restoreCompactHomeLayout(ScreenEvent.Init.Post event) {
        Map<String, Button> buttons = new LinkedHashMap<>();
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof Button button) {
                buttons.put(button.getMessage().getString(), button);
            }
        }

        Button crosshair = buttons.get("Crosshair");
        Button general = buttons.get("General & Modules");
        Button inventory = buttons.get("Inventory, Equipment & Codex");
        Button interactions = buttons.get("Contextual Interactions");
        Button drinks = buttons.get("SCP-294 Drinks");
        Button recipes = buttons.get("SCP-914 Recipes");
        Button accessibility = buttons.get("Accessibility");
        Button debug = buttons.get("Debug Tools");
        Button reload = buttons.get("Reload Snapshot");
        Button done = buttons.get("Done");
        if (crosshair == null || general == null || inventory == null
                || interactions == null || drinks == null || recipes == null
                || accessibility == null || debug == null || reload == null
                || done == null) {
            return;
        }

        int startY = crosshair.getY();
        int step = 27;
        crosshair.visible = false;
        crosshair.active = false;

        general.setY(startY);
        inventory.setY(startY + step);
        interactions.setY(startY + step * 2);
        drinks.setY(startY + step * 3);
        recipes.setY(startY + step * 4);
        accessibility.setY(startY + step * 5);
        debug.setY(startY + step * 5);

        int bottomY = startY + step * 6 + 3;
        reload.setY(bottomY);
        done.setY(bottomY);
    }

    private static void addCrosshairGameplayEntry(ScreenEvent.Init.Post event,
            Screen screen) {
        int panelWidth = Math.min(560, screen.width - 20);
        int panelHeight = Math.min(380, screen.height - 16);
        int panelX = Math.max(8, (screen.width - panelWidth) / 2);
        int panelY = Math.max(8, (screen.height - panelHeight) / 2);

        // The Gameplay Features heading occupies the first row. The entry sits
        // on that row where the decorative divider normally continues.
        int buttonWidth = 138;
        int buttonX = panelX + panelWidth - buttonWidth - 18;
        int buttonY = panelY + 48;
        event.addListener(new NavigationButton(buttonX, buttonY, buttonWidth, 20,
                ScpFonts.roboto("Crosshair"), () -> openCrosshair(screen)));
    }

    private static void openCrosshair(Screen parent) {
        try {
            Field workingField = parent.getClass().getDeclaredField("working");
            workingField.setAccessible(true);
            Object value = workingField.get(parent);
            if (!(value instanceof JsonObject working)) return;

            Class<?> screenType = Class.forName(CROSSHAIR_SCREEN);
            Constructor<?> constructor = screenType.getDeclaredConstructor(
                    Screen.class, JsonObject.class);
            constructor.setAccessible(true);
            Object created = constructor.newInstance(parent, working);
            if (created instanceof Screen screen) {
                Minecraft.getInstance().setScreen(screen);
            }
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not open Crosshair from General & Modules",
                    exception);
        }
    }

    private static final class NavigationButton extends AbstractButton {
        private final Runnable action;

        private NavigationButton(int x, int y, int width, int height,
                Component message, Runnable action) {
            super(x, y, width, height, message);
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            boolean hovered = isHoveredOrFocused();
            int left = getX();
            int top = getY();
            int right = left + getWidth();
            int bottom = top + getHeight();
            int border = hovered ? BORDER_HOVER : BORDER;

            graphics.fill(left, top, right, bottom,
                    hovered ? NAVY_HOVER : NAVY);
            graphics.fill(left, top, right, top + 1, border);
            graphics.fill(left, bottom - 1, right, bottom, border);
            graphics.fill(left, top, left + 1, bottom, border);
            graphics.fill(right - 1, top, right, bottom, border);
            graphics.fill(left + 1, top + 1,
                    left + (hovered ? 4 : 2), bottom - 1,
                    hovered ? ACCENT_SOFT : BORDER);

            int textX = left + Math.max(5,
                    (getWidth() - Minecraft.getInstance().font.width(getMessage())) / 2);
            int textY = top + Math.max(1, (getHeight() - 8) / 2);
            graphics.drawString(Minecraft.getInstance().font, getMessage(),
                    textX, textY, WHITE, false);
        }
    }
}
