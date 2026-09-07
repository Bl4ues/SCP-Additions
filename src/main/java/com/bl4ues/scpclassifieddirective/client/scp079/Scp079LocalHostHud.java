package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

/**
 * Local-host presentation for playable SCP-079.
 *
 * The physical computer is not a surveillance feed, so it deliberately omits
 * the camera corner brackets and the Auxiliary Power meter. Those elements only
 * become relevant once the operator has entered the facility network.
 */
public final class Scp079LocalHostHud {
    private static final int EDGE_INSET = 34;

    private Scp079LocalHostHud() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int x = EDGE_INSET;
        int y = 33;

        Scp079UiTheme.draw(graphics, minecraft.font, "SCP-079", x, y,
                1.34F, Scp079UiTheme.TEXT);
        Scp079UiTheme.draw(graphics, minecraft.font, "LOCAL HOST", x, y + 21,
                1.24F, Scp079UiTheme.ACCENT);

        FacilityRoomSnapshot room = FacilityMappingClientState.roomAt(
                Scp079PlayableClient.hostDimension(),
                Scp079PlayableClient.hostPos());
        if (room != null) {
            String floor = room.floorShortLabel().isBlank()
                    ? "UNASSIGNED" : room.floorShortLabel();
            Scp079UiTheme.draw(graphics, minecraft.font,
                    floor.toUpperCase(Locale.ROOT), x, y + 48,
                    1.14F, Scp079UiTheme.MUTED);
            if (!room.name().isBlank()) {
                Scp079UiTheme.draw(graphics, minecraft.font,
                        room.name().toUpperCase(Locale.ROOT), x, y + 66,
                        1.22F, Scp079UiTheme.TEXT);
            }
        }

        int right = width - EDGE_INSET;
        String inventory = keyLabel(minecraft.options.keyInventory);
        if (Scp079PlayableClient.networkAvailable()) {
            String action = Scp079BootSequenceClient.completed()
                    ? "OPEN FACILITY MAP" : "INITIALIZE CAMERA NETWORK";
            drawCommand(graphics, minecraft, action, inventory,
                    right, 33, 1.05F, Scp079UiTheme.ACCENT, true);
        } else {
            drawRight(graphics, minecraft, "AUXILIARY POWER OFFLINE",
                    right, 33, 1.05F, Scp079UiTheme.OFFLINE);
        }

        drawCommand(graphics, minecraft, "LEAVE SCP ROLE",
                "SHIFT + " + inventory, right, 62, 1.02F,
                Scp079UiTheme.MUTED, true);
    }

    private static void drawRight(GuiGraphics graphics, Minecraft minecraft,
            String value, int right, int y, float scale, int color) {
        int width = Scp079UiTheme.scaledWidth(minecraft.font, value, scale);
        Scp079UiTheme.draw(graphics, minecraft.font, value,
                right - width, y, scale, color);
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

        float labelTextHeight = minecraft.font.lineHeight * scale;
        float labelY = capY + (capH - labelTextHeight) * 0.5F + 2.0F;
        float keyTextHeight = minecraft.font.lineHeight * keyScale;
        float keyY = capY + (capH - keyTextHeight) * 0.5F + 2.0F;
        int labelW = Scp079UiTheme.scaledWidth(minecraft.font, label, scale);
        int labelRight = capX - 7;

        Scp079UiTheme.draw(graphics, minecraft.font, label,
                labelRight - labelW, labelY, scale, color);
        Scp079UiTheme.drawCentered(graphics, minecraft.font, normalizedKey,
                capX + capW * 0.5F - 1.0F, keyY,
                keyScale, 0xFF071116);
    }

    private static int opaque(int color) {
        return 0xFF000000 | color & 0x00FFFFFF;
    }

    private static String keyLabel(KeyMapping key) {
        return key.getTranslatedKeyMessage().getString()
                .toUpperCase(Locale.ROOT);
    }
}
