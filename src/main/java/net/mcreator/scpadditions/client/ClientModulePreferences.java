package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ConfigFilePersistence;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * Persistent settings that belong to one Minecraft client rather than to the
 * world host. These values never alter server simulation or world state.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModulePreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("scpadditions").resolve("client_preferences.json");

    private static final Set<String> CLIENT_KEYS = Set.of(
            "crosshair.enabled",
            "crosshair.in_game_enabled",
            "crosshair.red",
            "crosshair.green",
            "crosshair.blue",
            "crosshair.alpha",
            "inventory.custom_hotbar",
            "hud.enabled",
            "hud.hide_active_effect_indicators",
            "hud.hide_empty_hand",
            "hud.disable_experience_bar",
            "hud.custom_oxygen_bar",
            "hud.action_bars_roboto",
            "hud.disable_text_drop_shadows",
            "hud.facility_chat_interface",
            "vitals.custom_health_enabled",
            "ui.custom_main_menu",
            "ui.custom_loading_screen",
            "audio.enter_sound_enabled",
            "audio.save_game_sound_enabled",
            "audio.custom_item_interaction_sounds",
            "audio.replace_player_hurt_sounds",
            "audio.use_voice_profile_b",
            "audio.mute_non_player_hit_sounds",
            "audio.disable_vanilla_music",
            "audio.main_menu_music_enabled",
            "accessibility.reduce_scp_012_visual_effects"
    );

    private static volatile Data current = new Data();

    private ClientModulePreferences() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientModulePreferences::load);
    }

    public static synchronized void load() {
        Data loaded = new Data();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH,
                        StandardCharsets.UTF_8)) {
                    Data parsed = GSON.fromJson(reader, Data.class);
                    if (parsed != null) loaded = parsed.normalize();
                }
            }
            current = loaded;
            write();
        } catch (IOException | JsonParseException exception) {
            current = loaded;
            ScpAdditionsMod.LOGGER.error(
                    "Failed to load client module preferences from {}",
                    CONFIG_PATH, exception);
        }
        applyRuntime();
    }

    public static synchronized void captureAndSave(JsonObject modules) {
        if (modules == null) return;
        Data next = current.copy();

        JsonObject crosshair = object(modules, "crosshair");
        next.crosshair.enabled = bool(crosshair, "enabled",
                next.crosshair.enabled);
        next.crosshair.inGameEnabled = bool(crosshair, "in_game_enabled",
                next.crosshair.inGameEnabled);
        next.crosshair.red = unit(crosshair, "red", next.crosshair.red);
        next.crosshair.green = unit(crosshair, "green", next.crosshair.green);
        next.crosshair.blue = unit(crosshair, "blue", next.crosshair.blue);
        next.crosshair.alpha = unit(crosshair, "alpha", next.crosshair.alpha);

        JsonObject inventory = object(modules, "inventory");
        next.inventory.customHotbar = bool(inventory, "custom_hotbar",
                next.inventory.customHotbar);

        JsonObject hud = object(modules, "hud");
        next.hud.enabled = bool(hud, "enabled", next.hud.enabled);
        next.hud.hideActiveEffectIndicators = bool(hud,
                "hide_active_effect_indicators",
                next.hud.hideActiveEffectIndicators);
        next.hud.hideEmptyHand = bool(hud, "hide_empty_hand",
                next.hud.hideEmptyHand);
        next.hud.disableExperienceBar = bool(hud,
                "disable_experience_bar", next.hud.disableExperienceBar);
        next.hud.customOxygenBar = bool(hud, "custom_oxygen_bar",
                next.hud.customOxygenBar);
        next.hud.actionBarsRoboto = bool(hud, "action_bars_roboto",
                next.hud.actionBarsRoboto);
        next.hud.disableTextDropShadows = bool(hud,
                "disable_text_drop_shadows",
                next.hud.disableTextDropShadows);
        next.hud.facilityChatInterface = bool(hud,
                "facility_chat_interface",
                next.hud.facilityChatInterface);

        JsonObject vitals = object(modules, "vitals");
        next.vitals.customHealthEnabled = bool(vitals,
                "custom_health_enabled", next.vitals.customHealthEnabled);

        JsonObject ui = object(modules, "ui");
        next.ui.customMainMenu = bool(ui, "custom_main_menu",
                next.ui.customMainMenu);
        next.ui.customLoadingScreen = bool(ui, "custom_loading_screen",
                next.ui.customLoadingScreen);

        JsonObject audio = object(modules, "audio");
        next.audio.enterSoundEnabled = bool(audio, "enter_sound_enabled",
                next.audio.enterSoundEnabled);
        next.audio.saveGameSoundEnabled = bool(audio,
                "save_game_sound_enabled", next.audio.saveGameSoundEnabled);
        next.audio.customItemInteractionSounds = bool(audio,
                "custom_item_interaction_sounds",
                next.audio.customItemInteractionSounds);
        next.audio.replacePlayerHurtSounds = bool(audio,
                "replace_player_hurt_sounds",
                next.audio.replacePlayerHurtSounds);
        next.audio.useVoiceProfileB = bool(audio, "use_voice_profile_b",
                next.audio.useVoiceProfileB);
        next.audio.muteNonPlayerHitSounds = bool(audio,
                "mute_non_player_hit_sounds",
                next.audio.muteNonPlayerHitSounds);
        next.audio.disableVanillaMusic = bool(audio,
                "disable_vanilla_music", next.audio.disableVanillaMusic);
        next.audio.mainMenuMusicEnabled = bool(audio,
                "main_menu_music_enabled", next.audio.mainMenuMusicEnabled);

        JsonObject accessibility = object(modules, "accessibility");
        next.accessibility.reduceScp012VisualEffects = bool(accessibility,
                "reduce_scp_012_visual_effects",
                next.accessibility.reduceScp012VisualEffects);

        current = next.normalize();
        try {
            write();
        } catch (IOException exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Failed to save client module preferences from {}",
                    CONFIG_PATH, exception);
        }
        applyRuntime();
    }

    public static synchronized void applyTo(JsonObject modules) {
        if (modules == null) return;
        Data value = current;

        JsonObject crosshair = object(modules, "crosshair");
        crosshair.addProperty("enabled", value.crosshair.enabled);
        crosshair.addProperty("in_game_enabled",
                value.crosshair.inGameEnabled);
        crosshair.addProperty("red", value.crosshair.red);
        crosshair.addProperty("green", value.crosshair.green);
        crosshair.addProperty("blue", value.crosshair.blue);
        crosshair.addProperty("alpha", value.crosshair.alpha);

        object(modules, "inventory").addProperty("custom_hotbar",
                value.inventory.customHotbar);

        JsonObject hud = object(modules, "hud");
        hud.addProperty("enabled", value.hud.enabled);
        hud.addProperty("hide_active_effect_indicators",
                value.hud.hideActiveEffectIndicators);
        hud.addProperty("hide_empty_hand", value.hud.hideEmptyHand);
        hud.addProperty("disable_experience_bar",
                value.hud.disableExperienceBar);
        hud.addProperty("custom_oxygen_bar", value.hud.customOxygenBar);
        hud.addProperty("action_bars_roboto", value.hud.actionBarsRoboto);
        hud.addProperty("disable_text_drop_shadows",
                value.hud.disableTextDropShadows);
        hud.addProperty("facility_chat_interface",
                value.hud.facilityChatInterface);

        object(modules, "vitals").addProperty("custom_health_enabled",
                value.vitals.customHealthEnabled);

        JsonObject ui = object(modules, "ui");
        ui.addProperty("custom_main_menu", value.ui.customMainMenu);
        ui.addProperty("custom_loading_screen", value.ui.customLoadingScreen);

        JsonObject audio = object(modules, "audio");
        audio.addProperty("enter_sound_enabled",
                value.audio.enterSoundEnabled);
        audio.addProperty("save_game_sound_enabled",
                value.audio.saveGameSoundEnabled);
        audio.addProperty("custom_item_interaction_sounds",
                value.audio.customItemInteractionSounds);
        audio.addProperty("replace_player_hurt_sounds",
                value.audio.replacePlayerHurtSounds);
        audio.addProperty("use_voice_profile_b",
                value.audio.useVoiceProfileB);
        audio.addProperty("mute_non_player_hit_sounds",
                value.audio.muteNonPlayerHitSounds);
        audio.addProperty("disable_vanilla_music",
                value.audio.disableVanillaMusic);
        audio.addProperty("main_menu_music_enabled",
                value.audio.mainMenuMusicEnabled);

        object(modules, "accessibility").addProperty(
                "reduce_scp_012_visual_effects",
                value.accessibility.reduceScp012VisualEffects);
    }

    public static synchronized void resetDefaults(JsonObject modules) {
        current = new Data();
        applyTo(modules);
        applyRuntime();
    }

    /** Keeps personal values from being written into the host's module file. */
    public static JsonObject mergeServerSettings(JsonObject working,
            JsonObject baseline) {
        JsonObject merged = baseline == null
                ? new JsonObject() : baseline.deepCopy();
        if (working == null) return merged;

        for (Map.Entry<String, JsonElement> groupEntry : working.entrySet()) {
            String group = groupEntry.getKey();
            JsonElement value = groupEntry.getValue();
            if (!value.isJsonObject()) {
                if (!isClientPreference(group, "")) {
                    merged.add(group, value.deepCopy());
                }
                continue;
            }

            JsonObject target = object(merged, group);
            for (Map.Entry<String, JsonElement> setting :
                    value.getAsJsonObject().entrySet()) {
                if (!isClientPreference(group, setting.getKey())) {
                    target.add(setting.getKey(), setting.getValue().deepCopy());
                }
            }
        }
        return merged;
    }

    public static boolean isClientPreference(String group, String key) {
        if (group == null || key == null) return false;
        return CLIENT_KEYS.contains(group + "." + key);
    }

    public static boolean mainMenuMusicEnabled() {
        return current.audio.mainMenuMusicEnabled;
    }

    public static boolean customMainMenuEnabled() {
        return current.ui.customMainMenu;
    }

    public static boolean customLoadingScreenEnabled() {
        return current.ui.customLoadingScreen;
    }

    public static boolean customItemInteractionSoundsEnabled() {
        return current.audio.customItemInteractionSounds;
    }

    public static boolean disableTextDropShadows() {
        return current.hud.disableTextDropShadows;
    }

    public static boolean facilityChatInterfaceEnabled() {
        return current.hud.facilityChatInterface;
    }

    private static synchronized void write() throws IOException {
        ConfigFilePersistence.writeWithBackup(CONFIG_PATH,
                GSON.toJson(current) + System.lineSeparator());
    }

    private static void applyRuntime() {
        Data value = current;
        InventoryModuleRuntimeState.updateLocalPreferences(
                value.hud.enabled,
                value.vitals.customHealthEnabled,
                value.inventory.customHotbar,
                value.accessibility.reduceScp012VisualEffects,
                value.audio.enterSoundEnabled,
                value.audio.saveGameSoundEnabled,
                value.audio.replacePlayerHurtSounds,
                value.audio.useVoiceProfileB,
                value.audio.muteNonPlayerHitSounds,
                value.audio.disableVanillaMusic,
                value.hud.hideActiveEffectIndicators,
                value.hud.hideEmptyHand,
                value.hud.disableExperienceBar,
                value.hud.customOxygenBar,
                value.hud.actionBarsRoboto,
                value.crosshair.enabled,
                value.crosshair.inGameEnabled,
                (float) value.crosshair.red,
                (float) value.crosshair.green,
                (float) value.crosshair.blue,
                (float) value.crosshair.alpha);
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
                || !root.get(key).isJsonPrimitive()) return fallback;
        try {
            return root.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double unit(JsonObject root, String key, double fallback) {
        if (root == null || !root.has(key)
                || !root.get(key).isJsonPrimitive()) return fallback;
        try {
            double value = root.get(key).getAsDouble();
            if (!Double.isFinite(value)) return fallback;
            return Math.max(0.0D, Math.min(1.0D, value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static final class Data {
        private Crosshair crosshair = new Crosshair();
        private Inventory inventory = new Inventory();
        private Hud hud = new Hud();
        private Vitals vitals = new Vitals();
        private Ui ui = new Ui();
        private Audio audio = new Audio();
        private Accessibility accessibility = new Accessibility();

        private Data normalize() {
            if (crosshair == null) crosshair = new Crosshair();
            if (inventory == null) inventory = new Inventory();
            if (hud == null) hud = new Hud();
            if (vitals == null) vitals = new Vitals();
            if (ui == null) ui = new Ui();
            if (audio == null) audio = new Audio();
            if (accessibility == null) accessibility = new Accessibility();
            crosshair.red = clamp(crosshair.red);
            crosshair.green = clamp(crosshair.green);
            crosshair.blue = clamp(crosshair.blue);
            crosshair.alpha = clamp(crosshair.alpha);
            return this;
        }

        private Data copy() {
            return GSON.fromJson(GSON.toJson(this), Data.class).normalize();
        }

        private static double clamp(double value) {
            if (!Double.isFinite(value)) return 1.0D;
            return Math.max(0.0D, Math.min(1.0D, value));
        }
    }

    private static final class Crosshair {
        private boolean enabled = true;
        private boolean inGameEnabled = true;
        private double red = 1.0D;
        private double green = 1.0D;
        private double blue = 1.0D;
        private double alpha = 1.0D;
    }

    private static final class Inventory {
        private boolean customHotbar = true;
    }

    private static final class Hud {
        private boolean enabled = true;
        private boolean hideActiveEffectIndicators = true;
        private boolean hideEmptyHand = true;
        private boolean disableExperienceBar = true;
        private boolean customOxygenBar = true;
        private boolean actionBarsRoboto = true;
        private boolean disableTextDropShadows = true;
        private boolean facilityChatInterface = true;
    }

    private static final class Vitals {
        private boolean customHealthEnabled = true;
    }

    private static final class Ui {
        private boolean customMainMenu = true;
        private boolean customLoadingScreen = true;
    }

    private static final class Audio {
        private boolean enterSoundEnabled = true;
        private boolean saveGameSoundEnabled = true;
        private boolean customItemInteractionSounds = true;
        private boolean replacePlayerHurtSounds = true;
        private boolean useVoiceProfileB;
        private boolean muteNonPlayerHitSounds;
        private boolean disableVanillaMusic = true;
        private boolean mainMenuMusicEnabled = true;
    }

    private static final class Accessibility {
        private boolean reduceScp012VisualEffects;
    }
}
