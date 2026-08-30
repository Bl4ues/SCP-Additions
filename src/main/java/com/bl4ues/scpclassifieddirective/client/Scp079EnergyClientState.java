package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079DecisionLog;
import com.bl4ues.scpclassifieddirective.facility.Scp079DecisionLog.DecisionOutcome;
import com.bl4ues.scpclassifieddirective.facility.Scp079DecisionLog.DecisionType;
import com.bl4ues.scpclassifieddirective.network.Scp079DecisionPacket.DecisionEntry;
import com.bl4ues.scpclassifieddirective.network.Scp079EnergyPacket.RoamerEntry;
import com.bl4ues.scpclassifieddirective.roamer.RoamerResult;
import com.bl4ues.scpclassifieddirective.roamer.RoamerState;
import com.bl4ues.scpclassifieddirective.roamer.RoamerType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Client-only snapshot used by the optional developer HUDs. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class Scp079EnergyClientState {
    private static boolean active;
    private static float energy;
    private static float discovery;
    private static boolean auxiliaryOnline;
    private static boolean hostPresent;
    private static boolean protocolExposed;
    private static final Map<RoamerType, ClientRoamerSnapshot> ROAMERS =
            new EnumMap<>(RoamerType.class);
    private static final List<ClientDecisionSnapshot> DECISIONS =
            new ArrayList<>();

    private Scp079EnergyClientState() {
    }

    public static void update(boolean shouldShowEnergy, boolean systemActive,
            float currentEnergy, float discoveryProgress,
            boolean isAuxiliaryOnline, boolean hasHost,
            boolean isProtocolExposed, boolean shouldShowSpawnTimers,
            List<RoamerEntry> entries) {
        active = systemActive;
        energy = Math.max(0.0F, Math.min(100.0F, currentEnergy));
        discovery = Math.max(0.0F, Math.min(100.0F, discoveryProgress));
        auxiliaryOnline = isAuxiliaryOnline;
        hostPresent = hasHost;
        protocolExposed = isProtocolExposed;
        ROAMERS.clear();
        long now = clientGameTick();
        if (entries != null) {
            for (RoamerEntry entry : entries) {
                ROAMERS.put(entry.type(), new ClientRoamerSnapshot(
                        entry.state(), entry.result(), entry.remainingTicks(),
                        now));
            }
        }
    }

    public static void replaceDecisions(boolean shouldShow,
            List<DecisionEntry> entries) {
        DECISIONS.clear();
        if (entries == null) return;
        long now = clientGameTick();
        for (DecisionEntry entry : entries) {
            DECISIONS.add(new ClientDecisionSnapshot(entry.sequence(),
                    entry.type(), entry.outcome(), entry.pos(),
                    entry.dimension(), entry.context(), entry.cost(),
                    entry.ageTicks(), now));
        }
    }

    public static boolean visible() {
        return StealthDebugClientPreferences.showScp079EnergyHud();
    }

    public static boolean decisionLogVisible() {
        return StealthDebugClientPreferences.showScp079DecisionLogHud();
    }

    public static boolean active() {
        return active;
    }

    public static float energy() {
        return energy;
    }

    public static float discovery() { return discovery; }
    public static boolean auxiliaryOnline() { return auxiliaryOnline; }
    public static boolean hostPresent() { return hostPresent; }
    public static boolean protocolExposed() { return protocolExposed; }

    public static boolean spawnTimersVisible() {
        return StealthDebugClientPreferences.showScpSpawnTimersHud();
    }

    public static List<ClientDecisionSnapshot> decisions() {
        if (!decisionLogVisible()) return List.of();
        long now = clientGameTick();
        List<ClientDecisionSnapshot> visible = new ArrayList<>();
        for (ClientDecisionSnapshot decision : DECISIONS) {
            if (decision.ageTicks(now)
                    < Scp079DecisionLog.CLIENT_LIFETIME_TICKS) {
                visible.add(decision);
            }
        }
        return List.copyOf(visible);
    }

    public static ClientRoamerSnapshot roamer(RoamerType type) {
        ClientRoamerSnapshot snapshot = ROAMERS.get(type);
        if (snapshot != null) return snapshot;
        return new ClientRoamerSnapshot(RoamerState.DISABLED,
                type != null && !type.spawnImplemented()
                        ? RoamerResult.NOT_IMPLEMENTED : RoamerResult.NONE,
                -1, clientGameTick());
    }

    public static void clear() {
        active = false;
        energy = 0.0F;
        discovery = 0.0F;
        auxiliaryOnline = false;
        hostPresent = false;
        protocolExposed = false;
        ROAMERS.clear();
        DECISIONS.clear();
    }

    private static long clientGameTick() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    public record ClientDecisionSnapshot(long sequence, DecisionType type,
            DecisionOutcome outcome, net.minecraft.core.BlockPos pos,
            String dimension, String context, float cost,
            int ageTicksAtSync, long clientTickAtSync) {
        public ClientDecisionSnapshot {
            if (type == null) type = DecisionType.ABORTED_ACTION;
            if (outcome == null) outcome = DecisionOutcome.ABORTED;
            if (pos == null) pos = net.minecraft.core.BlockPos.ZERO;
            if (dimension == null) dimension = "";
            if (context == null) context = "";
            cost = Math.max(0.0F, cost);
            ageTicksAtSync = Math.max(0, ageTicksAtSync);
        }

        public int ageTicks() {
            return ageTicks(clientGameTick());
        }

        private int ageTicks(long now) {
            long elapsed = Math.max(0L, now - clientTickAtSync);
            return (int) Math.min(Integer.MAX_VALUE,
                    ageTicksAtSync + elapsed);
        }
    }

    public record ClientRoamerSnapshot(RoamerState state, RoamerResult result,
            int remainingTicksAtSync, long clientTickAtSync) {
        public ClientRoamerSnapshot {
            if (state == null) state = RoamerState.DISABLED;
            if (result == null) result = RoamerResult.NONE;
            remainingTicksAtSync = Math.max(-1, remainingTicksAtSync);
        }

        public int remainingTicks() {
            if (state != RoamerState.COUNTDOWN || remainingTicksAtSync < 0) {
                return -1;
            }
            long elapsed = Math.max(0L,
                    clientGameTick() - clientTickAtSync);
            return (int) Math.max(0L, remainingTicksAtSync - elapsed);
        }
    }
}
