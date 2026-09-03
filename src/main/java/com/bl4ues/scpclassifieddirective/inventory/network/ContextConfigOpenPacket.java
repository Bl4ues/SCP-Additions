package com.bl4ues.scpclassifieddirective.inventory.network;

import com.bl4ues.scpclassifieddirective.inventory.client.ContextConfigClientHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ContextConfigOpenPacket {
    private static final int MAX_JSON_LENGTH = 32767;

    private final BlockPos pos;
    private final String blockId;
    private final boolean existing;
    private final String action;
    private final String name;
    private final boolean showName;
    private final double range;
    private final boolean allowE;
    private final boolean allowRightClick;
    private final boolean allowOffscreen;
    private final boolean likelyRightClick;
    private final String useItem;
    private final String icon;
    private final String requiredItem;
    private final String variantsJson;
    private final String clickFace;
    private final String rotateWith;
    private final double anchorX;
    private final double anchorY;
    private final double anchorZ;

    public ContextConfigOpenPacket(BlockPos pos, String blockId,
            boolean existing, String action, String name, boolean showName,
            double range, boolean allowE, boolean allowRightClick,
            boolean allowOffscreen, boolean likelyRightClick, String useItem,
            String clickFace, String rotateWith, double anchorX,
            double anchorY, double anchorZ) {
        this(pos, blockId, existing, action, name, showName, range, allowE,
                allowRightClick, allowOffscreen, likelyRightClick, useItem,
                useItem, "", "[]", clickFace, rotateWith, anchorX, anchorY,
                anchorZ);
    }

    public ContextConfigOpenPacket(BlockPos pos, String blockId,
            boolean existing, String action, String name, boolean showName,
            double range, boolean allowE, boolean allowRightClick,
            boolean allowOffscreen, boolean likelyRightClick, String useItem,
            String icon, String requiredItem, String variantsJson,
            String clickFace, String rotateWith, double anchorX,
            double anchorY, double anchorZ) {
        this.pos = pos == null ? BlockPos.ZERO : pos;
        this.blockId = blockId == null ? "minecraft:air" : blockId;
        this.existing = existing;
        this.action = action == null ? "Use" : action;
        this.name = name == null ? "" : name;
        this.showName = showName;
        this.range = range;
        // E is no longer a contextual-interaction control. Preserve old E-only
        // definitions by exposing them to editors as vanilla Use/right-click.
        this.allowE = false;
        this.allowRightClick = allowRightClick || allowE;
        this.allowOffscreen = allowOffscreen;
        this.likelyRightClick = likelyRightClick;
        this.useItem = normalize(useItem, "hand");
        this.icon = normalize(icon, this.useItem);
        this.requiredItem = normalize(requiredItem, "");
        this.variantsJson = normalize(variantsJson, "[]");
        this.clickFace = normalize(clickFace, "front");
        this.rotateWith = normalize(rotateWith, "none");
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
    }

    public static void encode(ContextConfigOpenPacket msg,
            FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.blockId);
        buf.writeBoolean(msg.existing);
        buf.writeUtf(msg.action);
        buf.writeUtf(msg.name);
        buf.writeBoolean(msg.showName);
        buf.writeDouble(msg.range);
        buf.writeBoolean(msg.allowE);
        buf.writeBoolean(msg.allowRightClick);
        buf.writeBoolean(msg.allowOffscreen);
        buf.writeBoolean(msg.likelyRightClick);
        buf.writeUtf(msg.useItem);
        buf.writeUtf(msg.icon);
        buf.writeUtf(msg.requiredItem);
        buf.writeUtf(msg.variantsJson, MAX_JSON_LENGTH);
        buf.writeUtf(msg.clickFace);
        buf.writeUtf(msg.rotateWith);
        buf.writeDouble(msg.anchorX);
        buf.writeDouble(msg.anchorY);
        buf.writeDouble(msg.anchorZ);
    }

    public static ContextConfigOpenPacket decode(FriendlyByteBuf buf) {
        return new ContextConfigOpenPacket(
                buf.readBlockPos(), buf.readUtf(), buf.readBoolean(),
                buf.readUtf(), buf.readUtf(), buf.readBoolean(),
                buf.readDouble(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(), buf.readUtf(),
                buf.readUtf(), buf.readUtf(), buf.readUtf(MAX_JSON_LENGTH),
                buf.readUtf(), buf.readUtf(), buf.readDouble(),
                buf.readDouble(), buf.readDouble());
    }

    public static void handle(ContextConfigOpenPacket msg,
            Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ContextConfigClientHandler.open(msg));
        ctx.get().setPacketHandled(true);
    }

    private static String normalize(String value, String fallback) {
        return value == null ? fallback : value;
    }

    public BlockPos pos() { return pos; }
    public String blockId() { return blockId; }
    public boolean existing() { return existing; }
    public String action() { return action; }
    public String name() { return name; }
    public boolean showName() { return showName; }
    public double range() { return range; }
    public boolean allowE() { return allowE; }
    public boolean allowRightClick() { return allowRightClick; }
    public boolean allowOffscreen() { return allowOffscreen; }
    public boolean likelyRightClick() { return likelyRightClick; }
    public String useItem() { return useItem; }
    public String icon() { return icon; }
    public String requiredItem() { return requiredItem; }
    public String variantsJson() { return variantsJson; }
    public String clickFace() { return clickFace; }
    public String rotateWith() { return rotateWith; }
    public double anchorX() { return anchorX; }
    public double anchorY() { return anchorY; }
    public double anchorZ() { return anchorZ; }
}
