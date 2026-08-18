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
import java.util.ArrayList;
import java.util.List;

/** Adds the save-load transition preference under Accessibility. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class RestoreMotionAccessibilityUi {
    private static final String EXTENDED_SCREEN =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";

    private RestoreMotionAccessibilityUi() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInitPre(ScreenEvent.Init.Pre event) {
        Screen screen = event.getScreen();
        if (screen == null || !EXTENDED_SCREEN.equals(screen.getClass().getName())
                || !"Accessibility".equals(screen.getTitle().getString())) return;

        try {
            Field rowsField = screen.getClass().getDeclaredField("rows");
            rowsField.setAccessible(true);
            Object value = rowsField.get(screen);
            if (!(value instanceof List<?> rows) || rows.isEmpty()
                    || contains(rows, "Disable Load Transition")) return;

            Class<?> rowType = rows.get(0).getClass();
            Constructor<?> constructor = rowType.getDeclaredConstructor(
                    String.class, String.class, String.class, String.class,
                    boolean.class);
            constructor.setAccessible(true);

            // Accessibility used to receive its first heading through the screen's
            // separate sectionTitle field while Motion Sickness was a real Row
            // section. That made the two headings render with visibly different
            // spacing and separators. Convert both headings into the same row type
            // so they share one layout path.
            Field sectionTitleField = screen.getClass().getDeclaredField("sectionTitle");
            sectionTitleField.setAccessible(true);
            sectionTitleField.set(screen, null);

            List<Object> expanded = new ArrayList<>(rows.size() + 3);
            expanded.add(constructor.newInstance(null, null,
                    "Photosensitive Epilepsy", "", false));
            expanded.addAll(rows);
            expanded.add(constructor.newInstance(null, null,
                    "Motion Sickness", "", false));
            expanded.add(constructor.newInstance("accessibility",
                    "reduce_restore_motion", "Disable Load Transition",
                    "Disables the animated FOV and screen effect after loading a save.",
                    false));
            rowsField.set(screen, List.copyOf(expanded));
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not add the load-transition accessibility row", exception);
        }
    }

    private static boolean contains(List<?> rows, String expected)
            throws ReflectiveOperationException {
        for (Object row : rows) {
            var method = row.getClass().getDeclaredMethod("label");
            method.setAccessible(true);
            if (expected.equals(String.valueOf(method.invoke(row)))) return true;
        }
        return false;
    }
}
