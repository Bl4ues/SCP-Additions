package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.entity.Scp294BlockEntity;
import com.bl4ues.scpclassifieddirective.client.gui.Scp294GuiScreen;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry.Frame;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Physical controls for SCP-294. The vending-machine model owns the large
 * decorative coffee display; only the tiny payment/status readout and temporary
 * paper cup are drawn in world space.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Scp294PhysicalClient {
    /** Close framing around the upper control cluster, not the entire machine. */
    public static final double FOCUS_DISTANCE = 0.90D;

    private static final double FOCUS_CENTER_X = 8.0D / 16.0D;
    private static final double FOCUS_CENTER_Y = 22.6D / 16.0D;
    private static final double FOCUS_CENTER_Z = 0.0D;
    private static final double FOCUS_WIDTH = 14.0D / 16.0D;
    private static final double FOCUS_HEIGHT = 18.0D / 16.0D;

    // Keyboard and payment panel geometry from the authored SCP-294 model.
    private static final double KEYBOARD_CENTER_X = 9.50D / 16.0D;
    private static final double KEYBOARD_CENTER_Y = 13.80D / 16.0D;
    private static final double KEYBOARD_CENTER_Z = 0.035D / 16.0D;
    private static final double KEYBOARD_WIDTH = 8.70D / 16.0D;
    private static final double KEYBOARD_HEIGHT = 10.60D / 16.0D;

    private static final double COIN_CENTER_X = 2.45D / 16.0D;
    private static final double COIN_CENTER_Y = 15.55D / 16.0D;
    private static final double COIN_CENTER_Z = 0.035D / 16.0D;
    private static final double COIN_WIDTH = 4.10D / 16.0D;
    private static final double COIN_HEIGHT = 10.10D / 16.0D;

    // The small horizontal readout at the top of the payment panel. This is the
    // machine's actual text display; the large coffee image above stays purely
    // decorative, matching the real SCP-294 prop and Containment Breach layout.
    private static final double STATUS_CENTER_X = 2.45D / 16.0D;
    private static final double STATUS_CENTER_Y = 18.95D / 16.0D;
    private static final double STATUS_CENTER_Z = -0.015D / 16.0D;
    private static final double STATUS_WIDTH = 2.90D / 16.0D;
    private static final double STATUS_HEIGHT = 0.82D / 16.0D;
    private static final int STATUS_W = 112;
    private static final int STATUS_H = 18;
    private static final int STATUS_TEXT = 0xFF203426;
    private static final double STATUS_TEXT_EPSILON = 0.0022D;

    // Dispensing recess on the lower front. The empty paper cup is visible here
    // only while the configured dispense delay is running.
    private static final double CUP_CENTER_X = 11.50D / 16.0D;
    private static final double CUP_CENTER_Y = 7.35D / 16.0D;
    private static final double CUP_CENTER_Z = -0.04D / 16.0D;
    private static final float CUP_SCALE = 0.20F;

    private Scp294PhysicalClient() { }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        BlockEntityType<Scp294BlockEntity> normal = (BlockEntityType)
                ScpClassifiedDirectiveModBlockEntities.SCP_294.get();
        event.registerBlockEntityRenderer(normal, Renderer::new);
    }

    public static Frame focusFrame(BlockPos pos, Direction facing) {
        return frame(pos, facing, FOCUS_CENTER_X, FOCUS_CENTER_Y,
                FOCUS_CENTER_Z, FOCUS_WIDTH, FOCUS_HEIGHT);
    }

    private static Frame keyboardFrame(BlockPos pos, Direction facing) {
        return frame(pos, facing, KEYBOARD_CENTER_X, KEYBOARD_CENTER_Y,
                KEYBOARD_CENTER_Z, KEYBOARD_WIDTH, KEYBOARD_HEIGHT);
    }

    private static Frame coinFrame(BlockPos pos, Direction facing) {
        return frame(pos, facing, COIN_CENTER_X, COIN_CENTER_Y,
                COIN_CENTER_Z, COIN_WIDTH, COIN_HEIGHT);
    }

    private static Frame statusFrame(BlockPos pos, Direction facing) {
        return frame(pos, facing, STATUS_CENTER_X, STATUS_CENTER_Y,
                STATUS_CENTER_Z, STATUS_WIDTH, STATUS_HEIGHT);
    }

    private static Frame cupFrame(BlockPos pos, Direction facing) {
        return frame(pos, facing, CUP_CENTER_X, CUP_CENTER_Y,
                CUP_CENTER_Z, 0.01D, 0.01D);
    }

    private static Frame frame(BlockPos pos, Direction facing,
            double centerX, double centerY, double centerZ,
            double width, double height) {
        return PhysicalBlockScreenGeometry.fromNorthFacing(pos, facing,
                centerX, centerY, centerZ, 0.0D, width, height);
    }

    /** Resolve the actual control surface underneath the OS cursor. */
    public static Control controlAt(BlockPos pos, Direction facing,
            double mouseX, double mouseY, int viewportWidth, int viewportHeight) {
        if (pos == null || viewportWidth <= 0 || viewportHeight <= 0) {
            return Control.NONE;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 origin = camera.getPosition();
        Vec3 forward = Vec3.directionFromRotation(camera.getXRot(),
                camera.getYRot()).normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-8D) return Control.NONE;
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        double nx = mouseX / viewportWidth * 2.0D - 1.0D;
        double ny = 1.0D - mouseY / viewportHeight * 2.0D;
        double tan = Math.tan(Math.toRadians(
                TeslaTerminalFocusClient.currentFovDegrees()) * 0.5D);
        double aspect = viewportWidth / (double) viewportHeight;
        Vec3 ray = forward.add(right.scale(nx * tan * aspect))
                .add(up.scale(ny * tan)).normalize();

        Control best = Control.NONE;
        double bestDistance = Double.POSITIVE_INFINITY;
        double hit = hitDistance(origin, ray, statusFrame(pos, facing));
        if (hit < bestDistance) {
            bestDistance = hit;
            best = Control.INPUT;
        }
        hit = hitDistance(origin, ray, keyboardFrame(pos, facing));
        if (hit < bestDistance) {
            bestDistance = hit;
            best = Control.ENTER;
        }
        hit = hitDistance(origin, ray, coinFrame(pos, facing));
        if (hit < bestDistance) {
            best = Control.COIN;
        }
        return best;
    }

    private static double hitDistance(Vec3 origin, Vec3 ray, Frame frame) {
        double denominator = ray.dot(frame.outward());
        if (Math.abs(denominator) < 1.0E-6D) {
            return Double.POSITIVE_INFINITY;
        }
        double distance = frame.center().subtract(origin)
                .dot(frame.outward()) / denominator;
        if (distance <= 0.0D) return Double.POSITIVE_INFINITY;
        Vec3 delta = origin.add(ray.scale(distance)).subtract(frame.center());
        double localX = delta.dot(frame.right());
        double localY = delta.dot(frame.up());
        if (Math.abs(localX) > frame.width() * 0.5D
                || Math.abs(localY) > frame.height() * 0.5D) {
            return Double.POSITIVE_INFINITY;
        }
        return distance;
    }

    public enum Control {
        NONE,
        INPUT,
        ENTER,
        COIN
    }

    private static final class Renderer
            implements BlockEntityRenderer<Scp294BlockEntity> {
        private final Font font;

        private Renderer(BlockEntityRendererProvider.Context context) {
            this.font = context.getFont();
        }

        @Override
        public void render(Scp294BlockEntity machine, float partialTick,
                PoseStack poseStack, MultiBufferSource buffers,
                int packedLight, int packedOverlay) {
            Direction facing = facing(machine.getBlockState());
            BlockPos pos = machine.getBlockPos();
            Scp294GuiScreen screen = activeScreen(pos);

            String status;
            if (machine.isPouring()) {
                status = "POURING";
            } else if (machine.isOutOfRange()) {
                status = "OUT OF RANGE";
            } else if (screen != null) {
                boolean hasCoin = screen.physicalHasCoinInserted();
                String order = screen.physicalOrder();
                if (!hasCoin) {
                    status = "INSERT $0.50";
                } else if (order.isBlank()) {
                    status = "ENTER ORDER";
                } else {
                    status = order;
                    if (screen.physicalInputFocused()
                            && (System.currentTimeMillis() / 400L) % 2L == 0L) {
                        status += "_";
                    }
                }
            } else if (machine.getItem(0).is(ScpClassifiedDirectiveModItems.COIN.get())) {
                status = "ENTER ORDER";
            } else {
                status = "INSERT $0.50";
            }

            renderStatus(font, poseStack, buffers, pos, facing, status);
            if (machine.isPouring()) {
                renderEmptyCup(machine, poseStack, buffers, pos, facing);
            }
        }

        @Override
        public boolean shouldRenderOffScreen(Scp294BlockEntity blockEntity) {
            return true;
        }
    }

    private static Direction facing(BlockState state) {
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
    }

    private static void renderStatus(Font font, PoseStack poseStack,
            MultiBufferSource buffers, BlockPos pos, Direction facing,
            String rawStatus) {
        Frame frame = statusFrame(pos, facing);
        Vec3 topLeft = local(frame.point(-0.5D, 0.5D,
                STATUS_TEXT_EPSILON), pos);
        float pixelScaleX = (float) (frame.width() / STATUS_W);
        float pixelScaleY = (float) (frame.height() / STATUS_H);
        float depthScale = Math.min(pixelScaleX, pixelScaleY);
        String status = fitToWidth(font, rawStatus == null ? "" : rawStatus,
                STATUS_W - 6);

        poseStack.pushPose();
        poseStack.translate(topLeft.x, topLeft.y, topLeft.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(textYaw(facing)));
        poseStack.scale(pixelScaleX, -pixelScaleY, depthScale);
        float x = Math.max(3.0F, (STATUS_W - font.width(status)) * 0.5F);
        float y = (STATUS_H - 9.0F) * 0.5F;
        font.drawInBatch(Component.literal(status), x, y, STATUS_TEXT, false,
                poseStack.last().pose(), buffers, Font.DisplayMode.NORMAL,
                0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static String fitToWidth(Font font, String raw, int width) {
        String value = raw;
        while (!value.isEmpty() && font.width(value) > width) {
            value = value.substring(1);
        }
        return value;
    }

    private static void renderEmptyCup(Scp294BlockEntity machine,
            PoseStack poseStack, MultiBufferSource buffers, BlockPos pos,
            Direction facing) {
        if (machine.getLevel() == null) return;
        Frame anchor = cupFrame(pos, facing);
        Vec3 center = local(anchor.center().add(anchor.outward().scale(0.006D)), pos);
        ItemStack cup = new ItemStack(ScpClassifiedDirectiveModItems.EMPTY_CUP.get());

        poseStack.pushPose();
        poseStack.translate(center.x, center.y, center.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(textYaw(facing)));
        poseStack.scale(CUP_SCALE, CUP_SCALE, CUP_SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(cup,
                ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, poseStack, buffers,
                machine.getLevel(), 0);
        poseStack.popPose();
    }

    private static Scp294GuiScreen activeScreen(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof Scp294GuiScreen screen
                && screen.isFor(pos)) {
            return screen;
        }
        return null;
    }

    private static float textYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case EAST -> 90.0F;
            case SOUTH -> 0.0F;
            case WEST -> -90.0F;
            default -> 180.0F;
        };
    }

    private static Vec3 local(Vec3 world, BlockPos pos) {
        return world.subtract(pos.getX(), pos.getY(), pos.getZ());
    }
}
