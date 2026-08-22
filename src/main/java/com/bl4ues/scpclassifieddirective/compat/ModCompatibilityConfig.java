package com.bl4ues.scpclassifieddirective.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.config.ConfigFilePersistence;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Host-owned opt-in compatibility settings for optional external mods. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModCompatibilityConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get()
            .resolve("scp_classified_directive").resolve("compatibilities.json");

    private static volatile Data current = new Data();

    private ModCompatibilityConfig() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModCompatibilityConfig::load);
    }

    public static synchronized void load() {
        Data loaded = new Data();
        try {
            Files.createDirectories(PATH.getParent());
            if (Files.exists(PATH)) {
                try (Reader reader = Files.newBufferedReader(PATH,
                        StandardCharsets.UTF_8)) {
                    Data parsed = GSON.fromJson(reader, Data.class);
                    if (parsed != null) loaded = parsed;
                }
            }
            current = loaded;
            write();
        } catch (Exception exception) {
            current = loaded;
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Failed to load mod compatibility settings", exception);
        }
    }

    public static boolean mineZeroEnabled() {
        return current.mineZero;
    }

    public static synchronized boolean setMineZeroEnabled(boolean value) {
        current.mineZero = value;
        try {
            write();
            return true;
        } catch (IOException exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Failed to save MineZero compatibility setting", exception);
            return false;
        }
    }

    public static boolean simpleVoiceChatEnabled() {
        return current.simpleVoiceChat;
    }

    public static synchronized boolean setSimpleVoiceChatEnabled(boolean value) {
        current.simpleVoiceChat = value;
        try {
            write();
            return true;
        } catch (IOException exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Failed to save Simple Voice Chat compatibility setting",
                    exception);
            return false;
        }
    }

    private static void write() throws IOException {
        ConfigFilePersistence.writeWithBackup(PATH,
                GSON.toJson(current) + System.lineSeparator());
    }

    private static final class Data {
        private boolean mineZero = true;
        private boolean simpleVoiceChat = true;
    }
}
