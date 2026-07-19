package net.mcreator.scpadditions;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.scpadditions.config.Scp714ConfigBootstrap;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import net.mcreator.scpadditions.data.Scp294DrinkManager;
import net.mcreator.scpadditions.data.Scp914RecipeManager;
import net.mcreator.scpadditions.data.Scp914SkinManager;
import net.mcreator.scpadditions.entity.Scp131Sounds;
import net.mcreator.scpadditions.entity.Scp173Sounds;
import net.mcreator.scpadditions.entity.Scp173TargetConfig;
import net.mcreator.scpadditions.facility.FacilityModule;
import net.mcreator.scpadditions.facility.UBlocksModule;
import net.mcreator.scpadditions.facility.HeavyDoorPowerRelay;
import net.mcreator.scpadditions.facility.LeftDoorButtons;
import net.mcreator.scpadditions.facility.MirroredDoorButtons;
import net.mcreator.scpadditions.network.ScpEntityNetwork;
import net.mcreator.scpadditions.scp012.Scp012Module;
import net.mcreator.scpadditions.vitals.StaminaItemEffectConfig;
import net.mcreator.scpadditions.world.features.StructureFeature;
import net.mcreator.scpadditions.init.Scp131Items;
import net.mcreator.scpadditions.init.Scp714Items;
import net.mcreator.scpadditions.init.ScpAdditionsModTabs;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;
import net.mcreator.scpadditions.init.ScpAdditionsModMobEffects;
import net.mcreator.scpadditions.init.ScpAdditionsModMenus;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;
import net.mcreator.scpadditions.init.ScpAdditionsModEntities;
import net.mcreator.scpadditions.init.ScpAdditionsModParticleTypes;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.init.ScpAdditionsModBlockEntities;
import net.mcreator.scpadditions.init.UnifiedReaderItems;

import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.AbstractMap;

@Mod("scp_additions")
public class ScpAdditionsMod {
    public static final Logger LOGGER = LogManager.getLogger(ScpAdditionsMod.class);
    public static final String MODID = "scp_additions";

    public ScpAdditionsMod() {
        MinecraftForge.EVENT_BUS.register(this);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ScpAdditionsModSounds.REGISTRY.register(bus);
        Scp131Sounds.REGISTRY.register(bus);
        Scp173Sounds.REGISTRY.register(bus);
        ScpAdditionsModBlocks.REGISTRY.register(bus);
        ScpAdditionsModBlockEntities.REGISTRY.register(bus);
        ScpAdditionsModItems.REGISTRY.register(bus);
        Scp714Items.REGISTRY.register(bus);
        Scp012Module.register(bus);
        UnifiedReaderItems.REGISTRY.register(bus);
        Scp131Items.REGISTRY.register(bus);
        ScpAdditionsModEntities.REGISTRY.register(bus);
        ScpAdditionsModParticleTypes.REGISTRY.register(bus);

        ScpAdditionsModTabs.REGISTRY.register(bus);
        UBlocksModule.register(bus);
        FacilityModule.register(bus);
        MirroredDoorButtons.register(bus);
        LeftDoorButtons.register(bus);
        HeavyDoorPowerRelay.register(bus);
        StructureFeature.REGISTRY.register(bus);
        ScpAdditionsModMobEffects.REGISTRY.register(bus);
        ScpAdditionsModMenus.REGISTRY.register(bus);
        ScpEntityNetwork.register();
        com.bl4ues.scpinventory.network.ModNetwork.register();

        ScpAdditionsModulesConfig.load();
        Scp714ConfigBootstrap.ensureAccessoryRule();
        ScpInventoryConfig.reload();
        Scp173TargetConfig.load();
        StaminaItemEffectConfig.load();
        Scp294DrinkManager.loadFromConfig();
        Scp914RecipeManager.loadFromConfig();
        Scp914SkinManager.initialize();
    }

    private static final String PROTOCOL_VERSION = "8";
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
        workQueue.add(new AbstractMap.SimpleEntry(action, tick));
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            List<AbstractMap.SimpleEntry<Runnable, Integer>> actions =
                    new ArrayList<>();
            workQueue.forEach(work -> {
                work.setValue(work.getValue() - 1);
                if (work.getValue() == 0) actions.add(work);
            });
            actions.forEach(e -> e.getKey().run());
            workQueue.removeAll(actions);
        }
    }
}
