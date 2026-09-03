package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.inventory.client.gui.ScpInventoryScreen;
import com.bl4ues.scpclassifieddirective.inventory.network.CraftingActionPacket;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

/** Inventory-screen input conveniences that need to run before panel handlers. */
@Mod.EventBusSubscriber(modid = "scp_classified_directive", value = Dist.CLIENT)
public final class CraftingWorldDropClientEvents {
    private CraftingWorldDropClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof ScpInventoryScreen screen)
                || !Minecraft.getInstance().options.keyInventory.matches(
                        event.getKeyCode(), event.getScanCode())) {
            return;
        }
        screen.onClose();
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof ScpInventoryScreen screen)
                || !Minecraft.getInstance().options.keyInventory
                        .matchesMouse(event.getButton())) {
            return;
        }
        screen.onClose();
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() != 0
                || !(event.getScreen() instanceof ScpInventoryScreen screen)) {
            return;
        }
        try {
            Object mode = field(screen, "mode");
            if (mode == null || !"CRAFTING".equals(mode.toString())) return;
            if (insideRoot(screen, event.getMouseX(), event.getMouseY())) return;

            Object panel = field(screen, "craftingPanel");
            if (panel == null) return;
            Object dragKind = field(panel, "dragKind");
            Object dragIndex = field(panel, "dragIndex");
            if (!(dragIndex instanceof Integer source) || source < 0
                    || dragKind == null) {
                return;
            }

            String kind = dragKind.toString();
            if ("MAIN".equals(kind)) {
                ClientInventoryBridge.moveMainToWorld(source);
            } else if ("GRID".equals(kind)) {
                ModNetwork.CHANNEL.sendToServer(new CraftingActionPacket(
                        CraftingActionPacket.MOVE_GRID_TO_WORLD,
                        source, -1, null));
            }
        } catch (ReflectiveOperationException ignored) {
            // The panel itself will still clear its drag state normally.
        }
    }

    private static boolean insideRoot(Screen screen, double mouseX,
            double mouseY) throws ReflectiveOperationException {
        int x = integerField(screen, "rootX");
        int y = integerField(screen, "rootY");
        int width = integerField(screen, "rootWidth");
        int height = integerField(screen, "rootHeight");
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    private static int integerField(Object target, String name)
            throws ReflectiveOperationException {
        Object value = field(target, name);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static Object field(Object target, String name)
            throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
