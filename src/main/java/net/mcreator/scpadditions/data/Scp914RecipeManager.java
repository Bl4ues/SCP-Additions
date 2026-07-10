package net.mcreator.scpadditions.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class Scp914RecipeManager {
	private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("scpadditions").resolve("914recipes.json");
	private static final String DEFAULT_CONFIG = """
			{
			  "version": 1,
			  "machine": {
			    "intake_offset": [-4, 0, -3],
			    "output_offset": [4, 0, -3],
			    "search_radius": 1.5,
			    "start_delay_ticks": 30,
			    "finish_delay_ticks": 160
			  },
			  "recipes": [
			    {
			      "id": "scp_additions:level_1_keycard_rough",
			      "enabled": true,
			      "setting": "rough",
			      "item_inputs": [
			        {
			          "item": "scp_additions:level_1_keycard",
			          "count": 1
			        }
			      ],
			      "item_outputs": [
			        {
			          "item": "scp_additions:pieces_of_paper",
			          "count": 1
			        }
			      ],
			      "entity_inputs": [],
			      "entity_outputs": [],
			      "chance": 1.0,
			      "copy_input_nbt": false
			    }
			  ]
			}
			""";

	private static List<RecipeDefinition> recipes = List.of();
	private static MachineConfig machineConfig = MachineConfig.defaults();

	private Scp914RecipeManager() {
	}

	public static synchronized void loadFromConfig() {
		ensureConfigExists();
		List<RecipeDefinition> parsed = new ArrayList<>();

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			machineConfig = readMachineConfig(root);
			JsonArray recipesArray = root.has("recipes") ? GsonHelper.getAsJsonArray(root, "recipes") : new JsonArray();

			for (JsonElement element : recipesArray) {
				try {
					JsonObject json = GsonHelper.convertToJsonObject(element, "SCP-914 recipe");
					if (json.has("enabled") && !GsonHelper.getAsBoolean(json, "enabled")) {
						continue;
					}
					parsed.add(parseRecipe(json));
				} catch (Exception exception) {
					ScpAdditionsMod.LOGGER.error("Failed to load one SCP-914 recipe entry", exception);
				}
			}
		} catch (Exception exception) {
			ScpAdditionsMod.LOGGER.error("Failed to load SCP-914 recipes from {}", CONFIG_PATH, exception);
		}

		recipes = List.copyOf(parsed);
		ScpAdditionsMod.LOGGER.info("Loaded {} SCP-914 recipe definitions from config", recipes.size());
	}

	private static void ensureConfigExists() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			if (Files.notExists(CONFIG_PATH)) {
				Files.writeString(CONFIG_PATH, DEFAULT_CONFIG, StandardCharsets.UTF_8);
			}
		} catch (IOException exception) {
			ScpAdditionsMod.LOGGER.error("Failed to create SCP-914 config at {}", CONFIG_PATH, exception);
		}
	}

	private static MachineConfig readMachineConfig(JsonObject root) {
		if (!root.has("machine")) {
			return MachineConfig.defaults();
		}

		JsonObject json = GsonHelper.getAsJsonObject(root, "machine");
		return new MachineConfig(
				readOffset(json, "intake_offset", -4, 0, -3),
				readOffset(json, "output_offset", 4, 0, -3),
				Math.max(0.5D, GsonHelper.getAsDouble(json, "search_radius", 1.5D)),
				Math.max(0, GsonHelper.getAsInt(json, "start_delay_ticks", 30)),
				Math.max(0, GsonHelper.getAsInt(json, "finish_delay_ticks", 160)));
	}

	private static Offset readOffset(JsonObject json, String name, int defaultX, int defaultY, int defaultZ) {
		if (!json.has(name)) {
			return new Offset(defaultX, defaultY, defaultZ);
		}

		JsonArray array = GsonHelper.getAsJsonArray(json, name);
		if (array.size() != 3) {
			throw new IllegalArgumentException(name + " must have exactly 3 integers");
		}
		return new Offset(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
	}

	private static RecipeDefinition parseRecipe(JsonObject json) {
		ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "id"));
		Setting setting = Setting.fromSerializedName(GsonHelper.getAsString(json, "setting"));
		List<ItemIngredient> itemInputs = readItemInputs(json);
		List<EntityIngredient> entityInputs = readEntityInputs(json);
		List<ItemOutput> itemOutputs = readItemOutputs(json);
		List<EntityOutput> entityOutputs = readEntityOutputs(json);
		float chance = Math.max(0.0F, Math.min(1.0F, GsonHelper.getAsFloat(json, "chance", 1.0F)));
		boolean copyInputNbt = GsonHelper.getAsBoolean(json, "copy_input_nbt", false);
		String actionbar = GsonHelper.getAsString(json, "actionbar", "");

		if (itemInputs.isEmpty() && entityInputs.isEmpty()) {
			throw new IllegalArgumentException("SCP-914 recipe " + id + " has no inputs");
		}
		if (itemOutputs.isEmpty() && entityOutputs.isEmpty()) {
			throw new IllegalArgumentException("SCP-914 recipe " + id + " has no outputs");
		}

		return new RecipeDefinition(id, setting, itemInputs, entityInputs, itemOutputs, entityOutputs, chance, copyInputNbt, actionbar);
	}

	private static List<ItemIngredient> readItemInputs(JsonObject json) {
		List<ItemIngredient> inputs = new ArrayList<>();
		if (json.has("input")) {
			JsonObject input = GsonHelper.getAsJsonObject(json, "input");
			inputs.add(new ItemIngredient(new ResourceLocation(GsonHelper.getAsString(input, "item")), Math.max(1, GsonHelper.getAsInt(input, "count", 1))));
		}
		if (json.has("item_inputs")) {
			JsonArray array = GsonHelper.getAsJsonArray(json, "item_inputs");
			for (JsonElement element : array) {
				JsonObject input = GsonHelper.convertToJsonObject(element, "SCP-914 item input");
				inputs.add(new ItemIngredient(new ResourceLocation(GsonHelper.getAsString(input, "item")), Math.max(1, GsonHelper.getAsInt(input, "count", 1))));
			}
		}
		return List.copyOf(inputs);
	}

	private static List<EntityIngredient> readEntityInputs(JsonObject json) {
		List<EntityIngredient> inputs = new ArrayList<>();
		if (json.has("entity_inputs")) {
			JsonArray array = GsonHelper.getAsJsonArray(json, "entity_inputs");
			for (JsonElement element : array) {
				JsonObject input = GsonHelper.convertToJsonObject(element, "SCP-914 entity input");
				inputs.add(new EntityIngredient(new ResourceLocation(GsonHelper.getAsString(input, "entity")), Math.max(1, GsonHelper.getAsInt(input, "count", 1)), GsonHelper.getAsBoolean(input, "consume", true)));
			}
		}
		return List.copyOf(inputs);
	}

	private static List<ItemOutput> readItemOutputs(JsonObject json) {
		List<ItemOutput> outputs = new ArrayList<>();
		if (json.has("output")) {
			JsonObject output = GsonHelper.getAsJsonObject(json, "output");
			outputs.add(new ItemOutput(new ResourceLocation(GsonHelper.getAsString(output, "item")), Math.max(1, GsonHelper.getAsInt(output, "count", 1))));
		}
		if (json.has("item_outputs")) {
			JsonArray array = GsonHelper.getAsJsonArray(json, "item_outputs");
			for (JsonElement element : array) {
				JsonObject output = GsonHelper.convertToJsonObject(element, "SCP-914 item output");
				outputs.add(new ItemOutput(new ResourceLocation(GsonHelper.getAsString(output, "item")), Math.max(1, GsonHelper.getAsInt(output, "count", 1))));
			}
		}
		return List.copyOf(outputs);
	}

	private static List<EntityOutput> readEntityOutputs(JsonObject json) {
		List<EntityOutput> outputs = new ArrayList<>();
		if (json.has("entity_outputs")) {
			JsonArray array = GsonHelper.getAsJsonArray(json, "entity_outputs");
			for (JsonElement element : array) {
				JsonObject output = GsonHelper.convertToJsonObject(element, "SCP-914 entity output");
				outputs.add(new EntityOutput(new ResourceLocation(GsonHelper.getAsString(output, "entity")), Math.max(1, GsonHelper.getAsInt(output, "count", 1))));
			}
		}
		return List.copyOf(outputs);
	}

	public static Optional<RecipeMatch> findRecipe(Setting setting, List<ItemEntity> itemEntities, List<Entity> entities) {
		return recipes.stream()
				.filter(recipe -> recipe.setting() == setting)
				.map(recipe -> match(recipe, itemEntities, entities))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst();
	}

	private static Optional<RecipeMatch> match(RecipeDefinition recipe, List<ItemEntity> itemEntities, List<Entity> entities) {
		List<ItemUse> itemUses = new ArrayList<>();
		Map<ItemEntity, Integer> reservedItemCounts = new HashMap<>();

		for (ItemIngredient ingredient : recipe.itemInputs()) {
			int remaining = ingredient.count();
			for (ItemEntity itemEntity : itemEntities) {
				ItemStack stack = itemEntity.getItem();
				ResourceLocation stackId = ForgeRegistries.ITEMS.getKey(stack.getItem());
				if (!ingredient.item().equals(stackId)) {
					continue;
				}
				int alreadyReserved = reservedItemCounts.getOrDefault(itemEntity, 0);
				int available = stack.getCount() - alreadyReserved;
				if (available <= 0) {
					continue;
				}
				int used = Math.min(available, remaining);
				reservedItemCounts.put(itemEntity, alreadyReserved + used);
				itemUses.add(new ItemUse(itemEntity, used));
				remaining -= used;
				if (remaining <= 0) {
					break;
				}
			}
			if (remaining > 0) {
				return Optional.empty();
			}
		}

		List<EntityUse> entityUses = new ArrayList<>();
		List<Entity> reservedEntities = new ArrayList<>();
		for (EntityIngredient ingredient : recipe.entityInputs()) {
			int remaining = ingredient.count();
			for (Entity entity : entities) {
				if (reservedEntities.contains(entity)) {
					continue;
				}
				ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
				if (!ingredient.entity().equals(entityId)) {
					continue;
				}
				reservedEntities.add(entity);
				entityUses.add(new EntityUse(entity, ingredient.consume()));
				remaining--;
				if (remaining <= 0) {
					break;
				}
			}
			if (remaining > 0) {
				return Optional.empty();
			}
		}

		return Optional.of(new RecipeMatch(recipe, List.copyOf(itemUses), List.copyOf(entityUses)));
	}

	public static ItemStack createItemOutput(ItemOutput output, ItemStack inputStack, boolean copyInputNbt) {
		Item item = ForgeRegistries.ITEMS.getValue(output.item());
		if (item == null || item == Items.AIR) {
			ScpAdditionsMod.LOGGER.warn("SCP-914 output points to missing item {}", output.item());
			return ItemStack.EMPTY;
		}

		ItemStack result = new ItemStack(item, output.count());
		if (copyInputNbt && inputStack != null && inputStack.hasTag()) {
			result.setTag(inputStack.getTag().copy());
		}
		return result;
	}

	public static Optional<EntityType<?>> getEntityType(EntityOutput output) {
		EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(output.entity());
		return Optional.ofNullable(type);
	}

	public static MachineConfig machineConfig() {
		return machineConfig;
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

	public record RecipeDefinition(ResourceLocation id, Setting setting, List<ItemIngredient> itemInputs, List<EntityIngredient> entityInputs, List<ItemOutput> itemOutputs, List<EntityOutput> entityOutputs, float chance,
			boolean copyInputNbt, String actionbar) {
	}

	public record ItemIngredient(ResourceLocation item, int count) {
	}

	public record EntityIngredient(ResourceLocation entity, int count, boolean consume) {
	}

	public record ItemOutput(ResourceLocation item, int count) {
	}

	public record EntityOutput(ResourceLocation entity, int count) {
	}

	public record ItemUse(ItemEntity entity, int count) {
	}

	public record EntityUse(Entity entity, boolean consume) {
	}

	public record RecipeMatch(RecipeDefinition recipe, List<ItemUse> itemUses, List<EntityUse> entityUses) {
		public ItemStack firstInputStack() {
			return itemUses.isEmpty() ? ItemStack.EMPTY : itemUses.get(0).entity().getItem();
		}
	}

	public record MachineConfig(Offset intakeOffset, Offset outputOffset, double searchRadius, int startDelayTicks, int finishDelayTicks) {
		public static MachineConfig defaults() {
			return new MachineConfig(new Offset(-4, 0, -3), new Offset(4, 0, -3), 1.5D, 30, 160);
		}
	}

	public record Offset(int x, int y, int z) {
	}
}