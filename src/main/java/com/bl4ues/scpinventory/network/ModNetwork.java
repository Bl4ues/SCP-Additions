package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.bl4ues.scpadditions.compat.network.NetworkRegistry;
import com.bl4ues.scpadditions.compat.network.PacketDistributor;
import com.bl4ues.scpadditions.compat.network.SimpleChannel;
import net.mcreator.scpadditions.config.ui.ConfigCenterNetwork;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "5";
    private static boolean registered;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ScpInventoryMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private ModNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
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
        CHANNEL.registerMessage(id++, InventoryModuleStatePacket.class, InventoryModuleStatePacket::encode, InventoryModuleStatePacket::decode, InventoryModuleStatePacket::handle);
        CHANNEL.registerMessage(id++, ContextInteractPacket.class, ContextInteractPacket::encode, ContextInteractPacket::decode, ContextInteractPacket::handle);
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
        CHANNEL.registerMessage(id++, CraftingStateSyncPacket.class, CraftingStateSyncPacket::encode, CraftingStateSyncPacket::decode, CraftingStateSyncPacket::handle);
        CHANNEL.registerMessage(id++, RequestCraftingStatePacket.class, RequestCraftingStatePacket::encode, RequestCraftingStatePacket::decode, RequestCraftingStatePacket::handle);
        CHANNEL.registerMessage(id++, CraftingActionPacket.class, CraftingActionPacket::encode, CraftingActionPacket::decode, CraftingActionPacket::handle);
        ConfigCenterNetwork.register(CHANNEL, id);
    }

    public static void syncTo(ServerPlayer player, IScpInventory inventory) {
        if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;
        if (player != null && inventory != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncInventoryPacket(inventory.serializeNBT(player.registryAccess())));
        }
    }

    public static void syncModuleState(ServerPlayer player) {
        if (player == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new InventoryModuleStatePacket(ScpAdditionsModulesConfig.get().inventory.enabled));
    }

    public static void syncModuleState(Iterable<ServerPlayer> players) {
        if (players == null) return;
        for (ServerPlayer player : players) syncModuleState(player);
    }

    public static void showInventoryFull(ServerPlayer player) {
        if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;
        if (player != null && !player.isSpectator()) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new InventoryFullPacket());
        }
    }

    public static void activateUsableItem(ServerPlayer player, int hotbarSlot,
                                          boolean continuousUse, ItemStack stack) {
        activateUsableItem(player, hotbarSlot, -1, continuousUse, stack);
    }

    public static void activateUsableItem(ServerPlayer player, int hotbarSlot,
                                          int sourceSlot, boolean continuousUse,
                                          ItemStack stack) {
        if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;
        if (player != null && !player.isCreative() && !player.isSpectator()) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new UseHotbarItemPacket(hotbarSlot, sourceSlot, continuousUse, stack));
        }
    }
}
