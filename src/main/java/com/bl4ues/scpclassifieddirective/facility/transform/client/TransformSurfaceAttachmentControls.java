package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Selection;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SelectionType;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformAttachmentNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

/** Explicitly switches a curved-surface payload between bent and rigid modes. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformSurfaceAttachmentControls {
    private static final double MAX_SLOT_DISTANCE_SQR = 2.25D;

    private TransformSurfaceAttachmentControls() {
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS
                || event.getKey() != GLFW.GLFW_KEY_V) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || !player.isCreative()
                || !player.getMainHandItem().is(
                        TransformConstructionModule.getSurfaceTool())) return;
        Selection selection = TransformConstructionClientState.selection();
        if (selection == null || selection.type() != SelectionType.SURFACE) return;
        ConstructionSurface surface = TransformConstructionClientState.surface(
                selection.id());
        if (surface == null || surface.attachments().isEmpty()) return;

        Vec3 target = targetPoint(minecraft);
        if (target == null) return;
        ConstructionSurface.SurfaceSlot best = null;
        double bestDistance = MAX_SLOT_DISTANCE_SQR;
        for (Map.Entry<ConstructionSurface.SurfaceSlot,
                ConstructionSurface.SurfaceAttachment> entry
                : surface.attachments().entrySet()) {
            ConstructionSurface.SurfaceSlot slot = entry.getKey();
            double u = (slot.column() + 0.5D) / surface.columns();
            double v = (slot.row() + 0.5D) / surface.rows();
            double distance = surface.gridPoint(u, v).distanceToSqr(target);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = slot;
            }
        }
        if (best == null) {
            status("Aim at a placed surface block to change its bend mode");
            return;
        }
        ConstructionSurface.SurfaceAttachment attachment =
                surface.attachments().get(best);
        if (!attachment.deform()
                && !TransformSurfaceGeometry.canDeform(attachment.state())) {
            status("Functional fixture: RIGID local surface frame");
            return;
        }
        boolean deform = !attachment.deform();
        TransformAttachmentNetwork.setMode(surface.id(), best, deform);
        status(deform
                ? "Surface block: DEFORM with curve"
                : "Surface block: RIGID, follow local surface rotation");
    }

    private static Vec3 targetPoint(Minecraft minecraft) {
        HitResult hit = minecraft.hitResult;
        if (hit instanceof BlockHitResult blockHit) return blockHit.getLocation();
        LocalPlayer player = minecraft.player;
        return player == null ? null : player.getEyePosition().add(
                player.getViewVector(1.0F).scale(6.0D));
    }

    private static void status(String text) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.literal(text),
                true);
    }
}
