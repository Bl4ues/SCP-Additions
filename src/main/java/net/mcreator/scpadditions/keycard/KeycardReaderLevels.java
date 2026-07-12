package net.mcreator.scpadditions.keycard;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;

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
        if (matches(block, ScpAdditionsModBlocks.LEFT_READER,
                ScpAdditionsModBlocks.LEFT_READER_ACCEPT,
                ScpAdditionsModBlocks.LEFT_READER_WRONG)) {
            return new ReaderDescriptor(1, Side.LEFT);
        }
        if (matches(block, ScpAdditionsModBlocks.RIGHT_READER,
                ScpAdditionsModBlocks.RIGHT_READER_ACCEPT,
                ScpAdditionsModBlocks.RIGHT_READER_WRONG)) {
            return new ReaderDescriptor(1, Side.RIGHT);
        }
        if (matches(block, ScpAdditionsModBlocks.LV_2_LEFT_READER,
                ScpAdditionsModBlocks.LV_2_LEFT_READER_ACCEPT,
                ScpAdditionsModBlocks.LV_2_LEFT_READER_WRONG)) {
            return new ReaderDescriptor(2, Side.LEFT);
        }
        if (matches(block, ScpAdditionsModBlocks.LV_2_RIGHT_READER,
                ScpAdditionsModBlocks.LV_2_RIGHT_READER_ACCEPT,
                ScpAdditionsModBlocks.LV_2_RIGHT_READER_WRONG)) {
            return new ReaderDescriptor(2, Side.RIGHT);
        }
        if (matches(block, ScpAdditionsModBlocks.LV_3_LEFT_READER,
                ScpAdditionsModBlocks.LV_3_LEFT_READER_ACCEPT,
                ScpAdditionsModBlocks.LV_3_LEFT_READER_WRONG)) {
            return new ReaderDescriptor(3, Side.LEFT);
        }
        if (matches(block, ScpAdditionsModBlocks.LV_3_RIGHT_READER,
                ScpAdditionsModBlocks.LV_3_RIGHT_READER_ACCEPT,
                ScpAdditionsModBlocks.LV_3_RIGHT_READER_WRONG)) {
            return new ReaderDescriptor(3, Side.RIGHT);
        }
        if (matches(block, ScpAdditionsModBlocks.LV_4_LEFT_READER,
                ScpAdditionsModBlocks.LV_4_LEFT_READER_ACCEPT,
                ScpAdditionsModBlocks.LV_4_LEFT_READER_WRONG)) {
            return new ReaderDescriptor(4, Side.LEFT);
        }
        if (matches(block, ScpAdditionsModBlocks.LV_4_RIGHT_READER,
                ScpAdditionsModBlocks.LV_4_RIGHT_READER_ACCEPT,
                ScpAdditionsModBlocks.LV_4_RIGHT_READER_WRONG)) {
            return new ReaderDescriptor(4, Side.RIGHT);
        }
        if (matches(block, ScpAdditionsModBlocks.LV_5_LEFT_READER,
                ScpAdditionsModBlocks.LV_5_LEFT_READER_ACCEPT,
                ScpAdditionsModBlocks.LV_5_LEFT_READER_WRONG)) {
            return new ReaderDescriptor(5, Side.LEFT);
        }
        if (matches(block, ScpAdditionsModBlocks.LV_5_RIGHT_READER,
                ScpAdditionsModBlocks.LV_5_RIGHT_READER_ACCEPT,
                ScpAdditionsModBlocks.LV_5_RIGHT_READER_WRONG)) {
            return new ReaderDescriptor(5, Side.RIGHT);
        }
        if (matches(block, ScpAdditionsModBlocks.LV_6_LEFT_READER,
                ScpAdditionsModBlocks.LV_6_LEFT_READER_ACCEPT,
                ScpAdditionsModBlocks.LV_6_LEFT_READER_WRONG)) {
            return new ReaderDescriptor(6, Side.LEFT);
        }
        if (matches(block, ScpAdditionsModBlocks.LV_6_RIGHT_READER,
                ScpAdditionsModBlocks.LV_6_RIGHT_READER_ACCEPT,
                ScpAdditionsModBlocks.LV_6_RIGHT_READER_WRONG)) {
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
        BlockState replacement = targetBlock.defaultBlockState();

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

    public static Block normalBlock(int level, Side side) {
        return switch (level) {
            case 1 -> side == Side.LEFT
                    ? ScpAdditionsModBlocks.LEFT_READER.get()
                    : ScpAdditionsModBlocks.RIGHT_READER.get();
            case 2 -> side == Side.LEFT
                    ? ScpAdditionsModBlocks.LV_2_LEFT_READER.get()
                    : ScpAdditionsModBlocks.LV_2_RIGHT_READER.get();
            case 3 -> side == Side.LEFT
                    ? ScpAdditionsModBlocks.LV_3_LEFT_READER.get()
                    : ScpAdditionsModBlocks.LV_3_RIGHT_READER.get();
            case 4 -> side == Side.LEFT
                    ? ScpAdditionsModBlocks.LV_4_LEFT_READER.get()
                    : ScpAdditionsModBlocks.LV_4_RIGHT_READER.get();
            case 5 -> side == Side.LEFT
                    ? ScpAdditionsModBlocks.LV_5_LEFT_READER.get()
                    : ScpAdditionsModBlocks.LV_5_RIGHT_READER.get();
            case 6 -> side == Side.LEFT
                    ? ScpAdditionsModBlocks.LV_6_LEFT_READER.get()
                    : ScpAdditionsModBlocks.LV_6_RIGHT_READER.get();
            default -> throw new IllegalArgumentException("Reader level must be between 1 and 6");
        };
    }

    private static boolean matches(Block block, RegistryObject<Block> normal,
            RegistryObject<Block> accepted, RegistryObject<Block> wrong) {
        return block == normal.get() || block == accepted.get() || block == wrong.get();
    }
}
