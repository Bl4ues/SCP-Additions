package com.bl4ues.scpclassifieddirective.keycard;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.RegistryObject;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;

/**
 * Maps the legacy reader blocks to a configurable access level while keeping
 * their published registry IDs and existing redstone/procedure behavior.
 */
public final class KeycardReaderLevels {
    public enum Side {
        LEFT,
        RIGHT
    }

    public record ReaderDescriptor(int level, Side side) {
    }

    private KeycardReaderLevels() {
    }

    public static ReaderDescriptor describe(BlockState state) {
        return describe(state.getBlock());
    }

    public static ReaderDescriptor describe(Block block) {
        if (matches(block, ScpClassifiedDirectiveModBlocks.LEFT_READER,
                ScpClassifiedDirectiveModBlocks.LEFT_READER_ACCEPT,
                ScpClassifiedDirectiveModBlocks.LEFT_READER_WRONG)) {
            return new ReaderDescriptor(1, Side.LEFT);
        }
        if (matches(block, ScpClassifiedDirectiveModBlocks.RIGHT_READER,
                ScpClassifiedDirectiveModBlocks.RIGHT_READER_ACCEPT,
                ScpClassifiedDirectiveModBlocks.RIGHT_READER_WRONG)) {
            return new ReaderDescriptor(1, Side.RIGHT);
        }
        if (matches(block, ScpClassifiedDirectiveModBlocks.LV_2_LEFT_READER,
                ScpClassifiedDirectiveModBlocks.LV_2_LEFT_READER_ACCEPT,
                ScpClassifiedDirectiveModBlocks.LV_2_LEFT_READER_WRONG)) {
            return new ReaderDescriptor(2, Side.LEFT);
        }
        if (matches(block, ScpClassifiedDirectiveModBlocks.LV_2_RIGHT_READER,
                ScpClassifiedDirectiveModBlocks.LV_2_RIGHT_READER_ACCEPT,
                ScpClassifiedDirectiveModBlocks.LV_2_RIGHT_READER_WRONG)) {
            return new ReaderDescriptor(2, Side.RIGHT);
        }
        if (matches(block, ScpClassifiedDirectiveModBlocks.LV_3_LEFT_READER,
                ScpClassifiedDirectiveModBlocks.LV_3_LEFT_READER_ACCEPT,
                ScpClassifiedDirectiveModBlocks.LV_3_LEFT_READER_WRONG)) {
            return new ReaderDescriptor(3, Side.LEFT);
        }
        if (matches(block, ScpClassifiedDirectiveModBlocks.LV_3_RIGHT_READER,
                ScpClassifiedDirectiveModBlocks.LV_3_RIGHT_READER_ACCEPT,
                ScpClassifiedDirectiveModBlocks.LV_3_RIGHT_READER_WRONG)) {
            return new ReaderDescriptor(3, Side.RIGHT);
        }
        if (matches(block, ScpClassifiedDirectiveModBlocks.LV_4_LEFT_READER,
                ScpClassifiedDirectiveModBlocks.LV_4_LEFT_READER_ACCEPT,
                ScpClassifiedDirectiveModBlocks.LV_4_LEFT_READER_WRONG)) {
            return new ReaderDescriptor(4, Side.LEFT);
        }
        if (matches(block, ScpClassifiedDirectiveModBlocks.LV_4_RIGHT_READER,
                ScpClassifiedDirectiveModBlocks.LV_4_RIGHT_READER_ACCEPT,
                ScpClassifiedDirectiveModBlocks.LV_4_RIGHT_READER_WRONG)) {
            return new ReaderDescriptor(4, Side.RIGHT);
        }
        if (matches(block, ScpClassifiedDirectiveModBlocks.LV_5_LEFT_READER,
                ScpClassifiedDirectiveModBlocks.LV_5_LEFT_READER_ACCEPT,
                ScpClassifiedDirectiveModBlocks.LV_5_LEFT_READER_WRONG)) {
            return new ReaderDescriptor(5, Side.LEFT);
        }
        if (matches(block, ScpClassifiedDirectiveModBlocks.LV_5_RIGHT_READER,
                ScpClassifiedDirectiveModBlocks.LV_5_RIGHT_READER_ACCEPT,
                ScpClassifiedDirectiveModBlocks.LV_5_RIGHT_READER_WRONG)) {
            return new ReaderDescriptor(5, Side.RIGHT);
        }
        if (matches(block, ScpClassifiedDirectiveModBlocks.LV_6_LEFT_READER,
                ScpClassifiedDirectiveModBlocks.LV_6_LEFT_READER_ACCEPT,
                ScpClassifiedDirectiveModBlocks.LV_6_LEFT_READER_WRONG)) {
            return new ReaderDescriptor(6, Side.LEFT);
        }
        if (matches(block, ScpClassifiedDirectiveModBlocks.LV_6_RIGHT_READER,
                ScpClassifiedDirectiveModBlocks.LV_6_RIGHT_READER_ACCEPT,
                ScpClassifiedDirectiveModBlocks.LV_6_RIGHT_READER_WRONG)) {
            return new ReaderDescriptor(6, Side.RIGHT);
        }
        return null;
    }

    public static boolean replaceLevel(Level level, BlockPos pos, int requestedLevel) {
        if (requestedLevel < 1 || requestedLevel > 6) {
            return false;
        }

        BlockState current = level.getBlockState(pos);
        ReaderDescriptor descriptor = describe(current);
        if (descriptor == null) {
            return false;
        }

        Block targetBlock = normalBlock(requestedLevel, descriptor.side());
        return replacePreservingState(level, pos, current,
                targetBlock.defaultBlockState());
    }

    /**
     * Forces the current reader into its ordinary accepted/redstone state. Every
     * successful authorization explicitly rearms a fresh five-second pulse, even
     * if the reader was already accepted. This makes repeated access extend from
     * the latest acceptance instead of inheriting a stale scheduled reset.
     */
    public static boolean activateAccepted(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;
        BlockState current = level.getBlockState(pos);
        ReaderDescriptor descriptor = describe(current);
        if (descriptor == null) return false;
        Block target = acceptedBlock(descriptor.level(), descriptor.side());

        boolean changed = replacePreservingState(level, pos, current,
                target.defaultBlockState());
        BlockState acceptedState = level.getBlockState(pos);
        if (level instanceof ServerLevel serverLevel
                && acceptedState.getBlock() == target) {
            KeycardReaderPulse.arm(serverLevel, pos, target);
            return true;
        }
        return changed;
    }

    public static Block normalBlock(int level, Side side) {
        return switch (level) {
            case 1 -> side == Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LEFT_READER.get()
                    : ScpClassifiedDirectiveModBlocks.RIGHT_READER.get();
            case 2 -> side == Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LV_2_LEFT_READER.get()
                    : ScpClassifiedDirectiveModBlocks.LV_2_RIGHT_READER.get();
            case 3 -> side == Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LV_3_LEFT_READER.get()
                    : ScpClassifiedDirectiveModBlocks.LV_3_RIGHT_READER.get();
            case 4 -> side == Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LV_4_LEFT_READER.get()
                    : ScpClassifiedDirectiveModBlocks.LV_4_RIGHT_READER.get();
            case 5 -> side == Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LV_5_LEFT_READER.get()
                    : ScpClassifiedDirectiveModBlocks.LV_5_RIGHT_READER.get();
            case 6 -> side == Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LV_6_LEFT_READER.get()
                    : ScpClassifiedDirectiveModBlocks.LV_6_RIGHT_READER.get();
            default -> throw new IllegalArgumentException("Reader level must be between 1 and 6");
        };
    }

    public static Block acceptedBlock(int level, Side side) {
        return switch (level) {
            case 1 -> side == Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LEFT_READER_ACCEPT.get()
                    : ScpClassifiedDirectiveModBlocks.RIGHT_READER_ACCEPT.get();
            case 2 -> side == Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LV_2_LEFT_READER_ACCEPT.get()
                    : ScpClassifiedDirectiveModBlocks.LV_2_RIGHT_READER_ACCEPT.get();
            case 3 -> side == Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LV_3_LEFT_READER_ACCEPT.get()
                    : ScpClassifiedDirectiveModBlocks.LV_3_RIGHT_READER_ACCEPT.get();
            case 4 -> side == Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LV_4_LEFT_READER_ACCEPT.get()
                    : ScpClassifiedDirectiveModBlocks.LV_4_RIGHT_READER_ACCEPT.get();
            case 5 -> side == Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LV_5_LEFT_READER_ACCEPT.get()
                    : ScpClassifiedDirectiveModBlocks.LV_5_RIGHT_READER_ACCEPT.get();
            case 6 -> side == Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LV_6_LEFT_READER_ACCEPT.get()
                    : ScpClassifiedDirectiveModBlocks.LV_6_RIGHT_READER_ACCEPT.get();
            default -> throw new IllegalArgumentException("Reader level must be between 1 and 6");
        };
    }

    private static boolean replacePreservingState(Level level, BlockPos pos,
            BlockState current, BlockState replacement) {
        if (current.hasProperty(HorizontalDirectionalBlock.FACING)
                && replacement.hasProperty(HorizontalDirectionalBlock.FACING)) {
            replacement = replacement.setValue(HorizontalDirectionalBlock.FACING,
                    current.getValue(HorizontalDirectionalBlock.FACING));
        }
        if (current.hasProperty(BlockStateProperties.WATERLOGGED)
                && replacement.hasProperty(BlockStateProperties.WATERLOGGED)) {
            replacement = replacement.setValue(BlockStateProperties.WATERLOGGED,
                    current.getValue(BlockStateProperties.WATERLOGGED));
        }
        return level.setBlock(pos, replacement, Block.UPDATE_ALL);
    }

    private static boolean matches(Block block, RegistryObject<Block> normal,
            RegistryObject<Block> accepted, RegistryObject<Block> wrong) {
        return block == normal.get() || block == accepted.get() || block == wrong.get();
    }
}
