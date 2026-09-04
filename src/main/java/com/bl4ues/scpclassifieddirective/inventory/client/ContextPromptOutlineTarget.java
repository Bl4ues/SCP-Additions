package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.entity.PlayerCorpseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Read-only bridge from the contextual prompt selector to the shared thin
 * outline renderer. The selector deliberately keeps its implementation record
 * private, so this bridge reflects only the three stable values required for
 * presentation instead of coupling rendering to the selection algorithm.
 */
final class ContextPromptOutlineTarget {
    private static final Access ACCESS = createAccess();
    private static boolean warned;

    private ContextPromptOutlineTarget() {
    }

    static Target current(Minecraft minecraft) {
        if (minecraft.level == null || ACCESS == null) return null;
        try {
            Object promptTarget = ACCESS.targetField().get(null);
            if (promptTarget == null) return null;

            boolean entity = (boolean) ACCESS.entityMethod().invoke(promptTarget);
            if (entity) {
                int entityId = (int) ACCESS.entityIdMethod().invoke(promptTarget);
                Entity candidate = minecraft.level.getEntity(entityId);
                if (candidate instanceof PlayerCorpseEntity corpse
                        && corpse.isAlive() && !corpse.isRemoved()) {
                    return Target.corpse(corpse);
                }
                return null;
            }

            BlockPos pos = (BlockPos) ACCESS.posMethod().invoke(promptTarget);
            if (pos == null || minecraft.level.getBlockState(pos).isAir()) {
                return null;
            }
            return Target.block(pos.immutable());
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!warned) {
                warned = true;
                ScpClassifiedDirectiveMod.LOGGER.warn(
                        "Could not resolve contextual prompt target for thin outline",
                        exception);
            }
            return null;
        }
    }

    private static Access createAccess() {
        try {
            Field targetField = ContextPromptClient.class.getDeclaredField("target");
            targetField.setAccessible(true);
            Class<?> type = targetField.getType();
            Method posMethod = type.getDeclaredMethod("pos");
            Method entityMethod = type.getDeclaredMethod("entity");
            Method entityIdMethod = type.getDeclaredMethod("entityId");
            posMethod.setAccessible(true);
            entityMethod.setAccessible(true);
            entityIdMethod.setAccessible(true);
            return new Access(targetField, posMethod, entityMethod, entityIdMethod);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not initialize contextual prompt outline bridge",
                    exception);
            return null;
        }
    }

    record Target(BlockPos blockPos, PlayerCorpseEntity corpse) {
        static Target block(BlockPos pos) {
            return new Target(pos, null);
        }

        static Target corpse(PlayerCorpseEntity corpse) {
            return new Target(null, corpse);
        }

        boolean isBlock() {
            return blockPos != null;
        }

        boolean isCorpse() {
            return corpse != null;
        }
    }

    private record Access(Field targetField, Method posMethod,
            Method entityMethod, Method entityIdMethod) {
    }
}
