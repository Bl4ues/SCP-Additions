from pathlib import Path
import json


def replace(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if new in text:
        return
    if old not in text:
        raise SystemExit(f"Expected block not found in {path}: {old[:160]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


entity = "src/main/java/net/mcreator/scpadditions/entity/Scp106Entity.java"

replace(
    entity,
    "import net.minecraft.core.BlockPos;\n",
    "import net.minecraft.core.BlockPos;\nimport net.minecraft.core.particles.ParticleTypes;\n",
)
replace(
    entity,
    "import net.minecraft.world.level.Level;\n",
    "import net.minecraft.world.level.ClipContext;\nimport net.minecraft.world.level.Level;\n",
)
replace(
    entity,
    "import net.minecraft.world.phys.AABB;\nimport net.minecraft.world.phys.Vec3;\n",
    "import net.minecraft.world.phys.AABB;\nimport net.minecraft.world.phys.HitResult;\nimport net.minecraft.world.phys.Vec3;\n",
)

replace(
    entity,
    "    private static final double RANGED_MIN_DISTANCE_SQR = 6.0D * 6.0D;\n"
    "    private static final double RANGED_MAX_DISTANCE_SQR = 12.0D * 12.0D;",
    "    private static final double RANGED_MIN_DISTANCE_SQR = 6.5D * 6.5D;\n"
    "    private static final double RANGED_MAX_DISTANCE_SQR = 11.5D * 11.5D;",
)
replace(
    entity,
    "    private static final int RANGED_PREPARE_TICKS = 8;\n"
    "    private static final int RANGED_AIM_LOCK_TICK = 38;\n"
    "    private static final int RANGED_RELEASE_TICK = 42;\n"
    "    private static final int RANGED_ATTACK_DURATION_TICKS = 69;\n"
    "    private static final int RANGED_SEGMENTS = 15;\n"
    "    private static final double RANGED_SEGMENT_SPACING = 0.65D;\n"
    "    private static final int RANGED_COOLDOWN_TICKS = 8 * 20;",
    "    private static final int RANGED_PREPARE_TICKS = 20;\n"
    "    private static final int RANGED_AIM_LOCK_TICK = 38;\n"
    "    private static final int RANGED_RELEASE_TICK = 42;\n"
    "    private static final int RANGED_ATTACK_DURATION_TICKS = 69;\n"
    "    private static final int RANGED_SEGMENTS = 15;\n"
    "    private static final double RANGED_SEGMENT_SPACING = 0.65D;\n"
    "    private static final int RANGED_COOLDOWN_TICKS = 14 * 20;\n"
    "    private static final int RANGED_ABORT_COOLDOWN_TICKS = 4 * 20;\n"
    "    private static final int RANGED_HAND_PARTICLE_START_TICK = 26;",
)
replace(
    entity,
    "    private Vec3 rangedLockedDirection = Vec3.ZERO;\n"
    "    private boolean rangedHit;\n",
    "    private Vec3 rangedLockedDirection = Vec3.ZERO;\n"
    "    private boolean rangedHit;\n"
    "    private boolean rangedBlocked;\n"
    "    private float emergenceYaw;\n",
)

replace(
    entity,
    '        tag.putBoolean("Scp106VanishForDespawn", vanishForDespawn);\n',
    '        tag.putBoolean("Scp106VanishForDespawn", vanishForDespawn);\n'
    '        tag.putFloat("Scp106EmergenceYaw", emergenceYaw);\n',
)
replace(
    entity,
    '        vanishForDespawn = tag.getBoolean("Scp106VanishForDespawn");\n'
    "        huntedPlayerId = tag.hasUUID(\"Scp106HuntedPlayer\")",
    '        vanishForDespawn = tag.getBoolean("Scp106VanishForDespawn");\n'
    '        emergenceYaw = tag.contains("Scp106EmergenceYaw")\n'
    '                ? tag.getFloat("Scp106EmergenceYaw") : getYRot();\n'
    "        huntedPlayerId = tag.hasUUID(\"Scp106HuntedPlayer\")",
)

replace(
    entity,
    "    private void tickEmergence() {\n"
    "        getNavigation().stop();\n"
    "        stopHorizontalMovement();",
    "    private void tickEmergence() {\n"
    "        getNavigation().stop();\n"
    "        stopHorizontalMovement();\n"
    "        lockEmergenceRotation();",
)
replace(
    entity,
    "    private void startEmergence(Emergence emergence) {\n"
    "        vanishForDespawn = false;\n"
    "        entityData.set(ATTACKING, false);",
    "    private void startEmergence(Emergence emergence) {\n"
    "        vanishForDespawn = false;\n"
    "        emergenceYaw = getYRot();\n"
    "        if (emergence == Emergence.GROUND) alignGroundEmergencePosition();\n"
    "        entityData.set(ATTACKING, false);",
)

replace(
    entity,
    "    private boolean isRangedOpportunity(Player player) {\n"
    "        if (player == null || rangedCooldownTicks > 0 || !onGround()\n"
    "                || !hasLineOfSight(player)) {\n"
    "            return false;\n"
    "        }\n"
    "        double distanceSqr = distanceToSqr(player);\n"
    "        return distanceSqr >= RANGED_MIN_DISTANCE_SQR\n"
    "                && distanceSqr <= RANGED_MAX_DISTANCE_SQR\n"
    "                && Math.abs(player.getY() - getY()) <= 1.75D;\n"
    "    }",
    "    private boolean isRangedOpportunity(Player player) {\n"
    "        if (player == null || rangedCooldownTicks > 0 || !onGround()\n"
    "                || !hasClearRangedPath(player)) {\n"
    "            return false;\n"
    "        }\n"
    "        double distanceSqr = distanceToSqr(player);\n"
    "        return distanceSqr >= RANGED_MIN_DISTANCE_SQR\n"
    "                && distanceSqr <= RANGED_MAX_DISTANCE_SQR\n"
    "                && Math.abs(player.getY() - getY()) <= 1.75D;\n"
    "    }",
)

replace(
    entity,
    "        rangedOpportunityTicks = 0;\n"
    "        rangedHit = false;\n"
    "        rangedLockedDirection = horizontalDirectionTo(target);\n"
    "        getNavigation().stop();\n"
    "        stopHorizontalMovement();",
    "        rangedOpportunityTicks = 0;\n"
    "        rangedHit = false;\n"
    "        rangedBlocked = false;\n"
    "        rangedLockedDirection = horizontalDirectionTo(target);\n"
    "        getNavigation().stop();\n"
    "        stopHorizontalMovement();\n"
    "        triggerAnim(\"movement\", \"ranged_attack\");",
)
replace(
    entity,
    "    private void tickRangedAttack(Player target) {\n"
    "        getNavigation().stop();\n"
    "        stopHorizontalMovement();\n"
    "        rangedAttackTicks++;\n\n"
    "        if (rangedAttackTicks <= RANGED_AIM_LOCK_TICK) {",
    "    private void tickRangedAttack(Player target) {\n"
    "        getNavigation().stop();\n"
    "        stopHorizontalMovement();\n"
    "        rangedAttackTicks++;\n\n"
    "        if (rangedAttackTicks < RANGED_AIM_LOCK_TICK\n"
    "                && !hasClearRangedPath(target)) {\n"
    "            cancelRangedAttack();\n"
    "            rangedCooldownTicks = RANGED_ABORT_COOLDOWN_TICKS;\n"
    "            return;\n"
    "        }\n\n"
    "        if (rangedAttackTicks <= RANGED_AIM_LOCK_TICK) {",
)
replace(
    entity,
    "        int segment = rangedAttackTicks - RANGED_RELEASE_TICK;\n"
    "        if (segment >= 0 && segment < RANGED_SEGMENTS) {\n"
    "            spawnRangedSegment(segment);\n"
    "        }",
    "        if (rangedAttackTicks >= RANGED_HAND_PARTICLE_START_TICK\n"
    "                && rangedAttackTicks <= RANGED_RELEASE_TICK + 2) {\n"
    "            spawnRangedHandParticles();\n"
    "        }\n\n"
    "        int segment = rangedAttackTicks - RANGED_RELEASE_TICK;\n"
    "        if (segment >= 0 && segment < RANGED_SEGMENTS && !rangedBlocked) {\n"
    "            spawnRangedSegment(segment);\n"
    "        }",
)
replace(
    entity,
    "    private void spawnRangedSegment(int segment) {\n"
    "        if (!(level() instanceof ServerLevel serverLevel)\n"
    "                || rangedLockedDirection.lengthSqr() < 1.0E-6D) {\n"
    "            return;\n"
    "        }\n\n"
    "        double distance = 0.75D + segment * RANGED_SEGMENT_SPACING;\n"
    "        Vec3 point = position().add(rangedLockedDirection.scale(distance));\n"
    "        double surfaceY = findCorrosionSurfaceY(point.x, point.y, point.z);\n"
    "        Vec3 puddle = new Vec3(point.x, surfaceY + 0.025D, point.z);\n"
    "        double sizeScale = 1.25D - segment * (0.35D / (RANGED_SEGMENTS - 1));\n\n"
    "        serverLevel.sendParticles(\n"
    "                ScpAdditionsModParticleTypes.SCP_106_CORROSION.get(),\n"
    "                puddle.x, puddle.y, puddle.z,\n"
    "                0, sizeScale, 0.0D, 0.0D, 1.0D);",
    "    private void spawnRangedSegment(int segment) {\n"
    "        if (!(level() instanceof ServerLevel serverLevel)\n"
    "                || rangedLockedDirection.lengthSqr() < 1.0E-6D) {\n"
    "            return;\n"
    "        }\n\n"
    "        double distance = 0.75D + segment * RANGED_SEGMENT_SPACING;\n"
    "        double previousDistance = Math.max(0.30D,\n"
    "                distance - RANGED_SEGMENT_SPACING);\n"
    "        Vec3 rayStart = position().add(0.0D, 0.38D, 0.0D)\n"
    "                .add(rangedLockedDirection.scale(previousDistance));\n"
    "        Vec3 rayEnd = position().add(0.0D, 0.38D, 0.0D)\n"
    "                .add(rangedLockedDirection.scale(distance));\n"
    "        if (!hasClearBlockRay(rayStart, rayEnd)) {\n"
    "            rangedBlocked = true;\n"
    "            return;\n"
    "        }\n\n"
    "        Vec3 point = position().add(rangedLockedDirection.scale(distance));\n"
    "        double surfaceY = findCorrosionSurfaceY(point.x, point.y, point.z);\n"
    "        Vec3 puddle = new Vec3(point.x, surfaceY + 0.025D, point.z);\n"
    "        double progress = segment / (double) (RANGED_SEGMENTS - 1);\n"
    "        double sizeScale = Mth.lerp(progress, 1.25D, 0.38D);\n"
    "        double opacityScale = Mth.lerp(progress, 1.0D, 0.28D);\n\n"
    "        serverLevel.sendParticles(\n"
    "                ScpAdditionsModParticleTypes.SCP_106_CORROSION.get(),\n"
    "                puddle.x, puddle.y, puddle.z,\n"
    "                0, sizeScale, opacityScale, 0.0D, 1.0D);",
)

helpers = r'''    private boolean hasClearRangedPath(Player target) {
        if (target == null || !hasLineOfSight(target)) return false;
        Vec3 start = position().add(0.0D, 0.72D, 0.0D);
        Vec3 targetCenter = target.position().add(0.0D, 0.55D, 0.0D);
        Vec3 motion = target.getDeltaMovement();
        Vec3 predicted = targetCenter.add(motion.x * 6.0D, 0.0D,
                motion.z * 6.0D);
        return hasClearRangedCorridor(start, targetCenter)
                && hasClearRangedCorridor(start, predicted);
    }

    private boolean hasClearRangedCorridor(Vec3 start, Vec3 end) {
        Vec3 horizontal = end.subtract(start).multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            return hasClearBlockRay(start, end);
        }
        Vec3 side = new Vec3(-horizontal.z, 0.0D, horizontal.x)
                .normalize().scale(0.22D);
        return hasClearBlockRay(start, end)
                && hasClearBlockRay(start.add(side), end.add(side))
                && hasClearBlockRay(start.subtract(side), end.subtract(side));
    }

    private boolean hasClearBlockRay(Vec3 start, Vec3 end) {
        HitResult obstruction = level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return obstruction.getType() == HitResult.Type.MISS;
    }

    private void spawnRangedHandParticles() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        Vec3 forward = rangedLockedDirection.lengthSqr() < 1.0E-6D
                ? getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize()
                : rangedLockedDirection;
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        double progress = Mth.clamp((rangedAttackTicks
                - RANGED_HAND_PARTICLE_START_TICK)
                / (double) Math.max(1, RANGED_RELEASE_TICK
                - RANGED_HAND_PARTICLE_START_TICK), 0.0D, 1.0D);
        Vec3 hand = position()
                .add(0.0D, Mth.lerp(progress, 1.28D, 0.48D), 0.0D)
                .add(forward.scale(Mth.lerp(progress, 0.18D, 0.72D)))
                .add(right.scale(0.36D));
        serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                hand.x, hand.y, hand.z, 2,
                0.055D, 0.055D, 0.055D, 0.008D);
        serverLevel.sendParticles(ParticleTypes.SMOKE,
                hand.x, hand.y, hand.z, 1,
                0.035D, 0.035D, 0.035D, 0.004D);
    }

    private void alignGroundEmergencePosition() {
        AABB box = getBoundingBox().deflate(0.025D);
        double originalY = getY();
        double bestSurfaceY = Double.NEGATIVE_INFINITY;
        int x = Mth.floor(getX());
        int z = Mth.floor(getZ());
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = Mth.floor(originalY) + 1;
                y >= Mth.floor(originalY) - 2; y--) {
            mutable.set(x, y, z);
            BlockState state = level().getBlockState(mutable);
            VoxelShape shape = state.getCollisionShape(level(), mutable);
            for (AABB local : shape.toAabbs()) {
                double top = y + local.maxY;
                if (top >= originalY - 0.15D
                        && top <= originalY + 1.10D
                        && top > bestSurfaceY) {
                    bestSurfaceY = top;
                }
            }
        }
        if (bestSurfaceY > Double.NEGATIVE_INFINITY) {
            double lift = bestSurfaceY - originalY;
            AABB aligned = box.move(0.0D, lift, 0.0D);
            if (level().noCollision(this, aligned)) {
                setPos(getX(), bestSurfaceY, getZ());
                return;
            }
        }
        if (level().noCollision(this, box)) return;
        for (double lift = 0.10D; lift <= 1.60D; lift += 0.10D) {
            if (level().noCollision(this, box.move(0.0D, lift, 0.0D))) {
                setPos(getX(), originalY + lift, getZ());
                return;
            }
        }
    }

    private void lockEmergenceRotation() {
        setYRot(emergenceYaw);
        setYBodyRot(emergenceYaw);
        setYHeadRot(emergenceYaw);
    }

'''
replace(
    entity,
    "    private double findCorrosionSurfaceY(double x, double referenceY,",
    helpers + "    private double findCorrosionSurfaceY(double x, double referenceY,",
)
replace(
    entity,
    "        rangedLockedDirection = Vec3.ZERO;\n"
    "        rangedHit = false;\n"
    "    }",
    "        rangedLockedDirection = Vec3.ZERO;\n"
    "        rangedHit = false;\n"
    "        rangedBlocked = false;\n"
    "    }",
)
replace(
    entity,
    "        controllers.add(movementController);",
    "        movementController.triggerableAnim(\"ranged_attack\",\n"
    "                RANGED_ATTACK_ANIMATION);\n"
    "        controllers.add(movementController);",
)

particle = "src/main/java/net/mcreator/scpadditions/client/Scp106CorrosionParticle.java"
replace(
    particle,
    "    private final SpriteSet sprites;\n",
    "    private final SpriteSet sprites;\n    private final float maximumAlpha;\n",
)
replace(
    particle,
    "            double z, double sizeScale, SpriteSet sprites) {",
    "            double z, double sizeScale, double opacityScale,\n"
    "            SpriteSet sprites) {",
)
replace(
    particle,
    "        float safeScale = (float) Mth.clamp(sizeScale, 0.75D, 1.45D);",
    "        float safeScale = (float) Mth.clamp(sizeScale, 0.30D, 1.45D);\n"
    "        float safeOpacity = (float) Mth.clamp(\n"
    "                opacityScale > 0.0D ? opacityScale : 1.0D, 0.25D, 1.0D);\n"
    "        this.maximumAlpha = MAX_ALPHA * safeOpacity;",
)
replace(particle, "        this.setAlpha(MAX_ALPHA);", "        this.setAlpha(maximumAlpha);")
replace(
    particle,
    "        this.setAlpha(MAX_ALPHA * fade);",
    "        this.setAlpha(maximumAlpha * fade);",
)
replace(
    particle,
    "            return new Scp106CorrosionParticle(level, x, y, z,\n"
    "                    sizeScale, sprites);",
    "            double opacityScale = velocityY > 0.0D\n"
    "                    ? velocityY : 1.0D;\n"
    "            return new Scp106CorrosionParticle(level, x, y, z,\n"
    "                    sizeScale, opacityScale, sprites);",
)

timers = "src/main/java/net/mcreator/scpadditions/vitals/client/ScpSpawnTimersOverlay.java"
replace(
    timers,
    "    private ScpSpawnTimersOverlay() {\n    }\n\n    public static void render",
    "    private ScpSpawnTimersOverlay() {\n    }\n\n"
    "    public static int occupiedHeight() {\n"
    "        return Scp079EnergyClientState.spawnTimersVisible() ? HEIGHT : 0;\n"
    "    }\n\n"
    "    public static void render",
)
replace(
    timers,
    "        int occupied = Scp079EnergyOverlay.occupiedHeight();\n"
    "        int x = screenWidth - WIDTH - MARGIN;\n"
    "        int y = MARGIN + occupied + (occupied > 0 ? 6 : 0);",
    "        int x = screenWidth - WIDTH - MARGIN;\n"
    "        int y = MARGIN;",
)

energy = "src/main/java/net/mcreator/scpadditions/vitals/client/Scp079EnergyOverlay.java"
replace(
    energy,
    "        if (energy) renderEnergy(graphics, minecraft, screenWidth);\n"
    "        if (decisions) renderDecisionFeed(graphics, minecraft, screenWidth,\n"
    "                energy ? MARGIN + ENERGY_HEIGHT + GAP : MARGIN);",
    "        int roamerHeight = ScpSpawnTimersOverlay.occupiedHeight();\n"
    "        int baseY = MARGIN + roamerHeight + (roamerHeight > 0 ? GAP : 0);\n"
    "        if (energy) renderEnergy(graphics, minecraft, screenWidth, baseY);\n"
    "        if (decisions) renderDecisionFeed(graphics, minecraft, screenWidth,\n"
    "                energy ? baseY + ENERGY_HEIGHT + GAP : baseY);",
)
replace(
    energy,
    "    private static void renderEnergy(GuiGraphics graphics,\n"
    "            Minecraft minecraft, int screenWidth) {",
    "    private static void renderEnergy(GuiGraphics graphics,\n"
    "            Minecraft minecraft, int screenWidth, int y) {",
)
replace(
    energy,
    "        int x = screenWidth - ENERGY_WIDTH - MARGIN;\n"
    "        int y = MARGIN;",
    "        int x = screenWidth - ENERGY_WIDTH - MARGIN;",
)

chase = "src/main/java/net/mcreator/scpadditions/client/Scp106ChaseSound.java"
replace(
    chase,
    "    private static final int FADE_OUT_TICKS = 40;\n\n"
    "    private int fadeTicksRemaining = -1;\n"
    "    private boolean playStopCue;",
    "    private static final int FADE_OUT_TICKS = 32;\n"
    "    private static final int STOP_CUE_LEAD_TICKS = 14;\n"
    "    private static final float STOP_CUE_VOLUME = 0.34F;\n\n"
    "    private int fadeTicksRemaining = -1;\n"
    "    private boolean playStopCue;\n"
    "    private boolean stopCuePlayed;",
)
replace(
    chase,
    "        if (fadeTicksRemaining < 0) return;\n"
    "        if (fadeTicksRemaining == 0) {\n"
    "            volume = 0.0F;\n"
    "            stop();\n"
    "            if (playStopCue && minecraft.player != null\n"
    "                    && minecraft.level != null) {\n"
    "                minecraft.getSoundManager().play(\n"
    "                        SimpleSoundInstance.forUI(Scp106Sounds.STOP.get(),\n"
    "                                1.0F, 1.0F));\n"
    "            }\n"
    "            return;\n"
    "        }",
    "        if (fadeTicksRemaining < 0) return;\n"
    "        if (playStopCue && !stopCuePlayed\n"
    "                && fadeTicksRemaining <= STOP_CUE_LEAD_TICKS\n"
    "                && minecraft.player != null && minecraft.level != null) {\n"
    "            minecraft.getSoundManager().play(\n"
    "                    SimpleSoundInstance.forUI(Scp106Sounds.STOP.get(),\n"
    "                            1.0F, STOP_CUE_VOLUME));\n"
    "            stopCuePlayed = true;\n"
    "        }\n"
    "        if (fadeTicksRemaining == 0) {\n"
    "            volume = 0.0F;\n"
    "            stop();\n"
    "            return;\n"
    "        }",
)

locator = "src/main/java/net/mcreator/scpadditions/roamer/Scp106EmergenceLocator.java"
replace(
    locator,
    "            // emerge_wall is authored opposite the normal entity forward axis.\n"
    "            float wallYaw = Mth.wrapDegrees(yawFor(outward.getStepX(),\n"
    "                    outward.getStepZ()) + 180.0F);\n"
    "            return new Placement(center, wallYaw, Emergence.WALL);",
    "            // Face the open side directly. The animation begins behind\n"
    "            // the model on local +Z, so its initial pose remains inside the wall.\n"
    "            float wallYaw = yawFor(outward.getStepX(), outward.getStepZ());\n"
    "            return new Placement(center, wallYaw, Emergence.WALL);",
)

surface = "src/main/java/net/mcreator/scpadditions/event/Scp106SurfaceEvents.java"
replace(
    surface,
    "        // The wall emergence animation moves opposite the model's ordinary\n"
    "        // forward axis, so its real surface normal is the inverse direction.\n"
    "        Vec3 outward = modelForward.scale(-1.0D);",
    "        // The model faces the open side, so its forward vector is the\n"
    "        // actual outward normal used by the wall portal.\n"
    "        Vec3 outward = modelForward;",
)

changelog = Path("CHANGELOG.md")
text = changelog.read_text(encoding="utf-8")
marker = (
    "- Added a ranged attack that throws a straight trail of corrosion across the floor "
    "when the player creates distance but remains in clear view, dealing damage, Wither, "
    "and Slowness on a direct hit;\n"
)
if "waits for a reliable opening" not in text and marker in text:
    text = text.replace(
        marker,
        marker
        + "- Refined the ranged attack so SCP-106 waits for a reliable opening, performs its full throwing animation, and has the corrosion stop at walls instead of passing through them;\n"
        + "- Added black buildup particles around SCP-106's right hand, while the end of the thrown corrosion now becomes smaller and fainter as it loses strength;\n",
        1,
    )
audio_marker = "- Added phasing sounds and chase soundtrack;\n"
if "final cue begins during the fade" not in text and audio_marker in text:
    text = text.replace(
        audio_marker,
        audio_marker
        + "- Adjusted the chase ending so its final cue begins during the fade at a lower volume, blending both sounds more naturally;\n"
        + "- Corrected ground and wall emergence alignment so SCP-106 begins at its final height and keeps the intended wall-facing direction throughout the animation;\n",
        1,
    )
debug_marker = "- Added optional Debug Tools displays showing each roamer's state, next check, and latest result.\n"
if "Reordered the debug HUD stack" not in text and debug_marker in text:
    text = text.replace(
        debug_marker,
        "- Added optional Debug Tools displays showing each roamer's state, next check, and latest result;\n"
        + "- Reordered the debug HUD stack so roamer information appears first, followed by SCP-079 processing power and its decision log.\n",
        1,
    )
changelog.write_text(text, encoding="utf-8")

animation_path = Path(
    "src/main/resources/assets/scp_additions/animations/entity/scp106.animation.json"
)
animations = json.loads(animation_path.read_text(encoding="utf-8"))["animations"]
ranged = animations.get("ranged_attack")
if not ranged:
    raise SystemExit("The supplied ranged_attack animation is missing")
length = float(ranged.get("animation_length", 0.0))
if not 3.35 <= length <= 3.50:
    raise SystemExit(f"Unexpected ranged_attack length: {length}")
print("Using supplied ranged_attack animation:", length, "seconds")
