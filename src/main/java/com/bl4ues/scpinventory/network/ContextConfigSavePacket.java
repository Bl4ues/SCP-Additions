package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.context.ContextConfigSaveService;
import com.bl4ues.scpinventory.context.ContextEntityConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.mcreator.scpadditions.config.ui.ConfigCenterService;

import java.util.function.Supplier;

public class ContextConfigSavePacket {
    private final BlockPos pos;
    private final String blockId;
    private final String action;
    private final String name;
    private final boolean showName;
    private final double range;
    private final boolean allowE;
    private final boolean allowRightClick;
    private final String useItem;
    private final String clickFace;
    private final String rotateWith;
    private final double anchorX;
    private final double anchorY;
    private final double anchorZ;

    public ContextConfigSavePacket(BlockPos pos, String blockId, String action, String name, boolean showName, double range,
                                   boolean allowE, boolean allowRightClick, String useItem, String clickFace, String rotateWith,
                                   double anchorX, double anchorY, double anchorZ) {
        this.pos = pos == null ? BlockPos.ZERO : pos;
        this.blockId = blockId == null ? "minecraft:air" : blockId;
        this.action = action == null ? "Use" : action;
        this.name = name == null ? "" : name;
        this.showName = showName;
        this.range = range;
        this.allowE = allowE;
        this.allowRightClick = allowRightClick;
        this.useItem = useItem == null ? "hand" : useItem;
        this.clickFace = clickFace == null ? "front" : clickFace;
        this.rotateWith = rotateWith == null ? "none" : rotateWith;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
    }

    public static void encode(ContextConfigSavePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.blockId);
        buf.writeUtf(msg.action);
        buf.writeUtf(msg.name);
        buf.writeBoolean(msg.showName);
        buf.writeDouble(msg.range);
        buf.writeBoolean(msg.allowE);
        buf.writeBoolean(msg.allowRightClick);
        buf.writeUtf(msg.useItem);
        buf.writeUtf(msg.clickFace);
        buf.writeUtf(msg.rotateWith);
        buf.writeDouble(msg.anchorX);
        buf.writeDouble(msg.anchorY);
        buf.writeDouble(msg.anchorZ);
    }

    public static ContextConfigSavePacket decode(FriendlyByteBuf buf) {
        return new ContextConfigSavePacket(
                buf.readBlockPos(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readDouble(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );
    }

    public static void handle(ContextConfigSavePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (!ConfigCenterService.requireEdit(player)) return;

            boolean handledAsEntity = ContextEntityConfigManager.saveClientRuleIfEntitySession(
                    player, msg.pos, msg.blockId, msg.action, msg.name, msg.showName, msg.range,
                    msg.allowE, msg.allowRightClick, msg.useItem, msg.clickFace, msg.rotateWith,
                    msg.anchorX, msg.anchorY, msg.anchorZ);
            if (handledAsEntity) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new ContextConfigReloadPacket());
                return;
            }

            ConfigCenterService.SaveResult result = ContextConfigSaveService.saveBlockRule(
                    player, msg.pos, msg.blockId, msg.action, msg.name, msg.showName, msg.range,
                    msg.allowE, msg.allowRightClick, msg.useItem, msg.clickFace, msg.rotateWith,
                    msg.anchorX, msg.anchorY, msg.anchorZ);
            if (!result.success()) {
                player.sendSystemMessage(Component.literal(
                        "[SCP Inventory] Could not save context interaction: " + result.message())
                        .withStyle(ChatFormatting.RED));
                return;
            }

            player.sendSystemMessage(Component.literal(
                    "[SCP Inventory] Saved context interaction for " + msg.blockId
                            + " at anchor [" + round(msg.anchorX) + ", "
                            + round(msg.anchorY) + ", " + round(msg.anchorZ) + "]")
                    .withStyle(ChatFormatting.GREEN));
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new ContextConfigReloadPacket());
        });
        ctx.get().setPacketHandled(true);
    }

    private static double round(double value) {
        return Math.round(value * 1000.0D) / 1000.0D;
    }
}
