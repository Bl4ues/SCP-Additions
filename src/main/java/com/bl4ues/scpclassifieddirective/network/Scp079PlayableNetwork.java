package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilityCameraDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Role state and responsive controls for playable SCP-079. */
public final class Scp079PlayableNetwork {
    private static boolean registered;

    private Scp079PlayableNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(State.class,
                State::encode, State::decode, State::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(ReleaseRequest.class,
                ReleaseRequest::encode, ReleaseRequest::decode,
                ReleaseRequest::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(SwitchRoomRequest.class,
                SwitchRoomRequest::encode, SwitchRoomRequest::decode,
                SwitchRoomRequest::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(ReturnLocalRequest.class,
                ReturnLocalRequest::encode, ReturnLocalRequest::decode,
                ReturnLocalRequest::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(ActionRequest.class,
                ActionRequest::encode, ActionRequest::decode,
                ActionRequest::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(TrackingState.class,
                TrackingState::encode, TrackingState::decode,
                TrackingState::handle);
    }

    public static void sendState(ServerPlayer player,
            ResourceKey<Level> dimension, BlockPos hostPos, int power,
            boolean auxiliaryOnline, boolean networkAvailable,
            FacilityCameraDefinition camera) {
        if (player == null || dimension == null || hostPos == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                State.active(dimension.location(), hostPos,
                        Math.max(0, Math.min(100, power)), auxiliaryOnline,
                        networkAvailable, camera));
    }

    public static void sendInactive(ServerPlayer player) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player), State.inactive());
    }

    public static void sendTracking(ServerPlayer player, int totalLifeforms,
            int targets, int scpSubjects, List<TrackerEntry> entries) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new TrackingState(totalLifeforms, targets, scpSubjects,
                        entries == null ? List.of() : List.copyOf(entries)));
    }

    public static void requestRelease() {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new ReleaseRequest());
    }

    public static void requestRoom(UUID roomId) {
        if (roomId != null) {
            ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                    new SwitchRoomRequest(roomId));
        }
    }

    public static void requestLocal() {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new ReturnLocalRequest());
    }

    public static void requestAction(Scp079PlayableManager.ManualAction action,
            BlockPos aimedPos) {
        if (action == null || aimedPos == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new ActionRequest(action, aimedPos));
    }

    public record State(boolean active, ResourceLocation dimension,
            BlockPos hostPos, int power, boolean auxiliaryOnline,
            boolean networkAvailable, UUID cameraId, String cameraName,
            double cameraX, double cameraY, double cameraZ,
            float baseYaw, float basePitch, float yawLimit,
            float minPitch, float maxPitch, float maxZoom) {
        public boolean cameraMode() {
            return active && cameraId != null;
        }

        private static State active(ResourceLocation dimension,
                BlockPos hostPos, int power, boolean auxiliaryOnline,
                boolean networkAvailable, FacilityCameraDefinition camera) {
            if (camera == null) {
                return new State(true, dimension, hostPos, power,
                        auxiliaryOnline, networkAvailable, null, "",
                        0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0.0F,
                        -55.0F, 55.0F, 1.0F);
            }
            return new State(true, dimension, hostPos, power,
                    auxiliaryOnline, networkAvailable, camera.id(),
                    camera.name(), camera.eyePosition().x,
                    camera.eyePosition().y, camera.eyePosition().z,
                    camera.baseYaw(), camera.basePitch(), camera.yawLimit(),
                    camera.minPitch(), camera.maxPitch(), camera.maxZoom());
        }

        private static State inactive() {
            return new State(false, Level.OVERWORLD.location(), BlockPos.ZERO,
                    0, false, false, null, "", 0.0D, 0.0D, 0.0D,
                    0.0F, 0.0F, 0.0F, -55.0F, 55.0F, 1.0F);
        }

        private static void encode(State message, FriendlyByteBuf buffer) {
            buffer.writeBoolean(message.active);
            buffer.writeResourceLocation(message.dimension);
            buffer.writeBlockPos(message.hostPos);
            buffer.writeVarInt(message.power);
            buffer.writeBoolean(message.auxiliaryOnline);
            buffer.writeBoolean(message.networkAvailable);
            buffer.writeBoolean(message.cameraId != null);
            if (message.cameraId != null) {
                buffer.writeUUID(message.cameraId);
                buffer.writeUtf(message.cameraName, 64);
                buffer.writeDouble(message.cameraX);
                buffer.writeDouble(message.cameraY);
                buffer.writeDouble(message.cameraZ);
                buffer.writeFloat(message.baseYaw);
                buffer.writeFloat(message.basePitch);
                buffer.writeFloat(message.yawLimit);
                buffer.writeFloat(message.minPitch);
                buffer.writeFloat(message.maxPitch);
                buffer.writeFloat(message.maxZoom);
            }
        }

        private static State decode(FriendlyByteBuf buffer) {
            boolean active = buffer.readBoolean();
            ResourceLocation dimension = buffer.readResourceLocation();
            BlockPos host = buffer.readBlockPos();
            int power = buffer.readVarInt();
            boolean auxiliary = buffer.readBoolean();
            boolean network = buffer.readBoolean();
            if (!buffer.readBoolean()) {
                return new State(active, dimension, host, power, auxiliary,
                        network, null, "", 0.0D, 0.0D, 0.0D,
                        0.0F, 0.0F, 0.0F, -55.0F, 55.0F, 1.0F);
            }
            return new State(active, dimension, host, power, auxiliary,
                    network, buffer.readUUID(), buffer.readUtf(64),
                    buffer.readDouble(), buffer.readDouble(),
                    buffer.readDouble(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat());
        }

        private static void handle(State message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient
                            .receive(message)));
            context.setPacketHandled(true);
        }
    }

    public record TrackerEntry(ResourceLocation dimension, UUID roomId,
            double x, double z, float yaw, int scpNumber) {
        private static void write(FriendlyByteBuf buffer, TrackerEntry entry) {
            buffer.writeResourceLocation(entry.dimension);
            buffer.writeUUID(entry.roomId);
            buffer.writeDouble(entry.x);
            buffer.writeDouble(entry.z);
            buffer.writeFloat(entry.yaw);
            buffer.writeVarInt(entry.scpNumber);
        }

        private static TrackerEntry read(FriendlyByteBuf buffer) {
            return new TrackerEntry(buffer.readResourceLocation(),
                    buffer.readUUID(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readFloat(), buffer.readVarInt());
        }
    }

    public record TrackingState(int totalLifeforms, int targets,
            int scpSubjects, List<TrackerEntry> entries) {
        private static void encode(TrackingState message,
                FriendlyByteBuf buffer) {
            buffer.writeVarInt(Math.max(0, message.totalLifeforms));
            buffer.writeVarInt(Math.max(0, message.targets));
            buffer.writeVarInt(Math.max(0, message.scpSubjects));
            int count = Math.min(256, message.entries.size());
            buffer.writeVarInt(count);
            for (int index = 0; index < count; index++) {
                TrackerEntry.write(buffer, message.entries.get(index));
            }
        }

        private static TrackingState decode(FriendlyByteBuf buffer) {
            int total = buffer.readVarInt();
            int targets = buffer.readVarInt();
            int subjects = buffer.readVarInt();
            int count = Math.max(0, Math.min(256, buffer.readVarInt()));
            List<TrackerEntry> entries = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                entries.add(TrackerEntry.read(buffer));
            }
            return new TrackingState(total, targets, subjects, entries);
        }

        private static void handle(TrackingState message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.client.scp079.Scp079TrackingClientState
                            .update(message.totalLifeforms, message.targets,
                                    message.scpSubjects, message.entries)));
            context.setPacketHandled(true);
        }
    }

    public record ReleaseRequest() {
        private static void encode(ReleaseRequest message,
                FriendlyByteBuf buffer) { }
        private static ReleaseRequest decode(FriendlyByteBuf buffer) {
            return new ReleaseRequest();
        }
        private static void handle(ReleaseRequest message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null && Scp079PlayableManager.isController(player)) {
                    Scp079PlayableManager.release(player);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record SwitchRoomRequest(UUID roomId) {
        private static void encode(SwitchRoomRequest message,
                FriendlyByteBuf buffer) { buffer.writeUUID(message.roomId); }
        private static SwitchRoomRequest decode(FriendlyByteBuf buffer) {
            return new SwitchRoomRequest(buffer.readUUID());
        }
        private static void handle(SwitchRoomRequest message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    Scp079PlayableManager.switchToRoom(player, message.roomId);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record ReturnLocalRequest() {
        private static void encode(ReturnLocalRequest message,
                FriendlyByteBuf buffer) { }
        private static ReturnLocalRequest decode(FriendlyByteBuf buffer) {
            return new ReturnLocalRequest();
        }
        private static void handle(ReturnLocalRequest message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null && Scp079PlayableManager.isController(player)) {
                    Scp079PlayableManager.returnToHost(player);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record ActionRequest(Scp079PlayableManager.ManualAction action,
            BlockPos aimedPos) {
        private static void encode(ActionRequest message,
                FriendlyByteBuf buffer) {
            buffer.writeEnum(message.action);
            buffer.writeBlockPos(message.aimedPos);
        }
        private static ActionRequest decode(FriendlyByteBuf buffer) {
            return new ActionRequest(buffer.readEnum(
                    Scp079PlayableManager.ManualAction.class),
                    buffer.readBlockPos());
        }
        private static void handle(ActionRequest message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    Scp079PlayableManager.performAction(player,
                            message.action, message.aimedPos);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
