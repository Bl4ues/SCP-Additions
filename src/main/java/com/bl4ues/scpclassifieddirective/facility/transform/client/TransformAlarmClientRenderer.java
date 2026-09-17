package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reuses the real Alarm BlockEntityRenderer for Alarm states stored in an
 * off-grid local grid. The BlockEntity exists only as a client render host; the
 * authoritative active state, sound and light are driven by TransformAlarmRuntime.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformAlarmClientRenderer {
    private static final double MAX_DISTANCE_SQR = 96.0D * 96.0D;
    private static final Map<CellKey, AlarmModule.AlarmBlockEntity> HOSTS =
            new HashMap<>();

    private TransformAlarmClientRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            HOSTS.clear();
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers()
                .bufferSource();
        var dimension = minecraft.level.dimension().location();

        for (TransformGroup group : TransformConstructionClientState.groups(
                dimension)) {
            for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                    : group.cells().entrySet()) {
                BlockState state = entry.getValue();
                if (state == null || !AlarmModule.isController(state)) continue;
                TransformGroup.GridPos cell = entry.getKey();
                Vec3 center = group.cellCenter(cell);
                if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) continue;

                CellKey key = new CellKey(group.id(), cell);
                BlockPos hostPos = BlockPos.containing(center);
                AlarmModule.AlarmBlockEntity alarm = HOSTS.get(key);
                if (alarm == null || alarm.getLevel() != minecraft.level
                        || !alarm.getBlockPos().equals(hostPos)
                        || alarm.getBlockState() != state) {
                    alarm = new AlarmModule.AlarmBlockEntity(hostPos, state);
                    alarm.setLevel(minecraft.level);
                    HOSTS.put(key, alarm);
                }

                pose.pushPose();
                pose.translate(-camera.x, -camera.y, -camera.z);
                pose.translate(group.origin().x, group.origin().y,
                        group.origin().z);
                pose.mulPose(TransformMath.quaternion(group.rotationX(),
                        group.rotationY(), group.rotationZ()));
                pose.translate(cell.x() - 0.5D, cell.y() - 0.5D,
                        cell.z() - 0.5D);
                minecraft.getBlockEntityRenderDispatcher().render(alarm,
                        event.getPartialTick(), pose, buffers);
                pose.popPose();
            }
        }
        // Drop render hosts for cells that were deleted. Keeping only current
        // ids avoids an unbounded cache in long construction sessions.
        HOSTS.keySet().removeIf(key -> !stillExists(dimension, key));
    }

    private static boolean stillExists(
            net.minecraft.resources.ResourceLocation dimension, CellKey key) {
        TransformGroup group = TransformConstructionClientState.group(key.groupId());
        if (group == null || !group.dimension().equals(dimension)) return false;
        BlockState state = group.cells().get(key.cell());
        return state != null && AlarmModule.isController(state);
    }

    private record CellKey(UUID groupId, TransformGroup.GridPos cell) {
    }
}
