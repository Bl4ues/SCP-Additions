from pathlib import Path
import json
import re

ROOT = Path('.')


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding='utf-8')
    if text.count(old) != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {text.count(old)}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')

module = ROOT / 'src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorModule.java'
manager = ROOT / 'src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorManager.java'
carriage = ROOT / 'src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorCarriageEntity.java'
lang_path = ROOT / 'src/main/resources/assets/scp_additions/lang/en_us.json'

replace_once(
    module,
    '''    public static final RegistryObject<Item> FLOOR_ITEM = ITEMS.register(
            "core_room_floor", () -> new ElevatorBlockItem(
                    FLOOR.get(), "tooltip.scp_additions.core_room_floor"));
''',
    '''    public static final RegistryObject<Item> FLOOR_ITEM = ITEMS.register(
            "core_room_floor", () -> new CoreRoomBlockItem(FLOOR.get()));
''',
    'core room floor item registration',
)

replace_once(
    module,
    '''    public static class ElevatorBlockItem extends BlockItem {
        private final String tooltipKey;

        protected ElevatorBlockItem(Block block, String tooltipKey) {
            super(block, new Item.Properties());
            this.tooltipKey = tooltipKey;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(
                    "tooltip.scp_additions.core_room")
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable(tooltipKey)
                    .withStyle(ChatFormatting.GRAY));
            super.appendHoverText(stack, level, tooltip, flag);
        }
    }
''',
    '''    public static class CoreRoomBlockItem extends BlockItem {
        protected CoreRoomBlockItem(Block block) {
            super(block, new Item.Properties());
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(
                    "tooltip.scp_additions.core_room")
                    .withStyle(ChatFormatting.BLUE));
            super.appendHoverText(stack, level, tooltip, flag);
        }
    }

    public static class ElevatorBlockItem extends CoreRoomBlockItem {
        private final String tooltipKey;

        protected ElevatorBlockItem(Block block, String tooltipKey) {
            super(block);
            this.tooltipKey = tooltipKey;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltip, flag);
            tooltip.add(Component.translatable(tooltipKey)
                    .withStyle(ChatFormatting.GRAY));
        }
    }
''',
    'core room tooltip item classes',
)

replace_once(
    manager,
    '''        int directionalDestination = stationIndex.getAsInt()
                + direction.step();
        if (directionalDestination < 0
                || directionalDestination >= layout.stations().size()) {
            player.sendSystemMessage(Component.translatable(
                    direction == ElevatorFoundation.TravelDirection.UP
                            ? "message.scp_additions.elevator_no_floor_above"
                            : "message.scp_additions.elevator_no_floor_below")
                    .withStyle(ChatFormatting.YELLOW));
            return false;
        }
        return carriage.requestFromStation(stationIndex.getAsInt(), direction,
                player);
''',
    '''        int requestedFloor = stationIndex.getAsInt();
        if (carriage.isAtFloorHeight(stationPos.getY())) {
            return false;
        }
        return carriage.requestFromStation(requestedFloor, direction, player);
''',
    'same-floor station request handling',
)

replace_once(
    carriage,
    '''    private double previousServerY;
    private final Map<Integer, Vec3> previousEntityPositions = new HashMap<>();
''',
    '''    private double previousServerY;
    private double previousClientY = Double.NaN;
    private final Map<Integer, Vec3> previousEntityPositions = new HashMap<>();
''',
    'client carriage position tracking field',
)

replace_once(
    carriage,
    '''        if (level().isClientSide) {
            previousServerY = getY();
            resolveNearbyEntities(0.0D);
            return;
        }
''',
    '''        if (level().isClientSide) {
            double currentY = getY();
            double oldY = Double.isNaN(previousClientY)
                    ? currentY : previousClientY;
            double clientDeltaY = currentY - oldY;
            previousClientY = currentY;
            previousServerY = oldY;
            if (Math.abs(clientDeltaY) > 1.0D) {
                clientDeltaY = 0.0D;
            }
            resolveNearbyEntities(clientDeltaY);
            return;
        }
''',
    'client-side carriage transport',
)

replace_once(
    carriage,
    '''    private static double soundSyncedProgress(double time) {
        double clamped = Mth.clamp(time, 0.0D, 1.0D);
        double forward = clamped * clamped;
        double remaining = 1.0D - clamped;
        double denominator = forward + 0.30D * remaining * remaining;
        return denominator <= 1.0E-9D ? 1.0D : forward / denominator;
    }
''',
    '''    private static double soundSyncedProgress(double time) {
        double clamped = Mth.clamp(time, 0.0D, 1.0D);
        return clamped * clamped * clamped
                * (clamped * (clamped * 6.0D - 15.0D) + 10.0D);
    }
''',
    'smooth carriage travel curve',
)

replace_once(
    carriage,
    '''        List<AABB> world = new ArrayList<>();
        for (AABB box : local) {
            AABB facingAligned = CoreRoomElevatorGeometry.rotateAabb(
                    box, facing().getOpposite(), 0.0D, 0.0D);
            world.add(facingAligned.move(getX(), getY(), getZ()));
        }
''',
    '''        List<AABB> world = new ArrayList<>();
        for (AABB box : local) {
            AABB modelAligned = CoreRoomElevatorGeometry.rotateAabb(box,
                    Direction.EAST, 0.0D, 0.0D);
            AABB facingAligned = CoreRoomElevatorGeometry.rotateAabb(
                    modelAligned, facing(), 0.0D, 0.0D);
            world.add(facingAligned.move(getX(), getY(), getZ()));
        }
''',
    'carriage shell orientation',
)

replace_once(
    carriage,
    '''        double modelX = -10.95508D / 16.0D;
        double modelY = (up ? 21.25D : 19.25D) / 16.0D;
        double modelZ = 11.00251D / 16.0D;
        Vec3 facingRotated = CoreRoomElevatorGeometry.rotateLocalVector(
                facing().getOpposite(), modelX, modelY, modelZ);
        return position().add(facingRotated);
''',
    '''        double modelX = -10.95508D / 16.0D;
        double modelY = (up ? 21.25D : 19.25D) / 16.0D;
        double modelZ = 11.00251D / 16.0D;
        Vec3 modelAligned = CoreRoomElevatorGeometry.rotateLocalVector(
                Direction.EAST, modelX, modelY, modelZ);
        Vec3 facingRotated = CoreRoomElevatorGeometry.rotateLocalVector(
                facing(), modelAligned.x, modelAligned.y, modelAligned.z);
        return position().add(facingRotated);
''',
    'carriage button anchor orientation',
)

lang = json.loads(lang_path.read_text(encoding='utf-8'))
station_key = 'tooltip.scp_additions.core_room_elevator_station'
if station_key not in lang:
    raise SystemExit(f'missing language key: {station_key}')
original_station_tooltip = lang[station_key]
updated_station_tooltip = re.sub(
    r',?\s*and landing collision', '', original_station_tooltip,
    flags=re.IGNORECASE,
)
updated_station_tooltip = re.sub(r'\s+([.,])', r'\1', updated_station_tooltip)
if updated_station_tooltip == original_station_tooltip:
    raise SystemExit(
        f'floor station tooltip did not contain expected phrase: {original_station_tooltip!r}'
    )
lang[station_key] = updated_station_tooltip
lang_path.write_text(
    json.dumps(lang, ensure_ascii=False, separators=(',', ':')) + '\n',
    encoding='utf-8',
)

print('Applied elevator smooth transport, access orientation, station behavior, and Core Room tooltip fixes.')
