package net.mcreator.scpadditions.config.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.mcreator.scpadditions.client.CodexAssetClient;

import java.util.List;
import java.util.function.Supplier;

public final class ConfigCenterNetwork {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private ConfigCenterNetwork() {
    }

    public static int register(SimpleChannel channel, int id) {
        channel.registerMessage(id++, OpenRequest.class, OpenRequest::encode, OpenRequest::decode, OpenRequest::handle);
        channel.registerMessage(id++, Snapshot.class, Snapshot::encode, Snapshot::decode, Snapshot::handle);
        channel.registerMessage(id++, SaveRequest.class, SaveRequest::encode, SaveRequest::decode, SaveRequest::handle);
        channel.registerMessage(id++, SaveResult.class, SaveResult::encode, SaveResult::decode, SaveResult::handle);
        channel.registerMessage(id++, AssetUploadRequest.class, AssetUploadRequest::encode, AssetUploadRequest::decode, AssetUploadRequest::handle);
        channel.registerMessage(id++, AssetUploadResult.class, AssetUploadResult::encode, AssetUploadResult::decode, AssetUploadResult::handle);
        channel.registerMessage(id++, AssetRequest.class, AssetRequest::encode, AssetRequest::decode, AssetRequest::handle);
        channel.registerMessage(id++, AssetData.class, AssetData::encode, AssetData::decode, AssetData::handle);
        channel.registerMessage(id++, GiveCodexItemRequest.class, GiveCodexItemRequest::encode, GiveCodexItemRequest::decode, GiveCodexItemRequest::handle);
        return id;
    }

    public static void openFor(ServerPlayer player, SimpleChannel channel) {
        if (!ConfigCenterService.canEdit(player)) return;
        try {
            channel.send(PacketDistributor.PLAYER.with(() -> player),
                    new Snapshot(GSON.toJson(ConfigCenterService.snapshot())));
        } catch (Exception exception) {
            channel.send(PacketDistributor.PLAYER.with(() -> player),
                    new SaveResult(false, "Could not read configuration files: "
                            + readable(exception), "", List.of()));
        }
    }

    public static final class OpenRequest {
        public static void encode(OpenRequest ignored, FriendlyByteBuf buffer) { }
        public static OpenRequest decode(FriendlyByteBuf buffer) { return new OpenRequest(); }
        public static void handle(OpenRequest ignored, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
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
        public Snapshot { payload = payload == null ? "{}" : payload; }
        public static void encode(Snapshot message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.payload, ConfigCenterService.MAX_PAYLOAD_LENGTH);
        }
        public static Snapshot decode(FriendlyByteBuf buffer) {
            return new Snapshot(buffer.readUtf(ConfigCenterService.MAX_PAYLOAD_LENGTH));
        }
        public static void handle(Snapshot message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ConfigCenterClient.openSnapshot(message.payload)));
            context.setPacketHandled(true);
        }
    }

    public record SaveRequest(String changes) {
        public SaveRequest { changes = changes == null ? "{}" : changes; }
        public static void encode(SaveRequest message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.changes, ConfigCenterService.MAX_PAYLOAD_LENGTH);
        }
        public static SaveRequest decode(FriendlyByteBuf buffer) {
            return new SaveRequest(buffer.readUtf(ConfigCenterService.MAX_PAYLOAD_LENGTH));
        }
        public static void handle(SaveRequest message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                ConfigCenterService.SaveResult result = ConfigCenterService.saveBatch(player, message.changes);
                if (result.success()) {
                    com.bl4ues.scpinventory.network.ModNetwork.syncModuleState(
                            player.server.getPlayerList().getPlayers());
                }
                String snapshot = result.success() ? GSON.toJson(result.snapshot()) : "";
                com.bl4ues.scpinventory.network.ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new SaveResult(result.success(), result.message(), snapshot, result.warnings()));
            });
            context.setPacketHandled(true);
        }
    }

    public record SaveResult(boolean success, String message, String snapshot,
                             List<String> warnings) {
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
        public static void handle(SaveResult result, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ConfigCenterClient.onSaveResult(result)));
            context.setPacketHandled(true);
        }
    }

    public record AssetUploadRequest(String requestId, String kind,
                                     String fileName, byte[] data) {
        public AssetUploadRequest {
            requestId = requestId == null ? "" : requestId;
            kind = kind == null ? "" : kind;
            fileName = fileName == null ? "asset" : fileName;
            data = data == null ? new byte[0] : data;
        }
        public static void encode(AssetUploadRequest message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.requestId, 64);
            buffer.writeUtf(message.kind, 16);
            buffer.writeUtf(message.fileName, 256);
            buffer.writeByteArray(message.data);
        }
        public static AssetUploadRequest decode(FriendlyByteBuf buffer) {
            return new AssetUploadRequest(buffer.readUtf(64), buffer.readUtf(16),
                    buffer.readUtf(256),
                    buffer.readByteArray(CodexAssetStorage.MAX_TRANSFER_BYTES));
        }
        public static void handle(AssetUploadRequest message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                CodexAssetStorage.UploadResult result = CodexAssetStorage.save(
                        player, message.kind, message.fileName, message.data);
                com.bl4ues.scpinventory.network.ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new AssetUploadResult(message.requestId, result.success(),
                                message.kind, result.key(), result.message()));
            });
            context.setPacketHandled(true);
        }
    }

    public record AssetUploadResult(String requestId, boolean success, String kind,
                                    String key, String message) {
        public AssetUploadResult {
            requestId = requestId == null ? "" : requestId;
            kind = kind == null ? "" : kind;
            key = key == null ? "" : key;
            message = message == null ? "" : message;
        }
        public static void encode(AssetUploadResult message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.requestId, 64);
            buffer.writeBoolean(message.success);
            buffer.writeUtf(message.kind, 16);
            buffer.writeUtf(message.key, 160);
            buffer.writeUtf(message.message, 2048);
        }
        public static AssetUploadResult decode(FriendlyByteBuf buffer) {
            return new AssetUploadResult(buffer.readUtf(64), buffer.readBoolean(),
                    buffer.readUtf(16), buffer.readUtf(160), buffer.readUtf(2048));
        }
        public static void handle(AssetUploadResult message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> CodexAssetClient.onUploadResult(message)));
            context.setPacketHandled(true);
        }
    }

    public record AssetRequest(String kind, String key) {
        public AssetRequest {
            kind = kind == null ? "" : kind;
            key = key == null ? "" : key;
        }
        public static void encode(AssetRequest message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.kind, 16);
            buffer.writeUtf(message.key, 160);
        }
        public static AssetRequest decode(FriendlyByteBuf buffer) {
            return new AssetRequest(buffer.readUtf(16), buffer.readUtf(160));
        }
        public static void handle(AssetRequest message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                byte[] bytes;
                try { bytes = CodexAssetStorage.read(player, message.key); }
                catch (Exception ignored) { bytes = new byte[0]; }
                com.bl4ues.scpinventory.network.ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new AssetData(message.kind, message.key, bytes));
            });
            context.setPacketHandled(true);
        }
    }

    public record AssetData(String kind, String key, byte[] data) {
        public AssetData {
            kind = kind == null ? "" : kind;
            key = key == null ? "" : key;
            data = data == null ? new byte[0] : data;
        }
        public static void encode(AssetData message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.kind, 16);
            buffer.writeUtf(message.key, 160);
            buffer.writeByteArray(message.data);
        }
        public static AssetData decode(FriendlyByteBuf buffer) {
            return new AssetData(buffer.readUtf(16), buffer.readUtf(160),
                    buffer.readByteArray(CodexAssetStorage.MAX_TRANSFER_BYTES));
        }
        public static void handle(AssetData message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> CodexAssetClient.onAssetData(message)));
            context.setPacketHandled(true);
        }
    }

    public record GiveCodexItemRequest(String itemId, String codexId,
                                       String displayName) {
        public GiveCodexItemRequest {
            itemId = itemId == null ? "" : itemId;
            codexId = codexId == null ? "" : codexId;
            displayName = displayName == null ? "" : displayName;
        }
        public static void encode(GiveCodexItemRequest message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.itemId, 256);
            buffer.writeUtf(message.codexId, 128);
            buffer.writeUtf(message.displayName, 256);
        }
        public static GiveCodexItemRequest decode(FriendlyByteBuf buffer) {
            return new GiveCodexItemRequest(buffer.readUtf(256), buffer.readUtf(128),
                    buffer.readUtf(256));
        }
        public static void handle(GiveCodexItemRequest message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) CodexAssetStorage.giveDocument(player,
                        message.itemId, message.codexId, message.displayName);
            });
            context.setPacketHandled(true);
        }
    }

    private static String readable(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName() : message;
    }
}
