package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.ObjectContainmentUnitModule;
import com.bl4ues.scpclassifieddirective.keycard.KeycardAccess;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderLevels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Client-only visual swipe; authorization remains entirely server-side. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class KeycardSwipeClient {
    public static final long DURATION_NANOS = 600_000_000L;

    private static final Vec3 RIGHT_START = pixels(-1.875D, 2.8D, 14.5D);
    private static final Vec3 RIGHT_END = pixels(-1.875D, -0.1D, 14.5D);
    private static final Vec3 LEFT_START = pixels(18.025D, 2.8D, 14.5D);
    private static final Vec3 LEFT_END = pixels(18.025D, -0.1D, 14.5D);

    /* Exact authored swipe path supplied for the OCU reader. */
    private static final Vec3 OCU_START = pixels(-10.3D, 15.9D, 2.95D);
    private static final Vec3 OCU_END = pixels(-10.3D, 13.05D, -1.15D);

    /*
     * Raw keycard geometry is x=6.8..9.1, y=1..4.6, z=7.5..7.6. The supplied
     * coordinates describe the physical line followed by the same thin edge of
     * the card on every reader. Keep that x=6.8 edge on the authored path.
     */
    private static final Vec3 SLOT_EDGE = pixels(6.8D, 2.8D, 7.55D);
    private static final Vec3 RENDER_COMPENSATION = compensation(SLOT_EDGE);

    /*
     * This is the orientation that already works on the wall readers: the card
     * turns edge-on by 90 degrees around Y, then flips 180 degrees around Z so
     * its narrow/top end follows the reader arrow. The OCU uses the exact same
     * physical edge and orientation; only its measured swipe path differs.
     */
    private static final float EDGE_INTO_SLOT_YAW = 90.0F;
    private static final float CARD_UPSIDE_DOWN_ROLL = 180.0F;

    private static final Map<BlockPos, Swipe> SWIPES = new HashMap<>();

    private KeycardSwipeClient() {
    }

    public static synchronized void start(BlockPos pos, int keycardLevel,
            boolean objectContainmentUnit) {
        if (pos == null) return;
        SWIPES.put(pos.immutable(), new Swipe(System.nanoTime(),
                Math.max(1, Math.min(6, keycardLevel)), objectContainmentUnit));
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        long now = System.nanoTime();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers().bufferSource();

        synchronized (KeycardSwipeClient.class) {
            Iterator<Map.Entry<BlockPos, Swipe>> iterator =
                    SWIPES.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, Swipe> entry = iterator.next();
                BlockPos pos = entry.getKey();
                Swipe swipe = entry.getValue();
                double raw = (now - swipe.startedNanos)
                        / (double) DURATION_NANOS;
                if (raw >= 1.0D) {
                    iterator.remove();
                    continue;
                }
                if (raw < 0.0D || !minecraft.level.hasChunkAt(pos)) continue;

                BlockState state = minecraft.level.getBlockState(pos);
                if (!isStillCompatible(state, swipe.objectContainmentUnit)) {
                    iterator.remove();
                    continue;
                }

                double t = smooth(Mth.clamp(raw, 0.0D, 1.0D));
                renderSwipe(minecraft, poseStack, buffers, camera, pos, state,
                        swipe, t);
            }
        }
        buffers.endBatch();
    }

    private static void renderSwipe(Minecraft minecraft, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, Vec3 camera, BlockPos pos,
            BlockState state, Swipe swipe, double progress) {
        Direction facing = horizontalFacing(state);
        Vec3 start;
        Vec3 end;
        if (swipe.objectContainmentUnit) {
            start = OCU_START;
            end = OCU_END;
        } else {
            KeycardReaderLevels.ReaderDescriptor descriptor =
                    KeycardReaderLevels.describe(state);
            if (descriptor == null) return;
            boolean right = descriptor.side() == KeycardReaderLevels.Side.RIGHT;
            start = right ? RIGHT_START : LEFT_START;
            end = right ? RIGHT_END : LEFT_END;
        }

        Vec3 local = start.lerp(end, progress);
        Vec3 absoluteLocal = swipe.objectContainmentUnit
                ? centeredModelToBlockLocal(local) : local;
        Vec3 world = localToWorld(pos, absoluteLocal, facing).subtract(camera);
        ItemStack card = KeycardAccess.visualStack(swipe.keycardLevel);

        poseStack.pushPose();
        poseStack.translate(world.x, world.y, world.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(modelYaw(facing)));

        // Same physical card edge and orientation on wall readers and the OCU.
        poseStack.mulPose(Axis.YP.rotationDegrees(EDGE_INTO_SLOT_YAW));
        poseStack.mulPose(Axis.ZP.rotationDegrees(CARD_UPSIDE_DOWN_ROLL));
        poseStack.translate(RENDER_COMPENSATION.x,
                RENDER_COMPENSATION.y, RENDER_COMPENSATION.z);

        int light = LevelRenderer.getLightColor(minecraft.level, pos);
        minecraft.getItemRenderer().renderStatic(card, ItemDisplayContext.NONE,
                light, OverlayTexture.NO_OVERLAY, poseStack, buffers,
                minecraft.level, 0);
        poseStack.popPose();
    }

    private static Vec3 compensation(Vec3 anchor) {
        return new Vec3(0.5D - anchor.x,
                0.5D - anchor.y,
                0.5D - anchor.z);
    }

    private static Vec3 centeredModelToBlockLocal(Vec3 centered) {
        return new Vec3(0.5D + centered.x, centered.y, 0.5D + centered.z);
    }

    private static boolean isStillCompatible(BlockState state, boolean ocu) {
        return ocu ? state.is(ObjectContainmentUnitModule.UNIT.get())
                : KeycardReaderLevels.describe(state) != null;
    }

    private static Direction horizontalFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (facing.getAxis().isHorizontal()) return facing;
        }
        return Direction.NORTH;
    }

    private static float modelYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    private static Vec3 localToWorld(BlockPos pos, Vec3 local,
            Direction facing) {
        double x = local.x - 0.5D;
        double z = local.z - 0.5D;
        Vec3 rotated = switch (facing) {
            case EAST -> new Vec3(-z, local.y, x);
            case SOUTH -> new Vec3(-x, local.y, -z);
            case WEST -> new Vec3(z, local.y, -x);
            default -> new Vec3(x, local.y, z);
        };
        return new Vec3(pos.getX() + rotated.x + 0.5D,
                pos.getY() + rotated.y,
                pos.getZ() + rotated.z + 0.5D);
    }

    private static double smooth(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static Vec3 pixels(double x, double y, double z) {
        return new Vec3(x / 16.0D, y / 16.0D, z / 16.0D);
    }

    @SubscribeEvent
    public static synchronized void onLogout(
            ClientPlayerNetworkEvent.LoggingOut event) {
        SWIPES.clear();
    }

    private record Swipe(long startedNanos, int keycardLevel,
            boolean objectContainmentUnit) {
    }
}
