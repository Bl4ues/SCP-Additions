from pathlib import Path

root = Path(__file__).resolve().parents[2]
path = root / "src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorCarriageEntity.java"
text = path.read_text(encoding="utf-8")


def replace_once(old, new, label):
    global text
    if old not in text:
        raise RuntimeError(f"Could not locate {label}")
    text = text.replace(old, new, 1)


def replace_between(start, end, replacement, label):
    global text
    try:
        first = text.index(start)
        last = text.index(end, first)
    except ValueError as exc:
        raise RuntimeError(f"Could not locate {label}") from exc
    text = text[:first] + replacement + text[last:]


replace_once('import net.minecraft.world.level.Level;\n',
             'import net.minecraft.world.level.Level;\nimport net.minecraft.world.level.block.SoundType;\n',
             'SoundType import')
replace_once('import java.util.ArrayList;\nimport java.util.Arrays;\nimport java.util.List;\n',
             'import java.util.ArrayList;\nimport java.util.Arrays;\nimport java.util.HashMap;\nimport java.util.HashSet;\nimport java.util.List;\nimport java.util.Map;\nimport java.util.Optional;\nimport java.util.Set;\n',
             'collection imports')
replace_once('    private double previousServerY;\n',
             '    private double previousServerY;\n'
             '    private final Map<Integer, Vec3> previousEntityPositions = new HashMap<>();\n'
             '    private final Map<Integer, Double> cabinStepDistance = new HashMap<>();\n',
             'tracking fields')

replace_between('    private void resolveNearbyEntities(double deltaY) {\n',
                '    private boolean isStandingOnFloor(Entity entity, double oldFloorY) {\n',
'''    private void resolveNearbyEntities(double deltaY) {
        AABB outer = cabinOuterBox().inflate(0.45D, 0.45D, 0.45D);
        List<Entity> nearby = level().getEntities(this, outer,
                entity -> entity.isAlive() && !entity.noPhysics
                        && !(entity instanceof CoreRoomElevatorCarriageEntity)
                        && !(entity instanceof Player player
                        && player.isSpectator()));
        Set<Integer> present = new HashSet<>();
        for (Entity entity : nearby) {
            present.add(entity.getId());
            Vec3 previous = previousEntityPositions.getOrDefault(
                    entity.getId(), entity.position());
            boolean inside = cabinInteriorBox().intersects(entity.getBoundingBox());
            boolean standing = isStandingOnFloor(entity, previousServerY);
            if ((inside || standing) && Math.abs(deltaY) > 1.0E-7D) {
                entity.move(MoverType.SHULKER,
                        new Vec3(0.0D, deltaY, 0.0D));
                entity.fallDistance = 0.0F;
                if (standing) entity.setOnGround(true);
            }
            resolveSweptFloorCollision(entity, previous);
            resolveSweptHorizontalCollision(entity, previous);
            resolveShellCollision(entity);
            playCabinFootstep(entity, previous);
            previousEntityPositions.put(entity.getId(), entity.position());
        }
        previousEntityPositions.keySet().removeIf(id -> !present.contains(id));
        cabinStepDistance.keySet().removeIf(id -> !present.contains(id));
    }

    private void resolveSweptFloorCollision(Entity entity, Vec3 previous) {
        AABB floor = shellBoxes().get(0);
        AABB box = entity.getBoundingBox();
        boolean horizontalOverlap = box.maxX > floor.minX
                && box.minX < floor.maxX && box.maxZ > floor.minZ
                && box.minZ < floor.maxZ;
        if (!horizontalOverlap || previous.y < floor.maxY - 0.12D
                || box.minY >= floor.maxY) return;
        entity.move(MoverType.SHULKER, new Vec3(0.0D,
                floor.maxY - box.minY + COLLISION_EPSILON, 0.0D));
        entity.setOnGround(true);
        entity.fallDistance = 0.0F;
    }

    private void resolveSweptHorizontalCollision(Entity entity, Vec3 previous) {
        List<AABB> shells = shellBoxes();
        AABB entityBox = entity.getBoundingBox();
        double halfWidthX = entityBox.getXsize() * 0.5D;
        double halfWidthZ = entityBox.getZsize() * 0.5D;
        double halfHeight = entityBox.getYsize() * 0.5D;
        Vec3 current = entityBox.getCenter();
        Vec3 start = new Vec3(previous.x, current.y, previous.z);
        for (int index = 2; index < shells.size(); index++) {
            AABB expanded = shells.get(index).inflate(halfWidthX,
                    halfHeight, halfWidthZ);
            if (expanded.contains(start)) continue;
            Optional<Vec3> intersection = expanded.clip(start, current);
            if (intersection.isEmpty()) continue;
            Vec3 travel = current.subtract(start);
            if (travel.horizontalDistanceSqr() <= 1.0E-9D) continue;
            Vec3 safe = intersection.get().subtract(travel.normalize()
                    .scale(COLLISION_EPSILON * 4.0D));
            entity.move(MoverType.SHULKER, new Vec3(
                    safe.x - current.x, 0.0D, safe.z - current.z));
            current = entity.getBoundingBox().getCenter();
        }
    }

    private void playCabinFootstep(Entity entity, Vec3 previous) {
        if (!(entity instanceof Player) || !isStandingOnFloor(entity, getY())) {
            cabinStepDistance.remove(entity.getId());
            return;
        }
        double dx = entity.getX() - previous.x;
        double dz = entity.getZ() - previous.z;
        double travelled = Math.sqrt(dx * dx + dz * dz);
        if (travelled <= 1.0E-4D || travelled > 1.25D) return;
        double accumulated = cabinStepDistance.getOrDefault(entity.getId(), 0.0D)
                + travelled;
        if (accumulated >= 0.58D && level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, entity.blockPosition(),
                    SoundType.STONE.getStepSound(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.32F,
                    0.96F + serverLevel.getRandom().nextFloat() * 0.08F);
            accumulated %= 0.58D;
        }
        cabinStepDistance.put(entity.getId(), accumulated);
    }

''', 'carriage collision solver')

replace_once('''        for (AABB box : local) {
            AABB authored = CoreRoomElevatorGeometry.rotateAabb(box,
                    Direction.EAST, 0.0D, 0.0D);
            AABB rotated = CoreRoomElevatorGeometry.rotateAabb(authored,
                    facing(), 0.0D, 0.0D);
            world.add(rotated.move(getX(), getY(), getZ()));
        }
''', '''        for (AABB box : local) {
            AABB rotated = CoreRoomElevatorGeometry.rotateAabb(box,
                    facing(), 0.0D, 0.0D);
            world.add(rotated.move(getX(), getY(), getZ()));
        }
''', 'shell orientation')

replace_once('''        // The authored cabin root carries a -90 degree Y rotation.
        Vec3 rootRotated = new Vec3(-modelZ, modelY, modelX);
        Vec3 facingRotated = CoreRoomElevatorGeometry.rotateLocalVector(
                facing(), rootRotated.x, rootRotated.y, rootRotated.z);
        return position().add(facingRotated);
''', '''        Vec3 facingRotated = CoreRoomElevatorGeometry.rotateLocalVector(
                facing(), modelX, modelY, modelZ);
        return position().add(facingRotated);
''', 'button anchor orientation')

replace_once('''        double modelX = (front ? -7.0D : 7.0D) / 16.0D;
        double modelY = 53.0D / 16.0D;
        Vec3 rootRotated = new Vec3(0.0D, modelY, modelX);
        return getPosition(partialTick).add(
                CoreRoomElevatorGeometry.rotateLocalVector(facing(),
                        rootRotated.x, rootRotated.y, rootRotated.z));
''', '''        double modelY = 53.0D / 16.0D;
        double modelZ = (front ? -7.0D : 7.0D) / 16.0D;
        return getPosition(partialTick).add(
                CoreRoomElevatorGeometry.rotateLocalVector(facing(),
                        0.0D, modelY, modelZ));
''', 'cable attachment orientation')

path.write_text(text, encoding='utf-8')
