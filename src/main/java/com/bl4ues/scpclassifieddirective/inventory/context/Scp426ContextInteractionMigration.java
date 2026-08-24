package com.bl4ues.scpclassifieddirective.inventory.context;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.config.ConfigFilePersistence;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Removes the obsolete bundled SCP-426 "Touch" prompt from existing context
 * configs. Explicitly custom SCP-426 rules are preserved.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID)
public final class Scp426ContextInteractionMigration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Scp426ContextInteractionMigration() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        removeLegacyRule();
    }

    private static void removeLegacyRule() {
        try {
            File configFile = ContextConfigManager.ensureConfigFile();
            Path config = configFile.toPath();
            if (!Files.isRegularFile(config)) return;

            JsonElement parsed = JsonParser.parseString(Files.readString(config));
            if (!parsed.isJsonObject()) return;
            JsonObject root = parsed.getAsJsonObject();
            if (!root.has("interactions")
                    || !root.get("interactions").isJsonArray()) return;

            JsonArray interactions = root.getAsJsonArray("interactions");
            boolean changed = false;
            for (int i = interactions.size() - 1; i >= 0; i--) {
                JsonElement element = interactions.get(i);
                if (!element.isJsonObject()) continue;
                JsonObject rule = element.getAsJsonObject();
                if (isLegacy426Rule(rule)) {
                    interactions.remove(i);
                    changed = true;
                }
            }

            if (!changed) return;

            ConfigFilePersistence.writeWithBackup(config,
                    GSON.toJson(root) + System.lineSeparator());
            ContextInteractionRegistry.reloadFromDisk();
            ScpClassifiedDirectiveMod.LOGGER.info(
                    "Removed obsolete SCP-426 contextual interaction prompt");
        } catch (Exception exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not remove obsolete SCP-426 contextual interaction prompt",
                    exception);
        }
    }

    private static boolean isLegacy426Rule(JsonObject rule) {
        if (!"block".equalsIgnoreCase(string(rule, "type"))) return false;
        if (!"scp_classified_directive:scp_426".equals(string(rule, "id"))) {
            return false;
        }

        JsonObject text = rule.has("text") && rule.get("text").isJsonObject()
                ? rule.getAsJsonObject("text") : null;
        if (text == null) return false;

        return "Touch".equals(string(text, "action"))
                && "SCP-426".equals(string(text, "name"));
    }

    private static String string(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)
                || object.get(key).isJsonNull()) return "";
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
