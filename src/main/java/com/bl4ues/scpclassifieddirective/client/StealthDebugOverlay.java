package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.stealth.AdvancedCrouchController;
import com.bl4ues.scpclassifieddirective.stealth.PerceptionFramework;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

/** Individual client-only developer overlay for the local player's stealth scalar. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class StealthDebugOverlay {
    private static final int BACKGROUND = 0xD9080B10;
    private static final int BORDER = 0xFF3A424D;
    private static final int ACCENT = 0xFFC99B18;
    private static final int TEXT = 0xFFF7F8FC;
    private static final int MUTED = 0xFF9CA3AF;

    private StealthDebugOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!StealthDebugClientPreferences.showStealthHud()) return;

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        double value = PerceptionFramework.playerStealthValue(player);
        int light = Math.max(0, Math.min(15,
                minecraft.level.getMaxLocalRawBrightness(player.blockPosition())));
        String posture = posture(player);
        boolean moduleEnabled = AdvancedCrouchController.clientModuleEnabled();

        Component title = ScpFonts.roboto("STEALTH VALUE  "
                + String.format(Locale.ROOT, "%.3f", value));
        Component detail = ScpFonts.roboto(moduleEnabled
                ? posture + "  ·  LIGHT " + light
                : "MODULE OFF");

        int width = Math.max(font.width(title), font.width(detail)) + 20;
        int height = 34;
        int x = 8;
        int y = 8;
        int right = x + width;
        int bottom = y + height;

        graphics.fill(x, y, right, bottom, BACKGROUND);
        graphics.fill(x, y, right, y + 1, BORDER);
        graphics.fill(x, bottom - 1, right, bottom, BORDER);
        graphics.fill(x, y, x + 1, bottom, BORDER);
        graphics.fill(right - 1, y, right, bottom, BORDER);
        graphics.fill(x + 1, y + 1, x + 4, bottom - 1, ACCENT);
        graphics.drawString(font, title, x + 9, y + 7, TEXT, false);
        graphics.drawString(font, detail, x + 9, y + 20, MUTED, false);
    }

    private static String posture(Player player) {
        if (AdvancedCrouchController.isLowCrawling(player)) return "CRAWLING";
        if (player.getPose() == Pose.CROUCHING || player.isCrouching()) {
            return "CROUCHING";
        }
        return "STANDING";
    }
}
