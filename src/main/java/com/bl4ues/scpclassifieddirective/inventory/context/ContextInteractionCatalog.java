package com.bl4ues.scpclassifieddirective.inventory.context;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Read-only presentation catalog for contextual interactions.
 *
 * <p>The runtime intentionally keeps configured and integrated rules separate.
 * The Configuration Center needs the same distinction without mutating an old
 * external config just so newly bundled defaults become visible. This class
 * exposes that merged view and labels explicit overrides of integrated rules.</p>
 */
public final class ContextInteractionCatalog {
    private static final CatalogData DATA = loadBundledData();

    private ContextInteractionCatalog() {
    }

    /**
     * Returns every integrated base target shipped by the mod, including native
     * item-specific targets whose base interaction is synthesized at runtime.
     */
    public static List<JsonObject> integratedBaseRules() {
        LinkedHashMap<TargetKey, JsonObject> result = new LinkedHashMap<>();
        for (Map.Entry<TargetKey, JsonObject> entry : DATA.baseRules.entrySet()) {
            result.put(entry.getKey(), entry.getValue().deepCopy());
        }
        for (NativeContextVariants.NativeTarget target
                : NativeContextVariants.nativeTargets()) {
            TargetKey key = new TargetKey(target.type().toLowerCase(Locale.ROOT),
                    target.id().toString(), "");
            result.putIfAbsent(key, syntheticNativeBase(target));
        }
        return result.values().stream().map(JsonObject::deepCopy).toList();
    }

    public static EntryView inspect(JsonObject rule, boolean configured) {
        JsonObject safeRule = rule == null ? new JsonObject() : rule;
        TargetKey key = targetKey(safeRule);
        String type = key.type();
        ResourceLocation id = parseId(key.id());

        JsonObject integratedBase = DATA.baseRules.get(key);
        if (integratedBase == null && id != null
                && NativeContextVariants.isNativeTarget(type, id)) {
            integratedBase = syntheticNativeBase(
                    new NativeContextVariants.NativeTarget(type, id));
        }

        Source baseSource;
        if (!configured) {
            baseSource = Source.INTEGRATED;
        } else if (integratedBase == null) {
            baseSource = Source.CUSTOM;
        } else if (baseEquivalent(safeRule, integratedBase)) {
            baseSource = Source.INTEGRATED;
        } else {
            baseSource = Source.OVERRIDE;
        }

        LinkedHashMap<String, JsonObject> integratedVariants =
                integratedVariants(type, id, integratedBase);
        LinkedHashMap<String, JsonObject> configuredVariants = configured
                ? explicitVariants(safeRule) : new LinkedHashMap<>();

        List<VariantView> variants = new ArrayList<>();
        Set<String> consumed = new LinkedHashSet<>();
        for (Map.Entry<String, JsonObject> entry : configuredVariants.entrySet()) {
            String interactionId = entry.getKey();
            JsonObject definition = entry.getValue();
            JsonObject integratedDefinition = integratedVariants.get(interactionId);
            Source source = integratedDefinition == null ? Source.CUSTOM
                    : variantEquivalent(definition, integratedDefinition)
                    ? Source.INTEGRATED : Source.OVERRIDE;
            variants.add(new VariantView(interactionId, source,
                    definition.deepCopy(), effective(safeRule, definition)));
            consumed.add(interactionId);
        }
        for (Map.Entry<String, JsonObject> entry : integratedVariants.entrySet()) {
            if (consumed.contains(entry.getKey())) continue;
            JsonObject definition = entry.getValue();
            variants.add(new VariantView(entry.getKey(), Source.INTEGRATED,
                    definition.deepCopy(), effective(safeRule, definition)));
        }

        return new EntryView(baseSource, configured,
                integratedBase != null, safeRule.deepCopy(),
                integratedBase == null ? null : integratedBase.deepCopy(),
                List.copyOf(variants));
    }

    public static boolean sameIdentity(JsonObject first, JsonObject second) {
        return targetKey(first).equals(targetKey(second));
    }

    public static String identity(JsonObject rule) {
        TargetKey key = targetKey(rule);
        return key.type() + "|" + key.id() + "|" + key.interactionId();
    }

    private static CatalogData loadBundledData() {
        LinkedHashMap<TargetKey, JsonObject> bases = new LinkedHashMap<>();
        try {
            JsonElement parsed = JsonParser.parseString(
                    DefaultContextInteractions.loadBundledConfig());
            if (!parsed.isJsonObject()) return new CatalogData(bases);
            JsonObject root = parsed.getAsJsonObject();
            if (!root.has("interactions")
                    || !root.get("interactions").isJsonArray()) {
                return new CatalogData(bases);
            }
            for (JsonElement element : root.getAsJsonArray("interactions")) {
                if (!element.isJsonObject()) continue;
                JsonObject rule = element.getAsJsonObject();
                TargetKey key = targetKey(rule);
                if (key.type().isBlank() || key.id().isBlank()) continue;
                bases.putIfAbsent(key, rule.deepCopy());
            }
        } catch (Exception ignored) {
            // Runtime has its own emergency fallback. The configuration center
            // simply loses source decoration if the bundled file is unreadable.
        }
        return new CatalogData(bases);
    }

    private static LinkedHashMap<String, JsonObject> integratedVariants(
            String type, ResourceLocation id, JsonObject integratedBase) {
        LinkedHashMap<String, JsonObject> result = new LinkedHashMap<>();
        if (integratedBase != null && integratedBase.has("variants")
                && integratedBase.get("variants").isJsonArray()) {
            int generated = 1;
            for (JsonElement element : integratedBase.getAsJsonArray("variants")) {
                if (!element.isJsonObject()) continue;
                JsonObject variant = element.getAsJsonObject();
                String key = string(variant, "interactionId", "").trim();
                if (key.isBlank()) key = "integrated_variant_" + generated++;
                result.putIfAbsent(key, variant.deepCopy());
            }
        }
        if (id != null && NativeContextVariants.isNativeTarget(type, id)) {
            JsonObject nativeVariant = NativeContextVariants.nativeVariant(type, id);
            String key = string(nativeVariant, "interactionId", "native").trim();
            result.putIfAbsent(key, nativeVariant);
        }
        return result;
    }

    private static LinkedHashMap<String, JsonObject> explicitVariants(
            JsonObject rule) {
        LinkedHashMap<String, JsonObject> result = new LinkedHashMap<>();
        if (rule == null || !rule.has("variants")
                || !rule.get("variants").isJsonArray()) return result;
        int generated = 1;
        for (JsonElement element : rule.getAsJsonArray("variants")) {
            if (!element.isJsonObject()) continue;
            JsonObject variant = element.getAsJsonObject();
            String key = string(variant, "interactionId", "").trim();
            if (key.isBlank()) key = "variant_" + generated++;
            result.put(key, variant.deepCopy());
        }
        return result;
    }

    private static JsonObject effective(JsonObject base, JsonObject variant) {
        JsonObject result = base == null ? new JsonObject() : base.deepCopy();
        result.remove("variants");
        deepMerge(result, variant);
        result.remove("variants");
        return result;
    }

    private static void deepMerge(JsonObject target, JsonObject overlay) {
        for (Map.Entry<String, JsonElement> entry : overlay.entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonObject() && target.has(entry.getKey())
                    && target.get(entry.getKey()).isJsonObject()) {
                deepMerge(target.getAsJsonObject(entry.getKey()),
                        value.getAsJsonObject());
            } else {
                target.add(entry.getKey(), value.deepCopy());
            }
        }
    }

    private static boolean baseEquivalent(JsonObject configured,
            JsonObject integrated) {
        JsonObject left = configured.deepCopy();
        JsonObject right = integrated.deepCopy();
        left.remove("variants");
        right.remove("variants");
        return left.equals(right);
    }

    private static boolean variantEquivalent(JsonObject configured,
            JsonObject integrated) {
        return configured.equals(integrated);
    }

    private static TargetKey targetKey(JsonObject rule) {
        String type = string(rule, "type", "").trim()
                .toLowerCase(Locale.ROOT);
        String idText = string(rule, "id", "").trim();
        ResourceLocation id = parseId(idText);
        if (id != null) idText = id.toString();
        String interactionId = string(rule, "interactionId",
                string(rule, "interactionKey", "")).trim();
        return new TargetKey(type, idText, interactionId);
    }

    private static JsonObject syntheticNativeBase(
            NativeContextVariants.NativeTarget target) {
        boolean entity = "entity".equalsIgnoreCase(target.type());
        JsonObject rule = new JsonObject();
        rule.addProperty("type", entity ? "entity" : "block");
        rule.addProperty("id", target.id().toString());
        rule.addProperty("range", 2.25D);
        rule.addProperty("priority", entity ? 25 : 30);
        rule.addProperty("useItem", "hand");

        JsonObject text = new JsonObject();
        text.addProperty("action", entity ? "Interact" : "Use");
        text.addProperty("nameMode", "auto");
        text.addProperty("name", "");
        text.addProperty("showAction", true);
        text.addProperty("showName", true);
        rule.add("text", text);

        JsonObject input = new JsonObject();
        input.addProperty("allowE", true);
        input.addProperty("allowRightClick", true);
        rule.add("input", input);

        JsonObject visual = new JsonObject();
        visual.addProperty("allowOffscreen", false);
        rule.add("visual", visual);

        JsonObject click = new JsonObject();
        click.addProperty("face", "front");
        rule.add("click", click);

        JsonObject anchor = new JsonObject();
        JsonArray position = new JsonArray();
        position.add(0.5D);
        position.add(0.5D);
        position.add(0.5D);
        anchor.add("position", position);
        anchor.addProperty("rotateWith", "none");
        rule.add("anchor", anchor);
        return rule;
    }

    private static ResourceLocation parseId(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new ResourceLocation(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String string(JsonObject object, String key,
            String fallback) {
        try {
            return object != null && object.has(key)
                    && !object.get(key).isJsonNull()
                    ? object.get(key).getAsString() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private record CatalogData(Map<TargetKey, JsonObject> baseRules) {
    }

    private record TargetKey(String type, String id, String interactionId) {
    }

    public enum Source {
        INTEGRATED,
        CUSTOM,
        OVERRIDE
    }

    public record EntryView(Source source, boolean configured,
            boolean hasIntegratedBase, JsonObject effectiveBase,
            JsonObject integratedBase, List<VariantView> variants) {
        public int variantCount() {
            return variants.size();
        }

        public int itemVariantCount() {
            int count = 0;
            for (VariantView variant : variants) {
                if (!variant.requiredItem().isBlank()) count++;
            }
            return count;
        }

        public List<String> requiredItems() {
            LinkedHashSet<String> items = new LinkedHashSet<>();
            for (VariantView variant : variants) {
                String item = variant.requiredItem();
                if (!item.isBlank()) items.add(item);
            }
            return List.copyOf(items);
        }
    }

    public record VariantView(String interactionId, Source source,
            JsonObject definition, JsonObject effective) {
        public String action() {
            JsonObject text = object(effective, "text");
            return string(text, "action", string(effective, "action", "Use"));
        }

        public String requiredItem() {
            JsonObject input = object(effective, "input");
            return string(input, "requiredItem",
                    string(effective, "requiredItem", "")).trim();
        }

        public String icon() {
            JsonObject visual = object(effective, "visual");
            return string(visual, "icon",
                    string(effective, "icon",
                            string(effective, "useItem", "hand")));
        }

        public boolean enabled() {
            return bool(effective, "enabled", true);
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        return parent != null && parent.has(key)
                && parent.get(key).isJsonObject()
                ? parent.getAsJsonObject(key) : new JsonObject();
    }

    private static boolean bool(JsonObject object, String key,
            boolean fallback) {
        try {
            return object != null && object.has(key)
                    && !object.get(key).isJsonNull()
                    ? object.get(key).getAsBoolean() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
