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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = "scp_additions")
public final class ContextConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String BUNDLED_CONFIG = "config/scpinventory/context_interactions.json";
    private static final double SELECT_REACH = 6.0D;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, PendingSelection> PENDING = new HashMap<>();

    private ContextConfigManager() {
    }

    public static void openGuiForLookedBlock(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Selection selection = pickSelection(player);
        if (selection == null) {
            player.sendSystemMessage(Component.literal("[SCP Inventory] No block selected.").withStyle(ChatFormatting.RED));
            return;
        }

        JsonObject root = loadRoot();
        JsonObject rule = findBlockRule(root, selection.id());
        boolean existing = rule != null;
        if (rule == null) {
            rule = createDefaultRule(selection.id(), selection.state());
        }

        setSession(player, selection.pos(), selection.id());
        ContextConfigOpenPacket packet = packetFromRule(selection.pos(), selection.id(), selection.state(), rule, existing);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void saveClientRule(ServerPlayer player, BlockPos pos, String idText, String action, String name,
                                      boolean showName, double range, boolean allowE, boolean allowRightClick, String useItem,
                                      String clickFace, String rotateWith, double anchorX, double anchorY, double anchorZ) {
        if (player == null || pos == null) {
            return;
        }

        ResourceLocation id;
        try {
            id = ResourceLocation.parse(idText);
        } catch (Exception ex) {
            player.sendSystemMessage(Component.literal("[SCP Inventory] Invalid block id: " + idText).withStyle(ChatFormatting.RED));
            return;
        }

        JsonObject root = loadRoot();
        JsonObject rule = findBlockRule(root, id);
        if (rule == null) {
            rule = createDefaultRule(id, player.level().getBlockState(pos));
            interactions(root).add(rule);
        }

        rule.addProperty("type", "block");
        rule.addProperty("id", id.toString());
        rule.addProperty("range", Math.max(0.25D, range));
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
        setSession(player, pos, id);
        player.sendSystemMessage(Component.literal("[SCP Inventory] Saved context interaction for ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(id.toString()).withStyle(ChatFormatting.AQUA)));
    }

    public static void deleteClientRule(ServerPlayer player, BlockPos pos, String idText) {
        if (player == null) {
            return;
        }

        ResourceLocation id;
        try {
            id = ResourceLocation.parse(idText);
        } catch (Exception ex) {
            player.sendSystemMessage(Component.literal("[SCP Inventory] Invalid block id: " + idText).withStyle(ChatFormatting.RED));
            return;
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
            if ("block".equalsIgnoreCase(string(obj, "type", "")) && id.toString().equals(string(obj, "id", ""))) {
                interactions.remove(i);
                removed = true;
            }
        }

        if (removed) {
            saveRoot(root);
            ContextInteractionRegistry.reload();
            clearSession(player);
            player.sendSystemMessage(Component.literal("[SCP Inventory] Deleted context interaction for ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(id.toString()).withStyle(ChatFormatting.AQUA)));
        } else {
            player.sendSystemMessage(Component.literal("[SCP Inventory] No context interaction exists for " + id + ".").withStyle(ChatFormatting.YELLOW));
            if (pos != null) {
                setSession(player, pos, id);
            }
        }
    }

    public static void selectLookedBlock(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Selection selection = pickSelection(player);
        if (selection == null) {
            player.sendSystemMessage(Component.literal("[SCP Inventory] No block selected.").withStyle(ChatFormatting.RED));
            return;
        }

        if (hasRule(selection.id())) {
            setSession(player, selection.pos(), selection.id());
            ContextInteractionRegistry.reload();
            sendAnchorMarkerHint(player);
            sendSelectedHelp(player, selection.id(), false);
        } else {
            PENDING.put(player.getUUID(), new PendingSelection(selection.pos(), selection.id()));
            player.sendSystemMessage(Component.literal("[SCP Inventory] Add ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(selection.id().toString()).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" to context interactions? ").withStyle(ChatFormatting.YELLOW))
                    .append(commandButton("[Add]", "/scpinventory context add", ChatFormatting.GREEN, "Create a default interaction entry for this block"))
                    .append(Component.literal(" "))
                    .append(commandButton("[Cancel]", "/scpinventory context cancel", ChatFormatting.RED, "Cancel this selection")));
        }
    }

    public static int addPending(CommandSourceStack source) {
        ServerPlayer player = asPlayer(source);
        if (player == null) {
            return 0;
        }
        PendingSelection pending = PENDING.remove(player.getUUID());
        if (pending == null) {
            selectLookedBlock(player);
            return 1;
        }

        JsonObject root = loadRoot();
        JsonObject rule = findBlockRule(root, pending.id());
        if (rule == null) {
            rule = createDefaultRule(pending.id(), player.level().getBlockState(pending.pos()));
            interactions(root).add(rule);
            saveRoot(root);
        }

        setSession(player, pending.pos(), pending.id());
        ContextInteractionRegistry.reload();
        sendAnchorMarkerHint(player);
        sendSelectedHelp(player, pending.id(), true);
        return 1;
    }

    public static int cancel(CommandSourceStack source) {
        ServerPlayer player = asPlayer(source);
        if (player == null) {
            return 0;
        }
        PENDING.remove(player.getUUID());
        clearSession(player);
        player.sendSystemMessage(Component.literal("[SCP Inventory] Context config cancelled.").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    public static int done(CommandSourceStack source) {
        ServerPlayer player = asPlayer(source);
        if (player == null) {
            return 0;
        }
        clearSession(player);
        player.sendSystemMessage(Component.literal("[SCP Inventory] Context config session closed.").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    public static int setText(CommandSourceStack source, String key, String value) {
        ServerPlayer player = asPlayer(source);
        Session session = session(player);
        if (player == null || session == null) {
            return noSession(source);
        }
        JsonObject root = loadRoot();
        JsonObject rule = requireRule(root, session.id(), player);
        JsonObject text = object(rule, "text");
        text.addProperty(key, value == null ? "" : value);
        if ("name".equals(key)) {
            text.addProperty("nameMode", value == null || value.isBlank() ? "auto" : "manual");
            text.addProperty("showName", true);
        }
        if ("action".equals(key)) {
            text.addProperty("showAction", true);
        }
        saveRoot(root);
        ContextInteractionRegistry.reload();
        player.sendSystemMessage(Component.literal("[SCP Inventory] Set " + key + " = " + value).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    public static int setRange(CommandSourceStack source, double range) {
        ServerPlayer player = asPlayer(source);
        Session session = session(player);
        if (player == null || session == null) {
            return noSession(source);
        }
        JsonObject root = loadRoot();
        JsonObject rule = requireRule(root, session.id(), player);
        rule.addProperty("range", Math.max(0.25D, range));
        saveRoot(root);
        ContextInteractionRegistry.reload();
        player.sendSystemMessage(Component.literal("[SCP Inventory] Range set to " + Math.max(0.25D, range)).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    public static int setInput(CommandSourceStack source, String value) {
        ServerPlayer player = asPlayer(source);
        Session session = session(player);
        if (player == null || session == null) {
            return noSession(source);
        }
        String mode = value == null ? "both" : value.toLowerCase(Locale.ROOT);
        boolean allowE = true;
        boolean allowRightClick = true;
        if ("e".equals(mode) || "key".equals(mode)) {
            allowRightClick = false;
        } else if ("right_click".equals(mode) || "rightclick".equals(mode) || "mouse".equals(mode)) {
            allowE = false;
        } else if (!"both".equals(mode)) {
            player.sendSystemMessage(Component.literal("[SCP Inventory] input must be both, e, or right_click.").withStyle(ChatFormatting.RED));
            return 0;
        }

        JsonObject root = loadRoot();
        JsonObject rule = requireRule(root, session.id(), player);
        JsonObject input = object(rule, "input");
        input.addProperty("allowE", allowE);
        input.addProperty("allowRightClick", allowRightClick);
        saveRoot(root);
        ContextInteractionRegistry.reload();
        player.sendSystemMessage(Component.literal("[SCP Inventory] Input mode set to " + mode).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    public static int setUseItem(CommandSourceStack source, String value) {
        ServerPlayer player = asPlayer(source);
        Session session = session(player);
        if (player == null || session == null) {
            return noSession(source);
        }
        String mode = cleanUseItem(value);
        JsonObject root = loadRoot();
        JsonObject rule = requireRule(root, session.id(), player);
        rule.addProperty("useItem", mode);
        saveRoot(root);
        ContextInteractionRegistry.reload();
        player.sendSystemMessage(Component.literal("[SCP Inventory] Use item set to " + mode).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    public static int setClickFace(CommandSourceStack source, String value) {
        ServerPlayer player = asPlayer(source);
        Session session = session(player);
        if (player == null || session == null) {
            return noSession(source);
        }
        JsonObject root = loadRoot();
        JsonObject rule = requireRule(root, session.id(), player);
        object(rule, "click").addProperty("face", cleanClickFace(value));
        saveRoot(root);
        ContextInteractionRegistry.reload();
        player.sendSystemMessage(Component.literal("[SCP Inventory] Click face set to " + value).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    public static int setRotateWith(CommandSourceStack source, String value) {
        ServerPlayer player = asPlayer(source);
        Session session = session(player);
        if (player == null || session == null) {
            return noSession(source);
        }
        String mode = cleanRotateWith(value);
        JsonObject root = loadRoot();
        JsonObject rule = requireRule(root, session.id(), player);
        object(rule, "anchor").addProperty("rotateWith", mode);
        saveRoot(root);
        ContextInteractionRegistry.reload();
        sendAnchorMarkerHint(player);
        player.sendSystemMessage(Component.literal("[SCP Inventory] Anchor rotation set to " + mode).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    public static int setAnchorHere(CommandSourceStack source, double distance) {
        ServerPlayer player = asPlayer(source);
        Session session = session(player);
        if (player == null || session == null) {
            return noSession(source);
        }
        Vec3 anchor = player.getEyePosition().add(player.getViewVector(1.0F).normalize().scale(Math.max(0.25D, distance)));
        return setAnchorWorld(player, session, anchor);
    }

    public static int setAnchorHit(CommandSourceStack source) {
        ServerPlayer player = asPlayer(source);
        Session session = session(player);
        if (player == null || session == null) {
            return noSession(source);
        }
        BlockHitResult hit = pickAny(player, SELECT_REACH);
        Vec3 anchor = hit != null ? hit.getLocation() : player.getEyePosition().add(player.getViewVector(1.0F).normalize().scale(2.0D));
        return setAnchorWorld(player, session, anchor);
    }

    public static int nudgeAnchor(CommandSourceStack source, double dx, double dy, double dz) {
        ServerPlayer player = asPlayer(source);
        Session session = session(player);
        if (player == null || session == null) {
            return noSession(source);
        }
        JsonObject root = loadRoot();
        JsonObject rule = requireRule(root, session.id(), player);
        JsonObject anchor = object(rule, "anchor");
        JsonArray position = positionArray(anchor);
        position.set(0, number(position.get(0).getAsDouble() + dx));
        position.set(1, number(position.get(1).getAsDouble() + dy));
        position.set(2, number(position.get(2).getAsDouble() + dz));
        anchor.addProperty("rotateWith", "none");
        saveRoot(root);
        ContextInteractionRegistry.reload();
        sendAnchorMarkerHint(player);
        player.sendSystemMessage(Component.literal("[SCP Inventory] Anchor nudged by [" + dx + ", " + dy + ", " + dz + "]").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    public static int marker(CommandSourceStack source) {
        ServerPlayer player = asPlayer(source);
        if (player == null || session(player) == null) {
            return noSession(source);
        }
        sendAnchorMarkerHint(player);
        return 1;
    }

    public static int reload(CommandSourceStack source) {
        ContextInteractionRegistry.reload();
        source.sendSuccess(() -> Component.literal("[SCP Inventory] Context interactions reloaded.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int setAnchorWorld(ServerPlayer player, Session session, Vec3 anchorWorld) {
        JsonObject root = loadRoot();
        JsonObject rule = requireRule(root, session.id(), player);
        JsonObject anchor = object(rule, "anchor");
        Vec3 local = anchorWorld.subtract(Vec3.atLowerCornerOf(session.pos()));
        JsonArray position = new JsonArray();
        position.add(round(local.x));
        position.add(round(local.y));
        position.add(round(local.z));
        anchor.add("position", position);
        anchor.addProperty("rotateWith", "none");
        saveRoot(root);
        ContextInteractionRegistry.reload();
        sendAnchorMarkerHint(player);
        player.sendSystemMessage(Component.literal("[SCP Inventory] Anchor set to local [" + round(local.x) + ", " + round(local.y) + ", " + round(local.z) + "]").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static ContextConfigOpenPacket packetFromRule(BlockPos pos, ResourceLocation id, BlockState state, JsonObject rule, boolean existing) {
        JsonObject text = object(rule, "text");
        JsonObject input = object(rule, "input");
        JsonObject click = object(rule, "click");
        JsonObject anchor = object(rule, "anchor");
        JsonArray position = positionArray(anchor);
        String name = string(text, "name", string(rule, "name", state.getBlock().getName().getString()));
        boolean showName = bool(text, "showName", bool(rule, "showName", !name.isBlank()));
        return new ContextConfigOpenPacket(
                pos,
                id.toString(),
                existing,
                string(text, "action", string(rule, "action", "Use")),
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

    private static void sendSelectedHelp(ServerPlayer player, ResourceLocation id, boolean created) {
        player.sendSystemMessage(Component.literal("[SCP Inventory] " + (created ? "Created" : "Selected") + " context rule for ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(id.toString()).withStyle(ChatFormatting.AQUA)));
        player.sendSystemMessage(Component.literal("Quick edit: ").withStyle(ChatFormatting.GRAY)
                .append(commandButton("[Anchor Hit]", "/scpinventory context anchor hit", ChatFormatting.YELLOW, "Move anchor to the looked point"))
                .append(Component.literal(" "))
                .append(commandButton("[Hand]", "/scpinventory context item hand", ChatFormatting.WHITE, "Set use item to hand"))
                .append(Component.literal(" "))
                .append(commandButton("[Card]", "/scpinventory context item card", ChatFormatting.AQUA, "Set use item to card"))
                .append(Component.literal(" "))
                .append(commandButton("[Open GUI]", "/scpinventory context gui", ChatFormatting.LIGHT_PURPLE, "Open the visual editor"))
                .append(Component.literal(" "))
                .append(commandButton("[Done]", "/scpinventory context done", ChatFormatting.GREEN, "Close config session")));
        player.sendSystemMessage(Component.literal("Commands: /scpinventory context set action <text>, name <text>, range <value>, input <both|e|right_click>, anchor here <distance>, anchor nudge <x> <y> <z>").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void sendAnchorMarkerHint(ServerPlayer player) {
        Session session = session(player);
        if (session == null) {
            return;
        }
        JsonObject rule = findBlockRule(loadRoot(), session.id());
        Vec3 anchor = readAnchorWorld(session, rule);
        player.sendSystemMessage(Component.literal("[SCP Inventory] Anchor preview is now handled by the GUI particle marker. Current world anchor: ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("[" + round(anchor.x) + ", " + round(anchor.y) + ", " + round(anchor.z) + "]").withStyle(ChatFormatting.AQUA)));
    }

    private static Component commandButton(String text, String command, ChatFormatting color, String hover) {
        return Component.literal(text).withStyle(style -> style.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    private static Selection pickSelection(ServerPlayer player) {
        BlockHitResult hit = pickBlock(player, SELECT_REACH);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null || state.isAir()) {
            return null;
        }
        return new Selection(pos, state, id);
    }

    private static BlockHitResult pickBlock(ServerPlayer player, double reach) {
        BlockHitResult hit = pickAny(player, reach);
        return hit != null && hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private static BlockHitResult pickAny(ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).normalize().scale(reach));
        return player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    private static JsonObject loadRoot() {
        try {
            File file = ensureConfigFile();
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

    static File ensureConfigFile() {
        File file = configFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            copyBundledConfig(file);
        }
        if (!file.exists()) {
            try {
                Files.writeString(file.toPath(),
                        DefaultContextInteractions.loadBundledConfig(),
                        StandardCharsets.UTF_8);
            } catch (Exception exception) {
                ScpInventoryMod.LOGGER.error(
                        "Failed to create fallback context interaction config", exception);
            }
        }
        return file;
    }

    private static void copyBundledConfig(File file) {
        try (InputStream stream = ContextConfigManager.class.getClassLoader()
                .getResourceAsStream(BUNDLED_CONFIG)) {
            if (stream != null) {
                Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            ScpInventoryMod.LOGGER.error("Failed to copy bundled context interaction defaults", exception);
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

    private static boolean hasRule(ResourceLocation id) {
        return findBlockRule(loadRoot(), id) != null;
    }

    private static JsonObject requireRule(JsonObject root, ResourceLocation id, ServerPlayer player) {
        JsonObject rule = findBlockRule(root, id);
        if (rule == null) {
            Session session = session(player);
            BlockState state = session == null ? player.level().getBlockState(player.blockPosition()) : player.level().getBlockState(session.pos());
            rule = createDefaultRule(id, state);
            interactions(root).add(rule);
        }
        return rule;
    }

    private static JsonObject findBlockRule(JsonObject root, ResourceLocation id) {
        for (JsonElement element : interactions(root)) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            if ("block".equalsIgnoreCase(string(obj, "type", "")) && id.toString().equals(string(obj, "id", ""))) {
                return obj;
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

        JsonObject anchor = new JsonObject();
        JsonArray position = new JsonArray();
        position.add(0.5D);
        position.add(0.5D);
        position.add(0.5D);
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

    private static JsonObject object(JsonObject parent, String key) {
        if (!parent.has(key) || !parent.get(key).isJsonObject()) {
            parent.add(key, new JsonObject());
        }
        return parent.getAsJsonObject(key);
    }

    private static JsonArray positionArray(JsonObject anchor) {
        if (!anchor.has("position") || !anchor.get("position").isJsonArray()) {
            JsonArray position = new JsonArray();
            position.add(0.5D);
            position.add(0.5D);
            position.add(0.5D);
            anchor.add("position", position);
        }
        JsonArray position = anchor.getAsJsonArray("position");
        while (position.size() < 3) {
            position.add(0.5D);
        }
        return position;
    }

    private static JsonElement number(double value) {
        return GSON.toJsonTree(round(value));
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

    private static double round(double value) {
        return Math.round(value * 1000.0D) / 1000.0D;
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

    private static void setSession(ServerPlayer player, BlockPos pos, ResourceLocation id) {
        SESSIONS.put(player.getUUID(), new Session(pos, id));
    }

    private static Session session(ServerPlayer player) {
        return player == null ? null : SESSIONS.get(player.getUUID());
    }

    private static void clearSession(ServerPlayer player) {
        if (player != null) {
            SESSIONS.remove(player.getUUID());
        }
    }

    private static Vec3 readAnchorWorld(Session session, JsonObject rule) {
        JsonObject anchor = rule == null ? new JsonObject() : object(rule, "anchor");
        JsonArray position = positionArray(anchor);
        return Vec3.atLowerCornerOf(session.pos()).add(position.get(0).getAsDouble(), position.get(1).getAsDouble(), position.get(2).getAsDouble());
    }

    private static ServerPlayer asPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return null;
        }
    }

    private static int noSession(CommandSourceStack source) {
        source.sendFailure(Component.literal("No context config session. Look at a block and press the Context Config key, or run /scpinventory context select."));
        return 0;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearSession(player);
            PENDING.remove(player.getUUID());
        }
    }

    private record Selection(BlockPos pos, BlockState state, ResourceLocation id) {
    }

    private record PendingSelection(BlockPos pos, ResourceLocation id) {
    }

    private record Session(BlockPos pos, ResourceLocation id) {
    }
}
