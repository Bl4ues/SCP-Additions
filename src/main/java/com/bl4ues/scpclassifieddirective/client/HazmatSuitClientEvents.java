package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.equipment.HazmatSuitAccess;
import com.bl4ues.scpclassifieddirective.network.HazmatRemovalInputPacket;

import java.util.IdentityHashMap;
import java.util.Map;

/** Client input and player-render adjustments for the complete Hazmat Suit. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
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
                ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                        new HazmatRemovalInputPacket(true));
                manualRemovalHeldSent = true;
            }
            return;
        }

        if (!canContinueHolding) {
            ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
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
        if (!HIDDEN_OUTER_LAYERS.containsKey(model)) {
            HIDDEN_OUTER_LAYERS.put(model, hideOuterLayers(model));
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        PlayerModel<?> model = event.getRenderer().getModel();
        OuterLayerVisibility previous = HIDDEN_OUTER_LAYERS.remove(model);
        if (previous != null) {
            restoreOuterLayers(model, previous);
        }
    }

    static OuterLayerVisibility hideOuterLayers(PlayerModel<?> model) {
        OuterLayerVisibility previous = new OuterLayerVisibility(
                model.hat.visible,
                model.jacket.visible,
                model.leftSleeve.visible,
                model.rightSleeve.visible,
                model.leftPants.visible,
                model.rightPants.visible);

        model.hat.visible = false;
        model.jacket.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.visible = false;
        model.leftPants.visible = false;
        model.rightPants.visible = false;
        return previous;
    }

    static void restoreOuterLayers(PlayerModel<?> model,
            OuterLayerVisibility previous) {
        model.hat.visible = previous.hat;
        model.jacket.visible = previous.jacket;
        model.leftSleeve.visible = previous.leftSleeve;
        model.rightSleeve.visible = previous.rightSleeve;
        model.leftPants.visible = previous.leftPants;
        model.rightPants.visible = previous.rightPants;
    }

    record OuterLayerVisibility(boolean hat, boolean jacket,
            boolean leftSleeve, boolean rightSleeve,
            boolean leftPants, boolean rightPants) {
    }
}
