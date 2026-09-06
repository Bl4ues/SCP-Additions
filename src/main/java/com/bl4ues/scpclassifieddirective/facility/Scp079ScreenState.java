package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.block.Scp079onBlock;
import com.bl4ues.scpclassifieddirective.facility.speaker.SpeakerBroadcastManager;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import com.bl4ues.scpclassifieddirective.network.Scp079AudioNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Controls the physical CRT face independently from SCP-079's AI logic. */
public final class Scp079ScreenState {
    private static final int ACTION_PULSE_TICKS = 60;
    private static final float CRT_STATE_SOUND_VOLUME = 0.42F;
    private static final float SPEAKER_CRT_CUE_VOLUME = 0.78F;
    private static final Map<MinecraftServer, Set<Scp079FacilityAccessSavedData.TrackedPosition>>
            LOCAL_ACTIVE = new WeakHashMap<>();
    private static final Map<MinecraftServer, Map<Scp079FacilityAccessSavedData.TrackedPosition, Long>>
            ACTION_ACTIVE_UNTIL = new WeakHashMap<>();

    private Scp079ScreenState() {
    }

    /**
     * Lights every physical SCP-079 host for three seconds after a real
     * facility action. Repeating an action restarts the timer rather than
     * allowing an older scheduled tick to switch the CRT off early.
     */
    public static void pulse(MinecraftServer server) {
        if (server == null) return;
        for (Scp079FacilityAccessSavedData.TrackedPosition host
                : Set.copyOf(Scp079FacilityAccessSavedData.get(server).hosts())) {
            ServerLevel level = level(server, host.dimension());
            if (level == null) continue;
            BlockPos pos = BlockPos.of(host.packedPos());
            long until = level.getGameTime() + ACTION_PULSE_TICKS;
            synchronized (ACTION_ACTIVE_UNTIL) {
                ACTION_ACTIVE_UNTIL.computeIfAbsent(server,
                        ignored -> new HashMap<>()).put(host, until);
            }
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
                tracked(level, pos);
        synchronized (LOCAL_ACTIVE) {
            Set<Scp079FacilityAccessSavedData.TrackedPosition> activeHosts =
                    LOCAL_ACTIVE.computeIfAbsent(server,
                            ignored -> new HashSet<>());
            if (active) activeHosts.add(tracked);
            else activeHosts.remove(tracked);
            if (activeHosts.isEmpty()) LOCAL_ACTIVE.remove(server);
        }
        if (active) setPoweredFace(level, pos, true);
        else settle(level, pos);
    }

    public static void refreshLocal(ServerLevel level, BlockPos pos) {
        if (isLocalActive(level, pos) || isActionActive(level, pos)) {
            setPoweredFace(level, pos, true);
        }
    }

    /** Called by the ON block's scheduled tick. */
    public static void settle(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || isLocalActive(level, pos)) return;
        Scp079FacilityAccessSavedData.TrackedPosition tracked = tracked(level, pos);
        long now = level.getGameTime();
        Long until;
        synchronized (ACTION_ACTIVE_UNTIL) {
            Map<Scp079FacilityAccessSavedData.TrackedPosition, Long> active =
                    ACTION_ACTIVE_UNTIL.get(level.getServer());
            until = active == null ? null : active.get(tracked);
            if (until != null && now >= until) {
                active.remove(tracked);
                if (active.isEmpty()) ACTION_ACTIVE_UNTIL.remove(level.getServer());
                until = null;
            }
        }
        if (until != null) {
            int remaining = (int) Math.max(1L, until - now);
            level.scheduleTick(pos,
                    ScpClassifiedDirectiveModBlocks.SCP_079ON.get(), remaining);
            setPoweredFace(level, pos, true);
            return;
        }
        setPoweredFace(level, pos, false);
    }

    public static boolean isLocalActive(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;
        Scp079FacilityAccessSavedData.TrackedPosition tracked = tracked(level, pos);
        synchronized (LOCAL_ACTIVE) {
            Set<Scp079FacilityAccessSavedData.TrackedPosition> activeHosts =
                    LOCAL_ACTIVE.get(level.getServer());
            return activeHosts != null && activeHosts.contains(tracked);
        }
    }

    private static boolean isActionActive(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;
        Scp079FacilityAccessSavedData.TrackedPosition tracked = tracked(level, pos);
        synchronized (ACTION_ACTIVE_UNTIL) {
            Map<Scp079FacilityAccessSavedData.TrackedPosition, Long> active =
                    ACTION_ACTIVE_UNTIL.get(level.getServer());
            Long until = active == null ? null : active.get(tracked);
            return until != null && level.getGameTime() < until;
        }
    }

    private static Scp079FacilityAccessSavedData.TrackedPosition tracked(
            ServerLevel level, BlockPos pos) {
        return new Scp079FacilityAccessSavedData.TrackedPosition(
                level.dimension().location().toString(), pos.asLong());
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
        SoundEvent cue = powered
                ? ScpClassifiedDirectiveModSounds.SCP079_1.get()
                : ScpClassifiedDirectiveModSounds.SCP079_2.get();
        level.playSound(null, pos, cue, SoundSource.BLOCKS,
                CRT_STATE_SOUND_VOLUME, 1.0F);
        relayCueThroughScp079Speakers(level.getServer(), cue);
    }

    private static void relayCueThroughScp079Speakers(MinecraftServer server,
            SoundEvent cue) {
        if (server == null || cue == null) return;
        ServerPlayer operator = Scp079PlayableManager.controller(server);
        if (operator == null || !SpeakerBroadcastManager.isBroadcasting(operator)) {
            return;
        }
        for (SpeakerBroadcastManager.VoiceSource source
                : SpeakerBroadcastManager.voiceSources(server,
                operator.getUUID())) {
            if (source.sourceType() != SpeakerBroadcastManager.SourceType.SCP_079) {
                continue;
            }
            Scp079AudioNetwork.sendSpeakerCue(server, source.dimension(),
                    cue.getLocation(), source.position().x, source.position().y,
                    source.position().z, SPEAKER_CRT_CUE_VOLUME, 1.0F);
        }
    }

    private static ServerLevel level(MinecraftServer server,
            String dimension) {
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        if (id == null) return null;
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }
}
