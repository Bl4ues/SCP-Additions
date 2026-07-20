package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.DistExecutor;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;
import net.mcreator.scpadditions.client.gui.KeycardReaderConfigScreen;

import java.util.function.Supplier;

public final class KeycardReaderOpenScreenPacket {
    private final BlockPos pos;
    private final int level;

    public KeycardReaderOpenScreenPacket(BlockPos pos, int level) {
        this.pos = pos.immutable();
        this.level = level;
    }

    public static void encode(KeycardReaderOpenScreenPacket message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeByte(message.level);
    }

    public static KeycardReaderOpenScreenPacket decode(FriendlyByteBuf buffer) {
        return new KeycardReaderOpenScreenPacket(buffer.readBlockPos(), buffer.readUnsignedByte());
    }

    public static void handle(KeycardReaderOpenScreenPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> KeycardReaderConfigScreen.open(message.pos, message.level)));
        context.setPacketHandled(true);
    }
}
