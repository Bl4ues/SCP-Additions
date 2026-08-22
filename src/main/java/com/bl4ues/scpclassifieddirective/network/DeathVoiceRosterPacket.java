package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Synchronizes the dead-player voice roster used by the personnel-feed UI. */
public record DeathVoiceRosterPacket(List<Participant> participants) {
    private static final int MAX_PARTICIPANTS = 64;

    public DeathVoiceRosterPacket {
        if (participants == null || participants.isEmpty()) {
            participants = List.of();
        } else {
            participants = List.copyOf(participants.subList(0,
                    Math.min(MAX_PARTICIPANTS, participants.size())));
        }
    }

    public static void encode(DeathVoiceRosterPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.participants.size());
        for (Participant participant : message.participants) {
            buffer.writeUUID(participant.id());
            buffer.writeUtf(participant.name(), 64);
            buffer.writeUtf(participant.skinOverride(), 128);
        }
    }

    public static DeathVoiceRosterPacket decode(FriendlyByteBuf buffer) {
        int count = Math.min(MAX_PARTICIPANTS, Math.max(0, buffer.readVarInt()));
        List<Participant> participants = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            participants.add(new Participant(buffer.readUUID(),
                    buffer.readUtf(64), buffer.readUtf(128)));
        }
        return new DeathVoiceRosterPacket(participants);
    }

    public static void handle(DeathVoiceRosterPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> dispatchClient(message));
        context.setPacketHandled(true);
    }

    private static void dispatchClient(DeathVoiceRosterPacket message) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        try {
            Class<?> target = Class.forName(
                    "com.bl4ues.scpclassifieddirective.client.DeathVoiceRosterClient");
            Method method = target.getMethod("receive", List.class);
            method.invoke(null, message.participants);
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Could not synchronize the dead voice roster", exception);
        }
    }

    public record Participant(UUID id, String name, String skinOverride) {
        public Participant {
            id = id == null ? new UUID(0L, 0L) : id;
            name = name == null ? "" : name;
            skinOverride = skinOverride == null ? "" : skinOverride;
        }
    }
}
