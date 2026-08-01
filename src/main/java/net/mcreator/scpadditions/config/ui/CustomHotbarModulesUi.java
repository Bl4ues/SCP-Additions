package net.mcreator.scpadditions.config.ui;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Shows the custom hotbar option only while SCP Inventory is enabled. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CustomHotbarModulesUi {
    private static final String EXTENDED_MODULES_SCREEN =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";
    private static final String ROW_TYPE =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$Row";
    private static final String INVENTORY_LABEL = "SCP Inventory";
    private static final String HOTBAR_LABEL = "Custom Hotbar";

    private CustomHotbarModulesUi() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onScreenInitPre(ScreenEvent.Init.Pre event) {
        if (isGeneralModulesScreen(event.getScreen())) {
            synchronizeRow(event.getScreen(), false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (isGeneralModulesScreen(event.getScreen())) {
            synchronizeRow(event.getScreen(), true);
        }
    }

    private static boolean isGeneralModulesScreen(Screen screen) {
        return screen != null
                && EXTENDED_MODULES_SCREEN.equals(screen.getClass().getName())
                && "General & Modules".equals(screen.getTitle().getString());
    }

    private static void synchronizeRow(Screen screen, boolean rebuild) {
        try {
            Field rowsField = screen.getClass().getDeclaredField("rows");
            Field workingField = screen.getClass().getDeclaredField("working");
            rowsField.setAccessible(true);
            workingField.setAccessible(true);

            Object rowsValue = rowsField.get(screen);
            Object workingValue = workingField.get(screen);
            if (!(rowsValue instanceof List<?> currentRows)
                    || !(workingValue instanceof JsonObject working)) {
                return;
            }

            Class<?> rowType = Class.forName(ROW_TYPE);
            Method labelMethod = rowType.getDeclaredMethod("label");
            labelMethod.setAccessible(true);

            int inventoryIndex = -1;
            int hotbarIndex = -1;
            for (int i = 0; i < currentRows.size(); i++) {
                String label = String.valueOf(
                        labelMethod.invoke(currentRows.get(i)));
                if (INVENTORY_LABEL.equals(label)) inventoryIndex = i;
                if (HOTBAR_LABEL.equals(label)) hotbarIndex = i;
            }

            boolean inventoryEnabled = bool(object(working, "inventory"),
                    "enabled", true);
            if (inventoryEnabled && hotbarIndex < 0) {
                Constructor<?> constructor = rowType.getDeclaredConstructor(
                        String.class, String.class, String.class,
                        String.class, boolean.class);
                constructor.setAccessible(true);
                Object row = constructor.newInstance(
                        "inventory", "custom_hotbar", HOTBAR_LABEL,
                        "Replaces the vanilla hotbar with a rotating category-based item list.",
                        true);

                List<Object> updated = new ArrayList<>(currentRows);
                int insertAt = inventoryIndex >= 0
                        ? inventoryIndex + 1 : updated.size();
                updated.add(insertAt, row);
                rowsField.set(screen, List.copyOf(updated));
                if (rebuild) rebuildWidgets(screen);
            } else if (!inventoryEnabled && hotbarIndex >= 0) {
                List<Object> updated = new ArrayList<>(currentRows);
                updated.remove(hotbarIndex);
                rowsField.set(screen, List.copyOf(updated));
                if (rebuild) rebuildWidgets(screen);
            }
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not synchronize the Custom Hotbar module row",
                    exception);
        }
    }

    private static void rebuildWidgets(Screen screen)
            throws ReflectiveOperationException {
        Method method = screen.getClass().getDeclaredMethod("rebuildWidgets");
        method.setAccessible(true);
        method.invoke(screen);
    }

    private static JsonObject object(JsonObject root, String key) {
        if (!root.has(key) || !root.get(key).isJsonObject()) {
            root.add(key, new JsonObject());
        }
        return root.getAsJsonObject(key);
    }

    private static boolean bool(JsonObject root, String key,
            boolean fallback) {
        if (root == null || !root.has(key)
                || !root.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return root.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
