package net.mcreator.scpadditions.inventory;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.capability.ScpInventoryProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

public final class ScpInventoryEvents {
    private static final ResourceLocation ID =
            new ResourceLocation(ScpAdditionsMod.MODID, "scp_inventory");

    private ScpInventoryEvents() {
    }

    @Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.register(IScpInventory.class);
        }
    }

    @Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeBus {
        private ForgeBus() {
        }

        @SubscribeEvent
        public static void attachPlayerCapability(AttachCapabilitiesEvent<Entity> event) {
            if (!(event.getObject() instanceof Player)) return;
            ScpInventoryProvider provider = new ScpInventoryProvider();
            event.addCapability(ID, provider);
            event.addListener(provider::invalidate);
        }

        @SubscribeEvent
        public static void clonePlayer(PlayerEvent.Clone event) {
            event.getOriginal().reviveCaps();
            event.getOriginal().getCapability(ScpInventoryCapability.INSTANCE).ifPresent(oldInventory ->
                    event.getEntity().getCapability(ScpInventoryCapability.INSTANCE).ifPresent(newInventory ->
                            newInventory.deserializeNBT(oldInventory.serializeNBT())));
            event.getOriginal().invalidateCaps();
        }
    }
}
