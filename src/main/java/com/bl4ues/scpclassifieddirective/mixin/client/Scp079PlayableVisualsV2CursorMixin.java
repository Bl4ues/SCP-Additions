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
     * Inventory opens the room map when the network is available. If Auxiliary
     * Power is offline there is no map to operate, so the same key opens the
     * leave-role confirmation instead. Shift has no SCP-079 control function.
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

        if (Scp079PlayableClient.networkAvailable()) {
            Scp079FacilityMapScreen.open();
        } else {
            Scp079LeaveRoleScreen.open();
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
     * Remove the obsolete Shift cursor/exit hints. Offline local-host mode keeps
     * a discoverable Inventory-key exit hint because no facility map can open.
     */
    @Redirect(method = {"renderLocalHud", "renderCameraHud"},
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/scp079/Scp079PlayableVisualsV2;drawRight(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/Minecraft;Ljava/lang/String;IIFI)V"),
            remap = false)
    private static void scpclassifieddirective$removeShiftHints(
            GuiGraphics graphics, Minecraft minecraft, String value,
            int right, int y, float scale, int color) {
        if (value == null || value.equals("HOLD SHIFT  CURSOR")) return;
        if (value.startsWith("SHIFT + ") && value.endsWith("  LEAVE SCP ROLE")) {
            if (Scp079PlayableClient.networkAvailable()) return;
            String key = value.substring("SHIFT + ".length(),
                    value.length() - "  LEAVE SCP ROLE".length());
            value = "[" + key + "]  LEAVE SCP ROLE";
        }
        int width = Scp079UiTheme.scaledWidth(minecraft.font, value, scale);
        Scp079UiTheme.draw(graphics, minecraft.font, value,
                right - width, y, scale, color);
    }
}
