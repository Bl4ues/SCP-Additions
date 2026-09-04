package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps the placeholder role selector usable after SCP-079 moves the player into
 * spectator mode. Vanilla spectator item-use is rejected server-side, so the
 * already-held selector routes its Use click through SCP-079's release packet.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class Scp079RoleSelectorClient {
    private Scp079RoleSelectorClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !Scp079PlayableClient.active()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        if (!holdingSelector(minecraft)) return;

        while (minecraft.options.keyUse.consumeClick()) {
            Scp079PlayableNetwork.requestRelease();
        }
    }

    private static boolean holdingSelector(Minecraft minecraft) {
        ItemStack main = minecraft.player.getMainHandItem();
        ItemStack off = minecraft.player.getOffhandItem();
        return main.is(ScpClassifiedDirectiveModItems.SCP_ROLE_SELECTOR.get())
                || off.is(ScpClassifiedDirectiveModItems.SCP_ROLE_SELECTOR.get());
    }
}
