package net.mcreator.scpadditions.network;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Appends save-game packets after the core SCP network registrations. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SaveGameNetwork {
    private static boolean registered;

    private SaveGameNetwork() {
    }

    @SubscribeEvent
    public static synchronized void onCommonSetup(FMLCommonSetupEvent event) {
        if (registered) return;
        registered = true;
        ScpAdditionsMod.addNetworkMessage(QuickSavePacket.class,
                QuickSavePacket::encode, QuickSavePacket::decode,
                QuickSavePacket::handle);
        ScpAdditionsMod.addNetworkMessage(SaveStatePacket.class,
                SaveStatePacket::encode, SaveStatePacket::decode,
                SaveStatePacket::handle);
    }
}
