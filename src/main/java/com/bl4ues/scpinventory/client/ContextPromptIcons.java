package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ContextPromptIcons {
    public static final ResourceLocation DEFAULT_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/pickup.png");
    public static final ResourceLocation CARD_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/card.png");

    private static final Map<ResourceLocation, ResourceLocation> ICONS = new HashMap<>();
    private static boolean loaded = false;

    private ContextPromptIcons() {
    }

    public static ResourceLocation get(ResourceLocation id) {
        ensureLoaded();
        return ICONS.getOrDefault(id, DEFAULT_ICON);
    }

    public static ResourceLocation resolve(String iconOrMode, ResourceLocation fallbackId) {
        ensureLoaded();
        String text = iconOrMode == null ? "" : iconOrMode.trim();
        if (!text.isEmpty()) {
            return resolveIcon(text);
        }
        return fallbackId == null ? DEFAULT_ICON : ICONS.getOrDefault(fallbackId, DEFAULT_ICON);
    }

    public static void reload() {
        loaded = false;
        load();
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private static void load() {
        ICONS.clear();

        File file = new File("config/scpinventory/context_interactions.json");
        if (!file.exists()) {
            loaded = true;
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray interactions = root.has("interactions") && root.get("interactions").isJsonArray()
                    ? root.getAsJsonArray("interactions")
                    : new JsonArray();

            for (JsonElement element : interactions) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                String idText = getString(obj, "id", "");
                if (idText.isEmpty()) {
                    continue;
                }
                String iconText = getString(obj, "icon", "");
                if (iconText.isEmpty()) {
                    iconText = getString(getObject(obj, "visual"), "icon", "");
                }
                if (iconText.isEmpty()) {
                    iconText = getString(obj, "useItem", "");
                }
                if (iconText.isEmpty()) {
                    continue;
                }

                ResourceLocation id = new ResourceLocation(idText);
                ICONS.put(id, resolveIcon(iconText));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        loaded = true;
    }

    private static ResourceLocation resolveIcon(String value) {
        String text = value.trim();
        String lower = text.toLowerCase(Locale.ROOT);
        if ("pickup".equals(lower) || "default".equals(lower) || "hand".equals(lower)) {
            return DEFAULT_ICON;
        }
        if ("card".equals(lower)) {
            return CARD_ICON;
        }
        return new ResourceLocation(text);
    }

    private static JsonObject getObject(JsonObject obj, String key) {
        return obj != null && obj.has(key) && obj.get(key).isJsonObject() ? obj.getAsJsonObject(key) : new JsonObject();
    }

    private static String getString(JsonObject obj, String key, String fallback) {
        try {
            return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
