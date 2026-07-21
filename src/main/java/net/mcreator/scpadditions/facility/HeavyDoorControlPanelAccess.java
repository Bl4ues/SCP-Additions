package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Resolves the physical control interfaces connected to a heavy-door controller
 * or one of its upper relay positions.
 *
 * SCP-079 is allowed to manipulate a heavy door only when it has a real facility
 * interface: a functional Unity button, a keycard reader, or an already placed
 * legacy Facility Pulse Node. Locked buttons and arbitrary redstone sources do
 * not grant access.
 */
public final class HeavyDoorControlPanelAccess {
    private static final int LEGACY_PULSE_TICKS = 100;
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

    private static final Map<PanelKey, DenialState> ACTIVE_DENIALS =
            new ConcurrentHashMap<>();
    private static final AtomicLong DENIAL_GENERATION = new AtomicLong();

    private HeavyDoorControlPanelAccess() {
    }

    public static boolean hasControllableInterface(ServerLevel level,
            BlockPos doorPos) {
        return inspect(level, doorPos).canManipulate();
    }

    public static boolean hasDeniableInterface(ServerLevel level,
            BlockPos doorPos) {
        return inspect(level, doorPos).hasModernPanel();
    }

    /**
     * Opens every connected valid interface. Buttons enter OPENING/OPEN, readers
     * temporarily enter their ACCEPT state, and a legacy node is pulsed only when
     * no modern panel is connected.
     *
     * @return number of connected interfaces authorizing the action
     */
    public static int openConnectedControls(ServerLevel level, BlockPos doorPos) {
        ControlSnapshot controls = inspect(level, doorPos);
        if (!controls.canManipulate()) {
            return 0;
        }

        if (!controls.buttons().isEmpty()) {
            DoorButtonIndependentInteractionEvents.synchronizeDoorPanels(
                    level, doorPos, true);
        }
        for (BlockPos readerPos : controls.readers()) {
            setReaderAccepted(level, readerPos, true);
        }

        // A legacy pulse node is the fallback authorizer for installations that
        // intentionally have no button or reader attached to the door.
        if (!controls.hasModernPanel()) {
            for (BlockPos legacyPos : controls.legacyNodes()) {
                setLegacyNodePowered(level, legacyPos, true);
            }
        }
        return controls.authorizingCount();
    }

    /**
     * Closes every connected valid interface and removes its redstone output.
     *
     * @return number of connected interfaces authorizing the action
     */
    public static int closeConnectedControls(ServerLevel level, BlockPos doorPos) {
        ControlSnapshot controls = inspect(level, doorPos);
        if (!controls.canManipulate()) {
            return 0;
        }

        if (!controls.buttons().isEmpty()) {
            DoorButtonIndependentInteractionEvents.synchronizeDoorPanels(
                    level, doorPos, false);
        }
        for (BlockPos readerPos : controls.readers()) {
            setReaderAccepted(level, readerPos, false);
        }
        for (BlockPos legacyPos : controls.legacyNodes()) {
            setLegacyNodePowered(level, legacyPos, false);
        }
        return controls.authorizingCount();
    }

    /**
     * Temporarily replaces functional buttons with their locked visual and
     * readers with their ACCESS DENIED state. Authored decorative locked buttons
     * are never included by inspect(), so SCP-079 cannot convert or restore them.
     */
    public static int temporarilyDenyConnectedControls(ServerLevel level,
            BlockPos doorPos, int durationTicks) {
        ControlSnapshot controls = inspect(level, doorPos);
        if (!controls.hasModernPanel()) return 0;

        int changed = 0;
        for (BlockPos buttonPos : controls.buttons()) {
            if (denyButton(level, buttonPos, durationTicks)) changed++;
        }
        for (BlockPos readerPos : controls.readers()) {
            if (denyReader(level, readerPos, durationTicks)) changed++;
        }
        return changed;
    }

    private static boolean denyButton(ServerLevel level, BlockPos pos,
            int durationTicks) {
        BlockState current = level.getBlockState(pos);
        Block denied = deniedButton(current.getBlock());
        return denied != null && temporarilyReplace(level, pos, current,
                denied, durationTicks);
    }

    private static boolean denyReader(ServerLevel level, BlockPos pos,
            int durationTicks) {
        BlockState current = level.getBlockState(pos);
        String basePath = readerBasePath(current.getBlock());
        if (basePath == null) return false;
        Block denied = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(
                ScpAdditionsMod.MODID, basePath + "_wrong"));
        return denied != null && current.getBlock() != denied
                && temporarilyReplace(level, pos, current, denied,
                durationTicks);
    }

    private static boolean temporarilyReplace(ServerLevel level, BlockPos pos,
            BlockState original, Block deniedBlock, int durationTicks) {
        PanelKey key = new PanelKey(level.dimension(), pos.asLong());
        DenialState previous = ACTIVE_DENIALS.get(key);
        BlockState current = level.getBlockState(pos);
        long generation = DENIAL_GENERATION.incrementAndGet();

        if (previous != null && current.getBlock() == previous.deniedBlock()) {
            DenialState extended = new DenialState(previous.originalState(),
                    previous.deniedBlock(), generation);
            ACTIVE_DENIALS.put(key, extended);
            scheduleRestore(level, pos, key, extended,
                    Math.max(1, durationTicks));
            return true;
        }

        BlockState replacement = copySharedProperties(original,
                deniedBlock.defaultBlockState());
        if (!level.setBlock(pos, replacement, Block.UPDATE_ALL)) return false;

        DenialState active = new DenialState(original, deniedBlock, generation);
        ACTIVE_DENIALS.put(key, active);
        scheduleRestore(level, pos, key, active,
                Math.max(1, durationTicks));
        return true;
    }

    private static void scheduleRestore(ServerLevel level, BlockPos pos,
            PanelKey key, DenialState expected, int delay) {
        ScpAdditionsMod.queueServerWork(delay,
                () -> restoreWhenLoaded(level, pos, key, expected));
    }

    private static void restoreWhenLoaded(ServerLevel level, BlockPos pos,
            PanelKey key, DenialState expected) {
        DenialState active = ACTIVE_DENIALS.get(key);
        if (active == null || active.generation() != expected.generation()) {
            return;
        }
        if (!level.hasChunkAt(pos)) {
            scheduleRestore(level, pos, key, expected, 20);
            return;
        }

        BlockState current = level.getBlockState(pos);
        if (current.getBlock() == active.deniedBlock()) {
            level.setBlock(pos, active.originalState(), Block.UPDATE_ALL);
        }
        ACTIVE_DENIALS.remove(key, active);
    }

    private static Block deniedButton(Block block) {
        if (block == FacilityModule.BUTTON_CLOSED.get()
                || block == FacilityModule.BUTTON_OPENING.get()
                || block == FacilityModule.BUTTON_OPEN.get()
                || block == FacilityModule.BUTTON_CLOSING.get()) {
            return FacilityModule.BUTTON_LOCKED.get();
        }
        if (LeftDoorButtons.isFunctional(block)) {
            return LeftDoorButtons.BUTTON_LOCKED.get();
        }
        if (MirroredDoorButtons.isFunctional(block)) {
            return MirroredDoorButtons.BUTTON_LOCKED.get();
        }
        return null;
    }

    private static ControlSnapshot inspect(ServerLevel level, BlockPos doorPos) {
        Set<BlockPos> buttons = new LinkedHashSet<>();
        Set<BlockPos> readers = new LinkedHashSet<>();
        Set<BlockPos> legacyNodes = new LinkedHashSet<>();

        // Heavy doors accept redstone at the base and through the two upper relay
        // heights. Only horizontal neighbours represent wall-mounted controls.
        for (int yOffset = 0; yOffset <= 2; yOffset++) {
            BlockPos probe = doorPos.above(yOffset);
            for (Direction direction : HORIZONTAL) {
                BlockPos candidate = probe.relative(direction);
                Block block = level.getBlockState(candidate).getBlock();
                if (isFunctionalButton(block)) {
                    buttons.add(candidate.immutable());
                } else if (readerBasePath(block) != null) {
                    readers.add(candidate.immutable());
                } else if (isLegacyNode(block)) {
                    legacyNodes.add(candidate.immutable());
                }
            }
        }
        return new ControlSnapshot(buttons, readers, legacyNodes);
    }

    private static boolean isFunctionalButton(Block block) {
        return block == FacilityModule.BUTTON_CLOSED.get()
                || block == FacilityModule.BUTTON_OPENING.get()
                || block == FacilityModule.BUTTON_OPEN.get()
                || block == FacilityModule.BUTTON_CLOSING.get()
                || LeftDoorButtons.isFunctional(block)
                || MirroredDoorButtons.isFunctional(block);
    }

    private static boolean isLegacyNode(Block block) {
        return block == ScpAdditionsModBlocks.SCP_079CONTROL.get()
                || block == ScpAdditionsModBlocks.SCP_079CONTROLOFF.get();
    }

    private static String readerBasePath(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null || !ScpAdditionsMod.MODID.equals(id.getNamespace())) {
            return null;
        }

        String path = id.getPath();
        if (path.endsWith("_accept")) {
            path = path.substring(0, path.length() - "_accept".length());
        } else if (path.endsWith("_wrong")) {
            path = path.substring(0, path.length() - "_wrong".length());
        }
        return READER_BASE_PATHS.contains(path) ? path : null;
    }

    private static boolean setReaderAccepted(ServerLevel level, BlockPos pos,
            boolean accepted) {
        BlockState current = level.getBlockState(pos);
        String basePath = readerBasePath(current.getBlock());
        if (basePath == null) {
            return false;
        }

        ResourceLocation targetId = new ResourceLocation(ScpAdditionsMod.MODID,
                accepted ? basePath + "_accept" : basePath);
        Block target = ForgeRegistries.BLOCKS.getValue(targetId);
        if (target == null) {
            return false;
        }

        BlockState replacement = copySharedProperties(current,
                target.defaultBlockState());
        if (current.equals(replacement)) {
            return false;
        }
        return level.setBlock(pos, replacement, Block.UPDATE_ALL);
    }

    private static boolean setLegacyNodePowered(ServerLevel level, BlockPos pos,
            boolean powered) {
        BlockState current = level.getBlockState(pos);
        if (!isLegacyNode(current.getBlock())) {
            return false;
        }

        Block target = powered
                ? ScpAdditionsModBlocks.SCP_079CONTROL.get()
                : ScpAdditionsModBlocks.SCP_079CONTROLOFF.get();
        BlockState replacement = copySharedProperties(current,
                target.defaultBlockState());
        boolean changed = !current.equals(replacement)
                && level.setBlock(pos, replacement, Block.UPDATE_ALL);

        if (powered) {
            ScpAdditionsMod.queueServerWork(LEGACY_PULSE_TICKS, () -> {
                BlockState later = level.getBlockState(pos);
                if (later.getBlock()
                        == ScpAdditionsModBlocks.SCP_079CONTROL.get()) {
                    BlockState off = copySharedProperties(later,
                            ScpAdditionsModBlocks.SCP_079CONTROLOFF.get()
                                    .defaultBlockState());
                    level.setBlock(pos, off, Block.UPDATE_ALL);
                }
            });
        }
        return changed || current.getBlock() == target;
    }

    private static BlockState copySharedProperties(BlockState source,
            BlockState target) {
        BlockState result = target;
        if (source.hasProperty(HorizontalDirectionalBlock.FACING)
                && result.hasProperty(HorizontalDirectionalBlock.FACING)) {
            result = result.setValue(HorizontalDirectionalBlock.FACING,
                    source.getValue(HorizontalDirectionalBlock.FACING));
        }
        if (source.hasProperty(BlockStateProperties.WATERLOGGED)
                && result.hasProperty(BlockStateProperties.WATERLOGGED)) {
            result = result.setValue(BlockStateProperties.WATERLOGGED,
                    source.getValue(BlockStateProperties.WATERLOGGED));
        }
        return result;
    }

    private record ControlSnapshot(Set<BlockPos> buttons,
            Set<BlockPos> readers, Set<BlockPos> legacyNodes) {
        private boolean hasModernPanel() {
            return !buttons.isEmpty() || !readers.isEmpty();
        }

        private boolean canManipulate() {
            return hasModernPanel() || !legacyNodes.isEmpty();
        }

        private int authorizingCount() {
            if (hasModernPanel()) {
                return buttons.size() + readers.size();
            }
            return legacyNodes.size();
        }
    }

    private record PanelKey(ResourceKey<Level> dimension, long pos) {
    }

    private record DenialState(BlockState originalState, Block deniedBlock,
                               long generation) {
    }
}
