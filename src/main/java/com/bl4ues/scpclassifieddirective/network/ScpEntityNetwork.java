package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079DecisionLog;
import com.bl4ues.scpclassifieddirective.facility.FacilitySignBlockEntity;
import com.bl4ues.scpclassifieddirective.facility.ScpSignSupportBlockEntity;
import com.bl4ues.scpclassifieddirective.facility.ScpSignTemplateLibrary;
import com.bl4ues.scpclassifieddirective.network.Scp079DecisionPacket.DecisionEntry;
import com.bl4ues.scpclassifieddirective.network.Scp079EnergyPacket.RoamerEntry;
import com.bl4ues.scpclassifieddirective.roamer.RoamerDebugSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class ScpEntityNetwork {
    private static boolean registered;

    private ScpEntityNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(Scp131NoticePacket.class,
                Scp131NoticePacket::encode, Scp131NoticePacket::decode,
                Scp131NoticePacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(Scp131StopPacket.class,
                Scp131StopPacket::encode, Scp131StopPacket::decode,
                Scp131StopPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(BlinkStatePacket.class,
                BlinkStatePacket::encode, BlinkStatePacket::decode,
                BlinkStatePacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(BlinkInputStatePacket.class,
                BlinkInputStatePacket::encode,
                BlinkInputStatePacket::decode,
                BlinkInputStatePacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(Scp173ObservationPacket.class,
                Scp173ObservationPacket::encode,
                Scp173ObservationPacket::decode,
                Scp173ObservationPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(ScareSoundPacket.class,
                ScareSoundPacket::encode, ScareSoundPacket::decode,
                ScareSoundPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(EnterSoundPacket.class,
                EnterSoundPacket::encode, EnterSoundPacket::decode,
                EnterSoundPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(Scp1176MusicPacket.class,
                Scp1176MusicPacket::encode, Scp1176MusicPacket::decode,
                Scp1176MusicPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(KeycardReaderOpenScreenPacket.class,
                KeycardReaderOpenScreenPacket::encode,
                KeycardReaderOpenScreenPacket::decode,
                KeycardReaderOpenScreenPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(KeycardReaderSetLevelPacket.class,
                KeycardReaderSetLevelPacket::encode,
                KeycardReaderSetLevelPacket::decode,
                KeycardReaderSetLevelPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(
                KeycardReaderApplySavedLevelPacket.class,
                KeycardReaderApplySavedLevelPacket::encode,
                KeycardReaderApplySavedLevelPacket::decode,
                KeycardReaderApplySavedLevelPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(KeycardReaderCopyLevelPacket.class,
                KeycardReaderCopyLevelPacket::encode,
                KeycardReaderCopyLevelPacket::decode,
                KeycardReaderCopyLevelPacket::handle);
        // Append new packets after the established sequence so existing packet
        // IDs stay stable inside each protocol version.
        ScpClassifiedDirectiveMod.addNetworkMessage(EquipmentProgressPacket.class,
                EquipmentProgressPacket::encode,
                EquipmentProgressPacket::decode,
                EquipmentProgressPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(HazmatRemovalInputPacket.class,
                HazmatRemovalInputPacket::encode,
                HazmatRemovalInputPacket::decode,
                HazmatRemovalInputPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(Scp714ExposurePacket.class,
                Scp714ExposurePacket::encode,
                Scp714ExposurePacket::decode,
                Scp714ExposurePacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(Scp012InfluencePacket.class,
                Scp012InfluencePacket::encode,
                Scp012InfluencePacket::decode,
                Scp012InfluencePacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(Scp079EnergyPacket.class,
                Scp079EnergyPacket::encode,
                Scp079EnergyPacket::decode,
                Scp079EnergyPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(FacilityDiagnosticsPacket.class,
                FacilityDiagnosticsPacket::encode,
                FacilityDiagnosticsPacket::decode,
                FacilityDiagnosticsPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(Scp079DecisionPacket.class,
                Scp079DecisionPacket::encode,
                Scp079DecisionPacket::decode,
                Scp079DecisionPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(Scp106ChasePacket.class,
                Scp106ChasePacket::encode,
                Scp106ChasePacket::decode,
                Scp106ChasePacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(FacilitySignOpenScreenPacket.class,
                FacilitySignOpenScreenPacket::encode,
                FacilitySignOpenScreenPacket::decode,
                FacilitySignOpenScreenPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(FacilitySignSavePacket.class,
                FacilitySignSavePacket::encode,
                FacilitySignSavePacket::decode,
                FacilitySignSavePacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(FacilitySignClipboardPacket.class,
                FacilitySignClipboardPacket::encode,
                FacilitySignClipboardPacket::decode,
                FacilitySignClipboardPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(ScpSignOpenScreenPacket.class,
                ScpSignOpenScreenPacket::encode,
                ScpSignOpenScreenPacket::decode,
                ScpSignOpenScreenPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(ScpSignSavePacket.class,
                ScpSignSavePacket::encode,
                ScpSignSavePacket::decode,
                ScpSignSavePacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(ScpSignTemplateRequestPacket.class,
                ScpSignTemplateRequestPacket::encode,
                ScpSignTemplateRequestPacket::decode,
                ScpSignTemplateRequestPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(ScpSignTemplateDataPacket.class,
                ScpSignTemplateDataPacket::encode,
                ScpSignTemplateDataPacket::decode,
                ScpSignTemplateDataPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(ScpSignTemplateLibraryPacket.class,
                ScpSignTemplateLibraryPacket::encode,
                ScpSignTemplateLibraryPacket::decode,
                ScpSignTemplateLibraryPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(ScpSignTemplateUploadPacket.class,
                ScpSignTemplateUploadPacket::encode,
                ScpSignTemplateUploadPacket::decode,
                ScpSignTemplateUploadPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(ScpSignTemplateDeletePacket.class,
                ScpSignTemplateDeletePacket::encode,
                ScpSignTemplateDeletePacket::decode,
                ScpSignTemplateDeletePacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(
                ElevatorArrivalOpenScreenPacket.class,
                ElevatorArrivalOpenScreenPacket::encode,
                ElevatorArrivalOpenScreenPacket::decode,
                ElevatorArrivalOpenScreenPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(ElevatorArrivalSavePacket.class,
                ElevatorArrivalSavePacket::encode,
                ElevatorArrivalSavePacket::decode,
                ElevatorArrivalSavePacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(
                ElevatorArrivalDisplayPacket.class,
                ElevatorArrivalDisplayPacket::encode,
                ElevatorArrivalDisplayPacket::decode,
                ElevatorArrivalDisplayPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(FacilityDiagnosticsResetPacket.class,
                FacilityDiagnosticsResetPacket::encode,
                FacilityDiagnosticsResetPacket::decode,
                FacilityDiagnosticsResetPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(AdvancementCatalogRequestPacket.class,
                AdvancementCatalogRequestPacket::encode,
                AdvancementCatalogRequestPacket::decode,
                AdvancementCatalogRequestPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(AdvancementCatalogPacket.class,
                AdvancementCatalogPacket::encode,
                AdvancementCatalogPacket::decode,
                AdvancementCatalogPacket::handle);
    }

    public static void sendAdvancementCatalog(ServerPlayer player) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                AdvancementCatalogPacket.fromPlayer(player));
    }

    public static void showScp131Notice(ServerPlayer player,
            boolean following) {
        if (player == null || player.isSpectator()) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp131NoticePacket(following));
    }

    public static void setBlinkActive(ServerPlayer player, boolean active) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new BlinkStatePacket(active));
    }

    public static void beginEquipmentProgress(ServerPlayer player,
            int durationTicks) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                EquipmentProgressPacket.begin(durationTicks));
    }

    public static void syncEquipmentProgress(ServerPlayer player,
            int elapsedTicks, int durationTicks) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                EquipmentProgressPacket.sync(elapsedTicks, durationTicks));
    }

    public static void completeEquipmentProgress(ServerPlayer player) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                EquipmentProgressPacket.complete());
    }

    public static void cancelEquipmentProgress(ServerPlayer player) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                EquipmentProgressPacket.cancel());
    }

    public static void syncScp714Exposure(ServerPlayer player,
            boolean active, float progress, boolean immobilized) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp714ExposurePacket(active, progress, immobilized));
    }

    public static void syncScp012Influence(ServerPlayer player,
            boolean active, BlockPos target, float contactProgress,
            boolean damageActive) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp012InfluencePacket(active, target, contactProgress,
                        damageActive));
    }

    public static void syncDebugState(ServerPlayer player,
            boolean energyVisible, boolean active, float energy,
            float discovery, boolean auxiliaryOnline, boolean hostPresent,
            boolean protocolExposed, boolean spawnTimersVisible,
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
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp079EnergyPacket(energyVisible, active, energy, discovery,
                        auxiliaryOnline, hostPresent, protocolExposed,
                        spawnTimersVisible, entries));
    }

    public static void openFacilityDiagnostics(ServerPlayer player,
            com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager
                    .DiagnosticSnapshot snapshot, BlockPos terminalPos) {
        if (player == null || snapshot == null || terminalPos == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new FacilityDiagnosticsPacket(snapshot, terminalPos));
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
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp079DecisionPacket(visible, entries));
    }

    public static void playScare(ServerPlayer player) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ScareSoundPacket());
    }

    public static void playScp1176Music(ServerPlayer player) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp1176MusicPacket());
    }

    public static void setScp106Chase(ServerPlayer player,
            boolean active) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp106ChasePacket(active));
    }

    public static void playEnterSound(ServerPlayer player) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new EnterSoundPacket());
    }

    public static void openKeycardReaderScreen(ServerPlayer player,
            BlockPos pos, int level) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new KeycardReaderOpenScreenPacket(pos, level));
    }

    public static void openFacilitySignScreen(ServerPlayer player,
            FacilitySignBlockEntity sign) {
        if (player == null || sign == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new FacilitySignOpenScreenPacket(
                        sign.getBlockPos(), sign.type(), sign.entries()));
    }

    public static void openScpSignScreen(ServerPlayer player,
            ScpSignSupportBlockEntity sign) {
        if (player == null || sign == null || player.getServer() == null) {
            return;
        }
        ScpSignTemplateLibrary library =
                ScpSignTemplateLibrary.get(player.getServer());
        ScpSignTemplateLibrary.Entry selected =
                library.entry(sign.data().templateId());
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ScpSignOpenScreenPacket(sign.getBlockPos(), sign.data(),
                        library.summaries(), selected));
    }

    public static void openElevatorArrivalEditor(ServerPlayer player,
            BlockPos stationPos,
            com.bl4ues.scpclassifieddirective.facility.elevator.
                    ElevatorArrivalDisplayData data) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ElevatorArrivalOpenScreenPacket(stationPos, data));
    }

    public static void showElevatorArrival(ServerPlayer player,
            com.bl4ues.scpclassifieddirective.facility.elevator.
                    ElevatorArrivalDisplayData data, int delayTicks) {
        if (player == null || data == null || !data.enabled()) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ElevatorArrivalDisplayPacket(data, delayTicks));
    }
}
