package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.network.Scp1576StatePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client snapshot of physical SCP-1576 sources and communication windows. */
public final class Scp1576ClientState {
    private static final Map<UUID, SessionState> ACTIVE = new ConcurrentHashMap<>();

    private Scp1576ClientState() {
    }

    public static void handle(Scp1576StatePacket packet) {
        switch (packet.action()) {
            case Scp1576StatePacket.WIND_START ->
                    Scp1576ClientAudio.startWind(packet.sessionId(),
                            packet.hostId(), packet.dimension(),
                            packet.x(), packet.y(), packet.z());
            case Scp1576StatePacket.WIND_CANCEL ->
                    Scp1576ClientAudio.cancelWind(packet.sessionId());
            case Scp1576StatePacket.ACTIVE_START -> {
                ACTIVE.put(packet.sessionId(), state(packet));
                Scp1576ClientAudio.finishWind(packet.sessionId());
                Scp1576ClientAudio.startSignal(packet.sessionId());
            }
            case Scp1576StatePacket.ACTIVE_UPDATE ->
                    ACTIVE.put(packet.sessionId(), state(packet));
            case Scp1576StatePacket.ACTIVE_STOP -> {
                ACTIVE.remove(packet.sessionId());
                Scp1576ClientAudio.endSignal(packet.sessionId());
            }
            default -> {
            }
        }
    }

    private static SessionState state(Scp1576StatePacket packet) {
        return new SessionState(packet.sessionId(), packet.hostId(),
                packet.hostName(), packet.dimension(), packet.x(), packet.y(),
                packet.z(), packet.voiceOpen(), packet.ageTicks(),
                packet.remainingTicks());
    }

    public static SessionState get(UUID sessionId) {
        return sessionId == null ? null : ACTIVE.get(sessionId);
    }

    public static List<SessionState> activeSessions() {
        return List.copyOf(new ArrayList<>(ACTIVE.values()));
    }

    public static void clear() {
        ACTIVE.clear();
        Scp1576ClientAudio.clear();
    }

    public record SessionState(UUID sessionId, UUID hostId, String hostName,
            ResourceLocation dimension, double x, double y, double z,
            boolean voiceOpen, int ageTicks, int remainingTicks) {
    }
}
