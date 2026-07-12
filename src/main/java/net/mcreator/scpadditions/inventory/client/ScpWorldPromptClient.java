package net.mcreator.scpadditions.inventory.client;

import com.bl4ues.scpinventory.context.ContextInteractionRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.inventory.ScpContextInteractPacket;
import net.mcreator.scpadditions.inventory.ScpInventoryPickupPacket;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/** Final-style world pickup and configured context prompt controller. */
public final class ScpWorldPromptClient {
    private static final ResourceLocation HAND_ICON = new ResourceLocation(
            "scpinventory", "textures/gui/pickup.png");
    private static final ResourceLocation CARD_ICON = new ResourceLocation(
            "scpinventory", "textures/gui/card.png");
    private static final ResourceLocation MONTSERRAT = new ResourceLocation(
            "scpinventory", "montserrat");

    private static final int ICON_SOURCE_SIZE = 128;
    private static final int ICON_SIZE = 82;
    private static final int TEXT_WHITE = 0xFFE8E8E8;
    private static final int TEXT_GRAY = 0xFFB2B3B3;
    private static final float ACTION_SCALE = 1.55F;
    private static final float NAME_SCALE = 1.85F;
    private static final double MAX_PICKUP_REACH = 2.25D;
    private static final double MAX_CONTEXT_REACH = 6.0D;
    private static final double AIM_RADIUS_SQR = 0.58D * 0.58D;
    private static final int INPUT_COOLDOWN_TICKS = 5;

    private static Target target;
    private static int cooldownTicks;

    private ScpWorldPromptClient() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null
                || player.isCreative() || player.isSpectator()) {
            target = null;
            cooldownTicks = 0;
            drainInteractionKey();
            return;
        }

        if (cooldownTicks > 0) cooldownTicks--;
        target = findPickupTarget(minecraft, player);
        if (target == null) target = findContextTarget(minecraft, player);

        while (ScpInventoryKeybinds.CONTEXT_INTERACT.consumeClick()) {
            if (target != null && target.allowE && cooldownTicks <= 0) {
                // E is also vanilla inventory. Drain its queued click whenever a
                // world prompt owns the input so the vanilla screen never flashes.
                while (minecraft.options.keyInventory.consumeClick()) {
                }
                trigger();
            }
        }
    }

    public static boolean ownsRightClick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || minecraft.screen != null) return false;
        if (target == null || !target.isAlive(minecraft)) {
            target = findPickupTarget(minecraft, minecraft.player);
            if (target == null) target = findContextTarget(minecraft, minecraft.player);
        }
        return target != null && target.allowRightClick;
    }

    public static boolean triggerRightClick() {
        if (!ownsRightClick() || cooldownTicks > 0) return false;
        trigger();
        return true;
    }

    private static void trigger() {
        if (target == null) return;
        if (target.pickupEntityId >= 0) {
            ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                    new ScpInventoryPickupPacket(target.pickupEntityId));
        } else {
            ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                    new ScpContextInteractPacket(target.blockPos,
                            target.contextEntityId, target.entityTarget));
        }
        cooldownTicks = INPUT_COOLDOWN_TICKS;
        target = null;
    }

    public static void render(GuiGraphics graphics, int screenWidth,
            int screenHeight, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || minecraft.screen != null || minecraft.options.hideGui
                || target == null || !target.isAlive(minecraft)) return;

        ScreenPoint point = project(minecraft, target.anchor,
                screenWidth, screenHeight);
        if (point == null) point = new ScreenPoint(screenWidth / 2,
                screenHeight - 28);

        int screenX = Mth.clamp(point.x, 28, screenWidth - 28);
        int screenY = Mth.clamp(point.y, 28, screenHeight - 28);
        int iconX = screenX - ICON_SIZE / 2 - 3;
        int iconY = screenY - ICON_SIZE / 2 + 8;
        int textX = iconX + ICON_SIZE + 4;
        int actionY = iconY + 22;
        int nameY = actionY + 32;

        int textWidth = Math.max(scaledWidth(minecraft, target.action,
                        ACTION_SCALE),
                scaledWidth(minecraft, target.name, NAME_SCALE));
        if (textX + textWidth > screenWidth - 8) {
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

        drawIcon(graphics, target.icon, iconX, iconY);
        if (target.showAction && !target.action.isBlank())
            drawText(graphics, target.action, textX, actionY,
                    ACTION_SCALE, TEXT_GRAY);
        if (target.showName && !target.name.isBlank())
            drawText(graphics, target.name, textX,
                    target.showAction ? nameY : actionY + 14,
                    NAME_SCALE, TEXT_WHITE);
    }

    private static Target findPickupTarget(Minecraft minecraft,
            LocalPlayer player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        double reach = MAX_PICKUP_REACH;
        if (minecraft.hitResult != null
                && minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
            reach = Math.min(reach,
                    eye.distanceTo(minecraft.hitResult.getLocation()) + 0.35D);
        }
        AABB search = player.getBoundingBox()
                .expandTowards(look.scale(reach)).inflate(0.85D);
        List<ItemEntity> items = player.level().getEntitiesOfClass(
                ItemEntity.class, search,
                item -> item.isAlive() && !item.getItem().isEmpty());

        ItemEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            Vec3 center = item.getBoundingBox().getCenter();
            Vec3 to = center.subtract(eye);
            double along = to.dot(look);
            if (along < 0.0D || along > reach) continue;
            double line = eye.add(look.scale(along)).distanceToSqr(center);
            boolean direct = item.getBoundingBox().inflate(0.35D)
                    .clip(eye, eye.add(look.scale(reach))).isPresent();
            if (!direct && line > AIM_RADIUS_SQR) continue;
            double score = line + along * 0.015D;
            if (score < bestScore) {
                bestScore = score;
                best = item;
            }
        }
        if (best == null) return null;
        return Target.pickup(best.getId(), best.getBoundingBox().getCenter()
                        .add(0.0D, -0.08D, 0.0D),
                best.getItem().getHoverName().getString());
    }

    private static Target findContextTarget(Minecraft minecraft,
            LocalPlayer player) {
        Target block = findBlockTarget(minecraft, player);
        Target entity = findEntityTarget(minecraft, player);
        if (block == null) return entity;
        if (entity == null) return block;
        return entity.score < block.score ? entity : block;
    }

    private static Target findBlockTarget(Minecraft minecraft,
            LocalPlayer player) {
        if (!ContextInteractionRegistry.hasBlockRules()) return null;
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        double maxRange = Math.min(MAX_CONTEXT_REACH,
                ContextInteractionRegistry.getMaxBlockRange());
        int radius = Math.max(1, (int) Math.ceil(maxRange));
        BlockHitResult directHit = minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK ? hit : null;

        Target best = null;
        double bestScore = Double.MAX_VALUE;
        BlockPos center = player.blockPosition();
        for (BlockPos mutable : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockPos pos = mutable.immutable();
            BlockState state = player.level().getBlockState(pos);
            List<ContextInteractionRegistry.Rule> rules =
                    ContextInteractionRegistry.getBlockRules(state.getBlock());
            if (rules.isEmpty()) continue;
            boolean direct = directHit != null && directHit.getBlockPos().equals(pos);
            for (ContextInteractionRegistry.Rule rule : rules) {
                Vec3 anchor = rule.resolveBlockAnchor(pos, state);
                double score = contextScore(anchor, eye, look, rule.range(),
                        direct, rule.priority());
                if (score < bestScore) {
                    bestScore = score;
                    String name = rule.showName() ? rule.blockName(state) : "";
                    best = Target.block(pos, anchor, rule, name,
                            resolveIcon(rule), score);
                }
            }
        }
        return best;
    }

    private static Target findEntityTarget(Minecraft minecraft,
            LocalPlayer player) {
        if (!ContextInteractionRegistry.hasEntityRules()) return null;
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        double maxRange = Math.min(MAX_CONTEXT_REACH,
                ContextInteractionRegistry.getMaxEntityRange());
        EntityHitResult directHit = minecraft.hitResult instanceof EntityHitResult hit
                ? hit : null;
        AABB search = player.getBoundingBox()
                .expandTowards(look.scale(maxRange)).inflate(1.0D);

        Target best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity entity : player.level().getEntities(player, search,
                candidate -> candidate.isAlive() && candidate.isPickable())) {
            List<ContextInteractionRegistry.Rule> rules =
                    ContextInteractionRegistry.getEntityRules(entity.getType());
            if (rules.isEmpty()) continue;
            boolean direct = directHit != null
                    && directHit.getEntity().getId() == entity.getId();
            for (ContextInteractionRegistry.Rule rule : rules) {
                Vec3 anchor = rule.resolveEntityAnchor(entity);
                double score = contextScore(anchor, eye, look, rule.range(),
                        direct, rule.priority());
                if (score < bestScore) {
                    bestScore = score;
                    String name = rule.showName() ? rule.entityName(entity) : "";
                    best = Target.entity(entity, anchor, rule, name,
                            resolveIcon(rule), score);
                }
            }
        }
        return best;
    }

    private static double contextScore(Vec3 point, Vec3 eye, Vec3 look,
            double reach, boolean direct, int priority) {
        Vec3 to = point.subtract(eye);
        double distanceSqr = to.lengthSqr();
        if (distanceSqr > reach * reach) return Double.MAX_VALUE;
        double distance = Math.sqrt(distanceSqr);
        Vec3 direction = distance <= 0.001D ? look : to.scale(1.0D / distance);
        double dot = direction.dot(look);
        double centerPenalty;
        if (dot > 0.0D) {
            double along = Math.max(0.0D, to.dot(look));
            centerPenalty = eye.add(look.scale(along)).distanceToSqr(point);
        } else {
            centerPenalty = AIM_RADIUS_SQR + (1.0D - dot) * 0.35D;
        }
        return centerPenalty + distance * 0.035D - priority * 0.01D
                - (direct ? 0.35D : 0.0D);
    }

    private static ResourceLocation resolveIcon(ContextInteractionRegistry.Rule rule) {
        String value = rule.icon() == null ? "" : rule.icon().trim();
        if (value.equalsIgnoreCase("card")) return CARD_ICON;
        if (value.isBlank() || value.equalsIgnoreCase("hand")
                || value.equalsIgnoreCase("pickup")
                || value.equalsIgnoreCase("default")) return HAND_ICON;
        ResourceLocation custom = ResourceLocation.tryParse(value);
        return custom == null ? HAND_ICON : custom;
    }

    private static ScreenPoint project(Minecraft minecraft, Vec3 world,
            int width, int height) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 relative = world.subtract(camera.getPosition());
        Quaternionf rotation = new Quaternionf(camera.rotation()).conjugate();
        Vector3f transformed = new Vector3f((float) relative.x,
                (float) relative.y, (float) relative.z).rotate(rotation);
        double depth = Math.abs(transformed.z());
        if (depth < 0.05D) return null;
        double fov = minecraft.options.fov().get();
        double scale = height / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));
        int x = (int) Math.round(width / 2.0D
                - transformed.x() * scale / depth);
        int y = transformed.z() < 0.0F ? height - 28
                : (int) Math.round(height / 2.0D
                        - transformed.y() * scale / depth);
        return new ScreenPoint(x, y);
    }

    private static int scaledWidth(Minecraft minecraft, String text, float scale) {
        return text == null ? 0 : Math.round(minecraft.font.width(text) * scale);
    }

    private static void drawIcon(GuiGraphics graphics, ResourceLocation icon,
            int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.98F);
        graphics.blit(icon, x, y, ICON_SIZE, ICON_SIZE,
                0.0F, 0.0F, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE,
                ICON_SOURCE_SIZE, ICON_SOURCE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawText(GuiGraphics graphics, String text, int x,
            int y, float scale, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        Component component = Component.literal(text)
                .withStyle(style -> style.withFont(MONTSERRAT));
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.drawString(minecraft.font, component, 0, 0, color, true);
        pose.popPose();
    }

    private static void drainInteractionKey() {
        while (ScpInventoryKeybinds.CONTEXT_INTERACT.consumeClick()) {
        }
    }

    private record ScreenPoint(int x, int y) {
    }

    private static final class Target {
        private final int pickupEntityId;
        private final BlockPos blockPos;
        private final int contextEntityId;
        private final boolean entityTarget;
        private final Vec3 anchor;
        private final String action;
        private final String name;
        private final boolean showAction;
        private final boolean showName;
        private final boolean allowE;
        private final boolean allowRightClick;
        private final ResourceLocation icon;
        private final double score;

        private Target(int pickupEntityId, BlockPos blockPos,
                int contextEntityId, boolean entityTarget, Vec3 anchor,
                String action, String name, boolean showAction,
                boolean showName, boolean allowE, boolean allowRightClick,
                ResourceLocation icon, double score) {
            this.pickupEntityId = pickupEntityId;
            this.blockPos = blockPos;
            this.contextEntityId = contextEntityId;
            this.entityTarget = entityTarget;
            this.anchor = anchor;
            this.action = action;
            this.name = name;
            this.showAction = showAction;
            this.showName = showName;
            this.allowE = allowE;
            this.allowRightClick = allowRightClick;
            this.icon = icon;
            this.score = score;
        }

        static Target pickup(int id, Vec3 anchor, String name) {
            return new Target(id, BlockPos.ZERO, 0, false, anchor,
                    "Pickup", name, true, true, true, true,
                    HAND_ICON, 0.0D);
        }

        static Target block(BlockPos pos, Vec3 anchor,
                ContextInteractionRegistry.Rule rule, String name,
                ResourceLocation icon, double score) {
            return new Target(-1, pos, 0, false, anchor, rule.action(), name,
                    rule.showAction(), rule.showName() && !name.isBlank(),
                    rule.allowE(), rule.allowRightClick(), icon, score);
        }

        static Target entity(Entity entity, Vec3 anchor,
                ContextInteractionRegistry.Rule rule, String name,
                ResourceLocation icon, double score) {
            return new Target(-1, entity.blockPosition(), entity.getId(), true,
                    anchor, rule.action(), name, rule.showAction(),
                    rule.showName() && !name.isBlank(), rule.allowE(),
                    rule.allowRightClick(), icon, score);
        }

        boolean isAlive(Minecraft minecraft) {
            if (minecraft.level == null) return false;
            if (pickupEntityId >= 0)
                return minecraft.level.getEntity(pickupEntityId) instanceof ItemEntity item
                        && item.isAlive();
            if (entityTarget)
                return minecraft.level.getEntity(contextEntityId) != null;
            return blockPos != null && !minecraft.level.getBlockState(blockPos).isAir();
        }
    }
}
