package com.bl4ues.scpclassifieddirective.inventory.client.pda;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.inventory.client.gui.ScpInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Places the physical inventory rig inside the shader-aware hand pass. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class InventoryPdaFirstPersonClient {
    private InventoryPdaFirstPersonClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof ScpInventoryScreen screen)) return;
        if (minecraft.options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }

        // The PDA owns the complete two-handed pose while its screen exists.
        event.setCanceled(true);
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            screen.renderPhysicalPda(event.getPackedLight(), true);
        }
    }
}
