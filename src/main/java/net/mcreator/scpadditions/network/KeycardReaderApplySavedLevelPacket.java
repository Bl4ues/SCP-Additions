package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.keycard.KeycardReaderInteractionEvents;

import java.util.function.Supplier;

public final class KeycardReaderApplySavedLevelPacket {
    private final BlockPos pos;

    public KeycardReaderApplySavedLevelPacket(BlockPos pos) {
        this.pos = pos.immutable();
    }

    public static void encode(KeycardReaderApplySavedLevelPacket message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
    }

    public static KeycardReaderApplySavedLevelPacket decode(FriendlyByteBuf buffer) {
        return new KeycardReaderApplySavedLevelPacket(buffer.readBlockPos());
    }

    public static void handle(KeycardReaderApplySavedLevelPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.distanceToSqr(Vec3.atCenterOf(message.pos)) > 64.0D
                    || !player.level().hasChunkAt(message.pos)) return;
            if (KeycardReaderInteractionEvents.tryHandleInteraction(player, message.pos, false, true)) {
                KeycardReaderInteractionEvents.suppressNextInteraction(player, message.pos);
            }
        });
        context.setPacketHandled(true);
    }
}
