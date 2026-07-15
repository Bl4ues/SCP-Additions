package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.context.ContextInteractionRegistry;
import com.bl4ues.scpinventory.network.ContextInteractPacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.entity.AbstractScp131Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public final class ContextPromptClient {
    private static final int ICON_SOURCE_SIZE = 128;
    private static final int ICON_SIZE = 82;
    private static final int TEXT_WHITE = 0xFFE8E8E8;
    private static final int TEXT_GRAY = 0xFFB2B3B3;
    private static final float ACTION_TEXT_SCALE = 1.55F;
    private static final float NAME_TEXT_SCALE = 1.85F;
    private static final double MAX_CONTEXT_REACH = 6.0D;
    private static final int CLICK_COOLDOWN_TICKS = 5;

    private static ContextTarget target;
    private static boolean useWasDown = false;
    private static int cooldownTicks = 0;

    private ContextPromptClient() {
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null
                || !ScpAdditionsModulesConfig.customInteractionsEnabledFor(player)) {
            clear();
            useWasDown = false;
            cooldownTicks = 0;
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        if (PickupPromptClient.hasActiveTarget()) {
            clear();
            useWasDown = mc.options.keyUse.isDown();
            return;
        }

        target = findContextTarget(mc, player);

        boolean useDown = mc.options.keyUse.isDown();
        boolean usePressedThisTick = useDown && !useWasDown;
        useWasDown = useDown;
        boolean contextPressedThisTick = Keybinds.CONTEXT_INTERACT.consumeClick();

        if (target != null && cooldownTicks <= 0) {
            boolean rightClickAccepted = usePressedThisTick && target.allowRightClick();
            boolean contextKeyAccepted = contextPressedThisTick && target.allowE();
            if (rightClickAccepted || contextKeyAccepted) {
                ModNetwork.CHANNEL.sendToServer(new ContextInteractPacket(target.pos(), target.entityId(),
                        target.entity(), Screen.hasShiftDown(), Screen.hasControlDown()));
                cooldownTicks = CLICK_COOLDOWN_TICKS;
                clear();
            }
        }
    }

    public static boolean hasRightClickTarget() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null
                || !ScpAdditionsModulesConfig.customInteractionsEnabledFor(player)
                || PickupPromptClient.hasActiveTarget()) {
            return false;
        }
        if (target == null || !target.isAlive(mc)) {
            target = findContextTarget(mc, player);
        }
        return target != null && target.allowRightClick();
    }

    public static void render(GuiGraphics g, int screenWidth, int screenHeight, float partialTick) {
        clientTick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.options.hideGui || PickupPromptClient.hasActiveTarget() || target == null || !target.isAlive(mc)) {
            return;
        }

        ScreenPoint point = projectToScreen(mc, target.anchor(), screenWidth, screenHeight);
        if (point == null) {
            point = new ScreenPoint(screenWidth / 2, screenHeight - 28);
        }

        int screenX = Mth.clamp(point.x(), 28, screenWidth - 28);
        int screenY = Mth.clamp(point.y(), 28, screenHeight - 28);

        int iconX = screenX - (ICON_SIZE / 2) - 3;
        int iconY = screenY - (ICON_SIZE / 2) + 8;
        int textX = iconX + ICON_SIZE + 4;
        int actionY = iconY + 22;
        int nameY = actionY + 32;

        int textWidth = target.maxTextWidth(mc);
        if (textWidth > 0 && textX + textWidth > screenWidth - 8) {
            textX = Math.max(8, screenWidth - textWidth - 8);
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

        drawIcon(g, target.icon(), iconX, iconY);
        if (target.showAction()) {
            drawScaledString(g, mc, target.action(), textX, actionY, ACTION_TEXT_SCALE, TEXT_GRAY);
        }
        if (target.showName()) {
            drawScaledString(g, mc, target.name(), textX, target.showAction() ? nameY : actionY + 14, NAME_TEXT_SCALE, TEXT_WHITE);
        }
    }

    public static void clear() {
        target = null;
    }

    private static ContextTarget findContextTarget(Minecraft mc, LocalPlayer player) {
        ContextTarget block = findBlockTarget(mc, player);
        ContextTarget entity = findEntityTarget(mc, player);
        if (block == null) {
            return entity;
        }
        if (entity == null) {
            return block;
        }
        return entity.score() < block.score() ? entity : block;
    }

    private static ContextTarget findBlockTarget(Minecraft mc, LocalPlayer player) {
        if (!ContextInteractionRegistry.hasBlockRules()) {
            return null;
        }

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        double maxRange = Math.min(MAX_CONTEXT_REACH, ContextInteractionRegistry.getMaxBlockRange());
        if (maxRange <= 0.0D) {
            return null;
        }

        BlockHitResult blockHit = mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK ? hit : null;
        int radius = Math.max(1, (int) Math.ceil(maxRange));
        BlockPos playerPos = player.blockPosition();

        ContextTarget best = null;
        double bestScore = Double.MAX_VALUE;
        for (BlockPos mutablePos : BlockPos.betweenClosed(playerPos.offset(-radius, -radius, -radius), playerPos.offset(radius, radius, radius))) {
            BlockPos pos = mutablePos.immutable();
            BlockState state = player.level().getBlockState(pos);
            List<ContextInteractionRegistry.Rule> rules = ContextInteractionRegistry.getBlockRules(state.getBlock());
            if (rules.isEmpty()) {
                continue;
            }

            boolean directBlockHit = blockHit != null && blockHit.getBlockPos().equals(pos);
            for (ContextInteractionRegistry.Rule rule : rules) {
                Vec3 anchor = rule.resolveBlockAnchor(pos, state);
                double score = scorePoint(anchor, eye, look, rule.range(), directBlockHit, rule.priority());
                if (score < bestScore) {
                    bestScore = score;
                    String name = rule.showName() ? rule.blockName(state) : "";
                    boolean shouldShowName = rule.showName() && !name.isEmpty();
                    boolean shouldShowAction = rule.showAction() && shouldShowName;
                    ResourceLocation icon = ContextPromptIcons.resolve(rule.icon(), rule.id());
                    best = new ContextTarget(pos, 0, false, anchor, rule.action(), name, shouldShowAction, shouldShowName, rule.allowE(), rule.allowRightClick(), icon, score);
                }
            }
        }
        return best;
    }

    private static ContextTarget findEntityTarget(Minecraft mc, LocalPlayer player) {
        if (!ContextInteractionRegistry.hasEntityRules()) {
            return null;
        }

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        double maxRange = Math.min(MAX_CONTEXT_REACH, ContextInteractionRegistry.getMaxEntityRange());
        if (maxRange <= 0.0D) {
            return null;
        }

        EntityHitResult entityHit = mc.hitResult instanceof EntityHitResult hit ? hit : null;
        AABB area = player.getBoundingBox().expandTowards(look.scale(maxRange)).inflate(1.0D);
        ContextTarget best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity entity : player.level().getEntities(player, area, candidate -> candidate.isAlive() && candidate.isPickable())) {
            if (entity instanceof AbstractScp131Entity scp131 && scp131.isFollowing()) {
                continue;
            }

            List<ContextInteractionRegistry.Rule> rules = ContextInteractionRegistry.getEntityRules(entity.getType());
            if (rules.isEmpty()) {
                continue;
            }

            boolean directEntityHit = entityHit != null && entityHit.getEntity().getId() == entity.getId();
            for (ContextInteractionRegistry.Rule rule : rules) {
                Vec3 anchor = rule.resolveEntityAnchor(entity);
                double score = scorePoint(anchor, eye, look, rule.range(), directEntityHit, rule.priority());
                if (score < bestScore) {
                    bestScore = score;
                    String name = rule.showName() ? rule.entityName(entity) : "";
                    boolean shouldShowName = rule.showName() && !name.isEmpty();
                    boolean shouldShowAction = rule.showAction() && shouldShowName;
                    ResourceLocation icon = ContextPromptIcons.resolve(rule.icon(), rule.id());
                    best = new ContextTarget(entity.blockPosition(), entity.getId(), true, anchor, rule.action(), name, shouldShowAction, shouldShowName, rule.allowE(), rule.allowRightClick(), icon, score);
                }
            }
        }
        return best;
    }

    private static double scorePoint(Vec3 point, Vec3 eye, Vec3 look, double reach, boolean directHit, int priority) {
        Vec3 toPoint = point.subtract(eye);
        double distanceSqr = toPoint.lengthSqr();
        if (distanceSqr > reach * reach) {
            return Double.MAX_VALUE;
        }

        double distance = Math.sqrt(distanceSqr);
        Vec3 direction = distance <= 0.001D ? look : toPoint.scale(1.0D / distance);
        double dot = direction.dot(look);
        double centerPenalty;
        if (dot > 0.0D) {
            double alongRay = toPoint.dot(look);
            Vec3 closest = eye.add(look.scale(Math.max(0.0D, alongRay)));
            centerPenalty = closest.distanceToSqr(point);
        } else {
            centerPenalty = 0.58D * 0.58D + ((1.0D - dot) * 0.35D);
        }

        return centerPenalty + (distance * 0.035D) - (priority * 0.01D) - (directHit ? 0.35D : 0.0D);
    }

    private static ScreenPoint projectToScreen(Minecraft mc, Vec3 worldPos, int screenWidth, int screenHeight) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 relative = worldPos.subtract(camera.getPosition());
        Quaternionf rotation = new Quaternionf(camera.rotation());
        rotation.conjugate();

        Vector3f transformed = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z);
        transformed.rotate(rotation);

        double z = transformed.z();
        double depth = Math.abs(z);
        if (depth < 0.05D) return null;

        double fov = mc.options.fov().get();
        double scale = screenHeight / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));

        int x = (int) Math.round((screenWidth / 2.0D) - (transformed.x() * scale / depth));
        int y;
        if (z < 0.0D) {
            y = screenHeight - 28;
        } else {
            y = (int) Math.round((screenHeight / 2.0D) - (transformed.y() * scale / depth));
        }
        return new ScreenPoint(x, y);
    }

    private static void drawIcon(GuiGraphics g, ResourceLocation icon, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.98F);
        g.blit(icon, x, y, ICON_SIZE, ICON_SIZE, 0.0F, 0.0F, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE);
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

    private record ContextTarget(BlockPos pos, int entityId, boolean entity, Vec3 anchor, String action, String name, boolean showAction, boolean showName, boolean allowE, boolean allowRightClick, ResourceLocation icon, double score) {
        private boolean isAlive(Minecraft mc) {
            if (mc.level == null) {
                return false;
            }
            if (entity) {
                Entity found = mc.level.getEntity(entityId);
                return found != null && found.isAlive();
            }
            return pos != null && !mc.level.getBlockState(pos).isAir();
        }

        private int maxTextWidth(Minecraft mc) {
            int width = 0;
            if (showAction) {
                width = Math.max(width, Math.round(mc.font.width(ScpFonts.roboto(action)) * ACTION_TEXT_SCALE));
            }
            if (showName) {
                width = Math.max(width, Math.round(mc.font.width(ScpFonts.roboto(name)) * NAME_TEXT_SCALE));
            }
            return width;
        }
    }

    private record ScreenPoint(int x, int y) {
    }
}
