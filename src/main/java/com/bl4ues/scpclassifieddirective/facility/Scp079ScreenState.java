package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.block.Scp079onBlock;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Controls the physical CRT face independently from SCP-079's AI logic. */
public final class Scp079ScreenState {
    private static final int ACTION_PULSE_TICKS = 8;
    private static final Map<MinecraftServer, Set<Scp079FacilityAccessSavedData.TrackedPosition>>
            LOCAL_ACTIVE = new WeakHashMap<>();

    private Scp079ScreenState() {
    }

    /** Briefly lights every physical SCP-079 host after a real facility action. */
    public static void pulse(MinecraftServer server) {
        if (server == null) return;
        for (Scp079FacilityAccessSavedData.TrackedPosition host
                : Set.copyOf(Scp079FacilityAccessSavedData.get(server).hosts())) {
            ServerLevel level = level(server, host.dimension());
            if (level == null) continue;
            BlockPos pos = BlockPos.of(host.packedPos());
            setPoweredFace(level, pos, true);
            level.scheduleTick(pos,
                    ScpClassifiedDirectiveModBlocks.SCP_079ON.get(),
                    ACTION_PULSE_TICKS);
        }
    }

    /** Keeps the CRT lit while a playable SCP-079 is observing its own host. */
    public static void setLocalActive(ServerLevel level, BlockPos pos,
            boolean active) {
        if (level == null || pos == null) return;
        MinecraftServer server = level.getServer();
        Scp079FacilityAccessSavedData.TrackedPosition tracked =
                new Scp079FacilityAccessSavedData.TrackedPosition(
                        level.dimension().location().toString(), pos.asLong());
        synchronized (LOCAL_ACTIVE) {
            Set<Scp079FacilityAccessSavedData.TrackedPosition> activeHosts =
                    LOCAL_ACTIVE.computeIfAbsent(server,
                            ignored -> new HashSet<>());
            if (active) activeHosts.add(tracked);
            else activeHosts.remove(tracked);
            if (activeHosts.isEmpty()) LOCAL_ACTIVE.remove(server);
        }
        setPoweredFace(level, pos, active);
    }

    public static void refreshLocal(ServerLevel level, BlockPos pos) {
        if (isLocalActive(level, pos)) setPoweredFace(level, pos, true);
    }

    /** Called by the ON block's scheduled tick. */
    public static void settle(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || isLocalActive(level, pos)) return;
        setPoweredFace(level, pos, false);
    }

    public static boolean isLocalActive(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;
        Scp079FacilityAccessSavedData.TrackedPosition tracked =
                new Scp079FacilityAccessSavedData.TrackedPosition(
                        level.dimension().location().toString(), pos.asLong());
        synchronized (LOCAL_ACTIVE) {
            Set<Scp079FacilityAccessSavedData.TrackedPosition> activeHosts =
                    LOCAL_ACTIVE.get(level.getServer());
            return activeHosts != null && activeHosts.contains(tracked);
        }
    }

    private static void setPoweredFace(ServerLevel level, BlockPos pos,
            boolean powered) {
        BlockState state = level.getBlockState(pos);
        boolean isOn = state.is(ScpClassifiedDirectiveModBlocks.SCP_079ON.get());
        boolean isOff = state.is(ScpClassifiedDirectiveModBlocks.SCP_079OFF.get());
        if ((!isOn && !isOff) || powered == isOn) return;

        BlockState next = (powered
                ? ScpClassifiedDirectiveModBlocks.SCP_079ON.get()
                : ScpClassifiedDirectiveModBlocks.SCP_079OFF.get())
                .defaultBlockState();
        if (state.hasProperty(Scp079onBlock.FACING)) {
            next = next.setValue(Scp079onBlock.FACING,
                    state.getValue(Scp079onBlock.FACING));
        }
        if (state.hasProperty(Scp079onBlock.WATERLOGGED)) {
            next = next.setValue(Scp079onBlock.WATERLOGGED,
                    state.getValue(Scp079onBlock.WATERLOGGED));
        }
        level.setBlock(pos, next, 3);
    }

    private static ServerLevel level(MinecraftServer server,
            String dimension) {
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        if (id == null) return null;
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }
}
