package net.mcreator.scpadditions.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Floor whitelist used by natural Roomba encounters.
 *
 * <p>Built-in SCP Additions floors are deliberately kept outside the user
 * configuration. New integrated floors can therefore be added by future mod
 * versions without forcing existing installations to reset their JSON file.</p>
 */
public final class RoombaSpawnConfig {
    public static final String CONFIG_KEY = "roomba_spawn_blocks";

    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("scpinventory").resolve("scpinventory.json");
    private static final List<ResourceLocation> INTEGRATED_BLOCKS = List.of(
            new ResourceLocation(ScpAdditionsMod.MODID, "sl_1_floor_2"),
            new ResourceLocation(ScpAdditionsMod.MODID, "sl_1_floor_1"),
            new ResourceLocation(ScpAdditionsMod.MODID, "sl_2_floor"));

    private static volatile Set<ResourceLocation> customBlocks = Set.of();
    private static volatile long loadedModifiedTime = Long.MIN_VALUE;

    private RoombaSpawnConfig() {
    }

    public static List<ResourceLocation> integratedBlocks() {
        return INTEGRATED_BLOCKS;
    }

    public static synchronized void reloadIfChanged() {
        long modified = modifiedTime();
        if (modified == loadedModifiedTime) {
            return;
        }

        LinkedHashSet<ResourceLocation> loaded = new LinkedHashSet<>();
        if (Files.isRegularFile(CONFIG_PATH)) {
            try {
                JsonElement parsed = JsonParser.parseString(
                        Files.readString(CONFIG_PATH, StandardCharsets.UTF_8));
                if (parsed.isJsonObject()) {
                    JsonObject root = parsed.getAsJsonObject();
                    if (root.has(CONFIG_KEY)
                            && root.get(CONFIG_KEY).isJsonArray()) {
                        JsonArray entries = root.getAsJsonArray(CONFIG_KEY);
                        for (JsonElement entry : entries) {
                            if (!entry.isJsonPrimitive()) {
                                continue;
                            }
                            ResourceLocation id = ResourceLocation.tryParse(
                                    entry.getAsString().trim());
                            if (id == null || INTEGRATED_BLOCKS.contains(id)) {
                                continue;
                            }
                            if (ForgeRegistries.BLOCKS.containsKey(id)
                                    && ForgeRegistries.BLOCKS.getValue(id)
                                    != Blocks.AIR) {
                                loaded.add(id);
                            }
                        }
                    }
                }
            } catch (Exception exception) {
                ScpAdditionsMod.LOGGER.warn(
                        "Could not read custom Roomba spawn floors from {}",
                        CONFIG_PATH, exception);
            }
        }

        customBlocks = Set.copyOf(loaded);
        loadedModifiedTime = modified;
    }

    public static boolean isValidFloor(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && (INTEGRATED_BLOCKS.contains(id)
                || customBlocks.contains(id));
    }

    private static long modifiedTime() {
        try {
            return Files.isRegularFile(CONFIG_PATH)
                    ? Files.getLastModifiedTime(CONFIG_PATH).toMillis() : -1L;
        } catch (Exception ignored) {
            return -1L;
        }
    }
}
