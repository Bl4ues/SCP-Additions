package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

/**
 * Cheap client-side probe for the Blackout command. Presence and immediate
 * usability are deliberately separate: a room keeps its Blackout row while its
 * compatible lamps are currently suppressed, but the row is disabled until a
 * powered light is available again. Rooms with no compatible lights stay clean.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079BlackoutAvailabilityClient {
    private static final int SCAN_HEIGHT = 8;
    private static final int BLOCK_BUDGET_PER_TICK = 1024;
    private static final int REFRESH_TICKS = 20;

    private static UUID roomId;
    private static FacilityRoomSnapshot room;
    private static int patchIndex;
    private static int x;
    private static int y;
    private static int z;
    private static boolean scanning;
    private static boolean supported;
    private static boolean available;
    private static long completedAt = Long.MIN_VALUE;

    private Scp079BlackoutAvailabilityClient() { }

    /** True when the room contains at least one light state Blackout can target. */
    public static boolean supported() {
        return Scp079PlayableClient.cameraMode() && supported;
    }

    /** True only when at least one compatible light is presently powered/on. */
    public static boolean available() {
        return Scp079PlayableClient.cameraMode() && available;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!Scp079PlayableClient.cameraMode() || minecraft.level == null) {
            clear();
            return;
        }

        FacilityRoomSnapshot current = FacilityMappingClientState.roomAt(
                Scp079PlayableClient.hostDimension(),
                BlockPos.containing(Scp079PlayableClient.viewPosition()));
        if (current == null) {
            clear();
            return;
        }

        long tick = minecraft.level.getGameTime();
        if (roomId == null || !roomId.equals(current.id())) {
            begin(current, false);
        } else if (!scanning && tick - completedAt >= REFRESH_TICKS) {
            // Preserve structural presence while the refresh walks the room so
            // a just-used Blackout never makes its own command row blink away.
            begin(current, true);
        }
        if (!scanning) return;

        int budget = BLOCK_BUDGET_PER_TICK;
        while (budget-- > 0 && scanning) {
            BlockPos pos = new BlockPos(x, y, z);
            if (isPotentialLight(minecraft, pos)) {
                supported = true;
                if (isValidPoweredLight(minecraft, pos)) {
                    available = true;
                    scanning = false;
                    completedAt = tick;
                    return;
                }
            }
            advance();
        }
        if (!scanning) completedAt = tick;
    }

    private static void begin(FacilityRoomSnapshot current,
            boolean preserveSupported) {
        room = current;
        roomId = current.id();
        patchIndex = 0;
        if (!preserveSupported) supported = false;
        available = false;
        completedAt = Long.MIN_VALUE;
        List<FacilityFloorPatch> patches = current.patches();
        if (patches.isEmpty()) {
            scanning = false;
            supported = false;
            return;
        }
        scanning = true;
        resetCursor(patches.get(0));
    }

    private static void advance() {
        if (room == null || !scanning) return;
        List<FacilityFloorPatch> patches = room.patches();
        FacilityFloorPatch patch = patches.get(patchIndex);
        if (++y <= patch.y() + SCAN_HEIGHT) return;
        y = patch.y();
        if (++z <= patch.maxZ()) return;
        z = patch.minZ();
        if (++x <= patch.maxX()) return;

        patchIndex++;
        if (patchIndex >= patches.size()) {
            scanning = false;
            available = false;
            return;
        }
        resetCursor(patches.get(patchIndex));
    }

    private static void resetCursor(FacilityFloorPatch patch) {
        x = patch.minX();
        z = patch.minZ();
        y = patch.y();
    }

    private static boolean isPotentialLight(Minecraft minecraft, BlockPos pos) {
        if (minecraft.level == null || !minecraft.level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = minecraft.level.getBlockState(pos);
        return state.hasProperty(BlockStateProperties.LIT)
                || state.hasProperty(BlockStateProperties.POWERED);
    }

    private static boolean isValidPoweredLight(Minecraft minecraft,
            BlockPos pos) {
        if (minecraft.level == null || !minecraft.level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = minecraft.level.getBlockState(pos);
        boolean active = state.hasProperty(BlockStateProperties.LIT)
                && state.getValue(BlockStateProperties.LIT)
                || state.hasProperty(BlockStateProperties.POWERED)
                && state.getValue(BlockStateProperties.POWERED);
        if (!active || state.getLightEmission(minecraft.level, pos) <= 0) {
            return false;
        }
        boolean powered = state.hasProperty(BlockStateProperties.POWERED)
                && state.getValue(BlockStateProperties.POWERED);
        return powered || minecraft.level.hasNeighborSignal(pos);
    }

    private static void clear() {
        roomId = null;
        room = null;
        patchIndex = 0;
        scanning = false;
        supported = false;
        available = false;
        completedAt = Long.MIN_VALUE;
    }
}
