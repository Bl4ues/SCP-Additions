package com.bl4ues.scpclassifieddirective.inventory.client.pda;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Draws the physical PDA and a two-handed presentation for nearby observers. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class InventoryPdaThirdPersonClient {
    private static final long TRANSITION_NANOS = 700_000_000L;
    private static final Map<UUID, PresentationState> PRESENTATIONS =
            new HashMap<>();
    private static final Map<PlayerModel<?>, ArmPose> ARM_POSES =
            new IdentityHashMap<>();

    private InventoryPdaThirdPersonClient() {
    }

    public static void setOpen(UUID playerId, boolean open) {
        if (playerId == null) return;
        long now = System.nanoTime();
        PresentationState existing = PRESENTATIONS.get(playerId);
        if (existing != null && existing.opening == open) return;
        float current = existing == null ? 0.0F : existing.progress(now);
        if (!open && current <= 0.0F) {
            PRESENTATIONS.remove(playerId);
            return;
        }
        PRESENTATIONS.put(playerId,
                new PresentationState(open, now, current));
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        float progress = progress(event.getEntity().getUUID());
        if (progress <= 0.0F) return;
        PlayerModel<?> model = event.getRenderer().getModel();
        ArmPose original = new ArmPose(model);
        ARM_POSES.put(model, original);
        poseArm(model.rightArm,
                lerp(original.rightX, -1.16F, progress),
                lerp(original.rightY, -0.24F, progress),
                lerp(original.rightZ, 0.12F, progress));
        poseArm(model.leftArm,
                lerp(original.leftX, -1.16F, progress),
                lerp(original.leftY, 0.24F, progress),
                lerp(original.leftZ, -0.12F, progress));
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        PlayerModel<?> model = event.getRenderer().getModel();
        ArmPose previous = ARM_POSES.remove(model);
        float progress = progress(player.getUUID());
        if (progress <= 0.0F) {
            if (previous != null) previous.restore(model);
            return;
        }
        if (player == Minecraft.getInstance().player
                && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            if (previous != null) previous.restore(model);
            return;
        }

        event.getPoseStack().pushPose();
        float hiddenY = player.isCrouching() ? 0.38F : 0.58F;
        float heldY = player.isCrouching() ? 0.86F : 1.06F;
        event.getPoseStack().translate(
                lerp(0.28F, 0.0F, progress),
                lerp(hiddenY, heldY, progress),
                lerp(0.06F, -0.34F, progress));
        event.getPoseStack().mulPose(Axis.YP.rotationDegrees(
                lerp(226.0F, 180.0F, progress)));
        event.getPoseStack().mulPose(Axis.XP.rotationDegrees(
                lerp(12.0F, 68.0F, progress)));
        event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(
                lerp(-5.0F, -90.0F, progress)));
        event.getPoseStack().scale(0.24F, -0.24F, 0.24F);
        // GeoObjectRenderer adds (0.5, 0.51, 0.5) before rendering. Offset
        // the authored screen center so the device, rather than its raw model
        // origin, sits between the player's hands.
        event.getPoseStack().translate(-0.5F,
                -(0.51F + 43.0F / 16.0F), 0.0F);
        InventoryPdaRenderer.INSTANCE.renderThirdPerson(event.getPoseStack(),
                event.getPackedLight());
        event.getPoseStack().popPose();
        if (previous != null) previous.restore(model);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().level == null) {
            PRESENTATIONS.clear();
            ARM_POSES.clear();
            return;
        }
        long now = System.nanoTime();
        Iterator<Map.Entry<UUID, PresentationState>> iterator =
                PRESENTATIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            PresentationState state = iterator.next().getValue();
            if (!state.opening && state.progress(now) <= 0.0F) {
                iterator.remove();
            }
        }
    }

    private static float progress(UUID playerId) {
        PresentationState state = PRESENTATIONS.get(playerId);
        return state == null ? 0.0F : state.progress(System.nanoTime());
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    private static float smooth(float value) {
        value = Math.max(0.0F, Math.min(1.0F, value));
        return value * value * (3.0F - 2.0F * value);
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

    private record PresentationState(boolean opening, long changedNanos,
            float fromProgress) {
        private float progress(long now) {
            float elapsed = Math.min(1.0F,
                    (now - changedNanos) / (float) TRANSITION_NANOS);
            return lerp(fromProgress, opening ? 1.0F : 0.0F,
                    smooth(elapsed));
        }
    }
}
