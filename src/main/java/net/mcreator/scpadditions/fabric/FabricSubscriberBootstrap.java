package net.mcreator.scpadditions.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SimpleEventBus;

final class FabricSubscriberBootstrap {
    private static SimpleEventBus modBus;
    private FabricSubscriberBootstrap() {}
    static void registerAll(SimpleEventBus bus) {
        modBus = bus;
        register("com.bl4ues.scpadditions.compat.LegacyClientTickBridge", EventBusSubscriber.Bus.GAME, true);
        register("com.bl4ues.scpadditions.compat.LegacyGameTickBridge", EventBusSubscriber.Bus.GAME, false);
        register("com.bl4ues.scpadditions.compat.network.LegacyNetworkPayloads", EventBusSubscriber.Bus.MOD, false);
        register("com.bl4ues.scpinventory.client.ClientGameplayEvents", EventBusSubscriber.Bus.GAME, true);
        register("com.bl4ues.scpinventory.client.ClientKeyHandler", EventBusSubscriber.Bus.GAME, true);
        register("com.bl4ues.scpinventory.client.ClientModEvents", EventBusSubscriber.Bus.MOD, true);
        register("com.bl4ues.scpinventory.client.ContextConfigClientEvents", EventBusSubscriber.Bus.GAME, true);
        register("com.bl4ues.scpinventory.client.ContextPromptClickGuard", EventBusSubscriber.Bus.GAME, true);
        register("com.bl4ues.scpinventory.client.CraftingInputHandler", EventBusSubscriber.Bus.GAME, true);
        register("com.bl4ues.scpinventory.client.InventoryUiSessionEvents", EventBusSubscriber.Bus.GAME, true);
        register("com.bl4ues.scpinventory.client.PickupPromptWorldEvents", EventBusSubscriber.Bus.GAME, true);
        register("com.bl4ues.scpinventory.client.ShiftClickEquipHandler", EventBusSubscriber.Bus.GAME, true);
        register("com.bl4ues.scpinventory.client.UsableHotbarSessionClient", EventBusSubscriber.Bus.GAME, true);
        register("com.bl4ues.scpinventory.commands.ScpInventoryCommands", EventBusSubscriber.Bus.GAME, false);
        register("com.bl4ues.scpinventory.context.ContextConfigCommands", EventBusSubscriber.Bus.GAME, false);
        register("com.bl4ues.scpinventory.context.ContextConfigManager", EventBusSubscriber.Bus.GAME, false);
        register("com.bl4ues.scpinventory.context.ContextEntityConfigManager", EventBusSubscriber.Bus.GAME, false);
        register("com.bl4ues.scpinventory.crafting.ScpCraftingKnowledgeEvents", EventBusSubscriber.Bus.GAME, false);
        register("com.bl4ues.scpinventory.event.ScpInventoryMaintenanceEvents", EventBusSubscriber.Bus.GAME, false);
        register("com.bl4ues.scpinventory.event.ScpInventoryUsableOutputGuardEvents", EventBusSubscriber.Bus.GAME, false);
        register("com.bl4ues.scpinventory.events.BlockPickupHandler", EventBusSubscriber.Bus.GAME, false);
        register("com.bl4ues.scpinventory.events.InventoryModuleStateEvents", EventBusSubscriber.Bus.GAME, false);
        register("com.bl4ues.scpinventory.events.VanillaMirrorSyncHandler", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.block.TeslaGateStructureBlocks", EventBusSubscriber.Bus.MOD, false);
        register("net.mcreator.scpadditions.block.TeslaGateStructureEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.client.AudioMufflingClient", EventBusSubscriber.Bus.GAME, true);
        register("net.mcreator.scpadditions.client.BlinkClientEvents", EventBusSubscriber.Bus.GAME, true);
        register("net.mcreator.scpadditions.client.EquipmentProgressClientEvents", EventBusSubscriber.Bus.GAME, true);
        register("net.mcreator.scpadditions.client.GameplayItemTooltipEvents", EventBusSubscriber.Bus.GAME, true);
        register("net.mcreator.scpadditions.client.HazmatSuitClientEvents", EventBusSubscriber.Bus.GAME, true);
        register("net.mcreator.scpadditions.client.KeycardReaderClientInteractionEvents", EventBusSubscriber.Bus.GAME, true);
        register("net.mcreator.scpadditions.client.Scp012AudioClient", EventBusSubscriber.Bus.GAME, true);
        register("net.mcreator.scpadditions.client.Scp012ClientEvents", EventBusSubscriber.Bus.GAME, true);
        register("net.mcreator.scpadditions.client.Scp131ClientEvents", EventBusSubscriber.Bus.GAME, true);
        register("net.mcreator.scpadditions.client.Scp131LoopSoundEvents", EventBusSubscriber.Bus.GAME, true);
        register("net.mcreator.scpadditions.client.Scp173ClientModEvents", EventBusSubscriber.Bus.MOD, true);
        register("net.mcreator.scpadditions.client.Scp714MusicClient", EventBusSubscriber.Bus.GAME, true);
        register("net.mcreator.scpadditions.client.Scp914SkinRenderEvents", EventBusSubscriber.Bus.GAME, true);
        register("net.mcreator.scpadditions.client.UnityConfigurationUiEvents", EventBusSubscriber.Bus.GAME, true);
        register("net.mcreator.scpadditions.client.color.Scp294CupColorHandler", EventBusSubscriber.Bus.MOD, true);
        register("net.mcreator.scpadditions.config.ScpAdditionsReloadCommand", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.config.ui.ConfigCenterClient", EventBusSubscriber.Bus.MOD, true);
        register("net.mcreator.scpadditions.config.ui.ConfigCenterCommand", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.effect.EyeSoreEffectEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.effect.HazmatExternalEffectEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.effect.Scp714ExposureManager", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.equipment.HazmatSuitEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.event.BlinkWatcherAggroEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.event.Scp173DurabilityEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.event.Scp173MovementScrapeEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.event.Scp173PostKillDespawnEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.event.Scp173SpawnEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.event.Scp173TargetRecoveryEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.event.TeslaGateSynchronizationEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.facility.CornerWallDetailPlacementEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.facility.DoorButtonIndependentInteractionEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.facility.DoorButtonPlacementEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.facility.FacilityBlockEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.facility.FacilityBlockMiningEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.facility.FacilityClientRenderEvents", EventBusSubscriber.Bus.MOD, true);
        register("net.mcreator.scpadditions.facility.HeavyDoorAnimationTiming", EventBusSubscriber.Bus.MOD, false);
        register("net.mcreator.scpadditions.facility.HeavyDoorPowerRelay", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.facility.RaisedFacilityPlacementEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.facility.Scp079FacilityThreatEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.facility.UBlocksClientEvents", EventBusSubscriber.Bus.MOD, true);
        register("net.mcreator.scpadditions.handler.Scp294DrinkHandler", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.init.ScpAdditionsModEntities", EventBusSubscriber.Bus.MOD, false);
        register("net.mcreator.scpadditions.init.ScpAdditionsModEntityRenderers", EventBusSubscriber.Bus.MOD, true);
        register("net.mcreator.scpadditions.init.ScpAdditionsModScreens", EventBusSubscriber.Bus.MOD, true);
        register("net.mcreator.scpadditions.inventory.ScpInventoryIntegration", EventBusSubscriber.Bus.MOD, false);
        register("net.mcreator.scpadditions.keycard.KeycardReaderInteractionEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.network.Scp294GuiButtonMessage", EventBusSubscriber.Bus.MOD, false);
        register("net.mcreator.scpadditions.network.Scp294GuiSlotMessage", EventBusSubscriber.Bus.MOD, false);
        register("net.mcreator.scpadditions.network.Scp914GuiButtonMessage", EventBusSubscriber.Bus.MOD, false);
        register("net.mcreator.scpadditions.network.ScpAdditionsModVariables", EventBusSubscriber.Bus.MOD, false);
        register("net.mcreator.scpadditions.network.TeslaTerminalButtonMessage", EventBusSubscriber.Bus.MOD, false);
        register("net.mcreator.scpadditions.procedures.BloodType1Procedure", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.procedures.SCPAdditionsAchievementProcedure", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.procedures.Scp330CandyClear2Procedure", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.procedures.Scp330CandyClearProcedure", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.procedures.Scp9141to1PlayerReset2Procedure", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.procedures.Scp9141to1PlayerResetProcedure", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.procedures.Scp914RefiningProcedure", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.scp012.Scp012BleedingEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.scp012.Scp012Commands", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.scp012.Scp012InfluenceEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.vitals.HorrorMovementEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.vitals.HorrorMovementNetwork", EventBusSubscriber.Bus.MOD, false);
        register("net.mcreator.scpadditions.vitals.PlayerStaminaEvents", EventBusSubscriber.Bus.GAME, false);
        register("net.mcreator.scpadditions.vitals.client.ClientVitalsEvents", EventBusSubscriber.Bus.GAME, true);
        register("net.mcreator.scpadditions.vitals.client.ClientVitalsModEvents", EventBusSubscriber.Bus.MOD, true);
        register("net.mcreator.scpadditions.vitals.client.HorrorMovementClientEvents", EventBusSubscriber.Bus.GAME, true);
        register("net.mcreator.scpadditions.world.inventory.Scp294GuiMenu", EventBusSubscriber.Bus.GAME, false);
    }
    private static void register(String name, EventBusSubscriber.Bus bus, boolean clientOnly) {
        if (clientOnly && FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
        try {
            Class<?> type = Class.forName(name, true, FabricSubscriberBootstrap.class.getClassLoader());
            (bus == EventBusSubscriber.Bus.MOD ? modBus : NeoForge.EVENT_BUS).register(type);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Missing Fabric subscriber " + name, exception);
        }
    }
}
