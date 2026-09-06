package com.bl4ues.scpclassifieddirective.config.ui;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Routes screwdriver machine edits past the Configuration Center home screen. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ConfigCenterDirectOpenClient {
    private static volatile String pendingSection = "";

    private ConfigCenterDirectOpenClient() {
    }

    public static void arm(String section) {
        pendingSection = section == null ? "" : section;
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        String wanted = pendingSection;
        if (wanted.isBlank()
                || !event.getScreen().getClass().getName().endsWith(
                        "ConfigCenterClient$HomeScreen")) {
            return;
        }

        for (GuiEventListener listener : event.getListenersList()) {
            if (!(listener instanceof AbstractWidget widget)
                    || !(widget instanceof AbstractButton button)
                    || !wanted.equals(widget.getMessage().getString())) {
                continue;
            }
            pendingSection = "";
            Minecraft.getInstance().execute(button::onPress);
            return;
        }

        ScpClassifiedDirectiveMod.LOGGER.warn(
                "Could not find requested Configuration Center section '{}'",
                wanted);
        pendingSection = "";
    }
}
