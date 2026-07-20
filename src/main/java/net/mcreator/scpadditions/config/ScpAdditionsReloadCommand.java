package net.mcreator.scpadditions.config;

import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.bl4ues.scpinventory.context.ContextInteractionRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.data.Scp294DrinkManager;
import net.mcreator.scpadditions.data.Scp914RecipeManager;
import net.mcreator.scpadditions.entity.Scp173TargetConfig;
import net.mcreator.scpadditions.vitals.StaminaItemEffectConfig;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScpAdditionsReloadCommand {
    private static final Path SCP_CONFIG = FMLPaths.CONFIGDIR.get().resolve("scpadditions");
    private static final Path INVENTORY_CONFIG = FMLPaths.CONFIGDIR.get().resolve("scpinventory");

    private ScpAdditionsReloadCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("scpadditions")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload").executes(context -> reload(context.getSource()))));
    }

    private static int reload(net.minecraft.commands.CommandSourceStack source) {
        ValidationReport validation = validateAll();
        if (!validation.errors().isEmpty()) {
            source.sendFailure(Component.literal("SCP Additions config reload cancelled: " + validation.errors().size() + " problem(s) found."));
            for (String error : validation.errors()) {
                source.sendFailure(Component.literal("• " + error));
            }
            return 0;
        }

        try {
            ScpAdditionsModulesConfig.load();
            ScpInventoryConfig.reload();
            Scp173TargetConfig.load();
            StaminaItemEffectConfig.load();
            Scp294DrinkManager.loadFromConfig();
            Scp914RecipeManager.loadFromConfig();
            ContextInteractionRegistry.reload();
            ModNetwork.syncModuleState(source.getServer().getPlayerList().getPlayers());
            ModNetwork.syncServerConfig(source.getServer().getPlayerList().getPlayers());
            source.sendSuccess(() -> Component.literal("SCP Additions configurations reloaded successfully.")
                    .withStyle(ChatFormatting.GREEN), true);
            if (!validation.warnings().isEmpty()) {
                source.sendSuccess(() -> Component.literal(validation.warnings().size()
                                + " unavailable config reference(s) were ignored:")
                        .withStyle(ChatFormatting.YELLOW), false);
                for (String warning : validation.warnings()) {
                    source.sendSuccess(() -> Component.literal("• " + warning)
                            .withStyle(ChatFormatting.YELLOW), false);
                }
            }
            return 1;
        } catch (Exception exception) {
            ScpAdditionsMod.LOGGER.error("Unexpected failure while reloading SCP Additions configurations", exception);
            source.sendFailure(Component.literal("Reload failed: " + readable(exception)));
            return 0;
        }
    }

    private static ValidationReport validateAll() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        validateModules(SCP_CONFIG.resolve("modules.json"), errors);
        validateInventory(INVENTORY_CONFIG.resolve("scpinventory.json"), errors, warnings);
        validateContext(INVENTORY_CONFIG.resolve("context_interactions.json"), errors, warnings);
        validate294(SCP_CONFIG.resolve("294drinks.json"), errors, warnings);
        validate914(SCP_CONFIG.resolve("914recipes.json"), errors, warnings);

        Path fragments = SCP_CONFIG.resolve("914recipes.d");
        if (Files.isDirectory(fragments)) {
            try (Stream<Path> files = Files.list(fragments)) {
                files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .forEach(path -> validate914(path, errors, warnings));
            } catch (Exception exception) {
                errors.add(relative(fragments) + ": " + readable(exception));
            }
        }
        return new ValidationReport(List.copyOf(errors), List.copyOf(warnings));
    }

    private static void validateModules(Path path, List<String> errors) {
        JsonObject root = readObject(path, errors);
        if (root == null) return;
        requireObjectIfPresent(root, "inventory", path, errors);
        requireObjectIfPresent(root, "interactions", path, errors);
        requireObjectIfPresent(root, "hud", path, errors);
        requireObjectIfPresent(root, "vitals", path, errors);
        requireObjectIfPresent(root, "blink", path, errors);
        requireObjectIfPresent(root, "scp_173", path, errors);
        validateBooleanMember(root, "inventory", "enabled", path, errors);
        validateBooleanMember(root, "inventory", "remember_ui_state", path, errors);
        rejectUnknownMember(root, "inventory", "disabled", path, errors,
                "Use inventory.enabled: false instead of inventory.disabled.");
    }

    private static void validateBooleanMember(JsonObject root, String objectKey,
                                              String memberKey, Path path,
                                              List<String> errors) {
        if (!root.has(objectKey) || !root.get(objectKey).isJsonObject()) return;
        JsonObject object = root.getAsJsonObject(objectKey);
        if (object.has(memberKey) && (!object.get(memberKey).isJsonPrimitive()
                || !object.get(memberKey).getAsJsonPrimitive().isBoolean())) {
            errors.add(relative(path) + ": " + objectKey + "." + memberKey
                    + " must be true or false");
        }
    }

    private static void rejectUnknownMember(JsonObject root, String objectKey,
                                            String memberKey, Path path,
                                            List<String> errors, String help) {
        if (!root.has(objectKey) || !root.get(objectKey).isJsonObject()) return;
        if (root.getAsJsonObject(objectKey).has(memberKey)) {
            errors.add(relative(path) + ": unknown field " + objectKey + "."
                    + memberKey + ". " + help);
        }
    }

    private static void validateInventory(Path path, List<String> errors, List<String> warnings) {
        JsonObject root = readObject(path, errors);
        if (root == null) return;
        validateConfiguredIds(root, "item_rules", path, errors, warnings, "id", "item", ScpAdditionsReloadCommand::itemExists);
        validateConfiguredIds(root, "item_effects", path, errors, warnings, "id", "item", ScpAdditionsReloadCommand::itemExists);
        validateConfiguredIds(root, "codex_documents", path, errors, warnings, "id", "item", ScpAdditionsReloadCommand::itemExists);
        validateSimpleIds(root, "hidden_status_effects", path, errors, warnings, ScpAdditionsReloadCommand::effectExists, false);
        validateSimpleIds(root, "scp_173_targets", path, errors, warnings, ScpAdditionsReloadCommand::entityExists, true);
    }

    private static void validateContext(Path path, List<String> errors, List<String> warnings) {
        JsonObject root = readObject(path, errors);
        if (root == null || !root.has("interactions")) return;
        if (!root.get("interactions").isJsonArray()) {
            errors.add(relative(path) + ": interactions must be an array");
            return;
        }
        int index = 0;
        for (JsonElement element : root.getAsJsonArray("interactions")) {
            if (!element.isJsonObject()) {
                errors.add(relative(path) + ": interactions[" + index + "] must be an object");
                index++;
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            String type = string(entry, "type");
            String id = string(entry, "id");
            Predicate<ResourceLocation> exists = "entity".equals(type) ? ScpAdditionsReloadCommand::entityExists
                    : "block".equals(type) ? ScpAdditionsReloadCommand::blockExists : ignored -> false;
            if (!"block".equals(type) && !"entity".equals(type)) {
                errors.add(relative(path) + ": interactions[" + index + "].type must be block or entity");
            } else validateId(path, "interactions[" + index + "].id", id, exists, false, errors, warnings);
            index++;
        }
    }

    private static void validate294(Path path, List<String> errors, List<String> warnings) {
        JsonObject root = readObject(path, errors);
        if (root == null || !array(root, "drinks", path, errors)) return;
        int index = 0;
        for (JsonElement element : root.getAsJsonArray("drinks")) {
            if (!element.isJsonObject()) {
                errors.add(relative(path) + ": drinks[" + index + "] must be an object");
                index++;
                continue;
            }
            JsonObject drink = element.getAsJsonObject();
            if (disabled(drink, path, "drinks[" + index + "]", errors)) {
                index++;
                continue;
            }
            validateId(path, "drinks[" + index + "].id", string(drink, "id"), ignored -> true, false, errors, warnings);
            JsonObject result = drink.has("result") && drink.get("result").isJsonObject() ? drink.getAsJsonObject("result") : null;
            if (result != null && result.has("item")) validateId(path, "drinks[" + index + "].result.item", string(result, "item"), ScpAdditionsReloadCommand::itemExists, false, errors, warnings);
            if (drink.has("sound")) validateId(path, "drinks[" + index + "].sound", string(drink, "sound"), ScpAdditionsReloadCommand::soundExists, false, errors, warnings);
            validateObjectArrayIds(drink, "effects", "id", path, "drinks[" + index + "].effects", errors, warnings, ScpAdditionsReloadCommand::effectExists);
            validate294Actions(drink, "actions", path, "drinks[" + index + "].actions", errors, warnings);
            validate294Actions(drink, "drink_actions", path, "drinks[" + index + "].drink_actions", errors, warnings);
            validate294Actions(drink, "dispense_actions", path, "drinks[" + index + "].dispense_actions", errors, warnings);
            index++;
        }
    }

    private static void validate914(Path path, List<String> errors, List<String> warnings) {
        JsonObject root = readObject(path, errors);
        if (root == null || !array(root, "recipes", path, errors)) return;
        int index = 0;
        for (JsonElement element : root.getAsJsonArray("recipes")) {
            if (!element.isJsonObject()) {
                errors.add(relative(path) + ": recipes[" + index + "] must be an object");
                index++;
                continue;
            }
            JsonObject recipe = element.getAsJsonObject();
            if (disabled(recipe, path, "recipes[" + index + "]", errors)) {
                index++;
                continue;
            }
            validateId(path, "recipes[" + index + "].id", string(recipe, "id"), ignored -> true, false, errors, warnings);
            String setting = string(recipe, "setting");
            if (!Set.of("rough", "coarse", "1_to_1", "fine", "very_fine").contains(setting)) {
                errors.add(relative(path) + ": recipes[" + index + "].setting has invalid value '" + setting + "'");
            }
            validateObjectArrayIds(recipe, "item_inputs", "item", path, "recipes[" + index + "].item_inputs", errors, warnings, ScpAdditionsReloadCommand::itemExists);
            validateObjectArrayIds(recipe, "item_outputs", "item", path, "recipes[" + index + "].item_outputs", errors, warnings, ScpAdditionsReloadCommand::itemExists);
            validateObjectArrayIds(recipe, "weighted_item_outputs", "item", path, "recipes[" + index + "].weighted_item_outputs", errors, warnings, ScpAdditionsReloadCommand::itemExists);
            validateObjectArrayIds(recipe, "entity_inputs", "entity", path, "recipes[" + index + "].entity_inputs", errors, warnings, ScpAdditionsReloadCommand::entityExists);
            validateObjectArrayIds(recipe, "entity_outputs", "entity", path, "recipes[" + index + "].entity_outputs", errors, warnings, ScpAdditionsReloadCommand::entityExists);
            if (recipe.has("input") && recipe.get("input").isJsonObject()) validateId(path, "recipes[" + index + "].input.item", string(recipe.getAsJsonObject("input"), "item"), ScpAdditionsReloadCommand::itemExists, false, errors, warnings);
            if (recipe.has("output") && recipe.get("output").isJsonObject()) validateId(path, "recipes[" + index + "].output.item", string(recipe.getAsJsonObject("output"), "item"), ScpAdditionsReloadCommand::itemExists, false, errors, warnings);
            index++;
        }
    }

    private static JsonObject readObject(Path path, List<String> errors) {
        if (Files.notExists(path)) return null;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                errors.add(relative(path) + ": root must be a JSON object");
                return null;
            }
            return parsed.getAsJsonObject();
        } catch (Exception exception) {
            errors.add(relative(path) + ": invalid JSON — " + readable(exception));
            return null;
        }
    }

    private static void validateConfiguredIds(JsonObject root, String key, Path path, List<String> errors,
                                              List<String> warnings,
                                              String firstKey, String secondKey, Predicate<ResourceLocation> exists) {
        if (!array(root, key, path, errors)) return;
        int index = 0;
        for (JsonElement element : root.getAsJsonArray(key)) {
            String raw = element.isJsonObject() ? firstString(element.getAsJsonObject(), firstKey, secondKey)
                    : element.isJsonPrimitive() ? element.getAsString().split("[|;]", 2)[0].trim() : "";
            if (raw.startsWith("id=")) raw = raw.substring(3).trim();
            validateId(path, key + "[" + index + "]", raw, exists, true, errors, warnings);
            index++;
        }
    }

    private static void validateSimpleIds(JsonObject root, String key, Path path, List<String> errors,
                                          List<String> warnings,
                                          Predicate<ResourceLocation> exists, boolean allowTag) {
        if (!array(root, key, path, errors)) return;
        int index = 0;
        for (JsonElement element : root.getAsJsonArray(key)) {
            String raw = element.isJsonPrimitive() ? element.getAsString()
                    : element.isJsonObject() ? firstString(element.getAsJsonObject(), "id", "entity", "effect", "tag") : "";
            validateId(path, key + "[" + index + "]", raw, exists, allowTag, errors, warnings);
            index++;
        }
    }

    private static void validateObjectArrayIds(JsonObject root, String arrayKey, String idKey, Path path,
                                               String label, List<String> errors, List<String> warnings,
                                               Predicate<ResourceLocation> exists) {
        if (!root.has(arrayKey)) return;
        if (!root.get(arrayKey).isJsonArray()) {
            errors.add(relative(path) + ": " + label + " must be an array");
            return;
        }
        int index = 0;
        for (JsonElement element : root.getAsJsonArray(arrayKey)) {
            String raw = element.isJsonObject() ? string(element.getAsJsonObject(), idKey) : "";
            validateId(path, label + "[" + index + "]." + idKey, raw, exists, false, errors, warnings);
            index++;
        }
    }

    private static void validate294Actions(JsonObject root, String arrayKey, Path path,
                                           String label, List<String> errors, List<String> warnings) {
        if (!root.has(arrayKey)) return;
        if (!root.get(arrayKey).isJsonArray()) {
            errors.add(relative(path) + ": " + label + " must be an array");
            return;
        }
        int index = 0;
        for (JsonElement element : root.getAsJsonArray(arrayKey)) {
            if (!element.isJsonObject()) {
                errors.add(relative(path) + ": " + label + "[" + index + "] must be an object");
                index++;
                continue;
            }
            JsonObject action = element.getAsJsonObject();
            String type = string(action, "type");
            if (("effect".equals(type) || "remove_effect".equals(type))) {
                validateId(path, label + "[" + index + "].effect",
                        firstString(action, "effect", "id"), ScpAdditionsReloadCommand::effectExists, false, errors, warnings);
            }
            if ("sound".equals(type) || ("visual_explosion".equals(type) && action.has("sound"))) {
                validateId(path, label + "[" + index + "].sound",
                        string(action, "sound"), ScpAdditionsReloadCommand::soundExists, false, errors, warnings);
            }
            index++;
        }
    }

    private static boolean disabled(JsonObject object, Path path, String label, List<String> errors) {
        if (!object.has("enabled")) return false;
        if (!object.get("enabled").isJsonPrimitive()) {
            errors.add(relative(path) + ": " + label + ".enabled must be true or false");
            return false;
        }
        try {
            return !object.get("enabled").getAsBoolean();
        } catch (Exception exception) {
            errors.add(relative(path) + ": " + label + ".enabled must be true or false");
            return false;
        }
    }

    private static void validateId(Path path, String field, String raw, Predicate<ResourceLocation> exists,
                                   boolean allowTag, List<String> errors, List<String> warnings) {
        String value = raw == null ? "" : raw.trim();
        boolean tag = allowTag && value.startsWith("#");
        if (tag) value = value.substring(1);
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) errors.add(relative(path) + ": " + field + " has invalid ID '" + raw + "'");
        else if (!tag && !exists.test(id)) {
            String warning = relative(path) + ": " + field + " references unavailable ID '" + raw + "'";
            if (!warnings.contains(warning)) warnings.add(warning);
        }
    }

    private static boolean array(JsonObject root, String key, Path path, List<String> errors) {
        if (!root.has(key)) return false;
        if (root.get(key).isJsonArray()) return true;
        errors.add(relative(path) + ": " + key + " must be an array");
        return false;
    }

    private static void requireObjectIfPresent(JsonObject root, String key, Path path, List<String> errors) {
        if (root.has(key) && !root.get(key).isJsonObject()) errors.add(relative(path) + ": " + key + " must be an object");
    }

    private static String firstString(JsonObject object, String... keys) {
        for (String key : keys) if (object.has(key) && object.get(key).isJsonPrimitive()) return object.get(key).getAsString().trim();
        return "";
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString().trim() : "";
    }

    private static boolean itemExists(ResourceLocation id) { return ForgeRegistries.ITEMS.containsKey(id); }
    private static boolean blockExists(ResourceLocation id) { return ForgeRegistries.BLOCKS.containsKey(id); }
    private static boolean entityExists(ResourceLocation id) { return ForgeRegistries.ENTITY_TYPES.containsKey(id); }
    private static boolean effectExists(ResourceLocation id) { return ForgeRegistries.MOB_EFFECTS.containsKey(id); }
    private static boolean soundExists(ResourceLocation id) { return ForgeRegistries.SOUND_EVENTS.containsKey(id); }

    private static String relative(Path path) {
        Path config = FMLPaths.CONFIGDIR.get();
        return path.startsWith(config) ? "config/" + config.relativize(path).toString().replace('\\', '/') : path.toString();
    }

    private static String readable(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private record ValidationReport(List<String> errors, List<String> warnings) {
    }
}
