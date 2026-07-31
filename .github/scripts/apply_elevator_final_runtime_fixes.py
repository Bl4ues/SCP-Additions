from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def write(relative: str, content: str) -> None:
    (ROOT / relative).write_text(content, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected one {label}, found {count}")
    return text.replace(old, new, 1)


def replace_between(text: str, start_marker: str, end_marker: str,
                    replacement: str, label: str) -> str:
    try:
        start = text.index(start_marker)
        end = text.index(end_marker, start)
    except ValueError as exc:
        raise RuntimeError(f"Could not locate {label}") from exc
    return text[:start] + replacement + text[end:]


# Use the same camera projection convention as the proven pickup prompt.
context_path = "src/main/java/com/bl4ues/scpinventory/client/ContextPromptClient.java"
context = read(context_path)
context = replace_between(
    context,
    "    private static ScreenPoint projectToScreen",
    "    private static void drawIcon",
    '''    private static ScreenPoint projectToScreen(Minecraft minecraft,
            Vec3 worldPos, int screenWidth, int screenHeight) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 relative = worldPos.subtract(camera.getPosition());
        Quaternionf rotation = new Quaternionf(camera.rotation());
        rotation.conjugate();
        Vector3f transformed = new Vector3f((float) relative.x,
                (float) relative.y, (float) relative.z);
        transformed.rotate(rotation);
        double depth = Math.abs(transformed.z());
        if (depth < 0.05D) return null;
        double fov = minecraft.options.fov().get();
        double scale = screenHeight
                / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));
        int x = (int) Math.round(screenWidth / 2.0D
                - transformed.x() * scale / depth);
        int y = (int) Math.round(screenHeight / 2.0D
                - transformed.y() * scale / depth);
        return new ScreenPoint(x, y);
    }

''',
    "context prompt projection",
)
write(context_path, context)


# Fill only the authored front opening with a low, state-controlled railing.
geometry_path = "src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorGeometry.java"
geometry = read(geometry_path)
geometry = replace_once(
    geometry,
    '''    private static final AABB STATION_GATE = modelBox(
            -10, 0, -19.65, 10, 9.5, -19.15);''',
    '''    private static final AABB STATION_GATE = modelBox(
            -12, 0, -16.75, 12, 13.5, -16.25);''',
    "station front gate geometry",
)
write(geometry_path, geometry)


# Align the carriage collision and buttons with the current unrotated model,
# and stop the support code from overriding ordinary player jump physics.
carriage_path = "src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorCarriageEntity.java"
carriage = read(carriage_path)
carriage = replace_between(
    carriage,
    "    private void resolveSweptFloorCollision",
    "    private void playCabinFootstep",
    '''    private void resolveSweptFloorCollision(Entity entity, Vec3 previous) {
        AABB floor = shellBoxes().get(0);
        AABB box = entity.getBoundingBox();
        Vec3 motion = entity.getDeltaMovement();
        boolean horizontalOverlap = box.maxX > floor.minX
                && box.minX < floor.maxX && box.maxZ > floor.minZ
                && box.minZ < floor.maxZ;
        boolean descending = motion.y <= 0.0D;
        boolean crossedFloor = descending
                && previous.y >= floor.maxY - 0.10D
                && box.minY < floor.maxY;
        boolean recoverBelowFloor = descending
                && previous.y >= floor.maxY - 0.35D
                && box.minY < floor.maxY
                && box.minY > floor.minY - 0.45D;
        if (!horizontalOverlap || (!crossedFloor && !recoverBelowFloor)) return;
        placeEntityOnFloor(entity, floor);
    }

    private static void placeEntityOnFloor(Entity entity, AABB floor) {
        Vec3 motion = entity.getDeltaMovement();
        if (motion.y > 0.0D) return;
        AABB box = entity.getBoundingBox();
        entity.move(MoverType.SHULKER, new Vec3(0.0D,
                floor.maxY - box.minY + COLLISION_EPSILON, 0.0D));
        stabilizeGroundedEntity(entity);
    }

    private static void stabilizeGroundedEntity(Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        if (motion.y > 0.0D) return;
        entity.setDeltaMovement(motion.x, 0.0D, motion.z);
        entity.setOnGround(true);
        entity.fallDistance = 0.0F;
    }

''',
    "carriage floor support helpers",
)
carriage = replace_between(
    carriage,
    "    private boolean isStandingOnFloor",
    "    private void resolveShellCollision",
    '''    private boolean isStandingOnFloor(Entity entity, double oldFloorY) {
        AABB box = entity.getBoundingBox();
        double floorTop = oldFloorY + FLOOR_TOP;
        return entity.getDeltaMovement().y <= 0.02D
                && box.maxX > getX() - 0.74D
                && box.minX < getX() + 0.74D
                && box.maxZ > getZ() - 0.74D
                && box.minZ < getZ() + 0.74D
                && box.minY >= floorTop - 0.08D
                && box.minY <= floorTop + 0.12D;
    }

''',
    "standing-on-carriage check",
)
carriage = replace_between(
    carriage,
    "    private void resolveFloorCollision",
    "    private List<AABB> shellBoxes",
    '''    private void resolveFloorCollision(Entity entity, AABB floor) {
        AABB box = entity.getBoundingBox();
        if (entity.getDeltaMovement().y > 0.0D || !box.intersects(floor)) {
            return;
        }
        if (box.getCenter().y >= floor.getCenter().y) {
            placeEntityOnFloor(entity, floor);
        }
    }

    private void resolveCeilingCollision(Entity entity, AABB ceiling) {
        AABB box = entity.getBoundingBox();
        if (!box.intersects(ceiling)
                || box.getCenter().y > ceiling.getCenter().y) return;
        entity.move(MoverType.SHULKER, new Vec3(0.0D,
                ceiling.minY - box.maxY - COLLISION_EPSILON, 0.0D));
        Vec3 motion = entity.getDeltaMovement();
        if (motion.y > 0.0D) {
            entity.setDeltaMovement(motion.x, 0.0D, motion.z);
        }
        entity.setOnGround(false);
    }

    private void resolveHorizontalCollision(Entity entity, AABB shell) {
        AABB box = entity.getBoundingBox();
        if (!box.intersects(shell)) return;
        double west = box.maxX - shell.minX;
        double east = shell.maxX - box.minX;
        double north = box.maxZ - shell.minZ;
        double south = shell.maxZ - box.minZ;
        double smallest = west;
        Vec3 push = new Vec3(-west - COLLISION_EPSILON, 0.0D, 0.0D);
        if (east < smallest) {
            smallest = east;
            push = new Vec3(east + COLLISION_EPSILON, 0.0D, 0.0D);
        }
        if (north < smallest) {
            smallest = north;
            push = new Vec3(0.0D, 0.0D,
                    -north - COLLISION_EPSILON);
        }
        if (south < smallest) {
            push = new Vec3(0.0D, 0.0D,
                    south + COLLISION_EPSILON);
        }
        Vec3 motion = entity.getDeltaMovement();
        entity.move(MoverType.SHULKER, push);
        double motionX = motion.x;
        double motionZ = motion.z;
        if (push.x != 0.0D && motionX * push.x < 0.0D) motionX = 0.0D;
        if (push.z != 0.0D && motionZ * push.z < 0.0D) motionZ = 0.0D;
        entity.setDeltaMovement(motionX, motion.y, motionZ);
    }

''',
    "carriage shell collision handlers",
)
carriage = replace_once(
    carriage,
    '''        for (AABB box : local) {
            AABB modelAligned = CoreRoomElevatorGeometry.rotateAabb(box,
                    Direction.EAST, 0.0D, 0.0D);
            AABB facingAligned = CoreRoomElevatorGeometry.rotateAabb(
                    modelAligned, facing().getOpposite(), 0.0D, 0.0D);
            world.add(facingAligned.move(getX(), getY(), getZ()));
        }''',
    '''        for (AABB box : local) {
            AABB facingAligned = CoreRoomElevatorGeometry.rotateAabb(
                    box, facing().getOpposite(), 0.0D, 0.0D);
            world.add(facingAligned.move(getX(), getY(), getZ()));
        }''',
    "obsolete carriage root collision rotation",
)
carriage = replace_between(
    carriage,
    "    public Vec3 contextAnchor",
    "    public Vec3 cableAttachment",
    '''    public Vec3 contextAnchor(boolean up) {
        double modelX = -10.95508D / 16.0D;
        double modelY = (up ? 21.25D : 19.25D) / 16.0D;
        double modelZ = 11.00251D / 16.0D;
        Vec3 facingRotated = CoreRoomElevatorGeometry.rotateLocalVector(
                facing().getOpposite(), modelX, modelY, modelZ);
        return position().add(facingRotated);
    }

''',
    "carriage context button anchor",
)
if "public static final int TRAVEL_TICKS = 8 * 20;" not in carriage:
    raise RuntimeError("Eight-second elevator travel timing was not preserved")
write(carriage_path, carriage)


# The authored SCP-330 paper faces model +Z. Apply the block FACING value to
# that axis rather than treating model -Z as the front.
scp330_path = "src/main/java/net/mcreator/scpadditions/client/Scp330Client.java"
scp330 = read(scp330_path)
scp330 = replace_once(
    scp330,
    '''            float rotation = switch (animatable.getBlockState().getValue(Scp330Block.FACING)) {
                case SOUTH -> (float) Math.PI;
                case EAST -> (float) (-Math.PI / 2.0D);
                case WEST -> (float) (Math.PI / 2.0D);
                default -> 0.0F;
            };''',
    '''            float rotation = switch (animatable.getBlockState().getValue(Scp330Block.FACING)) {
                case NORTH -> (float) Math.PI;
                case EAST -> (float) (Math.PI / 2.0D);
                case WEST -> (float) (-Math.PI / 2.0D);
                default -> 0.0F;
            };''',
    "SCP-330 model-front rotation",
)
write(scp330_path, scp330)

print("Applied elevator prompt, collision, physics, and SCP-330 orientation fixes.")
