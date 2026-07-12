package com.bl4ues.scpinventory.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Shared readable configuration used by inventory classification, vitals and
 * later interaction/codex modules. Missing sections are added without replacing
 * sections already written by another integrated subsystem.
 */
public final class ScpInventoryConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("scpinventory").resolve("scpinventory.json");

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
    private static final List<String> DEFAULT_ITEM_EFFECTS =
            List.of("minecraft:leather|NO_STAMINA");
    private static final List<String> DEFAULT_HIDDEN_STATUS_EFFECTS =
            List.of("minecraft:bad_omen");

    private static volatile boolean loaded;
    private static List<String> itemRules = DEFAULT_ITEM_RULES;
    private static List<String> itemEffects = DEFAULT_ITEM_EFFECTS;
    private static List<String> hiddenStatusEffects = DEFAULT_HIDDEN_STATUS_EFFECTS;
    private static List<String> codexDocuments = List.of();

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

    public static List<String> hiddenStatusEffects() {
        ensureLoaded();
        return hiddenStatusEffects;
    }

    public static List<String> codexDocuments() {
        ensureLoaded();
        return codexDocuments;
    }

    public static synchronized void reload() {
        loaded = false;
        load();
    }

    public static synchronized void load() {
        itemRules = DEFAULT_ITEM_RULES;
        itemEffects = DEFAULT_ITEM_EFFECTS;
        hiddenStatusEffects = DEFAULT_HIDDEN_STATUS_EFFECTS;
        codexDocuments = List.of();

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject root = new JsonObject();
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH,
                        StandardCharsets.UTF_8)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (parsed.isJsonObject()) root = parsed.getAsJsonObject();
                }
            }

            boolean changed = false;
            if (root.has("item_rules") && root.get("item_rules").isJsonArray()) {
                itemRules = Collections.unmodifiableList(
                        parseTypedRules(root.getAsJsonArray("item_rules"), DEFAULT_ITEM_RULES));
            } else {
                root.add("item_rules", typedRulesToJson(DEFAULT_ITEM_RULES));
                changed = true;
            }

            if (root.has("item_effects") && root.get("item_effects").isJsonArray()) {
                itemEffects = Collections.unmodifiableList(
                        parseEffectRules(root.getAsJsonArray("item_effects")));
            } else {
                root.add("item_effects", effectRulesToJson(DEFAULT_ITEM_EFFECTS));
                changed = true;
            }

            if (root.has("hidden_status_effects")
                    && root.get("hidden_status_effects").isJsonArray()) {
                hiddenStatusEffects = Collections.unmodifiableList(
                        parseSimpleList(root.getAsJsonArray("hidden_status_effects")));
            } else {
                root.add("hidden_status_effects",
                        simpleListToJson(DEFAULT_HIDDEN_STATUS_EFFECTS));
                changed = true;
            }

            if (root.has("codex_documents")
                    && root.get("codex_documents").isJsonArray()) {
                codexDocuments = Collections.unmodifiableList(
                        parseCodex(root.getAsJsonArray("codex_documents")));
            } else {
                root.add("codex_documents", new JsonArray());
                changed = true;
            }

            if (!root.has("_comment")) {
                root.addProperty("_comment",
                        "SCP Inventory classification, item effects and codex configuration.");
                changed = true;
            }

            if (changed || !Files.exists(CONFIG_PATH)) {
                try (Writer writer = Files.newBufferedWriter(CONFIG_PATH,
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)) {
                    GSON.toJson(root, writer);
                }
            }
        } catch (Exception exception) {
            ScpAdditionsMod.LOGGER.error("Failed to load SCP Inventory config from {}",
                    CONFIG_PATH, exception);
        }

        loaded = true;
        ScpAdditionsMod.LOGGER.info("Loaded {} SCP Inventory item rule(s)",
                itemRules.size());
    }

    private static void ensureLoaded() {
        if (!loaded) load();
    }

    private static List<String> parseTypedRules(JsonArray array,
            List<String> defaults) {
        List<String> result = new ArrayList<>();
        for (JsonElement entry : array) {
            if (entry.isJsonPrimitive()) {
                String raw = entry.getAsString().trim();
                if (!raw.isBlank()) result.add(raw);
            } else if (entry.isJsonObject()) {
                JsonObject object = entry.getAsJsonObject();
                String id = firstString(object, "id", "item");
                String type = firstString(object, "type", "slot");
                if (!id.isBlank() && !type.isBlank()) {
                    result.add(id + "|" + type.toUpperCase(Locale.ROOT));
                }
            }
        }
        return result.isEmpty() ? new ArrayList<>(defaults) : result;
    }

    private static List<String> parseEffectRules(JsonArray array) {
        List<String> result = new ArrayList<>();
        for (JsonElement entry : array) {
            if (entry.isJsonPrimitive()) {
                String raw = entry.getAsString().trim();
                if (!raw.isBlank()) result.add(raw);
            } else if (entry.isJsonObject()) {
                JsonObject object = entry.getAsJsonObject();
                String id = firstString(object, "id", "item");
                if (id.isBlank()) continue;
                if (object.has("effects") && object.get("effects").isJsonArray()) {
                    for (JsonElement effect : object.getAsJsonArray("effects")) {
                        if (effect.isJsonPrimitive() && !effect.getAsString().isBlank()) {
                            result.add(id + "|" + effect.getAsString().trim());
                        }
                    }
                } else {
                    String effect = firstString(object, "effect");
                    if (!effect.isBlank()) result.add(id + "|" + effect);
                }
            }
        }
        return result;
    }

    private static List<String> parseSimpleList(JsonArray array) {
        List<String> result = new ArrayList<>();
        for (JsonElement entry : array) {
            if (entry.isJsonPrimitive() && !entry.getAsString().isBlank()) {
                result.add(entry.getAsString().trim());
            }
        }
        return result;
    }

    private static List<String> parseCodex(JsonArray array) {
        List<String> result = new ArrayList<>();
        for (JsonElement entry : array) {
            if (entry.isJsonPrimitive()) {
                String raw = entry.getAsString().trim();
                if (!raw.isBlank()) result.add(raw);
            } else if (entry.isJsonObject()) {
                JsonObject object = entry.getAsJsonObject();
                String id = firstString(object, "id", "item");
                if (!id.isBlank()) result.add("id=" + id);
            }
        }
        return result;
    }

    private static JsonArray typedRulesToJson(List<String> rules) {
        JsonArray array = new JsonArray();
        for (String raw : rules) {
            String[] parts = raw.split("\\|", 2);
            JsonObject object = new JsonObject();
            object.addProperty("id", parts[0]);
            object.addProperty("type", parts.length > 1 ? parts[1] : "MISC");
            array.add(object);
        }
        return array;
    }

    private static JsonArray effectRulesToJson(List<String> rules) {
        JsonArray array = new JsonArray();
        for (String raw : rules) {
            String[] parts = raw.split("\\|", 2);
            JsonObject object = new JsonObject();
            object.addProperty("id", parts[0]);
            JsonArray effects = new JsonArray();
            if (parts.length > 1) effects.add(parts[1]);
            object.add("effects", effects);
            array.add(object);
        }
        return array;
    }

    private static JsonArray simpleListToJson(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    private static String firstString(JsonObject object, String... keys) {
        for (String key : keys) {
            if (object.has(key) && object.get(key).isJsonPrimitive()) {
                return object.get(key).getAsString().trim();
            }
        }
        return "";
    }
}
