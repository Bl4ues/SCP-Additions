package com.bl4ues.scpclassifieddirective.hacking;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Arrays;

/** Immutable four-byte XOR-checksum repair challenge. */
public record HackingDevicePuzzle(int[] bytes, int missingIndex,
        int checksum, int[] candidates, int correctIndex) {
    public HackingDevicePuzzle {
        bytes = sanitize(bytes, 4);
        candidates = sanitize(candidates, 4);
        missingIndex = Math.max(0, Math.min(3, missingIndex));
        checksum &= 0xFF;
        correctIndex = Math.max(0, Math.min(3, correctIndex));
    }

    @Override
    public int[] bytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public int[] candidates() {
        return Arrays.copyOf(candidates, candidates.length);
    }

    public int candidate(int index) {
        return candidates[Math.max(0, Math.min(candidates.length - 1, index))];
    }

    public int computedChecksum(int candidateIndex) {
        int value = 0;
        for (int index = 0; index < bytes.length; index++) {
            value ^= index == missingIndex ? candidate(candidateIndex) : bytes[index];
        }
        return value & 0xFF;
    }

    public void encode(FriendlyByteBuf buffer) {
        for (int value : bytes) buffer.writeByte(value & 0xFF);
        buffer.writeByte(missingIndex);
        buffer.writeByte(checksum);
        for (int value : candidates) buffer.writeByte(value & 0xFF);
        buffer.writeByte(correctIndex);
    }

    public static HackingDevicePuzzle decode(FriendlyByteBuf buffer) {
        int[] bytes = new int[4];
        int[] candidates = new int[4];
        for (int index = 0; index < 4; index++) bytes[index] = buffer.readUnsignedByte();
        int missing = buffer.readUnsignedByte();
        int checksum = buffer.readUnsignedByte();
        for (int index = 0; index < 4; index++) candidates[index] = buffer.readUnsignedByte();
        int correct = buffer.readUnsignedByte();
        return new HackingDevicePuzzle(bytes, missing, checksum, candidates, correct);
    }

    private static int[] sanitize(int[] values, int length) {
        int[] result = new int[length];
        if (values != null) {
            System.arraycopy(values, 0, result, 0, Math.min(length, values.length));
        }
        for (int index = 0; index < result.length; index++) result[index] &= 0xFF;
        return result;
    }
}
