package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.PacketDistributor;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.HazardSignBlockEntity;

/** Network messages dedicated to the Hazard Sign editor. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HazardSignNetwork {
    private static boolean registered;

    private HazardSignNetwork() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(HazardSignNetwork::register);
    }

    private static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(
                HazardSignOpenScreenPacket.class,
                HazardSignOpenScreenPacket::encode,
                HazardSignOpenScreenPacket::decode,
                HazardSignOpenScreenPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(HazardSignSavePacket.class,
                HazardSignSavePacket::encode,
                HazardSignSavePacket::decode,
                HazardSignSavePacket::handle);
    }

    public static void openEditor(ServerPlayer player,
            HazardSignBlockEntity sign) {
        if (player == null || sign == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new HazardSignOpenScreenPacket(sign.getBlockPos(),
                        sign.hazardId()));
    }
}
