package net.mcreator.scpadditions.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class Scp914RecipeManager extends SimpleJsonResourceReloadListener {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String DIRECTORY = "scp914/recipes";
	private static List<RecipeDefinition> recipes = List.of();

	public Scp914RecipeManager() {
		super(GSON, DIRECTORY);
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
		List<RecipeDefinition> parsed = new ArrayList<>();

		for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
			ResourceLocation id = entry.getKey();
			try {
				JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), DIRECTORY + "/" + id);
				if (json.has("enabled") && !GsonHelper.getAsBoolean(json, "enabled")) {
					continue;
				}
				parsed.add(parseRecipe(id, json));
			} catch (Exception exception) {
				ScpAdditionsMod.LOGGER.error("Failed to load SCP-914 recipe definition {}", id, exception);
			}
		}

		recipes = Collections.unmodifiableList(parsed);
		ScpAdditionsMod.LOGGER.info("Loaded {} SCP-914 recipe definitions", recipes.size());
	}

	private static RecipeDefinition parseRecipe(ResourceLocation id, JsonObject json) {
		Setting setting = Setting.fromSerializedName(GsonHelper.getAsString(json, "setting"));

		JsonObject input = GsonHelper.getAsJsonObject(json, "input");
		ResourceLocation inputItem = new ResourceLocation(GsonHelper.getAsString(input, "item"));
		int inputCount = Math.max(1, GsonHelper.getAsInt(input, "count", 1));

		JsonObject output = GsonHelper.getAsJsonObject(json, "output");
		ResourceLocation outputItem = new ResourceLocation(GsonHelper.getAsString(output, "item"));
		int outputCount = Math.max(1, GsonHelper.getAsInt(output, "count", 1));

		float chance = Math.max(0.0F, Math.min(1.0F, GsonHelper.getAsFloat(json, "chance", 1.0F)));
		boolean copyInputNbt = GsonHelper.getAsBoolean(json, "copy_input_nbt", false);

		return new RecipeDefinition(id, setting, inputItem, inputCount, outputItem, outputCount, chance, copyInputNbt);
	}

	public static Optional<RecipeDefinition> findRecipe(Setting setting, ItemStack inputStack) {
		if (inputStack.isEmpty()) {
			return Optional.empty();
		}

		ResourceLocation inputId = ForgeRegistries.ITEMS.getKey(inputStack.getItem());
		if (inputId == null) {
			return Optional.empty();
		}

		return recipes.stream()
				.filter(recipe -> recipe.setting() == setting)
				.filter(recipe -> recipe.inputItem().equals(inputId))
				.filter(recipe -> inputStack.getCount() >= recipe.inputCount())
				.findFirst();
	}

	public static ItemStack createResult(RecipeDefinition recipe, ItemStack inputStack) {
		Item item = ForgeRegistries.ITEMS.getValue(recipe.outputItem());
		if (item == null || item == Items.AIR) {
			ScpAdditionsMod.LOGGER.warn("SCP-914 recipe {} points to missing output item {}", recipe.id(), recipe.outputItem());
			return ItemStack.EMPTY;
		}

		ItemStack result = new ItemStack(item, recipe.outputCount());
		if (recipe.copyInputNbt() && inputStack.hasTag()) {
			result.setTag(inputStack.getTag().copy());
		}
		return result;
	}

	public enum Setting {
		ROUGH("rough"),
		COARSE("coarse"),
		ONE_TO_ONE("1_to_1"),
		FINE("fine"),
		VERY_FINE("very_fine");

		private final String serializedName;

		Setting(String serializedName) {
			this.serializedName = serializedName;
		}

		public String serializedName() {
			return serializedName;
		}

		public static Setting fromSerializedName(String value) {
			String normalized = value.trim().toLowerCase(Locale.ROOT);
			for (Setting setting : values()) {
				if (setting.serializedName.equals(normalized)) {
					return setting;
				}
			}
			throw new IllegalArgumentException("Unknown SCP-914 setting: " + value);
		}
	}

	public record RecipeDefinition(ResourceLocation id, Setting setting, ResourceLocation inputItem, int inputCount, ResourceLocation outputItem, int outputCount, float chance, boolean copyInputNbt) {
	}
}