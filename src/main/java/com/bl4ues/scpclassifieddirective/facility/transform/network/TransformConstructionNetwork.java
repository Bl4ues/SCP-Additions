package com.bl4ues.scpclassifieddirective.facility.transform.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionSavedData;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
    }

    public static void updateGroup(UUID id, Vec3 origin, float rotationX,
            float rotationY, float rotationZ) {
        if (id == null || origin == null) return;
        CHANNEL.sendToServer(new UpdateGroup(id, origin, rotationX, rotationY,
                rotationZ));
    }

    public static void updateSurface(UUID id, Vec3 bottomStart, Vec3 bottomEnd,
            Vec3 topStart, Vec3 topEnd, Vec3 curveOffset) {
        if (id == null || bottomStart == null || bottomEnd == null
                || topStart == null || topEnd == null || curveOffset == null) {
            return;
        }
        CHANNEL.sendToServer(new UpdateSurface(id, bottomStart, bottomEnd,
                topStart, topEnd, curveOffset));
    }

    public static void delete(UUID id, boolean surface) {
        if (id != null) CHANNEL.sendToServer(new Delete(id, surface));
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
                // Always return the authoritative value. Invalid drag previews
                // therefore snap back instead of lingering only on this client.
                sendSnapshot(sender);
            });
            context.setPacketHandled(true);
        }
    }

    public record UpdateSurface(UUID id, Vec3 bottomStart, Vec3 bottomEnd,
            Vec3 topStart, Vec3 topEnd, Vec3 curveOffset) {
        private static void encode(UpdateSurface message,
                FriendlyByteBuf buffer) {
            buffer.writeUUID(message.id);
            writeVec(buffer, message.bottomStart);
            writeVec(buffer, message.bottomEnd);
            writeVec(buffer, message.topStart);
            writeVec(buffer, message.topEnd);
            writeVec(buffer, message.curveOffset);
        }

        private static UpdateSurface decode(FriendlyByteBuf buffer) {
            return new UpdateSurface(buffer.readUUID(), readVec(buffer),
                    readVec(buffer), readVec(buffer), readVec(buffer),
                    readVec(buffer));
        }

        private static void handle(UpdateSurface message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                TransformConstructionManager.updateSurface(sender, message.id,
                        message.bottomStart, message.bottomEnd, message.topStart,
                        message.topEnd, message.curveOffset);
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
}
