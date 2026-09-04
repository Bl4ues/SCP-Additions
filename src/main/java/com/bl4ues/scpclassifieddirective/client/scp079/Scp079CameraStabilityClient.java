package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Final render-frame correction for SCP-079's detached camera rig.
 *
 * The playable controller authors the rig directly every render frame. Camera#setup,
 * however, still interpolates the camera entity from its old transform and applies
 * the entity eye height. If those channels are left stale, the view oscillates
 * between the previous/current transforms and the nominal camera point is shifted
 * vertically. That is especially visible with shaderpacks because temporal effects
 * amplify tiny camera discontinuities.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079CameraStabilityClient {
    private Scp079CameraStabilityClient() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !Scp079PlayableClient.active()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.getCameraEntity() instanceof ArmorStand rig)) return;

        // Scp079PlayableClient places the rig at the desired camera EYE point.
        // Convert that point to the ArmorStand base position before Camera adds
        // getEyeHeight() again.
        double eyeOffset = rig.getEyeY() - rig.getY();
        rig.setPos(rig.getX(), rig.getY() - eyeOffset, rig.getZ());

        float yaw = rig.getYRot();
        float pitch = rig.getXRot();

        // Pin every interpolation channel to this exact render-frame transform.
        // The rig is not a normally ticking world entity, so interpolation against
        // stale xOld/xo and rotation values is both unnecessary and visibly wrong.
        rig.setOldPosAndRot();
        rig.yHeadRot = yaw;
        rig.yHeadRotO = yaw;
        rig.yBodyRot = yaw;
        rig.yBodyRotO = yaw;
        rig.setYRot(yaw);
        rig.setXRot(pitch);
        rig.yRotO = yaw;
        rig.xRotO = pitch;
    }
}
