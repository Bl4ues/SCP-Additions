package com.bl4ues.scpclassifieddirective.inventory.network;

import com.bl4ues.scpclassifieddirective.inventory.context.ContextConfigSaveService;
import com.bl4ues.scpclassifieddirective.inventory.context.ContextEntityConfigManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterService;

import java.util.function.Supplier;

public class ContextConfigSavePacket {
    private static final int MAX_JSON_LENGTH = 32767;

    private final BlockPos pos;
    private final String blockId;
    private final String action;
    private final String name;
    private final boolean showName;
    private final double range;
    private final boolean allowE;
    private final boolean allowRightClick;
    private final boolean allowOffscreen;
    private final String useItem;
    private final String icon;
    private final String requiredItem;
    private final String variantsJson;
    private final String clickFace;
    private final String rotateWith;
    private final double anchorX;
    private final double anchorY;
    private final double anchorZ;

    public ContextConfigSavePacket(BlockPos pos, String blockId,
            String action, String name, boolean showName, double range,
            boolean allowE, boolean allowRightClick, boolean allowOffscreen,
            String useItem, String clickFace, String rotateWith,
            double anchorX, double anchorY, double anchorZ) {
        this(pos, blockId, action, name, showName, range, allowE,
                allowRightClick, allowOffscreen, useItem, useItem, "", "[]",
                clickFace, rotateWith, anchorX, anchorY, anchorZ);
    }

    public ContextConfigSavePacket(BlockPos pos, String blockId,
            String action, String name, boolean showName, double range,
            boolean allowE, boolean allowRightClick, boolean allowOffscreen,
            String useItem, String icon, String requiredItem,
            String variantsJson, String clickFace, String rotateWith,
            double anchorX, double anchorY, double anchorZ) {
        this.pos = pos == null ? BlockPos.ZERO : pos;
        this.blockId = blockId == null ? "minecraft:air" : blockId;
        this.action = action == null ? "Use" : action;
        this.name = name == null ? "" : name;
        this.showName = showName;
        this.range = range;
        // Convert legacy E permission into vanilla Use/right-click permission.
        // Both-false remains valid for physical controls such as the SCP-914 dial.
        this.allowE = false;
        this.allowRightClick = allowRightClick || allowE;
        this.allowOffscreen = allowOffscreen;
        this.useItem = normalize(useItem, "hand");
        this.icon = normalize(icon, this.useItem);
        this.requiredItem = normalize(requiredItem, "");
        this.variantsJson = normalizeVariantInputs(variantsJson);
        this.clickFace = normalize(clickFace, "front");
        this.rotateWith = normalize(rotateWith, "none");
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
    }

    public static void encode(ContextConfigSavePacket msg,
            FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.blockId);
        buf.writeUtf(msg.action);
        buf.writeUtf(msg.name);
        buf.writeBoolean(msg.showName);
        buf.writeDouble(msg.range);
        buf.writeBoolean(msg.allowE);
        buf.writeBoolean(msg.allowRightClick);
        buf.writeBoolean(msg.allowOffscreen);
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

    public static ContextConfigSavePacket decode(FriendlyByteBuf buf) {
        return new ContextConfigSavePacket(
                buf.readBlockPos(), buf.readUtf(), buf.readUtf(),
                buf.readUtf(), buf.readBoolean(), buf.readDouble(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readUtf(), buf.readUtf(), buf.readUtf(),
                buf.readUtf(MAX_JSON_LENGTH), buf.readUtf(), buf.readUtf(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(ContextConfigSavePacket msg,
            Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (!ConfigCenterService.requireEdit(player)) return;

            boolean handledAsEntity =
                    ContextEntityConfigManager.saveClientRuleIfEntitySession(
                            player, msg.pos, msg.blockId, msg.action, msg.name,
                            msg.showName, msg.range, msg.allowE,
                            msg.allowRightClick, msg.allowOffscreen,
                            msg.useItem, msg.icon, msg.requiredItem,
                            msg.variantsJson, msg.clickFace, msg.rotateWith,
                            msg.anchorX, msg.anchorY, msg.anchorZ);
            if (handledAsEntity) {
                ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new ContextConfigReloadPacket());
                return;
            }

            ConfigCenterService.SaveResult result =
                    ContextConfigSaveService.saveBlockRule(
                            player, msg.pos, msg.blockId, msg.action, msg.name,
                            msg.showName, msg.range, msg.allowE,
                            msg.allowRightClick, msg.allowOffscreen,
                            msg.useItem, msg.icon, msg.requiredItem,
                            msg.variantsJson, msg.clickFace, msg.rotateWith,
                            msg.anchorX, msg.anchorY, msg.anchorZ);
            if (!result.success()) {
                player.sendSystemMessage(Component.literal(
                        "[SCP Inventory] Could not save context interaction: "
                                + result.message())
                        .withStyle(ChatFormatting.RED));
                return;
            }

            player.sendSystemMessage(Component.literal(
                    "[SCP Inventory] Saved context interaction for "
                            + msg.blockId + " with "
                            + variantCount(msg.variantsJson)
                            + " alternate variant(s).")
                    .withStyle(ChatFormatting.GREEN));
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new ContextConfigReloadPacket());
        });
        ctx.get().setPacketHandled(true);
    }

    private static String normalizeVariantInputs(String json) {
        try {
            JsonElement parsed = JsonParser.parseString(normalize(json, "[]"));
            if (!parsed.isJsonArray()) return "[]";
            for (JsonElement element : parsed.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                JsonObject input = object.has("input")
                        && object.get("input").isJsonObject()
                        ? object.getAsJsonObject("input") : new JsonObject();
                boolean legacyE = booleanValue(input, "allowE",
                        booleanValue(object, "allowE", false));
                boolean rightClick = booleanValue(input, "allowRightClick",
                        booleanValue(object, "allowRightClick", false));
                input.addProperty("allowE", false);
                input.addProperty("allowRightClick", rightClick || legacyE);
                object.add("input", input);
                object.remove("allowE");
                object.remove("allowRightClick");
            }
            return parsed.toString();
        } catch (Exception ignored) {
            return "[]";
        }
    }

    private static boolean booleanValue(JsonObject object, String key,
            boolean fallback) {
        try {
            return object.has(key) && !object.get(key).isJsonNull()
                    ? object.get(key).getAsBoolean() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int variantCount(String json) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            return parsed.isJsonArray() ? parsed.getAsJsonArray().size() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String normalize(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
