package net.mcreator.scpadditions.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class Scp294DrinkManager extends SimpleJsonResourceReloadListener {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String DIRECTORY = "scp294/drinks";
	private static Map<ResourceLocation, DrinkDefinition> drinksById = Map.of();
	private static Map<String, DrinkDefinition> drinksByAlias = Map.of();

	public Scp294DrinkManager() {
		super(GSON, DIRECTORY);
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
		Map<ResourceLocation, DrinkDefinition> parsedById = new LinkedHashMap<>();
		Map<String, DrinkDefinition> parsedByAlias = new HashMap<>();

		for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
			ResourceLocation id = entry.getKey();
			try {
				JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), DIRECTORY + "/" + id);
				if (json.has("enabled") && !GsonHelper.getAsBoolean(json, "enabled")) {
					continue;
				}

				DrinkDefinition drink = parseDrink(id, json);
				parsedById.put(id, drink);
				for (String alias : drink.aliases()) {
					parsedByAlias.put(normalize(alias), drink);
				}
			} catch (Exception exception) {
				ScpAdditionsMod.LOGGER.error("Failed to load SCP-294 drink definition {}", id, exception);
			}
		}

		drinksById = Collections.unmodifiableMap(parsedById);
		drinksByAlias = Collections.unmodifiableMap(parsedByAlias);
		ScpAdditionsMod.LOGGER.info("Loaded {} SCP-294 drink definitions", drinksById.size());
	}

	private static DrinkDefinition parseDrink(ResourceLocation id, JsonObject json) {
		List<String> aliases = readAliases(id, json);
		JsonObject result = GsonHelper.getAsJsonObject(json, "result");
		ResourceLocation resultItem = new ResourceLocation(GsonHelper.getAsString(result, "item"));
		int resultCount = Math.max(1, GsonHelper.getAsInt(result, "count", 1));
		int delayTicks = Math.max(0, GsonHelper.getAsInt(json, "delay_ticks", 40));
		ResourceLocation sound = new ResourceLocation(GsonHelper.getAsString(json, "sound", "scp_additions:scp294pouring"));
		boolean consumesCoin = GsonHelper.getAsBoolean(json, "consumes_coin", true);

		return new DrinkDefinition(id, aliases, resultItem, resultCount, delayTicks, sound, consumesCoin);
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

	public static Optional<DrinkDefinition> findByAlias(String rawInput) {
		return Optional.ofNullable(drinksByAlias.get(normalize(rawInput)));
	}

	public static ItemStack createResult(DrinkDefinition drink) {
		Item item = ForgeRegistries.ITEMS.getValue(drink.resultItem());
		if (item == null || item == Items.AIR) {
			ScpAdditionsMod.LOGGER.warn("SCP-294 drink {} points to missing item {}", drink.id(), drink.resultItem());
			return ItemStack.EMPTY;
		}
		return new ItemStack(item, drink.resultCount());
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	public record DrinkDefinition(ResourceLocation id, List<String> aliases, ResourceLocation resultItem, int resultCount, int delayTicks, ResourceLocation sound, boolean consumesCoin) {
	}
}