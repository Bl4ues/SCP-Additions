package net.mcreator.scpadditions.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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

/** Local-only preference backing the Custom Death Screen module row. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CustomDeathScreenPreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get()
            .resolve("scpadditions").resolve("death_screen.json");

    private static volatile boolean enabled = true;

    private CustomDeathScreenPreferences() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(CustomDeathScreenPreferences::load);
    }

    public static boolean enabled() {
        return enabled;
    }

    public static synchronized void load() {
        boolean loaded = true;
        try {
            Files.createDirectories(PATH.getParent());
            if (Files.exists(PATH)) {
                try (Reader reader = Files.newBufferedReader(PATH,
                        StandardCharsets.UTF_8)) {
                    JsonObject root = GSON.fromJson(reader, JsonObject.class);
                    if (root != null && root.has("enabled")) {
                        loaded = root.get("enabled").getAsBoolean();
                    }
                }
            }
            enabled = loaded;
            write();
        } catch (Exception exception) {
            enabled = loaded;
            ScpAdditionsMod.LOGGER.error(
                    "Failed to load custom death screen preference", exception);
        }
    }

    public static synchronized void captureAndSave(JsonObject modules) {
        if (modules == null) return;
        JsonObject ui = object(modules, "ui");
        if (ui.has("custom_death_screen")) {
            try {
                enabled = ui.get("custom_death_screen").getAsBoolean();
            } catch (Exception ignored) {
            }
        }
        try {
            write();
        } catch (IOException exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Failed to save custom death screen preference", exception);
        }
    }

    public static synchronized void applyTo(JsonObject modules) {
        if (modules == null) return;
        object(modules, "ui").addProperty("custom_death_screen", enabled);
    }

    public static synchronized void reset(JsonObject modules) {
        enabled = true;
        applyTo(modules);
        try {
            write();
        } catch (IOException exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Failed to reset custom death screen preference", exception);
        }
    }

    private static void write() throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", enabled);
        ConfigFilePersistence.writeWithBackup(PATH,
                GSON.toJson(root) + System.lineSeparator());
    }

    private static JsonObject object(JsonObject root, String key) {
        if (!root.has(key) || !root.get(key).isJsonObject()) {
            root.add(key, new JsonObject());
        }
        return root.getAsJsonObject(key);
    }
}
