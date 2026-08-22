package com.bl4ues.scpclassifieddirective.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/**
 * Registers the invisible collision-only block used by decontamination
 * checkpoints. No BlockItem is exposed; the whole structure drops the normal
 * open checkpoint block.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DecontaminationStructureBlocks {
    public static final ResourceLocation COLLISION_ID = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "decontamination_collision");

    private static DecontaminationCollisionBlock collision;

    private DecontaminationStructureBlocks() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, COLLISION_ID, () -> {
            collision = new DecontaminationCollisionBlock();
            return collision;
        });
    }

    public static DecontaminationCollisionBlock collision() {
        DecontaminationCollisionBlock value = collision;
        if (value == null) {
            throw new IllegalStateException(
                    "Decontamination collision block requested before registration");
        }
        return value;
    }
}
