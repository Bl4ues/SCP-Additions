package net.mcreator.scpadditions.fabric.menu;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.Consumer;

public record LegacyMenuData(byte[] payload) {
    private static final int MAX_PAYLOAD_SIZE = 1 << 20;

    public static final StreamCodec<RegistryFriendlyByteBuf, LegacyMenuData> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> buffer.writeByteArray(data.payload()),
            buffer -> new LegacyMenuData(buffer.readByteArray(MAX_PAYLOAD_SIZE)));

    public static LegacyMenuData create(Consumer<FriendlyByteBuf> writer) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        writer.accept(buffer);
        byte[] payload = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), payload);
        buffer.release();
        return new LegacyMenuData(payload);
    }

    public FriendlyByteBuf toBuffer() {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(payload));
    }
}
