package com.bl4ues.scpinventory.events;

import net.neoforged.fml.common.EventBusSubscriber;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.capability.ScpInventoryProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = "scp_additions")
public class CapabilityEvents {

    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("scpinventory", "scp_inventory");

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ID, new ScpInventoryProvider());
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {

        event.getOriginal().reviveCaps();

        event.getOriginal().getCapability(ScpInventoryCapability.INSTANCE).ifPresent(oldCap -> {

            event.getEntity().getCapability(ScpInventoryCapability.INSTANCE).ifPresent(newCap -> {

                newCap.deserializeNBT(oldCap.serializeNBT());

            });

        });

        event.getOriginal().invalidateCaps();
    }
}