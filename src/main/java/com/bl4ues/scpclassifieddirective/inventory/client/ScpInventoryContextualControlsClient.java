package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.inventory.client.gui.ContextAnchorEditorScreen;
import com.bl4ues.scpclassifieddirective.inventory.client.gui.ScpInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Bridges SCP Inventory to vanilla controls without adding duplicate keybinds. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class ScpInventoryContextualControlsClient {
    private static final int BUTTON_WIDTH = 118;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BACKGROUND = 0xF0081022;
    private static final int BACKGROUND_HOVER = 0xF0131E36;
    private static final int BORDER = 0xFF46536C;
    private static final int BORDER_HOVER = 0xFFC59A2A;
    private static final int ACCENT = 0xFFC59A2A;
    private static final int TEXT = 0xFFF7F8FC;

    private ScpInventoryContextualControlsClient() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (event.getScreen() instanceof ScpInventoryScreen
                && minecraft.player != null
                && minecraft.player.isCreative()) {
            event.addListener(new CreativeInventoryButton(
                    creativeButtonX(event.getScreen()), 8));
        }

        if (event.getScreen() instanceof ContextAnchorEditorScreen) {
            hideObsoleteInputSelector(event.getScreen());
        }
    }

    /**
     * ScpInventoryScreen consumes its own inventory-area mouse routing before
     * Screen can dispatch to externally registered listeners. Catch this one
     * control before that routing so the visible Creative button is functional.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0
                || !(event.getScreen() instanceof ScpInventoryScreen screen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isCreative()) return;

        int x = creativeButtonX(screen);
        int y = 8;
        if (event.getMouseX() < x || event.getMouseX() > x + BUTTON_WIDTH
                || event.getMouseY() < y
                || event.getMouseY() > y + BUTTON_HEIGHT) {
            return;
        }

        openCreativeInventory();
        event.setCanceled(true);
    }

    /** The editor rebuilds its own widgets without another Init event. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof ContextAnchorEditorScreen) {
            hideObsoleteInputSelector(event.getScreen());
        }
    }

    private static int creativeButtonX(Screen screen) {
        return Math.max(8, screen.width - BUTTON_WIDTH - 8);
    }

    private static void hideObsoleteInputSelector(Screen screen) {
        for (GuiEventListener listener : screen.children()) {
            if (!(listener instanceof AbstractButton button)) continue;
            String label = button.getMessage().getString();
            if (label.startsWith("Input:")) {
                button.active = false;
                button.visible = false;
            }
        }
    }

    private static void openCreativeInventory() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null
                || !minecraft.player.isCreative()) {
            return;
        }
        minecraft.setScreen(new CreativeModeInventoryScreen(
                minecraft.player,
                minecraft.player.connection.enabledFeatures(),
                minecraft.options.operatorItemsTab().get()));
    }

    private static final class CreativeInventoryButton extends AbstractButton {
        private CreativeInventoryButton(int x, int y) {
            super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                    ScpFonts.roboto("Creative Inventory"));
        }

        @Override
        public void onPress() {
            openCreativeInventory();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = isHoveredOrFocused()
                    ? BACKGROUND_HOVER : BACKGROUND;
            int border = isHoveredOrFocused() ? BORDER_HOVER : BORDER;
            graphics.fill(getX(), getY(), getX() + getWidth(),
                    getY() + getHeight(), background);
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + 1,
                    border);
            graphics.fill(getX(), getY() + getHeight() - 1,
                    getX() + getWidth(), getY() + getHeight(), border);
            graphics.fill(getX(), getY(), getX() + 1,
                    getY() + getHeight(), border);
            graphics.fill(getX() + getWidth() - 1, getY(),
                    getX() + getWidth(), getY() + getHeight(), border);
            graphics.fill(getX() + 1, getY() + 1, getX() + 4,
                    getY() + getHeight() - 1, ACCENT);
            graphics.drawCenteredString(Minecraft.getInstance().font,
                    ScpFonts.roboto(getMessage()), getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2, TEXT);
        }
    }
}
