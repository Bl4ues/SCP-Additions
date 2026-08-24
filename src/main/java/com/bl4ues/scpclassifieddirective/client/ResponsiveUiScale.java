package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Keeps SCP: Classified Directive interfaces at the same physical proportions
 * regardless of Minecraft's GUI-scale setting.
 *
 * The authored reference is a 1920x1080 display at GUI scale 2, which produces
 * a 960x540 logical canvas. Other GUI scales are mapped back to that same
 * reference canvas; lower/higher display resolutions then scale the whole canvas
 * uniformly instead of forcing every screen to solve the same responsive-layout
 * problem independently.
 */
public final class ResponsiveUiScale {
    public static final int REFERENCE_WIDTH = 960;
    public static final int REFERENCE_HEIGHT = 540;

    private static final String MOD_PACKAGE =
            "com.bl4ues.scpclassifieddirective.";
    private static final ThreadLocal<Deque<Context>> ACTIVE =
            ThreadLocal.withInitial(ArrayDeque::new);

    private ResponsiveUiScale() {
    }

    /** Only mod-owned screens are normalized. Vanilla/other-mod GUIs are untouched. */
    public static boolean manages(Screen screen) {
        return screen != null
                && screen.getClass().getName().startsWith(MOD_PACKAGE);
    }

    public static Context current() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return new Context(1.0F, REFERENCE_WIDTH, REFERENCE_HEIGHT);
        }
        return context(minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight());
    }

    public static Context context(int actualWidth, int actualHeight) {
        int safeWidth = Math.max(1, actualWidth);
        int safeHeight = Math.max(1, actualHeight);
        float scale = Math.min(safeWidth / (float) REFERENCE_WIDTH,
                safeHeight / (float) REFERENCE_HEIGHT);
        if (!Float.isFinite(scale) || scale <= 0.0F) scale = 1.0F;

        // The limiting dimension maps exactly to the authored reference size.
        // The other dimension is allowed to grow so ultrawide / 16:10 displays
        // gain safe-area space instead of stretching the interface.
        int virtualWidth = Math.max(1,
                Mth.ceil(safeWidth / scale));
        int virtualHeight = Math.max(1,
                Mth.ceil(safeHeight / scale));
        return new Context(scale, virtualWidth, virtualHeight);
    }

    public static void applyVirtualSize(Screen screen) {
        if (!manages(screen)) return;
        Context context = current();
        screen.width = context.virtualWidth();
        screen.height = context.virtualHeight();
    }

    public static void push(GuiGraphics graphics, Context context) {
        if (graphics == null || context == null) return;
        graphics.pose().pushPose();
        graphics.pose().scale(context.scale(), context.scale(), 1.0F);
        ACTIVE.get().addLast(context);
    }

    public static void pop(GuiGraphics graphics) {
        if (graphics == null) return;
        Deque<Context> stack = ACTIVE.get();
        if (!stack.isEmpty()) stack.removeLast();
        if (stack.isEmpty()) ACTIVE.remove();
        graphics.pose().popPose();
    }

    /** Product of responsive transforms currently applied to GuiGraphics. */
    public static float activeScale() {
        Deque<Context> stack = ACTIVE.get();
        if (stack.isEmpty()) return 1.0F;
        float scale = 1.0F;
        for (Context context : stack) scale *= context.scale();
        return Float.isFinite(scale) && scale > 0.0F ? scale : 1.0F;
    }

    public static int scissorFloor(int coordinate) {
        float scale = activeScale();
        return Math.abs(scale - 1.0F) < 0.0001F
                ? coordinate : Mth.floor(coordinate * scale);
    }

    public static int scissorCeil(int coordinate) {
        float scale = activeScale();
        return Math.abs(scale - 1.0F) < 0.0001F
                ? coordinate : Mth.ceil(coordinate * scale);
    }

    /**
     * Wraps a mod-owned HUD render in the same virtual canvas used by screens.
     */
    public static void renderHud(GuiGraphics graphics, int actualWidth,
            int actualHeight, HudRenderer renderer) {
        if (graphics == null || renderer == null) return;
        Context context = context(actualWidth, actualHeight);
        push(graphics, context);
        try {
            renderer.render(context.virtualWidth(), context.virtualHeight());
        } finally {
            pop(graphics);
        }
    }

    @FunctionalInterface
    public interface HudRenderer {
        void render(int width, int height);
    }

    public record Context(float scale, int virtualWidth, int virtualHeight) {
        public int virtualX(int actualX) {
            return Mth.floor(actualX / scale);
        }

        public int virtualY(int actualY) {
            return Mth.floor(actualY / scale);
        }

        public double virtualX(double actualX) {
            return actualX / scale;
        }

        public double virtualY(double actualY) {
            return actualY / scale;
        }

        public double virtualDelta(double actualDelta) {
            return actualDelta / scale;
        }
    }
}
