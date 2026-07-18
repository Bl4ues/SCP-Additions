package net.mcreator.scpadditions.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.io.File;
import java.io.FileReader;

/** Adds the canonical SCP-714 category to existing configs without overriding users. */
public final class Scp714ConfigBootstrap {
    private static final File CONFIG_FILE =
            new File("config/scpinventory/scpinventory.json");
    private static final String ITEM_ID = "scp_additions:scp_714";
    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private Scp714ConfigBootstrap() {
    }

    public static void ensureAccessoryRule() {
        if (!CONFIG_FILE.isFile()) {
            // ScpInventoryConfig will create the updated bundled default.
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return;
            }

            JsonObject root = parsed.getAsJsonObject();
            JsonArray rules;
            if (root.has("item_rules") && root.get("item_rules").isJsonArray()) {
                rules = root.getAsJsonArray("item_rules");
            } else {
                rules = new JsonArray();
                root.add("item_rules", rules);
            }

            if (containsRuleFor(rules, ITEM_ID)) {
                // Explicit JSON rules remain authoritative, including a deliberate
                // user override to a category other than ACCESSORYHAND.
                return;
            }

            JsonObject rule = new JsonObject();
            rule.addProperty("id", ITEM_ID);
            rule.addProperty("type", "ACCESSORYHAND");
            rules.add(rule);
            ConfigFilePersistence.writeWithBackup(CONFIG_FILE.toPath(),
                    GSON.toJson(root) + System.lineSeparator());
        } catch (Exception exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not append the SCP-714 accessory rule to the existing SCP Inventory config",
                    exception);
        }
    }

    private static boolean containsRuleFor(JsonArray rules, String itemId) {
        for (JsonElement entry : rules) {
            if (entry == null) {
                continue;
            }
            if (entry.isJsonPrimitive()) {
                String[] parts = entry.getAsString().split("\\|", 2);
                if (parts.length > 0 && itemId.equals(parts[0].trim())) {
                    return true;
                }
                continue;
            }
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject object = entry.getAsJsonObject();
            String id = firstString(object, "id", "item");
            if (itemId.equals(id)) {
                return true;
            }
        }
        return false;
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
