package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.effect.Scp714ExposureManager;
import com.bl4ues.scpclassifieddirective.inventory.client.ContextPromptClient;
import com.bl4ues.scpclassifieddirective.inventory.client.ContextPromptIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;

/**
 * Adds the client-side half of SCP-714 coma recovery. The server already
 * validates the action through Scp714ExposureManager, so this only exposes the
 * prompt when the local player is actually aiming at a bedless sleeping player,
 * which is the presentation used by 714-induced coma.
 */
@Mixin(value = ContextPromptClient.class, remap = false)
public abstract class Scp714ComaPromptMixin {
    private static Constructor<?> scpClassifiedDirective$targetConstructor;

    @Inject(method = "findEntityTarget", at = @At("HEAD"), cancellable = true)
    private static void scpClassifiedDirective$findComatose714Target(
            Minecraft minecraft, LocalPlayer player,
            CallbackInfoReturnable<Object> cir) {
        if (minecraft == null || player == null || player.level() == null) return;

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F).normalize();
        double reach = 3.0D;
        EntityHitResult direct = minecraft.hitResult instanceof EntityHitResult hit
                ? hit : null;
        AABB area = player.getBoundingBox()
                .expandTowards(look.scale(reach)).inflate(0.75D);

        Player best = null;
        Vec3 bestAnchor = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity candidate : player.level().getEntities(player, area,
                entity -> entity instanceof Player && entity.isAlive())) {
            if (!(candidate instanceof Player victim)
                    || victim == player
                    || victim.getPose() != Pose.SLEEPING
                    || victim.getSleepingPos().isPresent()) {
                continue;
            }

            Vec3 anchor = victim.position().add(0.0D,
                    Math.max(0.22D, victim.getBbHeight() * 0.45D), 0.0D);
            Vec3 delta = anchor.subtract(eye);
            double distanceSqr = delta.lengthSqr();
            if (distanceSqr > reach * reach) continue;
            double distance = Math.sqrt(distanceSqr);
            if (distance <= 0.001D) distance = 0.001D;
            double dot = delta.scale(1.0D / distance).dot(look);
            if (dot < 0.94D) continue;

            boolean directlyHit = direct != null
                    && direct.getEntity().getId() == victim.getId();
            double score = (1.0D - dot) * 4.0D + distance * 0.03D
                    - (directlyHit ? 0.35D : 0.0D);
            if (score < bestScore) {
                best = victim;
                bestAnchor = anchor;
                bestScore = score;
            }
        }

        if (best == null || bestAnchor == null) return;
        Object target = scpClassifiedDirective$createTarget(best, bestAnchor,
                bestScore);
        if (target != null) cir.setReturnValue(target);
    }

    private static Object scpClassifiedDirective$createTarget(Player victim,
            Vec3 anchor, double score) {
        try {
            if (scpClassifiedDirective$targetConstructor == null) {
                Class<?> type = Class.forName(
                        "com.bl4ues.scpclassifieddirective.inventory.client.ContextPromptClient$ContextTarget");
                Constructor<?> constructor = type.getDeclaredConstructor(
                        BlockPos.class, int.class, boolean.class, Vec3.class,
                        String.class, String.class, String.class,
                        boolean.class, boolean.class, boolean.class,
                        boolean.class, net.minecraft.resources.ResourceLocation.class,
                        float.class, boolean.class, double.class);
                constructor.setAccessible(true);
                scpClassifiedDirective$targetConstructor = constructor;
            }
            return scpClassifiedDirective$targetConstructor.newInstance(
                    victim.blockPosition(), victim.getId(), true, anchor,
                    Scp714ExposureManager.REMOVE_INTERACTION_KEY,
                    "Remove", "SCP-714", true, true, true, true,
                    ContextPromptIcons.DEFAULT_ICON, 1.0F, false, score);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }
}
