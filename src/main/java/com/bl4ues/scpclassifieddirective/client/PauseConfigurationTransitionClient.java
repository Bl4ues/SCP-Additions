package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterVisuals;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * Bridges the Forge Mods panel and the animated Configuration Center when the
 * panel lives inside the custom pause menu. The title screen already owns this
 * transition directly; the pause path needs to keep its in-world logo alive
 * until the Configuration Center takes over.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class PauseConfigurationTransitionClient {
    private static final int MOD_PANEL_FOOTER_HEIGHT = 28;

    private PauseConfigurationTransitionClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0
                || !(event.getScreen() instanceof CustomPauseMenuScreen pause)) {
            return;
        }
        if (!isScpClassifiedDirectiveSettingsClick(pause,
                event.getMouseX(), event.getMouseY())) {
            return;
        }

        event.setCanceled(true);
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(
                        ScpClassifiedDirectiveModSounds.SELECT.get(), 1.0F, 0.35F));
        pause.openConfigurationCenterAnimated();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof CustomPauseMenuScreen pause)) {
            return;
        }
        if (event.getCurrentScreen() == null
                || !event.getCurrentScreen().getClass().getName().startsWith(
                "com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterClient$")) {
            return;
        }
        pause.resumeFromConfiguration(
                ConfigCenterVisuals.outerAngle(),
                ConfigCenterVisuals.innerAngle());
    }

    private static boolean isScpClassifiedDirectiveSettingsClick(
            CustomPauseMenuScreen screen, double mouseX, double mouseY) {
        try {
            Object rawStates = readStaticField(PauseMenuModsPanelClient.class,
                    "STATES");
            if (!(rawStates instanceof Map<?, ?> states)) return false;
            Object state = states.get(screen);
            if (state == null
                    || !booleanField(state, "open")
                    || numberField(state, "progress", 0.0D) < 0.78D
                    || !booleanField(state, "hasConfig")
                    || !ScpClassifiedDirectiveMod.MODID.equals(
                    stringField(state, "selectedId"))) {
                return false;
            }

            Object layout = readField(state, "layout");
            if (layout == null) return false;
            int detailX = (int) numberField(layout, "detailX", Integer.MIN_VALUE);
            int detailRight = (int) numberField(layout, "detailRight", Integer.MIN_VALUE);
            int detailBottom = (int) numberField(layout, "detailBottom", Integer.MIN_VALUE);
            if (detailX == Integer.MIN_VALUE || detailRight == Integer.MIN_VALUE
                    || detailBottom == Integer.MIN_VALUE) {
                return false;
            }
            int top = detailBottom - MOD_PANEL_FOOTER_HEIGHT - 4;
            return mouseX >= detailX + 8 && mouseX < detailRight - 8
                    && mouseY >= top && mouseY < top + MOD_PANEL_FOOTER_HEIGHT;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object readStaticField(Class<?> owner, String name)
            throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        if (!Modifier.isStatic(field.getModifiers())) return null;
        return field.get(null);
    }

    private static Object readField(Object target, String name) {
        if (target == null) return null;
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static boolean booleanField(Object target, String name) {
        Object value = readField(target, name);
        return value instanceof Boolean bool && bool;
    }

    private static double numberField(Object target, String name,
            double fallback) {
        Object value = readField(target, name);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static String stringField(Object target, String name) {
        Object value = readField(target, name);
        return value instanceof String string ? string : "";
    }
}
