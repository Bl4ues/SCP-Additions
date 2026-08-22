package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Shared support contract for facility objects mounted on wall faces. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WallMountedSupportEvents {
    private static final Map<ServerLevel, Set<BlockPos>> PENDING_SCANS =
            new WeakHashMap<>();

    private WallMountedSupportEvents() {
    }

    public static boolean hasWallSupport(LevelReader level, BlockPos pos,
            Direction facing) {
        if (facing == null || facing.getAxis() == Direction.Axis.Y) return false;
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(
                level, supportPos, facing);
    }

    public static boolean hasLargePropWallSupport(LevelReader level,
            BlockPos controllerPos, FacilityLargePropStructure.Kind kind,
            Direction facing) {
        return hasLargePropWallSupport(level, controllerPos, kind, facing,
                FramedSignPosition.CENTER);
    }

    /** Checks every visible cell occupied by the chosen placement variant. */
    public static boolean hasLargePropWallSupport(LevelReader level,
            BlockPos controllerPos, FacilityLargePropStructure.Kind kind,
            Direction facing, FramedSignPosition position) {
        if (facing == null || facing.getAxis() == Direction.Axis.Y) return false;
        if (!hasWallSupport(level, controllerPos, facing)) return false;

        for (FacilityPropPartBlock.Part part :
                FacilityPropPartBlock.Part.forKind(kind, position)) {
            BlockPos visualCell = FacilityLargePropStructure.partPosition(
                    controllerPos, facing, part);
            if (!hasWallSupport(level, visualCell, facing)) return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void onNearbyNeighborUpdate(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            queueScan(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            queueScan(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            queueScan(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)) {
            return;
        }

        Set<BlockPos> pending;
        synchronized (PENDING_SCANS) {
            pending = PENDING_SCANS.remove(level);
        }
        if (pending == null) return;
        for (BlockPos source : pending) validateAround(level, source);
    }

    private static void queueScan(ServerLevel level, BlockPos pos) {
        synchronized (PENDING_SCANS) {
            PENDING_SCANS.computeIfAbsent(level, ignored -> new HashSet<>())
                    .add(pos.immutable());
        }
    }

    private static void validateAround(ServerLevel level, BlockPos source) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos candidate = source.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(candidate);
                    if (!isTrackedWallObject(state.getBlock())) continue;
                    if (!hasExpectedSupport(level, candidate, state)) {
                        destroyMountedObject(level, candidate, state);
                    }
                }
            }
        }
    }

    private static boolean hasExpectedSupport(ServerLevel level, BlockPos pos,
            BlockState state) {
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) return true;
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        Block block = state.getBlock();

        if (block instanceof AbstractFramedSignBlock framedSign) {
            return hasLargePropWallSupport(level, pos,
                    framedSign.framedSignKind(), facing,
                    state.getValue(AbstractFramedSignBlock.POSITION));
        }
        if (block == FacilityModule.TV.get()) {
            return hasLargePropWallSupport(level, pos,
                    FacilityLargePropStructure.Kind.TV, facing,
                    FramedSignPosition.CENTER);
        }

        if (isDoorButton(block)) {
            Direction screenLeft = facing.getClockWise();
            Direction visualOffset = LeftDoorButtons.isAny(block)
                    || MirroredDoorButtons.isAny(block)
                    ? screenLeft : screenLeft.getOpposite();
            return hasWallSupport(level, pos.relative(visualOffset), facing);
        }

        if (isKeycardReader(block)) {
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            boolean rightModel = id != null && id.getPath().contains("right");
            Direction screenLeft = facing.getClockWise();
            Direction visualOffset = rightModel
                    ? screenLeft.getOpposite() : screenLeft;
            return hasWallSupport(level, pos.relative(visualOffset), facing);
        }

        return hasWallSupport(level, pos, facing);
    }

    private static void destroyMountedObject(ServerLevel level, BlockPos pos,
            BlockState state) {
        if (state.is(FacilityModule.WALLLIGHT_2.get())) {
            BlockPos lower = pos.below();
            if (level.getBlockState(lower).is(FacilityModule.WALLLIGHT.get())) {
                level.destroyBlock(lower, true);
                return;
            }
        }
        level.destroyBlock(pos, true);
    }

    private static boolean isTrackedWallObject(Block block) {
        return block == FacilityModule.WALLLIGHT.get()
                || block == FacilityModule.WALLLIGHT_2.get()
                || block == FacilityModule.EMERGENCY_BUTTON.get()
                || block == FacilityModule.FIRE_EXTINGUISHER.get()
                || block == FacilityModule.WATER_FAUCET.get()
                || block instanceof AbstractFramedSignBlock
                || block == FacilityModule.CORE_ROOM_SIGN.get()
                || block == FacilityModule.DOOR_SIGN.get()
                || block == FacilityModule.TV.get()
                || isDoorButton(block)
                || isKeycardReader(block);
    }

    private static boolean isDoorButton(Block block) {
        return block == FacilityModule.BUTTON_LOCKED.get()
                || block == FacilityModule.BUTTON_CLOSED.get()
                || block == FacilityModule.BUTTON_OPENING.get()
                || block == FacilityModule.BUTTON_OPEN.get()
                || block == FacilityModule.BUTTON_CLOSING.get()
                || LeftDoorButtons.isAny(block)
                || MirroredDoorButtons.isAny(block);
    }

    private static boolean isKeycardReader(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null || !ScpClassifiedDirectiveMod.MODID.equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        return path.contains("reader")
                && (path.contains("left") || path.contains("right"));
    }
}
