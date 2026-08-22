package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/**
 * Keeps the detached live-feed camera rig's view rotation locked to the orbit
 * rotation calculated by {@link MineZeroSpectateClient}.
 *
 * Camera#setup reads LivingEntity#getViewYRot, which is based on yHeadRot rather
 * than Entity#getYRot. The detached ArmorStand is never ticked as a normal world
 * entity, so changing only its body yaw makes the camera move around the target
 * while continuing to look in an old direction. Synchronizing both current and
 * previous head/body rotations removes that decoupling and its interpolation
 * jitter without touching ordinary armor stands.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DeathSpectateCameraRigEvents {
    private DeathSpectateCameraRigEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START
                || !MineZeroSpectateClient.active()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.getCameraEntity() instanceof ArmorStand rig)) {
            return;
        }

        float yaw = rig.getYRot();
        float pitch = rig.getXRot();

        // Camera#setup interpolates these fields. Keep old/current values equal
        // because this rig is already positioned and rotated at render cadence.
        rig.yRotO = yaw;
        rig.xRotO = pitch;
        rig.yHeadRot = yaw;
        rig.yHeadRotO = yaw;
        rig.yBodyRot = yaw;
        rig.yBodyRotO = yaw;
    }
}
