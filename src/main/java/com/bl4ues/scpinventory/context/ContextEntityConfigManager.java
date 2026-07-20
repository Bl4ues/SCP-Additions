package com.bl4ues.scpinventory.context;

import net.neoforged.fml.common.EventBusSubscriber;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.network.ContextConfigOpenPacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.mcreator.scpadditions.config.ConfigFilePersistence;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = "scp_additions")
public final class ContextEntityConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final double SELECT_REACH = 6.0D;
    private static final Map<UUID, EntitySession> ENTITY_SESSIONS = new HashMap<>();

    private ContextEntityConfigManager() {
    }

    public static void openGuiForLookedTarget(ServerPlayer player) {
        if (player == null) {
            return;
        }

        Entity entity = pickEntity(player);
        if (entity == null) {
            ENTITY_SESSIONS.remove(player.getUUID());
            ContextConfigManager.openGuiForLookedBlock(player);
            return;
        }

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null) {
            player.sendSystemMessage(Component.literal("[SCP Inventory] No entity selected.").withStyle(ChatFormatting.RED));
            return;
        }

        JsonObject root = loadRoot();
        JsonObject rule = findEntityRule(root, id);
        boolean existing = rule != null;
        if (rule == null) {
            rule = createDefaultEntityRule(id, entity.getDisplayName().getString());
        }

        ENTITY_SESSIONS.put(player.getUUID(), new EntitySession(id));
        ContextConfigOpenPacket packet = packetFromEntityRule(entity.blockPosition(), id, entity.getDisplayName().getString(), rule, existing);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static boolean saveClientRuleIfEntitySession(ServerPlayer player, BlockPos pos, String idText, String action, String name,
                                                        boolean showName, double range, boolean allowE, boolean allowRightClick, String useItem,
                                                        String clickFace, String rotateWith, double anchorX, double anchorY, double anchorZ) {
        if (player == null) {
            return false;
        }

        EntitySession session = ENTITY_SESSIONS.get(player.getUUID());
        ResourceLocation id = parseId(idText);
        if (session == null || id == null || !session.id().equals(id)) {
            return false;
        }

        JsonObject root = loadRoot();
        JsonObject rule = findEntityRule(root, id);
        if (rule == null) {
            rule = createDefaultEntityRule(id, name);
            interactions(root).add(rule);
        }

        rule.addProperty("type", "entity");
        rule.addProperty("id", id.toString());
        rule.addProperty("range", Math.max(0.25D, range));
        rule.addProperty("priority", 25);
        rule.addProperty("useItem", cleanUseItem(useItem));

        JsonObject text = object(rule, "text");
        text.addProperty("action", emptyTo(action, "Interact"));
        text.addProperty("nameMode", name == null || name.isBlank() ? "auto" : "manual");
        text.addProperty("name", name == null ? "" : name);
        text.addProperty("showAction", true);
        text.addProperty("showName", showName);

        JsonObject input = object(rule, "input");
        input.addProperty("allowE", allowE);
        input.addProperty("allowRightClick", allowRightClick);

        JsonObject click = object(rule, "click");
        click.addProperty("face", cleanClickFace(clickFace));

        JsonObject anchor = object(rule, "anchor");
        JsonArray position = new JsonArray();
        position.add(round(anchorX));
        position.add(round(anchorY));
        position.add(round(anchorZ));
        anchor.add("position", position);
        anchor.addProperty("rotateWith", cleanRotateWith(rotateWith));

        saveRoot(root);
        ContextInteractionRegistry.reload();
        player.sendSystemMessage(Component.literal("[SCP Inventory] Saved context interaction for entity ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(id.toString()).withStyle(ChatFormatting.AQUA)));
        return true;
    }

    public static boolean deleteClientRuleIfEntitySession(ServerPlayer player, String idText) {
        if (player == null) {
            return false;
        }

        EntitySession session = ENTITY_SESSIONS.get(player.getUUID());
        ResourceLocation id = parseId(idText);
        if (session == null || id == null || !session.id().equals(id)) {
            return false;
        }

        JsonObject root = loadRoot();
        JsonArray interactions = interactions(root);
        boolean removed = false;
        for (int i = interactions.size() - 1; i >= 0; i--) {
            JsonElement element = interactions.get(i);
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            if ("entity".equalsIgnoreCase(string(obj, "type", "")) && id.toString().equals(string(obj, "id", ""))) {
                interactions.remove(i);
                removed = true;
            }
        }

        if (removed) {
            saveRoot(root);
            ContextInteractionRegistry.reload();
            ENTITY_SESSIONS.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("[SCP Inventory] Deleted context interaction for entity ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(id.toString()).withStyle(ChatFormatting.AQUA)));
        } else {
            player.sendSystemMessage(Component.literal("[SCP Inventory] No entity context interaction exists for " + id + ".").withStyle(ChatFormatting.YELLOW));
        }
        return true;
    }

    private static Entity pickEntity(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 end = eye.add(look.scale(SELECT_REACH));
        double maxDistanceSqr = SELECT_REACH * SELECT_REACH;

        BlockHitResult blockHit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
            maxDistanceSqr = Math.min(maxDistanceSqr, eye.distanceToSqr(blockHit.getLocation()));
        }

        AABB area = player.getBoundingBox().expandTowards(look.scale(SELECT_REACH)).inflate(1.0D);
        Entity best = null;
        double bestDistance = maxDistanceSqr;
        for (Entity candidate : player.level().getEntities(player, area, entity -> entity.isAlive() && entity.isPickable())) {
            AABB box = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            Optional<Vec3> hit = box.clip(eye, end);
            double distance = box.contains(eye) ? 0.0D : hit.map(eye::distanceToSqr).orElse(Double.MAX_VALUE);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static ContextConfigOpenPacket packetFromEntityRule(BlockPos pos, ResourceLocation id, String entityName, JsonObject rule, boolean existing) {
        JsonObject text = object(rule, "text");
        JsonObject input = object(rule, "input");
        JsonObject click = object(rule, "click");
        JsonObject anchor = object(rule, "anchor");
        JsonArray position = positionArray(anchor);
        String name = string(text, "name", string(rule, "name", entityName));
        boolean showName = bool(text, "showName", bool(rule, "showName", !name.isBlank()));
        return new ContextConfigOpenPacket(
                pos,
                id.toString(),
                existing,
                string(text, "action", string(rule, "action", "Interact")),
                name,
                showName,
                number(rule, "range", 2.25D),
                bool(input, "allowE", bool(rule, "allowE", true)),
                bool(input, "allowRightClick", bool(rule, "allowRightClick", true)),
                cleanUseItem(string(rule, "useItem", "hand")),
                cleanClickFace(string(click, "face", string(rule, "clickFace", "front"))),
                cleanRotateWith(string(anchor, "rotateWith", "none")),
                position.get(0).getAsDouble(),
                position.get(1).getAsDouble(),
                position.get(2).getAsDouble()
        );
    }

    private static JsonObject createDefaultEntityRule(ResourceLocation id, String entityName) {
        JsonObject rule = new JsonObject();
        rule.addProperty("type", "entity");
        rule.addProperty("id", id.toString());
        rule.addProperty("range", 2.25D);
        rule.addProperty("priority", 25);
        rule.addProperty("useItem", "hand");

        JsonObject text = new JsonObject();
        text.addProperty("action", "Interact");
        text.addProperty("nameMode", entityName == null || entityName.isBlank() ? "auto" : "manual");
        text.addProperty("name", entityName == null ? "" : entityName);
        text.addProperty("showAction", true);
        text.addProperty("showName", true);
        rule.add("text", text);

        JsonObject anchor = new JsonObject();
        JsonArray position = new JsonArray();
        position.add(0.0D);
        position.add(0.5D);
        position.add(0.0D);
        anchor.add("position", position);
        anchor.addProperty("rotateWith", "none");
        rule.add("anchor", anchor);

        JsonObject input = new JsonObject();
        input.addProperty("allowE", true);
        input.addProperty("allowRightClick", true);
        rule.add("input", input);

        JsonObject click = new JsonObject();
        click.addProperty("face", "front");
        rule.add("click", click);
        return rule;
    }

    private static JsonObject findEntityRule(JsonObject root, ResourceLocation id) {
        for (JsonElement element : interactions(root)) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            if ("entity".equalsIgnoreCase(string(obj, "type", "")) && id.toString().equals(string(obj, "id", ""))) {
                return obj;
            }
        }
        return null;
    }

    private static JsonObject loadRoot() {
        try {
            File file = configFile();
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                JsonObject root = new JsonObject();
                root.addProperty("_comment", "Context interaction prompts for SCP Inventory. Edited in-game with /scpinventory context or config/scpinventory/context_interactions.json.");
                root.add("interactions", new JsonArray());
                saveRoot(root);
                return root;
            }
            JsonElement parsed = JsonParser.parseReader(new FileReader(file));
            JsonObject root = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            interactions(root);
            return root;
        } catch (Exception ex) {
            ex.printStackTrace();
            JsonObject root = new JsonObject();
            root.add("interactions", new JsonArray());
            return root;
        }
    }

    private static void saveRoot(JsonObject root) {
        try {
            File file = configFile();
            file.getParentFile().mkdirs();
            ConfigFilePersistence.writeWithBackup(file.toPath(),
                    GSON.toJson(root) + System.lineSeparator());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static File configFile() {
        return new File(new File("config/scpinventory"), "context_interactions.json");
    }

    private static JsonArray interactions(JsonObject root) {
        if (!root.has("interactions") || !root.get("interactions").isJsonArray()) {
            root.add("interactions", new JsonArray());
        }
        return root.getAsJsonArray("interactions");
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (!parent.has(key) || !parent.get(key).isJsonObject()) {
            parent.add(key, new JsonObject());
        }
        return parent.getAsJsonObject(key);
    }

    private static JsonArray positionArray(JsonObject anchor) {
        if (!anchor.has("position") || !anchor.get("position").isJsonArray()) {
            JsonArray position = new JsonArray();
            position.add(0.0D);
            position.add(0.5D);
            position.add(0.0D);
            anchor.add("position", position);
        }
        JsonArray position = anchor.getAsJsonArray("position");
        while (position.size() < 3) {
            position.add(0.0D);
        }
        return position;
    }

    private static ResourceLocation parseId(String idText) {
        try {
            return ResourceLocation.parse(idText);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static double number(JsonObject obj, String key, double fallback) {
        try {
            return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsDouble() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject obj, String key, boolean fallback) {
        try {
            return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsBoolean() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String string(JsonObject obj, String key, String fallback) {
        try {
            return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String emptyTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String cleanUseItem(String value) {
        String mode = value == null ? "hand" : value.toLowerCase(Locale.ROOT);
        return "card".equals(mode) ? "card" : "hand";
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

    private static double round(double value) {
        return Math.round(value * 1000.0D) / 1000.0D;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ENTITY_SESSIONS.remove(player.getUUID());
        }
    }

    private record EntitySession(ResourceLocation id) {
    }
}
