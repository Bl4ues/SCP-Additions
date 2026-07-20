package net.mcreator.scpadditions.block;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.ForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * Registers the invisible collision-only part used by the Tesla Gate structure.
 * No BlockItem is registered: players only ever obtain the normal Tesla Gate.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TeslaGateStructureBlocks {
    public static final ResourceLocation COLLISION_ID =
            ResourceLocation.fromNamespaceAndPath(ScpAdditionsMod.MODID, "tesla_gate_collision");

    private static TeslaGateCollisionBlock collision;

    private TeslaGateStructureBlocks() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, COLLISION_ID, () -> {
            collision = new TeslaGateCollisionBlock();
            return collision;
        });
    }

    public static TeslaGateCollisionBlock collision() {
        TeslaGateCollisionBlock value = collision;
        if (value == null) {
            throw new IllegalStateException("Tesla Gate collision block was requested before block registration");
        }
        return value;
    }
}
