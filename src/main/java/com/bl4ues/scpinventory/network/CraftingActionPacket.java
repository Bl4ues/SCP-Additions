package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.crafting.ScpCraftingService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client request for a server-authoritative crafting interaction. */
public final class CraftingActionPacket {
    public static final int MOVE_MAIN_TO_GRID = 0;
    public static final int MOVE_GRID_TO_MAIN = 1;
    public static final int MOVE_GRID_TO_GRID = 2;
    public static final int AUTO_FILL = 3;
    public static final int CRAFT = 4;
    public static final int TOGGLE_PIN = 5;

    private final int action;
    private final int source;
    private final int target;
    private final String recipeId;

    public CraftingActionPacket(int action, int source, int target,
                                ResourceLocation recipeId) {
        this.action = action;
        this.source = source;
        this.target = target;
        this.recipeId = recipeId == null ? "" : recipeId.toString();
    }

    public static void encode(CraftingActionPacket message,
                              FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.action);
        buffer.writeVarInt(message.source);
        buffer.writeVarInt(message.target);
        buffer.writeUtf(message.recipeId, 256);
    }

    public static CraftingActionPacket decode(FriendlyByteBuf buffer) {
        int action = buffer.readVarInt();
        int source = buffer.readVarInt();
        int target = buffer.readVarInt();
        ResourceLocation recipeId = ResourceLocation.tryParse(
                buffer.readUtf(256));
        return new CraftingActionPacket(action, source, target, recipeId);
    }

    public static void handle(CraftingActionPacket message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> ScpCraftingService.handle(player,
                message.action, message.source, message.target,
                ResourceLocation.tryParse(message.recipeId)));
        context.setPacketHandled(true);
    }
}
