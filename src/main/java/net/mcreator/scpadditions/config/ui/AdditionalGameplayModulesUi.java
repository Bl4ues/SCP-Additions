package net.mcreator.scpadditions.config.ui;

import net.minecraft.client.gui.GuiGraphics;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Adds the remaining presentation controls to Gameplay Features. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class AdditionalGameplayModulesUi {
    private static final String EXTENDED_MODULES_SCREEN =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";
    private static final String CROSSHAIR_SCREEN =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$CrosshairScreen";
    private static final String ROW_TYPE =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$Row";
    private static final int PANEL = 0xEE111317;

    private AdditionalGameplayModulesUi() {
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onScreenInitPre(ScreenEvent.Init.Pre event) {
        Screen screen = event.getScreen();
        if (isGeneralModulesScreen(screen)) {
            injectRows(screen);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (screen == null
                || !CROSSHAIR_SCREEN.equals(screen.getClass().getName())) {
            return;
        }

        // The preview size is an implementation detail, not a player-facing
        // setting. Remove the old explanatory line without disturbing the
        // preview or the controls around it.
        GuiGraphics graphics = event.getGuiGraphics();
        int panelWidth = Math.min(650, screen.width - 20);
        int panelX = Math.max(8, (screen.width - panelWidth) / 2);
        int panelY = Math.max(8,
                (screen.height - Math.min(360, screen.height - 16)) / 2);
        int coverRight = Math.min(panelX + panelWidth - 8, panelX + 242);
        graphics.fill(panelX + 16, panelY + 232,
                coverRight, panelY + 250, PANEL);
    }

    private static boolean isGeneralModulesScreen(Screen screen) {
        return screen != null
                && EXTENDED_MODULES_SCREEN.equals(screen.getClass().getName())
                && "General & Modules".equals(screen.getTitle().getString());
    }

    private static void injectRows(Screen screen) {
        try {
            Field rowsField = screen.getClass().getDeclaredField("rows");
            rowsField.setAccessible(true);
            Object value = rowsField.get(screen);
            if (!(value instanceof List<?> currentRows)) return;

            Class<?> rowType = Class.forName(ROW_TYPE);
            Method labelMethod = rowType.getDeclaredMethod("label");
            labelMethod.setAccessible(true);
            Set<String> labels = new HashSet<>();
            for (Object row : currentRows) {
                Object label = labelMethod.invoke(row);
                if (label instanceof String text) labels.add(text);
            }

            Constructor<?> constructor = rowType.getDeclaredConstructor(
                    String.class, String.class, String.class,
                    String.class, boolean.class);
            constructor.setAccessible(true);

            List<Object> additions = new ArrayList<>();
            if (!labels.contains("Hide Empty Hand")) {
                additions.add(constructor.newInstance(
                        "hud", "hide_empty_hand", "Hide Empty Hand",
                        "Hides the first-person arm whenever that hand is not holding an item.",
                        true));
            }
            if (!labels.contains("Disable Experience Bar")) {
                additions.add(constructor.newInstance(
                        "hud", "disable_experience_bar",
                        "Disable Experience Bar",
                        "Removes the vanilla experience bar and level indicator from the HUD.",
                        true));
            }
            if (!labels.contains("Custom Oxygen Bar")) {
                additions.add(constructor.newInstance(
                        "hud", "custom_oxygen_bar", "Custom Oxygen Bar",
                        "Replaces vanilla air bubbles with a centered survival-horror oxygen meter.",
                        true));
            }
            if (!labels.contains("Disable Text Drop Shadows")) {
                additions.add(constructor.newInstance(
                        "hud", "disable_text_drop_shadows",
                        "Disable Text Drop Shadows",
                        "Removes Minecraft's dark offset shadow from rendered text while preserving the text itself.",
                        true));
            }
            if (!labels.contains("Facility Chat Interface")) {
                additions.add(constructor.newInstance(
                        "hud", "facility_chat_interface",
                        "Facility Chat Interface",
                        "Reanchors vanilla chat to the upper-left and presents it as a top-down SCP Additions communications console.",
                        true));
            }
            if (additions.isEmpty()) return;

            List<Object> updated = new ArrayList<>(currentRows);
            int insertAt = Math.min(1, updated.size());
            updated.addAll(insertAt, additions);
            rowsField.set(screen, List.copyOf(updated));
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not add presentation modules to Gameplay Features",
                    exception);
        }
    }
}
