package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079RoleSelection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Network bridge for the admin-only playable SCP selector screen. */
public final class ScpRoleSelectorNetwork {
    private static boolean registered;

    private ScpRoleSelectorNetwork() {
    }

    public enum Role {
        HUMAN,
        SCP_079
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(OpenScreen.class,
                OpenScreen::encode, OpenScreen::decode, OpenScreen::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(SelectRoleRequest.class,
                SelectRoleRequest::encode, SelectRoleRequest::decode,
                SelectRoleRequest::handle);
    }

    public static boolean openSelector(ServerPlayer player) {
        if (!Scp079RoleSelection.canOpenSelector(player)) return false;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenScreen(Scp079PlayableManager.isController(player)));
        return true;
    }

    public static void requestRole(Role role) {
        if (role == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new SelectRoleRequest(role));
    }

    public record OpenScreen(boolean scp079Active) {
        private static void encode(OpenScreen message, FriendlyByteBuf buffer) {
            buffer.writeBoolean(message.scp079Active);
        }

        private static OpenScreen decode(FriendlyByteBuf buffer) {
            return new OpenScreen(buffer.readBoolean());
        }

        private static void handle(OpenScreen message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.client.scp079.ScpRoleSelectorScreen
                            .open(message.scp079Active)));
            context.setPacketHandled(true);
        }
    }

    public record SelectRoleRequest(Role role) {
        private static void encode(SelectRoleRequest message,
                FriendlyByteBuf buffer) {
            buffer.writeEnum(message.role);
        }

        private static SelectRoleRequest decode(FriendlyByteBuf buffer) {
            return new SelectRoleRequest(buffer.readEnum(Role.class));
        }

        private static void handle(SelectRoleRequest message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                switch (message.role) {
                    case HUMAN -> Scp079RoleSelection.release(player);
                    case SCP_079 -> Scp079RoleSelection.selectScp079(player);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
