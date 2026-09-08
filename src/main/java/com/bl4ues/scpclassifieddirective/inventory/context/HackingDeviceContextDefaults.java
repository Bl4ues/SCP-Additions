package com.bl4ues.scpclassifieddirective.inventory.context;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Set;

/** Adds update-safe pickup-style prompts without rewriting user context configs. */
public final class HackingDeviceContextDefaults {
    public static final String ATTACH_KEY = "attach_hacking_device";
    public static final String REMOVE_KEY = "remove_hacking_device";

    private static final Set<String> READER_IDS = Set.of(
            "right_reader", "lv_2_right_reader", "lv_3_right_reader",
            "lv_4_right_reader", "lv_5_right_reader", "lv_6_right_reader",
            "left_reader", "lv_2_left_reader", "lv_3_left_reader",
            "lv_4_left_reader", "lv_5_left_reader", "lv_6_left_reader");

    private HackingDeviceContextDefaults() {
    }

    public static String append(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        try {
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            JsonArray interactions = root.has("interactions")
                    && root.get("interactions").isJsonArray()
                    ? root.getAsJsonArray("interactions") : new JsonArray();
            root.add("interactions", interactions);

            for (JsonElement element : interactions) {
                if (!element.isJsonObject()) continue;
                JsonObject rule = element.getAsJsonObject();
                String id = text(rule, "id");
                if (!"block".equalsIgnoreCase(text(rule, "type"))
                        || !id.startsWith(ScpClassifiedDirectiveMod.MODID + ":")) {
                    continue;
                }
                String path = id.substring(id.indexOf(':') + 1);
                if (!READER_IDS.contains(path)) continue;
                appendReaderVariants(rule);
            }

            appendOcuRuleIfMissing(interactions, ATTACH_KEY, true);
            appendOcuRuleIfMissing(interactions, REMOVE_KEY, false);
            return root.toString();
        } catch (Exception exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Failed to append integrated Hacking Device context prompts",
                    exception);
            return raw;
        }
    }

    private static void appendReaderVariants(JsonObject rule) {
        JsonArray variants = rule.has("variants")
                && rule.get("variants").isJsonArray()
                ? rule.getAsJsonArray("variants") : new JsonArray();
        rule.add("variants", variants);
        if (!hasVariant(variants, ATTACH_KEY)) {
            variants.add(variant(ATTACH_KEY, "Attach", true, 96));
        }
        if (!hasVariant(variants, REMOVE_KEY)) {
            variants.add(variant(REMOVE_KEY, "Remove", false, 100));
        }
    }

    private static JsonObject variant(String key, String action,
            boolean requiresDevice, int priority) {
        JsonObject variant = new JsonObject();
        variant.addProperty("interactionId", key);
        variant.addProperty("priority", priority);
        variant.addProperty("useItem", "hand");
        variant.addProperty("icon", "pickup");

        JsonObject text = new JsonObject();
        text.addProperty("action", action);
        text.addProperty("nameMode", "manual");
        text.addProperty("name", "Hacking Device");
        text.addProperty("showAction", true);
        text.addProperty("showName", true);
        variant.add("text", text);

        JsonObject input = new JsonObject();
        input.addProperty("allowE", true);
        input.addProperty("allowRightClick", true);
        if (requiresDevice) {
            input.addProperty("requiredItem",
                    ScpClassifiedDirectiveMod.MODID + ":hacking_device");
        }
        variant.add("input", input);
        return variant;
    }

    private static void appendOcuRuleIfMissing(JsonArray interactions,
            String key, boolean requiresDevice) {
        String ocuId = ScpClassifiedDirectiveMod.MODID
                + ":object_containment_unit";
        for (JsonElement element : interactions) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            if (ocuId.equals(text(object, "id"))
                    && key.equals(text(object, "interactionId"))) {
                return;
            }
        }

        JsonObject rule = new JsonObject();
        rule.addProperty("type", "block");
        rule.addProperty("id", ocuId);
        rule.addProperty("interactionId", key);
        rule.addProperty("range", 1.75D);
        rule.addProperty("priority", requiresDevice ? 96 : 100);
        rule.addProperty("useItem", "hand");
        rule.addProperty("icon", "pickup");

        JsonObject text = new JsonObject();
        text.addProperty("action", requiresDevice ? "Attach" : "Remove");
        text.addProperty("nameMode", "manual");
        text.addProperty("name", "Hacking Device");
        text.addProperty("showAction", true);
        text.addProperty("showName", true);
        rule.add("text", text);

        JsonObject anchor = new JsonObject();
        JsonArray position = new JsonArray();
        position.add(0.5D - 9.625D / 16.0D);
        position.add(13.28094476D / 16.0D);
        position.add(0.5D + 0.90481263D / 16.0D);
        anchor.add("position", position);
        anchor.addProperty("rotateWith", "horizontal_facing");
        rule.add("anchor", anchor);

        JsonObject input = new JsonObject();
        input.addProperty("allowE", true);
        input.addProperty("allowRightClick", true);
        if (requiresDevice) {
            input.addProperty("requiredItem",
                    ScpClassifiedDirectiveMod.MODID + ":hacking_device");
        }
        rule.add("input", input);

        JsonObject click = new JsonObject();
        click.addProperty("face", "player");
        rule.add("click", click);

        JsonObject visual = new JsonObject();
        visual.addProperty("allowOffscreen", false);
        visual.addProperty("scale", 0.82D);
        rule.add("visual", visual);
        interactions.add(rule);
    }

    private static boolean hasVariant(JsonArray variants, String key) {
        for (JsonElement element : variants) {
            if (element.isJsonObject()
                    && key.equals(text(element.getAsJsonObject(),
                    "interactionId"))) {
                return true;
            }
        }
        return false;
    }

    private static String text(JsonObject object, String key) {
        try {
            return object != null && object.has(key)
                    && !object.get(key).isJsonNull()
                    ? object.get(key).getAsString() : "";
        } catch (Exception ignored) {
            return "";
        }
    }
}
