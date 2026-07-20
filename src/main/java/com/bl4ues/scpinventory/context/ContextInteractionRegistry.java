package com.bl4ues.scpinventory.context;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ContextInteractionRegistry {

    private static final Map<Block, List<Rule>> BLOCK_RULES = new HashMap<>();
    private static final Map<EntityType<?>, List<Rule>> ENTITY_RULES = new HashMap<>();
    private static boolean loaded = false;
    private static volatile String serverSnapshotJson;
    private static double maxBlockRange = 0.0D;
    private static double maxEntityRange = 0.0D;

    private ContextInteractionRegistry() {
    }

    public static void ensureLoaded() {
        if (!loaded) {
            load();
        }
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

    private static void load() {
        BLOCK_RULES.clear();
        ENTITY_RULES.clear();
        maxBlockRange = 0.0D;
        maxEntityRange = 0.0D;

        try {
            String snapshot = serverSnapshotJson;
            JsonObject root;
            if (snapshot != null && !snapshot.isBlank()) {
                root = JsonParser.parseString(snapshot).getAsJsonObject();
            } else {
                File file = ContextConfigManager.ensureConfigFile();
                root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
            }
            JsonArray interactions = root.has("interactions") && root.get("interactions").isJsonArray()
                    ? root.getAsJsonArray("interactions")
                    : new JsonArray();

            for (JsonElement element : interactions) {
                if (!element.isJsonObject()) {
                    continue;
                }
                Rule rule = parseRule(element.getAsJsonObject());
                if (rule == null) {
                    continue;
                }
                if (rule.kind == Kind.BLOCK && rule.block != null && rule.block != Blocks.AIR) {
                    BLOCK_RULES.computeIfAbsent(rule.block, ignored -> new ArrayList<>()).add(rule);
                    maxBlockRange = Math.max(maxBlockRange, rule.range);
                } else if (rule.kind == Kind.ENTITY && rule.entityType != null) {
                    ENTITY_RULES.computeIfAbsent(rule.entityType, ignored -> new ArrayList<>()).add(rule);
                    maxEntityRange = Math.max(maxEntityRange, rule.range);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        loaded = true;
    }

    private static Rule parseRule(JsonObject obj) {
        String type = getString(obj, "type", "").toLowerCase(Locale.ROOT);
        String idText = getString(obj, "id", "");
        if (type.isEmpty() || idText.isEmpty()) {
            return null;
        }
        // The old default 1499 entity rule resolves to a vanilla pig in the
        // external gas-mask mod. Ignore it even in pre-hotfix user configs.
        if ("entity".equals(type) && "gas_mask:scp_1499".equals(idText)) {
            return null;
        }

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
            block = ForgeRegistries.BLOCKS.getValue(id);
            if (block == null || block == Blocks.AIR) {
                return null;
            }
        } else if ("entity".equals(type)) {
            kind = Kind.ENTITY;
            entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (entityType == null) {
                return null;
            }
        } else {
            return null;
        }

        JsonObject text = getObject(obj, "text");
        JsonObject anchor = getObject(obj, "anchor");
        JsonObject input = getObject(obj, "input");
        JsonObject click = getObject(obj, "click");
        JsonObject visual = getObject(obj, "visual");

        String action = getString(text, "action", getString(obj, "action", "Use"));
        boolean showAction = getBoolean(text, "showAction", getBoolean(obj, "showAction", true));

        String nameMode = getString(text, "nameMode", getString(obj, "nameMode", "manual"));
        String name = getString(text, "name", getString(obj, "name", ""));
        boolean autoName = "auto".equalsIgnoreCase(nameMode) || "auto".equalsIgnoreCase(name);
        if (autoName) {
            name = "";
        }
        boolean defaultShowName = autoName || !name.isEmpty();
        boolean showName = getBoolean(text, "showName", getBoolean(obj, "showName", defaultShowName));

        double[] local = getVec3(anchor, "position", 0.5D, 0.5D, 0.5D);
        double[] worldOffset = getVec3(anchor, "worldOffset", 0.0D, 0.0D, 0.0D);
        RotationMode rotationMode = RotationMode.from(getString(anchor, "rotateWith", "auto"));

        double range = getDouble(obj, "range", 2.25D);
        int priority = getInt(obj, "priority", kind == Kind.BLOCK ? 30 : 25);
        boolean allowE = getBoolean(input, "allowE", getBoolean(obj, "allowE", true));
        boolean allowRightClick = getBoolean(input, "allowRightClick", getBoolean(obj, "allowRightClick", true));
        String clickFace = getString(click, "face", getString(obj, "clickFace", "front"));
        String useItem = getString(obj, "useItem", "hand");
        String icon = getString(obj, "icon", getString(visual, "icon", useItem));

        return new Rule(kind, id, block, entityType, range, priority, action, name, showAction, showName, autoName,
                local[0], local[1], local[2], worldOffset[0], worldOffset[1], worldOffset[2], rotationMode,
                allowE, allowRightClick, clickFace, useItem, icon);
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

    private static boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        try {
            return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsBoolean() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int getInt(JsonObject obj, String key, int fallback) {
        try {
            return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double getDouble(JsonObject obj, String key, double fallback) {
        try {
            return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsDouble() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double[] getVec3(JsonObject obj, String key, double x, double y, double z) {
        double[] value = new double[]{x, y, z};
        try {
            if (obj != null && obj.has(key) && obj.get(key).isJsonArray()) {
                JsonArray array = obj.getAsJsonArray(key);
                for (int i = 0; i < Math.min(3, array.size()); i++) {
                    value[i] = array.get(i).getAsDouble();
                }
            }
        } catch (Exception ignored) {
            return new double[]{x, y, z};
        }
        return value;
    }

    public enum Kind {
        BLOCK,
        ENTITY
    }

    public enum RotationMode {
        NONE,
        AUTO,
        FACING,
        HORIZONTAL_FACING,
        AXIS;

        public static RotationMode from(String value) {
            if (value == null) {
                return AUTO;
            }
            try {
                return RotationMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
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

        private Rule(Kind kind, ResourceLocation id, Block block, EntityType<?> entityType, double range, int priority,
                     String action, String name, boolean showAction, boolean showName, boolean autoName,
                     double localX, double localY, double localZ, double worldOffsetX, double worldOffsetY, double worldOffsetZ,
                     RotationMode rotationMode, boolean allowE, boolean allowRightClick, String clickFace, String useItem, String icon) {
            this.kind = kind;
            this.id = id;
            this.block = block;
            this.entityType = entityType;
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
            this.rotationMode = rotationMode == null ? RotationMode.AUTO : rotationMode;
            this.allowE = allowE;
            this.allowRightClick = allowRightClick;
            this.clickFace = clickFace == null ? "front" : clickFace;
            this.useItem = useItem == null ? "hand" : useItem;
            this.icon = icon == null || icon.isEmpty() ? this.useItem : icon;
        }

        public Kind kind() {
            return kind;
        }

        public ResourceLocation id() {
            return id;
        }

        public double range() {
            return range;
        }

        public int priority() {
            return priority;
        }

        public String action() {
            return action;
        }

        public boolean showAction() {
            return showAction;
        }

        public boolean showName() {
            return showName;
        }

        public boolean allowE() {
            return allowE;
        }

        public boolean allowRightClick() {
            return allowRightClick;
        }

        public String useItem() {
            return useItem;
        }

        public String icon() {
            return icon;
        }

        public String blockName(BlockState state) {
            if (!autoName) {
                return name;
            }
            ItemStack stack = new ItemStack(state.getBlock().asItem());
            if (!stack.isEmpty()) {
                return stack.getHoverName().getString();
            }
            return state.getBlock().getName().getString();
        }

        public String entityName(Entity entity) {
            if (!autoName) {
                return name;
            }
            return entity.getDisplayName().getString();
        }

        public Vec3 resolveBlockAnchor(BlockPos pos, BlockState state) {
            Vec3 centered = new Vec3(localX - 0.5D, localY - 0.5D, localZ - 0.5D);
            Vec3 rotated = rotate(centered, state);
            return Vec3.atLowerCornerOf(pos)
                    .add(0.5D, 0.5D, 0.5D)
                    .add(rotated)
                    .add(worldOffsetX, worldOffsetY, worldOffsetZ);
        }

        public Vec3 resolveEntityAnchor(Entity entity) {
            return entity.getBoundingBox().getCenter().add(worldOffsetX, worldOffsetY, worldOffsetZ);
        }

        public Direction resolveClickFace(BlockState state, Player player) {
            String face = clickFace.trim().toLowerCase(Locale.ROOT);
            Direction facing = resolveFacing(state, rotationMode);
            return switch (face) {
                case "front" -> facing != null ? facing : player.getDirection().getOpposite();
                case "back" -> facing != null ? facing.getOpposite() : player.getDirection();
                case "player" -> player.getDirection().getOpposite();
                case "north" -> Direction.NORTH;
                case "south" -> Direction.SOUTH;
                case "east" -> Direction.EAST;
                case "west" -> Direction.WEST;
                case "up" -> Direction.UP;
                case "down" -> Direction.DOWN;
                default -> facing != null ? facing : player.getDirection().getOpposite();
            };
        }

        private Vec3 rotate(Vec3 local, BlockState state) {
            Direction facing = resolveFacing(state, rotationMode);
            if (facing == null || facing == Direction.NORTH) {
                return local;
            }
            return switch (facing) {
                case SOUTH -> new Vec3(-local.x, local.y, -local.z);
                case EAST -> new Vec3(-local.z, local.y, local.x);
                case WEST -> new Vec3(local.z, local.y, -local.x);
                case UP, DOWN -> local;
                default -> local;
            };
        }

        private Direction resolveFacing(BlockState state, RotationMode mode) {
            if (mode == RotationMode.NONE || state == null) {
                return null;
            }
            if (state.hasProperty(BlockStateProperties.FACING)) {
                return state.getValue(BlockStateProperties.FACING);
            }
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            }
            if (state.hasProperty(BlockStateProperties.AXIS)) {
                return null;
            }
            return mode == RotationMode.AUTO ? null : Direction.NORTH;
        }
    }
}
