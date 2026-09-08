package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/** Shared client/server rules for SCP-079 door controls. */
public final class Scp079DoorControlPolicy {
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };
    private static final Set<String> READER_BASE_PATHS = Set.of(
            "left_reader", "right_reader",
            "lv_2_left_reader", "lv_2_right_reader",
            "lv_3_left_reader", "lv_3_right_reader",
            "lv_4_left_reader", "lv_4_right_reader",
            "lv_5_left_reader", "lv_5_right_reader",
            "lv_6_left_reader", "lv_6_right_reader"
    );

    private Scp079DoorControlPolicy() {
    }

    /**
     * Keycard readers are access-control hardware, not lockout buttons. SCP-079
     * may still open/close their door, but must never offer the temporary LOCK
     * action for a door controlled by one.
     */
    public static boolean hasKeycardReader(Level level, BlockPos doorPos) {
        if (level == null || doorPos == null) return false;
        for (int yOffset = 0; yOffset <= 2; yOffset++) {
            BlockPos probe = doorPos.above(yOffset);
            for (Direction direction : HORIZONTAL) {
                BlockPos candidate = probe.relative(direction);
                if (!level.hasChunkAt(candidate)) continue;
                if (isKeycardReader(level.getBlockState(candidate).getBlock())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isKeycardReader(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null || !ScpClassifiedDirectiveMod.MODID.equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        if (path.endsWith("_accept")) {
            path = path.substring(0, path.length() - "_accept".length());
        } else if (path.endsWith("_wrong")) {
            path = path.substring(0, path.length() - "_wrong".length());
        }
        return READER_BASE_PATHS.contains(path);
    }
}
