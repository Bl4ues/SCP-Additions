package com.bl4ues.scpclassifieddirective.inventory.context;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.DocumentHolderBlockEntity;
import com.bl4ues.scpclassifieddirective.facility.FacilityLargePropStructure;
import com.bl4ues.scpclassifieddirective.facility.FacilityPropPartBlock;
import com.bl4ues.scpclassifieddirective.facility.ObjectContainmentUnitModule;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorCarriageEntity;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorGeometry;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorModule;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorManager;
import com.bl4ues.scpclassifieddirective.facility.elevator.ElevatorFoundation;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.integration.PlayerItemAccess;
import com.bl4ues.scpclassifieddirective.block.entity.Scp914BlockEntity;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Module;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Runtime registry for configurable and built-in contextual interactions. */
public final class ContextInteractionRegistry {
    private static final Map<Block, List<Rule>> BLOCK_RULES = new HashMap<>();
    private static final Map<EntityType<?>, List<Rule>> ENTITY_RULES =
            new HashMap<>();
    private static boolean loaded;
    private static volatile String serverSnapshotJson;
    private static double maxBlockRange;
    private static double maxEntityRange;

    private ContextInteractionRegistry() {
    }

    public static void ensureLoaded() {
        if (!loaded) load();
    }

    public static synchronized void applyServerSnapshot(String json) {
        serverSnapshotJson = json == null ? "" : json;
        loaded = false;
        load();
    }

    public static synchronized void clearServerSnapshot() {
        serverSnapshotJson = null;
        loaded = false;
        load();
    }

    public static void reload() {
        loaded = false;
        load();
    }

    /** Reloads the host file instead of a client snapshot shared in singleplayer. */
    public static synchronized void reloadFromDisk() {
        serverSnapshotJson = null;
        loaded = false;
        load();
    }

    public static List<Rule> getBlockRules(Block block) {
        ensureLoaded();
        return BLOCK_RULES.getOrDefault(block, Collections.emptyList());
    }

    public static List<Rule> getEntityRules(EntityType<?> type) {
        ensureLoaded();
        return ENTITY_RULES.getOrDefault(type, Collections.emptyList());
    }

    public static boolean hasBlockRules() {
        ensureLoaded();
        return !BLOCK_RULES.isEmpty();
    }

    public static boolean hasEntityRules() {
        ensureLoaded();
        return !ENTITY_RULES.isEmpty();
    }

    public static double getMaxBlockRange() {
        ensureLoaded();
        return maxBlockRange;
    }

    public static double getMaxEntityRange() {
        ensureLoaded();
        return maxEntityRange;
    }

    private static synchronized void load() {
        BLOCK_RULES.clear();
        ENTITY_RULES.clear();
        maxBlockRange = 0.0D;
        maxEntityRange = 0.0D;

        int configuredCount = 0;
        int integratedCount = 0;
        Set<InteractionIdentity> configuredIdentities = new HashSet<>();
        try {
            JsonObject configuredRoot = loadConfiguredRoot();
            int sequence = 0;

            JsonArray configuredInteractions = getInteractions(configuredRoot);
            for (JsonElement element : configuredInteractions) {
                if (!element.isJsonObject()) continue;
                JsonObject source = element.getAsJsonObject();
                InteractionIdentity sourceIdentity = interactionIdentity(source);
                if (sourceIdentity != null
                        && sourceIdentity.interactionKey().isBlank()
                        && !getBoolean(source, "enabled", true)) {
                    configuredIdentities.add(new InteractionIdentity(
                            sourceIdentity.type(), sourceIdentity.id(), "*"));
                }
                for (JsonObject object : expandRuleVariants(source)) {
                    InteractionIdentity identity = interactionIdentity(object);
                    if (identity != null) configuredIdentities.add(identity);
                    if (!getBoolean(source, "enabled", true)
                            || !getBoolean(object, "enabled", true)) {
                        continue;
                    }

                    Rule rule = parseRule(object, sequence++);
                    if (rule != null) {
                        addRule(rule);
                        configuredCount++;
                    }
                }
            }

            JsonObject integratedRoot = JsonParser.parseString(
                    DefaultContextInteractions.loadBundledConfig())
                    .getAsJsonObject();
            Set<InteractionIdentity> integratedIdentities = new HashSet<>();
            for (JsonElement element : getInteractions(integratedRoot)) {
                if (!element.isJsonObject()) continue;
                JsonObject source = element.getAsJsonObject();
                for (JsonObject object : expandRuleVariants(source)) {
                    InteractionIdentity identity = interactionIdentity(object);
                    if (identity == null
                            || isConfiguredIdentity(configuredIdentities, identity)
                            || !integratedIdentities.add(identity)
                            || !getBoolean(source, "enabled", true)
                            || !getBoolean(object, "enabled", true)) {
                        continue;
                    }

                    Rule rule = parseRule(object, sequence++);
                    if (rule != null) {
                        addRule(rule);
                        integratedCount++;
                    }
                }
            }
        } catch (Exception exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Failed to load contextual interactions", exception);
        }

        integratedCount += registerElevatorRules(configuredIdentities);
        integratedCount += registerObjectContainmentUnitRules(
                configuredIdentities);
        integratedCount += registerNativeScrewdriverRules(configuredIdentities);
        loaded = true;
        ScpClassifiedDirectiveMod.LOGGER.info(
                "Loaded {} configured and {} integrated contextual interactions",
                configuredCount, integratedCount);
    }

    private static JsonObject loadConfiguredRoot() throws Exception {
        String snapshot = serverSnapshotJson;
        if (snapshot != null && !snapshot.isBlank()) {
            return JsonParser.parseString(snapshot).getAsJsonObject();
        }
        File file = ContextConfigManager.ensureConfigFile();
        try (FileReader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonArray getInteractions(JsonObject root) {
        return root != null && root.has("interactions")
                && root.get("interactions").isJsonArray()
                ? root.getAsJsonArray("interactions") : new JsonArray();
    }


    /**
     * Expands one target definition into its default interaction and optional
     * inherited variants. Variant objects inherit the target, anchor, text and
     * input values they do not replace, keeping configuration files compact.
     */
    private static List<JsonObject> expandRuleVariants(JsonObject source) {
        List<JsonObject> expanded = new ArrayList<>();
        JsonObject base = source.deepCopy();
        base.remove("variants");
        expanded.add(base);

        if (!source.has("variants") || !source.get("variants").isJsonArray()) {
            return expanded;
        }
        for (JsonElement element : source.getAsJsonArray("variants")) {
            if (!element.isJsonObject()) continue;
            JsonObject variant = base.deepCopy();
            deepMerge(variant, element.getAsJsonObject());
            variant.remove("variants");
            expanded.add(variant);
        }
        return expanded;
    }

    private static void deepMerge(JsonObject target, JsonObject overlay) {
        for (Map.Entry<String, JsonElement> entry : overlay.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonObject() && target.has(key)
                    && target.get(key).isJsonObject()) {
                deepMerge(target.getAsJsonObject(key), value.getAsJsonObject());
            } else {
                target.add(key, value.deepCopy());
            }
        }
    }

    private static InteractionIdentity interactionIdentity(JsonObject object) {
        String type = getString(object, "type", "")
                .trim().toLowerCase(Locale.ROOT);
        String idText = getString(object, "id", "").trim();
        if (type.isEmpty() || idText.isEmpty()) return null;

        try {
            ResourceLocation id = new ResourceLocation(idText);
            String interactionKey = getString(object, "interactionId",
                    getString(object, "interactionKey", "")).trim();
            return new InteractionIdentity(type, id.toString(), interactionKey);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void addRule(Rule rule) {
        if (rule.kind == Kind.BLOCK && rule.block != null
                && rule.block != Blocks.AIR) {
            BLOCK_RULES.computeIfAbsent(rule.block,
                    ignored -> new ArrayList<>()).add(rule);
            maxBlockRange = Math.max(maxBlockRange, rule.range);
        } else if (rule.kind == Kind.ENTITY && rule.entityType != null) {
            ENTITY_RULES.computeIfAbsent(rule.entityType,
                    ignored -> new ArrayList<>()).add(rule);
            maxEntityRange = Math.max(maxEntityRange, rule.range);
        }
    }

    private static int registerElevatorRules(
            Set<InteractionIdentity> configuredIdentities) {
        int registered = 0;
        try {
            ResourceLocation stationId = new ResourceLocation(
                    ScpClassifiedDirectiveMod.MODID, "core_room_elevator_station");
            Block station = CoreRoomElevatorModule.STATION.get();
            double x = 0.5D;
            double z = 0.5D;
            registered += addIntegratedRule(configuredIdentities,
                    new InteractionIdentity("block", stationId.toString(),
                            "elevator_station_up"),
                    new Rule(Kind.BLOCK, stationId, station, null,
                            "elevator_station_up", 2.8D, 120, "",
                            "", false, false, false,
                            x, 21.25D / 16.0D, z,
                            0.0D, 0.0D, 0.0D,
                            RotationMode.HORIZONTAL_FACING, true, true,
                            "front", "hand", "hand", null, 0.38D, false));
            registered += addIntegratedRule(configuredIdentities,
                    new InteractionIdentity("block", stationId.toString(),
                            "elevator_station_down"),
                    new Rule(Kind.BLOCK, stationId, station, null,
                            "elevator_station_down", 2.8D, 120, "",
                            "", false, false, false,
                            x, 19.25D / 16.0D, z,
                            0.0D, 0.0D, 0.0D,
                            RotationMode.HORIZONTAL_FACING, true, true,
                            "front", "hand", "hand", null, 0.38D, false));

            ResourceLocation carriageId = new ResourceLocation(
                    ScpClassifiedDirectiveMod.MODID, "core_room_elevator_carriage");
            EntityType<?> carriage = CoreRoomElevatorModule.CARRIAGE.get();
            registered += addIntegratedRule(configuredIdentities,
                    new InteractionIdentity("entity", carriageId.toString(),
                            "elevator_carriage_up"),
                    new Rule(Kind.ENTITY, carriageId, null, carriage,
                            "elevator_carriage_up", 2.8D, 125, "",
                            "", false, false, false,
                            0.5D, 0.5D, 0.5D,
                            0.0D, 0.0D, 0.0D,
                            RotationMode.NONE, true, true,
                            "front", "hand", "hand", null, 0.38D, false));
            registered += addIntegratedRule(configuredIdentities,
                    new InteractionIdentity("entity", carriageId.toString(),
                            "elevator_carriage_down"),
                    new Rule(Kind.ENTITY, carriageId, null, carriage,
                            "elevator_carriage_down", 2.8D, 125, "",
                            "", false, false, false,
                            0.5D, 0.5D, 0.5D,
                            0.0D, 0.0D, 0.0D,
                            RotationMode.NONE, true, true,
                            "front", "hand", "hand", null, 0.38D, false));
        } catch (Exception exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Failed to register Core Room elevator interactions",
                    exception);
        }
        return registered;
    }

    private static int registerObjectContainmentUnitRules(
            Set<InteractionIdentity> configuredIdentities) {
        int registered = 0;
        try {
            ResourceLocation unitId = new ResourceLocation(
                    ScpClassifiedDirectiveMod.MODID,
                    "object_containment_unit");
            Block unit = ObjectContainmentUnitModule.UNIT.get();
            ResourceLocation screwdriver = new ResourceLocation(
                    ScpClassifiedDirectiveMod.MODID, "screwdriver");

            // Use the center of the actual rotated reader geometry, not merely
            // the bone pivot. GeckoLib mirrors the authored X axis in world
            // space, matching the SCP-1176 anchor transform.
            double readerX = 0.5D - 9.625D / 16.0D;
            double readerY = 13.28094476D / 16.0D;
            double readerZ = 0.5D + 0.90481263D / 16.0D;

            // One interaction point represents the opened lid. It sits near the
            // inner visual center of the glass panel; Close gets a hard aim
            // threshold, so it wins decisively while the crosshair is on the lid
            // and disappears once the aim moves back onto contained objects.
            double openLidX = 1.12D;
            double openLidY = 1.38D;
            double openLidZ = 0.5D;

            registered += addIntegratedRule(configuredIdentities,
                    new InteractionIdentity("block", unitId.toString(),
                            "open_object_containment_unit"),
                    new Rule(Kind.BLOCK, unitId, unit, null,
                            "open_object_containment_unit", 1.75D, 90,
                            "Open", "Object Containment Unit",
                            true, true, false,
                            readerX, readerY, readerZ,
                            0.0D, 0.0D, 0.0D,
                            RotationMode.HORIZONTAL_FACING, true, true,
                            "player", "card", "card", null, 1.0D, false));

            registered += addIntegratedRule(configuredIdentities,
                    new InteractionIdentity("block", unitId.toString(),
                            "close_object_containment_unit"),
                    new Rule(Kind.BLOCK, unitId, unit, null,
                            "close_object_containment_unit", 1.75D, 120,
                            "Close", "Object Containment Unit",
                            true, true, false,
                            openLidX, openLidY, openLidZ,
                            0.0D, 0.0D, 0.0D,
                            RotationMode.HORIZONTAL_FACING, true, true,
                            "player", "hand", "hand", null, 1.0D, false));

            registered += addIntegratedRule(configuredIdentities,
                    new InteractionIdentity("block", unitId.toString(),
                            "configure_object_containment_unit"),
                    new Rule(Kind.BLOCK, unitId, unit, null,
                            "configure_object_containment_unit", 1.75D, 130,
                            "Configure", "Object Containment Unit",
                            true, true, false,
                            readerX, readerY, readerZ,
                            0.0D, 0.0D, 0.0D,
                            RotationMode.HORIZONTAL_FACING, true, true,
                            "player", "screwdriver", "screwdriver",
                            screwdriver, 1.0D, false));
        } catch (Exception exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Failed to register Object Containment Unit interactions",
                    exception);
        }
        return registered;
    }

    private static boolean isConfiguredIdentity(
            Set<InteractionIdentity> configuredIdentities,
            InteractionIdentity identity) {
        if (identity == null) return false;
        return configuredIdentities.contains(identity)
                || configuredIdentities.contains(new InteractionIdentity(
                        identity.type(), identity.id(), "*"));
    }

    private static int addIntegratedRule(
            Set<InteractionIdentity> configuredIdentities,
            InteractionIdentity identity, Rule rule) {
        if (isConfiguredIdentity(configuredIdentities, identity)) return 0;
        addRule(rule);
        return 1;
    }

    private static int registerNativeScrewdriverRules(
            Set<InteractionIdentity> configuredIdentities) {
        ResourceLocation screwdriver = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID, "screwdriver");
        int count = 0;

        String[] readers = {
                "right_reader", "lv_2_right_reader", "lv_3_right_reader",
                "lv_4_right_reader", "lv_5_right_reader", "lv_6_right_reader",
                "left_reader", "lv_2_left_reader", "lv_3_left_reader",
                "lv_4_left_reader", "lv_5_left_reader", "lv_6_left_reader"
        };
        for (String path : readers) {
            count += addToolVariant(configuredIdentities, Kind.BLOCK,
                    new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path),
                    "configure_with_screwdriver", "Configure",
                    screwdriver, 45);
        }

        for (String path : List.of("tesla_terminal_block",
                "tesla_terminal_off", "core_room_elevator_station",
                "sign_support", "core_room_sign", "door_sign",
                "facility_prop_part")) {
            String action = path.contains("sign") ? "Edit"
                    : path.contains("elevator") ? "Configure Display"
                    : "Configure";
            count += addToolVariant(configuredIdentities, Kind.BLOCK,
                    new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path),
                    "configure_with_screwdriver", action, screwdriver, 45);
        }

        for (String path : List.of("scp_131_a", "scp_131_b", "roomba")) {
            count += addToolVariant(configuredIdentities, Kind.ENTITY,
                    new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path),
                    "dismantle_with_screwdriver", "Dismantle",
                    screwdriver, 45);
        }
        return count;
    }

    private static int addToolVariant(
            Set<InteractionIdentity> configuredIdentities, Kind kind,
            ResourceLocation id, String key, String action,
            ResourceLocation requiredItem, int priority) {
        String type = kind == Kind.BLOCK ? "block" : "entity";
        InteractionIdentity identity = new InteractionIdentity(type,
                id.toString(), key);
        if (isConfiguredIdentity(configuredIdentities, identity)) return 0;

        if (kind == Kind.BLOCK) {
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (block == null || block == Blocks.AIR) return 0;
            Rule template = BLOCK_RULES.getOrDefault(block, List.of()).stream()
                    .findFirst().orElse(null);
            if (template != null) {
                addRule(template.toolVariant(key, action, requiredItem,
                        priority));
            } else {
                addRule(new Rule(kind, id, block, null, key, 2.25D,
                        priority, action, "", true, true, true,
                        0.5D, 0.5D, 0.5D, 0.0D, 0.0D, 0.0D,
                        RotationMode.AUTO, true, true, "front", "hand",
                        "config", requiredItem, 1.0D, false));
            }
            return 1;
        }

        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (entityType == null) return 0;
        Rule template = ENTITY_RULES.getOrDefault(entityType, List.of()).stream()
                .findFirst().orElse(null);
        if (template != null) {
            addRule(template.toolVariant(key, action, requiredItem, priority));
        } else {
            addRule(new Rule(kind, id, null, entityType, key, 2.25D,
                    priority, action, "", true, true, true,
                    0.5D, 0.5D, 0.5D, 0.0D, 0.0D, 0.0D,
                    RotationMode.NONE, true, true, "front", "hand",
                    "config", requiredItem, 1.0D, false));
        }
        return 1;
    }

    private static Rule parseRule(JsonObject object, int sequence) {
        String type = getString(object, "type", "")
                .toLowerCase(Locale.ROOT);
        String idText = getString(object, "id", "");
        if (type.isEmpty() || idText.isEmpty()) return null;

        ResourceLocation id;
        try {
            id = new ResourceLocation(idText);
        } catch (Exception ignored) {
            return null;
        }

        Kind kind;
        Block block = null;
        EntityType<?> entityType = null;
        if ("block".equals(type)) {
            kind = Kind.BLOCK;
            if (!ForgeRegistries.BLOCKS.containsKey(id)) return null;
            block = ForgeRegistries.BLOCKS.getValue(id);
            if (block == null || block == Blocks.AIR) return null;
        } else if ("entity".equals(type)) {
            kind = Kind.ENTITY;
            if (!ForgeRegistries.ENTITY_TYPES.containsKey(id)) return null;
            entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (entityType == null) return null;
        } else {
            return null;
        }

        JsonObject text = getObject(object, "text");
        JsonObject anchor = getObject(object, "anchor");
        JsonObject input = getObject(object, "input");
        JsonObject click = getObject(object, "click");
        JsonObject visual = getObject(object, "visual");

        String action = getString(text, "action",
                getString(object, "action", "Use"));
        boolean showAction = getBoolean(text, "showAction",
                getBoolean(object, "showAction", true));
        String nameMode = getString(text, "nameMode",
                getString(object, "nameMode", "manual"));
        String name = getString(text, "name",
                getString(object, "name", ""));
        boolean autoName = "auto".equalsIgnoreCase(nameMode)
                || "auto".equalsIgnoreCase(name);
        if (autoName) name = "";
        boolean defaultShowName = autoName || !name.isEmpty();
        boolean showName = getBoolean(text, "showName",
                getBoolean(object, "showName", defaultShowName));

        double[] local = getVec3(anchor, "position", 0.5D, 0.5D, 0.5D);
        double[] world = getVec3(anchor, "worldOffset", 0.0D, 0.0D, 0.0D);
        RotationMode rotation = RotationMode.from(getString(anchor,
                "rotateWith", "auto"));
        double range = getDouble(object, "range", 2.25D);
        int priority = getInt(object, "priority",
                kind == Kind.BLOCK ? 30 : 25);
        boolean allowE = getBoolean(input, "allowE",
                getBoolean(object, "allowE", true));
        boolean allowRightClick = getBoolean(input, "allowRightClick",
                getBoolean(object, "allowRightClick", true));
        String requiredItemText = getString(input, "requiredItem",
                getString(object, "requiredItem", "")).trim();
        ResourceLocation requiredItem = parseOptionalId(requiredItemText);
        if (!requiredItemText.isBlank() && requiredItem == null) return null;
        String clickFace = getString(click, "face",
                getString(object, "clickFace", "front"));
        String useItem = getString(object, "useItem", "hand");
        String icon = getString(object, "icon",
                getString(visual, "icon", useItem));
        double promptScale = getDouble(visual, "scale",
                getDouble(object, "promptScale", 1.0D));
        boolean allowOffscreen = getBoolean(visual, "allowOffscreen",
                getBoolean(object, "allowOffscreen", false));
        String key = getString(object, "interactionId",
                getString(object, "interactionKey", ""));
        if (key.isBlank()) key = id + "#" + sequence;

        return new Rule(kind, id, block, entityType, key, range, priority,
                action, name, showAction, showName, autoName,
                local[0], local[1], local[2],
                world[0], world[1], world[2], rotation,
                allowE, allowRightClick, clickFace, useItem, icon,
                requiredItem, promptScale, allowOffscreen);
    }

    private static ResourceLocation parseOptionalId(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new ResourceLocation(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonObject getObject(JsonObject object, String key) {
        return object != null && object.has(key)
                && object.get(key).isJsonObject()
                ? object.getAsJsonObject(key) : new JsonObject();
    }

    private static String getString(JsonObject object, String key,
            String fallback) {
        try {
            return object != null && object.has(key)
                    && !object.get(key).isJsonNull()
                    ? object.get(key).getAsString() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean getBoolean(JsonObject object, String key,
            boolean fallback) {
        try {
            return object != null && object.has(key)
                    && !object.get(key).isJsonNull()
                    ? object.get(key).getAsBoolean() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        try {
            return object != null && object.has(key)
                    && !object.get(key).isJsonNull()
                    ? object.get(key).getAsInt() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double getDouble(JsonObject object, String key,
            double fallback) {
        try {
            return object != null && object.has(key)
                    && !object.get(key).isJsonNull()
                    ? object.get(key).getAsDouble() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double[] getVec3(JsonObject object, String key,
            double x, double y, double z) {
        double[] value = new double[]{x, y, z};
        try {
            if (object != null && object.has(key)
                    && object.get(key).isJsonArray()) {
                JsonArray array = object.getAsJsonArray(key);
                for (int i = 0; i < Math.min(3, array.size()); i++) {
                    value[i] = array.get(i).getAsDouble();
                }
            }
        } catch (Exception ignored) {
            return new double[]{x, y, z};
        }
        return value;
    }

    private static boolean hasReaderKeycard(Player player) {
        return PlayerItemAccess.has(player, stack ->
                stack.is(ScpClassifiedDirectiveModItems.LEVEL_1_KEYCARD.get())
                || stack.is(ScpClassifiedDirectiveModItems.LEVEL_2_KEYCARD.get())
                || stack.is(ScpClassifiedDirectiveModItems.LEVEL_3_KEYCARD.get())
                || stack.is(ScpClassifiedDirectiveModItems.LEVEL_4_KEYCARD.get())
                || stack.is(ScpClassifiedDirectiveModItems.LEVEL_5_KEYCARD.get())
                || stack.is(ScpClassifiedDirectiveModItems.LEVEL_6_KEYCARD.get()));
    }

    private record InteractionIdentity(String type, String id,
            String interactionKey) {
    }

    public enum Kind { BLOCK, ENTITY }

    public enum RotationMode {
        NONE, AUTO, FACING, HORIZONTAL_FACING, AXIS;

        public static RotationMode from(String value) {
            if (value == null) return AUTO;
            try {
                return RotationMode.valueOf(value.trim()
                        .toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {
                return AUTO;
            }
        }
    }

    public static final class Rule {
        private final Kind kind;
        private final ResourceLocation id;
        private final Block block;
        private final EntityType<?> entityType;
        private final String interactionKey;
        private final double range;
        private final int priority;
        private final String action;
        private final String name;
        private final boolean showAction;
        private final boolean showName;
        private final boolean autoName;
        private final double localX;
        private final double localY;
        private final double localZ;
        private final double worldOffsetX;
        private final double worldOffsetY;
        private final double worldOffsetZ;
        private final RotationMode rotationMode;
        private final boolean allowE;
        private final boolean allowRightClick;
        private final String clickFace;
        private final String useItem;
        private final String icon;
        private final ResourceLocation requiredItem;
        private final double promptScale;
        private final boolean allowOffscreen;

        private Rule(Kind kind, ResourceLocation id, Block block,
                EntityType<?> entityType, String interactionKey,
                double range, int priority, String action, String name,
                boolean showAction, boolean showName, boolean autoName,
                double localX, double localY, double localZ,
                double worldOffsetX, double worldOffsetY,
                double worldOffsetZ, RotationMode rotationMode,
                boolean allowE, boolean allowRightClick, String clickFace,
                String useItem, String icon, ResourceLocation requiredItem,
                double promptScale, boolean allowOffscreen) {
            this.kind = kind;
            this.id = id;
            this.block = block;
            this.entityType = entityType;
            this.interactionKey = interactionKey == null ? "" : interactionKey;
            this.range = Math.max(0.25D, range);
            this.priority = priority;
            this.action = action == null || action.isEmpty() ? "Use" : action;
            this.name = name == null ? "" : name;
            this.showAction = showAction;
            this.showName = showName;
            this.autoName = autoName;
            this.localX = localX;
            this.localY = localY;
            this.localZ = localZ;
            this.worldOffsetX = worldOffsetX;
            this.worldOffsetY = worldOffsetY;
            this.worldOffsetZ = worldOffsetZ;
            this.rotationMode = rotationMode == null
                    ? RotationMode.AUTO : rotationMode;
            this.allowE = allowE;
            this.allowRightClick = allowRightClick;
            this.clickFace = clickFace == null ? "front" : clickFace;
            this.useItem = useItem == null ? "hand" : useItem;
            this.icon = icon == null || icon.isEmpty() ? this.useItem : icon;
            this.requiredItem = requiredItem;
            this.promptScale = Math.max(0.35D,
                    Math.min(1.5D, promptScale));
            this.allowOffscreen = allowOffscreen;
        }

        private Rule toolVariant(String key, String action,
                ResourceLocation requiredItem, int priority) {
            return new Rule(kind, id, block, entityType, key, range,
                    Math.max(this.priority, priority), action, name,
                    true, showName, autoName, localX, localY, localZ,
                    worldOffsetX, worldOffsetY, worldOffsetZ, rotationMode,
                    allowE, allowRightClick, clickFace, "hand", "config",
                    requiredItem, promptScale, allowOffscreen);
        }

        public Kind kind() { return kind; }
        public ResourceLocation id() { return id; }
        public String interactionKey() { return interactionKey; }
        public double range() { return range; }
        public int priority() { return priority; }
        public String action() { return action; }
        public boolean showAction() { return showAction; }
        public boolean showName() { return showName; }
        public boolean allowE() { return allowE; }
        public boolean allowRightClick() { return allowRightClick; }
        public String useItem() { return useItem; }
        public String icon() { return icon; }
        public ResourceLocation requiredItem() { return requiredItem; }
        public boolean hasRequiredItem() { return requiredItem != null; }

        public InteractionHand matchingHand(Player player) {
            if (player == null) return null;
            if (requiredItem == null) {
                if ("card".equalsIgnoreCase(useItem)
                        && !hasReaderKeycard(player)) {
                    return null;
                }
                return InteractionHand.MAIN_HAND;
            }
            Item required = ForgeRegistries.ITEMS.getValue(requiredItem);
            if (required == null) return null;
            if (player.getMainHandItem().is(required)) {
                return InteractionHand.MAIN_HAND;
            }
            if (player.getOffhandItem().is(required)) {
                return InteractionHand.OFF_HAND;
            }
            return null;
        }

        public boolean isHeldItemSatisfied(Player player) {
            return matchingHand(player) != null;
        }

        public double promptScale() { return promptScale; }
        public boolean allowOffscreen() { return allowOffscreen; }
        public boolean requiresPreciseAim() {
            return "close_object_containment_unit".equals(interactionKey)
                    || interactionKey.startsWith("elevator_station_")
                    || interactionKey.startsWith("elevator_carriage_")
                    || interactionKey.startsWith("scp_914_");
        }

        public boolean isAvailable(Level level, BlockPos pos,
                BlockState state, Player player) {
            if (block == Scp914Module.SCP_914.get()) {
                return level.getBlockEntity(pos) instanceof Scp914BlockEntity machine
                        && !machine.isRefining();
            }
            if (ObjectContainmentUnitModule.isProtectedContent(level, pos)) {
                return false;
            }
            if (block == ObjectContainmentUnitModule.UNIT.get()
                    && "close_object_containment_unit".equals(interactionKey)
                    && player != null
                    && (!player.getMainHandItem().isEmpty()
                    || !player.getOffhandItem().isEmpty())) {
                return false;
            }
            if (block != null && "document_holder".equals(id.getPath())
                    && ScpClassifiedDirectiveMod.MODID.equals(id.getNamespace())) {
                if (!(level.getBlockEntity(pos)
                        instanceof DocumentHolderBlockEntity holder)) {
                    return false;
                }
                return holder.canContextInteract(player);
            }
            return isAvailable(level, pos, state);
        }

        public boolean isAvailable(Level level, BlockPos pos,
                BlockState state) {
            if (ObjectContainmentUnitModule.isProtectedContent(level, pos)) {
                return false;
            }
            if (block == ObjectContainmentUnitModule.UNIT.get()) {
                if (!(level.getBlockEntity(pos)
                        instanceof ObjectContainmentUnitModule.UnitBlockEntity unit)) {
                    return false;
                }
                return switch (interactionKey) {
                    case "open_object_containment_unit" ->
                            unit.phase()
                                    == ObjectContainmentUnitModule.UnitBlockEntity.Phase.CLOSED;
                    case "close_object_containment_unit" ->
                            unit.phase()
                                    == ObjectContainmentUnitModule.UnitBlockEntity.Phase.OPEN;
                    case "configure_object_containment_unit" ->
                            !unit.isTransitioning();
                    default -> true;
                };
            }
            if (block == CoreRoomElevatorModule.STATION.get()
                    && interactionKey.startsWith("elevator_station_")) {
                ElevatorFoundation.TravelDirection direction =
                        interactionKey.endsWith("up")
                                ? ElevatorFoundation.TravelDirection.UP
                                : ElevatorFoundation.TravelDirection.DOWN;
                return CoreRoomElevatorManager.hasStationInDirection(
                        level, pos, direction);
            }
            if (block instanceof FacilityPropPartBlock && state != null
                    && state.hasProperty(FacilityPropPartBlock.PART)) {
                return state.getValue(FacilityPropPartBlock.PART).kind()
                        == FacilityLargePropStructure.Kind.SIGN_SUPPORT;
            }
            return true;
        }

        public boolean isAvailable(Entity entity) {
            if (entity instanceof CoreRoomElevatorCarriageEntity carriage
                    && interactionKey.startsWith("elevator_carriage_")) {
                ElevatorFoundation.TravelDirection direction =
                        interactionKey.endsWith("up")
                                ? ElevatorFoundation.TravelDirection.UP
                                : ElevatorFoundation.TravelDirection.DOWN;
                return carriage.canTravel(direction);
            }
            return true;
        }

        public String blockName(BlockState state) {
            if (!autoName) return name;
            ItemStack stack = new ItemStack(state.getBlock().asItem());
            return !stack.isEmpty() ? stack.getHoverName().getString()
                    : state.getBlock().getName().getString();
        }

        public String entityName(Entity entity) {
            return autoName ? entity.getDisplayName().getString() : name;
        }

        public Vec3 resolveBlockAnchor(BlockPos pos, BlockState state) {
            if (block == CoreRoomElevatorModule.STATION.get()
                    && interactionKey.startsWith("elevator_station_")) {
                return CoreRoomElevatorGeometry.stationButtonWorld(pos,
                        state.getValue(CoreRoomElevatorModule.FACING),
                        interactionKey.endsWith("up"));
            }
            Vec3 centered = new Vec3(localX - 0.5D,
                    localY - 0.5D, localZ - 0.5D);
            Vec3 rotated = rotate(centered, state);
            return Vec3.atLowerCornerOf(pos).add(0.5D, 0.5D, 0.5D)
                    .add(rotated)
                    .add(worldOffsetX, worldOffsetY, worldOffsetZ);
        }

        public Vec3 resolveEntityAnchor(Entity entity) {
            if (entity instanceof CoreRoomElevatorCarriageEntity carriage) {
                if ("elevator_carriage_up".equals(interactionKey)) {
                    return carriage.contextAnchor(true);
                }
                if ("elevator_carriage_down".equals(interactionKey)) {
                    return carriage.contextAnchor(false);
                }
            }
            return entity.getBoundingBox().getCenter()
                    .add(worldOffsetX, worldOffsetY, worldOffsetZ);
        }

        public Direction resolveClickFace(BlockState state, Player player) {
            String face = clickFace.trim().toLowerCase(Locale.ROOT);
            Direction facing = resolveFacing(state, rotationMode);
            return switch (face) {
                case "front" -> facing != null ? facing
                        : player.getDirection().getOpposite();
                case "back" -> facing != null ? facing.getOpposite()
                        : player.getDirection();
                case "player" -> player.getDirection().getOpposite();
                case "north" -> Direction.NORTH;
                case "south" -> Direction.SOUTH;
                case "east" -> Direction.EAST;
                case "west" -> Direction.WEST;
                case "up" -> Direction.UP;
                case "down" -> Direction.DOWN;
                default -> facing != null ? facing
                        : player.getDirection().getOpposite();
            };
        }

        private Vec3 rotate(Vec3 local, BlockState state) {
            Direction facing = resolveFacing(state, rotationMode);
            if (facing == null || facing == Direction.NORTH) return local;
            return switch (facing) {
                case SOUTH -> new Vec3(-local.x, local.y, -local.z);
                case EAST -> new Vec3(-local.z, local.y, local.x);
                case WEST -> new Vec3(local.z, local.y, -local.x);
                case UP, DOWN -> local;
                default -> local;
            };
        }

        private Direction resolveFacing(BlockState state, RotationMode mode) {
            if (mode == RotationMode.NONE || state == null) return null;
            if (state.hasProperty(BlockStateProperties.FACING)) {
                return state.getValue(BlockStateProperties.FACING);
            }
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            }
            if (state.hasProperty(BlockStateProperties.AXIS)) return null;
            return mode == RotationMode.AUTO ? null : Direction.NORTH;
        }
    }
}
