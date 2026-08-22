package com.bl4ues.scpclassifieddirective.inventory.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DocumentNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "documents"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static boolean registered;

    private DocumentNetwork() {
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(DocumentNetwork::register);
    }

    private static synchronized void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, DocumentSavePacket.class,
                DocumentSavePacket::encode, DocumentSavePacket::decode,
                DocumentSavePacket::handle);
    }
}
