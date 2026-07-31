from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"Missing expected section: {label}")
    return text.replace(old, new, 1)

path = Path("src/main/java/com/bl4ues/scpinventory/client/ContextPromptClient.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
    "import net.minecraft.core.BlockPos;\n",
    "import net.minecraft.core.BlockPos;\nimport net.minecraft.core.Direction;\n",
    "Direction import")
text = replace_once(text,
    "    private static final double PRECISE_AIM_RADIUS_SQR = 0.60D * 0.60D;\n",
    "    private static final double PRECISE_AIM_RADIUS_SQR = 0.60D * 0.60D;\n"
    "    private static final double ELEVATOR_BUTTON_AIM_RADIUS_SQR =\n"
    "            0.20D * 0.20D;\n",
    "elevator aim radius")
text = replace_once(text,
    "                Vec3 anchor = rule.resolveBlockAnchor(rulePos, ruleState);\n"
    "                double score = scorePoint(anchor, eye, look, rule.range(),\n"
    "                        directHit, rule.priority(), rule.requiresPreciseAim());\n"
    "                if (isCurrentBlockTarget(rulePos, rule.interactionKey())) {\n"
    "                    score -= TARGET_STICKINESS_BONUS;\n"
    "                }\n",
    "                Vec3 anchor = rule.resolveBlockAnchor(rulePos, ruleState);\n"
    "                if (isElevatorStationButton(rule.interactionKey())\n"
    "                        && !isStationButtonViewedFromFront(eye, anchor,\n"
    "                        ruleState)) {\n"
    "                    continue;\n"
    "                }\n"
    "                double score = scorePoint(anchor, eye, look, rule.range(),\n"
    "                        directHit, rule.priority(), rule.requiresPreciseAim(),\n"
    "                        preciseAimRadiusSqr(rule.interactionKey()));\n"
    "                if (!isElevatorButton(rule.interactionKey())\n"
    "                        && isCurrentBlockTarget(rulePos,\n"
    "                        rule.interactionKey())) {\n"
    "                    score -= TARGET_STICKINESS_BONUS;\n"
    "                }\n",
    "block scoring")
text = replace_once(text,
    "                Vec3 anchor = rule.resolveEntityAnchor(entity);\n"
    "                double score = scorePoint(anchor, eye, look, rule.range(),\n"
    "                        directHit, rule.priority(),\n"
    "                        rule.requiresPreciseAim());\n"
    "                if (isCurrentEntityTarget(entity.getId(),\n"
    "                        rule.interactionKey())) {\n"
    "                    score -= TARGET_STICKINESS_BONUS;\n"
    "                }\n",
    "                Vec3 anchor = rule.resolveEntityAnchor(entity);\n"
    "                if (isElevatorCarriageButton(rule.interactionKey())\n"
    "                        && !isCarriageButtonViewedFromFront(eye, anchor,\n"
    "                        entity)) {\n"
    "                    continue;\n"
    "                }\n"
    "                double score = scorePoint(anchor, eye, look, rule.range(),\n"
    "                        directHit, rule.priority(),\n"
    "                        rule.requiresPreciseAim(),\n"
    "                        preciseAimRadiusSqr(rule.interactionKey()));\n"
    "                if (!isElevatorButton(rule.interactionKey())\n"
    "                        && isCurrentEntityTarget(entity.getId(),\n"
    "                        rule.interactionKey())) {\n"
    "                    score -= TARGET_STICKINESS_BONUS;\n"
    "                }\n",
    "entity scoring")
text = replace_once(text,
    "    private static double scorePoint(Vec3 point, Vec3 eye, Vec3 look,\n"
    "        double reach, boolean directHit, int priority,\n"
    "        boolean preciseAim) {\n",
    "    private static double scorePoint(Vec3 point, Vec3 eye, Vec3 look,\n"
    "            double reach, boolean directHit, int priority,\n"
    "            boolean preciseAim, double preciseAimRadiusSqr) {\n",
    "score signature")
text = replace_once(text,
    "    if (preciseAim && centerPenalty > PRECISE_AIM_RADIUS_SQR) {\n",
    "    if (preciseAim && centerPenalty > preciseAimRadiusSqr) {\n",
    "score radius")
marker = "    private static ScreenPoint projectToScreen(Minecraft minecraft,\n"
helpers = '''    private static boolean isElevatorButton(String interactionKey) {
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

'''
text = replace_once(text, marker, helpers + marker, "prompt helpers")
text = replace_once(text,
    "        double depth = Math.abs(transformed.z());\n"
    "        if (depth < 0.05D) return null;\n",
    "        double depth = -transformed.z();\n"
    "        if (depth <= 0.05D) return null;\n",
    "camera depth")
text = replace_once(text,
    "        int x = (int) Math.round(screenWidth / 2.0D\n"
    "                - transformed.x() * scale / depth);\n",
    "        int x = (int) Math.round(screenWidth / 2.0D\n"
    "                + transformed.x() * scale / depth);\n",
    "horizontal projection")
path.write_text(text, encoding="utf-8")

path = Path("src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorModule.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
    "            if (Math.min(upDistance, downDistance) > 0.32D * 0.32D) {\n"
    "                return InteractionResult.PASS;\n"
    "            }\n",
    "            if (Math.min(upDistance, downDistance) > 0.20D * 0.20D) {\n"
    "                return InteractionResult.PASS;\n"
    "            }\n"
    "            Vec3 selectedButton = upDistance <= downDistance\n"
    "                    ? upButton : downButton;\n"
    "            Direction facing = state.getValue(FACING);\n"
    "            Vec3 outward = new Vec3(facing.getStepX(), 0.0D,\n"
    "                    facing.getStepZ());\n"
    "            if (player.getEyePosition().subtract(selectedButton)\n"
    "                    .dot(outward) <= 0.02D) {\n"
    "                return InteractionResult.PASS;\n"
    "            }\n",
    "station interaction radius and side")
path.write_text(text, encoding="utf-8")

path = Path("src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorCarriageEntity.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
    "    private static final double BUTTON_HIT_RADIUS_SQR = 0.32D * 0.32D;\n",
    "    private static final double BUTTON_HIT_RADIUS_SQR = 0.20D * 0.20D;\n",
    "carriage radius")
text = replace_once(text,
    "    private double previousServerY;\n"
    "    private double previousClientY = Double.NaN;\n",
    "    private double previousServerY;\n"
    "    private int clientLerpSteps;\n"
    "    private double clientLerpX;\n"
    "    private double clientLerpY;\n"
    "    private double clientLerpZ;\n"
    "    private float clientLerpYRot;\n"
    "    private float clientLerpXRot;\n",
    "client lerp fields")
text = replace_once(text,
    "        if (level().isClientSide) {\n"
    "            double currentY = getY();\n"
    "            double oldY = Double.isNaN(previousClientY)\n"
    "                    ? currentY : previousClientY;\n"
    "            double clientDeltaY = currentY - oldY;\n"
    "            previousClientY = currentY;\n"
    "            previousServerY = oldY;\n"
    "            if (Math.abs(clientDeltaY) > 1.0D) {\n"
    "                clientDeltaY = 0.0D;\n"
    "            }\n"
    "            resolveNearbyEntities(clientDeltaY);\n"
    "            return;\n"
    "        }\n",
    "        if (level().isClientSide) {\n"
    "            double oldY = getY();\n"
    "            tickClientInterpolation();\n"
    "            double clientDeltaY = getY() - oldY;\n"
    "            previousServerY = oldY;\n"
    "            if (Math.abs(clientDeltaY) > 1.0D) {\n"
    "                clientDeltaY = 0.0D;\n"
    "            }\n"
    "            resolveNearbyEntities(clientDeltaY);\n"
    "            return;\n"
    "        }\n",
    "client tick")
marker = "    @Override\n    public void tick() {\n"
methods = '''    @Override
    public void lerpTo(double x, double y, double z, float yRot,
            float xRot, int steps, boolean teleport) {
        if (!level().isClientSide || teleport) {
            super.lerpTo(x, y, z, yRot, xRot, steps, teleport);
            return;
        }
        clientLerpX = x;
        clientLerpY = y;
        clientLerpZ = z;
        clientLerpYRot = yRot;
        clientLerpXRot = xRot;
        clientLerpSteps = Math.max(1, steps);
    }

    private void tickClientInterpolation() {
        if (clientLerpSteps <= 0) return;
        double fraction = 1.0D / clientLerpSteps;
        setPos(Mth.lerp(fraction, getX(), clientLerpX),
                Mth.lerp(fraction, getY(), clientLerpY),
                Mth.lerp(fraction, getZ(), clientLerpZ));
        setYRot(Mth.rotLerp((float) fraction, getYRot(),
                clientLerpYRot));
        setXRot(Mth.lerp((float) fraction, getXRot(),
                clientLerpXRot));
        clientLerpSteps--;
    }

'''
text = replace_once(text, marker, methods + marker, "lerp methods")
text = replace_once(text,
    "            if ((inside || standing) && Math.abs(deltaY) > 1.0E-7D) {\n"
    "                entity.move(MoverType.SHULKER,\n"
    "                        new Vec3(0.0D, deltaY, 0.0D));\n"
    "                entity.fallDistance = 0.0F;\n"
    "                if (standing) stabilizeGroundedEntity(entity);\n"
    "            }\n",
    "            if ((inside || standing) && Math.abs(deltaY) > 1.0E-7D) {\n"
    "                if (level().isClientSide) {\n"
    "                    entity.setPos(entity.getX(), entity.getY() + deltaY,\n"
    "                            entity.getZ());\n"
    "                } else {\n"
    "                    entity.move(MoverType.SHULKER,\n"
    "                            new Vec3(0.0D, deltaY, 0.0D));\n"
    "                }\n"
    "                entity.fallDistance = 0.0F;\n"
    "                if (standing) stabilizeGroundedEntity(entity);\n"
    "            }\n",
    "passenger translation")
text = replace_once(text,
    "        boolean up = upDistance <= downDistance;\n"
    "        return handleContextInteraction(serverPlayer,\n",
    "        boolean up = upDistance <= downDistance;\n"
    "        Vec3 selectedButton = up ? contextAnchor(true)\n"
    "                : contextAnchor(false);\n"
    "        Vec3 inward = position().subtract(selectedButton)\n"
    "                .multiply(1.0D, 0.0D, 1.0D);\n"
    "        if (inward.lengthSqr() >= 1.0E-6D\n"
    "                && player.getEyePosition().subtract(selectedButton)\n"
    "                .dot(inward.normalize()) <= 0.02D) {\n"
    "            return InteractionResult.PASS;\n"
    "        }\n"
    "        return handleContextInteraction(serverPlayer,\n",
    "carriage front side")
path.write_text(text, encoding="utf-8")

print("Applied elevator prompt and smoothing fixes")
