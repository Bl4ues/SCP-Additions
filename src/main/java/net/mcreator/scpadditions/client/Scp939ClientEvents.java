package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.Scp939InputPacket;
import net.mcreator.scpadditions.network.Scp939Network;

/** Client input and compact HUD for SCP-939 interactions. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp939ClientEvents {
    private static boolean previousHold;
    private static boolean previousLeft;
    private static boolean previousRight;

    private Scp939ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            previousHold = previousLeft = previousRight = false;
            Scp939ClientState.clear();
            return;
        }

        boolean hold = Scp939Keybinds.HOLD_BREATH.isDown();
        if (hold != previousHold) {
            previousHold = hold;
            Scp939Network.sendInput(Scp939InputPacket.HOLD_BREATH, hold);
        }

        boolean left = minecraft.options.keyLeft.isDown();
        boolean right = minecraft.options.keyRight.isDown();
        if (Scp939ClientState.pinned()) {
            if (left && !previousLeft) {
                Scp939Network.sendInput(Scp939InputPacket.STRUGGLE_LEFT, true);
            }
            if (right && !previousRight) {
                Scp939Network.sendInput(Scp939InputPacket.STRUGGLE_RIGHT, true);
            }
        }
        previousLeft = left;
        previousRight = right;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        // Render exactly once in Forge's overlay pass. Without this guard the
        // translucent panels were drawn once for every vanilla overlay, making
        // them progressively darker and doing needless work every frame.
        if (event.getOverlay() != VanillaGuiOverlay.PORTAL.type()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui
                || minecraft.screen != null) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        if (Scp939ClientState.breathActive()) {
            renderBreath(graphics, minecraft, width, height);
        }
        if (Scp939ClientState.pinned()) {
            renderStruggle(graphics, minecraft, width, height);
        }
    }

    private static void renderBreath(GuiGraphics graphics,
            Minecraft minecraft, int width, int height) {
        int barWidth = 86;
        int barHeight = 5;
        int x = width / 2 - barWidth / 2;
        int y = height - 48;
        graphics.fill(x - 2, y - 2, x + barWidth + 2,
                y + barHeight + 2, 0xA0000000);
        graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF272727);
        int fill = Math.round(barWidth * Scp939ClientState.breathReserve());
        graphics.fill(x, y, x + fill, y + barHeight, 0xFFE8E8E8);
        String label = Scp939ClientState.holdingBreath()
                ? "HOLDING BREATH" : "BREATH";
        graphics.drawCenteredString(minecraft.font, Component.literal(label),
                width / 2, y - 11, 0xFFE8E8E8);
    }

    private static void renderStruggle(GuiGraphics graphics,
            Minecraft minecraft, int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2 + 28;
        Component key = Scp939ClientState.expectedKey() == 0
                ? minecraft.options.keyLeft.getTranslatedKeyMessage()
                : minecraft.options.keyRight.getTranslatedKeyMessage();
        graphics.fill(centerX - 48, centerY - 20, centerX + 48,
                centerY + 23, 0xB8000000);
        graphics.drawCenteredString(minecraft.font,
                Component.literal("BREAK FREE"), centerX, centerY - 15,
                0xFFFFFFFF);
        graphics.drawCenteredString(minecraft.font, key,
                centerX, centerY - 2, 0xFFFFFFFF);

        int time = Math.max(0, Math.min(20,
                Scp939ClientState.pinWindowTicks()));
        int timeWidth = Math.round(72.0F * time / 20.0F);
        graphics.fill(centerX - 36, centerY + 10, centerX + 36,
                centerY + 13, 0xFF303030);
        graphics.fill(centerX - 36, centerY + 10,
                centerX - 36 + timeWidth, centerY + 13, 0xFFFFFFFF);

        String status = Scp939ClientState.pinProgress() + "/3  |  "
                + Scp939ClientState.pinFailures() + "/3 FAIL";
        graphics.drawCenteredString(minecraft.font,
                Component.literal(status), centerX, centerY + 15,
                0xFFD0D0D0);
    }
}
