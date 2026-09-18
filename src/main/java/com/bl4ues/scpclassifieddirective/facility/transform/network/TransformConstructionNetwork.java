package com.bl4ues.scpclassifieddirective.facility.transform.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.BlockStateCodec;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionSavedData;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformControlRuntime;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformDoorRuntime;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceAuthoringManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/** Dedicated sync/edit channel for transformed construction. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TransformConstructionNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                    "transform_construction"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static final Map<MinecraftServer, Long> LAST_REVISION =
            new WeakHashMap<>();
    private static boolean registered;

    private TransformConstructionNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, Snapshot.class,
                Snapshot::encode, Snapshot::decode, Snapshot::handle);
        CHANNEL.registerMessage(1, UpdateGroup.class,
                UpdateGroup::encode, UpdateGroup::decode, UpdateGroup::handle);
        CHANNEL.registerMessage(2, UpdateSurface.class,
                UpdateSurface::encode, UpdateSurface::decode,
                UpdateSurface::handle);
        CHANNEL.registerMessage(3, Delete.class,
                Delete::encode, Delete::decode, Delete::handle);
        CHANNEL.registerMessage(4, GroupCellState.class,
                GroupCellState::encode, GroupCellState::decode,
                GroupCellState::handle);
        CHANNEL.registerMessage(5, SurfaceSlotState.class,
                SurfaceSlotState::encode, SurfaceSlotState::decode,
                SurfaceSlotState::handle);
        CHANNEL.registerMessage(6, SetSurfaceFlipped.class,
                SetSurfaceFlipped::encode, SetSurfaceFlipped::decode,
                SetSurfaceFlipped::handle);
        CHANNEL.registerMessage(7, AuthorSurfacePoint.class,
                AuthorSurfacePoint::encode, AuthorSurfacePoint::decode,
                AuthorSurfacePoint::handle);
        CHANNEL.registerMessage(8, CancelSurfaceAuthoring.class,
                CancelSurfaceAuthoring::encode, CancelSurfaceAuthoring::decode,
                CancelSurfaceAuthoring::handle);
        CHANNEL.registerMessage(9, BlockedPlacement.class,
                BlockedPlacement::encode, BlockedPlacement::decode,
                BlockedPlacement::handle);
        CHANNEL.registerMessage(10, PlaceSurfaceBlock.class,
                PlaceSurfaceBlock::encode, PlaceSurfaceBlock::decode,
                PlaceSurfaceBlock::handle);
        CHANNEL.registerMessage(11, PlaceGroupBlock.class,
                PlaceGroupBlock::encode, PlaceGroupBlock::decode,
                PlaceGroupBlock::handle);
        CHANNEL.registerMessage(12, UseGroupCell.class,
                UseGroupCell::encode, UseGroupCell::decode,
                UseGroupCell::handle);
    }

    public static void updateGroup(UUID id, Vec3 origin, float rotationX,
            float rotationY, float rotationZ) {
        if (id == null || origin == null) return;
        CHANNEL.sendToServer(new UpdateGroup(id, origin, rotationX, rotationY,
                rotationZ));
    }

    public static void updateSurface(UUID id, Vec3 bottomStart, Vec3 bottomEnd,
            Vec3 topStart, Vec3 topEnd, Vec3 curveOffset) {
        updateSurface(id, bottomStart, bottomEnd, topStart, topEnd,
                curveOffset, Vec3.ZERO);
    }

    public static void updateSurface(UUID id, Vec3 bottomStart, Vec3 bottomEnd,
            Vec3 topStart, Vec3 topEnd, Vec3 curveOffset,
            Vec3 heightCurveOffset) {
        if (id == null || bottomStart == null || bottomEnd == null
                || topStart == null || topEnd == null || curveOffset == null
                || heightCurveOffset == null) return;
        CHANNEL.sendToServer(new UpdateSurface(id, bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, heightCurveOffset));
    }

    public static void delete(UUID id, boolean surface) {
        if (id != null) CHANNEL.sendToServer(new Delete(id, surface));
    }

    public static void setSurfaceFlipped(UUID id, boolean flipped) {
        if (id != null) CHANNEL.sendToServer(new SetSurfaceFlipped(id, flipped));
    }

    public static void authorSurfacePoint(Vec3 point) {
        if (point != null) CHANNEL.sendToServer(new AuthorSurfacePoint(point));
    }

    public static void cancelSurfaceAuthoring() {
        CHANNEL.sendToServer(new CancelSurfaceAuthoring());
    }

    public static void placeGroupBlock(UUID groupId,
            TransformGroup.GridPos sourceCell, TransformGroup.GridPos targetCell,
            net.minecraft.core.Direction outwardLocal, Vec3 hit) {
        if (groupId == null || sourceCell == null || targetCell == null
                || outwardLocal == null || hit == null) return;
        CHANNEL.sendToServer(new PlaceGroupBlock(groupId, sourceCell,
                targetCell, outwardLocal, hit));
    }

    public static void useGroupCell(UUID groupId,
            TransformGroup.GridPos cell) {
        if (groupId == null || cell == null) return;
        CHANNEL.sendToServer(new UseGroupCell(groupId, cell));
    }

    public static void placeSurfaceBlock(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, Vec3 hit) {
        if (surfaceId == null || slot == null || hit == null) return;
        CHANNEL.sendToServer(new PlaceSurfaceBlock(surfaceId, slot, hit));
    }

    public static void sendBlockedPlacement(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new BlockedPlacement(pos));
    }

    /** Tiny runtime-state packet; avoids a full facility snapshot per door frame. */
    public static void broadcastGroupCell(ServerLevel level, UUID groupId,
            TransformGroup.GridPos cell, BlockState state) {
        if (level == null || groupId == null || cell == null || state == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension),
                new GroupCellState(groupId, cell, state));
    }

    /** Tiny runtime-state packet for one rigid/deformed surface attachment. */
    public static void broadcastSurfaceSlot(ServerLevel level, UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, BlockState state,
            boolean deform) {
        if (level == null || surfaceId == null || slot == null || state == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension),
                new SurfaceSlotState(surfaceId, slot, state, deform));
    }

    public static void sendSnapshot(ServerPlayer player) {
        if (player == null || player.getServer() == null) return;
        ResourceLocation dimension = player.level().dimension().location();
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                player.getServer());
        List<TransformGroup> groups = data.groups().stream()
                .filter(group -> group.dimension().equals(dimension)).toList();
        List<ConstructionSurface> surfaces = data.surfaces().stream()
                .filter(surface -> surface.dimension().equals(dimension)).toList();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new Snapshot(dimension, groups, surfaces));
    }

    private static void broadcast(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendSnapshot(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendSnapshot(player);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendSnapshot(player);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        long revision = TransformConstructionSavedData.get(server).revision();
        Long previous = LAST_REVISION.put(server, revision);
        if (previous != null && previous.longValue() == revision) return;
        broadcast(server);
    }

    private static void writeVec(FriendlyByteBuf buffer, Vec3 value) {
        buffer.writeDouble(value.x);
        buffer.writeDouble(value.y);
        buffer.writeDouble(value.z);
    }

    private static Vec3 readVec(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble());
    }

    private static void writeState(FriendlyByteBuf buffer, BlockState state) {
        buffer.writeNbt(BlockStateCodec.save(state));
    }

    private static BlockState readState(FriendlyByteBuf buffer) {
        return BlockStateCodec.load(buffer.readNbt());
    }

    public record Snapshot(ResourceLocation dimension, List<TransformGroup> groups,
            List<ConstructionSurface> surfaces) {
        public Snapshot {
            groups = groups == null ? List.of() : List.copyOf(groups);
            surfaces = surfaces == null ? List.of() : List.copyOf(surfaces);
        }

        private static void encode(Snapshot message, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(message.dimension);
            buffer.writeVarInt(message.groups.size());
            for (TransformGroup group : message.groups) buffer.writeNbt(group.save());
            buffer.writeVarInt(message.surfaces.size());
            for (ConstructionSurface surface : message.surfaces) {
                buffer.writeNbt(surface.save());
            }
        }

        private static Snapshot decode(FriendlyByteBuf buffer) {
            ResourceLocation dimension = buffer.readResourceLocation();
            int groupCount = Math.max(0, Math.min(buffer.readVarInt(), 16_384));
            List<TransformGroup> groups = new ArrayList<>(groupCount);
            for (int index = 0; index < groupCount; index++) {
                CompoundTag tag = buffer.readNbt();
                TransformGroup group = TransformGroup.load(tag);
                if (group != null) groups.add(group);
            }
            int surfaceCount = Math.max(0,
                    Math.min(buffer.readVarInt(), 16_384));
            List<ConstructionSurface> surfaces = new ArrayList<>(surfaceCount);
            for (int index = 0; index < surfaceCount; index++) {
                CompoundTag tag = buffer.readNbt();
                ConstructionSurface surface = ConstructionSurface.load(tag);
                if (surface != null) surfaces.add(surface);
            }
            return new Snapshot(dimension, groups, surfaces);
        }

        private static void handle(Snapshot message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState
                            .sync(message.dimension, message.groups,
                                    message.surfaces)));
            context.setPacketHandled(true);
        }
    }

    public record UpdateGroup(UUID id, Vec3 origin, float rotationX,
            float rotationY, float rotationZ) {
        private static void encode(UpdateGroup message, FriendlyByteBuf buffer) {
            buffer.writeUUID(message.id);
            writeVec(buffer, message.origin);
            buffer.writeFloat(message.rotationX);
            buffer.writeFloat(message.rotationY);
            buffer.writeFloat(message.rotationZ);
        }

        private static UpdateGroup decode(FriendlyByteBuf buffer) {
            return new UpdateGroup(buffer.readUUID(), readVec(buffer),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        }

        private static void handle(UpdateGroup message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                TransformConstructionManager.updateGroup(sender, message.id,
                        message.origin, message.rotationX, message.rotationY,
                        message.rotationZ);
                sendSnapshot(sender);
            });
            context.setPacketHandled(true);
        }
    }

    public record UpdateSurface(UUID id, Vec3 bottomStart, Vec3 bottomEnd,
            Vec3 topStart, Vec3 topEnd, Vec3 curveOffset,
            Vec3 heightCurveOffset) {
        private static void encode(UpdateSurface message,
                FriendlyByteBuf buffer) {
            buffer.writeUUID(message.id);
            writeVec(buffer, message.bottomStart);
            writeVec(buffer, message.bottomEnd);
            writeVec(buffer, message.topStart);
            writeVec(buffer, message.topEnd);
            writeVec(buffer, message.curveOffset);
            writeVec(buffer, message.heightCurveOffset);
        }

        private static UpdateSurface decode(FriendlyByteBuf buffer) {
            return new UpdateSurface(buffer.readUUID(), readVec(buffer),
                    readVec(buffer), readVec(buffer), readVec(buffer),
                    readVec(buffer), readVec(buffer));
        }

        private static void handle(UpdateSurface message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                TransformConstructionManager.updateSurface(sender, message.id,
                        message.bottomStart, message.bottomEnd, message.topStart,
                        message.topEnd, message.curveOffset,
                        message.heightCurveOffset);
                sendSnapshot(sender);
            });
            context.setPacketHandled(true);
        }
    }

    public record Delete(UUID id, boolean surface) {
        private static void encode(Delete message, FriendlyByteBuf buffer) {
            buffer.writeUUID(message.id);
            buffer.writeBoolean(message.surface);
        }

        private static Delete decode(FriendlyByteBuf buffer) {
            return new Delete(buffer.readUUID(), buffer.readBoolean());
        }

        private static void handle(Delete message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (message.surface) {
                    TransformConstructionManager.removeSurface(sender, message.id);
                } else {
                    TransformConstructionManager.removeGroup(sender, message.id);
                }
                sendSnapshot(sender);
            });
            context.setPacketHandled(true);
        }
    }

    public record SetSurfaceFlipped(UUID id, boolean flipped) {
        private static void encode(SetSurfaceFlipped message,
                FriendlyByteBuf buffer) {
            buffer.writeUUID(message.id);
            buffer.writeBoolean(message.flipped);
        }

        private static SetSurfaceFlipped decode(FriendlyByteBuf buffer) {
            return new SetSurfaceFlipped(buffer.readUUID(),
                    buffer.readBoolean());
        }

        private static void handle(SetSurfaceFlipped message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                TransformConstructionManager.setSurfaceFlipped(sender,
                        message.id, message.flipped);
                sendSnapshot(sender);
            });
            context.setPacketHandled(true);
        }
    }

    public record AuthorSurfacePoint(Vec3 point) {
        private static void encode(AuthorSurfacePoint message,
                FriendlyByteBuf buffer) {
            writeVec(buffer, message.point);
        }

        private static AuthorSurfacePoint decode(FriendlyByteBuf buffer) {
            return new AuthorSurfacePoint(readVec(buffer));
        }

        private static void handle(AuthorSurfacePoint message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() ->
                    TransformSurfaceAuthoringManager.selectPoint(
                            context.getSender(), message.point));
            context.setPacketHandled(true);
        }
    }

    public record CancelSurfaceAuthoring() {
        private static void encode(CancelSurfaceAuthoring message,
                FriendlyByteBuf buffer) {
        }

        private static CancelSurfaceAuthoring decode(FriendlyByteBuf buffer) {
            return new CancelSurfaceAuthoring();
        }

        private static void handle(CancelSurfaceAuthoring message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() ->
                    TransformSurfaceAuthoringManager.cancel(context.getSender()));
            context.setPacketHandled(true);
        }
    }

    public record PlaceGroupBlock(UUID groupId, TransformGroup.GridPos sourceCell,
            TransformGroup.GridPos targetCell,
            net.minecraft.core.Direction outwardLocal, Vec3 hit) {
        private static void encode(PlaceGroupBlock message,
                FriendlyByteBuf buffer) {
            buffer.writeUUID(message.groupId);
            buffer.writeVarInt(message.sourceCell.x());
            buffer.writeVarInt(message.sourceCell.y());
            buffer.writeVarInt(message.sourceCell.z());
            buffer.writeVarInt(message.targetCell.x());
            buffer.writeVarInt(message.targetCell.y());
            buffer.writeVarInt(message.targetCell.z());
            buffer.writeEnum(message.outwardLocal);
            writeVec(buffer, message.hit);
        }

        private static PlaceGroupBlock decode(FriendlyByteBuf buffer) {
            UUID groupId = buffer.readUUID();
            TransformGroup.GridPos source = new TransformGroup.GridPos(
                    buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt());
            TransformGroup.GridPos target = new TransformGroup.GridPos(
                    buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt());
            return new PlaceGroupBlock(groupId, source, target,
                    buffer.readEnum(net.minecraft.core.Direction.class),
                    readVec(buffer));
        }

        private static void handle(PlaceGroupBlock message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() ->
                    TransformConstructionManager.placeGroupBlock(
                            context.getSender(), message.groupId,
                            message.sourceCell, message.targetCell,
                            message.outwardLocal, message.hit));
            context.setPacketHandled(true);
        }
    }

    public record UseGroupCell(UUID groupId,
            TransformGroup.GridPos cell) {
        private static void encode(UseGroupCell message,
                FriendlyByteBuf buffer) {
            buffer.writeUUID(message.groupId);
            buffer.writeVarInt(message.cell.x());
            buffer.writeVarInt(message.cell.y());
            buffer.writeVarInt(message.cell.z());
        }

        private static UseGroupCell decode(FriendlyByteBuf buffer) {
            return new UseGroupCell(buffer.readUUID(),
                    new TransformGroup.GridPos(buffer.readVarInt(),
                            buffer.readVarInt(), buffer.readVarInt()));
        }

        private static void handle(UseGroupCell message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null) return;
                if (!TransformControlRuntime.useGroupCell(sender,
                        message.groupId, message.cell)) {
                    TransformDoorRuntime.useGroupCell(sender,
                            message.groupId, message.cell);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record PlaceSurfaceBlock(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, Vec3 hit) {
        private static void encode(PlaceSurfaceBlock message,
                FriendlyByteBuf buffer) {
            buffer.writeUUID(message.surfaceId);
            buffer.writeVarInt(message.slot.column());
            buffer.writeVarInt(message.slot.row());
            writeVec(buffer, message.hit);
        }

        private static PlaceSurfaceBlock decode(FriendlyByteBuf buffer) {
            return new PlaceSurfaceBlock(buffer.readUUID(),
                    new ConstructionSurface.SurfaceSlot(buffer.readVarInt(),
                            buffer.readVarInt()), readVec(buffer));
        }

        private static void handle(PlaceSurfaceBlock message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() ->
                    TransformConstructionManager.placeSurfaceBlock(
                            context.getSender(), message.surfaceId,
                            message.slot, message.hit));
            context.setPacketHandled(true);
        }
    }

    public record BlockedPlacement(BlockPos pos) {
        private static void encode(BlockedPlacement message,
                FriendlyByteBuf buffer) {
            buffer.writeBlockPos(message.pos);
        }

        private static BlockedPlacement decode(FriendlyByteBuf buffer) {
            return new BlockedPlacement(buffer.readBlockPos());
        }

        private static void handle(BlockedPlacement message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState
                            .flashBlocked(message.pos)));
            context.setPacketHandled(true);
        }
    }

    public record GroupCellState(UUID groupId, TransformGroup.GridPos cell,
            BlockState state) {
        private static void encode(GroupCellState message,
                FriendlyByteBuf buffer) {
            buffer.writeUUID(message.groupId);
            buffer.writeVarInt(message.cell.x());
            buffer.writeVarInt(message.cell.y());
            buffer.writeVarInt(message.cell.z());
            writeState(buffer, message.state);
        }

        private static GroupCellState decode(FriendlyByteBuf buffer) {
            return new GroupCellState(buffer.readUUID(),
                    new TransformGroup.GridPos(buffer.readVarInt(),
                            buffer.readVarInt(), buffer.readVarInt()),
                    readState(buffer));
        }

        private static void handle(GroupCellState message,
                Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        var client = com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.class;
                        TransformGroup group = com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState
                                .group(message.groupId);
                        if (group != null) {
                            com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState
                                    .upsertGroup(group.withCell(message.cell,
                                            message.state));
                        }
                    }));
            context.setPacketHandled(true);
        }
    }

    public record SurfaceSlotState(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, BlockState state,
            boolean deform) {
        private static void encode(SurfaceSlotState message,
                FriendlyByteBuf buffer) {
            buffer.writeUUID(message.surfaceId);
            buffer.writeVarInt(message.slot.column());
            buffer.writeVarInt(message.slot.row());
            writeState(buffer, message.state);
            buffer.writeBoolean(message.deform);
        }

        private static SurfaceSlotState decode(FriendlyByteBuf buffer) {
            return new SurfaceSlotState(buffer.readUUID(),
                    new ConstructionSurface.SurfaceSlot(buffer.readVarInt(),
                            buffer.readVarInt()), readState(buffer),
                    buffer.readBoolean());
        }

        private static void handle(SurfaceSlotState message,
                Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        ConstructionSurface surface = com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState
                                .surface(message.surfaceId);
                        if (surface != null) {
                            com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState
                                    .upsertSurface(surface.withAttachment(
                                            message.slot, message.state,
                                            message.deform));
                        }
                    }));
            context.setPacketHandled(true);
        }
    }
}
