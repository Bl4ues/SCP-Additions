package net.mcreator.scpadditions.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public final class ScpAdditionsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScpAdditionsFabric.MOD_BUS.post(new FMLClientSetupEvent());
        FabricClientEventBridge.register();
        com.bl4ues.scpadditions.compat.network.SimpleChannel.registerAllClient();
    }
}
