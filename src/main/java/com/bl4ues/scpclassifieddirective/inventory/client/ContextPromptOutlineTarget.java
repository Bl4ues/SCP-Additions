package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.entity.PlayerCorpseEntity;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorCarriageEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Read-only bridge from the contextual prompt selector to the shared thin
 * outline renderer. The selector deliberately keeps its implementation record
 * private, so this bridge reflects only stable presentation values instead of
 * coupling rendering to the selection algorithm.
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
            Vec3 anchor = (Vec3) ACCESS.anchorMethod().invoke(promptTarget);
            String interactionKey = (String) ACCESS.interactionKeyMethod()
                    .invoke(promptTarget);
            if (entity) {
                int entityId = (int) ACCESS.entityIdMethod().invoke(promptTarget);
                Entity candidate = minecraft.level.getEntity(entityId);
                if (candidate instanceof PlayerCorpseEntity corpse
                        && corpse.isAlive() && !corpse.isRemoved()) {
                    return Target.entity(corpse, anchor, interactionKey);
                }
                if (candidate instanceof CoreRoomElevatorCarriageEntity carriage
                        && carriage.isAlive() && !carriage.isRemoved()
                        && isElevatorButton(interactionKey)) {
                    return Target.entity(carriage, anchor, interactionKey);
                }
                return null;
            }

            BlockPos pos = (BlockPos) ACCESS.posMethod().invoke(promptTarget);
            if (pos == null || minecraft.level.getBlockState(pos).isAir()) {
                return null;
            }
            return Target.block(pos.immutable(), anchor, interactionKey);
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

    private static boolean isElevatorButton(String interactionKey) {
        return interactionKey != null
                && (interactionKey.startsWith("elevator_station_")
                || interactionKey.startsWith("elevator_carriage_"));
    }

    private static boolean isScp914Control(String interactionKey) {
        return "scp_914_dial".equals(interactionKey)
                || "scp_914_start".equals(interactionKey);
    }

    private static Access createAccess() {
        try {
            Field targetField = ContextPromptClient.class.getDeclaredField("target");
            targetField.setAccessible(true);
            Class<?> type = targetField.getType();
            Method posMethod = type.getDeclaredMethod("pos");
            Method entityMethod = type.getDeclaredMethod("entity");
            Method entityIdMethod = type.getDeclaredMethod("entityId");
            Method anchorMethod = type.getDeclaredMethod("anchor");
            Method interactionKeyMethod = type.getDeclaredMethod("interactionKey");
            posMethod.setAccessible(true);
            entityMethod.setAccessible(true);
            entityIdMethod.setAccessible(true);
            anchorMethod.setAccessible(true);
            interactionKeyMethod.setAccessible(true);
            return new Access(targetField, posMethod, entityMethod,
                    entityIdMethod, anchorMethod, interactionKeyMethod);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not initialize contextual prompt outline bridge",
                    exception);
            return null;
        }
    }

    record Target(BlockPos blockPos, Entity entity, Vec3 anchor,
            String interactionKey) {
        static Target block(BlockPos pos, Vec3 anchor, String interactionKey) {
            return new Target(pos, null, anchor, interactionKey);
        }

        static Target entity(Entity entity, Vec3 anchor, String interactionKey) {
            return new Target(null, entity, anchor, interactionKey);
        }

        boolean isBlock() {
            return blockPos != null;
        }

        boolean isCorpse() {
            return entity instanceof PlayerCorpseEntity;
        }

        boolean isElevatorButton() {
            return anchor != null && ContextPromptOutlineTarget
                    .isElevatorButton(interactionKey);
        }

        boolean isScp914Control() {
            return blockPos != null && anchor != null
                    && ContextPromptOutlineTarget.isScp914Control(interactionKey);
        }
    }

    private record Access(Field targetField, Method posMethod,
            Method entityMethod, Method entityIdMethod, Method anchorMethod,
            Method interactionKeyMethod) {
    }
}
