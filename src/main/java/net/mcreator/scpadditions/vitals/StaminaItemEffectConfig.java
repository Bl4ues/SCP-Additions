package net.mcreator.scpadditions.vitals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads the standalone Inventory's item_effects section without owning the file. */
public final class StaminaItemEffectConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("scpinventory").resolve("scpinventory.json");
    private static final String DEFAULT_BLOCKER = "minecraft:leather";

    private StaminaItemEffectConfig() {
    }

    public static synchronized void load() {
        List<ResourceLocation> blockers = new ArrayList<>();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject root = new JsonObject();
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(
                        CONFIG_PATH, StandardCharsets.UTF_8)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (parsed.isJsonObject()) {
                        root = parsed.getAsJsonObject();
                    }
                }
            }

            if (root.has("item_effects")
                    && root.get("item_effects").isJsonArray()) {
                parseEffects(root.getAsJsonArray("item_effects"), blockers);
            } else {
                ResourceLocation defaultId = ResourceLocation.tryParse(DEFAULT_BLOCKER);
                if (defaultId != null) {
                    blockers.add(defaultId);
                }

                JsonObject rule = new JsonObject();
                rule.addProperty("id", DEFAULT_BLOCKER);
                JsonArray effects = new JsonArray();
                effects.add("NO_STAMINA");
                rule.add("effects", effects);
                JsonArray rules = new JsonArray();
                rules.add(rule);
                root.add("item_effects", rules);

                try (Writer writer = Files.newBufferedWriter(CONFIG_PATH,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)) {
                    GSON.toJson(root, writer);
                }
            }
        } catch (Exception exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Failed to load stamina item effects from {}", CONFIG_PATH,
                    exception);
            ResourceLocation defaultId = ResourceLocation.tryParse(DEFAULT_BLOCKER);
            if (defaultId != null) {
                blockers.add(defaultId);
            }
        }

        StaminaBlockerAccess.replaceConfiguredItems(blockers);
        ScpAdditionsMod.LOGGER.info("Loaded {} NO_STAMINA item rule(s)",
                blockers.size());
    }

    private static void parseEffects(JsonArray entries,
            List<ResourceLocation> blockers) {
        for (JsonElement entry : entries) {
            if (entry.isJsonPrimitive()) {
                parsePrimitive(entry.getAsString(), blockers);
            } else if (entry.isJsonObject()) {
                parseObject(entry.getAsJsonObject(), blockers);
            }
        }
    }

    private static void parsePrimitive(String raw,
            List<ResourceLocation> blockers) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String[] parts = raw.split("\\|", 2);
        if (parts.length == 2 && isNoStamina(parts[1])) {
            addId(parts[0], blockers);
        }
    }

    private static void parseObject(JsonObject object,
            List<ResourceLocation> blockers) {
        String id = firstString(object, "id", "item");
        if (id.isBlank()) {
            return;
        }

        if (object.has("effects") && object.get("effects").isJsonArray()) {
            for (JsonElement effect : object.getAsJsonArray("effects")) {
                if (effect.isJsonPrimitive() && isNoStamina(effect.getAsString())) {
                    addId(id, blockers);
                    return;
                }
            }
        } else if (object.has("effect") && object.get("effect").isJsonPrimitive()
                && isNoStamina(object.get("effect").getAsString())) {
            addId(id, blockers);
        }
    }

    private static void addId(String raw,
            List<ResourceLocation> blockers) {
        ResourceLocation id = ResourceLocation.tryParse(raw.trim());
        if (id != null && !blockers.contains(id)) {
            blockers.add(id);
        }
    }

    private static boolean isNoStamina(String raw) {
        if (raw == null) {
            return false;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        return normalized.equals("NO_STAMINA")
                || normalized.equals("ZERO_STAMINA")
                || normalized.equals("DISABLE_STAMINA")
                || normalized.equals("STAMINA_DISABLED");
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
