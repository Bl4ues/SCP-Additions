package net.mcreator.scpadditions.data;

import com.bl4ues.scpadditions.compat.LegacyItemTags;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ConfigFilePersistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Scp294DrinkManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("scpadditions").resolve("294drinks.json");
	private static final String BUNDLED_CONFIG = "config/scpadditions/294drinks.json";
	private static final ResourceLocation GENERIC_CUP = ResourceLocation.fromNamespaceAndPath("scp_additions", "cup_of_coffee");
	private static final Set<String> LEGACY_DRINK_ITEM_PATHS = Set.of(
			"aloe",
			"amnesia",
			"anti_energy",
			"apple_cider",
			"aqua_regia",
			"beer",
			"bleach",
			"blood",
			"blood_of_christ",
			"cactus",
			"carbon",
			"carrot",
			"cassis_fanta",
			"champagne",
			"champion",
			"chim",
			"chocolate",
			"cider",
			"cocaine",
			"coconut",
			"cola",
			"cold",
			"corrosive_acid",
			"corrosive_black",
			"cosmopolitan",
			"courage",
			"cup_of_alcohol",
			"curry",
			"death",
			"eggs",
			"energy_drink",
			"espresso",
			"estus",
			"ethanol",
			"fear",
			"feces",
			"feces_and_blood",
			"frozen_yogurt",
			"gin",
			"glass",
			"gold_c",
			"grimace_shake",
			"grog",
			"happiness",
			"heroin",
			"honey",
			"hot",
			"ice_cream",
			"ink",
			"insulin",
			"ipecac",
			"iron_c",
			"lager",
			"morphine",
			"neutronium",
			"pear_cider",
			"quantum",
			"spirit",
			"tea",
			"vodka",
			"yogurt");
	private static final String DEFAULT_CONFIG = """
			{
			  "version": 2,
			  "matching": {
			    "allow_partial": true,
			    "fuzzy_threshold": 0.66
			  },
			  "drinks": [
			    {
			      "id": "scp_additions:coffee",
			      "enabled": true,
			      "aliases": ["coffee", "black coffee"],
			      "result": { "item": "scp_additions:cup_of_coffee", "count": 1 },
			      "delay_ticks": 40,
			      "sound": "scp_additions:scp294pouring",
			      "consumes_coin": true,
			      "give_result": true,
			      "drinkable": true,
			      "cup_color": "#2B1608",
			      "actionbar": "The drink tastes like fairly strong black coffee.",
			      "effects": [
			        { "id": "minecraft:nausea", "duration": 40, "amplifier": 0, "visible": true, "show_icon": false }
			      ],
			      "drink_actions": [
			        { "type": "remove_effect", "effect": "minecraft:hunger" }
			      ],
			      "dispense_actions": []
			    },
			    {
			      "id": "scp_additions:empty_cup",
			      "enabled": true,
			      "aliases": ["air", "nothing", "hl3", "half life 3", "emptiness", "vacuum", "cup"],
			      "result": { "item": "scp_additions:empty_cup", "count": 1 },
			      "delay_ticks": 0,
			      "sound": "scp_additions:scp294emptycup",
			      "consumes_coin": true,
			      "give_result": true,
			      "drinkable": false,
			      "refuse_message": "There is nothing to drink in the cup.",
			      "cup_color": "#FFFFFF",
			      "effects": []
			    }
			  ]
			}
			""";

	private static Map<ResourceLocation, DrinkDefinition> drinksById = Map.of();
	private static Map<String, DrinkDefinition> drinksByAlias = Map.of();
	private static boolean allowPartialMatches = true;
	private static double fuzzyThreshold = 0.66D;

	private Scp294DrinkManager() {
	}

	public static synchronized void loadFromConfig() {
		ensureConfigExists();

		Map<ResourceLocation, DrinkDefinition> parsedById = new LinkedHashMap<>();
		Map<String, DrinkDefinition> parsedByAlias = new LinkedHashMap<>();

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			readMatchingConfig(root);
			JsonArray drinks = root.has("drinks") ? GsonHelper.getAsJsonArray(root, "drinks") : new JsonArray();

			for (JsonElement element : drinks) {
				try {
					JsonObject json = GsonHelper.convertToJsonObject(element, "SCP-294 drink");
					if (json.has("enabled") && !GsonHelper.getAsBoolean(json, "enabled")) {
						continue;
					}

					DrinkDefinition drink = parseDrink(json);
					parsedById.put(drink.id(), drink);
					for (String alias : drink.aliases()) {
						parsedByAlias.putIfAbsent(normalize(alias), drink);
					}
				} catch (Exception exception) {
					ScpAdditionsMod.LOGGER.error("Failed to load one SCP-294 drink entry", exception);
				}
			}
		} catch (Exception exception) {
			ScpAdditionsMod.LOGGER.error("Failed to load SCP-294 drinks from {}", CONFIG_PATH, exception);
		}

		drinksById = Map.copyOf(parsedById);
		drinksByAlias = new LinkedHashMap<>(parsedByAlias);
		ScpAdditionsMod.LOGGER.info("Loaded {} SCP-294 drink definitions from config", drinksById.size());
	}

	private static void ensureConfigExists() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			if (Files.notExists(CONFIG_PATH)) {
				writeDefaultConfig();
			} else {
				prettyPrintConfigIfNeeded();
			}
		} catch (IOException exception) {
			ScpAdditionsMod.LOGGER.error("Failed to create SCP-294 config at {}", CONFIG_PATH, exception);
		}
	}

	private static void writeDefaultConfig() throws IOException {
		try (InputStream stream = Scp294DrinkManager.class.getClassLoader().getResourceAsStream(BUNDLED_CONFIG)) {
			if (stream != null) {
				Files.copy(stream, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
				return;
			}
		}
		Files.writeString(CONFIG_PATH, DEFAULT_CONFIG, StandardCharsets.UTF_8);
	}

	private static void prettyPrintConfigIfNeeded() throws IOException {
		String content = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
		boolean looksMinified = !content.contains("\n") || content.lines().anyMatch(line -> line.length() > 240);
		if (!looksMinified) {
			return;
		}

		JsonElement json = JsonParser.parseString(content);
		ConfigFilePersistence.writeWithBackup(CONFIG_PATH,
				GSON.toJson(json) + System.lineSeparator());
	}

	private static void readMatchingConfig(JsonObject root) {
		if (!root.has("matching")) {
			return;
		}
		JsonObject matching = GsonHelper.getAsJsonObject(root, "matching");
		allowPartialMatches = GsonHelper.getAsBoolean(matching, "allow_partial", true);
		fuzzyThreshold = clamp(GsonHelper.getAsDouble(matching, "fuzzy_threshold", 0.66D), 0.0D, 1.0D);
	}

	private static DrinkDefinition parseDrink(JsonObject json) {
		ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(json, "id"));
		List<String> aliases = readAliases(id, json);
		JsonObject result = json.has("result") ? GsonHelper.getAsJsonObject(json, "result") : new JsonObject();
		ResourceLocation resultItem = normalizeLegacyDrinkItem(ResourceLocation.parse(
				GsonHelper.getAsString(result, "item", GENERIC_CUP.toString())));
		int resultCount = Math.max(1, GsonHelper.getAsInt(result, "count", 1));
		int delayTicks = Math.max(0, GsonHelper.getAsInt(json, "delay_ticks", 40));
		ResourceLocation sound = ResourceLocation.parse(GsonHelper.getAsString(json, "sound", "scp_additions:scp294pouring"));
		boolean consumesCoin = GsonHelper.getAsBoolean(json, "consumes_coin", true);
		boolean giveResult = GsonHelper.getAsBoolean(json, "give_result", true);
		boolean drinkable = GsonHelper.getAsBoolean(json, "drinkable", true);
		String refuseMessage = GsonHelper.getAsString(json, "refuse_message", "I shouldn't drink that.");
		String actionbar = GsonHelper.getAsString(json, "actionbar", "");
		int cupColor = parseColor(GsonHelper.getAsString(json, "cup_color", "#FFFFFF"));
		String placeholderCupTexture = GsonHelper.getAsString(json, "placeholder_cup_texture", "scp_additions:item/scp294_colored_cup_placeholder");
		List<ConfiguredEffect> effects = readEffects(json);
		List<ConfiguredAction> drinkActions = readActions(json, "drink_actions");
		if (json.has("actions")) {
			drinkActions = mergeActions(drinkActions, readActions(json, "actions"));
		}
		List<ConfiguredAction> dispenseActions = readActions(json, "dispense_actions");

		return new DrinkDefinition(id, aliases, resultItem, resultCount, delayTicks, sound, consumesCoin, giveResult, drinkable, refuseMessage, actionbar, cupColor, placeholderCupTexture, effects, drinkActions, dispenseActions);
	}

	private static ResourceLocation normalizeLegacyDrinkItem(ResourceLocation requested) {
		if (ScpAdditionsMod.MODID.equals(requested.getNamespace())
				&& LEGACY_DRINK_ITEM_PATHS.contains(requested.getPath())) {
			return GENERIC_CUP;
		}
		return requested;
	}

	private static List<String> readAliases(ResourceLocation id, JsonObject json) {
		List<String> aliases = new ArrayList<>();
		if (json.has("aliases")) {
			JsonArray array = GsonHelper.getAsJsonArray(json, "aliases");
			for (JsonElement element : array) {
				String alias = element.getAsString();
				if (!alias.isBlank()) {
					aliases.add(alias);
				}
			}
		}
		aliases.add(id.getPath());
		aliases.add(id.getPath().replace('_', ' '));
		return List.copyOf(aliases.stream().distinct().toList());
	}

	private static List<ConfiguredEffect> readEffects(JsonObject json) {
		List<ConfiguredEffect> effects = new ArrayList<>();
		if (!json.has("effects")) {
			return List.of();
		}
		JsonArray array = GsonHelper.getAsJsonArray(json, "effects");
		for (JsonElement element : array) {
			JsonObject effect = GsonHelper.convertToJsonObject(element, "SCP-294 effect");
			effects.add(new ConfiguredEffect(
					ResourceLocation.parse(GsonHelper.getAsString(effect, "id")),
					Math.max(1, GsonHelper.getAsInt(effect, "duration", 200)),
					Math.max(0, GsonHelper.getAsInt(effect, "amplifier", 0)),
					GsonHelper.getAsBoolean(effect, "ambient", false),
					GsonHelper.getAsBoolean(effect, "visible", true),
					GsonHelper.getAsBoolean(effect, "show_icon", true)));
		}
		return List.copyOf(effects);
	}

	private static List<ConfiguredAction> readActions(JsonObject json, String key) {
		List<ConfiguredAction> actions = new ArrayList<>();
		if (!json.has(key)) {
			return List.of();
		}
		JsonArray array = GsonHelper.getAsJsonArray(json, key);
		for (JsonElement element : array) {
			JsonObject action = GsonHelper.convertToJsonObject(element, "SCP-294 action");
			actions.add(new ConfiguredAction(
					GsonHelper.getAsString(action, "type", "actionbar"),
					Math.max(0, GsonHelper.getAsInt(action, "delay_ticks", 0)),
					GsonHelper.getAsString(action, "message", ""),
					GsonHelper.getAsString(action, "sound", ""),
					GsonHelper.getAsString(action, "particle", ""),
					GsonHelper.getAsString(action, "effect", GsonHelper.getAsString(action, "id", "")),
					GsonHelper.getAsString(action, "entity", ""),
					GsonHelper.getAsFloat(action, "amount", 0.0F),
					Math.max(1, GsonHelper.getAsInt(action, "duration", 200)),
					Math.max(0, GsonHelper.getAsInt(action, "amplifier", 0)),
					GsonHelper.getAsBoolean(action, "ambient", false),
					GsonHelper.getAsBoolean(action, "visible", true),
					GsonHelper.getAsBoolean(action, "show_icon", true),
					GsonHelper.getAsFloat(action, "radius", 0.0F),
					GsonHelper.getAsDouble(action, "spread", 0.0D),
					Math.max(0, GsonHelper.getAsInt(action, "count", 0)),
					Math.max(0, GsonHelper.getAsInt(action, "seconds", 0))));
		}
		return List.copyOf(actions);
	}

	private static List<ConfiguredAction> mergeActions(List<ConfiguredAction> first, List<ConfiguredAction> second) {
		List<ConfiguredAction> merged = new ArrayList<>(first);
		merged.addAll(second);
		return List.copyOf(merged);
	}

	public static MatchResult findByInput(String rawInput) {
		String normalizedInput = normalize(rawInput);
		if (normalizedInput.isBlank()) {
			return MatchResult.noMatch();
		}

		DrinkDefinition exact = drinksByAlias.get(normalizedInput);
		if (exact != null) {
			return MatchResult.match(exact);
		}

		DrinkDefinition bestDrink = null;
		double bestScore = 0.0D;

		for (Map.Entry<String, DrinkDefinition> entry : drinksByAlias.entrySet()) {
			String alias = entry.getKey();
			DrinkDefinition drink = entry.getValue();
			double score = score(normalizedInput, alias);

			if (score < fuzzyThreshold) {
				continue;
			}

			if (score > bestScore) {
				bestScore = score;
				bestDrink = drink;
			}
		}

		return bestDrink == null ? MatchResult.noMatch() : MatchResult.match(bestDrink);
	}

	public static ItemStack createResult(DrinkDefinition drink) {
		Item item = BuiltInRegistries.ITEM.get(drink.resultItem());
		if (item == null || item == Items.AIR) {
			ScpAdditionsMod.LOGGER.warn("SCP-294 drink {} points to missing item {}", drink.id(), drink.resultItem());
			return ItemStack.EMPTY;
		}

		ItemStack stack = new ItemStack(item, drink.resultCount());
		CompoundTag tag = LegacyItemTags.getOrCreateTag(stack);
		CompoundTag drinkTag = new CompoundTag();
		drinkTag.putString("id", drink.id().toString());
		drinkTag.putInt("cup_color", drink.cupColor());
		drinkTag.putString("actionbar", drink.actionbar());
		drinkTag.putBoolean("drinkable", drink.drinkable());
		drinkTag.putString("refuse_message", drink.refuseMessage());
		drinkTag.putString("placeholder_cup_texture", drink.placeholderCupTexture());
		drinkTag.put("effects", effectsToTag(drink.effects()));
		drinkTag.put("drink_actions", actionsToTag(drink.drinkActions()));
		tag.put("Scp294Drink", drinkTag);
		return stack;
	}

	public static ListTag actionsToTag(List<ConfiguredAction> actions) {
		ListTag actionTags = new ListTag();
		for (ConfiguredAction action : actions) {
			actionTags.add(action.toTag());
		}
		return actionTags;
	}

	private static ListTag effectsToTag(List<ConfiguredEffect> effects) {
		ListTag effectTags = new ListTag();
		for (ConfiguredEffect effect : effects) {
			CompoundTag effectTag = new CompoundTag();
			effectTag.putString("id", effect.id().toString());
			effectTag.putInt("duration", effect.duration());
			effectTag.putInt("amplifier", effect.amplifier());
			effectTag.putBoolean("ambient", effect.ambient());
			effectTag.putBoolean("visible", effect.visible());
			effectTag.putBoolean("show_icon", effect.showIcon());
			effectTags.add(effectTag);
		}
		return effectTags;
	}

	private static double score(String input, String alias) {
		if (input.equals(alias)) {
			return 1.0D;
		}
		if (allowPartialMatches && (alias.contains(input) || input.contains(alias))) {
			return 0.92D + (0.06D * tokenOverlap(input, alias));
		}
		double levenshtein = levenshteinSimilarity(input, alias);
		double tokenScore = tokenOverlap(input, alias);
		return (levenshtein * 0.75D) + (tokenScore * 0.25D);
	}

	private static double tokenOverlap(String input, String alias) {
		String[] inputTokens = input.split(" ");
		String[] aliasTokens = alias.split(" ");
		if (inputTokens.length == 0 || aliasTokens.length == 0) {
			return 0.0D;
		}
		int matches = 0;
		for (String inputToken : inputTokens) {
			for (String aliasToken : aliasTokens) {
				if (inputToken.equals(aliasToken) || aliasToken.contains(inputToken) || inputToken.contains(aliasToken)) {
					matches++;
					break;
				}
			}
		}
		return (double) matches / Math.max(inputTokens.length, aliasTokens.length);
	}

	private static double levenshteinSimilarity(String a, String b) {
		int max = Math.max(a.length(), b.length());
		if (max == 0) {
			return 1.0D;
		}
		return 1.0D - ((double) levenshteinDistance(a, b) / max);
	}

	private static int levenshteinDistance(String a, String b) {
		int[] previous = new int[b.length() + 1];
		int[] current = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) {
			previous[j] = j;
		}
		for (int i = 1; i <= a.length(); i++) {
			current[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
			}
			int[] temp = previous;
			previous = current;
			current = temp;
		}
		return previous[b.length()];
	}

	private static String normalize(String value) {
		if (value == null) {
			return "";
		}
		return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
	}

	private static int parseColor(String value) {
		String normalized = value.trim();
		if (normalized.startsWith("#")) {
			normalized = normalized.substring(1);
		}
		return Integer.parseInt(normalized, 16) & 0xFFFFFF;
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	public record DrinkDefinition(ResourceLocation id, List<String> aliases, ResourceLocation resultItem, int resultCount, int delayTicks, ResourceLocation sound, boolean consumesCoin, boolean giveResult,
			boolean drinkable, String refuseMessage, String actionbar, int cupColor, String placeholderCupTexture, List<ConfiguredEffect> effects, List<ConfiguredAction> drinkActions,
			List<ConfiguredAction> dispenseActions) {
	}

	public record ConfiguredEffect(ResourceLocation id, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
	}

	public record ConfiguredAction(String type, int delayTicks, String message, String sound, String particle, String effect, String entity, float amount, int duration, int amplifier, boolean ambient,
			boolean visible, boolean showIcon, float radius, double spread, int count, int seconds) {
		public CompoundTag toTag() {
			CompoundTag tag = new CompoundTag();
			tag.putString("type", type());
			tag.putInt("delay_ticks", delayTicks());
			tag.putString("message", message());
			tag.putString("sound", sound());
			tag.putString("particle", particle());
			tag.putString("effect", effect());
			tag.putString("entity", entity());
			tag.putFloat("amount", amount());
			tag.putInt("duration", duration());
			tag.putInt("amplifier", amplifier());
			tag.putBoolean("ambient", ambient());
			tag.putBoolean("visible", visible());
			tag.putBoolean("show_icon", showIcon());
			tag.putFloat("radius", radius());
			tag.putDouble("spread", spread());
			tag.putInt("count", count());
			tag.putInt("seconds", seconds());
			return tag;
		}
	}

	public record MatchResult(DrinkDefinition drink) {
		public static MatchResult match(DrinkDefinition drink) {
			return new MatchResult(drink);
		}

		public static MatchResult noMatch() {
			return new MatchResult(null);
		}

		public boolean found() {
			return drink != null;
		}
	}
}
