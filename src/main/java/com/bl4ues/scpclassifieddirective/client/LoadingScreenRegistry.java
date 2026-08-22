package com.bl4ues.scpclassifieddirective.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Data-driven loading-screen cards. Add a JSON file below
 * assets/scp_classified_directive/loading_screens/ and a matching texture to extend the
 * rotation without touching Java code.
 */
public final class LoadingScreenRegistry {
    private static final String DIRECTORY = "loading_screens";
    private static final Random RANDOM = new Random();

    private LoadingScreenRegistry() {
    }

    public static List<Definition> loadDefinitions() {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                DIRECTORY,
                location -> location.getPath().endsWith(".json"));

        List<Definition> definitions = new ArrayList<>();
        resources.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> {
                    try (Reader reader = entry.getValue().openAsReader()) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        definitions.add(parse(entry.getKey(), json));
                    } catch (Exception exception) {
                        ScpClassifiedDirectiveMod.LOGGER.warn(
                                "Ignoring invalid custom loading screen definition {}",
                                entry.getKey(), exception);
                    }
                });
        return List.copyOf(definitions);
    }

    public static Definition choose(List<Definition> definitions, ResourceLocation excludedId) {
        if (definitions == null || definitions.isEmpty()) return null;

        List<Definition> candidates = definitions.stream()
                .filter(definition -> definitions.size() <= 1
                        || excludedId == null
                        || !definition.id().equals(excludedId))
                .toList();
        if (candidates.isEmpty()) candidates = definitions;

        double totalWeight = candidates.stream()
                .mapToDouble(definition -> Math.max(0.0D, definition.weight()))
                .sum();
        if (totalWeight <= 0.0D) {
            return candidates.get(RANDOM.nextInt(candidates.size()));
        }

        double roll = RANDOM.nextDouble() * totalWeight;
        for (Definition definition : candidates) {
            roll -= Math.max(0.0D, definition.weight());
            if (roll <= 0.0D) return definition;
        }
        return candidates.get(candidates.size() - 1);
    }

    private static Definition parse(ResourceLocation source, JsonObject json) {
        String fallbackId = source.getNamespace() + ":" + source.getPath()
                .substring((DIRECTORY + "/").length(), source.getPath().length() - ".json".length());
        ResourceLocation id = new ResourceLocation(string(json, "id", fallbackId));
        ResourceLocation texture = new ResourceLocation(requiredString(json, "texture"));
        DescriptionAnchor anchor = DescriptionAnchor.from(string(json, "description_anchor", "center"));
        double weight = json.has("weight") ? json.get("weight").getAsDouble() : 1.0D;

        return new Definition(
                id,
                texture,
                requiredString(json, "left_title"),
                requiredString(json, "left_subtitle"),
                requiredString(json, "right_label"),
                requiredString(json, "right_value"),
                requiredString(json, "description"),
                anchor,
                weight);
    }

    private static String requiredString(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing string field '" + key + "'");
        }
        return json.get(key).getAsString();
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive()
                ? json.get(key).getAsString()
                : fallback;
    }

    public record Definition(
            ResourceLocation id,
            ResourceLocation texture,
            String leftTitle,
            String leftSubtitle,
            String rightLabel,
            String rightValue,
            String description,
            DescriptionAnchor descriptionAnchor,
            double weight) {
    }

    public enum DescriptionAnchor {
        LEFT,
        CENTER;

        private static DescriptionAnchor from(String value) {
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {
                return CENTER;
            }
        }
    }
}
