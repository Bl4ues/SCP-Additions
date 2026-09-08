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

/** Removes legacy SCP-294 prompts and keeps its physical control anchors current. */
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

                String key = string(rule, "interactionId");
                if (key.isBlank()) key = string(rule, "interactionKey");
                if (key.isBlank()) {
                    interactions.remove(i);
                    changed = true;
                    continue;
                }
                if ("scp_294_coin".equals(key)) {
                    // Center of the measured coin-panel box:
                    // X 1.4..3.9, Y 21.6..25.6, Z -0.1..0.
                    changed |= setAnchor(rule, 0.165625D, 1.475D, -0.003125D);
                } else if ("scp_294_keyboard".equals(key)) {
                    changed |= setAnchor(rule, 0.59375D, 1.41875D, -0.015D);
                }
            }

            if (changed) {
                ConfigFilePersistence.writeWithBackup(config,
                        GSON.toJson(root) + System.lineSeparator());
                ContextInteractionRegistry.reloadFromDisk();
                ScpClassifiedDirectiveMod.LOGGER.info(
                        "Migrated SCP-294 physical context interactions");
            }
        } catch (Exception exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not migrate SCP-294 context interactions", exception);
        }
    }

    private static boolean setAnchor(JsonObject rule, double x, double y,
            double z) {
        JsonObject anchor = rule.has("anchor") && rule.get("anchor").isJsonObject()
                ? rule.getAsJsonObject("anchor") : new JsonObject();
        boolean changed = !rule.has("anchor") || !rule.get("anchor").isJsonObject();
        rule.add("anchor", anchor);

        JsonArray old = anchor.has("position") && anchor.get("position").isJsonArray()
                ? anchor.getAsJsonArray("position") : null;
        if (old == null || old.size() != 3
                || Math.abs(number(old, 0) - x) > 1.0E-6D
                || Math.abs(number(old, 1) - y) > 1.0E-6D
                || Math.abs(number(old, 2) - z) > 1.0E-6D) {
            JsonArray position = new JsonArray();
            position.add(x);
            position.add(y);
            position.add(z);
            anchor.add("position", position);
            changed = true;
        }
        if (!"auto".equalsIgnoreCase(string(anchor, "rotateWith"))) {
            anchor.addProperty("rotateWith", "auto");
            changed = true;
        }
        return changed;
    }

    private static double number(JsonArray array, int index) {
        try {
            return array.get(index).getAsDouble();
        } catch (Exception ignored) {
            return Double.NaN;
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
