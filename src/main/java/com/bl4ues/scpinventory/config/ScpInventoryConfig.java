package com.bl4ues.scpinventory.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public final class ScpInventoryConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/scpinventory/scpinventory.json");
    private static final String BUNDLED_CONFIG = "config/scpinventory/scpinventory.json";

    private static final List<String> DEFAULT_ITEM_RULES = List.of(
            "minecraft:flint|COIN",
            "minecraft:arrow|AMMO",
            "minecraft:leather|ACCESSORYHAND",
            "minecraft:clock|ACCESSORY",
            "minecraft:fishing_rod|USABLE",
            "minecraft:spyglass|USABLE",
            "minecraft:bow|WEAPON",
            "minecraft:crossbow|WEAPON",
            "minecraft:shield|USABLE",
            "minecraft:goat_horn|USABLE",
            "minecraft:ender_pearl|USABLE",
            "minecraft:snowball|USABLE",
            "minecraft:egg|USABLE"
    );

    private static final List<String> DEFAULT_ITEM_EFFECTS = List.of(
            "minecraft:leather|NO_STAMINA"
    );

    private static final List<String> DEFAULT_HIDDEN_STATUS_EFFECTS = List.of(
            "minecraft:bad_omen"
    );

    private static final List<String> DEFAULT_SCP_173_TARGETS = List.of(
            "minecraft:villager",
            "minecraft:wandering_trader",
            "minecraft:iron_golem",
            "minecraft:snow_golem",
            "minecraft:enderman",
            "minecraft:zombie",
            "minecraft:zombie_villager",
            "minecraft:husk",
            "minecraft:drowned",
            "minecraft:zombified_piglin",
            "minecraft:skeleton",
            "minecraft:stray",
            "minecraft:wither_skeleton",
            "minecraft:pillager",
            "minecraft:vindicator",
            "minecraft:evoker",
            "minecraft:illusioner",
            "minecraft:ravager",
            "#minecraft:raiders"
    );

    private static final List<String> DEFAULT_CODEX_DOCUMENTS = List.of();

    public static final JsonListValue ITEM_RULES = new JsonListValue(ScpInventoryConfig::itemRules);
    public static final JsonListValue ITEM_EFFECTS = new JsonListValue(ScpInventoryConfig::itemEffects);
    public static final JsonListValue CODEX_DOCUMENTS = new JsonListValue(ScpInventoryConfig::codexDocuments);
    public static final JsonListValue HIDDEN_STATUS_EFFECTS = new JsonListValue(ScpInventoryConfig::hiddenStatusEffects);
    public static final JsonListValue SCP_173_TARGETS = new JsonListValue(ScpInventoryConfig::scp173Targets);

    private static boolean loaded = false;
    private static volatile boolean serverSnapshotActive = false;
    private static List<String> itemRules = DEFAULT_ITEM_RULES;
    private static List<String> itemEffects = DEFAULT_ITEM_EFFECTS;
    private static List<String> codexDocuments = DEFAULT_CODEX_DOCUMENTS;
    private static List<String> hiddenStatusEffects = DEFAULT_HIDDEN_STATUS_EFFECTS;
    private static List<String> scp173Targets = DEFAULT_SCP_173_TARGETS;

    private ScpInventoryConfig() {
    }

    public static List<String> itemRules() {
        ensureLoaded();
        return itemRules;
    }

    public static List<String> itemEffects() {
        ensureLoaded();
        return itemEffects;
    }

    public static List<String> codexDocuments() {
        ensureLoaded();
        return codexDocuments;
    }

    public static List<String> hiddenStatusEffects() {
        ensureLoaded();
        return hiddenStatusEffects;
    }

    public static List<String> scp173Targets() {
        ensureLoaded();
        return scp173Targets;
    }

    public static synchronized void applyServerSnapshot(
            List<String> serverItemRules,
            List<String> serverItemEffects,
            List<String> serverCodexDocuments,
            List<String> serverHiddenStatusEffects,
            List<String> serverScp173Targets) {
        itemRules = Collections.unmodifiableList(new ArrayList<>(serverItemRules));
        itemEffects = Collections.unmodifiableList(new ArrayList<>(serverItemEffects));
        codexDocuments = Collections.unmodifiableList(new ArrayList<>(serverCodexDocuments));
        hiddenStatusEffects = Collections.unmodifiableList(new ArrayList<>(serverHiddenStatusEffects));
        scp173Targets = Collections.unmodifiableList(new ArrayList<>(serverScp173Targets));
        serverSnapshotActive = true;
        loaded = true;
    }

    public static synchronized void clearServerSnapshot() {
        if (!serverSnapshotActive) {
            return;
        }
        serverSnapshotActive = false;
        loaded = false;
        load();
    }

    public static void reload() {
        if (serverSnapshotActive) {
            return;
        }
        loaded = false;
        load();
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private static void load() {
        itemRules = DEFAULT_ITEM_RULES;
        itemEffects = DEFAULT_ITEM_EFFECTS;
        codexDocuments = DEFAULT_CODEX_DOCUMENTS;
        hiddenStatusEffects = DEFAULT_HIDDEN_STATUS_EFFECTS;
        scp173Targets = DEFAULT_SCP_173_TARGETS;

        try {
            File dir = CONFIG_FILE.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            if (!CONFIG_FILE.exists()) {
                writeDefaultConfig();
            }

            JsonObject root = JsonParser.parseReader(new FileReader(CONFIG_FILE)).getAsJsonObject();
            if (root.has("item_rules")) {
                itemRules = Collections.unmodifiableList(parseItemRules(root.get("item_rules")));
            }
            if (root.has("item_effects")) {
                itemEffects = Collections.unmodifiableList(parseItemEffects(root.get("item_effects")));
            }
            if (root.has("codex_documents")) {
                codexDocuments = Collections.unmodifiableList(parseCodexDocuments(root.get("codex_documents")));
            }
            if (root.has("hidden_status_effects")) {
                hiddenStatusEffects = Collections.unmodifiableList(parseStringOrIdList(root.get("hidden_status_effects"), DEFAULT_HIDDEN_STATUS_EFFECTS));
            }
            if (root.has("scp_173_targets")) {
                scp173Targets = Collections.unmodifiableList(parseStringOrIdList(root.get("scp_173_targets"), DEFAULT_SCP_173_TARGETS));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        loaded = true;
    }

    private static List<String> parseItemRules(JsonElement element) {
        List<String> values = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            return new ArrayList<>(DEFAULT_ITEM_RULES);
        }
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry.isJsonPrimitive()) {
                values.add(entry.getAsString());
                continue;
            }
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject obj = entry.getAsJsonObject();
            String id = firstString(obj, "id", "item");
            String type = firstString(obj, "type", "slot");
            if (!id.isBlank() && !type.isBlank()) {
                values.add(id + "|" + type.toUpperCase(Locale.ROOT));
            }
        }
        return values;
    }

    private static List<String> parseItemEffects(JsonElement element) {
        List<String> values = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            return new ArrayList<>(DEFAULT_ITEM_EFFECTS);
        }
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry.isJsonPrimitive()) {
                values.add(entry.getAsString());
                continue;
            }
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject obj = entry.getAsJsonObject();
            String id = firstString(obj, "id", "item");
            if (id.isBlank()) {
                continue;
            }
            if (obj.has("effects") && obj.get("effects").isJsonArray()) {
                for (JsonElement effect : obj.getAsJsonArray("effects")) {
                    if (effect.isJsonPrimitive() && !effect.getAsString().isBlank()) {
                        values.add(id + "|" + effect.getAsString());
                    }
                }
            } else {
                String effect = firstString(obj, "effect");
                if (!effect.isBlank()) {
                    values.add(id + "|" + effect);
                }
            }
        }
        return values;
    }

    private static List<String> parseCodexDocuments(JsonElement element) {
        List<String> values = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            return new ArrayList<>(DEFAULT_CODEX_DOCUMENTS);
        }
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry.isJsonPrimitive()) {
                String raw = entry.getAsString();
                if (!isDebugCodexDocument(raw)) {
                    values.add(raw);
                }
                continue;
            }
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject obj = entry.getAsJsonObject();
            if (isDebugCodexDocument(obj)) {
                continue;
            }
            String id = firstString(obj, "id", "item");
            if (id.isBlank()) {
                continue;
            }
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("id", id);
            putIfPresent(fields, obj, "category");
            putIfPresent(fields, obj, "name");
            putIfPresent(fields, obj, "image");
            putIfPresent(fields, obj, "text");
            putIfPresent(fields, obj, "world_image");
            putIfPresent(fields, obj, "world_text");
            putIfPresent(fields, obj, "match_mode");
            putIfPresent(fields, obj, "codex_id");
            putIfPresent(fields, obj, "image_width");
            putIfPresent(fields, obj, "image_height");
            putIfPresent(fields, obj, "creator");
            putIfPresent(fields, obj, "timestamp");
            putIfPresent(fields, obj, "uuid");
            putIfPresent(fields, obj, "nbt_key");
            putIfPresent(fields, obj, "nbt_value");
            values.add(toKeyValueRule(fields));
        }
        return values;
    }

    private static boolean isDebugCodexDocument(JsonObject obj) {
        return isDebugCodexDocument(firstString(obj, "category") + " "
                + firstString(obj, "name") + " "
                + firstString(obj, "text"));
    }

    private static boolean isDebugCodexDocument(String raw) {
        String lower = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        return lower.contains("debug") || lower.contains("debug_paper_long");
    }

    private static List<String> parseStringOrIdList(JsonElement element, List<String> defaults) {
        List<String> values = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            return new ArrayList<>(defaults);
        }
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry.isJsonPrimitive()) {
                String value = entry.getAsString().trim();
                if (!value.isBlank()) {
                    values.add(value);
                }
            } else if (entry.isJsonObject()) {
                String id = firstString(entry.getAsJsonObject(), "id", "entity", "effect", "tag");
                if (!id.isBlank()) {
                    values.add(id);
                }
            }
        }
        return values;
    }

    private static void writeDefaultConfig() throws Exception {
        try (InputStream stream = ScpInventoryConfig.class.getClassLoader()
                .getResourceAsStream(BUNDLED_CONFIG)) {
            if (stream != null) {
                Files.copy(stream, CONFIG_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return;
            }
        }

        JsonObject root = new JsonObject();
        root.addProperty("_comment", "SCP Inventory configuration. This replaces scpinventory-common.toml with readable vertical JSON lists.");

        JsonArray rules = new JsonArray();
        for (String rule : DEFAULT_ITEM_RULES) {
            String[] parts = rule.split("\\|", 2);
            JsonObject obj = new JsonObject();
            obj.addProperty("id", parts[0]);
            obj.addProperty("type", parts.length > 1 ? parts[1] : "MISC");
            rules.add(obj);
        }
        root.add("item_rules", rules);

        JsonArray effects = new JsonArray();
        for (String rule : DEFAULT_ITEM_EFFECTS) {
            String[] parts = rule.split("\\|", 2);
            JsonObject obj = new JsonObject();
            obj.addProperty("id", parts[0]);
            JsonArray effectList = new JsonArray();
            if (parts.length > 1) {
                effectList.add(parts[1]);
            }
            obj.add("effects", effectList);
            effects.add(obj);
        }
        root.add("item_effects", effects);

        JsonArray hidden = new JsonArray();
        for (String effect : DEFAULT_HIDDEN_STATUS_EFFECTS) {
            hidden.add(effect);
        }
        root.add("hidden_status_effects", hidden);

        JsonArray scp173Targets = new JsonArray();
        for (String id : DEFAULT_SCP_173_TARGETS) {
            scp173Targets.add(id);
        }
        root.add("scp_173_targets", scp173Targets);

        JsonArray codex = new JsonArray();
        for (String rule : DEFAULT_CODEX_DOCUMENTS) {
            codex.add(keyValueRuleToJson(rule));
        }
        root.add("codex_documents", codex);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(root, writer);
        }
    }

    private static JsonObject keyValueRuleToJson(String raw) {
        JsonObject obj = new JsonObject();
        for (String part : raw.split(";|\\r?\\n")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && !pair[0].isBlank()) {
                String key = pair[0].trim();
                String value = pair[1].trim();
                if ("image_width".equals(key) || "image_height".equals(key)) {
                    try {
                        obj.addProperty(key, Integer.parseInt(value));
                    } catch (NumberFormatException ignored) {
                        obj.addProperty(key, value);
                    }
                } else {
                    obj.addProperty(key, value);
                }
            }
        }
        return obj;
    }

    private static void putIfPresent(Map<String, String> fields, JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            fields.put(key, obj.get(key).getAsString());
        }
    }

    private static String toKeyValueRule(Map<String, String> fields) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    private static String firstString(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsString().trim();
            }
        }
        return "";
    }

    public static final class JsonListValue {
        private final Supplier<List<String>> supplier;

        private JsonListValue(Supplier<List<String>> supplier) {
            this.supplier = supplier;
        }

        public List<String> get() {
            return supplier.get();
        }
    }
}
