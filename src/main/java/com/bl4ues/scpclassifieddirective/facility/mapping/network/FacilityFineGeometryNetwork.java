package com.bl4ues.scpclassifieddirective.facility.mapping.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingSavedData;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoom;
import com.bl4ues.scpclassifieddirective.init.FacilityMappingItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
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
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Precise polygon geometry companion channel for Facility Mapping. The legacy
 * room packet remains compatible and authoritative for metadata; this channel
 * overlays sub-block outlines on those patches and carries editor updates.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FacilityFineGeometryNetwork {
    private static final String PROTOCOL = "1";
    private static final int MAX_VERTICES = 256;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                    "facility_fine_geometry"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static boolean registered;

    private FacilityFineGeometryNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, GeometrySync.class,
                GeometrySync::encode, GeometrySync::decode, GeometrySync::handle);
        CHANNEL.registerMessage(1, UpdatePatch.class,
                UpdatePatch::encode, UpdatePatch::decode, UpdatePatch::handle);
    }

    public static void requestPatchUpdate(UUID roomId, int patchIndex, int y,
            List<FacilityFloorPatch.Vertex> vertices) {
        if (roomId == null || vertices == null) return;
        CHANNEL.sendToServer(new UpdatePatch(roomId, patchIndex, y, vertices));
    }

    public static void sendSync(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), snapshot(level));
    }

    public static void broadcast(ServerLevel level) {
        if (level == null) return;
        GeometrySync packet = snapshot(level);
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
    }

    private static GeometrySync snapshot(ServerLevel level) {
        List<PatchGeometry> geometry = new ArrayList<>();
        FacilityMappingSavedData data = FacilityMappingSavedData.get(level.getServer());
        ResourceLocation dimension = level.dimension().location();
        for (FacilityRoom room : data.rooms()) {
            if (!room.dimension().equals(dimension)) continue;
            for (int index = 0; index < room.patches().size(); index++) {
                FacilityFloorPatch patch = room.patches().get(index);
                if (patch.isPolygon()) {
                    geometry.add(new PatchGeometry(room.id(), index, patch.y(),
                            patch.vertices()));
                }
            }
        }
        return new GeometrySync(dimension, geometry);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendSync(player);
    }

    @SubscribeEvent
    public static void onChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendSync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendSync(player);
    }

    public record PatchGeometry(UUID roomId, int patchIndex, int y,
            List<FacilityFloorPatch.Vertex> vertices) {
        public PatchGeometry {
            vertices = vertices == null ? List.of() : List.copyOf(vertices);
        }
    }

    public record GeometrySync(ResourceLocation dimension,
            List<PatchGeometry> patches) {
        public GeometrySync {
            patches = patches == null ? List.of() : List.copyOf(patches);
        }

        private static void encode(GeometrySync message, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(message.dimension);
            buffer.writeVarInt(Math.min(8192, message.patches.size()));
            for (int i = 0; i < message.patches.size() && i < 8192; i++) {
                writeGeometry(buffer, message.patches.get(i));
            }
        }

        private static GeometrySync decode(FriendlyByteBuf buffer) {
            ResourceLocation dimension = buffer.readResourceLocation();
            int count = Math.max(0, Math.min(8192, buffer.readVarInt()));
            List<PatchGeometry> patches = new ArrayList<>(count);
            for (int i = 0; i < count; i++) patches.add(readGeometry(buffer));
            return new GeometrySync(dimension, patches);
        }

        private static void handle(GeometrySync message,
                Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState
                            .syncFineGeometry(message.dimension, message.patches)));
            context.setPacketHandled(true);
        }
    }

    public record UpdatePatch(UUID roomId, int patchIndex, int y,
            List<FacilityFloorPatch.Vertex> vertices) {
        public UpdatePatch {
            vertices = vertices == null ? List.of() : List.copyOf(vertices);
        }

        private static void encode(UpdatePatch message, FriendlyByteBuf buffer) {
            buffer.writeUUID(message.roomId);
            buffer.writeVarInt(message.patchIndex);
            buffer.writeInt(message.y);
            writeVertices(buffer, message.vertices);
        }

        private static UpdatePatch decode(FriendlyByteBuf buffer) {
            return new UpdatePatch(buffer.readUUID(), buffer.readVarInt(),
                    buffer.readInt(), readVertices(buffer));
        }

        private static void handle(UpdatePatch message,
                Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> apply(context.getSender(), message));
            context.setPacketHandled(true);
        }
    }

    private static void apply(ServerPlayer player, UpdatePatch message) {
        if (player == null || !player.isCreative()
                || !player.getMainHandItem().is(FacilityMappingItems.getTool())
                || !(player.level() instanceof ServerLevel level)
                || message.roomId == null || message.vertices.size() < 3
                || message.vertices.size() > MAX_VERTICES) return;
        FacilityMappingSavedData data = FacilityMappingSavedData.get(level.getServer());
        FacilityRoom room = data.room(message.roomId);
        if (room == null || !room.dimension().equals(level.dimension().location())
                || message.patchIndex < 0
                || message.patchIndex >= room.patches().size()) return;
        FacilityFloorPatch previous = room.patches().get(message.patchIndex);
        if (message.y != previous.y()) return;
        FacilityFloorPatch patch = FacilityFloorPatch.polygon(previous.y(),
                message.vertices);
        if (patch == null || patch.area() <= 0L
                || patch.area() > FacilityMappingManager.MAX_PATCH_AREA
                || patch.maxX() - patch.minX() + 1 > FacilityMappingManager.MAX_PATCH_SPAN
                || patch.maxZ() - patch.minZ() + 1 > FacilityMappingManager.MAX_PATCH_SPAN) {
            return;
        }
        data.putRoom(room.withReplacedPatch(message.patchIndex, patch));
        for (ServerPlayer listener : level.players()) {
            FacilityMappingManager.sync(listener);
        }
        broadcast(level);
    }

    private static void writeGeometry(FriendlyByteBuf buffer,
            PatchGeometry geometry) {
        buffer.writeUUID(geometry.roomId);
        buffer.writeVarInt(geometry.patchIndex);
        buffer.writeInt(geometry.y);
        writeVertices(buffer, geometry.vertices);
    }

    private static PatchGeometry readGeometry(FriendlyByteBuf buffer) {
        return new PatchGeometry(buffer.readUUID(), buffer.readVarInt(),
                buffer.readInt(), readVertices(buffer));
    }

    private static void writeVertices(FriendlyByteBuf buffer,
            List<FacilityFloorPatch.Vertex> vertices) {
        int count = Math.min(MAX_VERTICES, vertices.size());
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            FacilityFloorPatch.Vertex vertex = vertices.get(i);
            buffer.writeDouble(vertex.x());
            buffer.writeDouble(vertex.z());
        }
    }

    private static List<FacilityFloorPatch.Vertex> readVertices(
            FriendlyByteBuf buffer) {
        int count = Math.max(0, Math.min(MAX_VERTICES, buffer.readVarInt()));
        List<FacilityFloorPatch.Vertex> vertices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            vertices.add(new FacilityFloorPatch.Vertex(buffer.readDouble(),
                    buffer.readDouble()));
        }
        return vertices;
    }
}
