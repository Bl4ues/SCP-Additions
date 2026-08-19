package com.bl4ues.scpinventory.context;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads the authoritative bundled context interaction template. Keeping the
 * default in one JSON resource prevents the in-code fallback from drifting away
 * from the file shipped in the JAR.
 */
public final class DefaultContextInteractions {
    private static final String BUNDLED_CONFIG =
            "config/scpinventory/context_interactions.json";
    private static final String EMERGENCY_FALLBACK = """
            {
              "_comment": "Bundled context interaction defaults were unavailable.",
              "interactions": [],
              "examples": []
            }
            """;
    private static final String CORPSE_SEARCH_RULE = """
            {
              "type": "entity",
              "id": "scp_additions:player_corpse",
              "interactionId": "search_body",
              "range": 2.5,
              "priority": 80,
              "useItem": "hand",
              "icon": "hand",
              "text": {
                "action": "Search",
                "nameMode": "auto",
                "showAction": true,
                "showName": true
              },
              "anchor": {
                "position": [0.5, 0.22, 0.5],
                "rotateWith": "none"
              },
              "input": {
                "allowE": true,
                "allowRightClick": true
              }
            }
            """;

    private DefaultContextInteractions() {
    }

    public static String loadBundledConfig() {
        try (InputStream stream = DefaultContextInteractions.class.getClassLoader()
                .getResourceAsStream(BUNDLED_CONFIG)) {
            if (stream != null) {
                String raw = new String(stream.readAllBytes(),
                        StandardCharsets.UTF_8);
                return withRuntimeDefaults(raw);
            }
        } catch (Exception exception) {
            ScpInventoryMod.LOGGER.error(
                    "Failed to read bundled context interaction defaults", exception);
        }
        return withRuntimeDefaults(EMERGENCY_FALLBACK);
    }

    /**
     * Some integrated interactions belong to runtime-only entities rather than
     * the old standalone SCP Inventory template. Add them here so existing user
     * configs receive the new default without being rewritten or reset.
     */
    private static String withRuntimeDefaults(String raw) {
        try {
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            JsonArray interactions;
            if (root.has("interactions")
                    && root.get("interactions").isJsonArray()) {
                interactions = root.getAsJsonArray("interactions");
            } else {
                interactions = new JsonArray();
                root.add("interactions", interactions);
            }

            boolean exists = false;
            for (JsonElement element : interactions) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                if ("entity".equalsIgnoreCase(string(object, "type"))
                        && "scp_additions:player_corpse".equals(
                        string(object, "id"))
                        && "search_body".equals(string(object,
                        "interactionId"))) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                interactions.add(JsonParser.parseString(CORPSE_SEARCH_RULE)
                        .getAsJsonObject());
            }
            return root.toString();
        } catch (Exception exception) {
            ScpInventoryMod.LOGGER.error(
                    "Failed to append runtime contextual interaction defaults",
                    exception);
            return raw;
        }
    }

    private static String string(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) return "";
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
