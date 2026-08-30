package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client-local debug HUD preferences. These switches only control presentation
 * for the local player and must never become server gameplay state.
 */
public final class StealthDebugClientPreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get()
            .resolve("scp_classified_directive")
            .resolve("client_debug.json");

    private static boolean loaded;
    private static boolean showScp079EnergyHud;
    private static boolean showScp079DecisionLogHud;
    private static boolean showScpSpawnTimersHud;
    private static boolean showStealthHud;

    private StealthDebugClientPreferences() {
    }

    public static synchronized boolean showScp079EnergyHud() {
        loadIfNeeded();
        return showScp079EnergyHud;
    }

    public static synchronized void setShowScp079EnergyHud(boolean enabled) {
        loadIfNeeded();
        if (showScp079EnergyHud == enabled) return;
        showScp079EnergyHud = enabled;
        save();
    }

    public static synchronized boolean toggleScp079EnergyHud() {
        setShowScp079EnergyHud(!showScp079EnergyHud());
        return showScp079EnergyHud;
    }

    public static synchronized boolean showScp079DecisionLogHud() {
        loadIfNeeded();
        return showScp079DecisionLogHud;
    }

    public static synchronized void setShowScp079DecisionLogHud(boolean enabled) {
        loadIfNeeded();
        if (showScp079DecisionLogHud == enabled) return;
        showScp079DecisionLogHud = enabled;
        save();
    }

    public static synchronized boolean toggleScp079DecisionLogHud() {
        setShowScp079DecisionLogHud(!showScp079DecisionLogHud());
        return showScp079DecisionLogHud;
    }

    public static synchronized boolean showScpSpawnTimersHud() {
        loadIfNeeded();
        return showScpSpawnTimersHud;
    }

    public static synchronized void setShowScpSpawnTimersHud(boolean enabled) {
        loadIfNeeded();
        if (showScpSpawnTimersHud == enabled) return;
        showScpSpawnTimersHud = enabled;
        save();
    }

    public static synchronized boolean toggleScpSpawnTimersHud() {
        setShowScpSpawnTimersHud(!showScpSpawnTimersHud());
        return showScpSpawnTimersHud;
    }

    public static synchronized boolean showStealthHud() {
        loadIfNeeded();
        return showStealthHud;
    }

    public static synchronized void setShowStealthHud(boolean enabled) {
        loadIfNeeded();
        if (showStealthHud == enabled) return;
        showStealthHud = enabled;
        save();
    }

    public static synchronized boolean toggleStealthHud() {
        setShowStealthHud(!showStealthHud());
        return showStealthHud;
    }

    private static void loadIfNeeded() {
        if (loaded) return;
        loaded = true;
        showScp079EnergyHud = false;
        showScp079DecisionLogHud = false;
        showScpSpawnTimersHud = false;
        showStealthHud = false;
        if (Files.notExists(PATH)) return;

        try {
            JsonObject root = JsonParser.parseString(
                    Files.readString(PATH, StandardCharsets.UTF_8)).getAsJsonObject();
            showScp079EnergyHud = bool(root, "show_scp_079_energy_hud", false);
            showScp079DecisionLogHud = bool(root,
                    "show_scp_079_decision_log_hud", false);
            showScpSpawnTimersHud = bool(root, "show_scp_spawn_timers_hud", false);
            showStealthHud = bool(root, "show_stealth_hud", false);
        } catch (Exception exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not read client debug preferences from {}", PATH,
                    exception);
        }
    }

    private static boolean bool(JsonObject root, String key, boolean fallback) {
        try {
            return root.has(key) && root.get(key).isJsonPrimitive()
                    ? root.get(key).getAsBoolean() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("show_scp_079_energy_hud", showScp079EnergyHud);
            root.addProperty("show_scp_079_decision_log_hud", showScp079DecisionLogHud);
            root.addProperty("show_scp_spawn_timers_hud", showScpSpawnTimersHud);
            root.addProperty("show_stealth_hud", showStealthHud);
            Files.writeString(PATH, GSON.toJson(root) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
        } catch (Exception exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not save client debug preferences to {}", PATH,
                    exception);
        }
    }
}
