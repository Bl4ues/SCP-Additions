package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.effect.Scp714ExposureManager;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import com.bl4ues.scpclassifieddirective.inventory.ScpInventoryMod;
import com.bl4ues.scpclassifieddirective.inventory.network.ContextInteractPacket;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.bl4ues.scpclassifieddirective.scp330.Scp330Hands;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/** Client-side contextual prompt for rescuing a MineZero SCP-714 coma victim. */
public final class Scp714ComaPromptClient {
    private static final ResourceLocation HAND_ICON = new ResourceLocation(
            ScpInventoryMod.MODID, "textures/gui/pickup.png");
    private static final int ICON_SOURCE_SIZE = 128;
    private static final int ICON_SIZE = 82;
    private static final int TEXT_WHITE = 0xFFE8E8E8;
    private static final int TEXT_GRAY = 0xFFB2B3B3;
    private static final float ACTION_TEXT_SCALE = 1.55F;
    private static final float NAME_TEXT_SCALE = 1.85F;
    private static final double MAX_REACH = 2.5D;
    private static final double SOFT_AIM_RADIUS_SQR = 0.72D * 0.72D;
    private static final int CLICK_COOLDOWN_TICKS = 5;

    private static Player target;
    private static boolean useWasDown;
    private static int cooldownTicks;
    private static boolean localComatose;

    private Scp714ComaPromptClient() {
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            clear();
            localComatose = false;
            useWasDown = false;
            cooldownTicks = 0;
            return;
        }

        localComatose = isFloorSleeping(player);
        if (localComatose) {
            clear();
            if (minecraft.screen instanceof AbstractContainerScreen<?>) {
                minecraft.setScreen(null);
            }
            while (Keybinds.STOW_HELD_ITEM.consumeClick()) {
                // A comatose player cannot manipulate carried items through hotkeys.
            }
            useWasDown = minecraft.options.keyUse.isDown();
            return;
        }

        if (minecraft.screen != null || player.isCreative()
                || player.isSpectator() || Scp330Hands.isDisabled(player)
                || !ScpClassifiedDirectiveModulesConfig.customInteractionsEnabledFor(player)) {
            clear();
            useWasDown = minecraft.options.keyUse.isDown();
            return;
        }

        if (cooldownTicks > 0) cooldownTicks--;
        target = findTarget(player);

        boolean useDown = minecraft.options.keyUse.isDown();
        boolean rightClickPressed = useDown && !useWasDown;
        useWasDown = useDown;

        if (target != null && cooldownTicks <= 0 && rightClickPressed) {
            ModNetwork.CHANNEL.sendToServer(new ContextInteractPacket(
                    target.blockPosition(), target.getId(), true,
                    false, false,
                    Scp714ExposureManager.REMOVE_INTERACTION_KEY));
            cooldownTicks = CLICK_COOLDOWN_TICKS;
            clear();
        }
    }

    public static boolean hasActiveTarget() {
        return target != null && target.isAlive() && isFloorSleeping(target);
    }

    public static boolean blocksInteractionLayer() {
        return localComatose || hasActiveTarget();
    }

    public static void clearClientState() {
        clear();
        localComatose = false;
        useWasDown = false;
        cooldownTicks = 0;
    }

    public static void render(GuiGraphics graphics, int screenWidth,
            int screenHeight, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || minecraft.screen != null || minecraft.options.hideGui
                || !hasActiveTarget()) {
            return;
        }

        Vec3 anchor = target.getBoundingBox().getCenter().add(0.0D, 0.16D, 0.0D);
        ScreenPoint point = projectToScreen(minecraft, anchor,
                screenWidth, screenHeight);
        if (point == null) return;

        int screenX = Mth.clamp(point.x(), 28, screenWidth - 28);
        int screenY = Mth.clamp(point.y(), 28, screenHeight - 28);
        int iconX = screenX - Math.round(ICON_SIZE * 0.44F);
        int iconY = screenY - Math.round(ICON_SIZE * 0.40F);
        int textX = iconX + ICON_SIZE + 4;
        int actionY = iconY + 22;
        int nameY = actionY + 32;
        int nameWidth = Math.round(minecraft.font.width(
                ScpFonts.roboto("SCP-714")) * NAME_TEXT_SCALE);

        if (textX + nameWidth > screenWidth - 8) {
            textX = Math.max(8, screenWidth - nameWidth - 8);
            iconX = Math.max(6, textX - ICON_SIZE - 4);
        }
        if (iconX < 6) {
            iconX = 6;
            textX = iconX + ICON_SIZE + 4;
        }
        if (iconY < 6) {
            iconY = 6;
            actionY = iconY + 22;
            nameY = actionY + 32;
        }
        if (iconY + ICON_SIZE > screenHeight - 6) {
            iconY = screenHeight - ICON_SIZE - 6;
            actionY = iconY + 22;
            nameY = actionY + 32;
        }

        drawIcon(graphics, iconX, iconY);
        drawScaledString(graphics, minecraft, "Remove", textX, actionY,
                ACTION_TEXT_SCALE, TEXT_GRAY);
        drawScaledString(graphics, minecraft, "SCP-714", textX, nameY,
                NAME_TEXT_SCALE, TEXT_WHITE);
    }

    private static Player findTarget(LocalPlayer player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        AABB search = player.getBoundingBox()
                .expandTowards(look.scale(MAX_REACH)).inflate(0.85D);
        List<Player> candidates = player.level().getEntitiesOfClass(
                Player.class, search,
                candidate -> candidate != player && candidate.isAlive()
                        && isFloorSleeping(candidate));

        Player best = null;
        double bestScore = Double.MAX_VALUE;
        for (Player candidate : candidates) {
            if (!player.hasLineOfSight(candidate)) continue;
            Vec3 center = candidate.getBoundingBox().getCenter();
            Vec3 toTarget = center.subtract(eye);
            double distance = toTarget.length();
            if (distance > MAX_REACH) continue;
            double alongRay = toTarget.dot(look);
            if (alongRay < 0.0D || alongRay > MAX_REACH) continue;
            Vec3 closest = eye.add(look.scale(alongRay));
            double lineDistanceSqr = closest.distanceToSqr(center);
            boolean directHit = candidate.getBoundingBox().inflate(0.30D)
                    .clip(eye, eye.add(look.scale(MAX_REACH))).isPresent();
            if (!directHit && lineDistanceSqr > SOFT_AIM_RADIUS_SQR) continue;
            double score = lineDistanceSqr + distance * 0.015D;
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private static boolean isFloorSleeping(Player player) {
        if (player == null || player.getPose() != Pose.SLEEPING) return false;
        BlockPos pos = player.blockPosition();
        return !(player.level().getBlockState(pos).getBlock() instanceof BedBlock)
                && !(player.level().getBlockState(pos.below()).getBlock()
                instanceof BedBlock);
    }

    private static ScreenPoint projectToScreen(Minecraft minecraft,
            Vec3 worldPosition, int screenWidth, int screenHeight) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 relative = worldPosition.subtract(camera.getPosition());
        Quaternionf rotation = new Quaternionf(camera.rotation());
        rotation.conjugate();
        Vector3f transformed = new Vector3f((float) relative.x,
                (float) relative.y, (float) relative.z);
        transformed.rotate(rotation);
        if (transformed.z() >= -0.05F) return null;

        double depth = Math.abs(transformed.z());
        double fov = minecraft.options.fov().get();
        double scale = screenHeight
                / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));
        int x = (int) Math.round(screenWidth / 2.0D
                - transformed.x() * scale / depth);
        int y = (int) Math.round(screenHeight / 2.0D
                - transformed.y() * scale / depth);
        return new ScreenPoint(x, y);
    }

    private static void drawIcon(GuiGraphics graphics, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.98F);
        graphics.blit(HAND_ICON, x, y, ICON_SIZE, ICON_SIZE,
                0.0F, 0.0F, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE,
                ICON_SOURCE_SIZE, ICON_SOURCE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawScaledString(GuiGraphics graphics,
            Minecraft minecraft, String text, int x, int y, float scale,
            int color) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.drawString(minecraft.font, ScpFonts.roboto(text),
                0, 0, color, true);
        pose.popPose();
    }

    private static void clear() {
        target = null;
    }

    private record ScreenPoint(int x, int y) {
    }
}
