package net.mcreator.scpadditions.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Removes only the upward impulse produced by an actual jump event. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NoJumpEntityEvents {
    private NoJumpEntityEvents() {
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Scp173Entity)
                && !(entity instanceof Scp106Entity)
                && !(entity instanceof AbstractScp131Entity)
                && !(entity instanceof RoombaEntity)) {
            return;
        }

        Vec3 movement = entity.getDeltaMovement();
        if (movement.y > 0.0D) {
            entity.setDeltaMovement(movement.x, 0.0D, movement.z);
            entity.hasImpulse = true;
        }
    }
}
