package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.mcreator.scpadditions.ScpAdditionsMod;

public final class ScpEntityNetwork {
    private static boolean registered;

    private ScpEntityNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpAdditionsMod.addNetworkMessage(Scp131NoticePacket.class,
                Scp131NoticePacket::encode, Scp131NoticePacket::decode, Scp131NoticePacket::handle);
        ScpAdditionsMod.addNetworkMessage(Scp131StopPacket.class,
                Scp131StopPacket::encode, Scp131StopPacket::decode, Scp131StopPacket::handle);
        ScpAdditionsMod.addNetworkMessage(BlinkStatePacket.class,
                BlinkStatePacket::encode, BlinkStatePacket::decode, BlinkStatePacket::handle);
        ScpAdditionsMod.addNetworkMessage(BlinkInputStatePacket.class,
                BlinkInputStatePacket::encode, BlinkInputStatePacket::decode, BlinkInputStatePacket::handle);
        ScpAdditionsMod.addNetworkMessage(Scp173ObservationPacket.class,
                Scp173ObservationPacket::encode, Scp173ObservationPacket::decode, Scp173ObservationPacket::handle);
        ScpAdditionsMod.addNetworkMessage(ScareSoundPacket.class,
                ScareSoundPacket::encode, ScareSoundPacket::decode, ScareSoundPacket::handle);
        ScpAdditionsMod.addNetworkMessage(EnterSoundPacket.class,
                EnterSoundPacket::encode, EnterSoundPacket::decode, EnterSoundPacket::handle);
        ScpAdditionsMod.addNetworkMessage(Scp1176MusicPacket.class,
                Scp1176MusicPacket::encode, Scp1176MusicPacket::decode, Scp1176MusicPacket::handle);
        ScpAdditionsMod.addNetworkMessage(KeycardReaderOpenScreenPacket.class,
                KeycardReaderOpenScreenPacket::encode, KeycardReaderOpenScreenPacket::decode,
                KeycardReaderOpenScreenPacket::handle);
        ScpAdditionsMod.addNetworkMessage(KeycardReaderSetLevelPacket.class,
                KeycardReaderSetLevelPacket::encode, KeycardReaderSetLevelPacket::decode,
                KeycardReaderSetLevelPacket::handle);
        ScpAdditionsMod.addNetworkMessage(KeycardReaderApplySavedLevelPacket.class,
                KeycardReaderApplySavedLevelPacket::encode, KeycardReaderApplySavedLevelPacket::decode,
                KeycardReaderApplySavedLevelPacket::handle);
        ScpAdditionsMod.addNetworkMessage(KeycardReaderCopyLevelPacket.class,
                KeycardReaderCopyLevelPacket::encode, KeycardReaderCopyLevelPacket::decode,
                KeycardReaderCopyLevelPacket::handle);
        // Append new packets after the established sequence so existing packet IDs
        // stay stable for clients and servers built from earlier 3.0.x sources.
        ScpAdditionsMod.addNetworkMessage(EquipmentProgressPacket.class,
                EquipmentProgressPacket::encode, EquipmentProgressPacket::decode,
                EquipmentProgressPacket::handle);
        ScpAdditionsMod.addNetworkMessage(HazmatRemovalInputPacket.class,
                HazmatRemovalInputPacket::encode, HazmatRemovalInputPacket::decode,
                HazmatRemovalInputPacket::handle);
        ScpAdditionsMod.addNetworkMessage(HazmatAudioPacket.class,
                HazmatAudioPacket::encode, HazmatAudioPacket::decode,
                HazmatAudioPacket::handle);
    }

    public static void showScp131Notice(ServerPlayer player, boolean following) {
        if (player == null || player.isSpectator()) return;
        ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new Scp131NoticePacket(following));
    }

    public static void setBlinkActive(ServerPlayer player, boolean active) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new BlinkStatePacket(active));
    }

    public static void beginEquipmentProgress(ServerPlayer player, int durationTicks) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
                EquipmentProgressPacket.begin(durationTicks));
    }

    public static void syncEquipmentProgress(ServerPlayer player, int elapsedTicks,
            int durationTicks) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
                EquipmentProgressPacket.sync(elapsedTicks, durationTicks));
    }

    public static void completeEquipmentProgress(ServerPlayer player) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
                EquipmentProgressPacket.complete());
    }

    public static void cancelEquipmentProgress(ServerPlayer player) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
                EquipmentProgressPacket.cancel());
    }

    public static void beginHazmatEquipAudio(ServerPlayer player) {
        sendHazmatAudio(player, HazmatAudioPacket.BEGIN_EQUIP);
    }

    public static void beginHazmatRemoveAudio(ServerPlayer player) {
        sendHazmatAudio(player, HazmatAudioPacket.BEGIN_REMOVE);
    }

    public static void completeHazmatAudio(ServerPlayer player) {
        sendHazmatAudio(player, HazmatAudioPacket.COMPLETE_ACTION);
    }

    public static void cancelHazmatAudio(ServerPlayer player) {
        sendHazmatAudio(player, HazmatAudioPacket.CANCEL_ACTION);
    }

    private static void sendHazmatAudio(ServerPlayer player, int action) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
                new HazmatAudioPacket(action));
    }

    public static void playScare(ServerPlayer player) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ScareSoundPacket());
    }

    public static void playEnterSound(ServerPlayer player) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new EnterSoundPacket());
    }

    public static void playScp1176Music(ServerPlayer player) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new Scp1176MusicPacket());
    }

    public static void openKeycardReaderScreen(ServerPlayer player, BlockPos pos, int level) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
                new KeycardReaderOpenScreenPacket(pos, level));
    }
}
