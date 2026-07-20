package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import com.bl4ues.scpinventory.context.ContextInteractionRegistry;
import net.minecraft.network.FriendlyByteBuf;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Complete host-authoritative SCP Inventory configuration snapshot. */
public final class ServerConfigSyncPacket {
    private static final int MAX_LIST_ENTRIES = 16384;
    private static final int MAX_STRING_LENGTH = 1_000_000;

    private final List<String> itemRules;
    private final List<String> itemEffects;
    private final List<String> codexDocuments;
    private final List<String> hiddenStatusEffects;
    private final List<String> scp173Targets;
    private final String contextJson;

    public ServerConfigSyncPacket(
            List<String> itemRules,
            List<String> itemEffects,
            List<String> codexDocuments,
            List<String> hiddenStatusEffects,
            List<String> scp173Targets,
            String contextJson) {
        this.itemRules = List.copyOf(itemRules);
        this.itemEffects = List.copyOf(itemEffects);
        this.codexDocuments = List.copyOf(codexDocuments);
        this.hiddenStatusEffects = List.copyOf(hiddenStatusEffects);
        this.scp173Targets = List.copyOf(scp173Targets);
        this.contextJson = contextJson == null
                ? "{\"interactions\":[]}" : contextJson;
    }

    public static void encode(ServerConfigSyncPacket message,
            FriendlyByteBuf buffer) {
        writeStrings(buffer, message.itemRules);
        writeStrings(buffer, message.itemEffects);
        writeStrings(buffer, message.codexDocuments);
        writeStrings(buffer, message.hiddenStatusEffects);
        writeStrings(buffer, message.scp173Targets);
        buffer.writeUtf(message.contextJson, MAX_STRING_LENGTH);
    }

    public static ServerConfigSyncPacket decode(FriendlyByteBuf buffer) {
        return new ServerConfigSyncPacket(
                readStrings(buffer),
                readStrings(buffer),
                readStrings(buffer),
                readStrings(buffer),
                readStrings(buffer),
                buffer.readUtf(MAX_STRING_LENGTH));
    }

    public static void handle(ServerConfigSyncPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> {
            ScpInventoryConfig.applyServerSnapshot(
                    message.itemRules,
                    message.itemEffects,
                    message.codexDocuments,
                    message.hiddenStatusEffects,
                    message.scp173Targets);
            ContextInteractionRegistry.applyServerSnapshot(message.contextJson);
        });
        contextSupplier.get().setPacketHandled(true);
    }

    private static void writeStrings(FriendlyByteBuf buffer,
            List<String> values) {
        buffer.writeVarInt(values.size());
        for (String value : values) {
            buffer.writeUtf(value == null ? "" : value, MAX_STRING_LENGTH);
        }
    }

    private static List<String> readStrings(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_LIST_ENTRIES) {
            throw new IllegalArgumentException(
                    "Invalid SCP Inventory config list size: " + size);
        }
        List<String> values = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            values.add(buffer.readUtf(MAX_STRING_LENGTH));
        }
        return values;
    }
}
