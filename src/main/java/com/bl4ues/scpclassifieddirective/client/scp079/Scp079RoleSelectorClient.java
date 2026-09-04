package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps the selector accessible after SCP-079 has forced spectator mode. */
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
        if (!holdingSelector(minecraft) || !minecraft.options.keyShift.isDown()) {
            return;
        }

        while (minecraft.options.keyUse.consumeClick()) {
            ScpRoleSelectorScreen.open(true);
        }
    }

    private static boolean holdingSelector(Minecraft minecraft) {
        ItemStack main = minecraft.player.getMainHandItem();
        ItemStack off = minecraft.player.getOffhandItem();
        return main.is(ScpClassifiedDirectiveModItems.SCP_ROLE_SELECTOR.get())
                || off.is(ScpClassifiedDirectiveModItems.SCP_ROLE_SELECTOR.get());
    }
}
