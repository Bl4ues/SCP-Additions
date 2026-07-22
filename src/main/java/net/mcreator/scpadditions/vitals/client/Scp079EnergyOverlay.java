package net.mcreator.scpadditions.vitals.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.mcreator.scpadditions.client.Scp079EnergyClientState;
import net.mcreator.scpadditions.client.Scp079EnergyClientState.ClientDecisionSnapshot;
import net.mcreator.scpadditions.facility.Scp079DecisionLog;
import net.mcreator.scpadditions.facility.Scp079DecisionLog.DecisionOutcome;
import net.mcreator.scpadditions.facility.Scp079DecisionLog.DecisionType;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;

import java.util.List;
import java.util.Locale;

/** Independent upper-right developer HUDs for SCP-079 energy and decisions. */
public final class Scp079EnergyOverlay {
    private static final ResourceLocation ROBOTO_FONT =
            new ResourceLocation("scpinventory", "roboto");

    private static final int ENERGY_WIDTH = 94;
    private static final int ENERGY_HEIGHT = 32;
    private static final int FEED_WIDTH = 238;
    private static final int FEED_HEADER_HEIGHT = 16;
    private static final int ENTRY_HEIGHT = 27;
    private static final int MAX_VISIBLE_ENTRIES = 5;
    private static final int GAP = 5;
    private static final int MARGIN = 10;
    private static final int PANEL = 0xA8121518;
    private static final int PANEL_INACTIVE = 0x88121518;
    private static final int ENTRY_PANEL = 0xB0121518;
    private static final int WHITE = 0xFFF4F4F4;
    private static final int MUTED = 0xFFB8B8B8;
    private static final int TRACK = 0x773C4145;
    private static final int AMBER = 0xFFFFC56D;
    private static final int RED = 0xFFFF8B8B;
    private static final int FADE_START_TICKS = 220;

    private Scp079EnergyOverlay() {
    }

    public static int occupiedHeight() {
        boolean energy = Scp079EnergyClientState.visible();
        boolean decisions = Scp079EnergyClientState.decisionLogVisible();
        if (!energy && !decisions) return 0;

        int height = energy ? ENERGY_HEIGHT : 0;
        if (decisions) {
            if (height > 0) height += GAP;
            int rows = Math.max(1, Math.min(MAX_VISIBLE_ENTRIES,
                    Scp079EnergyClientState.decisions().size()));
            height += FEED_HEADER_HEIGHT + rows * ENTRY_HEIGHT;
        }
        return height;
    }

    public static void render(GuiGraphics graphics, int screenWidth,
            int screenHeight, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean energy = Scp079EnergyClientState.visible();
        boolean decisions = Scp079EnergyClientState.decisionLogVisible();
        if ((!energy && !decisions)
                || minecraft.player == null
                || minecraft.screen != null
                || minecraft.options.hideGui) {
            return;
        }

        if (energy) renderEnergy(graphics, minecraft, screenWidth);
        if (decisions) renderDecisionFeed(graphics, minecraft, screenWidth,
                energy ? MARGIN + ENERGY_HEIGHT + GAP : MARGIN);
    }

    private static void renderEnergy(GuiGraphics graphics,
            Minecraft minecraft, int screenWidth) {
        boolean active = Scp079EnergyClientState.active();
        float energy = Math.max(0.0F,
                Math.min(100.0F, Scp079EnergyClientState.energy()));
        int x = screenWidth - ENERGY_WIDTH - MARGIN;
        int y = MARGIN;

        graphics.fill(x, y, x + ENERGY_WIDTH, y + ENERGY_HEIGHT,
                active ? PANEL : PANEL_INACTIVE);
        border(graphics, x, y, ENERGY_WIDTH, ENERGY_HEIGHT, WHITE);

        ItemStack icon = new ItemStack(ScpAdditionsModItems.SCP_079ON.get());
        graphics.renderItem(icon, x + 6, y + 6);

        draw(graphics, minecraft.font, active ? "PROCESSING" : "OFFLINE",
                x + 28, y + 5, active ? WHITE : MUTED);
        draw(graphics, minecraft.font, Integer.toString(Math.round(energy)),
                x + 28, y + 15, WHITE);

        int barX = x + 51;
        int barY = y + 18;
        int barWidth = ENERGY_WIDTH - 57;
        graphics.fill(barX, barY, barX + barWidth, barY + 3, TRACK);
        int fill = Math.round(barWidth * energy / 100.0F);
        if (fill > 0) {
            graphics.fill(barX, barY, barX + fill, barY + 3,
                    active ? WHITE : MUTED);
        }
    }

    private static void renderDecisionFeed(GuiGraphics graphics,
            Minecraft minecraft, int screenWidth, int y) {
        List<ClientDecisionSnapshot> decisions =
                Scp079EnergyClientState.decisions();
        int visible = Math.min(MAX_VISIBLE_ENTRIES, decisions.size());
        int rows = Math.max(1, visible);
        int height = FEED_HEADER_HEIGHT + rows * ENTRY_HEIGHT;
        int x = screenWidth - FEED_WIDTH - MARGIN;

        graphics.fill(x, y, x + FEED_WIDTH, y + height, PANEL);
        border(graphics, x, y, FEED_WIDTH, height, WHITE);
        draw(graphics, minecraft.font, "SCP-079 DECISION LOG", x + 7,
                y + 5, WHITE);

        if (visible == 0) {
            int rowY = y + FEED_HEADER_HEIGHT;
            graphics.fill(x + 1, rowY, x + FEED_WIDTH - 1,
                    rowY + ENTRY_HEIGHT - 1, ENTRY_PANEL);
            draw(graphics, minecraft.font, "NO RECENT DECISIONS", x + 8,
                    rowY + 9, MUTED);
            return;
        }

        for (int index = 0; index < visible; index++) {
            renderDecision(graphics, minecraft, decisions.get(index), x,
                    y + FEED_HEADER_HEIGHT + index * ENTRY_HEIGHT);
        }
    }

    private static void renderDecision(GuiGraphics graphics,
            Minecraft minecraft, ClientDecisionSnapshot decision,
            int x, int y) {
        float alpha = fadeAlpha(decision.ageTicks());
        int panel = withAlpha(ENTRY_PANEL, alpha);
        int text = withAlpha(WHITE, alpha);
        int muted = withAlpha(MUTED, alpha);
        int stripe = withAlpha(outcomeColor(decision.outcome()), alpha);

        graphics.fill(x + 1, y, x + FEED_WIDTH - 1,
                y + ENTRY_HEIGHT - 1, panel);
        graphics.fill(x + 1, y, x + 4, y + ENTRY_HEIGHT - 1, stripe);

        String cost = decision.cost() > 0.01F
                ? "-" + formatCost(decision.cost()) + " AP" : "";
        int costWidth = cost.isEmpty() ? 0
                : minecraft.font.width(styled(cost));
        int titleWidth = FEED_WIDTH - 18 - costWidth;
        String title = fit(minecraft.font, title(decision.type()), titleWidth);
        draw(graphics, minecraft.font, title, x + 8, y + 5, text);
        if (!cost.isEmpty()) {
            draw(graphics, minecraft.font, cost,
                    x + FEED_WIDTH - 7 - costWidth, y + 5, text);
        }

        String detail = detail(minecraft, decision);
        draw(graphics, minecraft.font,
                fit(minecraft.font, detail, FEED_WIDTH - 15),
                x + 8, y + 16, muted);
    }

    private static String detail(Minecraft minecraft,
            ClientDecisionSnapshot decision) {
        StringBuilder result = new StringBuilder();
        if (!decision.context().isBlank()) result.append(decision.context());
        if (result.length() > 0) result.append(" · ");
        if (minecraft.level != null && !decision.dimension().isBlank()
                && !minecraft.level.dimension().location().toString()
                .equals(decision.dimension())) {
            result.append(shortDimension(decision.dimension())).append(' ');
        }
        result.append(decision.pos().getX()).append(' ')
                .append(decision.pos().getY()).append(' ')
                .append(decision.pos().getZ());
        return result.toString();
    }

    private static String shortDimension(String dimension) {
        int colon = dimension.indexOf(':');
        return colon >= 0 && colon + 1 < dimension.length()
                ? dimension.substring(colon + 1) : dimension;
    }

    private static String title(DecisionType type) {
        return switch (type) {
            case OPEN_DOOR -> "OPENED DOOR";
            case CLOSE_DOOR -> "CLOSED DOOR";
            case DENY_ACCESS -> "DENIED ACCESS";
            case TESLA_SUPPRESSION -> "SUPPRESSED TESLA GATE";
            case OPEN_SCP_012_ROUTE -> "OPENED SCP-012 ROUTE";
            case OPEN_SCP_012_BOX -> "OPENED SCP-012";
            case ABANDON_SCP_012_CONTEST ->
                    "ABANDONED SCP-012 CONTEST";
            case ABORTED_ACTION -> "ABORTED ACTION";
        };
    }

    private static int outcomeColor(DecisionOutcome outcome) {
        return switch (outcome) {
            case EXECUTED -> WHITE;
            case ABANDONED -> AMBER;
            case ABORTED -> RED;
        };
    }

    private static float fadeAlpha(int ageTicks) {
        if (ageTicks <= FADE_START_TICKS) return 1.0F;
        int fadeDuration = Math.max(1,
                Scp079DecisionLog.CLIENT_LIFETIME_TICKS - FADE_START_TICKS);
        return Math.max(0.0F, 1.0F
                - (ageTicks - FADE_START_TICKS) / (float) fadeDuration);
    }

    private static String formatCost(float cost) {
        if (Math.abs(cost - Math.round(cost)) < 0.05F) {
            return Integer.toString(Math.round(cost));
        }
        return String.format(Locale.ROOT, "%.1f", cost);
    }

    private static String fit(Font font, String text, int maxWidth) {
        if (text == null || text.isBlank() || maxWidth <= 0) return "";
        if (font.width(styled(text)) <= maxWidth) return text;
        String suffix = "...";
        int end = text.length();
        while (end > 0 && font.width(styled(
                text.substring(0, end) + suffix)) > maxWidth) {
            end--;
        }
        return end <= 0 ? suffix : text.substring(0, end) + suffix;
    }

    private static void draw(GuiGraphics graphics, Font font, String text,
            int x, int y, int color) {
        graphics.drawString(font, styled(text), x, y, color, false);
    }

    private static Component styled(String text) {
        return Component.literal(text == null ? "" : text)
                .withStyle(style -> style.withFont(ROBOTO_FONT));
    }

    private static int withAlpha(int color, float multiplier) {
        int alpha = (color >>> 24) & 0xFF;
        int adjusted = Math.max(0, Math.min(255,
                Math.round(alpha * multiplier)));
        return (adjusted << 24) | (color & 0x00FFFFFF);
    }

    private static void border(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
