package net.mcreator.scpadditions.vitals.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.mcreator.scpadditions.client.Scp079EnergyClientState;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;

/** White upper-right developer HUD for SCP-079 processing power. */
public final class Scp079EnergyOverlay {
    private static final ResourceLocation ROBOTO_FONT =
            new ResourceLocation("scpinventory", "roboto");

    private static final int WIDTH = 94;
    private static final int HEIGHT = 32;
    private static final int MARGIN = 10;
    private static final int PANEL = 0xA8121518;
    private static final int PANEL_INACTIVE = 0x88121518;
    private static final int WHITE = 0xFFF4F4F4;
    private static final int MUTED = 0xFFB8B8B8;
    private static final int TRACK = 0x773C4145;

    private Scp079EnergyOverlay() {
    }

    public static void render(GuiGraphics graphics, int screenWidth,
            int screenHeight, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Scp079EnergyClientState.visible()
                || minecraft.player == null
                || minecraft.screen != null
                || minecraft.options.hideGui) {
            return;
        }

        boolean active = Scp079EnergyClientState.active();
        float energy = Math.max(0.0F,
                Math.min(100.0F, Scp079EnergyClientState.energy()));
        int x = screenWidth - WIDTH - MARGIN;
        int y = MARGIN;

        graphics.fill(x, y, x + WIDTH, y + HEIGHT,
                active ? PANEL : PANEL_INACTIVE);
        graphics.fill(x, y, x + WIDTH, y + 1, WHITE);
        graphics.fill(x, y + HEIGHT - 1, x + WIDTH, y + HEIGHT, WHITE);
        graphics.fill(x, y, x + 1, y + HEIGHT, WHITE);
        graphics.fill(x + WIDTH - 1, y, x + WIDTH, y + HEIGHT, WHITE);

        ItemStack icon = new ItemStack(ScpAdditionsModItems.SCP_079ON.get());
        graphics.renderItem(icon, x + 6, y + 6);

        Component label = Component.literal(active ? "PROCESSING" : "OFFLINE")
                .withStyle(style -> style.withFont(ROBOTO_FONT));
        graphics.drawString(minecraft.font, label, x + 28, y + 5,
                active ? WHITE : MUTED, false);

        Component value = Component.literal(Integer.toString(Math.round(energy)))
                .withStyle(style -> style.withFont(ROBOTO_FONT));
        graphics.drawString(minecraft.font, value, x + 28, y + 15,
                WHITE, false);

        int barX = x + 51;
        int barY = y + 18;
        int barWidth = WIDTH - 57;
        graphics.fill(barX, barY, barX + barWidth, barY + 3, TRACK);
        int fill = Math.round(barWidth * energy / 100.0F);
        if (fill > 0) {
            graphics.fill(barX, barY, barX + fill, barY + 3,
                    active ? WHITE : MUTED);
        }
    }
}
