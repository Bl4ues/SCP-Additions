package com.bl4ues.scpclassifieddirective.safezone.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.safezone.network.SafeZoneNetwork;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Converts the tool's non-destructive left click into a selection packet. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SafeZoneToolClientEvents {
    private static boolean attackLatch;

    private SafeZoneToolClientEvents() {
    }

    @SubscribeEvent
    public static void onAttack(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        var player = Minecraft.getInstance().player;
        if (player == null || !player.canUseGameMasterBlocks()
                || !(player.getMainHandItem().is(
                        ScpClassifiedDirectiveModItems.SAFE_ZONE_TOOL.get())
                || player.getOffhandItem().is(
                        ScpClassifiedDirectiveModItems.SAFE_ZONE_TOOL.get()))) {
            return;
        }
        event.setCanceled(true);
        if (attackLatch) return;
        attackLatch = true;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK) {
            begin(hit.getBlockPos());
        }
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().is(
                        ScpClassifiedDirectiveModItems.SAFE_ZONE_TOOL.get())
                || !event.getEntity().canUseGameMasterBlocks()) {
            return;
        }
        if (!attackLatch) {
            attackLatch = true;
            begin(event.getPos());
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Minecraft.getInstance().options.keyAttack.isDown()) {
            attackLatch = false;
        }
    }

    private static void begin(net.minecraft.core.BlockPos pos) {
        if (pos == null) return;
        SafeZoneClientState.setSelectionStart(pos);
        SafeZoneNetwork.requestSelectionStart(pos);
    }

}
