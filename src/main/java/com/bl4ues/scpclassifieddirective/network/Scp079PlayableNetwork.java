package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Small role-state channel for the playable SCP-079 client. */
public final class Scp079PlayableNetwork {
    private static boolean registered;

    private Scp079PlayableNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(State.class,
                State::encode, State::decode, State::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(ReleaseRequest.class,
                ReleaseRequest::encode, ReleaseRequest::decode,
                ReleaseRequest::handle);
    }

    public static void sendState(ServerPlayer player,
            ResourceKey<Level> dimension, BlockPos hostPos, int power,
            boolean auxiliaryOnline, boolean networkAvailable) {
        if (player == null || dimension == null || hostPos == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new State(true, dimension.location(), hostPos,
                        Math.max(0, Math.min(100, power)), auxiliaryOnline,
                        networkAvailable));
    }

    public static void sendInactive(ServerPlayer player) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new State(false, Level.OVERWORLD.location(), BlockPos.ZERO,
                        0, false, false));
    }

    public static void requestRelease() {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new ReleaseRequest());
    }

    public record State(boolean active, ResourceLocation dimension,
            BlockPos hostPos, int power, boolean auxiliaryOnline,
            boolean networkAvailable) {
        private static void encode(State message, FriendlyByteBuf buffer) {
            buffer.writeBoolean(message.active);
            buffer.writeResourceLocation(message.dimension);
            buffer.writeBlockPos(message.hostPos);
            buffer.writeVarInt(message.power);
            buffer.writeBoolean(message.auxiliaryOnline);
            buffer.writeBoolean(message.networkAvailable);
        }

        private static State decode(FriendlyByteBuf buffer) {
            return new State(buffer.readBoolean(),
                    buffer.readResourceLocation(), buffer.readBlockPos(),
                    buffer.readVarInt(), buffer.readBoolean(),
                    buffer.readBoolean());
        }

        private static void handle(State message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient
                            .receive(message.active, message.dimension,
                                    message.hostPos, message.power,
                                    message.auxiliaryOnline,
                                    message.networkAvailable)));
            context.setPacketHandled(true);
        }
    }

    public record ReleaseRequest() {
        private static void encode(ReleaseRequest message,
                FriendlyByteBuf buffer) {
        }

        private static ReleaseRequest decode(FriendlyByteBuf buffer) {
            return new ReleaseRequest();
        }

        private static void handle(ReleaseRequest message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null && Scp079PlayableManager.isController(player)) {
                    Scp079PlayableManager.release(player);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
