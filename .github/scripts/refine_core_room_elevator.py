from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise RuntimeError(f"Unable to locate {label}")
    return text.replace(old, new, 1)


def write(path: Path, content: str) -> None:
    path.write_text(content, encoding="utf-8")


# Carriage: saved movement recovery, collision timing, precise physical buttons,
# and interpolated cable attachment points.
carriage_path = JAVA / "net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorCarriageEntity.java"
carriage = carriage_path.read_text(encoding="utf-8")
if "BUTTON_HIT_RADIUS_SQR" not in carriage:
    carriage = replace_once(carriage,
        "    private static final double FLOOR_EPSILON = 0.035D;\n",
        "    private static final double FLOOR_EPSILON = 0.035D;\n"
        "    private static final int DOOR_COLLISION_THRESHOLD = DOOR_TICKS / 2;\n"
        "    private static final double BUTTON_HIT_RADIUS_SQR = 0.32D * 0.32D;\n",
        "carriage constants")
    carriage = replace_once(carriage,
        "            case MOVING -> tickMotion();",
        "            case MOVING -> {\n"
        "                if (motionPlan == null && !resumeMotionAfterLoad()) break;\n"
        "                tickMotion();\n"
        "            }",
        "moving recovery dispatch")
    carriage = replace_once(carriage,
        "    private void tickMotion() {\n"
        "        if (motionPlan == null) {\n"
        "            setPhase(ElevatorFoundation.Phase.FAULT);\n"
        "            return;\n"
        "        }\n",
        "    private boolean resumeMotionAfterLoad() {\n"
        "        int target = targetFloorIndex();\n"
        "        if (floorHeights.length == 0 || target < 0\n"
        "                || target >= floorHeights.length) {\n"
        "            recoverAtNearestFloor();\n"
        "            return false;\n"
        "        }\n"
        "        motionPlan = ElevatorFoundation.MotionPlan.create(getY(),\n"
        "                floorHeights[target], MAX_SPEED, ACCELERATION);\n"
        "        phaseTicks = 0;\n"
        "        return true;\n"
        "    }\n\n"
        "    private void recoverAtNearestFloor() {\n"
        "        if (floorHeights.length == 0) {\n"
        "            setPhase(ElevatorFoundation.Phase.FAULT);\n"
        "            return;\n"
        "        }\n"
        "        int nearest = nearestFloorIndex(getY());\n"
        "        setPos(getX(), floorHeights[nearest], getZ());\n"
        "        entityData.set(CURRENT_FLOOR, nearest);\n"
        "        entityData.set(TARGET_FLOOR, -1);\n"
        "        queuedTarget = -1;\n"
        "        motionPlan = null;\n"
        "        setPhase(ElevatorFoundation.Phase.IDLE_OPEN);\n"
        "    }\n\n"
        "    private void tickMotion() {\n"
        "        if (motionPlan == null) {\n"
        "            recoverAtNearestFloor();\n"
        "            return;\n"
        "        }\n",
        "movement recovery methods")
    carriage = carriage.replace(
        "        if (phase() != ElevatorFoundation.Phase.IDLE_OPEN\n"
        "                && phase() != ElevatorFoundation.Phase.DOOR_OPENING) {\n",
        "        if (isDoorCollisionSolid()) {\n", 1)
    marker = "        return world;\n    }\n\n    private AABB cabinOuterBox()"
    carriage = replace_once(carriage, marker,
        "        return world;\n"
        "    }\n\n"
        "    public boolean isDoorCollisionSolid() {\n"
        "        return switch (phase()) {\n"
        "            case IDLE_OPEN -> false;\n"
        "            case DOOR_OPENING -> phaseTicks < DOOR_COLLISION_THRESHOLD;\n"
        "            case DOOR_CLOSING -> phaseTicks >= DOOR_COLLISION_THRESHOLD;\n"
        "            default -> true;\n"
        "        };\n"
        "    }\n\n"
        "    private AABB cabinOuterBox()",
        "door collision method")
    carriage = replace_once(carriage,
        "    public Vec3 cableAttachment(boolean front) {\n"
        "        double modelX = (front ? -7.0D : 7.0D) / 16.0D;\n"
        "        double modelY = 53.0D / 16.0D;\n"
        "        Vec3 rootRotated = new Vec3(0.0D, modelY, modelX);\n"
        "        return position().add(CoreRoomElevatorGeometry.rotateLocalVector(\n"
        "                facing(), rootRotated.x, rootRotated.y, rootRotated.z));\n"
        "    }\n",
        "    public Vec3 cableAttachment(boolean front) {\n"
        "        return cableAttachment(front, 1.0F);\n"
        "    }\n\n"
        "    public Vec3 cableAttachment(boolean front, float partialTick) {\n"
        "        double modelX = (front ? -7.0D : 7.0D) / 16.0D;\n"
        "        double modelY = 53.0D / 16.0D;\n"
        "        Vec3 rootRotated = new Vec3(0.0D, modelY, modelX);\n"
        "        return getPosition(partialTick).add(\n"
        "                CoreRoomElevatorGeometry.rotateLocalVector(facing(),\n"
        "                        rootRotated.x, rootRotated.y, rootRotated.z));\n"
        "    }\n",
        "interpolated cable attachment")
    carriage = replace_once(carriage,
        "    @Override\n"
        "    public InteractionResult interactAt(Player player, Vec3 hit,\n"
        "            InteractionHand hand) {\n"
        "        if (level().isClientSide) return InteractionResult.SUCCESS;\n"
        "        if (!(player instanceof ServerPlayer serverPlayer)) {\n"
        "            return InteractionResult.PASS;\n"
        "        }\n"
        "        boolean up = hit.y >= 1.25D;\n"
        "        return handleContextInteraction(serverPlayer,\n"
        "                up ? \"elevator_carriage_up\" : \"elevator_carriage_down\")\n"
        "                ? InteractionResult.CONSUME : InteractionResult.FAIL;\n"
        "    }\n",
        "    @Override\n"
        "    public InteractionResult interactAt(Player player, Vec3 hit,\n"
        "            InteractionHand hand) {\n"
        "        Vec3 upAnchor = contextAnchor(true).subtract(position());\n"
        "        Vec3 downAnchor = contextAnchor(false).subtract(position());\n"
        "        double upDistance = hit.distanceToSqr(upAnchor);\n"
        "        double downDistance = hit.distanceToSqr(downAnchor);\n"
        "        if (Math.min(upDistance, downDistance) > BUTTON_HIT_RADIUS_SQR) {\n"
        "            return InteractionResult.PASS;\n"
        "        }\n"
        "        if (level().isClientSide) return InteractionResult.SUCCESS;\n"
        "        if (!(player instanceof ServerPlayer serverPlayer)) {\n"
        "            return InteractionResult.PASS;\n"
        "        }\n"
        "        boolean up = upDistance <= downDistance;\n"
        "        return handleContextInteraction(serverPlayer,\n"
        "                up ? \"elevator_carriage_up\" : \"elevator_carriage_down\")\n"
        "                ? InteractionResult.CONSUME : InteractionResult.FAIL;\n"
        "    }\n",
        "carriage physical button hitboxes")
    carriage = carriage.replace(
        "        queuedTarget = tag.getInt(\"QueuedTarget\");\n"
        "        phaseTicks = tag.getInt(\"PhaseTicks\");\n",
        "        queuedTarget = tag.contains(\"QueuedTarget\")\n"
        "                ? tag.getInt(\"QueuedTarget\") : -1;\n"
        "        phaseTicks = tag.getInt(\"PhaseTicks\");\n"
        "        motionPlan = null;\n", 1)
write(carriage_path, carriage)

# Station gate collision follows the middle of its authored animation.
geometry_path = JAVA / "net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorGeometry.java"
geometry = geometry_path.read_text(encoding="utf-8")
if "boolean gateSolid" not in geometry:
    geometry = replace_once(geometry,
        "    public static VoxelShape stationCellShape(Direction facing, int localX,\n"
        "            int localY, int localZ,\n"
        "            CoreRoomElevatorModule.DoorVisualState doorState) {\n"
        "        List<AABB> boxes = new ArrayList<>(STATION_STATIC);\n"
        "        if (doorState == CoreRoomElevatorModule.DoorVisualState.CLOSED\n"
        "                || doorState == CoreRoomElevatorModule.DoorVisualState.CLOSING) {\n"
        "            boxes.add(STATION_GATE);\n"
        "        }\n",
        "    public static VoxelShape stationCellShape(Direction facing, int localX,\n"
        "            int localY, int localZ, boolean gateSolid) {\n"
        "        List<AABB> boxes = new ArrayList<>(STATION_STATIC);\n"
        "        if (gateSolid) {\n"
        "            boxes.add(STATION_GATE);\n"
        "        }\n",
        "station collision signature")
write(geometry_path, geometry)

# Station physical buttons and synchronized local door clock.
module_path = JAVA / "net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorModule.java"
module = module_path.read_text(encoding="utf-8")
if "Vec3 upButton =" not in module:
    module = replace_once(module,
        "            if (level.isClientSide) return InteractionResult.SUCCESS;\n"
        "            if (!(player instanceof ServerPlayer serverPlayer)\n"
        "                    || !(level instanceof ServerLevel serverLevel)) {\n"
        "                return InteractionResult.PASS;\n"
        "            }\n"
        "            Vec3 local = CoreRoomElevatorGeometry.worldToModelLocal(\n"
        "                    pos, state.getValue(FACING), hit.getLocation());\n"
        "            ElevatorFoundation.TravelDirection direction = local.y >= 1.25D\n"
        "                    ? ElevatorFoundation.TravelDirection.UP\n"
        "                    : ElevatorFoundation.TravelDirection.DOWN;\n"
        "            return handleContextInteraction(serverLevel, pos,\n"
        "                    serverPlayer,\n"
        "                    direction == ElevatorFoundation.TravelDirection.UP\n"
        "                            ? \"elevator_station_up\" : \"elevator_station_down\");\n",
        "            Vec3 local = CoreRoomElevatorGeometry.worldToModelLocal(\n"
        "                    pos, state.getValue(FACING), hit.getLocation());\n"
        "            Vec3 upButton = new Vec3(14.64492D / 16.0D,\n"
        "                    21.25D / 16.0D, -16.69749D / 16.0D);\n"
        "            Vec3 downButton = new Vec3(14.64492D / 16.0D,\n"
        "                    19.25D / 16.0D, -16.69749D / 16.0D);\n"
        "            double upDistance = local.distanceToSqr(upButton);\n"
        "            double downDistance = local.distanceToSqr(downButton);\n"
        "            if (Math.min(upDistance, downDistance) > 0.32D * 0.32D) {\n"
        "                return InteractionResult.PASS;\n"
        "            }\n"
        "            if (level.isClientSide) return InteractionResult.SUCCESS;\n"
        "            if (!(player instanceof ServerPlayer serverPlayer)\n"
        "                    || !(level instanceof ServerLevel serverLevel)) {\n"
        "                return InteractionResult.PASS;\n"
        "            }\n"
        "            ElevatorFoundation.TravelDirection direction =\n"
        "                    upDistance <= downDistance\n"
        "                    ? ElevatorFoundation.TravelDirection.UP\n"
        "                    : ElevatorFoundation.TravelDirection.DOWN;\n"
        "            return handleContextInteraction(serverLevel, pos, serverPlayer,\n"
        "                    direction == ElevatorFoundation.TravelDirection.UP\n"
        "                            ? \"elevator_station_up\" : \"elevator_station_down\");\n",
        "station physical button hitboxes")
    module = module.replace(
        "            DoorVisualState door = level.getBlockEntity(pos)\n"
        "                    instanceof StationBlockEntity station\n"
        "                    ? station.doorState() : DoorVisualState.CLOSED;\n"
        "            return CoreRoomElevatorGeometry.stationCellShape(\n"
        "                    state.getValue(FACING), 0, 0, 0, door);\n",
        "            boolean gateSolid = !(level.getBlockEntity(pos)\n"
        "                    instanceof StationBlockEntity station)\n"
        "                    || station.isGateCollisionSolid();\n"
        "            return CoreRoomElevatorGeometry.stationCellShape(\n"
        "                    state.getValue(FACING), 0, 0, 0, gateSolid);\n", 1)
    module = module.replace(
        "            DoorVisualState door = level.getBlockEntity(part.masterPos())\n"
        "                    instanceof StationBlockEntity station\n"
        "                    ? station.doorState() : DoorVisualState.CLOSED;\n"
        "            return CoreRoomElevatorGeometry.stationCellShape(facing,\n"
        "                    part.localX(), part.localY(), part.localZ(), door);\n",
        "            boolean gateSolid = !(level.getBlockEntity(part.masterPos())\n"
        "                    instanceof StationBlockEntity station)\n"
        "                    || station.isGateCollisionSolid();\n"
        "            return CoreRoomElevatorGeometry.stationCellShape(facing,\n"
        "                    part.localX(), part.localY(), part.localZ(), gateSolid);\n", 1)
    module = module.replace(
        "        private final AnimatableInstanceCache cache =\n"
        "                GeckoLibUtil.createInstanceCache(this);\n"
        "        private DoorVisualState doorState = DoorVisualState.CLOSED;\n"
        "        private boolean initialized;\n",
        "        private static final int DOOR_TICKS = 15;\n"
        "        private static final int COLLISION_THRESHOLD = DOOR_TICKS / 2;\n\n"
        "        private final AnimatableInstanceCache cache =\n"
        "                GeckoLibUtil.createInstanceCache(this);\n"
        "        private DoorVisualState doorState = DoorVisualState.CLOSED;\n"
        "        private int doorTicks = DOOR_TICKS;\n"
        "        private boolean initialized;\n", 1)
    module = module.replace(
        "        public static void tick(Level level, BlockPos pos, BlockState state,\n"
        "                StationBlockEntity blockEntity) {\n"
        "            if (!(level instanceof ServerLevel serverLevel)) return;\n"
        "            if (!blockEntity.initialized) {\n"
        "                blockEntity.initialized = true;\n"
        "                CoreRoomElevatorManager.rebuildColumn(serverLevel, pos, null);\n"
        "            }\n"
        "            DoorVisualState next = CoreRoomElevatorManager\n"
        "                    .visualStateForStation(serverLevel, pos);\n"
        "            blockEntity.setDoorState(next);\n"
        "        }\n",
        "        public static void tick(Level level, BlockPos pos, BlockState state,\n"
        "                StationBlockEntity blockEntity) {\n"
        "            if (level.isClientSide) {\n"
        "                blockEntity.advanceDoorClock();\n"
        "                return;\n"
        "            }\n"
        "            if (!(level instanceof ServerLevel serverLevel)) return;\n"
        "            if (!blockEntity.initialized) {\n"
        "                blockEntity.initialized = true;\n"
        "                CoreRoomElevatorManager.rebuildColumn(serverLevel, pos, null);\n"
        "            }\n"
        "            DoorVisualState next = CoreRoomElevatorManager\n"
        "                    .visualStateForStation(serverLevel, pos);\n"
        "            blockEntity.setDoorState(next);\n"
        "            blockEntity.advanceDoorClock();\n"
        "        }\n", 1)
    module = module.replace(
        "        public DoorVisualState doorState() {\n"
        "            return doorState;\n"
        "        }\n\n"
        "        private void setDoorState(DoorVisualState state) {\n"
        "            if (state == doorState) return;\n"
        "            doorState = state;\n"
        "            setChanged();\n",
        "        public DoorVisualState doorState() {\n"
        "            return doorState;\n"
        "        }\n\n"
        "        public boolean isGateCollisionSolid() {\n"
        "            return switch (doorState) {\n"
        "                case OPEN -> false;\n"
        "                case OPENING -> doorTicks < COLLISION_THRESHOLD;\n"
        "                case CLOSING -> doorTicks >= COLLISION_THRESHOLD;\n"
        "                default -> true;\n"
        "            };\n"
        "        }\n\n"
        "        private void advanceDoorClock() {\n"
        "            if ((doorState == DoorVisualState.OPENING\n"
        "                    || doorState == DoorVisualState.CLOSING)\n"
        "                    && doorTicks < DOOR_TICKS) {\n"
        "                doorTicks++;\n"
        "            }\n"
        "        }\n\n"
        "        private void setDoorState(DoorVisualState state) {\n"
        "            if (state == doorState) return;\n"
        "            doorState = state;\n"
        "            doorTicks = 0;\n"
        "            setChanged();\n", 1)
    module = module.replace(
        "            tag.putByte(\"DoorState\", (byte) doorState.ordinal());\n",
        "            tag.putByte(\"DoorState\", (byte) doorState.ordinal());\n"
        "            tag.putInt(\"DoorTicks\", doorTicks);\n", 1)
    module = module.replace(
        "            doorState = value >= 0 && value < DoorVisualState.values().length\n"
        "                    ? DoorVisualState.values()[value] : DoorVisualState.CLOSED;\n",
        "            doorState = value >= 0 && value < DoorVisualState.values().length\n"
        "                    ? DoorVisualState.values()[value] : DoorVisualState.CLOSED;\n"
        "            doorTicks = tag.contains(\"DoorTicks\")\n"
        "                    ? Mth.clamp(tag.getInt(\"DoorTicks\"), 0, DOOR_TICKS)\n"
        "                    : DOOR_TICKS;\n", 1)
write(module_path, module)

client_path = JAVA / "net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorClient.java"
client = client_path.read_text(encoding="utf-8")
client = client.replace("Vec3 attachment = entity.cableAttachment(front);",
                        "Vec3 attachment = entity.cableAttachment(front, partialTick);")
write(client_path, client)
