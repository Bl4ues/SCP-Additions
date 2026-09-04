package com.bl4ues.scpclassifieddirective.safezone;

import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorModule;
import com.bl4ues.scpclassifieddirective.scp012.Scp012Module;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Module;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Music catalog and automatic SCP soundtrack detection for safe zones. */
public enum SafeZoneTrack {
    SCP_914("scp_914", "SCP-914 Soundtrack", true),
    SCP_1176("scp_1176", "SCP-1176 Soundtrack", true),
    SCP_079("scp_079", "SCP-079 Soundtrack", true),
    SCP_012("scp_012", "SCP-012 Soundtrack", true),
    SCP_426("scp_426", "SCP-426 Soundtrack", true),
    SCP_294("scp_294", "SCP-294 Soundtrack", true),
    CORE_ROOM("core_room", "Core Room", true),
    OFFICES("offices", "Offices", false),
    SCP_131_CONTAINMENT("scp_131_containment",
            "SCP-131 Containment", false);

    private final String id;
    private final String displayName;
    private final boolean automatic;

    SafeZoneTrack(String id, String displayName, boolean automatic) {
        this.id = id;
        this.displayName = displayName;
        this.automatic = automatic;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public boolean automatic() {
        return automatic;
    }

    public static SafeZoneTrack byId(String id) {
        if (id != null) {
            for (SafeZoneTrack track : values()) {
                if (track.id.equals(id)) return track;
            }
        }
        return null;
    }

    public static boolean validId(String id) {
        if (id == null) return false;
        for (SafeZoneTrack track : values()) {
            if (track.id.equals(id)) return true;
        }
        return false;
    }

    public static boolean validManualId(String id) {
        SafeZoneTrack track = byId(id);
        return track != null && !track.automatic;
    }

    public static boolean isAutomaticId(String id) {
        SafeZoneTrack track = byId(id);
        return track != null && track.automatic;
    }

    public static List<SafeZoneTrack> manualTracks() {
        return java.util.Arrays.stream(values())
                .filter(track -> !track.automatic)
                .toList();
    }

    public static Detection detect(ServerLevel level, BlockPos min,
            BlockPos max) {
        if (level == null || min == null || max == null) {
            return Detection.EMPTY;
        }
        List<BlockPos> sources = new ArrayList<>();
        boolean[] found = new boolean[values().length];
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(cursor);
            SafeZoneTrack track = automaticTrackFor(state);
            if (track == null) continue;
            found[track.ordinal()] = true;
            if (sources.size() < 256) sources.add(cursor.immutable());
        }
        SafeZoneTrack selected = null;
        for (SafeZoneTrack track : values()) {
            if (track.automatic && found[track.ordinal()]) {
                selected = track;
                break;
            }
        }
        return new Detection(selected, List.copyOf(sources));
    }

    public static SafeZoneTrack automaticTrackFor(BlockState state) {
        if (state == null) return null;
        if (state.is(Scp914Module.SCP_914.get())) return SCP_914;
        if (state.is(ScpClassifiedDirectiveModBlocks.SCP_1176.get())) {
            return SCP_1176;
        }
        if (state.is(ScpClassifiedDirectiveModBlocks.SCP_079ON.get())
                || state.is(ScpClassifiedDirectiveModBlocks.SCP_079OFF.get())) {
            return SCP_079;
        }
        if (Scp012Module.isScp012(state)) return SCP_012;
        if (state.is(ScpClassifiedDirectiveModBlocks.SCP_426.get())) {
            return SCP_426;
        }
        if (state.is(ScpClassifiedDirectiveModBlocks.SCP_294.get())
                || state.is(ScpClassifiedDirectiveModBlocks.SCP_294_STOCKING.get())
                || state.is(ScpClassifiedDirectiveModBlocks.SCP_294_OUT_OF_RANGE.get())) {
            return SCP_294;
        }
        if (state.is(CoreRoomElevatorModule.STATION.get())) {
            return CORE_ROOM;
        }
        return null;
    }

    public record Detection(SafeZoneTrack track, List<BlockPos> sources) {
        private static final Detection EMPTY = new Detection(null, List.of());
    }
}
