package net.mcreator.scpadditions.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.mcreator.scpadditions.ScpAdditionsMod;

public final class ScpEntityNetwork {
    private static boolean registered;

    private ScpEntityNetwork() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        ScpAdditionsMod.addNetworkMessage(Scp131NoticePacket.class,
                Scp131NoticePacket::encode, Scp131NoticePacket::decode, Scp131NoticePacket::handle);
        ScpAdditionsMod.addNetworkMessage(Scp131StopPacket.class,
                Scp131StopPacket::encode, Scp131StopPacket::decode, Scp131StopPacket::handle);
    }

    public static void showScp131Notice(ServerPlayer player, boolean following) {
        ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
                new Scp131NoticePacket(following));
    }
}
