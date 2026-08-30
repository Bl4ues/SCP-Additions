package com.bl4ues.scpclassifieddirective.inventory.context;

import com.bl4ues.scpclassifieddirective.inventory.ScpInventoryMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Loads and normalizes the bundled contextual-interaction defaults. */
public final class DefaultContextInteractions {
    private static final String BUNDLED_CONFIG =
            "config/scp_classified_directive/context_interactions.json";
    private static final String EMERGENCY_FALLBACK = """
            {"_comment":"Bundled context interaction defaults were unavailable.","interactions":[],"examples":[]}
            """;

    private static final String CORPSE_SEARCH_RULE = rule(
            "entity", "scp_classified_directive:player_corpse", "search_body",
            "Search", "", "auto", 2.5, 80, 0.5, 0.22, 0.5);
    private static final String SCP714_TAKE_RULE = rule(
            "block", "scp_classified_directive:scp_714_placed", "take_scp_714",
            "Take", "SCP-714", "manual", 2.5, 85, 0.5, 0.08, 0.5);
    private static final String SCP1576_TAKE_RULE = rule(
            "block", "scp_classified_directive:scp_1576_placed", "take_scp_1576",
            "Take", "SCP-1576", "manual", 2.5, 85, 0.5, 0.35, 0.5);
    private static final String SCP426_TAKE_RULE = rule(
            "block", "scp_classified_directive:scp_426", "take_scp_426",
            "Take", "SCP-426", "manual", 2.5, 85, 0.5, 0.18, 0.5);

    private static final String SCP914_DIAL_RULE = """
            {
              "type":"block","id":"scp_classified_directive:scp_914","interactionId":"scp_914_dial",
              "range":2.25,"priority":65,"useItem":"hand","icon":"hand",
              "text":{"action":"","nameMode":"manual","name":"","showAction":false,"showName":false},
              "anchor":{"position":[0.5,1.2525,-0.015625],"rotateWith":"auto"},
              "input":{"allowE":false,"allowRightClick":false},"click":{"face":"front"},
              "visual":{"allowOffscreen":false,"scale":0.72}
            }
            """;
    private static final String SCP914_START_RULE = """
            {
              "type":"block","id":"scp_classified_directive:scp_914","interactionId":"scp_914_start",
              "range":2.25,"priority":70,"useItem":"hand","icon":"hand",
              "text":{"action":"Start","nameMode":"manual","name":"","showAction":true,"showName":false},
              "anchor":{"position":[0.5,0.90625,-0.0671875],"rotateWith":"auto"},
              "input":{"allowE":true,"allowRightClick":true},"click":{"face":"front"},
              "visual":{"allowOffscreen":false,"scale":0.72}
            }
            """;

    private DefaultContextInteractions() {
    }

    private static String rule(String type, String id, String interactionId,
            String action, String name, String nameMode, double range,
            int priority, double x, double y, double z) {
        return "{\"type\":\"" + type + "\",\"id\":\"" + id
                + "\",\"interactionId\":\"" + interactionId
                + "\",\"range\":" + range + ",\"priority\":" + priority
                + ",\"useItem\":\"hand\",\"icon\":\"hand\","
                + "\"text\":{\"action\":\"" + action
                + "\",\"nameMode\":\"" + nameMode + "\",\"name\":\""
                + name + "\",\"showAction\":true,\"showName\":true},"
                + "\"anchor\":{\"position\":[" + x + "," + y + "," + z
                + "],\"rotateWith\":\"none\"},"
                + "\"input\":{\"allowE\":true,\"allowRightClick\":true}}";
    }

    public static String loadBundledConfig() {
        try (InputStream stream = DefaultContextInteractions.class.getClassLoader()
                .getResourceAsStream(BUNDLED_CONFIG)) {
            if (stream != null) {
                return withRuntimeDefaults(new String(stream.readAllBytes(),
                        StandardCharsets.UTF_8));
            }
        } catch (Exception exception) {
            ScpInventoryMod.LOGGER.error(
                    "Failed to read bundled context interaction defaults", exception);
        }
        return withRuntimeDefaults(EMERGENCY_FALLBACK);
    }

    private static String withRuntimeDefaults(String raw) {
        try {
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            JsonArray interactions = root.has("interactions")
                    && root.get("interactions").isJsonArray()
                    ? root.getAsJsonArray("interactions") : new JsonArray();
            if (!root.has("interactions") || !root.get("interactions").isJsonArray()) {
                root.add("interactions", interactions);
            }

            for (int i = interactions.size() - 1; i >= 0; i--) {
                JsonElement element = interactions.get(i);
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                String type = string(object, "type");
                String id = string(object, "id");
                if ("block".equalsIgnoreCase(type) && isLegacyScp914Id(id)) {
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

            boolean corpse = false;
            boolean scp714 = false;
            boolean scp1576 = false;
            boolean scp426 = false;
            boolean dial = false;
            boolean start = false;
            for (JsonElement element : interactions) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                String type = string(object, "type");
                String id = string(object, "id");
                String key = string(object, "interactionId");
                corpse |= "entity".equalsIgnoreCase(type)
                        && "scp_classified_directive:player_corpse".equals(id)
                        && "search_body".equals(key);
                scp714 |= "block".equalsIgnoreCase(type)
                        && "scp_classified_directive:scp_714_placed".equals(id)
                        && "take_scp_714".equals(key);
                scp1576 |= "block".equalsIgnoreCase(type)
                        && "scp_classified_directive:scp_1576_placed".equals(id)
                        && "take_scp_1576".equals(key);
                scp426 |= "block".equalsIgnoreCase(type)
                        && "scp_classified_directive:scp_426".equals(id)
                        && "take_scp_426".equals(key);
                if ("block".equalsIgnoreCase(type)
                        && "scp_classified_directive:scp_914".equals(id)) {
                    dial |= "scp_914_dial".equals(key);
                    start |= "scp_914_start".equals(key);
                }
            }

            appendIfMissing(interactions, corpse, CORPSE_SEARCH_RULE);
            appendIfMissing(interactions, scp714, SCP714_TAKE_RULE);
            appendIfMissing(interactions, scp1576, SCP1576_TAKE_RULE);
            appendIfMissing(interactions, scp426, SCP426_TAKE_RULE);
            appendIfMissing(interactions, dial, SCP914_DIAL_RULE);
            appendIfMissing(interactions, start, SCP914_START_RULE);
            return root.toString();
        } catch (Exception exception) {
            ScpInventoryMod.LOGGER.error(
                    "Failed to append runtime contextual interaction defaults",
                    exception);
            return raw;
        }
    }

    private static void appendIfMissing(JsonArray interactions, boolean exists,
            String json) {
        if (!exists) interactions.add(JsonParser.parseString(json).getAsJsonObject());
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
        setAnchor(object, 0.767D, 0.252D, -0.702D);
        JsonObject text = object.has("text") && object.get("text").isJsonObject()
                ? object.getAsJsonObject("text") : new JsonObject();
        object.add("text", text);
        text.addProperty("action", "Ingest");
        text.addProperty("nameMode", "manual");
        text.addProperty("name", "SCP-1176-1");
        text.addProperty("showAction", true);
        text.addProperty("showName", true);

        JsonArray variants = object.has("variants")
                && object.get("variants").isJsonArray()
                ? object.getAsJsonArray("variants") : new JsonArray();
        object.add("variants", variants);
        for (JsonElement element : variants) {
            if (element.isJsonObject() && "take_scp_1176_honey".equals(
                    string(element.getAsJsonObject(), "interactionId"))) return;
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
        JsonObject anchor = object.has("anchor") && object.get("anchor").isJsonObject()
                ? object.getAsJsonObject("anchor") : new JsonObject();
        object.add("anchor", anchor);
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
