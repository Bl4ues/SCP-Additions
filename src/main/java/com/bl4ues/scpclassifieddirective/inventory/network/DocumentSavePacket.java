package com.bl4ues.scpclassifieddirective.inventory.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.config.ui.CodexAssetStorage;
import com.bl4ues.scpclassifieddirective.document.DocumentData;

import java.util.function.Supplier;

/** Saves edits to the actual held document after server-side sanitization. */
public record DocumentSavePacket(InteractionHand hand,
                                 CompoundTag documentTag) {
    public DocumentSavePacket {
        hand = hand == null ? InteractionHand.MAIN_HAND : hand;
        documentTag = documentTag == null
                ? new CompoundTag() : documentTag.copy();
    }

    public static void encode(DocumentSavePacket message,
                              FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.hand == InteractionHand.OFF_HAND);
        buffer.writeNbt(message.documentTag);
    }

    public static DocumentSavePacket decode(FriendlyByteBuf buffer) {
        InteractionHand hand = buffer.readBoolean()
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        CompoundTag tag = buffer.readNbt();
        return new DocumentSavePacket(hand,
                tag == null ? new CompoundTag() : tag);
    }

    public static void handle(DocumentSavePacket message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.isCreative()) return;

            ItemStack stack = player.getItemInHand(message.hand);
            if (!DocumentData.isDedicatedItem(stack)) return;

            DocumentData.State submitted =
                    DocumentData.read(message.documentTag);
            DocumentData.State existing =
                    DocumentData.read(stack);
            String id = existing.documentId().isBlank()
                    ? submitted.documentId()
                    : existing.documentId();

            String photoKey = submitted.photoKey();
            if (!photoKey.isBlank()
                    && !CodexAssetStorage.isSafeKey(photoKey)) {
                photoKey = "";
            }

            DocumentData.State safe = new DocumentData.State(
                    id,
                    submitted.template(),
                    submitted.title(),
                    submitted.category(),
                    submitted.header1(),
                    submitted.value1(),
                    submitted.header2(),
                    submitted.value2(),
                    submitted.header3(),
                    submitted.value3(),
                    submitted.body(),
                    photoKey,
                    submitted.photoWidth(),
                    submitted.photoHeight(),
                    submitted.caption());

            DocumentData.write(stack, safe);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        });
        context.setPacketHandled(true);
    }
}
