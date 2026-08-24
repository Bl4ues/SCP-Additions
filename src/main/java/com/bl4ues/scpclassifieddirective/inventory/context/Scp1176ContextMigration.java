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

/** Migrates obsolete built-in SCP-1176 contextual defaults in existing installs. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID)
public final class Scp1176ContextMigration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG = Path.of("config", "scp_classified_directive",
            "context_interactions.json");
    private static final double[] OLD_ANCHOR = {0.872D, 0.219D, -0.725D};
    private static final double[] FAUCET_ANCHOR = {0.233D, 0.252D, -0.702D};

    private Scp1176ContextMigration() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        migrateIfNeeded();
    }

    private static void migrateIfNeeded() {
        if (!Files.isRegularFile(CONFIG)) return;

        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(CONFIG));
            if (!parsed.isJsonObject()) return;
            JsonObject root = parsed.getAsJsonObject();
            if (!root.has("interactions")
                    || !root.get("interactions").isJsonArray()) return;

            boolean changed = false;
            for (JsonElement element : root.getAsJsonArray("interactions")) {
                if (!element.isJsonObject()) continue;
                JsonObject rule = element.getAsJsonObject();
                if (!"scp_classified_directive:scp_1176".equals(
                        string(rule, "id"))) continue;

                changed |= migrateAnchor(rule);
                changed |= migrateText(rule);
            }

            if (changed) {
                ConfigFilePersistence.writeWithBackup(CONFIG,
                        GSON.toJson(root) + System.lineSeparator());
                ContextInteractionRegistry.reloadFromDisk();
                ScpClassifiedDirectiveMod.LOGGER.info(
                        "Migrated legacy SCP-1176 contextual interaction defaults");
            }
        } catch (Exception exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not migrate legacy SCP-1176 contextual interaction defaults",
                    exception);
        }
    }

    private static boolean migrateAnchor(JsonObject rule) {
        if (!rule.has("anchor") || !rule.get("anchor").isJsonObject()) {
            return false;
        }
        JsonObject anchor = rule.getAsJsonObject("anchor");
        if (!anchor.has("position") || !anchor.get("position").isJsonArray()) {
            return false;
        }
        JsonArray position = anchor.getAsJsonArray("position");
        if (position.size() < 3) return false;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(position.get(i).getAsDouble() - OLD_ANCHOR[i])
                    > 0.000001D) {
                return false;
            }
        }

        JsonArray replacement = new JsonArray();
        replacement.add(FAUCET_ANCHOR[0]);
        replacement.add(FAUCET_ANCHOR[1]);
        replacement.add(FAUCET_ANCHOR[2]);
        anchor.add("position", replacement);
        anchor.addProperty("rotateWith", "auto");
        return true;
    }

    private static boolean migrateText(JsonObject rule) {
        if (!rule.has("text") || !rule.get("text").isJsonObject()) return false;
        JsonObject text = rule.getAsJsonObject("text");
        boolean changed = false;
        if ("Drink".equals(string(text, "action"))) {
            text.addProperty("action", "Ingest");
            changed = true;
        }
        if ("SCP-1176".equals(string(text, "name"))) {
            text.addProperty("name", "SCP-1176-1");
            changed = true;
        }
        return changed;
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
