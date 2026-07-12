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
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Runtime registry for the finalized E/right-click context prompt system. */
public final class ContextInteractionRegistry {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("scpinventory").resolve("context_interactions.json");
    private static final String DEFAULT_RESOURCE =
            "/defaults/scpinventory/context_interactions.json";

    private static final Map<Block, List<Rule>> BLOCK_RULES = new HashMap<>();
    private static final Map<EntityType<?>, List<Rule>> ENTITY_RULES = new HashMap<>();
    private static boolean loaded;
    private static double maxBlockRange;
    private static double maxEntityRange;

    private ContextInteractionRegistry() {
    }

    public static synchronized void ensureLoaded() {
        if (!loaded) load();
    }

    public static synchronized void reload() {
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
            Files.createDirectories(CONFIG_PATH.getParent());
            if (!Files.exists(CONFIG_PATH)) copyDefaultConfig();

            try (Reader reader = Files.newBufferedReader(CONFIG_PATH,
                    StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                JsonObject root = parsed.isJsonObject()
                        ? parsed.getAsJsonObject() : new JsonObject();
                JsonArray interactions = root.has("interactions")
                        && root.get("interactions").isJsonArray()
                        ? root.getAsJsonArray("interactions") : new JsonArray();

                for (JsonElement element : interactions) {
                    if (!element.isJsonObject()) continue;
                    Rule rule = parseRule(element.getAsJsonObject());
                    if (rule == null) continue;
                    if (rule.kind == Kind.BLOCK && rule.block != null
                            && rule.block != Blocks.AIR) {
                        BLOCK_RULES.computeIfAbsent(rule.block,
                                ignored -> new ArrayList<>()).add(rule);
                        maxBlockRange = Math.max(maxBlockRange, rule.range);
                    } else if (rule.kind == Kind.ENTITY
                            && rule.entityType != null) {
                        ENTITY_RULES.computeIfAbsent(rule.entityType,
                                ignored -> new ArrayList<>()).add(rule);
                        maxEntityRange = Math.max(maxEntityRange, rule.range);
                    }
                }
            }
        } catch (Exception exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Failed to load context interactions from {}", CONFIG_PATH,
                    exception);
        }

        loaded = true;
        ScpAdditionsMod.LOGGER.info(
                "Loaded {} block and {} entity context interaction groups",
                BLOCK_RULES.size(), ENTITY_RULES.size());
    }

    private static void copyDefaultConfig() throws Exception {
        try (InputStream stream = ContextInteractionRegistry.class
                .getResourceAsStream(DEFAULT_RESOURCE)) {
            if (stream == null) {
                try (Writer writer = Files.newBufferedWriter(CONFIG_PATH,
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
                    writer.write("{\"interactions\":[],\"examples\":[]}");
                }
                return;
            }
            Files.copy(stream, CONFIG_PATH);
        }
    }

    private static Rule parseRule(JsonObject object) {
        String type = string(object, "type", "").toLowerCase(Locale.ROOT);
        ResourceLocation id = ResourceLocation.tryParse(string(object, "id", ""));
        if (id == null) return null;

        Kind kind;
        Block block = null;
        EntityType<?> entityType = null;
        if (type.equals("block")) {
            kind = Kind.BLOCK;
            block = ForgeRegistries.BLOCKS.getValue(id);
            if (block == null || block == Blocks.AIR) return null;
        } else if (type.equals("entity")) {
            kind = Kind.ENTITY;
            entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (entityType == null && id.getNamespace().equals("scpinventory")) {
                ResourceLocation migrated = new ResourceLocation("scp_additions",
                        id.getPath());
                entityType = ForgeRegistries.ENTITY_TYPES.getValue(migrated);
                if (entityType != null) id = migrated;
            }
            if (entityType == null) return null;
        } else {
            return null;
        }

        JsonObject text = object(object, "text");
        JsonObject anchor = object(object, "anchor");
        JsonObject input = object(object, "input");
        JsonObject click = object(object, "click");
        JsonObject visual = object(object, "visual");

        String action = string(text, "action", string(object, "action", "Use"));
        boolean showAction = bool(text, "showAction",
                bool(object, "showAction", true));
        String nameMode = string(text, "nameMode",
                string(object, "nameMode", "manual"));
        String name = string(text, "name", string(object, "name", ""));
        boolean autoName = nameMode.equalsIgnoreCase("auto")
                || name.equalsIgnoreCase("auto");
        if (autoName) name = "";
        boolean showName = bool(text, "showName",
                bool(object, "showName", autoName || !name.isEmpty()));

        double[] local = vector(anchor, "position", 0.5D, 0.5D, 0.5D);
        double[] worldOffset = vector(anchor, "worldOffset", 0.0D, 0.0D, 0.0D);
        RotationMode rotationMode = RotationMode.from(
                string(anchor, "rotateWith", "auto"));
        double range = number(object, "range", 2.25D);
        int priority = integer(object, "priority", kind == Kind.BLOCK ? 30 : 25);
        boolean allowE = bool(input, "allowE", bool(object, "allowE", true));
        boolean allowRightClick = bool(input, "allowRightClick",
                bool(object, "allowRightClick", true));
        String clickFace = string(click, "face",
                string(object, "clickFace", "front"));
        String useItem = string(object, "useItem", "hand");
        String icon = string(object, "icon", string(visual, "icon", useItem));

        return new Rule(kind, id, block, entityType, range, priority,
                action, name, showAction, showName, autoName,
                local[0], local[1], local[2], worldOffset[0],
                worldOffset[1], worldOffset[2], rotationMode, allowE,
                allowRightClick, clickFace, useItem, icon);
    }

    private static JsonObject object(JsonObject parent, String key) {
        return parent != null && parent.has(key) && parent.get(key).isJsonObject()
                ? parent.getAsJsonObject(key) : new JsonObject();
    }

    private static String string(JsonObject object, String key, String fallback) {
        try {
            return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                    ? object.get(key).getAsString() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        try {
            return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                    ? object.get(key).getAsBoolean() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double number(JsonObject object, String key, double fallback) {
        try {
            return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                    ? object.get(key).getAsDouble() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        try {
            return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                    ? object.get(key).getAsInt() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double[] vector(JsonObject object, String key,
            double x, double y, double z) {
        double[] result = {x, y, z};
        try {
            if (object != null && object.has(key) && object.get(key).isJsonArray()) {
                JsonArray array = object.getAsJsonArray(key);
                for (int i = 0; i < Math.min(3, array.size()); i++) {
                    result[i] = array.get(i).getAsDouble();
                }
            }
        } catch (Exception ignored) {
            return new double[]{x, y, z};
        }
        return result;
    }

    public enum Kind { BLOCK, ENTITY }

    public enum RotationMode {
        NONE, AUTO, FACING, HORIZONTAL_FACING, AXIS;

        static RotationMode from(String raw) {
            try {
                return RotationMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
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
        private final double localX, localY, localZ;
        private final double worldOffsetX, worldOffsetY, worldOffsetZ;
        private final RotationMode rotationMode;
        private final boolean allowE, allowRightClick;
        private final String clickFace, useItem, icon;

        private Rule(Kind kind, ResourceLocation id, Block block,
                EntityType<?> entityType, double range, int priority,
                String action, String name, boolean showAction,
                boolean showName, boolean autoName, double localX,
                double localY, double localZ, double worldOffsetX,
                double worldOffsetY, double worldOffsetZ,
                RotationMode rotationMode, boolean allowE,
                boolean allowRightClick, String clickFace, String useItem,
                String icon) {
            this.kind = kind;
            this.id = id;
            this.block = block;
            this.entityType = entityType;
            this.range = Math.max(0.25D, range);
            this.priority = priority;
            this.action = action == null || action.isBlank() ? "Use" : action;
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
            this.icon = icon == null || icon.isBlank() ? this.useItem : icon;
        }

        public Kind kind() { return kind; }
        public ResourceLocation id() { return id; }
        public double range() { return range; }
        public int priority() { return priority; }
        public String action() { return action; }
        public boolean showAction() { return showAction; }
        public boolean showName() { return showName; }
        public boolean allowE() { return allowE; }
        public boolean allowRightClick() { return allowRightClick; }
        public String useItem() { return useItem; }
        public String icon() { return icon; }

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
            Vec3 centered = new Vec3(localX - 0.5D, localY - 0.5D,
                    localZ - 0.5D);
            Vec3 rotated = rotate(centered, state);
            return Vec3.atLowerCornerOf(pos).add(0.5D, 0.5D, 0.5D)
                    .add(rotated).add(worldOffsetX, worldOffsetY, worldOffsetZ);
        }

        public Vec3 resolveEntityAnchor(Entity entity) {
            Vec3 size = new Vec3(entity.getBbWidth(), entity.getBbHeight(),
                    entity.getBbWidth());
            return entity.position().add(localX * size.x,
                    localY * size.y, localZ * size.z)
                    .add(worldOffsetX, worldOffsetY, worldOffsetZ);
        }

        public Direction resolveClickFace(BlockState state, Player player) {
            String face = clickFace.trim().toLowerCase(Locale.ROOT);
            Direction facing = resolveFacing(state);
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
            Direction facing = resolveFacing(state);
            if (facing == null || facing == Direction.NORTH) return local;
            return switch (facing) {
                case SOUTH -> new Vec3(-local.x, local.y, -local.z);
                case EAST -> new Vec3(-local.z, local.y, local.x);
                case WEST -> new Vec3(local.z, local.y, -local.x);
                default -> local;
            };
        }

        private Direction resolveFacing(BlockState state) {
            if (rotationMode == RotationMode.NONE || state == null) return null;
            if (state.hasProperty(BlockStateProperties.FACING))
                return state.getValue(BlockStateProperties.FACING);
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
                return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return rotationMode == RotationMode.AUTO ? null : Direction.NORTH;
        }
    }
}
