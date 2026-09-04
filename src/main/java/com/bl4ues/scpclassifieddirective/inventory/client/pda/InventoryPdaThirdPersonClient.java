package com.bl4ues.scpclassifieddirective.inventory.client.pda;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Draws a privacy-safe static PDA for nearby observers. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class InventoryPdaThirdPersonClient {
    private static final Set<UUID> OPEN_PLAYERS = new HashSet<>();
    private static final Map<PlayerModel<?>, ArmPose> ARM_POSES =
            new IdentityHashMap<>();

    private InventoryPdaThirdPersonClient() {
    }

    public static void setOpen(UUID playerId, boolean open) {
        if (playerId == null) return;
        if (open) OPEN_PLAYERS.add(playerId);
        else OPEN_PLAYERS.remove(playerId);
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!OPEN_PLAYERS.contains(event.getEntity().getUUID())) return;
        PlayerModel<?> model = event.getRenderer().getModel();
        ARM_POSES.put(model, new ArmPose(model));
        poseArm(model.rightArm, -1.16F, -0.24F, 0.12F);
        poseArm(model.leftArm, -1.16F, 0.24F, -0.12F);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        AbstractClientPlayer player = event.getEntity();
        PlayerModel<?> model = event.getRenderer().getModel();
        ArmPose previous = ARM_POSES.remove(model);
        if (!OPEN_PLAYERS.contains(player.getUUID())) return;
        if (player == Minecraft.getInstance().player
                && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            if (previous != null) previous.restore(model);
            return;
        }

        event.getPoseStack().pushPose();
        event.getPoseStack().translate(0.0D,
                player.isCrouching() ? 0.82D : 1.02D, -0.34D);
        event.getPoseStack().mulPose(Axis.YP.rotationDegrees(180.0F));
        event.getPoseStack().mulPose(Axis.XP.rotationDegrees(72.0F));
        event.getPoseStack().scale(0.022F, -0.022F, 0.022F);
        InventoryPdaRenderer.INSTANCE.renderThirdPerson(event.getPoseStack());
        event.getPoseStack().popPose();
        if (previous != null) previous.restore(model);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && Minecraft.getInstance().level == null) {
            OPEN_PLAYERS.clear();
            ARM_POSES.clear();
        }
    }

    private static void poseArm(ModelPart arm, float x, float y, float z) {
        arm.xRot = x;
        arm.yRot = y;
        arm.zRot = z;
    }

    private record ArmPose(float rightX, float rightY, float rightZ,
            float leftX, float leftY, float leftZ) {
        private ArmPose(PlayerModel<?> model) {
            this(model.rightArm.xRot, model.rightArm.yRot,
                    model.rightArm.zRot, model.leftArm.xRot,
                    model.leftArm.yRot, model.leftArm.zRot);
        }

        private void restore(PlayerModel<?> model) {
            poseArm(model.rightArm, rightX, rightY, rightZ);
            poseArm(model.leftArm, leftX, leftY, leftZ);
            model.rightSleeve.copyFrom(model.rightArm);
            model.leftSleeve.copyFrom(model.leftArm);
        }
    }
}
