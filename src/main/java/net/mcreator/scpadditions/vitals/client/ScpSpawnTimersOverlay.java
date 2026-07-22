package net.mcreator.scpadditions.vitals.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.mcreator.scpadditions.client.Scp079EnergyClientState;
import net.mcreator.scpadditions.client.Scp079EnergyClientState.ClientRoamerSnapshot;
import net.mcreator.scpadditions.init.Scp131Items;
import net.mcreator.scpadditions.roamer.RoamerResult;
import net.mcreator.scpadditions.roamer.RoamerState;
import net.mcreator.scpadditions.roamer.RoamerType;

/** White upper-right developer HUD for roamer spawn scheduling. */
public final class ScpSpawnTimersOverlay {
    private static final ResourceLocation ROBOTO_FONT =
            new ResourceLocation("scpinventory", "roboto");
    private static final int WIDTH = 206;
    private static final int ROW_HEIGHT = 31;
    private static final int HEIGHT = 17 + ROW_HEIGHT * RoamerType.values().length;
    private static final int MARGIN = 10;
    private static final int PANEL = 0xB0121518;
    private static final int WHITE = 0xFFF4F4F4;
    private static final int MUTED = 0xFFB8B8B8;
    private static final int DIVIDER = 0x664D5358;

    private ScpSpawnTimersOverlay() {
    }

    public static void render(GuiGraphics graphics, int screenWidth,
            int screenHeight, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Scp079EnergyClientState.spawnTimersVisible()
                || minecraft.player == null || minecraft.screen != null
                || minecraft.options.hideGui) {
            return;
        }

        int occupied = Scp079EnergyOverlay.occupiedHeight();
        int x = screenWidth - WIDTH - MARGIN;
        int y = MARGIN + occupied + (occupied > 0 ? 6 : 0);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, PANEL);
        border(graphics, x, y, WIDTH, HEIGHT);
        draw(graphics, minecraft, "ROAMER SPAWN SCHEDULER", x + 7,
                y + 5, WHITE);

        int index = 0;
        for (RoamerType type : RoamerType.values()) {
            int rowY = y + 17 + index * ROW_HEIGHT;
            if (index > 0) {
                graphics.fill(x + 5, rowY, x + WIDTH - 5, rowY + 1,
                        DIVIDER);
            }
            renderRoamer(graphics, minecraft, type, x, rowY);
            index++;
        }
    }

    private static void renderRoamer(GuiGraphics graphics,
            Minecraft minecraft, RoamerType type, int x, int rowY) {
        ClientRoamerSnapshot snapshot = Scp079EnergyClientState.roamer(type);
        graphics.renderItem(icon(type), x + 6, rowY + 7);
        draw(graphics, minecraft, type.displayName(), x + 28,
                rowY + 4, WHITE);

        String state = stateText(snapshot);
        Component stateComponent = styled(state);
        int stateX = x + WIDTH - 7
                - minecraft.font.width(stateComponent);
        graphics.drawString(minecraft.font, stateComponent, stateX,
                rowY + 4, WHITE, false);
        draw(graphics, minecraft, resultText(snapshot), x + 28,
                rowY + 16, MUTED);
    }

    private static String stateText(ClientRoamerSnapshot snapshot) {
        return switch (snapshot.state()) {
            case DISABLED -> "DISABLED";
            case CONTAINED -> "CONTAINED";
            case PAUSED -> "PAUSED";
            case SPAWNED -> "SPAWNED";
            case COUNTDOWN -> formatTime(snapshot.remainingTicks());
        };
    }

    private static String resultText(ClientRoamerSnapshot snapshot) {
        if (snapshot.state() == RoamerState.CONTAINED) {
            return "SPAWN CYCLE HELD BY CONTAINMENT";
        }
        if (snapshot.state() == RoamerState.SPAWNED) {
            return "ACTIVE INSTANCE · TIMER STOPPED";
        }
        return switch (snapshot.result()) {
            case NONE -> "COUNTDOWN ACTIVE";
            case TIMER_STARTED -> "SPAWN ENABLED · TIMER STARTED";
            case SPAWNED -> "LAST CHECK: SPAWN SUCCESSFUL";
            case DESPAWNED_TIMER_RESET ->
                    "DESPAWNED · FULL TIMER RESTARTED";
            case CHANCE_FAILED -> "LAST CHECK: CHANCE FAILED";
            case NO_VALID_POSITION -> "LAST CHECK: NO VALID POSITION";
            case BLOCKED_BY_EXISTING ->
                    "EXISTING INSTANCE · TIMER STOPPED";
            case RULE_DISABLED -> "SPAWN GAMERULE IS OFF";
            case MODULE_DISABLED -> "ENTITY MODULE IS OFF";
            case NOT_IMPLEMENTED -> "SPAWN SYSTEM NOT IMPLEMENTED";
            case PAUSED_CREATIVE -> "PAUSED WHILE PLAYER IS CREATIVE";
            case PAUSED_SPECTATOR -> "PAUSED WHILE PLAYER IS SPECTATING";
        };
    }

    private static ItemStack icon(RoamerType type) {
        return switch (type) {
            case SCP_173 -> new ItemStack(
                    Scp131Items.SCP_173_SPAWN_EGG.get());
            case SCP_106 -> new ItemStack(
                    Scp131Items.SCP_106_SPAWN_EGG.get());
        };
    }

    private static String formatTime(int ticks) {
        if (ticks < 0) return "--:--";
        int totalSeconds = Math.max(0, (ticks + 19) / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static void draw(GuiGraphics graphics, Minecraft minecraft,
            String text, int x, int y, int color) {
        graphics.drawString(minecraft.font, styled(text), x, y, color, false);
    }

    private static Component styled(String text) {
        return Component.literal(text).withStyle(style ->
                style.withFont(ROBOTO_FONT));
    }

    private static void border(GuiGraphics graphics, int x, int y,
            int width, int height) {
        graphics.fill(x, y, x + width, y + 1, WHITE);
        graphics.fill(x, y + height - 1, x + width, y + height, WHITE);
        graphics.fill(x, y, x + 1, y + height, WHITE);
        graphics.fill(x + width - 1, y, x + width, y + height, WHITE);
    }
}
