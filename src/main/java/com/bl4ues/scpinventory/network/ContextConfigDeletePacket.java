package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.context.ContextConfigManager;
import com.bl4ues.scpinventory.context.ContextEntityConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class ContextConfigDeletePacket {
    private final BlockPos pos;
    private final String blockId;

    public ContextConfigDeletePacket(BlockPos pos, String blockId) {
        this.pos = pos == null ? BlockPos.ZERO : pos;
        this.blockId = blockId == null ? "minecraft:air" : blockId;
    }

    public static void encode(ContextConfigDeletePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.blockId);
    }

    public static ContextConfigDeletePacket decode(FriendlyByteBuf buf) {
        return new ContextConfigDeletePacket(buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(ContextConfigDeletePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && !player.isSpectator()) {
                if (!ContextEntityConfigManager.deleteClientRuleIfEntitySession(player, msg.blockId)) {
                    ContextConfigManager.deleteClientRule(player, msg.pos, msg.blockId);
                }
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ContextConfigReloadPacket());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
