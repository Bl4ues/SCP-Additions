package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.mixin.client.Scp079FacilityMapScreenAccessor;
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

        if (event.getScreen() instanceof Scp079LeaveRoleScreen screen) {
            double x = Scp079CrtPostProcessor.logicalX(event.getMouseX(),
                    event.getMouseY(), screen.width, screen.height);
            double y = Scp079CrtPostProcessor.logicalY(event.getMouseX(),
                    event.getMouseY(), screen.width, screen.height);
            if (leaveScreenButton(screen, x, y)) {
                Scp079PlayableAudioClient.playButton();
            }
            return;
        }
        if (!(event.getScreen() instanceof Scp079FacilityMapScreen screen)) return;

        double x = Scp079CrtPostProcessor.logicalX(event.getMouseX(),
                event.getMouseY(), screen.width, screen.height);
        double y = Scp079CrtPostProcessor.logicalY(event.getMouseX(),
                event.getMouseY(), screen.width, screen.height);
        Scp079FacilityMapScreenAccessor accessor =
                (Scp079FacilityMapScreenAccessor) (Object) screen;
        if (accessor.scpclassifieddirective$isLeaveConfirmationOpen()) {
            if (mapConfirmationButton(screen, x, y)) {
                Scp079PlayableAudioClient.playButton();
            }
            return;
        }

        int width = screen.width;
        boolean floorSelector = x >= width * 0.5D - 165.0D
                && x <= width * 0.5D + 165.0D
                && y >= 29.0D && y <= 56.0D;
        boolean leaveRole = x >= width - 178.0D && x <= width - 36.0D
                && y >= 29.0D && y <= 56.0D;
        boolean returnHost = Scp079PlayableClient.cameraMode()
                && x >= width - 354.0D && x <= width - 190.0D
                && y >= 29.0D && y <= 56.0D;
        boolean floorMenu = accessor.scpclassifieddirective$isFloorMenuOpen()
                && Math.abs(x - width * 0.5D) <= 165.0D
                && y > 56.0D && y < 265.0D;
        if (floorSelector || leaveRole || returnHost || floorMenu) {
            Scp079PlayableAudioClient.playButton();
        }
        if (returnHost) Scp079PlayableAudioClient.playDisplaySwitch();
    }

    private static boolean leaveScreenButton(Scp079LeaveRoleScreen screen,
            double mouseX, double mouseY) {
        int w = Math.min(370, screen.width - 56);
        int h = 142;
        int x = (screen.width - w) / 2;
        int y = (screen.height - h) / 2;
        int gap = 12;
        int buttonW = (w - 44 - gap) / 2;
        int buttonY = y + h - 43;
        int cancelX = x + 22;
        int leaveX = cancelX + buttonW + gap;
        return inside(mouseX, mouseY, cancelX, buttonY, buttonW, 29)
                || inside(mouseX, mouseY, leaveX, buttonY, buttonW, 29);
    }

    private static boolean mapConfirmationButton(Scp079FacilityMapScreen screen,
            double mouseX, double mouseY) {
        int w = Math.min(350, screen.width - 36);
        int h = 132;
        int x = (screen.width - w) / 2;
        int y = (screen.height - h) / 2;
        int gap = 10;
        int buttonW = (w - 38 - gap) / 2;
        int buttonY = y + h - 40;
        int cancelX = x + 19;
        int leaveX = cancelX + buttonW + gap;
        return inside(mouseX, mouseY, cancelX, buttonY, buttonW, 27)
                || inside(mouseX, mouseY, leaveX, buttonY, buttonW, 27);
    }

    private static boolean inside(double mouseX, double mouseY,
            int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w
                && mouseY >= y && mouseY < y + h;
    }
}
