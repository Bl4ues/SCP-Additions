package net.mcreator.scpadditions.config.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.loading.FMLPaths;
import net.mcreator.scpadditions.config.ConfigFilePersistence;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.data.Scp294DrinkManager;
import net.mcreator.scpadditions.data.Scp914RecipeManager;
import net.mcreator.scpadditions.entity.Scp173TargetConfig;
import net.mcreator.scpadditions.vitals.StaminaItemEffectConfig;
import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import com.bl4ues.scpinventory.context.ContextInteractionRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Server-authoritative storage, validation, backup and reload service for the native configuration center. */
public final class ConfigCenterService {
    public static final String MODULES = "modules";
    public static final String INVENTORY = "inventory";
    public static final String CONTEXT = "context";
    public static final String DRINKS = "drinks";
    public static final String RECIPE_MAIN = "recipe:main";
    public static final String RECIPE_PREFIX = "recipe:";
    public static final int MAX_PAYLOAD_LENGTH = 2_000_000;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_ROOT = FMLPaths.CONFIGDIR.get();
    private static final Path SCP_ROOT = CONFIG_ROOT.resolve("scpadditions");
    private static final Path INVENTORY_ROOT = CONFIG_ROOT.resolve("scpinventory");
    private static final Path RECIPE_FRAGMENTS = SCP_ROOT.resolve("914recipes.d");
    private static final Pattern SAFE_FRAGMENT = Pattern.compile("[A-Za-z0-9._-]+\\.json");
    private static final Set<String> SETTINGS = Set.of("rough", "coarse", "one_to_one", "fine", "very_fine");

    private ConfigCenterService() {
    }

    public static boolean canEdit(ServerPlayer player) {
        return player != null && (player.getServer() == null || !player.getServer().isDedicatedServer()
                || player.hasPermissions(2));
    }

    public static JsonObject snapshot() throws IOException {
        JsonObject snapshot = new JsonObject();
        snapshot.add(MODULES, readObject(pathFor(MODULES), defaultModules()));
        snapshot.add(INVENTORY, readObject(pathFor(INVENTORY), defaultInventory()));
        snapshot.add(CONTEXT, readObject(pathFor(CONTEXT), defaultContext()));
        snapshot.add(DRINKS, readObject(pathFor(DRINKS), defaultDrinks()));
        snapshot.add(RECIPE_MAIN, readObject(pathFor(RECIPE_MAIN), defaultRecipes()));
        for (Path fragment : recipeFragments()) {
            snapshot.add(RECIPE_PREFIX + fragment.getFileName(), readObject(fragment, defaultRecipeFragment()));
        }
        return snapshot;
    }

    public static SaveResult saveBatch(ServerPlayer player, String payload) {
        if (!canEdit(player)) return SaveResult.failure("Operator permission level 2 is required.");
        if (payload == null || payload.length() > MAX_PAYLOAD_LENGTH) return SaveResult.failure("Configuration payload is too large.");

        JsonObject changes;
        try {
            JsonElement parsed = JsonParser.parseString(payload);
            if (!parsed.isJsonObject()) return SaveResult.failure("Configuration payload must be a JSON object.");
            changes = parsed.getAsJsonObject();
        } catch (Exception exception) {
            return SaveResult.failure("Invalid configuration payload: " + readable(exception));
        }
        if (changes.size() == 0) return SaveResult.failure("No configuration changes were provided.");

        Map<String, JsonObject> normalized = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : changes.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                errors.add(entry.getKey() + " must be a JSON object");
                continue;
            }
            String key = entry.getKey();
            if (pathFor(key) == null) {
                errors.add("Unsupported configuration category: " + key);
                continue;
            }
            JsonObject root = entry.getValue().getAsJsonObject().deepCopy();
            Validation validation = validate(key, root);
            errors.addAll(validation.errors());
            warnings.addAll(validation.warnings());
            normalized.put(key, root);
        }
        if (!errors.isEmpty()) return SaveResult.failure(String.join("\n", errors), warnings);

        Map<Path, byte[]> previous = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, JsonObject> entry : normalized.entrySet()) {
                Path path = pathFor(entry.getKey());
                previous.put(path, Files.exists(path) ? Files.readAllBytes(path) : null);
                ConfigFilePersistence.writeWithBackup(path, GSON.toJson(entry.getValue()) + System.lineSeparator());
            }
            reloadAll();
            return SaveResult.success("Configuration saved and reloaded.", snapshot(), warnings);
        } catch (Exception exception) {
            rollback(previous);
            try { reloadAll(); } catch (Exception ignored) { }
            return SaveResult.failure("Configuration save failed and was rolled back: " + readable(exception), warnings);
        }
    }

    public static SaveResult deleteRecipeFragment(ServerPlayer player, String fileName) {
        if (!canEdit(player)) return SaveResult.failure("Operator permission level 2 is required.");
        if (fileName == null || !SAFE_FRAGMENT.matcher(fileName).matches() || "main".equals(fileName)) {
            return SaveResult.failure("Invalid recipe fragment name.");
        }
        Path path = RECIPE_FRAGMENTS.resolve(fileName).normalize();
        if (!path.startsWith(RECIPE_FRAGMENTS)) return SaveResult.failure("Invalid recipe fragment path.");
        try {
            if (Files.exists(path)) {
                Path backup = path.resolveSibling(path.getFileName() + ".bak");
                Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
                Files.delete(path);
            }
            reloadAll();
            return SaveResult.success("Recipe fragment deleted.", snapshot(), List.of());
        } catch (Exception exception) {
            return SaveResult.failure("Could not delete recipe fragment: " + readable(exception));
        }
    }

    private static void rollback(Map<Path, byte[]> previous) {
        for (Map.Entry<Path, byte[]> entry : previous.entrySet()) {
            try {
                if (entry.getValue() == null) Files.deleteIfExists(entry.getKey());
                else {
                    Files.createDirectories(entry.getKey().getParent());
                    Files.write(entry.getKey(), entry.getValue());
                }
            } catch (Exception ignored) { }
        }
    }

    private static void reloadAll() {
        ScpAdditionsModulesConfig.load();
        ScpInventoryConfig.reloadFromDisk();
        ContextInteractionRegistry.reloadFromDisk();
        Scp173TargetConfig.load();
        StaminaItemEffectConfig.load();
        Scp294DrinkManager.loadFromConfig();
        Scp914RecipeManager.loadFromConfig();
    }

    private static Path pathFor(String key) {
        if (MODULES.equals(key)) return SCP_ROOT.resolve("modules.json");
        if (INVENTORY.equals(key)) return INVENTORY_ROOT.resolve("scpinventory.json");
        if (CONTEXT.equals(key)) return INVENTORY_ROOT.resolve("context_interactions.json");
        if (DRINKS.equals(key)) return SCP_ROOT.resolve("294drinks.json");
        if (RECIPE_MAIN.equals(key)) return SCP_ROOT.resolve("914recipes.json");
        if (key != null && key.startsWith(RECIPE_PREFIX)) {
            String name = key.substring(RECIPE_PREFIX.length());
            if (!"main".equals(name) && SAFE_FRAGMENT.matcher(name).matches()) {
                return RECIPE_FRAGMENTS.resolve(name).normalize();
            }
        }
        return null;
    }

    private static JsonObject readObject(Path path, JsonObject fallback) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.notExists(path)) return fallback.deepCopy();

        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IOException("Configuration root must be a JSON object: " + path);
            }
            return parsed.getAsJsonObject();
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("Invalid JSON in " + path + ": " + readable(exception), exception);
        }
    }

    private static Validation validate(String key, JsonObject root) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (MODULES.equals(key)) validateModules(root, errors);
        else if (INVENTORY.equals(key)) validateInventory(root, errors, warnings);
        else if (CONTEXT.equals(key)) validateContext(root, errors, warnings);
        else if (DRINKS.equals(key)) validateDrinks(root, errors, warnings);
        else if (key.startsWith(RECIPE_PREFIX)) validateRecipes(root, errors, warnings, RECIPE_MAIN.equals(key));
        else errors.add("Unsupported configuration category");
        return new Validation(List.copyOf(errors), List.copyOf(warnings));
    }

    private static void validateModules(JsonObject root, List<String> errors) {
        for (String group : List.of("inventory", "interactions", "hud", "vitals", "blink", "scp_173", "debug")) {
            if (root.has(group) && !root.get(group).isJsonObject()) errors.add(group + " must be an object");
        }
        checkBoolean(root, "inventory", "enabled", errors);
        checkBoolean(root, "inventory", "remember_ui_state", errors);
        checkBoolean(root, "interactions", "enabled", errors);
        checkBoolean(root, "interactions", "disable_in_creative", errors);
        checkBoolean(root, "hud", "enabled", errors);
        checkBoolean(root, "vitals", "custom_health_enabled", errors);
        checkBoolean(root, "vitals", "stamina_enabled", errors);
        checkBoolean(root, "vitals", "horror_movement_enabled", errors);
        checkBoolean(root, "blink", "enabled", errors);
        checkBoolean(root, "scp_173", "enabled", errors);
        checkBoolean(root, "scp_173", "natural_spawn_enabled", errors);
        checkBoolean(root, "debug", "show_scp_079_energy_hud", errors);
    }

    private static void checkBoolean(JsonObject root, String group, String key, List<String> errors) {
        if (!root.has(group) || !root.get(group).isJsonObject()) return;
        JsonObject object = root.getAsJsonObject(group);
        if (object.has(key) && !object.get(key).isJsonPrimitive()) errors.add(group + "." + key + " must be a boolean");
        if (object.has(key)) {
            try { object.get(key).getAsBoolean(); } catch (Exception ex) { errors.add(group + "." + key + " must be a boolean"); }
        }
    }

    private static void validateInventory(JsonObject root, List<String> errors, List<String> warnings) {
        for (String key : List.of("item_rules", "item_effects", "hidden_status_effects", "codex_documents", "scp_173_targets")) {
            requireArray(root, key, errors);
        }
        validateObjectIds(root, "item_rules", "id", errors, warnings, true);
        validateObjectIds(root, "item_effects", "id", errors, warnings, true);
        validateObjectIds(root, "codex_documents", "id", errors, warnings, true);
        if (root.has("codex_documents") && root.get("codex_documents").isJsonArray()) {
            int index = 0;
            for (JsonElement element : root.getAsJsonArray("codex_documents")) {
                if (element.isJsonObject()) {
                    JsonObject document = element.getAsJsonObject();
                    String mode = string(document, "match_mode");
                    if (!mode.isBlank() && !"item".equalsIgnoreCase(mode) && !"unique".equalsIgnoreCase(mode)) {
                        errors.add("codex_documents[" + index + "].match_mode must be item or unique");
                    }
                    if ("unique".equalsIgnoreCase(mode) && string(document, "unique_id").isBlank()) {
                        errors.add("codex_documents[" + index + "].unique_id is required for unique matching");
                    }
                }
                index++;
            }
        }
        validateObjectIds(root, "scp_173_targets", "entity", errors, warnings, true);
        if (root.has("hidden_status_effects") && root.get("hidden_status_effects").isJsonArray()) {
            int index = 0;
            for (JsonElement element : root.getAsJsonArray("hidden_status_effects")) {
                String id = element.isJsonPrimitive() ? element.getAsString() : element.isJsonObject() ? string(element.getAsJsonObject(), "id") : "";
                validateId(id, "hidden_status_effects[" + index + "]", errors, warnings, true);
                index++;
            }
        }
    }

    private static void validateContext(JsonObject root, List<String> errors, List<String> warnings) {
        if (!requireArray(root, "interactions", errors)) return;
        int index = 0;
        for (JsonElement element : root.getAsJsonArray("interactions")) {
            String label = "interactions[" + index + "]";
            if (!element.isJsonObject()) { errors.add(label + " must be an object"); index++; continue; }
            JsonObject interaction = element.getAsJsonObject();
            String target = string(interaction, "target");
            String targetType = string(interaction, "target_type").toLowerCase(Locale.ROOT);
            if (target.isBlank()) errors.add(label + ".target is required");
            else if (!target.startsWith("#") && !"item".equals(targetType)) validateId(target, label + ".target", errors, warnings, true);
            if (interaction.has("icon")) validateId(string(interaction, "icon"), label + ".icon", errors, warnings, true);
            index++;
        }
    }

    private static void validateDrinks(JsonObject root, List<String> errors, List<String> warnings) {
        if (!requireArray(root, "drinks", errors)) return;
        int index = 0;
        for (JsonElement element : root.getAsJsonArray("drinks")) {
            String label = "drinks[" + index + "]";
            if (!element.isJsonObject()) { errors.add(label + " must be an object"); index++; continue; }
            JsonObject drink = element.getAsJsonObject();
            validateId(string(drink, "id"), label + ".id", errors, warnings, false);
            if (drink.has("aliases") && !drink.get("aliases").isJsonArray()) errors.add(label + ".aliases must be an array");
            if (drink.has("result")) {
                if (!drink.get("result").isJsonObject()) errors.add(label + ".result must be an object");
                else validateId(string(drink.getAsJsonObject("result"), "item"), label + ".result.item", errors, warnings, false);
            }
            if (drink.has("cup_color")) {
                String color = string(drink, "cup_color");
                if (!color.matches("#[0-9a-fA-F]{6}")) errors.add(label + ".cup_color must use #RRGGBB");
            }
            index++;
        }
    }

    private static void validateRecipes(JsonObject root, List<String> errors, List<String> warnings, boolean main) {
        if (main && root.has("machine") && !root.get("machine").isJsonObject()) errors.add("machine must be an object");
        if (!requireArray(root, "recipes", errors)) return;
        int index = 0;
        for (JsonElement element : root.getAsJsonArray("recipes")) {
            String label = "recipes[" + index + "]";
            if (!element.isJsonObject()) { errors.add(label + " must be an object"); index++; continue; }
            JsonObject recipe = element.getAsJsonObject();
            validateId(string(recipe, "id"), label + ".id", errors, warnings, false);
            if (!SETTINGS.contains(string(recipe, "setting").toLowerCase(Locale.ROOT))) errors.add(label + ".setting is invalid");
            boolean hasInput = nonEmptyArray(recipe, "item_inputs") || nonEmptyArray(recipe, "entity_inputs") || recipe.has("input");
            boolean hasOutput = nonEmptyArray(recipe, "item_outputs") || nonEmptyArray(recipe, "weighted_item_outputs") || nonEmptyArray(recipe, "entity_outputs") || recipe.has("output");
            if (!hasInput) errors.add(label + " needs at least one intake item or entity");
            if (!hasOutput) errors.add(label + " needs at least one output item or entity");
            validateRecipeItemArray(recipe, "item_inputs", "item", label, errors, warnings);
            validateRecipeItemArray(recipe, "item_outputs", "item", label, errors, warnings);
            validateRecipeItemArray(recipe, "weighted_item_outputs", "item", label, errors, warnings);
            validateRecipeItemArray(recipe, "entity_inputs", "entity", label, errors, warnings);
            validateRecipeItemArray(recipe, "entity_outputs", "entity", label, errors, warnings);
            if (recipe.has("chance")) {
                try {
                    double chance = recipe.get("chance").getAsDouble();
                    if (chance < 0 || chance > 1) errors.add(label + ".chance must be between 0 and 1");
                } catch (Exception ex) { errors.add(label + ".chance must be a number"); }
            }
            index++;
        }
    }

    private static void validateRecipeItemArray(JsonObject root, String arrayKey, String idKey, String label,
                                                 List<String> errors, List<String> warnings) {
        if (!root.has(arrayKey)) return;
        if (!root.get(arrayKey).isJsonArray()) { errors.add(label + "." + arrayKey + " must be an array"); return; }
        int index = 0;
        for (JsonElement element : root.getAsJsonArray(arrayKey)) {
            if (!element.isJsonObject()) errors.add(label + "." + arrayKey + "[" + index + "] must be an object");
            else {
                JsonObject value = element.getAsJsonObject();
                validateId(string(value, idKey), label + "." + arrayKey + "[" + index + "]." + idKey, errors, warnings, false);
                if (value.has("count")) {
                    try { if (value.get("count").getAsInt() < 1) errors.add(label + "." + arrayKey + "[" + index + "].count must be at least 1"); }
                    catch (Exception ex) { errors.add(label + "." + arrayKey + "[" + index + "].count must be an integer"); }
                }
                if (value.has("weight")) {
                    try { if (value.get("weight").getAsInt() < 1) errors.add(label + "." + arrayKey + "[" + index + "].weight must be at least 1"); }
                    catch (Exception ex) { errors.add(label + "." + arrayKey + "[" + index + "].weight must be an integer"); }
                }
            }
            index++;
        }
    }

    private static void validateObjectIds(JsonObject root, String arrayKey, String idKey, List<String> errors,
                                          List<String> warnings, boolean allowMissing) {
        if (!root.has(arrayKey) || !root.get(arrayKey).isJsonArray()) return;
        int index = 0;
        for (JsonElement element : root.getAsJsonArray(arrayKey)) {
            String id = element.isJsonObject() ? string(element.getAsJsonObject(), idKey) : element.isJsonPrimitive() ? element.getAsString().split("[|;]", 2)[0] : "";
            validateId(id, arrayKey + "[" + index + "]." + idKey, errors, warnings, allowMissing);
            index++;
        }
    }

    private static void validateSimpleIds(JsonObject root, String arrayKey, List<String> errors,
                                          List<String> warnings, boolean allowTag) {
        if (!root.has(arrayKey) || !root.get(arrayKey).isJsonArray()) return;
        int index = 0;
        for (JsonElement element : root.getAsJsonArray(arrayKey)) {
            String id = element.isJsonPrimitive() ? element.getAsString() : element.isJsonObject() ? string(element.getAsJsonObject(), "id") : "";
            validateId(id, arrayKey + "[" + index + "]", errors, warnings, allowTag);
            index++;
        }
    }

    private static void validateId(String id, String label, List<String> errors, List<String> warnings, boolean allowMissing) {
        if (id == null || id.isBlank()) { errors.add(label + " is required"); return; }
        String normalized = id.startsWith("#") ? id.substring(1) : id;
        ResourceLocation location = ResourceLocation.tryParse(normalized);
        if (location == null) { errors.add(label + " is not a valid namespaced ID: " + id); return; }
        if (allowMissing) return;
        boolean present = ForgeRegistries.ITEMS.containsKey(location) || ForgeRegistries.ENTITY_TYPES.containsKey(location)
                || ForgeRegistries.BLOCKS.containsKey(location) || ForgeRegistries.MOB_EFFECTS.containsKey(location);
        if (!present) warnings.add(label + " references an unavailable registry entry and may be skipped: " + id);
    }

    private static boolean requireArray(JsonObject root, String key, List<String> errors) {
        if (!root.has(key)) { errors.add(key + " is required"); return false; }
        if (!root.get(key).isJsonArray()) { errors.add(key + " must be an array"); return false; }
        return true;
    }

    private static boolean nonEmptyArray(JsonObject root, String key) {
        return root.has(key) && root.get(key).isJsonArray() && root.getAsJsonArray(key).size() > 0;
    }

    private static String string(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonPrimitive()) return "";
        try { return root.get(key).getAsString().trim(); } catch (Exception ignored) { return ""; }
    }

    private static JsonObject defaultModules() {
        JsonObject root = new JsonObject();
        root.add("inventory", bools("enabled", true, "remember_ui_state", true));
        root.add("interactions", bools("enabled", true, "disable_in_creative", false));
        root.add("hud", bools("enabled", true));
        root.add("vitals", bools("custom_health_enabled", true, "stamina_enabled", true,
                "horror_movement_enabled", true));
        root.add("blink", bools("enabled", true));
        root.add("scp_173", bools("enabled", true, "natural_spawn_enabled", true));
        root.add("debug", bools("show_scp_079_energy_hud", false));
        return root;
    }

    private static JsonObject defaultInventory() {
        JsonObject root = new JsonObject();
        root.add("item_rules", new JsonArray());
        root.add("item_effects", new JsonArray());
        root.add("hidden_status_effects", new JsonArray());
        root.add("codex_documents", new JsonArray());
        root.add("scp_173_targets", new JsonArray());
        return root;
    }

    private static JsonObject defaultContext() {
        JsonObject root = new JsonObject();
        root.add("interactions", new JsonArray());
        return root;
    }

    private static JsonObject defaultDrinks() {
        JsonObject root = new JsonObject();
        root.add("drinks", new JsonArray());
        return root;
    }

    private static JsonObject defaultRecipes() {
        JsonObject root = new JsonObject();
        root.add("machine", new JsonObject());
        root.add("recipes", new JsonArray());
        return root;
    }

    private static JsonObject defaultRecipeFragment() {
        JsonObject root = new JsonObject();
        root.add("recipes", new JsonArray());
        return root;
    }

    private static JsonObject bools(Object... values) {
        JsonObject object = new JsonObject();
        for (int index = 0; index + 1 < values.length; index += 2) {
            object.addProperty(String.valueOf(values[index]), (Boolean) values[index + 1]);
        }
        return object;
    }

    private static List<Path> recipeFragments() throws IOException {
        Files.createDirectories(RECIPE_FRAGMENTS);
        try (var stream = Files.list(RECIPE_FRAGMENTS)) {
            return stream.filter(path -> SAFE_FRAGMENT.matcher(path.getFileName().toString()).matches())
                    .sorted().toList();
        }
    }

    private static void rollback(Path path, byte[] bytes) throws IOException {
        if (bytes == null) Files.deleteIfExists(path);
        else Files.write(path, bytes);
    }

    private static String readable(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    public record Validation(List<String> errors, List<String> warnings) {
    }

    public record SaveResult(boolean success, String message, JsonObject snapshot, List<String> warnings) {
        public static SaveResult success(String message, JsonObject snapshot, List<String> warnings) {
            return new SaveResult(true, message, snapshot, warnings == null ? List.of() : List.copyOf(warnings));
        }

        public static SaveResult failure(String message) {
            return failure(message, List.of());
        }

        public static SaveResult failure(String message, List<String> warnings) {
            return new SaveResult(false, message, new JsonObject(), warnings == null ? List.of() : List.copyOf(warnings));
        }
    }
}
