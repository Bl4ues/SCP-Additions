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

/**
 * Migrates only the obsolete bundled SCP-902 anchors. User-customized anchors
 * are left alone because the old coordinates must match exactly before a rule
 * is changed.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID)
public final class Scp902ContextAnchorMigration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG = Path.of("config", "scp_classified_directive",
            "context_interactions.json");

    private static final double[] OLD_CLOSED = {0.531D, 0.468D, 0.25D};
    private static final double[] NEW_CLOSED = {0.418D, 0.052D, 0.5D};
    private static final double[] OLD_OPEN = {0.491D, 0.65D, 0.812D};
    private static final double[] NEW_OPEN = {0.49D, 0.052D, 0.5D};

    private Scp902ContextAnchorMigration() {
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
                String id = string(rule, "id");
                if ("scp_classified_directive:scp_902_closed".equals(id)) {
                    changed |= migrateAnchor(rule, OLD_CLOSED, NEW_CLOSED);
                } else if ("scp_classified_directive:scp_902_open".equals(id)) {
                    changed |= migrateAnchor(rule, OLD_OPEN, NEW_OPEN);
                }
            }

            if (changed) {
                ConfigFilePersistence.writeWithBackup(CONFIG,
                        GSON.toJson(root) + System.lineSeparator());
                ContextInteractionRegistry.reloadFromDisk();
                ScpClassifiedDirectiveMod.LOGGER.info(
                        "Migrated legacy SCP-902 contextual interaction anchors");
            }
        } catch (Exception exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not migrate legacy SCP-902 contextual interaction anchors",
                    exception);
        }
    }

    private static boolean migrateAnchor(JsonObject rule, double[] oldValue,
            double[] newValue) {
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
            if (Math.abs(position.get(i).getAsDouble() - oldValue[i]) > 0.000001D) {
                return false;
            }
        }

        JsonArray replacement = new JsonArray();
        replacement.add(newValue[0]);
        replacement.add(newValue[1]);
        replacement.add(newValue[2]);
        anchor.add("position", replacement);
        anchor.addProperty("rotateWith", "auto");
        return true;
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
