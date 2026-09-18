package com.bl4ues.scpclassifieddirective.facility.transform.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Small dedicated channel for per-cell surface presentation choices. Keeping
 * this separate from the snapshot/edit channel lets old authored surfaces load
 * unchanged while the editor gains explicit rigid/deformed attachment control.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TransformAttachmentNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                    "transform_attachment"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static boolean registered;

    private TransformAttachmentNetwork() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(TransformAttachmentNetwork::register);
    }

    private static synchronized void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, SetAttachmentMode.class,
                SetAttachmentMode::encode,
                SetAttachmentMode::decode,
                SetAttachmentMode::handle);
    }

    public static void setMode(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, boolean deform) {
        if (surfaceId == null || slot == null) return;
        CHANNEL.sendToServer(new SetAttachmentMode(surfaceId, slot.column(),
                slot.row(), deform));
    }

    private record SetAttachmentMode(UUID surfaceId, int column, int row,
            boolean deform) {
        private static void encode(SetAttachmentMode message,
                FriendlyByteBuf buffer) {
            buffer.writeUUID(message.surfaceId);
            buffer.writeVarInt(message.column);
            buffer.writeVarInt(message.row);
            buffer.writeBoolean(message.deform);
        }

        private static SetAttachmentMode decode(FriendlyByteBuf buffer) {
            return new SetAttachmentMode(buffer.readUUID(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readBoolean());
        }

        private static void handle(SetAttachmentMode message,
                Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !TransformConstructionManager.canEdit(player)
                        || player.getServer() == null) return;
                TransformConstructionSavedData data =
                        TransformConstructionSavedData.get(player.getServer());
                ConstructionSurface surface = data.surface(message.surfaceId);
                if (surface == null || !surface.dimension().equals(
                        player.level().dimension().location())) return;
                ConstructionSurface.SurfaceSlot slot =
                        new ConstructionSurface.SurfaceSlot(message.column,
                                message.row);
                ConstructionSurface.SurfaceAttachment current =
                        surface.attachments().get(slot);
                if (current == null || current.deform() == message.deform) return;

                Map<ConstructionSurface.SurfaceSlot,
                        ConstructionSurface.SurfaceAttachment> attachments =
                        new LinkedHashMap<>(surface.attachments());
                attachments.put(slot, new ConstructionSurface.SurfaceAttachment(
                        current.state(), message.deform));
                ConstructionSurface updated = new ConstructionSurface(
                        surface.id(), surface.dimension(), surface.bottomStart(),
                        surface.bottomEnd(), surface.topStart(), surface.topEnd(),
                        surface.curveOffset(), surface.heightCurveOffset(),
                        attachments, surface.overlays(), surface.flipped());
                data.putSurface(updated);
                TransformConstructionManager.refreshSurface(player.getServer(),
                        surface.id());
                TransformConstructionNetwork.broadcastSurfaceSlot(
                        (net.minecraft.server.level.ServerLevel) player.level(),
                        surface.id(), slot, current.state(), message.deform);
                TransformConstructionNetwork.acknowledgeRevision(
                        player.getServer());
            });
            context.setPacketHandled(true);
        }
    }
}
