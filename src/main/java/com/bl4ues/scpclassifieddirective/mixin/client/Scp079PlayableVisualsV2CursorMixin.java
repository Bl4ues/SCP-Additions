package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079FacilityMapScreen;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079LeaveRoleScreen;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079UiTheme;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SCP-079 uses direct mouse-look whenever no Screen is open. A visible cursor is
 * reserved for the surveillance map and the explicit leave-role confirmation.
 */
@Mixin(Scp079PlayableVisualsV2.class)
public abstract class Scp079PlayableVisualsV2CursorMixin {
    /**
     * Inventory opens the room map when the network is available. Holding Shift
     * while pressing Inventory always opens the leave-role confirmation, so the
     * exit path remains explicit even while SCP-079 has full network access.
     * With Auxiliary Power offline, Inventory alone still opens the exit screen.
     */
    @Inject(method = "handleInventoryKey", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$screenOnlyCursorRouting(
            TickEvent.ClientTickEvent event, CallbackInfo ci) {
        ci.cancel();
        if (event.phase != TickEvent.Phase.START
                || !Scp079PlayableClient.active()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;

        boolean requested = false;
        while (minecraft.options.keyInventory.consumeClick()) requested = true;
        if (!requested) return;

        if (minecraft.options.keyShift.isDown()
                || !Scp079PlayableClient.networkAvailable()) {
            Scp079LeaveRoleScreen.open();
        } else {
            Scp079FacilityMapScreen.open();
        }
    }

    /** Feed interaction always resolves from the screen centre/crosshair. */
    @Redirect(method = "pointer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;isDown()Z"),
            remap = false)
    private static boolean scpclassifieddirective$neverUseFreeCursor(
            KeyMapping keyMapping) {
        return false;
    }

    /**
     * Shift no longer releases a free feed cursor, but it remains available as
     * the modifier for the leave-role shortcut. Suppress only the obsolete
     * cursor hint and keep the exit hint visible in both local and camera modes.
     */
    @Redirect(method = {"renderLocalHud", "renderCameraHud"},
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/scp079/Scp079PlayableVisualsV2;drawRight(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/Minecraft;Ljava/lang/String;IIFI)V"),
            remap = false)
    private static void scpclassifieddirective$removeOnlyCursorHint(
            GuiGraphics graphics, Minecraft minecraft, String value,
            int right, int y, float scale, int color) {
        if (value == null || value.equals("HOLD SHIFT  CURSOR")) return;
        int width = Scp079UiTheme.scaledWidth(minecraft.font, value, scale);
        Scp079UiTheme.draw(graphics, minecraft.font, value,
                right - width, y, scale, color);
    }
}
