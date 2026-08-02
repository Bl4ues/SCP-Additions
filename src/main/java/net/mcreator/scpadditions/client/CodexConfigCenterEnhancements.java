package net.mcreator.scpadditions.client;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Field;
import java.util.UUID;

/** Keeps the legacy Config Center useful while making the dedicated item the default carrier. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CodexConfigCenterEnhancements {
    private CodexConfigCenterEnhancements() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        String className = event.getScreen().getClass().getName();
        if (className.endsWith("ConfigCenterClient$CodexListScreen")) {
            renameWidgets(event, "+ Paper Document", "+ Document");
            return;
        }
        if (!className.endsWith("ConfigCenterClient$CodexDetailScreen")) return;

        renameWidgets(event, "Write Text", "Write Markdown");
        renameWidgets(event, "Edit Text", "Edit Markdown");
        try {
            JsonObject edit = (JsonObject) getField(event.getScreen(), "edit");
            if (edit == null) return;
            boolean newlyCreated = "minecraft:paper".equals(string(edit, "id"))
                    && "New Document".equals(string(edit, "name"))
                    && !edit.has("match_mode") && !edit.has("codex_id");
            if (!newlyCreated) return;

            edit.addProperty("id", "scp_additions:document");
            edit.addProperty("match_mode", "unique");
            edit.addProperty("codex_id", UUID.randomUUID().toString());
            setBooleanField(event.getScreen(), "uniqueMode", true);
            Object idBox = getField(event.getScreen(), "idBox");
            if (idBox instanceof EditBox box) {
                box.setValue("scp_additions:document");
            }
            Object textBox = getField(event.getScreen(), "textBox");
            if (textBox instanceof EditBox box) {
                box.setHint(Component.literal("Packaged Markdown text resource"));
            }
            for (GuiEventListener listener : event.getListenersList()) {
                if (listener instanceof AbstractWidget widget
                        && widget.getMessage().getString()
                        .startsWith("Match Mode:")) {
                    widget.setMessage(Component.literal(
                            "Match Mode: Unique Generated Item"));
                } else if (listener instanceof AbstractWidget widget
                        && "Save & Give Test Item".equals(
                        widget.getMessage().getString())) {
                    widget.active = true;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // The enhancement is deliberately non-fatal if the private editor
            // layout changes in a later version.
        }
    }

    private static void renameWidgets(ScreenEvent.Init.Post event,
                                      String from, String to) {
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget widget
                    && from.equals(widget.getMessage().getString())) {
                widget.setMessage(Component.literal(to));
            }
        }
    }

    private static Object getField(Object instance, String name)
            throws ReflectiveOperationException {
        Field field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    private static void setBooleanField(Object instance, String name,
                                        boolean value)
            throws ReflectiveOperationException {
        Field field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setBoolean(instance, value);
    }

    private static String string(JsonObject object, String key) {
        if (object == null || !object.has(key)
                || !object.get(key).isJsonPrimitive()) return "";
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
