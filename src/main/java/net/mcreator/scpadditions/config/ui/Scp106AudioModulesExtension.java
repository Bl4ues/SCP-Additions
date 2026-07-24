package net.mcreator.scpadditions.config.ui;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Adds the optional entry-sound setting to the extended Modules screen. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class Scp106AudioModulesExtension {
    private static final String EXTENDED_SCREEN =
            "net.mcreator.scpadditions.config.ui."
                    + "Scp079ModulesScreenExtension$ExtendedToggleScreen";

    private Scp106AudioModulesExtension() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Pre event) {
        Object screen = event.getScreen();
        if (screen == null || !EXTENDED_SCREEN.equals(
                screen.getClass().getName())
                || !"General & Modules".equals(
                event.getScreen().getTitle().getString())) {
            return;
        }

        try {
            Field rowsField = screen.getClass().getDeclaredField("rows");
            rowsField.setAccessible(true);
            Object value = rowsField.get(screen);
            if (!(value instanceof List<?> rows) || rows.isEmpty()
                    || containsAudioRow(rows)) {
                return;
            }

            Class<?> rowClass = rows.get(0).getClass();
            Constructor<?> constructor = rowClass.getDeclaredConstructor(
                    String.class, String.class, String.class,
                    String.class, boolean.class);
            constructor.setAccessible(true);
            Object audioRow = constructor.newInstance(
                    "audio", "enter_sound_enabled", "World Entry Sound",
                    "Plays enter.ogg after joining or opening a world.", true);

            List<Object> expanded = new ArrayList<>(rows);
            int insertion = Math.max(0, expanded.size() - 1);
            expanded.add(insertion, audioRow);
            rowsField.set(screen, List.copyOf(expanded));
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not add the world entry sound module option",
                    exception);
        }
    }

    private static boolean containsAudioRow(List<?> rows)
            throws ReflectiveOperationException {
        for (Object row : rows) {
            Method group = row.getClass().getDeclaredMethod("group");
            Method key = row.getClass().getDeclaredMethod("key");
            group.setAccessible(true);
            key.setAccessible(true);
            if ("audio".equals(group.invoke(row))
                    && "enter_sound_enabled".equals(key.invoke(row))) {
                return true;
            }
        }
        return false;
    }
}
