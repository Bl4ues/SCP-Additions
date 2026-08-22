package com.bl4ues.scpclassifieddirective.config.ui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Adds the host-authoritative Persistent Death Bodies row to General & Modules. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class DeathBodiesModulesUi {
    private static final String EXTENDED_SCREEN =
            "com.bl4ues.scpclassifieddirective.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";
    private static final String GENERAL_TITLE = "General & Modules";
    private static final String LABEL = "Persistent Death Bodies";

    private DeathBodiesModulesUi() {
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onInitPre(ScreenEvent.Init.Pre event) {
        Screen screen = event.getScreen();
        if (screen == null || !EXTENDED_SCREEN.equals(screen.getClass().getName())
                || !GENERAL_TITLE.equals(screen.getTitle().getString())) {
            return;
        }

        try {
            Field rowsField = screen.getClass().getDeclaredField("rows");
            rowsField.setAccessible(true);
            Object value = rowsField.get(screen);
            if (!(value instanceof List<?> rows) || rows.isEmpty()
                    || containsLabel(rows, LABEL)) return;

            Class<?> rowType = rows.get(0).getClass();
            Constructor<?> constructor = rowType.getDeclaredConstructor(
                    String.class, String.class, String.class, String.class,
                    boolean.class);
            constructor.setAccessible(true);

            List<Object> expanded = new ArrayList<>(rows.size() + 1);
            expanded.addAll(rows);
            int insertion = indexBeforeLabel(rows, "Preferences");
            expanded.add(insertion, constructor.newInstance(
                    "death_bodies", "enabled", LABEL,
                    "Leaves an inert, interactable player body at the death location for world and SCP interactions.",
                    true));
            rowsField.set(screen, List.copyOf(expanded));
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not add the Persistent Death Bodies module row",
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

    private static int indexBeforeLabel(List<?> rows, String expected)
            throws ReflectiveOperationException {
        for (int index = 0; index < rows.size(); index++) {
            if (expected.equals(label(rows.get(index)))) return index;
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
