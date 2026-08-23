package com.bl4ues.scpclassifieddirective.inventory.context;

import com.bl4ues.scpclassifieddirective.inventory.ScpInventoryMod;
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
            "config/scp_classified_directive/context_interactions.json";
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
              "id": "scp_classified_directive:player_corpse",
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
    private static final String SCP714_TAKE_RULE = """
            {
              "type": "block",
              "id": "scp_classified_directive:scp_714_placed",
              "interactionId": "take_scp_714",
              "range": 2.5,
              "priority": 85,
              "useItem": "hand",
              "icon": "hand",
              "text": {
                "action": "Take",
                "nameMode": "manual",
                "name": "SCP-714",
                "showAction": true,
                "showName": true
              },
              "anchor": {
                "position": [0.5, 0.08, 0.5],
                "rotateWith": "none"
              },
              "input": {
                "allowE": true,
                "allowRightClick": true
              }
            }
            """;
    private static final String SCP1576_TAKE_RULE = """
            {
              "type": "block",
              "id": "scp_classified_directive:scp_1576_placed",
              "interactionId": "take_scp_1576",
              "range": 2.5,
              "priority": 85,
              "useItem": "hand",
              "icon": "hand",
              "text": {
                "action": "Take",
                "nameMode": "manual",
                "name": "SCP-1576",
                "showAction": true,
                "showName": true
              },
              "anchor": {
                "position": [0.5, 0.35, 0.5],
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
     * Runtime defaults are layered over old external configs so newly integrated
     * interactions appear without resetting a user's configuration file.
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

            boolean corpseExists = false;
            boolean scp714Exists = false;
            boolean scp1576Exists = false;
            for (JsonElement element : interactions) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                String type = string(object, "type");
                String id = string(object, "id");
                String interactionId = string(object, "interactionId");
                if ("entity".equalsIgnoreCase(type)
                        && "scp_classified_directive:player_corpse".equals(id)
                        && "search_body".equals(interactionId)) {
                    corpseExists = true;
                }
                if ("block".equalsIgnoreCase(type)
                        && "scp_classified_directive:scp_714_placed".equals(id)
                        && "take_scp_714".equals(interactionId)) {
                    scp714Exists = true;
                }
                if ("block".equalsIgnoreCase(type)
                        && "scp_classified_directive:scp_1576_placed".equals(id)
                        && "take_scp_1576".equals(interactionId)) {
                    scp1576Exists = true;
                }
            }
            if (!corpseExists) {
                interactions.add(JsonParser.parseString(CORPSE_SEARCH_RULE)
                        .getAsJsonObject());
            }
            if (!scp714Exists) {
                interactions.add(JsonParser.parseString(SCP714_TAKE_RULE)
                        .getAsJsonObject());
            }
            if (!scp1576Exists) {
                interactions.add(JsonParser.parseString(SCP1576_TAKE_RULE)
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
