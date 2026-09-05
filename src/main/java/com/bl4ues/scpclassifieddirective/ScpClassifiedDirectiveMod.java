package com.bl4ues.scpclassifieddirective;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;

import com.bl4ues.scpclassifieddirective.config.Scp714ConfigBootstrap;
import com.bl4ues.scpclassifieddirective.compat.LegacyConfigMigration;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import com.bl4ues.scpclassifieddirective.inventory.config.ScpInventoryConfig;
import com.bl4ues.scpclassifieddirective.data.Scp294DrinkManager;
import com.bl4ues.scpclassifieddirective.data.Scp914RecipeManager;
import com.bl4ues.scpclassifieddirective.data.Scp914SkinManager;
import com.bl4ues.scpclassifieddirective.entity.Scp131Sounds;
import com.bl4ues.scpclassifieddirective.entity.Scp173Sounds;
import com.bl4ues.scpclassifieddirective.entity.Scp173TargetConfig;
import com.bl4ues.scpclassifieddirective.facility.AreaUnderConstructionSignModule;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.TeslaGateTerminalTableModule;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorModule;
import com.bl4ues.scpclassifieddirective.facility.mapping.network.FacilityMappingNetwork;
import com.bl4ues.scpclassifieddirective.facility.UBlocksModule;
import com.bl4ues.scpclassifieddirective.facility.HeavyDoorPowerRelay;
import com.bl4ues.scpclassifieddirective.facility.LeftDoorButtons;
import com.bl4ues.scpclassifieddirective.facility.MirroredDoorButtons;
import com.bl4ues.scpclassifieddirective.network.ScpEntityNetwork;
import com.bl4ues.scpclassifieddirective.network.StealthNetwork;
import com.bl4ues.scpclassifieddirective.scp012.Scp012Module;
import com.bl4ues.scpclassifieddirective.scp1576.Scp1576Module;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Module;
import com.bl4ues.scpclassifieddirective.safezone.SafeZoneSounds;
import com.bl4ues.scpclassifieddirective.safezone.network.SafeZoneNetwork;
import com.bl4ues.scpclassifieddirective.sound.GameplaySounds;
import com.bl4ues.scpclassifieddirective.sound.AchievementSounds;
import com.bl4ues.scpclassifieddirective.vitals.StaminaItemEffectConfig;
import com.bl4ues.scpclassifieddirective.init.MainMenuSounds;
import com.bl4ues.scpclassifieddirective.init.Scp131Items;
import com.bl4ues.scpclassifieddirective.init.Scp714Items;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModTabs;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import com.bl4ues.scpclassifieddirective.init.PlayerVoiceSounds;
import com.bl4ues.scpclassifieddirective.init.Scp106Sounds;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModMobEffects;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModMenus;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModEntities;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModParticleTypes;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import com.bl4ues.scpclassifieddirective.init.UnifiedReaderItems;

import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.AbstractMap;

@Mod(ScpClassifiedDirectiveMod.MODID)
public class ScpClassifiedDirectiveMod {
    public static final Logger LOGGER = LogManager.getLogger(ScpClassifiedDirectiveMod.class);
    public static final String MODID = "scp_classified_directive";

    public ScpClassifiedDirectiveMod() {
        LegacyConfigMigration.migrate();
        MinecraftForge.EVENT_BUS.register(this);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::commonSetup);
        ScpClassifiedDirectiveModSounds.REGISTRY.register(bus);
        MainMenuSounds.REGISTRY.register(bus);
        PlayerVoiceSounds.REGISTRY.register(bus);
        Scp106Sounds.REGISTRY.register(bus);
        Scp131Sounds.REGISTRY.register(bus);
        Scp173Sounds.REGISTRY.register(bus);
        SafeZoneSounds.REGISTRY.register(bus);
        GameplaySounds.REGISTRY.register(bus);
        AchievementSounds.REGISTRY.register(bus);
        ScpClassifiedDirectiveModBlocks.REGISTRY.register(bus);
        ScpClassifiedDirectiveModBlockEntities.REGISTRY.register(bus);
        ScpClassifiedDirectiveModItems.REGISTRY.register(bus);
        Scp714Items.REGISTRY.register(bus);
        Scp714Items.BLOCKS.register(bus);
        Scp012Module.register(bus);
        Scp1576Module.register(bus);
        Scp914Module.register(bus);
        UnifiedReaderItems.REGISTRY.register(bus);
        Scp131Items.REGISTRY.register(bus);
        ScpClassifiedDirectiveModEntities.REGISTRY.register(bus);
        ScpClassifiedDirectiveModParticleTypes.REGISTRY.register(bus);

        ScpClassifiedDirectiveModTabs.REGISTRY.register(bus);
        UBlocksModule.register(bus);
        FacilityModule.register(bus);
        TeslaGateTerminalTableModule.register(bus);
        AreaUnderConstructionSignModule.register(bus);
        CoreRoomElevatorModule.register(bus);
        MirroredDoorButtons.register(bus);
        LeftDoorButtons.register(bus);
        HeavyDoorPowerRelay.register(bus);
        ScpClassifiedDirectiveModMobEffects.REGISTRY.register(bus);
        ScpClassifiedDirectiveModMenus.REGISTRY.register(bus);
        ScpEntityNetwork.register();
        SafeZoneNetwork.register();
        FacilityMappingNetwork.register();
        StealthNetwork.register();
        com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork.register();
        com.bl4ues.scpclassifieddirective.network.ScpRoleSelectorNetwork.register();
        com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork.register();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(this::loadRegistryDependentConfiguration);
    }

    private void loadRegistryDependentConfiguration() {
        ScpClassifiedDirectiveModulesConfig.load();
        Scp714ConfigBootstrap.ensureAccessoryRule();
        ScpInventoryConfig.reloadFromDisk();
        Scp173TargetConfig.load();
        StaminaItemEffectConfig.load();
        Scp294DrinkManager.loadFromConfig();
        Scp914RecipeManager.loadFromConfig();
        Scp914SkinManager.initialize();
    }

    private static final String PROTOCOL_VERSION = "31";
    public static final SimpleChannel PACKET_HANDLER =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(MODID, MODID),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals);
    private static int messageID = 0;

    public static <T> void addNetworkMessage(Class<T> messageType,
            BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder,
            BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
        PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder,
                messageConsumer);
        messageID++;
    }

    private static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>>
            workQueue = new ConcurrentLinkedQueue<>();

    public static void queueServerWork(int tick, Runnable action) {
        workQueue.add(new AbstractMap.SimpleEntry<>(action, tick));
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            List<AbstractMap.SimpleEntry<Runnable, Integer>> actions = new ArrayList<>();
            workQueue.forEach(work -> {
                work.setValue(work.getValue() - 1);
                if (work.getValue() == 0) actions.add(work);
            });
            actions.forEach(e -> e.getKey().run());
            workQueue.removeAll(actions);
        }
    }
}
