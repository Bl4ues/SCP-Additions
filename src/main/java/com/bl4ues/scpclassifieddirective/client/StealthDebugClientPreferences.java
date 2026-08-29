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

/** Client-local developer preferences that must never become server gameplay state. */
public final class StealthDebugClientPreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get()
            .resolve("scp_classified_directive")
            .resolve("client_debug.json");

    private static boolean loaded;
    private static boolean showStealthHud;

    private StealthDebugClientPreferences() {
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
        showStealthHud = false;
        if (Files.notExists(PATH)) return;

        try {
            JsonObject root = JsonParser.parseString(
                    Files.readString(PATH, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("show_stealth_hud")) {
                showStealthHud = root.get("show_stealth_hud").getAsBoolean();
            }
        } catch (Exception exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not read client debug preferences from {}", PATH,
                    exception);
        }
    }

    private static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            JsonObject root = new JsonObject();
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
