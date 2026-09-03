package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.inventory.context.ContextInteractionRegistry;
import com.bl4ues.scpclassifieddirective.inventory.network.ContextInteractPacket;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import com.bl4ues.scpclassifieddirective.entity.AbstractScp131Entity;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorModule;
import com.bl4ues.scpclassifieddirective.scp330.Scp330Hands;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/** Selects and renders the nearest contextual interaction anchor. */
public final class ContextPromptClient {
    private static final int ICON_SOURCE_SIZE = 128;
    private static final int BASE_ICON_SIZE = 82;
    private static final int TEXT_WHITE = 0xFFE8E8E8;
    private static final int TEXT_GRAY = 0xFFB2B3B3;
    private static final float BASE_ACTION_TEXT_SCALE = 1.55F;
    private static final float BASE_NAME_TEXT_SCALE = 1.85F;
    private static final double MAX_CONTEXT_REACH = 6.0D;
    private static final double TARGET_STICKINESS_BONUS = 0.045D;
    private static final double PRECISE_AIM_RADIUS_SQR = 0.60D * 0.60D;
    private static final double ELEVATOR_BUTTON_AIM_RADIUS_SQR =
            0.20D * 0.20D;
    private static final double SCP_914_CONTROL_AIM_RADIUS_SQR =
            0.18D * 0.18D;
    private static final int CLICK_COOLDOWN_TICKS = 5;

    private static ContextTarget target;
    private static boolean useWasDown;
    private static int cooldownTicks;

    private ContextPromptClient() {
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null
                || minecraft.screen != null
                || !ScpClassifiedDirectiveModulesConfig.customInteractionsEnabledFor(
                        player) || Scp330Hands.isDisabled(player)) {
            clear();
            useWasDown = false;
            cooldownTicks = 0;
            return;
        }

        if (cooldownTicks > 0) cooldownTicks--;
        if (PickupPromptClient.hasActiveTarget()) {
            clear();
            useWasDown = minecraft.options.keyUse.isDown();
            return;
        }

        target = findContextTarget(minecraft, player);
        boolean useDown = minecraft.options.keyUse.isDown();
        boolean rightClickPressed = useDown && !useWasDown;
        useWasDown = useDown;

        if (target != null && cooldownTicks <= 0
                && rightClickPressed && target.allowRightClick()) {
            ModNetwork.CHANNEL.sendToServer(new ContextInteractPacket(
                    target.pos(), target.entityId(), target.entity(),
                    Screen.hasShiftDown(), Screen.hasControlDown(),
                    target.interactionKey()));
            cooldownTicks = CLICK_COOLDOWN_TICKS;
            clear();
        }
    }

    public static boolean hasRightClickTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null
                || minecraft.screen != null
                || !ScpClassifiedDirectiveModulesConfig.customInteractionsEnabledFor(
                        player) || Scp330Hands.isDisabled(player)
                || PickupPromptClient.hasActiveTarget()) {
            return false;
        }
        if (target == null || !target.isAlive(minecraft)) {
            target = findContextTarget(minecraft, player);
        }
        return target != null && target.allowRightClick();
    }

    public static void render(GuiGraphics graphics, int screenWidth,
            int screenHeight, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || minecraft.screen != null || minecraft.options.hideGui
                || PickupPromptClient.hasActiveTarget() || target == null
                || !target.isAlive(minecraft)) {
            return;
        }

        ScreenPoint point = projectToScreen(minecraft, target.anchor(),
                screenWidth, screenHeight, target.allowOffscreen());
        if (point == null) return;

        if (renderScp914ControlPrompt(graphics, minecraft, target, point,
                screenWidth, screenHeight)) {
            return;
        }

        float promptScale = target.promptScale();
        int iconSize = Math.max(24,
                Math.round(BASE_ICON_SIZE * promptScale));
        float actionScale = BASE_ACTION_TEXT_SCALE * promptScale;
        float nameScale = BASE_NAME_TEXT_SCALE * promptScale;
        int screenX = Mth.clamp(point.x(), 18, screenWidth - 18);
        int screenY = Mth.clamp(point.y(), 18, screenHeight - 18);
        int iconX = screenX - iconSize / 2 - Math.round(3 * promptScale);
        int iconY = screenY - iconSize / 2 + Math.round(8 * promptScale);
        int gap = Math.max(2, Math.round(4 * promptScale));
        int textX = iconX + iconSize + gap;
        int actionY = iconY + Math.round(22 * promptScale);
        int nameY = actionY + Math.round(32 * promptScale);

        int textWidth = target.maxTextWidth(minecraft, actionScale, nameScale);
        if (textWidth > 0 && textX + textWidth > screenWidth - 8) {
            textX = Math.max(8, screenWidth - textWidth - 8);
            iconX = Math.max(6, textX - iconSize - gap);
        }
        if (iconX < 6) {
            iconX = 6;
            textX = iconX + iconSize + gap;
        }
        if (iconY < 6) {
            iconY = 6;
            actionY = iconY + Math.round(22 * promptScale);
            nameY = actionY + Math.round(32 * promptScale);
        }
        if (iconY + iconSize > screenHeight - 6) {
            iconY = screenHeight - iconSize - 6;
            actionY = iconY + Math.round(22 * promptScale);
            nameY = actionY + Math.round(32 * promptScale);
        }

        drawIcon(graphics, target.icon(), iconX, iconY, iconSize);
        if (target.showAction()) {
            drawScaledString(graphics, minecraft, target.action(), textX,
                    actionY, actionScale, TEXT_GRAY);
        }
        if (target.showName()) {
            drawScaledString(graphics, minecraft, target.name(), textX,
                    target.showAction() ? nameY
                            : actionY + Math.round(14 * promptScale),
                    nameScale, TEXT_WHITE);
        }
    }

    public static void clear() {
        target = null;
    }

    private static ContextTarget findContextTarget(Minecraft minecraft,
            LocalPlayer player) {
        ContextTarget block = findBlockTarget(minecraft, player);
        ContextTarget entity = findEntityTarget(minecraft, player);
        if (block == null) return entity;
        if (entity == null) return block;
        return entity.score() < block.score() ? entity : block;
    }

    private static ContextTarget findBlockTarget(Minecraft minecraft,
            LocalPlayer player) {
        if (!ContextInteractionRegistry.hasBlockRules()) return null;
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        double maxRange = Math.min(MAX_CONTEXT_REACH,
                ContextInteractionRegistry.getMaxBlockRange());
        if (maxRange <= 0.0D) return null;

        BlockHitResult blockHit = minecraft.hitResult
                instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK ? hit : null;
        int radius = Math.max(1, (int) Math.ceil(maxRange));
        BlockPos playerPos = player.blockPosition();
        ContextTarget best = null;
        double bestScore = Double.MAX_VALUE;

        for (BlockPos mutable : BlockPos.betweenClosed(
                playerPos.offset(-radius, -radius, -radius),
                playerPos.offset(radius, radius, radius))) {
            BlockPos scannedPos = mutable.immutable();
            BlockState scannedState = player.level().getBlockState(scannedPos);
            BlockPos rulePos = scannedPos;
            BlockState ruleState = scannedState;
            if (player.level().getBlockEntity(scannedPos)
                    instanceof CoreRoomElevatorModule.StructurePartBlockEntity part) {
                BlockPos master = part.masterPos();
                BlockState masterState = player.level().getBlockState(master);
                if (masterState.getBlock()
                        instanceof CoreRoomElevatorModule.StationBlock) {
                    rulePos = master;
                    ruleState = masterState;
                }
            }
            List<ContextInteractionRegistry.Rule> rules =
                    ContextInteractionRegistry.getBlockRules(ruleState.getBlock());
            if (rules.isEmpty()) continue;
            boolean directHit = blockHit != null
                    && hitBelongsTo(blockHit.getBlockPos(), rulePos, player);
            for (ContextInteractionRegistry.Rule rule : rules) {
                if (!rule.isAvailable(player.level(), rulePos, ruleState,
                        player) || !rule.isHeldItemSatisfied(player)) continue;
                Vec3 anchor = rule.resolveBlockAnchor(rulePos, ruleState);
                if (isElevatorStationButton(rule.interactionKey())
                        && !isStationButtonViewedFromFront(eye, anchor,
                        ruleState)) {
                    continue;
                }
                double score = scorePoint(anchor, eye, look, rule.range(),
                        directHit, rule.priority(), rule.requiresPreciseAim(),
                        preciseAimRadiusSqr(rule.interactionKey()),
                        rule.allowOffscreen());
                if (rule.hasRequiredItem()) score -= 0.12D;
                if (!isElevatorButton(rule.interactionKey())
                        && isCurrentBlockTarget(rulePos,
                        rule.interactionKey())) {
                    score -= TARGET_STICKINESS_BONUS;
                }
                if (score < bestScore && hasBlockLineOfSight(player, eye,
                        anchor, rulePos, directHit)) {
                    bestScore = score;
                    String name = rule.showName()
                            ? rule.blockName(ruleState) : "";
                    boolean showName = rule.showName() && !name.isEmpty();
                    boolean showAction = rule.showAction()
                            && rule.action() != null
                            && !rule.action().isBlank();
                    ResourceLocation icon = ContextPromptIcons.resolve(
                            rule.icon(), rule.id());
                    // Legacy E-only rules are migrated behaviorally to the vanilla
                    // use control instead of becoming dead configuration entries.
                    boolean allowUse = rule.allowRightClick() || rule.allowE();
                    best = new ContextTarget(rulePos, 0, false, anchor,
                            rule.interactionKey(), rule.action(), name,
                            showAction, showName, allowUse, icon,
                            (float) rule.promptScale(),
                            rule.allowOffscreen(), score);
                }
            }
        }
        return best;
    }

    private static ContextTarget findEntityTarget(Minecraft minecraft,
            LocalPlayer player) {
        if (!ContextInteractionRegistry.hasEntityRules()) return null;
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        double maxRange = Math.min(MAX_CONTEXT_REACH,
                ContextInteractionRegistry.getMaxEntityRange());
        if (maxRange <= 0.0D) return null;

        EntityHitResult entityHit = minecraft.hitResult
                instanceof EntityHitResult hit ? hit : null;
        AABB area = player.getBoundingBox()
                .expandTowards(look.scale(maxRange)).inflate(1.0D);
        ContextTarget best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity entity : player.level().getEntities(player, area,
                candidate -> candidate.isAlive() && candidate.isPickable())) {
            if (entity instanceof AbstractScp131Entity scp131
                    && scp131.isFollowing()) continue;
            List<ContextInteractionRegistry.Rule> rules =
                    ContextInteractionRegistry.getEntityRules(entity.getType());
            if (rules.isEmpty()) continue;
            boolean directHit = entityHit != null
                    && entityHit.getEntity().getId() == entity.getId();
            for (ContextInteractionRegistry.Rule rule : rules) {
                if (!rule.isAvailable(entity)
                        || !rule.isHeldItemSatisfied(player)) continue;
                Vec3 anchor = rule.resolveEntityAnchor(entity);
                if (isElevatorCarriageButton(rule.interactionKey())
                        && !isCarriageButtonViewedFromFront(eye, anchor,
                        entity)) {
                    continue;
                }
                double score = scorePoint(anchor, eye, look, rule.range(),
                        directHit, rule.priority(),
                        rule.requiresPreciseAim(),
                        preciseAimRadiusSqr(rule.interactionKey()),
                        rule.allowOffscreen());
                if (rule.hasRequiredItem()) score -= 0.12D;
                if (!isElevatorButton(rule.interactionKey())
                        && isCurrentEntityTarget(entity.getId(),
                        rule.interactionKey())) {
                    score -= TARGET_STICKINESS_BONUS;
                }
                if (score < bestScore && hasEntityLineOfSight(player, eye,
                        anchor, directHit)) {
                    bestScore = score;
                    String name = rule.showName()
                            ? rule.entityName(entity) : "";
                    boolean showName = rule.showName() && !name.isEmpty();
                    boolean showAction = rule.showAction()
                            && rule.action() != null
                            && !rule.action().isBlank();
                    ResourceLocation icon = ContextPromptIcons.resolve(
                            rule.icon(), rule.id());
                    boolean allowUse = rule.allowRightClick() || rule.allowE();
                    best = new ContextTarget(entity.blockPosition(),
                            entity.getId(), true, anchor,
                            rule.interactionKey(), rule.action(), name,
                            showAction, showName, allowUse, icon,
                            (float) rule.promptScale(),
                            rule.allowOffscreen(), score);
                }
            }
        }
        return best;
    }

    private static boolean hitBelongsTo(BlockPos hitPos, BlockPos master,
            LocalPlayer player) {
        if (hitPos.equals(master)) return true;
        return player.level().getBlockEntity(hitPos)
                instanceof CoreRoomElevatorModule.StructurePartBlockEntity part
                && part.masterPos().equals(master);
    }

    private static boolean isCurrentBlockTarget(BlockPos pos, String key) {
        return target != null && !target.entity() && target.pos().equals(pos)
                && target.interactionKey().equals(key);
    }

    private static boolean isCurrentEntityTarget(int entityId, String key) {
        return target != null && target.entity()
                && target.entityId() == entityId
                && target.interactionKey().equals(key);
    }

    private static boolean hasBlockLineOfSight(LocalPlayer player, Vec3 eye,
            Vec3 anchor, BlockPos targetPos, boolean directHit) {
        if (directHit) return true;
        BlockHitResult obstruction = player.level().clip(new ClipContext(
                eye, anchor, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (obstruction.getType() == HitResult.Type.MISS
                || obstruction.getBlockPos().equals(targetPos)) return true;
        return player.level().getBlockEntity(obstruction.getBlockPos())
                instanceof CoreRoomElevatorModule.StructurePartBlockEntity part
                && part.masterPos().equals(targetPos);
    }

    private static boolean hasEntityLineOfSight(LocalPlayer player, Vec3 eye,
            Vec3 anchor, boolean directHit) {
        if (directHit) return true;
        BlockHitResult obstruction = player.level().clip(new ClipContext(
                eye, anchor, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return obstruction.getType() == HitResult.Type.MISS;
    }

    private static double scorePoint(Vec3 point, Vec3 eye, Vec3 look,
            double reach, boolean directHit, int priority,
            boolean preciseAim, double preciseAimRadiusSqr,
            boolean allowOffscreen) {
        Vec3 toPoint = point.subtract(eye);
        double distanceSqr = toPoint.lengthSqr();
        if (distanceSqr > reach * reach) return Double.MAX_VALUE;
        double distance = Math.sqrt(distanceSqr);
        Vec3 direction = distance <= 0.001D ? look
                : toPoint.scale(1.0D / distance);
        double dot = direction.dot(look);
        double alongRay = toPoint.dot(look);
        if ((!allowOffscreen && alongRay <= 0.0D) || alongRay > reach) {
            return Double.MAX_VALUE;
        }
        double centerPenalty;
        if (dot > 0.0D) {
            Vec3 closest = eye.add(look.scale(Math.max(0.0D, alongRay)));
            centerPenalty = closest.distanceToSqr(point);
        } else {
            centerPenalty = 0.58D * 0.58D + (1.0D - dot) * 0.35D;
        }
        if (preciseAim && centerPenalty > preciseAimRadiusSqr) {
            return Double.MAX_VALUE;
        }
        return centerPenalty + distance * 0.035D - priority * 0.01D
                - (directHit ? 0.35D : 0.0D);
    }

    private static boolean isElevatorButton(String interactionKey) {
        return isElevatorStationButton(interactionKey)
                || isElevatorCarriageButton(interactionKey);
    }

    private static boolean isElevatorStationButton(String interactionKey) {
        return interactionKey != null
                && interactionKey.startsWith("elevator_station_");
    }

    private static boolean isElevatorCarriageButton(String interactionKey) {
        return interactionKey != null
                && interactionKey.startsWith("elevator_carriage_");
    }

    private static double preciseAimRadiusSqr(String interactionKey) {
        if (interactionKey != null
                && interactionKey.startsWith("scp_914_")) {
            return SCP_914_CONTROL_AIM_RADIUS_SQR;
        }
        return isElevatorButton(interactionKey)
                ? ELEVATOR_BUTTON_AIM_RADIUS_SQR
                : PRECISE_AIM_RADIUS_SQR;
    }

    private static boolean isStationButtonViewedFromFront(Vec3 eye,
            Vec3 anchor, BlockState state) {
        if (!state.hasProperty(CoreRoomElevatorModule.FACING)) return true;
        Direction facing = state.getValue(CoreRoomElevatorModule.FACING);
        Vec3 outward = new Vec3(facing.getStepX(), 0.0D,
                facing.getStepZ());
        return eye.subtract(anchor).dot(outward) > 0.02D;
    }

    private static boolean isCarriageButtonViewedFromFront(Vec3 eye,
            Vec3 anchor, Entity entity) {
        Vec3 inward = entity.position().subtract(anchor)
                .multiply(1.0D, 0.0D, 1.0D);
        if (inward.lengthSqr() < 1.0E-6D) return true;
        return eye.subtract(anchor).dot(inward.normalize()) > 0.02D;
    }

    private static ScreenPoint projectToScreen(Minecraft minecraft,
            Vec3 worldPos, int screenWidth, int screenHeight,
            boolean allowOffscreen) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 relative = worldPos.subtract(camera.getPosition());
        Quaternionf rotation = new Quaternionf(camera.rotation());
        rotation.conjugate();
        Vector3f transformed = new Vector3f((float) relative.x,
                (float) relative.y, (float) relative.z);
        transformed.rotate(rotation);
        double rawDepth = transformed.z();
        if (rawDepth <= 0.05D) {
            if (!allowOffscreen) return null;
            double offsetX = -transformed.x();
            double offsetY = -transformed.y();
            double length = Math.hypot(offsetX, offsetY);
            if (length < 1.0E-4D) {
                offsetY = 1.0D;
                length = 1.0D;
            }
            double edgeDistance = Math.max(screenWidth, screenHeight) * 2.0D;
            return new ScreenPoint(
                    (int) Math.round(screenWidth / 2.0D
                            + offsetX / length * edgeDistance),
                    (int) Math.round(screenHeight / 2.0D
                            + offsetY / length * edgeDistance));
        }
        double fov = minecraft.options.fov().get();
        double scale = screenHeight
                / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));
        int x = (int) Math.round(screenWidth / 2.0D
                - transformed.x() * scale / rawDepth);
        int y = (int) Math.round(screenHeight / 2.0D
                - transformed.y() * scale / rawDepth);
        return new ScreenPoint(x, y);
    }

    private static boolean renderScp914ControlPrompt(
            GuiGraphics graphics, Minecraft minecraft, ContextTarget target,
            ScreenPoint point, int screenWidth, int screenHeight) {
        if (!"scp_914_dial".equals(target.interactionKey())) return false;

        float promptScale = target.promptScale();
        int iconSize = Math.max(24,
                Math.round(BASE_ICON_SIZE * promptScale));
        int screenX = Mth.clamp(point.x(), iconSize / 2 + 6,
                screenWidth - iconSize / 2 - 6);
        int screenY = Mth.clamp(point.y(), iconSize / 2 + 6,
                screenHeight - iconSize / 2 - 6);
        int iconX = screenX - iconSize / 2;
        int iconY = screenY - iconSize / 2;
        drawIcon(graphics, target.icon(), iconX, iconY, iconSize);
        return true;
    }

    private static void drawIcon(GuiGraphics graphics,
            ResourceLocation icon, int x, int y, int size) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.98F);
        graphics.blit(icon, x, y, size, size, 0.0F, 0.0F,
                ICON_SOURCE_SIZE, ICON_SOURCE_SIZE,
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

    private record ContextTarget(BlockPos pos, int entityId, boolean entity,
            Vec3 anchor, String interactionKey, String action, String name,
            boolean showAction, boolean showName, boolean allowRightClick,
            ResourceLocation icon, float promptScale, boolean allowOffscreen,
            double score) {
        private boolean isAlive(Minecraft minecraft) {
            if (minecraft.level == null) return false;
            if (entity) {
                Entity found = minecraft.level.getEntity(entityId);
                return found != null && found.isAlive();
            }
            return pos != null && !minecraft.level.getBlockState(pos).isAir();
        }

        private int maxTextWidth(Minecraft minecraft, float actionScale,
                float nameScale) {
            int width = 0;
            if (showAction) {
                width = Math.max(width, Math.round(minecraft.font.width(
                        ScpFonts.roboto(action)) * actionScale));
            }
            if (showName) {
                width = Math.max(width, Math.round(minecraft.font.width(
                        ScpFonts.roboto(name)) * nameScale));
            }
            return width;
        }
    }

    private record ScreenPoint(int x, int y) {
    }
}
