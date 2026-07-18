package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.HazmatAudioClient;

import java.util.function.Supplier;

/** Clientbound controls for local Hazmat Suit action and breathing audio. */
public final class HazmatAudioPacket {
    public static final int BEGIN_EQUIP = 0;
    public static final int BEGIN_REMOVE = 1;
    public static final int COMPLETE_ACTION = 2;
    public static final int CANCEL_ACTION = 3;

    private final int action;

    public HazmatAudioPacket(int action) {
        this.action = Math.max(BEGIN_EQUIP, Math.min(CANCEL_ACTION, action));
    }

    public static void encode(HazmatAudioPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.action);
    }

    public static HazmatAudioPacket decode(FriendlyByteBuf buffer) {
        return new HazmatAudioPacket(buffer.readVarInt());
    }

    public static void handle(HazmatAudioPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> applyClient(message.action)));
        context.setPacketHandled(true);
    }

    private static void applyClient(int action) {
        switch (action) {
            case BEGIN_EQUIP -> HazmatAudioClient.beginEquip();
            case BEGIN_REMOVE -> HazmatAudioClient.beginRemove();
            case COMPLETE_ACTION -> HazmatAudioClient.completeAction();
            case CANCEL_ACTION -> HazmatAudioClient.cancelAction();
            default -> HazmatAudioClient.cancelAction();
        }
    }
}
