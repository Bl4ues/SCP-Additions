package net.mcreator.scpadditions.config.ui;

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

/** Adds the client-only presentation preferences to General & Modules. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CustomLoadingScreenModulesUi {
    private static final String EXTENDED_SCREEN =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";
    private static final String GENERAL_TITLE = "General & Modules";
    private static final String MAIN_MENU_LABEL = "Custom Main Menu";
    private static final String LOADING_LABEL = "Custom Loading Screen";

    private CustomLoadingScreenModulesUi() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInitPre(ScreenEvent.Init.Pre event) {
        Screen screen = event.getScreen();
        if (screen == null || !EXTENDED_SCREEN.equals(
                screen.getClass().getName())
                || !GENERAL_TITLE.equals(screen.getTitle().getString())) {
            return;
        }

        try {
            Field rowsField = screen.getClass().getDeclaredField("rows");
            rowsField.setAccessible(true);
            Object value = rowsField.get(screen);
            if (!(value instanceof List<?> rows) || rows.isEmpty()) return;

            boolean needsMainMenu = !containsLabel(rows, MAIN_MENU_LABEL);
            boolean needsLoading = !containsLabel(rows, LOADING_LABEL);
            if (!needsMainMenu && !needsLoading) return;

            Class<?> rowType = rows.get(0).getClass();
            Constructor<?> constructor = rowType.getDeclaredConstructor(
                    String.class, String.class, String.class, String.class,
                    boolean.class);
            constructor.setAccessible(true);

            List<Object> expanded = new ArrayList<>(rows.size() + 2);
            expanded.addAll(rows);
            int insertion = indexAfterLabel(rows, "Remember UI State");

            if (needsMainMenu) {
                expanded.add(insertion++, constructor.newInstance(
                        "ui", "custom_main_menu", MAIN_MENU_LABEL,
                        "Replaces Minecraft's title screen with the SCP Additions menu presentation.",
                        true));
            }
            if (needsLoading) {
                expanded.add(insertion, constructor.newInstance(
                        "ui", "custom_loading_screen", LOADING_LABEL,
                        "Replaces Minecraft's spawn-region loading display with the SCP Additions presentation.",
                        true));
            }
            rowsField.set(screen, List.copyOf(expanded));
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not add the client presentation preference rows",
                    exception);
        }
    }

    private static boolean containsLabel(List<?> rows, String expected)
            throws ReflectiveOperationException {
        for (Object row : rows) {
            if (expected.equals(label(row))) return true;
        }
        return false;
    }

    private static int indexAfterLabel(List<?> rows, String expected)
            throws ReflectiveOperationException {
        for (int index = 0; index < rows.size(); index++) {
            if (expected.equals(label(rows.get(index)))) return index + 1;
        }
        return rows.size();
    }

    private static String label(Object row)
            throws ReflectiveOperationException {
        Method label = row.getClass().getDeclaredMethod("label");
        label.setAccessible(true);
        return String.valueOf(label.invoke(row));
    }
}
