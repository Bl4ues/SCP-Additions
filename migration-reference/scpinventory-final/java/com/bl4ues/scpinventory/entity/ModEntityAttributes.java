package com.bl4ues.scpinventory.entity;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityAttributes {
    private ModEntityAttributes() {
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SCP_173.get(), Scp173Entity.createAttributes()
                .add(Attributes.ARMOR, 80.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .build());
        event.put(ModEntities.SCP_131_A.get(), AbstractScp131Entity.createAttributes().build());
        event.put(ModEntities.SCP_131_B.get(), AbstractScp131Entity.createAttributes().build());
    }
}
