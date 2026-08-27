package com.bl4ues.scpclassifieddirective.block;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Registers invisible helper blocks owned exclusively by Decontamination. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DecontaminationStructureBlocks {
    public static final ResourceLocation COLLISION_ID = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "decontamination_collision");
    public static final ResourceLocation LIGHT_ID = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "decontamination_light");

    private static DecontaminationCollisionBlock collision;
    private static DecontaminationLightBlock light;

    private DecontaminationStructureBlocks() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, COLLISION_ID, () -> {
            collision = new DecontaminationCollisionBlock();
            return collision;
        });
        event.register(ForgeRegistries.Keys.BLOCKS, LIGHT_ID, () -> {
            light = new DecontaminationLightBlock();
            return light;
        });
    }

    public static DecontaminationCollisionBlock collision() {
        if (collision == null) {
            throw new IllegalStateException(
                    "Decontamination collision block requested before registration");
        }
        return collision;
    }

    public static DecontaminationLightBlock light() {
        if (light == null) {
            throw new IllegalStateException(
                    "Decontamination light block requested before registration");
        }
        return light;
    }
}
