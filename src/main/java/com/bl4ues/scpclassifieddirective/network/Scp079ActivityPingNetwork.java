package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

/** One-way visual activity hints for the playable SCP-079 facility map. */
public final class Scp079ActivityPingNetwork {
    private static boolean registered;

    private Scp079ActivityPingNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(ActivityPing.class,
                ActivityPing::encode, ActivityPing::decode, ActivityPing::handle);
    }

    public static void send(ServerPlayer player, ResourceLocation dimension,
            UUID roomId, double x, double z) {
        if (player == null || dimension == null || roomId == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ActivityPing(dimension, roomId, x, z));
    }

    public record ActivityPing(ResourceLocation dimension, UUID roomId,
            double x, double z) {
        private static void encode(ActivityPing message, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(message.dimension);
            buffer.writeUUID(message.roomId);
            buffer.writeDouble(message.x);
            buffer.writeDouble(message.z);
        }

        private static ActivityPing decode(FriendlyByteBuf buffer) {
            return new ActivityPing(buffer.readResourceLocation(),
                    buffer.readUUID(), buffer.readDouble(), buffer.readDouble());
        }

        private static void handle(ActivityPing message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.client.scp079.Scp079ActivityPingClientState
                            .add(message.dimension, message.roomId,
                                    message.x, message.z)));
            context.setPacketHandled(true);
        }
    }
}
