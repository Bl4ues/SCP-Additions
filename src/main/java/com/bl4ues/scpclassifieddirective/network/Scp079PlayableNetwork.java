package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079SignalInterruptionManager;
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
        ScpClassifiedDirectiveMod.addNetworkMessage(ToggleSpeakerRequest.class,
                ToggleSpeakerRequest::encode, ToggleSpeakerRequest::decode,
                ToggleSpeakerRequest::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(SignalInterruption.class,
                SignalInterruption::encode, SignalInterruption::decode,
                SignalInterruption::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(TrackingState.class,
                TrackingState::encode, TrackingState::decode,
                TrackingState::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(DoorLockState.class,
                DoorLockState::encode, DoorLockState::decode,
                DoorLockState::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(DoorMapRequest.class,
                DoorMapRequest::encode, DoorMapRequest::decode,
                DoorMapRequest::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(DoorMapState.class,
                DoorMapState::encode, DoorMapState::decode,
                DoorMapState::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(MapDoorActionRequest.class,
                MapDoorActionRequest::encode, MapDoorActionRequest::decode,
                MapDoorActionRequest::handle);
    }

    public static void sendState(ServerPlayer player,
            ResourceKey<Level> dimension, BlockPos hostPos, int power,
            boolean auxiliaryOnline, boolean networkAvailable,
            FacilityCameraDefinition camera, boolean speakerAvailable,
            boolean speakerActive) {
        if (player == null || dimension == null || hostPos == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                State.active(dimension.location(), hostPos,
                        Math.max(0, Math.min(100, power)), auxiliaryOnline,
                        networkAvailable, camera, speakerAvailable,
                        speakerActive));
    }

    public static void sendInactive(ServerPlayer player) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player), State.inactive());
    }

    public static void sendSignalInterruption(ServerPlayer player, int kind,
            int durationTicks) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SignalInterruption(kind, Math.max(1, durationTicks)));
    }

    public static void sendTracking(ServerPlayer player, int totalLifeforms,
            int targets, int scpSubjects, List<TrackerEntry> entries,
            List<ObjectMarkerEntry> objects) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new TrackingState(totalLifeforms, targets, scpSubjects,
                        entries == null ? List.of() : List.copyOf(entries),
                        objects == null ? List.of() : List.copyOf(objects)));
    }

    public static void sendDoorLockState(ServerPlayer player, BlockPos doorPos,
            long untilGameTime) {
        if (player == null || doorPos == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new DoorLockState(doorPos.immutable(), untilGameTime));
    }

    public static void requestDoorMap(ResourceLocation dimension) {
        if (dimension != null) {
            ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                    new DoorMapRequest(dimension));
        }
    }

    public static void sendDoorMap(ServerPlayer player,
            List<com.bl4ues.scpclassifieddirective.facility
                    .Scp079FacilityAccessManager.MapDoor> doors) {
        if (player == null) return;
        List<DoorMapEntry> entries = new ArrayList<>();
        if (doors != null) {
            int count = Math.min(4096, doors.size());
            for (int index = 0; index < count; index++) {
                var door = doors.get(index);
                entries.add(new DoorMapEntry(door.pos(), door.facing(),
                        door.blast(), door.open(), door.requiredLevel(),
                        door.lockable(), door.controllable()));
            }
        }
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new DoorMapState(List.copyOf(entries)));
    }

    public static void requestMapDoorAction(
            Scp079PlayableManager.ManualAction action, BlockPos doorPos) {
        if (action == null || doorPos == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new MapDoorActionRequest(action, doorPos));
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

    public static void requestSpeakerToggle() {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new ToggleSpeakerRequest());
    }

    public record State(boolean active, ResourceLocation dimension,
            BlockPos hostPos, int power, boolean auxiliaryOnline,
            boolean networkAvailable, UUID cameraId, String cameraName,
            double cameraX, double cameraY, double cameraZ,
            float baseYaw, float basePitch, float yawLimit,
            float minPitch, float maxPitch, float maxZoom,
            boolean speakerAvailable, boolean speakerActive) {
        public boolean cameraMode() {
            return active && cameraId != null;
        }

        private static State active(ResourceLocation dimension,
                BlockPos hostPos, int power, boolean auxiliaryOnline,
                boolean networkAvailable, FacilityCameraDefinition camera,
                boolean speakerAvailable, boolean speakerActive) {
            if (camera == null) {
                return new State(true, dimension, hostPos, power,
                        auxiliaryOnline, networkAvailable, null, "",
                        0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0.0F,
                        -55.0F, 55.0F, 1.0F, false, false);
            }
            return new State(true, dimension, hostPos, power,
                    auxiliaryOnline, networkAvailable, camera.id(),
                    camera.name(), camera.eyePosition().x,
                    camera.eyePosition().y, camera.eyePosition().z,
                    camera.baseYaw(), camera.basePitch(), camera.yawLimit(),
                    camera.minPitch(), camera.maxPitch(), camera.maxZoom(),
                    speakerAvailable, speakerActive);
        }

        private static State inactive() {
            return new State(false, Level.OVERWORLD.location(), BlockPos.ZERO,
                    0, false, false, null, "", 0.0D, 0.0D, 0.0D,
                    0.0F, 0.0F, 0.0F, -55.0F, 55.0F, 1.0F,
                    false, false);
        }

        private static void encode(State message, FriendlyByteBuf buffer) {
            buffer.writeBoolean(message.active);
            buffer.writeResourceLocation(message.dimension);
            buffer.writeBlockPos(message.hostPos);
            buffer.writeVarInt(message.power);
            buffer.writeBoolean(message.auxiliaryOnline);
            buffer.writeBoolean(message.networkAvailable);
            buffer.writeBoolean(message.speakerAvailable);
            buffer.writeBoolean(message.speakerActive);
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
            boolean speakerAvailable = buffer.readBoolean();
            boolean speakerActive = buffer.readBoolean();
            if (!buffer.readBoolean()) {
                return new State(active, dimension, host, power, auxiliary,
                        network, null, "", 0.0D, 0.0D, 0.0D,
                        0.0F, 0.0F, 0.0F, -55.0F, 55.0F, 1.0F,
                        false, false);
            }
            return new State(active, dimension, host, power, auxiliary,
                    network, buffer.readUUID(), buffer.readUtf(64),
                    buffer.readDouble(), buffer.readDouble(),
                    buffer.readDouble(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), speakerAvailable, speakerActive);
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

    /** Modal signal-loss state used by cameras, EMP effects and host destruction. */
    public record SignalInterruption(int kind, int durationTicks) {
        private static void encode(SignalInterruption message,
                FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.kind);
            buffer.writeVarInt(message.durationTicks);
        }

        private static SignalInterruption decode(FriendlyByteBuf buffer) {
            return new SignalInterruption(buffer.readVarInt(),
                    Math.max(1, buffer.readVarInt()));
        }

        private static void handle(SignalInterruption message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraEffectsClient
                            .beginSignalInterruption(message.kind,
                                    message.durationTicks)));
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

    public record ObjectMarkerEntry(ResourceLocation dimension,
            UUID roomId, double x, double z, int scpNumber) {
        private static void write(FriendlyByteBuf buffer,
                ObjectMarkerEntry entry) {
            buffer.writeResourceLocation(entry.dimension);
            buffer.writeUUID(entry.roomId);
            buffer.writeDouble(entry.x);
            buffer.writeDouble(entry.z);
            buffer.writeVarInt(entry.scpNumber);
        }

        private static ObjectMarkerEntry read(FriendlyByteBuf buffer) {
            return new ObjectMarkerEntry(buffer.readResourceLocation(),
                    buffer.readUUID(), buffer.readDouble(),
                    buffer.readDouble(), buffer.readVarInt());
        }
    }

    public record TrackingState(int totalLifeforms, int targets,
            int scpSubjects, List<TrackerEntry> entries,
            List<ObjectMarkerEntry> objects) {
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
            int objectCount = Math.min(4096, message.objects.size());
            buffer.writeVarInt(objectCount);
            for (int index = 0; index < objectCount; index++) {
                ObjectMarkerEntry.write(buffer, message.objects.get(index));
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
            int objectCount = Math.max(0,
                    Math.min(4096, buffer.readVarInt()));
            List<ObjectMarkerEntry> objects = new ArrayList<>(objectCount);
            for (int index = 0; index < objectCount; index++) {
                objects.add(ObjectMarkerEntry.read(buffer));
            }
            return new TrackingState(total, targets, subjects, entries,
                    objects);
        }

        private static void handle(TrackingState message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.client.scp079.Scp079TrackingClientState
                            .update(message.totalLifeforms, message.targets,
                                    message.scpSubjects, message.entries,
                                    message.objects)));
            context.setPacketHandled(true);
        }
    }

    public record DoorLockState(BlockPos doorPos, long untilGameTime) {
        private static void encode(DoorLockState message,
                FriendlyByteBuf buffer) {
            buffer.writeBlockPos(message.doorPos);
            buffer.writeLong(message.untilGameTime);
        }

        private static DoorLockState decode(FriendlyByteBuf buffer) {
            return new DoorLockState(buffer.readBlockPos(), buffer.readLong());
        }

        private static void handle(DoorLockState message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.client.scp079.Scp079DoorLockClientState
                            .mark(message.doorPos, message.untilGameTime)));
            context.setPacketHandled(true);
        }
    }

    public record DoorMapEntry(BlockPos pos,
            net.minecraft.core.Direction facing, boolean blast, boolean open,
            int requiredLevel, boolean lockable, boolean controllable) {
        private static void write(FriendlyByteBuf buffer, DoorMapEntry entry) {
            buffer.writeBlockPos(entry.pos);
            buffer.writeEnum(entry.facing);
            buffer.writeBoolean(entry.blast);
            buffer.writeBoolean(entry.open);
            buffer.writeVarInt(Math.max(0, Math.min(6,
                    entry.requiredLevel)));
            buffer.writeBoolean(entry.lockable);
            buffer.writeBoolean(entry.controllable);
        }

        private static DoorMapEntry read(FriendlyByteBuf buffer) {
            return new DoorMapEntry(buffer.readBlockPos(),
                    buffer.readEnum(net.minecraft.core.Direction.class),
                    buffer.readBoolean(), buffer.readBoolean(),
                    Math.max(0, Math.min(6, buffer.readVarInt())),
                    buffer.readBoolean(), buffer.readBoolean());
        }
    }

    public record DoorMapRequest(ResourceLocation dimension) {
        private static void encode(DoorMapRequest message,
                FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(message.dimension);
        }

        private static DoorMapRequest decode(FriendlyByteBuf buffer) {
            return new DoorMapRequest(buffer.readResourceLocation());
        }

        private static void handle(DoorMapRequest message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null
                        || !Scp079PlayableManager.isController(player)) return;
                sendDoorMap(player,
                        com.bl4ues.scpclassifieddirective.facility
                                .Scp079FacilityAccessManager.mapDoors(
                                        player.getServer(),
                                        message.dimension));
            });
            context.setPacketHandled(true);
        }
    }

    public record DoorMapState(List<DoorMapEntry> entries) {
        public DoorMapState {
            entries = entries == null ? List.of() : List.copyOf(entries);
        }

        private static void encode(DoorMapState message,
                FriendlyByteBuf buffer) {
            int count = Math.min(4096, message.entries.size());
            buffer.writeVarInt(count);
            for (int index = 0; index < count; index++) {
                DoorMapEntry.write(buffer, message.entries.get(index));
            }
        }

        private static DoorMapState decode(FriendlyByteBuf buffer) {
            int count = Math.max(0, Math.min(4096, buffer.readVarInt()));
            List<DoorMapEntry> entries = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                entries.add(DoorMapEntry.read(buffer));
            }
            return new DoorMapState(entries);
        }

        private static void handle(DoorMapState message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.client.scp079
                            .Scp079DoorMapClientState.update(message.entries)));
            context.setPacketHandled(true);
        }
    }

    public record MapDoorActionRequest(
            Scp079PlayableManager.ManualAction action, BlockPos doorPos) {
        private static void encode(MapDoorActionRequest message,
                FriendlyByteBuf buffer) {
            buffer.writeEnum(message.action);
            buffer.writeBlockPos(message.doorPos);
        }

        private static MapDoorActionRequest decode(FriendlyByteBuf buffer) {
            return new MapDoorActionRequest(buffer.readEnum(
                    Scp079PlayableManager.ManualAction.class),
                    buffer.readBlockPos());
        }

        private static void handle(MapDoorActionRequest message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null
                        && Scp079PlayableManager.isController(player)
                        && !Scp079SignalInterruptionManager
                                .controlsBlocked(player)) {
                    Scp079PlayableManager.performMapDoorAction(player,
                            message.action, message.doorPos);
                }
            });
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
                if (player != null && Scp079PlayableManager.isController(player)
                        && !Scp079SignalInterruptionManager.controlsBlocked(player)) {
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
                if (player != null
                        && !Scp079SignalInterruptionManager.controlsBlocked(player)) {
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
                if (player != null && Scp079PlayableManager.isController(player)
                        && !Scp079SignalInterruptionManager.controlsBlocked(player)) {
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
                if (player != null
                        && !Scp079SignalInterruptionManager.controlsBlocked(player)) {
                    Scp079PlayableManager.performAction(player,
                            message.action, message.aimedPos);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record ToggleSpeakerRequest() {
        private static void encode(ToggleSpeakerRequest message,
                FriendlyByteBuf buffer) { }
        private static ToggleSpeakerRequest decode(FriendlyByteBuf buffer) {
            return new ToggleSpeakerRequest();
        }
        private static void handle(ToggleSpeakerRequest message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null
                        && !Scp079SignalInterruptionManager.controlsBlocked(player)) {
                    Scp079PlayableManager.toggleSpeaker(player);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
