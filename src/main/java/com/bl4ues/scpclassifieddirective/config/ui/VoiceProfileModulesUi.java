package com.bl4ues.scpclassifieddirective.config.ui;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.gui.screens.Screen;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.PlayerVoiceSounds;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Adds mutually exclusive player voice-profile controls below hurt audio. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class VoiceProfileModulesUi {
    private static final String EXTENDED_MODULES_SCREEN =
            "com.bl4ues.scpclassifieddirective.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";
    private static final String ROW_TYPE =
            "com.bl4ues.scpclassifieddirective.config.ui.Scp079ModulesScreenExtension$Row";
    private static final String PROFILE_ROW_LABEL = "Hurt Voice Profile";
    private static final String REPLACE_ROW_LABEL = "Replace Player Hurt Sounds";

    private static final Map<Screen, VoiceControls> CONTROLS =
            new WeakHashMap<>();

    private VoiceProfileModulesUi() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInitPre(ScreenEvent.Init.Pre event) {
        if (isGeneralModulesScreen(event.getScreen())) {
            syncProfileRow(event.getScreen(), false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInitPost(ScreenEvent.Init.Post event) {
        if (isGeneralModulesScreen(event.getScreen())) {
            wireControls(event.getScreen());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!isGeneralModulesScreen(screen)) return;
        syncProfileRow(screen, true);
        wireControls(screen);
    }

    private static boolean isGeneralModulesScreen(Screen screen) {
        return screen != null
                && EXTENDED_MODULES_SCREEN.equals(screen.getClass().getName())
                && "General & Modules".equals(screen.getTitle().getString());
    }

    private static void syncProfileRow(Screen screen, boolean rebuild) {
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

            int profileIndex = -1;
            int replaceIndex = -1;
            for (int i = 0; i < currentRows.size(); i++) {
                Object label = labelMethod.invoke(currentRows.get(i));
                if (PROFILE_ROW_LABEL.equals(label)) profileIndex = i;
                if (REPLACE_ROW_LABEL.equals(label)) replaceIndex = i;
            }

            boolean replacementEnabled = bool(object(working, "audio"),
                    "replace_player_hurt_sounds", true);
            if (replacementEnabled && profileIndex < 0) {
                Constructor<?> constructor = rowType.getDeclaredConstructor(
                        String.class, String.class, String.class,
                        String.class, boolean.class);
                constructor.setAccessible(true);
                Object profileRow = constructor.newInstance(
                        "audio", "use_voice_profile_b", PROFILE_ROW_LABEL,
                        "Selects the voice profile used for hurt reactions and recovery gasps.",
                        false);
                List<Object> updated = new ArrayList<>(currentRows);
                int insertAt = replaceIndex >= 0
                        ? replaceIndex + 1 : updated.size();
                updated.add(insertAt, profileRow);
                rowsField.set(screen, List.copyOf(updated));
                if (rebuild) rebuildWidgets(screen);
            } else if (!replacementEnabled && profileIndex >= 0) {
                List<Object> updated = new ArrayList<>(currentRows);
                updated.remove(profileIndex);
                rowsField.set(screen, List.copyOf(updated));
                CONTROLS.remove(screen);
                if (rebuild) rebuildWidgets(screen);
            }
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not synchronize player voice-profile controls",
                    exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static void wireControls(Screen screen) {
        try {
            Field buttonsField = screen.getClass().getDeclaredField("buttons");
            Field labelsField = screen.getClass().getDeclaredField("labels");
            Field workingField = screen.getClass().getDeclaredField("working");
            buttonsField.setAccessible(true);
            labelsField.setAccessible(true);
            workingField.setAccessible(true);

            Object buttonsValue = buttonsField.get(screen);
            Object labelsValue = labelsField.get(screen);
            Object workingValue = workingField.get(screen);
            if (!(buttonsValue instanceof List<?> rawButtons)
                    || !(labelsValue instanceof Map<?, ?> rawLabels)
                    || !(workingValue instanceof JsonObject working)) {
                return;
            }

            List<Button> buttons = (List<Button>) rawButtons;
            Map<Button, Component> labels =
                    (Map<Button, Component>) rawLabels;
            Button source = null;
            for (Map.Entry<Button, Component> entry : labels.entrySet()) {
                if (entry.getValue().getString().startsWith(
                        PROFILE_ROW_LABEL + ": ")) {
                    source = entry.getKey();
                    break;
                }
            }
            if (source == null) return;

            source.visible = false;
            source.active = false;
            VoiceControls existing = CONTROLS.get(screen);
            if (existing != null
                    && buttons.contains(existing.profileA())
                    && buttons.contains(existing.profileB())
                    && buttons.contains(existing.test())) {
                updateLabels(labels, existing, selectedProfileB(working));
                return;
            }

            int gap = 6;
            int testWidth = 92;
            int profilesWidth = source.getWidth() - testWidth - gap * 2;
            int profileAWidth = profilesWidth / 2;
            int profileBWidth = profilesWidth - profileAWidth;
            int x = source.getX();
            int y = source.getY();

            Button profileA = Button.builder(Component.empty(), button -> {
                setSelectedProfile(working, false);
                VoiceControls controls = CONTROLS.get(screen);
                if (controls != null) updateLabels(labels, controls, false);
            }).bounds(x, y, profileAWidth, source.getHeight()).build();

            Button profileB = Button.builder(Component.empty(), button -> {
                setSelectedProfile(working, true);
                VoiceControls controls = CONTROLS.get(screen);
                if (controls != null) updateLabels(labels, controls, true);
            }).bounds(x + profileAWidth + gap, y,
                    profileBWidth, source.getHeight()).build();

            Button test = Button.builder(Component.empty(), button ->
                    previewVoice(selectedProfileB(working)))
                    .bounds(x + profileAWidth + profileBWidth + gap * 2,
                            y, testWidth, source.getHeight()).build();

            VoiceControls controls = new VoiceControls(profileA, profileB, test);
            CONTROLS.put(screen, controls);
            buttons.add(profileA);
            buttons.add(profileB);
            buttons.add(test);
            addRenderableWidget(screen, profileA);
            addRenderableWidget(screen, profileB);
            addRenderableWidget(screen, test);
            updateLabels(labels, controls, selectedProfileB(working));
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not wire player voice-profile buttons",
                    exception);
        }
    }

    private static void updateLabels(Map<Button, Component> labels,
            VoiceControls controls, boolean profileB) {
        labels.put(controls.profileA(), ScpFonts.roboto(
                "Voice Profile A: " + (profileB ? "OFF" : "ON")));
        labels.put(controls.profileB(), ScpFonts.roboto(
                "Voice Profile B: " + (profileB ? "ON" : "OFF")));
        labels.put(controls.test(), ScpFonts.roboto("Test Voice"));
        controls.profileA().setMessage(Component.empty());
        controls.profileB().setMessage(Component.empty());
        controls.test().setMessage(Component.empty());
    }

    private static void setSelectedProfile(JsonObject working,
            boolean profileB) {
        object(working, "audio").addProperty(
                "use_voice_profile_b", profileB);
    }

    private static boolean selectedProfileB(JsonObject working) {
        return bool(object(working, "audio"),
                "use_voice_profile_b", false);
    }

    private static void previewVoice(boolean profileB) {
        SoundEvent sound = profileB
                ? PlayerVoiceSounds.VOICE_PROFILE_B_HURT.get()
                : ScpClassifiedDirectiveModSounds.PLAYER_HURT.get();
        RandomSource random = RandomSource.create();
        Minecraft.getInstance().getSoundManager().play(
                new SimpleSoundInstance(sound.getLocation(),
                        SoundSource.PLAYERS, 1.0F, 1.0F, random,
                        false, 0, SoundInstance.Attenuation.NONE,
                        0.0D, 0.0D, 0.0D, true));
    }

    private static void rebuildWidgets(Screen screen)
            throws ReflectiveOperationException {
        Method method = screen.getClass().getDeclaredMethod("rebuildWidgets");
        method.setAccessible(true);
        method.invoke(screen);
    }

    private static void addRenderableWidget(Screen screen, Button button)
            throws ReflectiveOperationException {
        Method target = null;
        for (Class<?> type = screen.getClass(); type != null && target == null;
                type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if ("addRenderableWidget".equals(method.getName())
                        && method.getParameterCount() == 1) {
                    target = method;
                    break;
                }
            }
        }
        if (target == null) {
            throw new NoSuchMethodException("Screen.addRenderableWidget");
        }
        target.setAccessible(true);
        target.invoke(screen, button);
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

    private record VoiceControls(Button profileA, Button profileB,
                                 Button test) {
    }
}
