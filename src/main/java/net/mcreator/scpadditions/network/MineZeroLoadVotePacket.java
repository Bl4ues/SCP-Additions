package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.compat.MineZeroDeathCoordinator;

import java.util.function.Supplier;

/** Casts the sender's vote to rewind the current MineZero checkpoint. */
public final class MineZeroLoadVotePacket {
    public static void encode(MineZeroLoadVotePacket message,
            FriendlyByteBuf buffer) {
    }

    public static MineZeroLoadVotePacket decode(FriendlyByteBuf buffer) {
        return new MineZeroLoadVotePacket();
    }

    public static void handle(MineZeroLoadVotePacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        var player = context.getSender();
        context.enqueueWork(() -> {
            if (player != null) MineZeroDeathCoordinator.voteToRestore(player);
        });
        context.setPacketHandled(true);
    }
}
