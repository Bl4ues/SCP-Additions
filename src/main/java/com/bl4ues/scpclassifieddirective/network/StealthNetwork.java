package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.stealth.AdvancedCrouchController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Synchronizes the server-owned stealth module switch to clients. */
public final class StealthNetwork {
    private static final Map<UUID, Boolean> LAST_SENT = new HashMap<>();
    private static boolean registered;

    private StealthNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(ModuleState.class,
                ModuleState::encode, ModuleState::decode, ModuleState::handle);
    }

    public static void syncModuleState(ServerPlayer player, boolean enabled) {
        if (player == null) return;
        Boolean previous = LAST_SENT.get(player.getUUID());
        // The periodic refresh also covers reconnects where the UUID survived in
        // this small process-local cache and Configuration Center reloads.
        if (previous != null && previous == enabled && player.tickCount % 100 != 1) {
            return;
        }
        LAST_SENT.put(player.getUUID(), enabled);
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ModuleState(enabled));
    }

    public record ModuleState(boolean enabled) {
        private static void encode(ModuleState message, FriendlyByteBuf buffer) {
            buffer.writeBoolean(message.enabled);
        }

        private static ModuleState decode(FriendlyByteBuf buffer) {
            return new ModuleState(buffer.readBoolean());
        }

        private static void handle(ModuleState message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (!context.getDirection().getReceptionSide().isServer()) {
                    AdvancedCrouchController.setClientModuleEnabled(message.enabled);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
