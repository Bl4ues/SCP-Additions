package net.mcreator.scpadditions.data;

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
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import net.mcreator.scpadditions.ScpAdditionsMod;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Scp294DrinkManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("scpadditions").resolve("294drinks.json");
	private static final String DEFAULT_CONFIG = """
			{
			  "version": 1,
			  "matching": {
			    "allow_partial": true,
			    "fuzzy_threshold": 0.66,
			    "ambiguous_margin": 0.02
			  },
			  "drinks": [
			    {
			      "id": "scp_additions:coffee",
			      "enabled": true,
			      "aliases": ["black coffee"],
			      "result": {
			        "item": "scp_additions:cup_of_coffee",
			        "count": 1
			      },
			      "delay_ticks": 40,
			      "sound": "scp_additions:scp294pouring",
			      "consumes_coin": true,
			      "actionbar": "Dispensing black coffee...",
			      "cup_color": "#2B1608",
			      "placeholder_cup_texture": "scp_additions:item/scp294_colored_cup_placeholder",
			      "effects": []
			    },
			    {
			      "id": "scp_additions:empty_cup",
			      "enabled": true,
			      "aliases": ["air", "nothing", "hl3", "half life 3", "emptiness", "vacuum", "cup"],
			      "result": {
			        "item": "scp_additions:empty_cup",
			        "count": 1
			      },
			      "delay_ticks": 0,
			      "sound": "scp_additions:scp294emptycup",
			      "consumes_coin": true,
			      "actionbar": "Dispensing an empty cup...",
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
	private static double ambiguousMargin = 0.02D;

	private Scp294DrinkManager() {
	}

	public static synchronized void loadFromConfig() {
		ensureConfigExists();

		Map<ResourceLocation, DrinkDefinition> parsedById = new LinkedHashMap<>();
		Map<String, DrinkDefinition> parsedByAlias = new HashMap<>();

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
						parsedByAlias.put(normalize(alias), drink);
					}
				} catch (Exception exception) {
					ScpAdditionsMod.LOGGER.error("Failed to load one SCP-294 drink entry", exception);
				}
			}
		} catch (Exception exception) {
			ScpAdditionsMod.LOGGER.error("Failed to load SCP-294 drinks from {}", CONFIG_PATH, exception);
		}

		drinksById = Map.copyOf(parsedById);
		drinksByAlias = Map.copyOf(parsedByAlias);
		ScpAdditionsMod.LOGGER.info("Loaded {} SCP-294 drink definitions from config", drinksById.size());
	}

	private static void ensureConfigExists() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			if (Files.notExists(CONFIG_PATH)) {
				Files.writeString(CONFIG_PATH, DEFAULT_CONFIG, StandardCharsets.UTF_8);
			}
		} catch (IOException exception) {
			ScpAdditionsMod.LOGGER.error("Failed to create SCP-294 config at {}", CONFIG_PATH, exception);
		}
	}

	private static void readMatchingConfig(JsonObject root) {
		if (!root.has("matching")) {
			return;
		}
		JsonObject matching = GsonHelper.getAsJsonObject(root, "matching");
		allowPartialMatches = GsonHelper.getAsBoolean(matching, "allow_partial", true);
		fuzzyThreshold = clamp(GsonHelper.getAsDouble(matching, "fuzzy_threshold", 0.66D), 0.0D, 1.0D);
		ambiguousMargin = clamp(GsonHelper.getAsDouble(matching, "ambiguous_margin", 0.02D), 0.0D, 1.0D);
	}

	private static DrinkDefinition parseDrink(JsonObject json) {
		ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "id"));
		List<String> aliases = readAliases(id, json);
		JsonObject result = GsonHelper.getAsJsonObject(json, "result");
		ResourceLocation resultItem = new ResourceLocation(GsonHelper.getAsString(result, "item"));
		int resultCount = Math.max(1, GsonHelper.getAsInt(result, "count", 1));
		int delayTicks = Math.max(0, GsonHelper.getAsInt(json, "delay_ticks", 40));
		ResourceLocation sound = new ResourceLocation(GsonHelper.getAsString(json, "sound", "scp_additions:scp294pouring"));
		boolean consumesCoin = GsonHelper.getAsBoolean(json, "consumes_coin", true);
		String actionbar = GsonHelper.getAsString(json, "actionbar", "");
		int cupColor = parseColor(GsonHelper.getAsString(json, "cup_color", "#FFFFFF"));
		String placeholderCupTexture = GsonHelper.getAsString(json, "placeholder_cup_texture", "scp_additions:item/scp294_colored_cup_placeholder");
		List<ConfiguredEffect> effects = readEffects(json);

		return new DrinkDefinition(id, aliases, resultItem, resultCount, delayTicks, sound, consumesCoin, actionbar, cupColor, placeholderCupTexture, effects);
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
		if (aliases.isEmpty()) {
			aliases.add(id.getPath());
		}
		return List.copyOf(aliases);
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
					new ResourceLocation(GsonHelper.getAsString(effect, "id")),
					Math.max(1, GsonHelper.getAsInt(effect, "duration", 200)),
					Math.max(0, GsonHelper.getAsInt(effect, "amplifier", 0)),
					GsonHelper.getAsBoolean(effect, "ambient", false),
					GsonHelper.getAsBoolean(effect, "visible", true),
					GsonHelper.getAsBoolean(effect, "show_icon", true)));
		}
		return List.copyOf(effects);
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
		boolean ambiguous = false;

		for (Map.Entry<String, DrinkDefinition> entry : drinksByAlias.entrySet()) {
			String alias = entry.getKey();
			DrinkDefinition drink = entry.getValue();
			double score = score(normalizedInput, alias);

			if (score < fuzzyThreshold) {
				continue;
			}

			if (score > bestScore + ambiguousMargin) {
				bestScore = score;
				bestDrink = drink;
				ambiguous = false;
			} else if (Math.abs(score - bestScore) <= ambiguousMargin && bestDrink != null && !bestDrink.id().equals(drink.id())) {
				ambiguous = true;
			}
		}

		if (bestDrink == null) {
			return MatchResult.noMatch();
		}
		return ambiguous ? MatchResult.ambiguous() : MatchResult.match(bestDrink);
	}

	public static ItemStack createResult(DrinkDefinition drink) {
		Item item = ForgeRegistries.ITEMS.getValue(drink.resultItem());
		if (item == null || item == Items.AIR) {
			ScpAdditionsMod.LOGGER.warn("SCP-294 drink {} points to missing item {}", drink.id(), drink.resultItem());
			return ItemStack.EMPTY;
		}

		ItemStack stack = new ItemStack(item, drink.resultCount());
		CompoundTag tag = stack.getOrCreateTag();
		CompoundTag drinkTag = new CompoundTag();
		drinkTag.putString("id", drink.id().toString());
		drinkTag.putInt("cup_color", drink.cupColor());
		drinkTag.putString("actionbar", drink.actionbar());
		drinkTag.putString("placeholder_cup_texture", drink.placeholderCupTexture());

		ListTag effectTags = new ListTag();
		for (ConfiguredEffect effect : drink.effects()) {
			CompoundTag effectTag = new CompoundTag();
			effectTag.putString("id", effect.id().toString());
			effectTag.putInt("duration", effect.duration());
			effectTag.putInt("amplifier", effect.amplifier());
			effectTag.putBoolean("ambient", effect.ambient());
			effectTag.putBoolean("visible", effect.visible());
			effectTag.putBoolean("show_icon", effect.showIcon());
			effectTags.add(effectTag);
		}
		drinkTag.put("effects", effectTags);
		tag.put("Scp294Drink", drinkTag);
		return stack;
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

	public record DrinkDefinition(ResourceLocation id, List<String> aliases, ResourceLocation resultItem, int resultCount, int delayTicks, ResourceLocation sound, boolean consumesCoin, String actionbar, int cupColor,
			String placeholderCupTexture, List<ConfiguredEffect> effects) {
	}

	public record ConfiguredEffect(ResourceLocation id, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
	}

	public record MatchResult(DrinkDefinition drink, boolean ambiguous) {
		public static MatchResult match(DrinkDefinition drink) {
			return new MatchResult(drink, false);
		}

		public static MatchResult ambiguous() {
			return new MatchResult(null, true);
		}

		public static MatchResult noMatch() {
			return new MatchResult(null, false);
		}

		public boolean found() {
			return drink != null;
		}
	}
}