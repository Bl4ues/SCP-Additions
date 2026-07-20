package com.bl4ues.scpinventory.client;

import net.neoforged.fml.common.EventBusSubscriber;

import com.bl4ues.scpinventory.client.gui.ScpInventoryScreen;
import com.bl4ues.scpinventory.client.gui.components.CraftingPanel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import java.lang.reflect.Field;

/** Routes Forge screen input to the Crafting panel without affecting other tabs. */
@EventBusSubscriber(modid = "scp_additions",
        bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CraftingInputHandler {
    private CraftingInputHandler() {
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        CraftingPanel panel = getActivePanel(event.getScreen());
        if (panel != null && panel.mouseClicked(event.getMouseX(),
                event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        CraftingPanel panel = getActivePanel(event.getScreen());
        if (panel != null && panel.mouseReleased(event.getMouseX(),
                event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        CraftingPanel panel = getActivePanel(event.getScreen());
        if (panel != null && panel.mouseDragged(event.getMouseX(),
                event.getMouseY(), event.getMouseButton(), event.getDragX(),
                event.getDragY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        CraftingPanel panel = getActivePanel(event.getScreen());
        if (panel != null && panel.mouseScrolled(event.getMouseX(),
                event.getMouseY(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    private static CraftingPanel getActivePanel(Object screen) {
        if (!(screen instanceof ScpInventoryScreen)) return null;
        Object mode = readField(screen, "mode");
        if (!"CRAFTING".equals(String.valueOf(mode))) return null;
        Object panel = readField(screen, "craftingPanel");
        return panel instanceof CraftingPanel craftingPanel
                ? craftingPanel : null;
    }

    private static Object readField(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
