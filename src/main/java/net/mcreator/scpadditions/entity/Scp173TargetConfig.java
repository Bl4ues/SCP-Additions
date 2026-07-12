package net.mcreator.scpadditions.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.loading.FMLPaths;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class Scp173TargetConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("scpinventory").resolve("scpinventory.json");
    private static final List<String> DEFAULT_TARGETS = List.of(
            "minecraft:villager",
            "minecraft:iron_golem",
            "minecraft:enderman",
            "minecraft:zombie",
            "minecraft:skeleton",
            "minecraft:pillager",
            "#minecraft:raiders"
    );
    private static volatile List<String> targets = DEFAULT_TARGETS;

    private Scp173TargetConfig() {
    }

    public static synchronized void load() {
        List<String> loaded = DEFAULT_TARGETS;
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject root = new JsonObject();
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH,
                        StandardCharsets.UTF_8)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (parsed.isJsonObject()) root = parsed.getAsJsonObject();
                }
            }
            if (root.has("scp_173_targets")
                    && root.get("scp_173_targets").isJsonArray()) {
                List<String> values = new ArrayList<>();
                for (JsonElement entry : root.getAsJsonArray("scp_173_targets")) {
                    String value = entry.isJsonPrimitive()
                            ? entry.getAsString().trim() : "";
                    if (!value.isBlank()) values.add(value);
                }
                if (!values.isEmpty()) loaded = Collections.unmodifiableList(values);
            } else {
                JsonArray values = new JsonArray();
                DEFAULT_TARGETS.forEach(values::add);
                root.add("scp_173_targets", values);
                try (Writer writer = Files.newBufferedWriter(CONFIG_PATH,
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)) {
                    GSON.toJson(root, writer);
                }
            }
        } catch (Exception exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Failed to load SCP-173 target configuration from {}",
                    CONFIG_PATH, exception);
        }
        targets = loaded;
    }

    public static boolean isConfiguredTarget(LivingEntity entity) {
        if (entity == null || entity instanceof Scp173Entity || !entity.isAlive())
            return false;
        if (entity instanceof AbstractScp131Entity) return true;
        EntityType<?> type = entity.getType();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) return false;
        for (String rule : targets) {
            if (matchesRule(type, id, rule)) return true;
        }
        return false;
    }

    private static boolean matchesRule(EntityType<?> type, ResourceLocation id,
            String rawRule) {
        if (rawRule == null || rawRule.isBlank()) return false;
        String rule = rawRule.trim();
        if (rule.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(rule.substring(1));
            return tagId != null && type.builtInRegistryHolder()
                    .is(TagKey.create(Registries.ENTITY_TYPE, tagId));
        }
        ResourceLocation exactId = ResourceLocation.tryParse(rule);
        if (exactId != null) return exactId.equals(id);
        String lower = rule.toLowerCase(Locale.ROOT);
        return id.getPath().equals(lower) || id.toString().equals(lower);
    }
}
