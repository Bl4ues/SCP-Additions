package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Shared geometry and presentation constants for the optional top-down chat. */
public final class FacilityChatLayout {
    public static final int PANEL = 0xFF111317;
    public static final int HEADER = 0xFF24282E;
    public static final int NAVY = 0xFF081022;
    public static final int NAVY_HOVER = 0xFF131E36;
    public static final int BORDER = 0xFF46536C;
    public static final int BORDER_HOVER = 0xFF73809A;
    public static final int ACCENT = 0xFFC59A2A;
    public static final int PALE_GOLD = 0xFFE5D49A;
    public static final int WHITE = 0xFFF7F8FC;
    public static final int MUTED = 0xFF9CA3AF;
    public static final int LIVE = 0xFF79D58B;

    public static final int TOP_MARGIN = 6;
    public static final int HEADER_HEIGHT = 18;
    public static final int INPUT_GAP = 4;
    private static final int INPUT_LEFT = 17;
    private static final long OPEN_ANIMATION_NANOS = 220_000_000L;
    private static volatile long openedAtNanos = Long.MIN_VALUE;

    private FacilityChatLayout() {
    }

    public static void beginOpenAnimation() {
        openedAtNanos = System.nanoTime();
    }

    public static float openProgress() {
        long started = openedAtNanos;
        if (started == Long.MIN_VALUE) return 1.0F;
        float raw = Mth.clamp((float) (System.nanoTime() - started)
                / (float) OPEN_ANIMATION_NANOS, 0.0F, 1.0F);
        if (raw >= 1.0F) return 1.0F;
        float inverse = 1.0F - raw;
        return 1.0F - inverse * inverse * inverse;
    }

    /**
     * Screen-space offset shared by the history panel, input and suggestions.
     * At the beginning of the animation the complete console sits just above
     * the viewport and eases down to its normal top-left anchor.
     */
    public static int openOffsetScreen(ChatComponent chat) {
        float progress = openProgress();
        if (progress >= 0.999F) return 0;
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int travel = Math.min(screenHeight,
                Math.max(32, inputY(chat) + 20));
        return -Math.round(travel * (1.0F - progress));
    }

    public static int lineHeight() {
        Minecraft minecraft = Minecraft.getInstance();
        return (int) (9.0D * (minecraft.options.chatLineSpacing().get() + 1.0D));
    }

    public static int topScaled(ChatComponent chat) {
        double scale = Math.max(0.01D, chat.getScale());
        return Mth.floor((double) TOP_MARGIN / scale);
    }

    public static int messageTopScaled(ChatComponent chat, boolean focused) {
        return topScaled(chat) + (focused ? HEADER_HEIGHT : 0);
    }

    public static int messageRegionHeightScaled(ChatComponent chat) {
        return chat.getLinesPerPage() * lineHeight();
    }

    public static int panelScreenRight(ChatComponent chat) {
        double scale = Math.max(0.01D, chat.getScale());
        int logicalWidth = Mth.ceil((float) chat.getWidth() / (float) scale);
        return Mth.ceil((logicalWidth + 12) * scale);
    }

    public static int inputY(ChatComponent chat) {
        double scale = Math.max(0.01D, chat.getScale());
        int logical = messageTopScaled(chat, true)
                + messageRegionHeightScaled(chat) + INPUT_GAP;
        int desired = Mth.ceil(logical * scale);
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return Math.min(desired, Math.max(TOP_MARGIN + 18, screenHeight - 16));
    }

    public static int inputX() {
        return INPUT_LEFT;
    }

    public static int inputWidth(ChatComponent chat) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int right = Math.min(panelScreenRight(chat) - 4, screenWidth - 4);
        return Math.max(40, right - INPUT_LEFT);
    }

    public static int suggestionTop(EditBox input, int screenHeight, int lineLimit) {
        int desired = input.getY() + input.getHeight() + 4;
        int reserve = Math.max(12, lineLimit * 12) + 4;
        return Math.max(4, Math.min(desired, screenHeight - reserve));
    }

    public static void drawInputFrame(GuiGraphics graphics,
            ChatComponent chat, EditBox input) {
        int left = 2;
        int right = Math.min(panelScreenRight(chat),
                Minecraft.getInstance().getWindow().getGuiScaledWidth() - 2);
        int top = input.getY() - 4;
        int bottom = input.getY() + input.getHeight();

        graphics.fill(left, top, right, bottom, 0xE6081022);
        graphics.fill(left, top, left + 2, bottom, ACCENT);
        graphics.fill(left + 2, top, right, top + 1, BORDER);
        graphics.fill(left + 2, bottom - 1, right, bottom, BORDER);
        graphics.fill(right - 1, top, right, bottom, BORDER);

        Component prompt = ScpFonts.roboto(">");
        graphics.drawString(Minecraft.getInstance().font, prompt,
                7, input.getY() + 2, PALE_GOLD, false);
    }

    public static int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }
}
