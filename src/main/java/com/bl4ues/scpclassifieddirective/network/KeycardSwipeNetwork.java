package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.KeycardSwipeClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Server cue for the purely visual physical keycard swipe. */
public final class KeycardSwipeNetwork {
    private static boolean registered;

    private KeycardSwipeNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(SwipeCue.class,
                SwipeCue::encode, SwipeCue::decode, SwipeCue::handle);
    }

    public static void broadcast(ServerLevel level, BlockPos pos,
            int keycardLevel, boolean objectContainmentUnit) {
        if (level == null || pos == null) return;
        SwipeCue cue = new SwipeCue(pos.immutable(), keycardLevel,
                objectContainmentUnit);
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(pos) > 4096.0D) continue;
            ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> player), cue);
        }
    }

    public record SwipeCue(BlockPos pos, int keycardLevel,
            boolean objectContainmentUnit) {
        private static void encode(SwipeCue cue, FriendlyByteBuf buffer) {
            buffer.writeBlockPos(cue.pos);
            buffer.writeByte(cue.keycardLevel);
            buffer.writeBoolean(cue.objectContainmentUnit);
        }

        private static SwipeCue decode(FriendlyByteBuf buffer) {
            return new SwipeCue(buffer.readBlockPos(),
                    buffer.readUnsignedByte(), buffer.readBoolean());
        }

        private static void handle(SwipeCue cue,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> KeycardSwipeClient.start(cue.pos,
                            cue.keycardLevel, cue.objectContainmentUnit)));
            context.setPacketHandled(true);
        }
    }
}
