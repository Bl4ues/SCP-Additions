package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.init.FacilityMappingItems;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Final client-side guard for Creative authoring tools that repurpose LMB.
 *
 * Canceling the Forge interaction trigger prevents new attacks, but a swing
 * already started on the previous input frame can otherwise keep animating
 * while a drag owns the button. This only resets the local visual swing and
 * never changes server combat state.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BuilderToolAttackGuard {
    private BuilderToolAttackGuard() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isCreative()
                || !minecraft.options.keyAttack.isDown()
                || !holdingAuthoringTool(player)) return;

        player.swinging = false;
        player.swingTime = 0;
    }

    private static boolean holdingAuthoringTool(LocalPlayer player) {
        return authoring(player.getMainHandItem())
                || authoring(player.getOffhandItem());
    }

    private static boolean authoring(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(TransformConstructionModule.getOffGridTool())
                || stack.is(TransformConstructionModule.getSurfaceTool())
                || stack.is(FacilityMappingItems.getTool())
                || stack.is(ScpClassifiedDirectiveModItems.SAFE_ZONE_TOOL.get());
    }
}
