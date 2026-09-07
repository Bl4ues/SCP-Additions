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
 * Cheap client-side availability probe for the Blackout command. The server is
 * still authoritative; this only keeps an impossible command out of the 079 HUD.
 * Work is spread across ticks so a large lamp-less room does not become a new
 * source of the very hitch Blackout has already spent far too much time causing.
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
    private static boolean available;
    private static long completedAt = Long.MIN_VALUE;

    private Scp079BlackoutAvailabilityClient() {
    }

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
            begin(current);
        } else if (!scanning && tick - completedAt >= REFRESH_TICKS) {
            begin(current);
        }
        if (!scanning) return;

        int budget = BLOCK_BUDGET_PER_TICK;
        while (budget-- > 0 && scanning) {
            if (isValidPoweredLight(minecraft, new BlockPos(x, y, z))) {
                available = true;
                scanning = false;
                completedAt = tick;
                return;
            }
            advance();
        }
        if (!scanning) completedAt = tick;
    }

    private static void begin(FacilityRoomSnapshot current) {
        room = current;
        roomId = current.id();
        patchIndex = 0;
        available = false;
        completedAt = Long.MIN_VALUE;
        List<FacilityFloorPatch> patches = current.patches();
        if (patches.isEmpty()) {
            scanning = false;
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

    private static boolean isValidPoweredLight(Minecraft minecraft,
            BlockPos pos) {
        if (minecraft.level == null || !minecraft.level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = minecraft.level.getBlockState(pos);
        boolean potential = state.hasProperty(BlockStateProperties.LIT)
                || state.hasProperty(BlockStateProperties.POWERED);
        if (!potential) return false;
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
        available = false;
        completedAt = Long.MIN_VALUE;
    }
}
