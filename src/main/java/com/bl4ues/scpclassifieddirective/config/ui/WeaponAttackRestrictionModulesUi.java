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

/** Adds the optional weapon-only attack rule to Gameplay Features. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class WeaponAttackRestrictionModulesUi {
    private static final String EXTENDED_MODULES_SCREEN =
            "com.bl4ues.scpclassifieddirective.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";
    private static final String ROW_TYPE =
            "com.bl4ues.scpclassifieddirective.config.ui.Scp079ModulesScreenExtension$Row";
    private static final String LABEL = "Require Equipped Weapon to Attack";

    private WeaponAttackRestrictionModulesUi() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInitPre(ScreenEvent.Init.Pre event) {
        Screen screen = event.getScreen();
        if (screen == null
                || !EXTENDED_MODULES_SCREEN.equals(screen.getClass().getName())
                || !"General & Modules".equals(screen.getTitle().getString())) {
            return;
        }
        injectRow(screen);
    }

    private static void injectRow(Screen screen) {
        try {
            Field rowsField = screen.getClass().getDeclaredField("rows");
            rowsField.setAccessible(true);
            Object value = rowsField.get(screen);
            if (!(value instanceof List<?> currentRows)) return;

            Class<?> rowType = Class.forName(ROW_TYPE);
            Method labelMethod = rowType.getDeclaredMethod("label");
            labelMethod.setAccessible(true);

            int preferencesIndex = -1;
            for (int i = 0; i < currentRows.size(); i++) {
                String label = String.valueOf(labelMethod.invoke(currentRows.get(i)));
                if (LABEL.equals(label)) return;
                if ("Preferences".equals(label)) preferencesIndex = i;
            }

            Constructor<?> constructor = rowType.getDeclaredConstructor(
                    String.class, String.class, String.class,
                    String.class, boolean.class);
            constructor.setAccessible(true);
            Object row = constructor.newInstance(
                    "inventory", "require_equipped_weapon_to_attack", LABEL,
                    "Prevents entity attacks and empty-air punches unless a Weapon is equipped.",
                    false);

            List<Object> updated = new ArrayList<>(currentRows);
            int insertAt = preferencesIndex >= 0
                    ? preferencesIndex : updated.size();
            updated.add(insertAt, row);
            rowsField.set(screen, List.copyOf(updated));
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not add the equipped-weapon attack module row",
                    exception);
        }
    }
}
