package com.bl4ues.scpclassifieddirective.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/**
 * Registers the invisible collision-only part used by the Tesla Gate structure.
 * No BlockItem is registered: players only ever obtain the normal Tesla Gate.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TeslaGateStructureBlocks {
    public static final ResourceLocation COLLISION_ID =
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "tesla_gate_collision");

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
