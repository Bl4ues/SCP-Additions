package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.Scp079DecisionLog;
import net.mcreator.scpadditions.facility.Scp079DecisionLog.DecisionOutcome;
import net.mcreator.scpadditions.facility.Scp079DecisionLog.DecisionType;
import net.mcreator.scpadditions.network.Scp079DecisionPacket.DecisionEntry;
import net.mcreator.scpadditions.network.Scp079EnergyPacket.RoamerEntry;
import net.mcreator.scpadditions.roamer.RoamerResult;
import net.mcreator.scpadditions.roamer.RoamerState;
import net.mcreator.scpadditions.roamer.RoamerType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Client-only snapshot used by the optional developer HUDs. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class Scp079EnergyClientState {
    private static boolean energyVisible;
    private static boolean decisionLogVisible;
    private static boolean active;
    private static float energy;
    private static boolean spawnTimersVisible;
    private static final Map<RoamerType, ClientRoamerSnapshot> ROAMERS =
            new EnumMap<>(RoamerType.class);
    private static final List<ClientDecisionSnapshot> DECISIONS =
            new ArrayList<>();

    private Scp079EnergyClientState() {
    }

    public static void update(boolean shouldShowEnergy, boolean systemActive,
            float currentEnergy, boolean shouldShowSpawnTimers,
            List<RoamerEntry> entries) {
        energyVisible = shouldShowEnergy;
        active = systemActive;
        energy = Math.max(0.0F, Math.min(100.0F, currentEnergy));
        spawnTimersVisible = shouldShowSpawnTimers;
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
        decisionLogVisible = shouldShow;
        DECISIONS.clear();
        if (!shouldShow || entries == null) return;
        long now = clientGameTick();
        for (DecisionEntry entry : entries) {
            DECISIONS.add(new ClientDecisionSnapshot(entry.sequence(),
                    entry.type(), entry.outcome(), entry.pos(),
                    entry.dimension(), entry.context(), entry.cost(),
                    entry.ageTicks(), now));
        }
    }

    public static boolean visible() {
        return energyVisible;
    }

    public static boolean decisionLogVisible() {
        return decisionLogVisible;
    }

    public static boolean active() {
        return active;
    }

    public static float energy() {
        return energy;
    }

    public static boolean spawnTimersVisible() {
        return spawnTimersVisible;
    }

    public static List<ClientDecisionSnapshot> decisions() {
        if (!decisionLogVisible) return List.of();
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
        energyVisible = false;
        decisionLogVisible = false;
        active = false;
        energy = 0.0F;
        spawnTimersVisible = false;
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
