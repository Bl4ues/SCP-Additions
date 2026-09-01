package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

/** Small client-only diagnostic overlay for the authoritative SCP-426 exposure. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class Scp426ExposureDebugHud {
    public static final double MAX_EXPOSURE = 1200.0D;

    private static volatile double exposureSeconds;

    private Scp426ExposureDebugHud() {
    }

    public static void updateExposure(double exposure) {
        exposureSeconds = Math.max(0.0D, Math.min(MAX_EXPOSURE, exposure));
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        if (!StealthDebugClientPreferences.showScp426ExposureHud()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        double exposure = exposureSeconds;
        int percent = (int) Math.round((exposure / MAX_EXPOSURE) * 100.0D);
        String tier = tier(exposure);

        Component title = ScpFonts.roboto("SCP-426 EXPOSURE");
        Component value = ScpFonts.roboto(String.format(Locale.ROOT,
                "%.0fs / %.0fs  %d%%  %s", exposure, MAX_EXPOSURE, percent, tier));

        int width = Math.max(font.width(title), font.width(value)) + 14;
        int height = 30;
        int x = event.getWindow().getGuiScaledWidth() - width - 8;
        int y = 8;

        graphics.fill(x, y, x + width, y + height, 0xCC0B0E12);
        graphics.fill(x, y, x + 3, y + height, 0xFFC99B18);
        graphics.drawString(font, title, x + 8, y + 5, 0xFFF7F8FC, false);
        graphics.drawString(font, value, x + 8, y + 17, 0xFF9CA3AF, false);
    }

    private static String tier(double exposure) {
        if (exposure >= 900.0D) return "T4";
        if (exposure >= 600.0D) return "T3";
        if (exposure >= 300.0D) return "T2";
        if (exposure >= 120.0D) return "T1";
        return "T0";
    }
}
