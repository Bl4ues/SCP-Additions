package com.bl4ues.scpclassifieddirective.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraftforge.fml.loading.FMLPaths;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.config.ConfigFilePersistence;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Client-local consent and first-run state for SCP-939 voice mimicry. */
public final class Scp939VoiceClientPreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("scp_classified_directive").resolve("scp939_voice_client.json");

    private static Data current = new Data();
    private static boolean loaded;

    private Scp939VoiceClientPreferences() {
    }

    public static synchronized boolean setupComplete() {
        ensureLoaded();
        return current.setupComplete;
    }

    public static synchronized boolean allowMimicry() {
        ensureLoaded();
        return current.allowMimicry;
    }

    public static synchronized void completeSetup(boolean allowMimicry) {
        ensureLoaded();
        current.setupComplete = true;
        current.allowMimicry = allowMimicry;
        save();
    }

    public static synchronized void setAllowMimicry(boolean allowMimicry) {
        ensureLoaded();
        current.allowMimicry = allowMimicry;
        save();
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Data loadedData = new Data();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.isRegularFile(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH,
                        StandardCharsets.UTF_8)) {
                    Data parsed = GSON.fromJson(reader, Data.class);
                    if (parsed != null) loadedData = parsed;
                }
            }
        } catch (IOException | JsonParseException exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Failed to load SCP-939 client voice preferences from {}",
                    CONFIG_PATH, exception);
        }
        current = loadedData;
    }

    private static void save() {
        try {
            ConfigFilePersistence.writeWithBackup(CONFIG_PATH,
                    GSON.toJson(current) + System.lineSeparator());
        } catch (IOException exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Failed to save SCP-939 client voice preferences to {}",
                    CONFIG_PATH, exception);
        }
    }

    private static final class Data {
        private boolean setupComplete;
        private boolean allowMimicry;
    }
}
