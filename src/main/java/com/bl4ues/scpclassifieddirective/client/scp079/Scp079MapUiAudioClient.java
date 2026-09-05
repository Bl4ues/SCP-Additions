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
        int width = screen.width;

        // Keep this in sync with the deliberately coarse top controls in the
        // map screen. Room tiles are excluded: requestRoom owns select_2.
        boolean floorSelector = x >= width * 0.5D - 165.0D
                && x <= width * 0.5D + 165.0D
                && y >= 29.0D && y <= 56.0D;
        boolean leaveRole = x >= width - 178.0D && x <= width - 36.0D
                && y >= 29.0D && y <= 56.0D;
        boolean returnHost = Scp079PlayableClient.cameraMode()
                && x >= width - 354.0D && x <= width - 190.0D
                && y >= 29.0D && y <= 56.0D;
        boolean floorMenu = Math.abs(x - width * 0.5D) <= 165.0D
                && y > 56.0D && y < 265.0D;
        if (floorSelector || leaveRole || returnHost || floorMenu) {
            Scp079PlayableAudioClient.playButton();
        }
    }
}
