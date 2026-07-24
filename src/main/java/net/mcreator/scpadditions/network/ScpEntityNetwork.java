package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.Scp079DecisionLog;
import net.mcreator.scpadditions.network.Scp079DecisionPacket.DecisionEntry;
import net.mcreator.scpadditions.network.Scp079EnergyPacket.RoamerEntry;
import net.mcreator.scpadditions.roamer.RoamerDebugSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class ScpEntityNetwork {
    private static boolean registered;

    private ScpEntityNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpAdditionsMod.addNetworkMessage(Scp131NoticePacket.class,
                Scp131NoticePacket::encode, Scp131NoticePacket::decode,
                Scp131NoticePacket::handle);
        ScpAdditionsMod.addNetworkMessage(Scp131StopPacket.class,
                Scp131StopPacket::encode, Scp131StopPacket::decode,
                Scp131StopPacket::handle);
        ScpAdditionsMod.addNetworkMessage(BlinkStatePacket.class,
                BlinkStatePacket::encode, BlinkStatePacket::decode,
                BlinkStatePacket::handle);
        ScpAdditionsMod.addNetworkMessage(BlinkInputStatePacket.class,
                BlinkInputStatePacket::encode,
                BlinkInputStatePacket::decode,
                BlinkInputStatePacket::handle);
        ScpAdditionsMod.addNetworkMessage(Scp173ObservationPacket.class,
                Scp173ObservationPacket::encode,
                Scp173ObservationPacket::decode,
                Scp173ObservationPacket::handle);
        ScpAdditionsMod.addNetworkMessage(ScareSoundPacket.class,
                ScareSoundPacket::encode, ScareSoundPacket::decode,
                ScareSoundPacket::handle);
        ScpAdditionsMod.addNetworkMessage(EnterSoundPacket.class,
                EnterSoundPacket::encode, EnterSoundPacket::decode,
                EnterSoundPacket::handle);
        ScpAdditionsMod.addNetworkMessage(Scp1176MusicPacket.class,
                Scp1176MusicPacket::encode, Scp1176MusicPacket::decode,
                Scp1176MusicPacket::handle);
        ScpAdditionsMod.addNetworkMessage(KeycardReaderOpenScreenPacket.class,
                KeycardReaderOpenScreenPacket::encode,
                KeycardReaderOpenScreenPacket::decode,
                KeycardReaderOpenScreenPacket::handle);
        ScpAdditionsMod.addNetworkMessage(KeycardReaderSetLevelPacket.class,
                KeycardReaderSetLevelPacket::encode,
                KeycardReaderSetLevelPacket::decode,
                KeycardReaderSetLevelPacket::handle);
        ScpAdditionsMod.addNetworkMessage(
                KeycardReaderApplySavedLevelPacket.class,
                KeycardReaderApplySavedLevelPacket::encode,
                KeycardReaderApplySavedLevelPacket::decode,
                KeycardReaderApplySavedLevelPacket::handle);
        ScpAdditionsMod.addNetworkMessage(KeycardReaderCopyLevelPacket.class,
                KeycardReaderCopyLevelPacket::encode,
                KeycardReaderCopyLevelPacket::decode,
                KeycardReaderCopyLevelPacket::handle);
        // Append new packets after the established sequence so existing packet
        // IDs stay stable inside each protocol version.
        ScpAdditionsMod.addNetworkMessage(EquipmentProgressPacket.class,
                EquipmentProgressPacket::encode,
                EquipmentProgressPacket::decode,
                EquipmentProgressPacket::handle);
        ScpAdditionsMod.addNetworkMessage(HazmatRemovalInputPacket.class,
                HazmatRemovalInputPacket::encode,
                HazmatRemovalInputPacket::decode,
                HazmatRemovalInputPacket::handle);
        ScpAdditionsMod.addNetworkMessage(Scp714ExposurePacket.class,
                Scp714ExposurePacket::encode,
                Scp714ExposurePacket::decode,
                Scp714ExposurePacket::handle);
        ScpAdditionsMod.addNetworkMessage(Scp012InfluencePacket.class,
                Scp012InfluencePacket::encode,
                Scp012InfluencePacket::decode,
                Scp012InfluencePacket::handle);
        ScpAdditionsMod.addNetworkMessage(Scp079EnergyPacket.class,
                Scp079EnergyPacket::encode,
                Scp079EnergyPacket::decode,
                Scp079EnergyPacket::handle);
        ScpAdditionsMod.addNetworkMessage(Scp079DecisionPacket.class,
                Scp079DecisionPacket::encode,
                Scp079DecisionPacket::decode,
                Scp079DecisionPacket::handle);
        ScpAdditionsMod.addNetworkMessage(Scp106ChasePacket.class,
                Scp106ChasePacket::encode,
                Scp106ChasePacket::decode,
                Scp106ChasePacket::handle);
    }

    public static void showScp131Notice(ServerPlayer player,
            boolean following) {
        if (player == null || player.isSpectator()) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp131NoticePacket(following));
    }

    public static void setBlinkActive(ServerPlayer player, boolean active) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new BlinkStatePacket(active));
    }

    public static void beginEquipmentProgress(ServerPlayer player,
            int durationTicks) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                EquipmentProgressPacket.begin(durationTicks));
    }

    public static void syncEquipmentProgress(ServerPlayer player,
            int elapsedTicks, int durationTicks) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                EquipmentProgressPacket.sync(elapsedTicks, durationTicks));
    }

    public static void completeEquipmentProgress(ServerPlayer player) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                EquipmentProgressPacket.complete());
    }

    public static void cancelEquipmentProgress(ServerPlayer player) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                EquipmentProgressPacket.cancel());
    }

    public static void syncScp714Exposure(ServerPlayer player,
            boolean active, float progress, boolean immobilized) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp714ExposurePacket(active, progress, immobilized));
    }

    public static void syncScp012Influence(ServerPlayer player,
            boolean active, BlockPos target, float contactProgress,
            boolean damageActive) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp012InfluencePacket(active, target, contactProgress,
                        damageActive));
    }

    public static void syncDebugState(ServerPlayer player,
            boolean energyVisible, boolean active, float energy,
            boolean spawnTimersVisible,
            List<RoamerDebugSnapshot> snapshots) {
        if (player == null) return;
        MinecraftServer server = player.getServer();
        int currentTick = server == null ? 0 : server.getTickCount();
        List<RoamerEntry> entries = new ArrayList<>();
        if (snapshots != null) {
            for (RoamerDebugSnapshot snapshot : snapshots) {
                entries.add(new RoamerEntry(snapshot.type(), snapshot.state(),
                        snapshot.result(),
                        snapshot.remainingTicks(currentTick)));
            }
        }
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp079EnergyPacket(energyVisible, active, energy,
                        spawnTimersVisible, entries));
    }

    public static void syncScp079Decisions(ServerPlayer player,
            boolean visible, Scp079DecisionLog.Snapshot snapshot) {
        if (player == null) return;
        MinecraftServer server = player.getServer();
        long currentTick = server == null ? 0L : server.getTickCount();
        List<DecisionEntry> entries = new ArrayList<>();
        if (visible && snapshot != null) {
            for (Scp079DecisionLog.DecisionEntry entry : snapshot.entries()) {
                entries.add(new DecisionEntry(entry.sequence(), entry.type(),
                        entry.outcome(), entry.pos(), entry.dimension(),
                        entry.context(), entry.cost(), (int) Math.max(0L,
                        currentTick - entry.createdTick())));
            }
        }
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp079DecisionPacket(visible, entries));
    }

    public static void playScare(ServerPlayer player) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ScareSoundPacket());
    }

    public static void playScp1176Music(ServerPlayer player) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp1176MusicPacket());
    }

    public static void setScp106Chase(ServerPlayer player,
            boolean active) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp106ChasePacket(active));
    }

    public static void playEnterSound(ServerPlayer player) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new EnterSoundPacket());
    }

    public static void openKeycardReaderScreen(ServerPlayer player,
            BlockPos pos, int level) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new KeycardReaderOpenScreenPacket(pos, level));
    }
}
