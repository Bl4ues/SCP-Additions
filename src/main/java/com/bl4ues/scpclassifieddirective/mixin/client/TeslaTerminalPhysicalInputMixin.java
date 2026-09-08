package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.block.TeslaTerminalBlockBlock;
import com.bl4ues.scpclassifieddirective.client.TeslaTerminalFocusClient;
import com.bl4ues.scpclassifieddirective.client.gui.TeslaTerminalScreen;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry.Frame;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Maps the normal Screen cursor onto the actual 3D CRT plane. Earlier versions
 * approximated the physical monitor as a centered 2D rectangle; that looked
 * close enough but its hitboxes drifted away from the rendered buttons. This
 * version ray-casts the cursor through the focused camera and intersects the
 * authored tilted screen itself, so visual pixels and input share one surface.
 */
@Mixin(value = TeslaTerminalScreen.class, remap = false)
public abstract class TeslaTerminalPhysicalInputMixin {
    private double scpclassifieddirective$mouseX;
    private double scpclassifieddirective$mouseY;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void scpclassifieddirective$waitForStableFocus(double mouseX,
            double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        scpclassifieddirective$mouseX = mouseX;
        scpclassifieddirective$mouseY = mouseY;
        if (button == 0 && TeslaTerminalFocusClient.active()
                && !TeslaTerminalFocusClient.inputReady()) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(method = "mouseClicked",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/gui/TeslaTerminalScreen;textureX(D)D"))
    private double scpclassifieddirective$physicalTextureX(
            TeslaTerminalScreen screen, double mouseX) {
        return scpclassifieddirective$physicalCoordinates(screen, mouseX,
                scpclassifieddirective$mouseY)[0];
    }

    @Redirect(method = "mouseClicked",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/gui/TeslaTerminalScreen;textureY(D)D"))
    private double scpclassifieddirective$physicalTextureY(
            TeslaTerminalScreen screen, double mouseY) {
        return scpclassifieddirective$physicalCoordinates(screen,
                scpclassifieddirective$mouseX, mouseY)[1];
    }

    private double[] scpclassifieddirective$physicalCoordinates(
            TeslaTerminalScreen screen, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return new double[] {-1.0E6D, -1.0E6D};
        }

        BlockState state = minecraft.level.getBlockState(screen.terminalPos());
        Direction facing = state.hasProperty(TeslaTerminalBlockBlock.FACING)
                ? state.getValue(TeslaTerminalBlockBlock.FACING)
                : Direction.NORTH;
        Frame frame = TeslaTerminalFocusClient.frame(screen.terminalPos(), facing);
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 origin = camera.getPosition();
        Vec3 forward = new Vec3(camera.getLookVector()).normalize();
        Vec3 right = new Vec3(camera.getLeftVector()).scale(-1.0D).normalize();
        Vec3 up = new Vec3(camera.getUpVector()).normalize();

        double guiWidth = Math.max(1.0D,
                minecraft.getWindow().getGuiScaledWidth());
        double guiHeight = Math.max(1.0D,
                minecraft.getWindow().getGuiScaledHeight());
        double ndcX = mouseX / guiWidth * 2.0D - 1.0D;
        double ndcY = 1.0D - mouseY / guiHeight * 2.0D;
        double aspect = minecraft.getWindow().getScreenWidth()
                / (double) Math.max(1, minecraft.getWindow().getScreenHeight());
        double tangent = Math.tan(Math.toRadians(60.0D * 0.5D));

        Vec3 ray = forward
                .add(right.scale(ndcX * tangent * aspect))
                .add(up.scale(ndcY * tangent))
                .normalize();
        double denominator = ray.dot(frame.outward());
        if (Math.abs(denominator) <= 1.0E-7D) {
            return new double[] {-1.0E6D, -1.0E6D};
        }
        double distance = frame.center().subtract(origin)
                .dot(frame.outward()) / denominator;
        if (!Double.isFinite(distance) || distance <= 0.0D) {
            return new double[] {-1.0E6D, -1.0E6D};
        }

        Vec3 local = origin.add(ray.scale(distance)).subtract(frame.center());
        double u = 0.5D + local.dot(frame.right()) / frame.width();
        double v = 0.5D - local.dot(frame.up()) / frame.height();
        return new double[] {
                u * TeslaTerminalScreen.TEX_W,
                v * TeslaTerminalScreen.TEX_H
        };
    }

    /**
     * Empty CRT space must not sound like a successful control press. Valid
     * controls still call playSelect() after their real pixel hitbox is reached.
     */
    @Redirect(method = "mouseClicked",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/gui/TeslaTerminalScreen;playRandomClick()V"))
    private void scpclassifieddirective$onlySoundValidControls(
            TeslaTerminalScreen screen) {
        // Intentionally empty. Valid controls still emit playSelect().
    }
}
