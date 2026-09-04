package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Button feedback for SCP-079's custom-drawn map and role-exit screens. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class Scp079MapUiAudioClient {
    private Scp079MapUiAudioClient() { }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0 || !Scp079PlayableClient.active()) return;
        if (event.getScreen() instanceof Scp079LeaveRoleScreen) {
            Scp079PlayableAudioClient.playButton();
            return;
        }
        if (!(event.getScreen() instanceof Scp079FacilityMapScreen screen)) return;

        double x = event.getMouseX();
        double y = event.getMouseY();
        // All permanent map buttons and the floor selector live in the top band.
        // The expanded floor list remains centred under it.
        boolean topControls = y >= 20.0D && y <= 66.0D;
        boolean floorList = Math.abs(x - screen.width * 0.5D) <= 150.0D
                && y > 66.0D && y < 260.0D;
        if (topControls || floorList) {
            Scp079PlayableAudioClient.playButton();
        }
    }
}
