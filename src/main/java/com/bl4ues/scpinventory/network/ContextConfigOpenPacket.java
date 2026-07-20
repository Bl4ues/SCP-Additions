package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.client.ContextConfigClientHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class ContextConfigOpenPacket {
    private final BlockPos pos;
    private final String blockId;
    private final boolean existing;
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

    public ContextConfigOpenPacket(BlockPos pos, String blockId, boolean existing, String action, String name, boolean showName, double range,
                                   boolean allowE, boolean allowRightClick, String useItem, String clickFace, String rotateWith,
                                   double anchorX, double anchorY, double anchorZ) {
        this.pos = pos == null ? BlockPos.ZERO : pos;
        this.blockId = blockId == null ? "minecraft:air" : blockId;
        this.existing = existing;
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

    public static void encode(ContextConfigOpenPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.blockId);
        buf.writeBoolean(msg.existing);
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

    public static ContextConfigOpenPacket decode(FriendlyByteBuf buf) {
        return new ContextConfigOpenPacket(
                buf.readBlockPos(),
                buf.readUtf(),
                buf.readBoolean(),
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

    public static void handle(ContextConfigOpenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ContextConfigClientHandler.open(msg));
        ctx.get().setPacketHandled(true);
    }

    public BlockPos pos() {
        return pos;
    }

    public String blockId() {
        return blockId;
    }

    public boolean existing() {
        return existing;
    }

    public String action() {
        return action;
    }

    public String name() {
        return name;
    }

    public boolean showName() {
        return showName;
    }

    public double range() {
        return range;
    }

    public boolean allowE() {
        return allowE;
    }

    public boolean allowRightClick() {
        return allowRightClick;
    }

    public String useItem() {
        return useItem;
    }

    public String clickFace() {
        return clickFace;
    }

    public String rotateWith() {
        return rotateWith;
    }

    public double anchorX() {
        return anchorX;
    }

    public double anchorY() {
        return anchorY;
    }

    public double anchorZ() {
        return anchorZ;
    }
}
