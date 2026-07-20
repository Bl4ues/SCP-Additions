package net.mcreator.scpadditions.fabric;

import java.util.ArrayList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;

final class FabricClientEventBridge {
    private FabricClientEventBridge() {}
    static void register() {
        var modBus=ScpAdditionsFabric.MOD_BUS;
        modBus.post(new RegisterKeyMappingsEvent());
        modBus.post(new RegisterMenuScreensEvent());
        modBus.post(new EntityRenderersEvent.RegisterRenderers());
        modBus.post(new RegisterParticleProvidersEvent());
        modBus.post(new RegisterColorHandlersEvent.Block());
        modBus.post(new RegisterColorHandlersEvent.Item());
        modBus.post(new RegisterGuiLayersEvent());
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) ->
                NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.ItemTooltipEvent(stack, lines)));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            NeoForge.EVENT_BUS.post(new ClientTickEvent.Pre());
            if (client.player != null) {
                NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(client.player));
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(client.player));
            }
            NeoForge.EVENT_BUS.post(new InputEvent.Key());
            NeoForge.EVENT_BUS.post(new ClientTickEvent.Post());
        });
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client) -> NeoForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.LoggingIn()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client) -> NeoForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.LoggingOut()));
        HudRenderCallback.EVENT.register(FabricHudLayerRegistry::render);
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if(context.matrixStack()!=null) NeoForge.EVENT_BUS.post(new RenderLevelStageEvent(RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS, context.matrixStack(), context.camera()));
        });

        ScreenEvents.AFTER_INIT.register((client,screen,width,height) -> {
            var listeners=new ArrayList<GuiEventListener>(screen.children());
            NeoForge.EVENT_BUS.post(new ScreenEvent.Init.Post(screen,listeners,
                    listener -> ((net.fabricmc.fabric.api.client.screen.v1.ScreensAccessor)(Object)screen).fabric$add(listener),
                    listener -> ((net.fabricmc.fabric.api.client.screen.v1.ScreensAccessor)(Object)screen).fabric$remove(listener)));
            ScreenMouseEvents.allowMouseClick(screen).register((s,x,y,b) -> !NeoForge.EVENT_BUS.post(new ScreenEvent.MouseButtonPressed.Pre(s,x,y,b)));
            ScreenMouseEvents.allowMouseRelease(screen).register((s,x,y,b) -> !NeoForge.EVENT_BUS.post(new ScreenEvent.MouseButtonReleased.Pre(s,x,y,b)));
            ScreenMouseEvents.allowMouseScroll(screen).register((s,x,y,dx,dy) -> !NeoForge.EVENT_BUS.post(new ScreenEvent.MouseScrolled.Pre(s,x,y,dx,dy)));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s,key,scan,mods) -> !NeoForge.EVENT_BUS.post(new ScreenEvent.KeyPressed.Pre(s,key,scan,mods)));
            ScreenEvents.beforeRender(screen).register((s,g,x,y,t) -> NeoForge.EVENT_BUS.post(new ScreenEvent.Render.Pre(s,g,x,y)));
            ScreenEvents.afterRender(screen).register((s,g,x,y,t) -> NeoForge.EVENT_BUS.post(new ScreenEvent.Render.Post(s,g,x,y)));
        });
    }
}
