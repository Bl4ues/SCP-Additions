package net.mcreator.scpadditions.fabric;

import net.fabricmc.api.ModInitializer;
import net.neoforged.bus.api.SimpleEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.init.ScpAdditionsModTabs;

public final class ScpAdditionsFabric implements ModInitializer {
    public static final SimpleEventBus MOD_BUS = new SimpleEventBus();
    @Override
    public void onInitialize() {
        ScpAdditionsModGameRules.bootstrap();
        new ScpAdditionsMod(MOD_BUS);
        ScpAdditionsModTabs.registerFabricEntries();
        FabricSubscriberBootstrap.registerAll(MOD_BUS);
        MOD_BUS.post(new RegisterEvent());
        MOD_BUS.post(new EntityAttributeCreationEvent());
        MOD_BUS.post(new FMLCommonSetupEvent());
        FabricGameEventBridge.register();
        com.bl4ues.scpadditions.compat.network.SimpleChannel.registerAllCommon();
    }
}
