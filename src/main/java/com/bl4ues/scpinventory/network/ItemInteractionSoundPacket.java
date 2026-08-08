package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.client.ClientItemInteractionSounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-authoritative action confirmation with client-personal presentation. */
public final class ItemInteractionSoundPacket {
    public enum Cue {
        PICKUP,
        EQUIP,
        FOOD,
        DRINK
    }

    private final Cue cue;

    public ItemInteractionSoundPacket(Cue cue) {
        this.cue = cue == null ? Cue.PICKUP : cue;
    }

    public static void encode(ItemInteractionSoundPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeEnum(message.cue);
    }

    public static ItemInteractionSoundPacket decode(FriendlyByteBuf buffer) {
        return new ItemInteractionSoundPacket(buffer.readEnum(Cue.class));
    }

    public static void handle(ItemInteractionSoundPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() ->
                ClientItemInteractionSounds.play(message.cue));
        contextSupplier.get().setPacketHandled(true);
    }
}
