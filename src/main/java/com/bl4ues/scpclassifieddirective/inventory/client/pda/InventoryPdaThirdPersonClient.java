package com.bl4ues.scpclassifieddirective.inventory.client.pda;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Draws the physical PDA and a two-handed presentation for nearby observers. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class InventoryPdaThirdPersonClient {
    private static final long OPEN_TRANSITION_NANOS = 820_000_000L;
    private static final long CLOSE_TRANSITION_NANOS = 1_050_000_000L;
    private static final Map<UUID, PresentationState> PRESENTATIONS =
            new HashMap<>();

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

    /** Applied after PlayerModel.setupAnim so vanilla cannot overwrite it. */
    public static void applyArmPose(PlayerModel<?> model, Player player) {
        float progress = progress(player.getUUID());
        if (progress <= 0.0F) return;
        poseArm(model.rightArm,
                lerp(model.rightArm.xRot, -1.38F, progress),
                lerp(model.rightArm.yRot, -0.18F, progress),
                lerp(model.rightArm.zRot, 0.08F, progress));
        poseArm(model.leftArm,
                lerp(model.leftArm.xRot, -1.30F, progress),
                lerp(model.leftArm.yRot, 0.22F, progress),
                lerp(model.leftArm.zRot, -0.08F, progress));
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
    }

    static float progress(Player player) {
        return player == null ? 0.0F : progress(player.getUUID());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().level == null) {
            PRESENTATIONS.clear();
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

    private record PresentationState(boolean opening, long changedNanos,
            float fromProgress) {
        private float progress(long now) {
            long duration = opening
                    ? OPEN_TRANSITION_NANOS : CLOSE_TRANSITION_NANOS;
            float elapsed = Math.min(1.0F,
                    (now - changedNanos) / (float) duration);
            return lerp(fromProgress, opening ? 1.0F : 0.0F,
                    smooth(elapsed));
        }
    }
}
