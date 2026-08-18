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

/** Adds the MineZero rewind-motion preference under Accessibility. */
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
                    || contains(rows, "Reduce Restore Motion")) return;

            Class<?> rowType = rows.get(0).getClass();
            Constructor<?> constructor = rowType.getDeclaredConstructor(
                    String.class, String.class, String.class, String.class,
                    boolean.class);
            constructor.setAccessible(true);

            List<Object> expanded = new ArrayList<>(rows.size() + 2);
            expanded.addAll(rows);
            expanded.add(constructor.newInstance(null, null,
                    "Motion Sickness", "", false));
            expanded.add(constructor.newInstance("accessibility",
                    "reduce_restore_motion", "Reduce Restore Motion",
                    "Disables the zoom, expanded-FOV and tunnel transition used when a MineZero checkpoint is restored.",
                    false));
            rowsField.set(screen, List.copyOf(expanded));
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not add the restore-motion accessibility row", exception);
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
