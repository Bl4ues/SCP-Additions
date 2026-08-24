package com.bl4ues.scpclassifieddirective.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Applies the resolution-independent virtual canvas to every mod-owned screen.
 *
 * Forge posts screen render/input events before calling the Screen methods. We
 * therefore dispatch the Screen method ourselves with already-normalized event
 * coordinates and cancel only Forge's duplicate call. Other event listeners keep
 * seeing the same Pre/Post lifecycle, just in virtual coordinates.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ResponsiveUiScaleEvents {
    private static final Deque<RenderFrame> RENDER_FRAMES = new ArrayDeque<>();

    private ResponsiveUiScaleEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInit(ScreenEvent.Init.Pre event) {
        ResponsiveUiScale.applyVirtualSize(event.getScreen());
    }

    /** Keep the scale active through the whole screen and all Render.Post UI. */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRenderBegin(ScreenEvent.Render.Pre event) {
        if (!ResponsiveUiScale.manages(event.getScreen())) return;
        ResponsiveUiScale.Context context = ResponsiveUiScale.current();
        event.getScreen().width = context.virtualWidth();
        event.getScreen().height = context.virtualHeight();
        ResponsiveUiScale.push(event.getGuiGraphics(), context);
        RENDER_FRAMES.push(new RenderFrame(event.getScreen(),
                event.getGuiGraphics()));
    }

    /**
     * Render with the virtual mouse coordinates produced by the event mixin,
     * then cancel Forge's second render call. Earlier Pre listeners can still
     * cancel normally.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onRenderDispatch(ScreenEvent.Render.Pre event) {
        if (!ResponsiveUiScale.manages(event.getScreen()) || event.isCanceled()) {
            return;
        }
        event.getScreen().renderWithTooltip(event.getGuiGraphics(),
                event.getMouseX(), event.getMouseY(), event.getPartialTick());
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderEnd(ScreenEvent.Render.Post event) {
        if (!ResponsiveUiScale.manages(event.getScreen())) return;
        RenderFrame frame = RENDER_FRAMES.peek();
        if (frame == null || frame.screen() != event.getScreen()) return;
        RENDER_FRAMES.pop();
        ResponsiveUiScale.pop(frame.graphics());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!ResponsiveUiScale.manages(event.getScreen()) || event.isCanceled()) {
            return;
        }
        boolean handled = event.getScreen().mouseClicked(event.getMouseX(),
                event.getMouseY(), event.getButton());
        ResponsiveUiScale.Context context = ResponsiveUiScale.current();
        MinecraftForge.EVENT_BUS.post(new ScreenEvent.MouseButtonPressed.Post(
                event.getScreen(), event.getMouseX() * context.scale(),
                event.getMouseY() * context.scale(), event.getButton(), handled));
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!ResponsiveUiScale.manages(event.getScreen()) || event.isCanceled()) {
            return;
        }
        boolean handled = event.getScreen().mouseReleased(event.getMouseX(),
                event.getMouseY(), event.getButton());
        ResponsiveUiScale.Context context = ResponsiveUiScale.current();
        MinecraftForge.EVENT_BUS.post(new ScreenEvent.MouseButtonReleased.Post(
                event.getScreen(), event.getMouseX() * context.scale(),
                event.getMouseY() * context.scale(), event.getButton(), handled));
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!ResponsiveUiScale.manages(event.getScreen()) || event.isCanceled()) {
            return;
        }
        boolean handled = event.getScreen().mouseDragged(event.getMouseX(),
                event.getMouseY(), event.getMouseButton(), event.getDragX(),
                event.getDragY());
        if (!handled) {
            ResponsiveUiScale.Context context = ResponsiveUiScale.current();
            MinecraftForge.EVENT_BUS.post(new ScreenEvent.MouseDragged.Post(
                    event.getScreen(), event.getMouseX() * context.scale(),
                    event.getMouseY() * context.scale(), event.getMouseButton(),
                    event.getDragX() * context.scale(),
                    event.getDragY() * context.scale()));
        }
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!ResponsiveUiScale.manages(event.getScreen()) || event.isCanceled()) {
            return;
        }
        boolean handled = event.getScreen().mouseScrolled(event.getMouseX(),
                event.getMouseY(), event.getScrollDelta());
        if (!handled) {
            ResponsiveUiScale.Context context = ResponsiveUiScale.current();
            MinecraftForge.EVENT_BUS.post(new ScreenEvent.MouseScrolled.Post(
                    event.getScreen(), event.getMouseX() * context.scale(),
                    event.getMouseY() * context.scale(), event.getScrollDelta()));
        }
        event.setCanceled(true);
    }

    private record RenderFrame(net.minecraft.client.gui.screens.Screen screen,
            net.minecraft.client.gui.GuiGraphics graphics) {
    }
}
