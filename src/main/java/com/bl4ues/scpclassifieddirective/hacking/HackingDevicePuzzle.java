package com.bl4ues.scpclassifieddirective.hacking;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Arrays;

/**
 * One server-authored Hacking Device challenge.
 *
 * The client receives only the puzzle type/difficulty/public data. serverAnswer
 * deliberately stays server-side and is never encoded into the packet.
 */
public record HackingDevicePuzzle(Type type, int difficulty, int[] data,
        int serverAnswer) {
    public enum Type {
        CIRCUIT_PATH,
        VISUAL_CHECKSUM,
        FIREWALL_WINDOWS,
        HOLD_SIGNAL,
        FREQUENCY_LOCK
    }

    private static final int MAX_DATA = 24;

    public HackingDevicePuzzle {
        type = type == null ? Type.FREQUENCY_LOCK : type;
        difficulty = Math.max(1, Math.min(6, difficulty));
        data = sanitize(data);
    }

    @Override
    public int[] data() {
        return Arrays.copyOf(data, data.length);
    }

    public int data(int index, int fallback) {
        return index >= 0 && index < data.length ? data[index] : fallback;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(type);
        buffer.writeByte(difficulty);
        buffer.writeVarInt(data.length);
        for (int value : data) buffer.writeVarInt(value);
    }

    public static HackingDevicePuzzle decode(FriendlyByteBuf buffer) {
        Type type = buffer.readEnum(Type.class);
        int difficulty = buffer.readUnsignedByte();
        int length = Math.max(0, Math.min(MAX_DATA, buffer.readVarInt()));
        int[] data = new int[length];
        for (int index = 0; index < length; index++) {
            data[index] = buffer.readVarInt();
        }
        return new HackingDevicePuzzle(type, difficulty, data, 0);
    }

    private static int[] sanitize(int[] values) {
        if (values == null || values.length == 0) return new int[0];
        return Arrays.copyOf(values, Math.min(values.length, MAX_DATA));
    }
}
