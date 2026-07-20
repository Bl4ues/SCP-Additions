package com.bl4ues.scpadditions.compat.network;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registers every compatibility channel on the NeoForge payload registry. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class LegacyNetworkPayloads {
    private LegacyNetworkPayloads() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        for (SimpleChannel channel : SimpleChannel.channels()) {
            channel.registerPayload(event.registrar(channel.version()));
        }
    }
}
