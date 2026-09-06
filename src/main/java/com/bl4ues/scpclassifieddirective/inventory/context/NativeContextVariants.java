package com.bl4ues.scpclassifieddirective.inventory.context;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared metadata for built-in item-specific contextual interactions. */
public final class NativeContextVariants {
    public static final String SCREWDRIVER_ID =
            ScpClassifiedDirectiveMod.MODID + ":screwdriver";
    public static final String SCREWDRIVER_INTERACTION =
            "configure_with_screwdriver";
    public static final String DISMANTLE_INTERACTION =
            "dismantle_with_screwdriver";

    private static final Set<String> READER_BLOCKS = Set.of(
            "right_reader", "lv_2_right_reader", "lv_3_right_reader",
            "lv_4_right_reader", "lv_5_right_reader", "lv_6_right_reader",
            "left_reader", "lv_2_left_reader", "lv_3_left_reader",
            "lv_4_left_reader", "lv_5_left_reader", "lv_6_left_reader");

    private static final Map<String, String> TOOL_BLOCKS = Map.ofEntries(
            Map.entry("tesla_terminal_block", "Configure"),
            Map.entry("tesla_terminal_off", "Configure"),
            Map.entry("core_room_elevator_station", "Configure Display"),
            Map.entry("sign_support", "Edit"),
            Map.entry("core_room_sign", "Edit"),
            Map.entry("door_sign", "Edit"),
            Map.entry("facility_prop_part", "Edit"),
            Map.entry("scp_294", "Configure"),
            Map.entry("scp_914", "Configure"),
            Map.entry("scp_914_reservation", "Configure"),
            Map.entry("scp_914_collision", "Configure"),
            Map.entry("scp_914_door_collision", "Configure"));

    private static final Set<String> TOOL_ENTITIES = Set.of(
            "scp_131_a", "scp_131_b", "roomba");

    private NativeContextVariants() {
    }

    /**
     * Enumerates every target that receives a built-in item-specific rule.
     * Configuration UI code uses this to surface native rules even when an
     * older external config predates the target entirely.
     */
    public static List<NativeTarget> nativeTargets() {
        List<NativeTarget> result = new ArrayList<>();
        for (String path : READER_BLOCKS) {
            result.add(new NativeTarget("block",
                    new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path)));
        }
        for (String path : TOOL_BLOCKS.keySet()) {
            result.add(new NativeTarget("block",
                    new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path)));
        }
        for (String path : TOOL_ENTITIES) {
            result.add(new NativeTarget("entity",
                    new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path)));
        }
        return List.copyOf(result);
    }

    public static boolean isNativeTarget(String type, ResourceLocation id) {
        if (id == null || !ScpClassifiedDirectiveMod.MODID.equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        return "block".equalsIgnoreCase(type)
                ? READER_BLOCKS.contains(path) || TOOL_BLOCKS.containsKey(path)
                : "entity".equalsIgnoreCase(type)
                && TOOL_ENTITIES.contains(path);
    }

    public static JsonArray mergedVariants(String type, ResourceLocation id,
            JsonObject rule) {
        JsonArray result = new JsonArray();
        if (rule != null && rule.has("variants")
                && rule.get("variants").isJsonArray()) {
            for (JsonElement element : rule.getAsJsonArray("variants")) {
                result.add(element.deepCopy());
            }
        }
        if (!isNativeTarget(type, id)) return result;

        String key = "entity".equalsIgnoreCase(type)
                ? DISMANTLE_INTERACTION : SCREWDRIVER_INTERACTION;
        for (JsonElement element : result) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            if (key.equals(string(object, "interactionId", ""))) {
                return result;
            }
        }
        result.add(nativeVariant(type, id));
        return result;
    }

    public static JsonObject nativeVariant(String type, ResourceLocation id) {
        JsonObject variant = new JsonObject();
        boolean entity = "entity".equalsIgnoreCase(type);
        variant.addProperty("interactionId", entity
                ? DISMANTLE_INTERACTION : SCREWDRIVER_INTERACTION);
        variant.addProperty("priority", isScpMachine(id) ? 150 : 45);
        variant.addProperty("useItem", "hand");

        JsonObject text = new JsonObject();
        text.addProperty("action", nativeAction(type, id));
        text.addProperty("showAction", true);
        variant.add("text", text);

        JsonObject input = new JsonObject();
        input.addProperty("requiredItem", SCREWDRIVER_ID);
        variant.add("input", input);

        JsonObject visual = new JsonObject();
        visual.addProperty("icon", "config");
        variant.add("visual", visual);
        return variant;
    }

    private static boolean isScpMachine(ResourceLocation id) {
        return id != null && ("scp_294".equals(id.getPath())
                || id.getPath().startsWith("scp_914"));
    }

    private static String nativeAction(String type, ResourceLocation id) {
        if ("entity".equalsIgnoreCase(type)) return "Dismantle";
        if (id == null) return "Configure";
        if (READER_BLOCKS.contains(id.getPath())) return "Configure";
        return TOOL_BLOCKS.getOrDefault(id.getPath(), "Configure");
    }

    private static String string(JsonObject object, String key,
            String fallback) {
        try {
            return object.has(key) && !object.get(key).isJsonNull()
                    ? object.get(key).getAsString() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public record NativeTarget(String type, ResourceLocation id) {
    }
}
