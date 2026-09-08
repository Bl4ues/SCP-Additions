package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraNetworkClientState;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079FacilityMapScreen;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079UiTheme;
import com.bl4ues.scpclassifieddirective.facility.Scp079CameraTravelRules;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/** Contextual LMB travel control shown while hovering a mapped camera room. */
@Mixin(value = Scp079FacilityMapScreen.class, remap = false)
public abstract class Scp079FacilityMapTravelPromptMixin {
    private static final int RIGHT = 36;
    private static final int TOP = 62;
    private static final int HEIGHT = 25;
    private static final int MIN_WIDTH = 190;
    private static final int MAX_WIDTH = 430;
    private static final int KEY_WIDTH = 37;
    private static final float SCALE = 1.01F;

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void scpclassifieddirective$renderTravelPrompt(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!Scp079PlayableClient.active()) return;
        Scp079FacilityMapScreenAccessor access =
                (Scp079FacilityMapScreenAccessor) (Object) this;
        if (access.scpclassifieddirective$isLeaveConfirmationOpen()
                || access.scpclassifieddirective$isFloorMenuOpen()) return;

        FacilityRoomSnapshot target = access.scpclassifieddirective$hoveredRoom();
        if (target == null
                || !Scp079CameraNetworkClientState.hasCamera(target.id())) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Screen screen = (Screen) (Object) this;
        FacilityRoomSnapshot current = FacilityMappingClientState.roomAt(
                Scp079PlayableClient.hostDimension(),
                BlockPos.containing(Scp079PlayableClient.viewPosition()));
        double cost = Scp079CameraTravelRules.displayedCost(
                minecraft.level.getDifficulty(), current, target);

        String action = target.name().isBlank()
                ? "GO TO ROOM"
                : "GO TO: " + target.name().strip().toUpperCase(Locale.ROOT);
        String label = action + " (" + formatCost(cost) + " AP)";
        int labelWidth = Scp079UiTheme.scaledWidth(minecraft.font, label, SCALE);
        int width = Math.max(MIN_WIDTH,
                Math.min(MAX_WIDTH, labelWidth + KEY_WIDTH + 29));
        int x = screen.width - RIGHT - width;

        graphics.fill(x, TOP, x + width, TOP + HEIGHT, 0xD20A1D27);
        border(graphics, x, TOP, width, HEIGHT, 0xFF557F91);

        int keyX = x + width - KEY_WIDTH - 7;
        int keyY = TOP + 3;
        int keyH = HEIGHT - 8;
        graphics.fill(keyX, keyY, keyX + KEY_WIDTH, keyY + keyH,
                0xE8D9EDF2);
        border(graphics, keyX, keyY, KEY_WIDTH, keyH, 0xFFF3FFFF);
        Scp079UiTheme.drawCenteredInControl(graphics, minecraft.font, "LMB",
                keyX + KEY_WIDTH * 0.5F, keyY, keyH, 0.92F, 0xFF142631);

        int available = keyX - x - 12;
        String drawn = fit(label, available, minecraft, SCALE);
        Scp079UiTheme.draw(graphics, minecraft.font, drawn,
                x + 8, TOP + 9, SCALE, Scp079UiTheme.TEXT);
    }

    private static String fit(String value, int available,
            Minecraft minecraft, float scale) {
        if (Scp079UiTheme.scaledWidth(minecraft.font, value, scale)
                <= available) return value;
        String suffix = "...";
        String out = value;
        while (!out.isEmpty()
                && Scp079UiTheme.scaledWidth(minecraft.font,
                out + suffix, scale) > available) {
            out = out.substring(0, out.length() - 1);
        }
        return out.stripTrailing() + suffix;
    }

    private static String formatCost(double cost) {
        double rounded = Math.rint(cost);
        if (Math.abs(cost - rounded) < 0.001D) {
            return Integer.toString((int) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", cost);
    }

    private static void border(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
