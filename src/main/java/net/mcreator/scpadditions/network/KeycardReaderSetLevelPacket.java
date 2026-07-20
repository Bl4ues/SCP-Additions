package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.NetworkEvent;
import net.mcreator.scpadditions.init.UnifiedReaderItems;
import net.mcreator.scpadditions.keycard.KeycardReaderLevels;

import java.util.function.Supplier;

public final class KeycardReaderSetLevelPacket {
    private final BlockPos pos;
    private final int level;

    public KeycardReaderSetLevelPacket(BlockPos pos, int level) {
        this.pos = pos.immutable();
        this.level = level;
    }

    public static void encode(KeycardReaderSetLevelPacket message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeByte(message.level);
    }

    public static KeycardReaderSetLevelPacket decode(FriendlyByteBuf buffer) {
        return new KeycardReaderSetLevelPacket(buffer.readBlockPos(), buffer.readUnsignedByte());
    }

    public static void handle(KeycardReaderSetLevelPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || message.level < 1 || message.level > 6) {
                return;
            }

            boolean hasScrewdriver = player.getMainHandItem().is(UnifiedReaderItems.SCREWDRIVER.get())
                    || player.getOffhandItem().is(UnifiedReaderItems.SCREWDRIVER.get());
            if (!hasScrewdriver) {
                return;
            }

            if (player.distanceToSqr(Vec3.atCenterOf(message.pos)) > 64.0D) {
                return;
            }

            KeycardReaderLevels.replaceLevel(player.level(), message.pos, message.level);
        });
        context.setPacketHandled(true);
    }
}
