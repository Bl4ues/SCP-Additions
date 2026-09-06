package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

/** Keeps the established Items/Entities hub wording after Roomba spawn moved to room mapping. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RoombaConfigCenterEnhancements {
    private static final String OLD_HUB_TITLE = "Inventory, Equipment & Codex";
    private static final String HUB_TITLE = "Items, Entities & Codex";

    private RoombaConfigCenterEnhancements() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        String className = event.getScreen().getClass().getName();
        if (className.endsWith("ConfigCenterClient$HomeScreen")) {
            renameWidget(event, OLD_HUB_TITLE, HUB_TITLE);
        } else if (className.endsWith("ConfigCenterClient$InventoryHubScreen")) {
            trySetField(event.getScreen(), "screenTitle", HUB_TITLE);
        }
    }

    private static void renameWidget(ScreenEvent.Init.Post event,
            String from, String to) {
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget widget
                    && from.equals(widget.getMessage().getString())) {
                widget.setMessage(ScpFonts.roboto(to));
                return;
            }
        }
    }

    private static void trySetField(Object instance, String name, Object value) {
        for (Class<?> type = instance.getClass(); type != null;
                type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(instance, value);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }
}
