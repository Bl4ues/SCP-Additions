package net.mcreator.scpadditions.config.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.function.Supplier;

/** Network messages for the server-authoritative configuration center. */
public final class ConfigCenterNetwork {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private ConfigCenterNetwork() {
    }

    public static int register(SimpleChannel channel, int id) {
        channel.registerMessage(id++, OpenRequest.class, OpenRequest::encode, OpenRequest::decode, OpenRequest::handle);
        channel.registerMessage(id++, Snapshot.class, Snapshot::encode, Snapshot::decode, Snapshot::handle);
        channel.registerMessage(id++, SaveRequest.class, SaveRequest::encode, SaveRequest::decode, SaveRequest::handle);
        channel.registerMessage(id++, SaveResult.class, SaveResult::encode, SaveResult::decode, SaveResult::handle);
        return id;
    }

    public static void openFor(ServerPlayer player, SimpleChannel channel) {
        if (!ConfigCenterService.canEdit(player)) return;
        try {
            channel.send(PacketDistributor.PLAYER.with(() -> player),
                    new Snapshot(GSON.toJson(ConfigCenterService.snapshot())));
        } catch (Exception exception) {
            channel.send(PacketDistributor.PLAYER.with(() -> player),
                    new SaveResult(false, "Could not read configuration files: " + readable(exception), "", List.of()));
        }
    }

    public static final class OpenRequest {
        public static void encode(OpenRequest ignored, FriendlyByteBuf buffer) {
        }

        public static OpenRequest decode(FriendlyByteBuf buffer) {
            return new OpenRequest();
        }

        public static void handle(OpenRequest ignored, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                if (!ConfigCenterService.canEdit(player)) {
                    com.bl4ues.scpinventory.network.ModNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new SaveResult(false, "Operator permission level 2 is required to edit server configuration.", "", List.of()));
                    return;
                }
                openFor(player, com.bl4ues.scpinventory.network.ModNetwork.CHANNEL);
            });
            context.setPacketHandled(true);
        }
    }

    public record Snapshot(String payload) {
        public Snapshot {
            payload = payload == null ? "{}" : payload;
        }

        public static void encode(Snapshot message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.payload, ConfigCenterService.MAX_PAYLOAD_LENGTH);
        }

        public static Snapshot decode(FriendlyByteBuf buffer) {
            return new Snapshot(buffer.readUtf(ConfigCenterService.MAX_PAYLOAD_LENGTH));
        }

        public static void handle(Snapshot message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ConfigCenterClient.openSnapshot(message.payload)));
            context.setPacketHandled(true);
        }
    }

    public record SaveRequest(String changes) {
        public SaveRequest {
            changes = changes == null ? "{}" : changes;
        }

        public static void encode(SaveRequest message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.changes, ConfigCenterService.MAX_PAYLOAD_LENGTH);
        }

        public static SaveRequest decode(FriendlyByteBuf buffer) {
            return new SaveRequest(buffer.readUtf(ConfigCenterService.MAX_PAYLOAD_LENGTH));
        }

        public static void handle(SaveRequest message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                ConfigCenterService.SaveResult result = ConfigCenterService.saveBatch(player, message.changes);
                String snapshot = result.success() ? GSON.toJson(result.snapshot()) : "";
                com.bl4ues.scpinventory.network.ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new SaveResult(result.success(), result.message(), snapshot, result.warnings()));
            });
            context.setPacketHandled(true);
        }
    }

    public record SaveResult(boolean success, String message, String snapshot, List<String> warnings) {
        public SaveResult {
            message = message == null ? "" : message;
            snapshot = snapshot == null ? "" : snapshot;
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }

        public static void encode(SaveResult result, FriendlyByteBuf buffer) {
            buffer.writeBoolean(result.success);
            buffer.writeUtf(result.message, 8192);
            buffer.writeUtf(result.snapshot, ConfigCenterService.MAX_PAYLOAD_LENGTH);
            buffer.writeVarInt(Math.min(result.warnings.size(), 128));
            for (int i = 0; i < Math.min(result.warnings.size(), 128); i++) {
                buffer.writeUtf(result.warnings.get(i), 2048);
            }
        }

        public static SaveResult decode(FriendlyByteBuf buffer) {
            boolean success = buffer.readBoolean();
            String message = buffer.readUtf(8192);
            String snapshot = buffer.readUtf(ConfigCenterService.MAX_PAYLOAD_LENGTH);
            int count = Math.min(buffer.readVarInt(), 128);
            java.util.ArrayList<String> warnings = new java.util.ArrayList<>(count);
            for (int i = 0; i < count; i++) warnings.add(buffer.readUtf(2048));
            return new SaveResult(success, message, snapshot, warnings);
        }

        public static void handle(SaveResult result, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ConfigCenterClient.onSaveResult(result)));
            context.setPacketHandled(true);
        }
    }

    private static String readable(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
