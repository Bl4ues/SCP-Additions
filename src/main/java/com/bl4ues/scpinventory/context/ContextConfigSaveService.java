package com.bl4ues.scpinventory.context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.scpadditions.config.ui.ConfigCenterService;

import java.util.Locale;

/**
 * Persists block rules edited by the K screen through the same authoritative
 * Forge config path, validation, backup, and reload flow used by the native
 * configuration center.
 */
public final class ContextConfigSaveService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ContextConfigSaveService() {
    }

    public static ConfigCenterService.SaveResult saveBlockRule(
            ServerPlayer player,
            BlockPos pos,
            String idText,
            String action,
            String name,
            boolean showName,
            double range,
            boolean allowE,
            boolean allowRightClick,
            String useItem,
            String clickFace,
            String rotateWith,
            double anchorX,
            double anchorY,
            double anchorZ) {
        if (player == null || pos == null) {
            return failure("Missing player or selected block position.");
        }

        ResourceLocation id;
        try {
            id = ResourceLocation.parse(idText);
        } catch (Exception exception) {
            return failure("Invalid block id: " + idText);
        }

        try {
            JsonObject snapshot = ConfigCenterService.snapshot();
            JsonObject root = snapshot.has(ConfigCenterService.CONTEXT)
                    && snapshot.get(ConfigCenterService.CONTEXT).isJsonObject()
                    ? snapshot.getAsJsonObject(ConfigCenterService.CONTEXT)
                    : new JsonObject();
            JsonArray interactions = interactions(root);
            JsonObject rule = findBlockRule(interactions, id);
            if (rule == null) {
                rule = createDefaultRule(id, player.level().getBlockState(pos));
                interactions.add(rule);
            }

            rule.addProperty("type", "block");
            rule.addProperty("id", id.toString());
            rule.addProperty("range", Math.max(0.25D, finite(range, 2.25D)));
            rule.addProperty("priority", 30);
            rule.addProperty("useItem", cleanUseItem(useItem));

            JsonObject text = object(rule, "text");
            text.addProperty("action", emptyTo(action, "Use"));
            text.addProperty("nameMode", name == null || name.isBlank() ? "auto" : "manual");
            text.addProperty("name", name == null ? "" : name);
            text.addProperty("showAction", true);
            text.addProperty("showName", showName);

            JsonObject input = object(rule, "input");
            input.addProperty("allowE", allowE);
            input.addProperty("allowRightClick", allowRightClick);

            object(rule, "click").addProperty("face", cleanClickFace(clickFace));

            JsonObject anchor = object(rule, "anchor");
            JsonArray position = new JsonArray();
            position.add(round(finite(anchorX, 0.5D)));
            position.add(round(finite(anchorY, 0.5D)));
            position.add(round(finite(anchorZ, 0.5D)));
            anchor.add("position", position);
            anchor.addProperty("rotateWith", cleanRotateWith(rotateWith));

            JsonObject changes = new JsonObject();
            changes.add(ConfigCenterService.CONTEXT, root);
            return ConfigCenterService.saveBatch(player, GSON.toJson(changes));
        } catch (Exception exception) {
            String message = exception.getMessage();
            return failure("Could not save context interaction: "
                    + (message == null || message.isBlank()
                    ? exception.getClass().getSimpleName() : message));
        }
    }

    private static JsonArray interactions(JsonObject root) {
        if (!root.has("interactions") || !root.get("interactions").isJsonArray()) {
            root.add("interactions", new JsonArray());
        }
        return root.getAsJsonArray("interactions");
    }

    private static JsonObject findBlockRule(JsonArray interactions, ResourceLocation id) {
        for (JsonElement element : interactions) {
            if (!element.isJsonObject()) continue;
            JsonObject rule = element.getAsJsonObject();
            if ("block".equalsIgnoreCase(string(rule, "type", ""))
                    && id.toString().equals(string(rule, "id", ""))) {
                return rule;
            }
        }
        return null;
    }

    private static JsonObject createDefaultRule(ResourceLocation id, BlockState state) {
        JsonObject rule = new JsonObject();
        rule.addProperty("type", "block");
        rule.addProperty("id", id.toString());
        rule.addProperty("range", 2.25D);
        rule.addProperty("priority", 30);
        rule.addProperty("useItem", "hand");

        JsonObject text = new JsonObject();
        text.addProperty("action", "Use");
        text.addProperty("nameMode", "manual");
        text.addProperty("name", state.getBlock().getName().getString());
        text.addProperty("showAction", true);
        text.addProperty("showName", true);
        rule.add("text", text);

        JsonObject input = new JsonObject();
        input.addProperty("allowE", true);
        input.addProperty("allowRightClick", true);
        rule.add("input", input);

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

    private static JsonObject object(JsonObject parent, String key) {
        if (!parent.has(key) || !parent.get(key).isJsonObject()) {
            parent.add(key, new JsonObject());
        }
        return parent.getAsJsonObject(key);
    }

    private static String string(JsonObject object, String key, String fallback) {
        try {
            return object.has(key) && !object.get(key).isJsonNull()
                    ? object.get(key).getAsString() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String emptyTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String cleanUseItem(String value) {
        return "card".equalsIgnoreCase(value) ? "card" : "hand";
    }

    private static String cleanClickFace(String value) {
        String face = value == null ? "front" : value.toLowerCase(Locale.ROOT);
        return switch (face) {
            case "back", "player", "north", "south", "east", "west", "up", "down" -> face;
            default -> "front";
        };
    }

    private static String cleanRotateWith(String value) {
        String mode = value == null ? "none" : value.toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "auto", "facing", "horizontal_facing", "axis" -> mode;
            default -> "none";
        };
    }

    private static double finite(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0D) / 1000.0D;
    }

    private static ConfigCenterService.SaveResult failure(String message) {
        return new ConfigCenterService.SaveResult(false, message, new JsonObject(), java.util.List.of());
    }
}
