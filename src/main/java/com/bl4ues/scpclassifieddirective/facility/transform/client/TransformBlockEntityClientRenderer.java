package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Visual bridge for authored BlockEntity blocks placed on an off-grid local
 * grid. The renderer receives the exact local transform while the virtual
 * BlockEntity remains a render host only. Gameplay/ticking that depends on a
 * vanilla BlockPos still requires an explicit transformed runtime adapter.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformBlockEntityClientRenderer {
    private static final double MAX_DISTANCE_SQR = 128.0D * 128.0D;
    private static final Map<CellKey, RenderHost> HOSTS = new HashMap<>();

    private TransformBlockEntityClientRenderer() {
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
        var groups = TransformConstructionClientState.groups(dimension);

        for (TransformGroup group : groups) {
            for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                    : group.cells().entrySet()) {
                BlockState state = entry.getValue();
                if (state == null || state.isAir()
                        || AlarmModule.isController(state)
                        || !(state.getBlock() instanceof EntityBlock entityBlock)) {
                    continue;
                }
                TransformGroup.GridPos cell = entry.getKey();
                Vec3 center = group.cellCenter(cell);
                if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) continue;
                CellKey key = new CellKey(group.id(), cell);
                RenderHost host = HOSTS.get(key);
                if (host == null || !host.state().equals(state)
                        || host.entity().getLevel() != minecraft.level) {
                    BlockEntity entity = entityBlock.newBlockEntity(
                            BlockPos.containing(center), state);
                    if (entity == null) continue;
                    entity.setLevel(minecraft.level);
                    host = new RenderHost(state, entity);
                    HOSTS.put(key, host);
                }
                if (minecraft.getBlockEntityRenderDispatcher().getRenderer(
                        host.entity()) == null) continue;

                pose.pushPose();
                pose.translate(-camera.x, -camera.y, -camera.z);
                pose.translate(group.origin().x, group.origin().y,
                        group.origin().z);
                pose.mulPose(TransformMath.quaternion(group.rotationX(),
                        group.rotationY(), group.rotationZ()));
                pose.translate(cell.x() - 0.5D, cell.y() - 0.5D,
                        cell.z() - 0.5D);
                minecraft.getBlockEntityRenderDispatcher().render(host.entity(),
                        event.getPartialTick(), pose, buffers);
                pose.popPose();
            }
        }

        Set<CellKey> current = groups.stream().flatMap(group ->
                group.cells().entrySet().stream()
                        .filter(entry -> entry.getValue() != null
                                && entry.getValue().getBlock()
                                instanceof EntityBlock
                                && !AlarmModule.isController(entry.getValue()))
                        .map(entry -> new CellKey(group.id(), entry.getKey())))
                .collect(Collectors.toSet());
        HOSTS.keySet().removeIf(key -> !current.contains(key));
    }

    private record CellKey(UUID groupId, TransformGroup.GridPos cell) {
    }

    private record RenderHost(BlockState state, BlockEntity entity) {
    }
}
