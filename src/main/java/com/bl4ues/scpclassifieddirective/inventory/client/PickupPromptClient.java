package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.inventory.ScpInventoryMod;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.bl4ues.scpclassifieddirective.inventory.network.PickupItemPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public final class PickupPromptClient {
    private static final ResourceLocation PICKUP_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/pickup.png");

    private static final int ICON_SOURCE_SIZE = 128;
    private static final int ICON_SIZE = 82;
    private static final float PICKUP_HOTSPOT_X = 0.44F;
    private static final float PICKUP_HOTSPOT_Y = 0.40F;
    private static final int TEXT_WHITE = 0xFFE8E8E8;
    private static final int TEXT_GRAY = 0xFFB2B3B3;
    private static final float PICKUP_TEXT_SCALE = 1.55F;
    private static final float ITEM_TEXT_SCALE = 1.85F;
    private static final double MAX_PICKUP_REACH = 2.25D;
    private static final double SOFT_AIM_RADIUS_SQR = 0.58D * 0.58D;
    private static final int PICKUP_CLICK_COOLDOWN_TICKS = 5;
    private static final boolean ITEM_PHYSIC_LOADED = ModList.get().isLoaded("itemphysic");

    private static ItemEntity target;
    private static boolean targetWasGlowing;
    private static boolean useWasDown = false;
    private static int pickupCooldownTicks = 0;

    private PickupPromptClient() {
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null || player.isCreative() || player.isSpectator()) {
            clearTarget();
            useWasDown = false;
            pickupCooldownTicks = 0;
            return;
        }

        if (pickupCooldownTicks > 0) {
            pickupCooldownTicks--;
        }

        setTarget(findTarget(mc, player));

        boolean useDown = mc.options.keyUse.isDown();
        boolean pressedThisTick = useDown && !useWasDown;
        useWasDown = useDown;

        if (target != null && pressedThisTick && pickupCooldownTicks <= 0) {
            InventoryFullOverlay.armPickupAttempt(target.getItem());
            ModNetwork.CHANNEL.sendToServer(new PickupItemPacket(target.getId()));
            pickupCooldownTicks = PICKUP_CLICK_COOLDOWN_TICKS;
            clearTarget();
        }
    }

    public static boolean hasActiveTarget() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && mc.screen == null && target != null && target.isAlive();
    }

    public static void render(GuiGraphics g, int screenWidth, int screenHeight, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.options.hideGui || target == null || !target.isAlive()) {
            return;
        }

        ScreenPoint point = projectToScreen(mc,
                renderedItemCenter(target, partialTick),
                screenWidth, screenHeight);
        if (point == null) return;

        int screenX = Mth.clamp(point.x(), 28, screenWidth - 28);
        int screenY = Mth.clamp(point.y(), 28, screenHeight - 28);

        int iconX = screenX - Math.round(ICON_SIZE * PICKUP_HOTSPOT_X);
        int iconY = screenY - Math.round(ICON_SIZE * PICKUP_HOTSPOT_Y);
        int textX = iconX + ICON_SIZE + 4;
        int pickupY = iconY + 22;
        int itemY = pickupY + 32;

        int itemWidth = Math.round(mc.font.width(ScpFonts.roboto(target.getItem().getHoverName().getString())) * ITEM_TEXT_SCALE);
        if (textX + itemWidth > screenWidth - 8) {
            textX = Math.max(8, screenWidth - itemWidth - 8);
            iconX = Math.max(6, textX - ICON_SIZE - 4);
        }
        if (iconX < 6) {
            iconX = 6;
            textX = iconX + ICON_SIZE + 4;
        }
        if (iconY < 6) {
            iconY = 6;
            pickupY = iconY + 22;
            itemY = pickupY + 32;
        }
        if (iconY + ICON_SIZE > screenHeight - 6) {
            iconY = screenHeight - ICON_SIZE - 6;
            pickupY = iconY + 22;
            itemY = pickupY + 32;
        }

        drawIcon(g, iconX, iconY);
        drawScaledString(g, mc, "Pickup", textX, pickupY, PICKUP_TEXT_SCALE, TEXT_GRAY);
        drawScaledString(g, mc, target.getItem().getHoverName().getString(), textX, itemY, ITEM_TEXT_SCALE, TEXT_WHITE);
    }

    private static ItemEntity findTarget(Minecraft mc, LocalPlayer player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        double reach = MAX_PICKUP_REACH;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            reach = Math.min(reach, eye.distanceTo(mc.hitResult.getLocation()) + 0.35D);
        }

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(0.85D);
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, searchBox, item -> item.isAlive() && !item.getItem().isEmpty());

        ItemEntity bestItem = null;
        double bestScore = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            double score = scoreItem(item, eye, look, reach);
            if (score < bestScore) {
                bestItem = item;
                bestScore = score;
            }
        }
        return bestItem;
    }

    private static double scoreItem(ItemEntity item, Vec3 eye, Vec3 look, double reach) {
        Vec3 center = item.getBoundingBox().getCenter();
        Vec3 toItem = center.subtract(eye);
        double alongRay = toItem.dot(look);
        if (alongRay < 0.0D || alongRay > reach) return Double.MAX_VALUE;

        Vec3 closest = eye.add(look.scale(alongRay));
        double lineDistanceSqr = closest.distanceToSqr(center);
        boolean directBoxHit = item.getBoundingBox().inflate(0.35D).clip(eye, eye.add(look.scale(reach))).isPresent();
        if (!directBoxHit && lineDistanceSqr > SOFT_AIM_RADIUS_SQR) return Double.MAX_VALUE;
        return lineDistanceSqr + (alongRay * 0.015D);
    }

    /**
     * Uses Minecraft's own entity-outline pipeline instead of re-rendering a
     * scaled, full-bright copy of the item. The glowing flag is changed only on
     * the local client while this item owns the pickup prompt, so the renderer's
     * exact silhouette becomes the white outline mask without changing gameplay.
     */
    private static void setTarget(ItemEntity next) {
        if (target == next) return;
        clearTarget();
        target = next;
        if (target == null) return;

        targetWasGlowing = target.isCurrentlyGlowing();
        if (!targetWasGlowing) {
            target.setGlowingTag(true);
        }
    }

    private static void clearTarget() {
        if (target != null && target.isAlive() && !targetWasGlowing) {
            target.setGlowingTag(false);
        }
        target = null;
        targetWasGlowing = false;
    }

    /**
     * Mirrors the active item renderer's visual center. Vanilla bobs an item
     * without moving its entity, while ItemPhysic deliberately leaves grounded
     * items visually at rest. The compatibility check stays entirely optional:
     * no ItemPhysic class is linked into our client code.
     */
    private static Vec3 renderedItemCenter(ItemEntity item,
            float partialTick) {
        Vec3 center = item.getBoundingBox().getCenter();
        double interpolatedX = Mth.lerp(partialTick, item.xOld, item.getX());
        double interpolatedY = Mth.lerp(partialTick, item.yOld, item.getY());
        double interpolatedZ = Mth.lerp(partialTick, item.zOld, item.getZ());

        float visualBob = 0.0F;
        if (!(ITEM_PHYSIC_LOADED && item.onGround())) {
            float bobPhaseOffset = item.getSpin(0.0F)
                    - ((float) item.getAge() / 20.0F);
            visualBob = Mth.sin(((float) item.getAge() + partialTick)
                    / 10.0F + bobPhaseOffset) * 0.1F + 0.1F;
        }

        return center.add(interpolatedX - item.getX(),
                interpolatedY - item.getY() + visualBob,
                interpolatedZ - item.getZ());
    }

    private static ScreenPoint projectToScreen(Minecraft mc, Vec3 worldPos, int screenWidth, int screenHeight) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 relative = worldPos.subtract(camera.getPosition());
        Quaternionf rotation = new Quaternionf(camera.rotation());
        rotation.conjugate();

        Vector3f transformed = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z);
        transformed.rotate(rotation);

        double depth = Math.abs(transformed.z());
        if (depth < 0.05D) return null;

        double fov = mc.options.fov().get();
        double scale = screenHeight / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));

        int x = (int) Math.round((screenWidth / 2.0D) - (transformed.x() * scale / depth));
        int y = (int) Math.round((screenHeight / 2.0D) - (transformed.y() * scale / depth));
        return new ScreenPoint(x, y);
    }

    private static void drawIcon(GuiGraphics g, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.98F);
        g.blit(PICKUP_ICON, x, y, ICON_SIZE, ICON_SIZE, 0.0F, 0.0F, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawScaledString(GuiGraphics g, Minecraft mc, String text, int x, int y, float scale, int color) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(scale, scale, 1.0F);
        g.drawString(mc.font, ScpFonts.roboto(text), 0, 0, color, true);
        pose.popPose();
    }

    private record ScreenPoint(int x, int y) {
    }
}
