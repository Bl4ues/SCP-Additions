package net.mcreator.scpadditions.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.mcreator.scpadditions.entity.RoombaEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Roombas still collide with blocks, but entity-to-entity contact is passive.
 * The target already ignores incoming pushes; overriding LivingEntity#doPush
 * removes the missing outbound half so mobs and players can simply overlap it.
 */
@Mixin(value = RoombaEntity.class, remap = false)
public abstract class RoombaNoEntityPushMixin extends PathfinderMob {
    protected RoombaNoEntityPushMixin(
            EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void doPush(Entity other) {
        // Intentionally empty.
    }
}
