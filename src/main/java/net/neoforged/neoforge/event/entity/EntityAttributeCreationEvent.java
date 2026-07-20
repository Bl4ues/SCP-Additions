package net.neoforged.neoforge.event.entity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.neoforged.bus.api.Event;
public class EntityAttributeCreationEvent extends Event {
    public <T extends LivingEntity> void put(EntityType<T> type, AttributeSupplier attributes) {
        FabricDefaultAttributeRegistry.register(type, attributes);
    }
}
