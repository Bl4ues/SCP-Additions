package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.NetworkEvent;
import net.mcreator.scpadditions.equipment.HazmatSuitManager;

import java.util.function.Supplier;

/** Client-to-server hold state for crouch-and-use Hazmat removal. */
public final class HazmatRemovalInputPacket {
    private final boolean held;

    public HazmatRemovalInputPacket(boolean held) {
        this.held = held;
    }

    public static void encode(HazmatRemovalInputPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.held);
    }

    public static HazmatRemovalInputPacket decode(FriendlyByteBuf buffer) {
        return new HazmatRemovalInputPacket(buffer.readBoolean());
    }

    public static void handle(HazmatRemovalInputPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                HazmatSuitManager.setManualRemovalInput(player, message.held);
            }
        });
        context.setPacketHandled(true);
    }
}
