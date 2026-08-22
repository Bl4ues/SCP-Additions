package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/**
 * The maul's authored camera and shake are first-person presentation effects.
 * Restore Camera.setup's untouched detached angles after all other handlers so
 * third-person remains a normal free inspection camera during the pin.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class Scp939ThirdPersonCameraGuard {
    private Scp939ThirdPersonCameraGuard() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void keepThirdPersonFree(
            ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Scp939ClientState.pinned()
                || minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        event.setYaw(event.getCamera().getYRot());
        event.setPitch(event.getCamera().getXRot());
        event.setRoll(0.0F);
    }
}
