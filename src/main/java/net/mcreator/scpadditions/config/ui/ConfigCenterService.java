package net.mcreator.scpadditions.config.ui;

import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import com.bl4ues.scpinventory.context.ContextInteractionRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ConfigFilePersistence;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.data.Scp294DrinkManager;
import net.mcreator.scpadditions.data.Scp914RecipeManager;
import net.mcreator.scpadditions.entity.Scp173TargetConfig;
import net.mcreator.scpadditions.vitals.StaminaItemEffectConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Server-owned persistence and validation for the in-game configuration center. */
public final class ConfigCenterService {
    public static final int REQUIRED_PERMISSION_LEVEL = 2;
    public static final int MAX_PAYLOAD_LENGTH = 2_000_000;

    public static final String MODULES = "modules";
    public static final String INVENTORY = "inventory";
    public static final String CONTEXT = "context";
    public static final String DRINKS = "294";
    public static final String RECIPE_MAIN = "914/main";
    public static final String RECIPE_PREFIX = "914/";
    public static final String EDITOR_FRAGMENT = "in_game_editor.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SCP_ROOT = FMLPaths.CONFIGDIR.get().resolve("scpadditions");
    private static final Path INVENTORY_ROOT = FMLPaths.CONFIGDIR.get().resolve("scpinventory");
    private static final Path RECIPE_FRAGMENTS = SCP_ROOT.resolve("914recipes.d");
    private static final Pattern SAFE_FRAGMENT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,95}\\.json");
    private static final Set<String> SETTINGS = Set.of("rough", "coarse", "1_to_1", "fine", "very_fine");

    private ConfigCenterService() {
    }

    public static boolean canEdit(ServerPlayer player) {
        if (player == null || player.isSpectator()) return false;
        if (player.hasPermissions(REQUIRED_PERMISSION_LEVEL)) return true;
        return player.getServer() != null && player.getServer().isSingleplayerOwner(player.getGameProfile());
    }

    public static boolean requireEdit(ServerPlayer player) {
        if (canEdit(player)) return true;
        if (player != null) player.sendSystemMessage(Component.literal(
                "SCP Additions configuration requires operator permission level 2.")
                .withStyle(ChatFormatting.RED));
        return false;
    }

    public static JsonObject snapshot() throws IOException {
        JsonObject files = new JsonObject();
        files.add(MODULES, readObject(pathFor(MODULES), defaultModules()));
        files.add(INVENTORY, readObject(pathFor(INVENTORY), defaultInventory()));
        files.add(CONTEXT, readObject(pathFor(CONTEXT), defaultContext()));
        files.add(DRINKS, readObject(pathFor(DRINKS), defaultDrinks()));
        files.add(RECIPE_MAIN, readObject(pathFor(RECIPE_MAIN), defaultRecipes(true)));

        Files.createDirectories(RECIPE_FRAGMENTS);
        try (Stream<Path> stream = Files.list(RECIPE_FRAGMENTS)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> SAFE_FRAGMENT.matcher(path.getFileName().toString()).matches())
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> {
                        try {
                            files.add(RECIPE_PREFIX + path.getFileName(), readObject(path, defaultRecipes(false)));
                        } catch (IOException exception) {
                            ScpAdditionsMod.LOGGER.warn("Could not include SCP-914 fragment {} in config snapshot", path, exception);
                        }
                    });
        }
        return files;
    }

    public static SaveResult saveBatch(ServerPlayer player, String changesText) {
        if (!canEdit(player)) {
            return SaveResult.failure("Operator permission level 2 is required.");
        }

        JsonObject changes;
        try {
            JsonElement parsed = JsonParser.parseString(changesText);
            if (!parsed.isJsonObject()) return SaveResult.failure("The submitted change set is not a JSON object.");
            changes = parsed.getAsJsonObject();
        } catch (Exception exception) {
            return SaveResult.failure("The submitted change set is invalid JSON: " + readable(exception));
        }
        if (changes.size() == 0) return SaveResult.failure("No configuration changes were submitted.");
        if (changes.size() > 16) return SaveResult.failure("Too many configuration files were submitted at once.");

        LinkedHashMap<Path, String> oldContents = new LinkedHashMap<>();
        LinkedHashMap<Path, String> newContents = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        try {
            for (Map.Entry<String, JsonElement> entry : changes.entrySet()) {
                String key = entry.getKey();
                Path target = pathFor(key);
                if (target == null) return SaveResult.failure("Unknown or unsafe configuration target: " + key);
                if (!entry.getValue().isJsonObject()) return SaveResult.failure(key + " must contain a JSON object.");

                JsonObject root = entry.getValue().getAsJsonObject();
                Validation validation = validate(key, root);
                if (!validation.errors().isEmpty()) {
                    return SaveResult.failure(key + ": " + String.join("; ", validation.errors()));
                }
                warnings.addAll(validation.warnings());
                oldContents.put(target, Files.exists(target) ? Files.readString(target, StandardCharsets.UTF_8) : null);
                newContents.put(target, GSON.toJson(root) + System.lineSeparator());
            }

            for (Map.Entry<Path, String> entry : newContents.entrySet()) {
                ConfigFilePersistence.writeWithBackup(entry.getKey(), entry.getValue());
            }

            try {
                reloadAll();
            } catch (Exception exception) {
                rollback(oldContents);
                reloadAll();
                return SaveResult.failure("Changes were restored because reload failed: " + readable(exception));
            }

            JsonObject updated = snapshot();
            String message = warnings.isEmpty()
                    ? "Configuration saved and reloaded."
                    : "Configuration saved with " + warnings.size() + " unavailable reference warning(s).";
            return SaveResult.success(message, updated, warnings);
        } catch (Exception exception) {
            try {
                rollback(oldContents);
                reloadAll();
            } catch (Exception rollbackFailure) {
                ScpAdditionsMod.LOGGER.error("Configuration center rollback also failed", rollbackFailure);
            }
            ScpAdditionsMod.LOGGER.error("Configuration center save failed", exception);
            return SaveResult.failure("Could not save configuration: " + readable(exception));
        }
    }

    public static void reloadAll() {
        ScpAdditionsModulesConfig.load();
        ScpInventoryConfig.reload();
        Scp173TargetConfig.load();
        StaminaItemEffectConfig.load();
        Scp294DrinkManager.loadFromConfig();
        Scp914RecipeManager.loadFromConfig();
        ContextInteractionRegistry.reload();
    }

    private static void rollback(Map<Path, String> oldContents) throws IOException {
        for (Map.Entry<Path, String> entry : oldContents.entrySet()) {
            if (entry.getValue() == null) Files.deleteIfExists(entry.getKey());
            else ConfigFilePersistence.writeWithBackup(entry.getKey(), entry.getValue());
        }
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
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : fallback.deepCopy();
        } catch (Exception exception) {
            ScpAdditionsMod.LOGGER.warn("Could not parse {} for the configuration center", path, exception);
            return fallback.deepCopy();
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
        for (String group : List.of("inventory", "interactions", "hud", "vitals", "blink", "scp_173")) {
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
        validateSimpleIds(root, "hidden_status_effects", errors, warnings, false);
        validateSimpleIds(root, "scp_173_targets", errors, warnings, true);
    }

    private static void validateContext(JsonObject root, List<String> errors, List<String> warnings) {
        if (!requireArray(root, "interactions", errors)) return;
        int index = 0;
        for (JsonElement element : root.getAsJsonArray("interactions")) {
            if (!element.isJsonObject()) { errors.add("interactions[" + index + "] must be an object"); index++; continue; }
            JsonObject object = element.getAsJsonObject();
            String type = string(object, "type");
            if (!"block".equals(type) && !"entity".equals(type)) errors.add("interactions[" + index + "].type must be block or entity");
            validateId(string(object, "id"), "interactions[" + index + "].id", errors, warnings, false);
            if (object.has("range")) requireNumber(object, "range", "interactions[" + index + "]", errors);
            if (object.has("priority")) requireNumber(object, "priority", "interactions[" + index + "]", errors);
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
            if (allowTag && id.startsWith("#")) id = id.substring(1);
            validateId(id, arrayKey + "[" + index + "]", errors, warnings, true);
            index++;
        }
    }

    private static void validateId(String raw, String label, List<String> errors, List<String> warnings, boolean allowMissing) {
        if (raw == null || raw.isBlank()) { errors.add(label + " is required"); return; }
        try { new ResourceLocation(raw.trim()); }
        catch (Exception exception) { errors.add(label + " has invalid resource id '" + raw + "'"); return; }
    }

    private static boolean requireArray(JsonObject root, String key, List<String> errors) {
        if (!root.has(key)) { root.add(key, new JsonArray()); return true; }
        if (!root.get(key).isJsonArray()) { errors.add(key + " must be an array"); return false; }
        return true;
    }

    private static void requireNumber(JsonObject root, String key, String label, List<String> errors) {
        try { root.get(key).getAsDouble(); } catch (Exception ex) { errors.add(label + "." + key + " must be a number"); }
    }

    private static boolean nonEmptyArray(JsonObject root, String key) {
        return root.has(key) && root.get(key).isJsonArray() && root.getAsJsonArray(key).size() > 0;
    }

    private static String string(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonPrimitive()) return "";
        try { return root.get(key).getAsString().trim(); } catch (Exception ignored) { return ""; }
    }

    private static JsonObject defaultModules() {
        return JsonParser.parseString("{\"inventory\":{\"enabled\":true,\"remember_ui_state\":true},\"interactions\":{\"enabled\":true,\"disable_in_creative\":false},\"hud\":{\"enabled\":true},\"vitals\":{\"custom_health_enabled\":true,\"stamina_enabled\":true,\"horror_movement_enabled\":true},\"blink\":{\"enabled\":true},\"scp_173\":{\"enabled\":true,\"natural_spawn_enabled\":true}}").getAsJsonObject();
    }

    private static JsonObject defaultInventory() {
        return JsonParser.parseString("{\"item_rules\":[],\"item_effects\":[],\"hidden_status_effects\":[],\"codex_documents\":[],\"scp_173_targets\":[]}").getAsJsonObject();
    }

    private static JsonObject defaultContext() {
        return JsonParser.parseString("{\"interactions\":[]}").getAsJsonObject();
    }

    private static JsonObject defaultDrinks() {
        return JsonParser.parseString("{\"version\":2,\"matching\":{\"allow_partial\":true,\"fuzzy_threshold\":0.66},\"drinks\":[]}").getAsJsonObject();
    }

    private static JsonObject defaultRecipes(boolean machine) {
        JsonObject root = JsonParser.parseString("{\"version\":2,\"recipes\":[]}").getAsJsonObject();
        if (machine) root.add("machine", JsonParser.parseString("{\"intake_offset\":[-5,0,-3],\"output_offset\":[5,0,-3],\"search_radius\":1.5,\"start_delay_ticks\":30,\"finish_delay_ticks\":160}"));
        return root;
    }

    private static String readable(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private record Validation(List<String> errors, List<String> warnings) {
    }

    public record SaveResult(boolean success, String message, JsonObject snapshot, List<String> warnings) {
        static SaveResult success(String message, JsonObject snapshot, List<String> warnings) {
            return new SaveResult(true, message, snapshot, List.copyOf(warnings));
        }

        static SaveResult failure(String message) {
            return new SaveResult(false, message, new JsonObject(), List.of());
        }
    }
}
