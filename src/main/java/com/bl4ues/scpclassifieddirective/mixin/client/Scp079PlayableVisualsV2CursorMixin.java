package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraNavigationClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraNavigationClient.Move;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraNavigationClient.NavigationTarget;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079FacilityMapScreen;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079Keybinds;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079LeaveRoleScreen;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079UiTheme;
import com.bl4ues.scpclassifieddirective.facility.Scp079RoomAbilityManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.Map;

/** Screen-only cursor routing and compact SCP-079 command keycaps. */
@Mixin(Scp079PlayableVisualsV2.class)
public abstract class Scp079PlayableVisualsV2CursorMixin {
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

    /** Removes the old inline/bracket command text before the keycap pass. */
    @Redirect(method = {"renderLocalHud", "renderCameraHud"},
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/scp079/Scp079PlayableVisualsV2;drawRight(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/Minecraft;Ljava/lang/String;IIFI)V"),
            remap = false)
    private static void scpclassifieddirective$replaceLegacyCommandRows(
            GuiGraphics graphics, Minecraft minecraft, String value,
            int right, int y, float scale, int color) {
        if (value == null) return;
        if (value.contains("FACILITY MAP")
                || value.contains("LEAVE SCP ROLE")
                || value.equals("HOLD SHIFT  CURSOR")) {
            return;
        }
        int width = Scp079UiTheme.scaledWidth(minecraft.font, value, scale);
        Scp079UiTheme.draw(graphics, minecraft.font, value,
                right - width, y, scale, color);
    }

    @Inject(method = "renderLocalHud", at = @At("TAIL"), remap = false)
    private static void scpclassifieddirective$renderLocalCommandRows(
            GuiGraphics graphics, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        int right = minecraft.getWindow().getGuiScaledWidth() - 24;
        String inventory = keyLabel(minecraft.options.keyInventory);
        int y = 23;
        if (Scp079PlayableClient.networkAvailable()) {
            drawCommand(graphics, minecraft, "OPEN FACILITY MAP", inventory,
                    right, y, 1.08F, Scp079UiTheme.ACCENT, true);
            y += 20;
        } else {
            y = 43;
        }
        drawCommand(graphics, minecraft, "LEAVE SCP ROLE",
                "SHIFT + " + inventory, right, y, 1.05F,
                Scp079UiTheme.MUTED, true);
    }

    @Inject(method = "renderCameraHud", at = @At("TAIL"), remap = false)
    private static void scpclassifieddirective$renderCameraCommandRows(
            GuiGraphics graphics, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        int right = minecraft.getWindow().getGuiScaledWidth() - 24;
        int y = 22;
        Map<Move, NavigationTarget> targets =
                Scp079CameraNavigationClient.targets();

        y = drawMove(graphics, minecraft, targets.get(Move.FORWARD),
                minecraft.options.keyUp, right, y);
        y = drawMove(graphics, minecraft, targets.get(Move.LEFT),
                minecraft.options.keyLeft, right, y);
        y = drawMove(graphics, minecraft, targets.get(Move.BACK),
                minecraft.options.keyDown, right, y);
        y = drawMove(graphics, minecraft, targets.get(Move.RIGHT),
                minecraft.options.keyRight, right, y);

        String inventory = keyLabel(minecraft.options.keyInventory);
        drawCommand(graphics, minecraft, "OPEN FACILITY MAP", inventory,
                right, y, 1.05F, Scp079UiTheme.TEXT, true);
        y += 19;

        FacilityRoomSnapshot activeRoom = FacilityMappingClientState.roomAt(
                Scp079PlayableClient.hostDimension(),
                BlockPos.containing(Scp079PlayableClient.viewPosition()));
        double blackoutCost = adjustedCost(minecraft,
                Scp079RoomAbilityManager.blackoutBaseCost(activeRoom));
        boolean blackoutAffordable = Scp079PlayableClient.power() + 0.001D
                >= blackoutCost;
        drawCommand(graphics, minecraft,
                "BLACKOUT  " + formatCost(blackoutCost),
                keyLabel(Scp079Keybinds.BLACKOUT), right, y, 1.04F,
                blackoutAffordable ? Scp079UiTheme.TEXT : 0xFF526873,
                blackoutAffordable);
        y += 19;

        boolean lockdownAffordable = Scp079PlayableClient.power() + 0.001D
                >= Scp079RoomAbilityManager.LOCKDOWN_COST;
        drawCommand(graphics, minecraft,
                "LOCKDOWN  " + formatCost(
                        Scp079RoomAbilityManager.LOCKDOWN_COST),
                keyLabel(Scp079Keybinds.LOCKDOWN), right, y, 1.04F,
                lockdownAffordable ? Scp079UiTheme.TEXT : 0xFF526873,
                lockdownAffordable);
        y += 19;

        boolean speakerAvailable = Scp079PlayableClientSpeakerAccessor
                .scpclassifieddirective$speakerAvailable();
        boolean speakerActive = Scp079PlayableClientSpeakerAccessor
                .scpclassifieddirective$speakerActive();
        if (speakerAvailable || speakerActive) {
            String speakerKey = keyLabel(Scp079Keybinds.USE_SPEAKER);
            drawCommand(graphics, minecraft,
                    speakerActive ? "STOP USING SPEAKER" : "USE SPEAKER",
                    speakerKey, right, y, 1.04F,
                    speakerActive ? 0xFFFFC68A : Scp079UiTheme.TEXT, true);
            y += 19;
        }

        drawCommand(graphics, minecraft, "LEAVE SCP ROLE",
                "SHIFT + " + inventory, right, y, 1.02F,
                Scp079UiTheme.MUTED, true);
    }

    private static int drawMove(GuiGraphics graphics, Minecraft minecraft,
            NavigationTarget target, KeyMapping key, int right, int y) {
        boolean enabled = target != null && target.available();
        String label = enabled ? "GO TO: " + target.roomName() : "NO CAMERA";
        int color = enabled ? Scp079UiTheme.TEXT : 0xFF526873;
        drawCommand(graphics, minecraft, label,
                keyLabel(key), right, y, 1.03F, color, enabled);
        return y + 18;
    }

    private static double adjustedCost(Minecraft minecraft, double base) {
        if (minecraft.level == null) return base;
        return base * switch (minecraft.level.getDifficulty()) {
            case PEACEFUL -> 1.50D;
            case EASY -> 1.25D;
            case HARD -> 0.80D;
            default -> 1.00D;
        };
    }

    private static String formatCost(double value) {
        return Math.abs(value - Math.rint(value)) < 0.01D
                ? (int) Math.rint(value) + " AP"
                : String.format(Locale.ROOT, "%.1f AP", value);
    }

    private static void drawCommand(GuiGraphics graphics, Minecraft minecraft,
            String label, String key, int right, int y, float scale,
            int color, boolean enabled) {
        String normalizedKey = key == null || key.isBlank() ? "?" : key;
        float keyScale = Math.max(0.86F, scale - 0.10F);
        int keyTextW = Scp079UiTheme.scaledWidth(minecraft.font,
                normalizedKey, keyScale);
        int capW = keyTextW + 10;
        int capH = Math.max(13,
                Math.round(minecraft.font.lineHeight * keyScale) + 5);
        int capX = right - capW;
        int capY = y - 2;
        int fill = enabled ? opaque(color) : 0xFF526873;
        graphics.fill(capX, capY, right, capY + capH, fill);

        int labelW = Scp079UiTheme.scaledWidth(minecraft.font, label, scale);
        int labelRight = capX - 7;
        Scp079UiTheme.draw(graphics, minecraft.font, label,
                labelRight - labelW, y, scale, color);
        Scp079UiTheme.drawCentered(graphics, minecraft.font, normalizedKey,
                capX + capW * 0.5F, capY + 3, keyScale, 0xFF071116);
    }

    private static int opaque(int color) {
        return 0xFF000000 | color & 0x00FFFFFF;
    }

    private static String keyLabel(KeyMapping key) {
        return key.getTranslatedKeyMessage().getString()
                .toUpperCase(Locale.ROOT);
    }
}
