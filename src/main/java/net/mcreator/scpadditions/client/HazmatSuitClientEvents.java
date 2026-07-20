package net.mcreator.scpadditions.client;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.equipment.HazmatSuitAccess;
import net.mcreator.scpadditions.network.HazmatRemovalInputPacket;

import java.util.IdentityHashMap;
import java.util.Map;

/** Client input and player-render adjustments for the complete Hazmat Suit. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class HazmatSuitClientEvents {
    private static final Map<PlayerModel<?>, OuterLayerVisibility> HIDDEN_OUTER_LAYERS =
            new IdentityHashMap<>();

    private static boolean manualRemovalHeldSent;

    private HazmatSuitClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            manualRemovalHeldSent = false;
            return;
        }

        boolean canContinueHolding = HazmatSuitAccess.isFullyEquipped(player)
                && minecraft.screen == null
                && minecraft.options.keyUse.isDown();

        if (!manualRemovalHeldSent) {
            if (canContinueHolding && player.isCrouching()) {
                ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                        new HazmatRemovalInputPacket(true));
                manualRemovalHeldSent = true;
            }
            return;
        }

        if (!canContinueHolding) {
            ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                    new HazmatRemovalInputPacket(false));
            manualRemovalHeldSent = false;
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!HazmatSuitAccess.isFullyEquipped(event.getEntity())) {
            return;
        }

        PlayerModel<?> model = event.getRenderer().getModel();
        HIDDEN_OUTER_LAYERS.putIfAbsent(model,
                new OuterLayerVisibility(
                        model.hat.visible,
                        model.jacket.visible,
                        model.leftSleeve.visible,
                        model.rightSleeve.visible,
                        model.leftPants.visible,
                        model.rightPants.visible));

        model.hat.visible = false;
        model.jacket.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.visible = false;
        model.leftPants.visible = false;
        model.rightPants.visible = false;
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        PlayerModel<?> model = event.getRenderer().getModel();
        OuterLayerVisibility previous = HIDDEN_OUTER_LAYERS.remove(model);
        if (previous == null) {
            return;
        }

        model.hat.visible = previous.hat;
        model.jacket.visible = previous.jacket;
        model.leftSleeve.visible = previous.leftSleeve;
        model.rightSleeve.visible = previous.rightSleeve;
        model.leftPants.visible = previous.leftPants;
        model.rightPants.visible = previous.rightPants;
    }

    private record OuterLayerVisibility(boolean hat, boolean jacket,
            boolean leftSleeve, boolean rightSleeve,
            boolean leftPants, boolean rightPants) {
    }
}
