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
    private static final String SCP914_DIAL_RULE = """
            {
              "type": "block",
              "id": "scp_classified_directive:scp_914",
              "interactionId": "scp_914_dial",
              "range": 2.25,
              "priority": 65,
              "useItem": "hand",
              "icon": "hand",
              "text": {"action": "", "nameMode": "manual", "name": "", "showAction": false, "showName": false},
              "anchor": {"position": [0.5, 1.2525, -0.015625], "rotateWith": "auto"},
              "input": {"allowE": false, "allowRightClick": false},
              "click": {"face": "front"},
              "visual": {"allowOffscreen": false, "scale": 0.72}
            }
            """;
    private static final String SCP914_START_RULE = """
            {
              "type": "block",
              "id": "scp_classified_directive:scp_914",
              "interactionId": "scp_914_start",
              "range": 2.25,
              "priority": 70,
              "useItem": "hand",
              "icon": "hand",
              "text": {"action": "Start", "nameMode": "manual", "name": "", "showAction": true, "showName": false},
              "anchor": {"position": [0.5, 0.90625, -0.0671875], "rotateWith": "auto"},
              "input": {"allowE": true, "allowRightClick": true},
              "click": {"face": "front"},
              "visual": {"allowOffscreen": false, "scale": 0.72}
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

            // SCP-426 no longer has a direct interaction. Strip any historical
            // bundled rule before this JSON is used as the integrated layer.
            // User-authored rules in the external config remain untouched.
            // SCP-902-A keeps the same interaction anchor whether its lid is
            // open or closed, matching the fixed closed-box selection shape.
            for (int i = interactions.size() - 1; i >= 0; i--) {
                JsonElement element = interactions.get(i);
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                String type = string(object, "type");
                String id = string(object, "id");
                if ("block".equalsIgnoreCase(type)
                        && "scp_classified_directive:scp_426".equals(id)) {
                    interactions.remove(i);
                    continue;
                }
                if ("block".equalsIgnoreCase(type)
                        && isLegacyScp914Id(id)) {
                    interactions.remove(i);
                    continue;
                }
                if ("block".equalsIgnoreCase(type)
                        && ("scp_classified_directive:scp_902_closed".equals(id)
                        || "scp_classified_directive:scp_902_open".equals(id))) {
                    setAnchor(object, 0.418D, 0.052D, 0.5D);
                    continue;
                }
                if ("block".equalsIgnoreCase(type)
                        && "scp_classified_directive:scp_1176".equals(id)) {
                    normalizeScp1176(object);
                }
            }

            boolean corpseExists = false;
            boolean scp714Exists = false;
            boolean scp1576Exists = false;
            boolean scp914DialExists = false;
            boolean scp914StartExists = false;
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
                if ("block".equalsIgnoreCase(type)
                        && "scp_classified_directive:scp_914".equals(id)) {
                    if ("scp_914_dial".equals(interactionId)) {
                        scp914DialExists = true;
                    } else if ("scp_914_start".equals(interactionId)) {
                        scp914StartExists = true;
                    }
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
            if (!scp914DialExists) {
                interactions.add(JsonParser.parseString(SCP914_DIAL_RULE)
                        .getAsJsonObject());
            }
            if (!scp914StartExists) {
                interactions.add(JsonParser.parseString(SCP914_START_RULE)
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

    private static boolean isLegacyScp914Id(String id) {
        return id != null && (id.equals("scp_classified_directive:scp_914_key_wind")
                || id.startsWith("scp_classified_directive:scp_914dial_")
                || id.equals("scp_classified_directive:scp_914block")
                || id.startsWith("scp_classified_directive:scp_914clockworks")
                || id.equals("scp_classified_directive:scp_914body")
                || id.equals("scp_classified_directive:scp_914_intake")
                || id.equals("scp_classified_directive:scp_914_output")
                || id.startsWith("scp_classified_directive:scp_914_intake_door")
                || id.startsWith("scp_classified_directive:scp_914_output_door"));
    }

    private static void normalizeScp1176(JsonObject object) {
        // GeckoLib's authored model faces the contextual rotation opposite to
        // the raw model X axis, so the faucet's model-space X needs mirroring
        // around the block center for the world-space prompt to land on it.
        setAnchor(object, 0.767D, 0.252D, -0.702D);

        JsonObject text;
        if (object.has("text") && object.get("text").isJsonObject()) {
            text = object.getAsJsonObject("text");
        } else {
            text = new JsonObject();
            object.add("text", text);
        }
        text.addProperty("action", "Ingest");
        text.addProperty("nameMode", "manual");
        text.addProperty("name", "SCP-1176-1");
        text.addProperty("showAction", true);
        text.addProperty("showName", true);

        JsonArray variants;
        if (object.has("variants") && object.get("variants").isJsonArray()) {
            variants = object.getAsJsonArray("variants");
        } else {
            variants = new JsonArray();
            object.add("variants", variants);
        }
        for (JsonElement element : variants) {
            if (element.isJsonObject()
                    && "take_scp_1176_honey".equals(
                    string(element.getAsJsonObject(), "interactionId"))) {
                return;
            }
        }

        JsonObject take = new JsonObject();
        take.addProperty("interactionId", "take_scp_1176_honey");
        take.addProperty("priority", 45);
        JsonObject takeText = new JsonObject();
        takeText.addProperty("action", "Take");
        take.add("text", takeText);
        JsonObject input = new JsonObject();
        input.addProperty("requiredItem", "minecraft:glass_bottle");
        take.add("input", input);
        variants.add(take);
    }

    private static void setAnchor(JsonObject object, double x, double y,
            double z) {
        JsonObject anchor;
        if (object.has("anchor") && object.get("anchor").isJsonObject()) {
            anchor = object.getAsJsonObject("anchor");
        } else {
            anchor = new JsonObject();
            object.add("anchor", anchor);
        }
        JsonArray position = new JsonArray();
        position.add(x);
        position.add(y);
        position.add(z);
        anchor.add("position", position);
        anchor.addProperty("rotateWith", "auto");
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
