package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ScpInventoryMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;

        CHANNEL.registerMessage(id++, InventoryFullPacket.class, InventoryFullPacket::encode, InventoryFullPacket::decode, InventoryFullPacket::handle);
        CHANNEL.registerMessage(id++, SyncInventoryPacket.class, SyncInventoryPacket::encode, SyncInventoryPacket::decode, SyncInventoryPacket::handle);
        CHANNEL.registerMessage(id++, RequestInventorySyncPacket.class, RequestInventorySyncPacket::encode, RequestInventorySyncPacket::decode, RequestInventorySyncPacket::handle);
        CHANNEL.registerMessage(id++, InventoryActionPacket.class, InventoryActionPacket::encode, InventoryActionPacket::decode, InventoryActionPacket::handle);
        CHANNEL.registerMessage(id++, EquipmentActionPacket.class, EquipmentActionPacket::encode, EquipmentActionPacket::decode, EquipmentActionPacket::handle);
        CHANNEL.registerMessage(id++, KeyActionPacket.class, KeyActionPacket::encode, KeyActionPacket::decode, KeyActionPacket::handle);
        CHANNEL.registerMessage(id++, DocumentActionPacket.class, DocumentActionPacket::encode, DocumentActionPacket::decode, DocumentActionPacket::handle);
        CHANNEL.registerMessage(id++, InventoryMovePacket.class, InventoryMovePacket::encode, InventoryMovePacket::decode, InventoryMovePacket::handle);
        CHANNEL.registerMessage(id++, PickupItemPacket.class, PickupItemPacket::encode, PickupItemPacket::decode, PickupItemPacket::handle);
        CHANNEL.registerMessage(id++, UseHotbarItemPacket.class, UseHotbarItemPacket::encode, UseHotbarItemPacket::decode, UseHotbarItemPacket::handle);
        CHANNEL.registerMessage(id++, UsableSessionReturnPacket.class, UsableSessionReturnPacket::encode, UsableSessionReturnPacket::decode, UsableSessionReturnPacket::handle);
        CHANNEL.registerMessage(id++, UsableSessionDropPacket.class, UsableSessionDropPacket::encode, UsableSessionDropPacket::decode, UsableSessionDropPacket::handle);
        CHANNEL.registerMessage(id++, MainUseActionPacket.class, MainUseActionPacket::encode, MainUseActionPacket::decode, MainUseActionPacket::handle);
        CHANNEL.registerMessage(id++, ContextInteractPacket.class, ContextInteractPacket::encode, ContextInteractPacket::decode, ContextInteractPacket::handle);
        CHANNEL.registerMessage(id++, BlinkStatePacket.class, BlinkStatePacket::encode, BlinkStatePacket::decode, BlinkStatePacket::handle);
        CHANNEL.registerMessage(id++, BlinkInputStatePacket.class, BlinkInputStatePacket::encode, BlinkInputStatePacket::decode, BlinkInputStatePacket::handle);
        CHANNEL.registerMessage(id++, ScareSoundPacket.class, ScareSoundPacket::encode, ScareSoundPacket::decode, ScareSoundPacket::handle);
        CHANNEL.registerMessage(id++, ContextConfigSelectPacket.class, ContextConfigSelectPacket::encode, ContextConfigSelectPacket::decode, ContextConfigSelectPacket::handle);
        CHANNEL.registerMessage(id++, ContextConfigOpenPacket.class, ContextConfigOpenPacket::encode, ContextConfigOpenPacket::decode, ContextConfigOpenPacket::handle);
        CHANNEL.registerMessage(id++, ContextConfigSavePacket.class, ContextConfigSavePacket::encode, ContextConfigSavePacket::decode, ContextConfigSavePacket::handle);
        CHANNEL.registerMessage(id++, ContextConfigReloadPacket.class, ContextConfigReloadPacket::encode, ContextConfigReloadPacket::decode, ContextConfigReloadPacket::handle);
        CHANNEL.registerMessage(id++, ContextConfigDeletePacket.class, ContextConfigDeletePacket::encode, ContextConfigDeletePacket::decode, ContextConfigDeletePacket::handle);
        CHANNEL.registerMessage(id++, ItemConfigOpenRequestPacket.class, ItemConfigOpenRequestPacket::encode, ItemConfigOpenRequestPacket::decode, ItemConfigOpenRequestPacket::handle);
        CHANNEL.registerMessage(id++, ItemConfigOpenPacket.class, ItemConfigOpenPacket::encode, ItemConfigOpenPacket::decode, ItemConfigOpenPacket::handle);
        CHANNEL.registerMessage(id++, ItemConfigSavePacket.class, ItemConfigSavePacket::encode, ItemConfigSavePacket::decode, ItemConfigSavePacket::handle);
        CHANNEL.registerMessage(id++, ItemConfigReloadPacket.class, ItemConfigReloadPacket::encode, ItemConfigReloadPacket::decode, ItemConfigReloadPacket::handle);
        CHANNEL.registerMessage(id++, ItemConfigDeletePacket.class, ItemConfigDeletePacket::encode, ItemConfigDeletePacket::decode, ItemConfigDeletePacket::handle);
        CHANNEL.registerMessage(id++, Scp131NoticePacket.class, Scp131NoticePacket::encode, Scp131NoticePacket::decode, Scp131NoticePacket::handle);
        CHANNEL.registerMessage(id++, Scp131StopPacket.class, Scp131StopPacket::encode, Scp131StopPacket::decode, Scp131StopPacket::handle);
        CHANNEL.registerMessage(id, HorrorSprintInputPacket.class, HorrorSprintInputPacket::encode, HorrorSprintInputPacket::decode, HorrorSprintInputPacket::handle);
    }

    public static void syncTo(ServerPlayer player, IScpInventory inventory) {
        if (player == null || inventory == null) {
            return;
        }

        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncInventoryPacket(inventory.serializeNBT())
        );
    }

    public static void showInventoryFull(ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }

        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new InventoryFullPacket());
    }

    public static void showScp131Notice(ServerPlayer player, boolean following) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Scp131NoticePacket(following));
    }

    public static void activateUsableItem(ServerPlayer player, int hotbarSlot, boolean continuousUse, ItemStack stack) {
        activateUsableItem(player, hotbarSlot, -1, continuousUse, stack);
    }

    public static void activateUsableItem(ServerPlayer player, int hotbarSlot, int sourceSlot, boolean continuousUse, ItemStack stack) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }

        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new UseHotbarItemPacket(hotbarSlot, sourceSlot, continuousUse, stack));
    }
}

class HorrorSprintInputPacket {
    private final boolean sprinting;

    HorrorSprintInputPacket(boolean sprinting) {
        this.sprinting = sprinting;
    }

    static void encode(HorrorSprintInputPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.sprinting);
    }

    static HorrorSprintInputPacket decode(FriendlyByteBuf buf) {
        return new HorrorSprintInputPacket(buf.readBoolean());
    }

    static void handle(HorrorSprintInputPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                HorrorSprintServerState.setSprinting(player, msg.sprinting);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID, value = Dist.CLIENT)
final class Scp131StopClientInput {
    private static final int HOLD_TICKS = 20;
    private static int heldTicks;
    private static boolean sent;

    private Scp131StopClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean down = false;
        if (player != null && minecraft.level != null && minecraft.screen == null && !player.isCreative() && !player.isSpectator()) {
            down = com.mojang.blaze3d.platform.InputConstants.isKeyDown(minecraft.getWindow().getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_G);
        }
        if (!down) {
            heldTicks = 0;
            sent = false;
            return;
        }
        heldTicks++;
        if (!sent && heldTicks >= HOLD_TICKS) {
            ModNetwork.CHANNEL.sendToServer(new Scp131StopPacket());
            sent = true;
        }
    }
}

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID, value = Dist.CLIENT)
final class HorrorSprintClientInput {
    private static boolean lastSprinting;

    private HorrorSprintClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean sprinting = false;

        if (player != null && minecraft.level != null && minecraft.screen == null && !player.isCreative() && !player.isSpectator()) {
            sprinting = minecraft.options.keySprint.isDown()
                    && player.input.forwardImpulse > 0.0F
                    && !player.isCrouching()
                    && !player.isPassenger()
                    && !player.getAbilities().flying;
        }

        if (sprinting != lastSprinting) {
            ModNetwork.CHANNEL.sendToServer(new HorrorSprintInputPacket(sprinting));
            lastSprinting = sprinting;
        }
    }
}

final class HorrorSprintServerState {
    private static final Map<UUID, Boolean> SPRINTING = new HashMap<>();

    private HorrorSprintServerState() {
    }

    static void setSprinting(ServerPlayer player, boolean sprinting) {
        if (player == null) {
            return;
        }

        if (sprinting) {
            SPRINTING.put(player.getUUID(), true);
        } else {
            SPRINTING.remove(player.getUUID());
        }
    }

    static boolean isSprinting(ServerPlayer player) {
        return player != null && Boolean.TRUE.equals(SPRINTING.get(player.getUUID()));
    }

    static void clear(ServerPlayer player) {
        if (player != null) {
            SPRINTING.remove(player.getUUID());
        }
    }
}

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
final class HorrorSprintMovementHandler {
    private static final double VANILLA_WALK_SPEED = 0.100D;
    private static final double HORROR_WALK_SPEED = 0.055D;
    private static final double HORROR_SPRINT_BASE_SPEED = 0.110D;
    private static final double EPSILON = 0.0001D;

    private HorrorSprintMovementHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            HorrorSprintServerState.clear(player);
            if (player.isSprinting()) {
                player.setSprinting(false);
            }
            applyMovementSpeed(player, VANILLA_WALK_SPEED);
            return;
        }

        boolean sprinting = HorrorSprintServerState.isSprinting(player) && canUseHorrorSprint(player);
        if (player.isSprinting() != sprinting) {
            player.setSprinting(sprinting);
        }
        applyMovementSpeed(player, sprinting ? HORROR_SPRINT_BASE_SPEED : HORROR_WALK_SPEED);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HorrorSprintServerState.clear(player);
        }
    }

    private static boolean canUseHorrorSprint(ServerPlayer player) {
        return player != null
                && !player.isCrouching()
                && !player.isPassenger()
                && !player.isFallFlying()
                && !player.getAbilities().flying
                && player.getFoodData().getFoodLevel() > 6;
    }

    private static void applyMovementSpeed(ServerPlayer player, double speed) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null && Math.abs(movementSpeed.getBaseValue() - speed) > EPSILON) {
            movementSpeed.setBaseValue(speed);
        }
    }
}
