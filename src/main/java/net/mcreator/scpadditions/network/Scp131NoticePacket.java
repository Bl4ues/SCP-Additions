package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.Scp131NoticeOverlay;

import java.util.function.Supplier;

public final class Scp131NoticePacket {
    private final boolean following;

    public Scp131NoticePacket(boolean following) {
        this.following = following;
    }

    public static void encode(Scp131NoticePacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.following);
    }

    public static Scp131NoticePacket decode(FriendlyByteBuf buffer) {
        return new Scp131NoticePacket(buffer.readBoolean());
    }

    public static void handle(Scp131NoticePacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Scp131NoticeOverlay.show(message.following)));
        context.setPacketHandled(true);
    }
}
