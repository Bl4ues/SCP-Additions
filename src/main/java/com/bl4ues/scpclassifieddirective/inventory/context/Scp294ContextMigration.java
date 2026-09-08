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

import java.nio.file.Files;
import java.nio.file.Path;

/** Removes the old whole-machine SCP-294 prompt from existing installations. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID)
public final class Scp294ContextMigration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SCP_294 = "scp_classified_directive:scp_294";

    private Scp294ContextMigration() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        migrateIfNeeded();
    }

    private static void migrateIfNeeded() {
        try {
            Path config = ContextConfigManager.ensureConfigFile().toPath();
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
                if (!"block".equalsIgnoreCase(string(rule, "type"))
                        || !SCP_294.equals(string(rule, "id"))) {
                    continue;
                }
                // The historical rule had no interaction ID and treated the
                // complete vending machine as one generic "Use SCP-294" target.
                // Keyed user rules remain untouched.
                String key = string(rule, "interactionId");
                if (key.isBlank()) key = string(rule, "interactionKey");
                if (key.isBlank()) {
                    interactions.remove(i);
                    changed = true;
                }
            }

            if (changed) {
                ConfigFilePersistence.writeWithBackup(config,
                        GSON.toJson(root) + System.lineSeparator());
                ContextInteractionRegistry.reloadFromDisk();
                ScpClassifiedDirectiveMod.LOGGER.info(
                        "Migrated legacy whole-machine SCP-294 context prompt");
            }
        } catch (Exception exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not migrate legacy SCP-294 context prompt", exception);
        }
    }

    private static String string(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull()
                    ? object.get(key).getAsString() : "";
        } catch (Exception ignored) {
            return "";
        }
    }
}
