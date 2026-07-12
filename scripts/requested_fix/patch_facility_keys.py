from patch_common import read, write, replace_once, remove_once
import re

def patch_facility_buttons():
    replace_once(
        "src/main/java/net/mcreator/scpadditions/facility/LeftDoorButtons.java",
"""            return switch (blockState.getValue(FACING)) {
                case NORTH -> Block.box(14.80D, -0.70D, -1.80D, 20.20D, 5.00D, 0.10D);
                case EAST -> Block.box(15.90D, -0.70D, 14.80D, 17.80D, 5.00D, 20.20D);
                case SOUTH -> Block.box(-4.20D, -0.70D, 15.90D, 1.20D, 5.00D, 17.80D);
                case WEST -> Block.box(-1.80D, -0.70D, -4.20D, 0.10D, 5.00D, 1.20D);
                default -> Shapes.empty();
            };""",
"""            return switch (blockState.getValue(FACING)) {
                case NORTH -> Block.box(16.9D, -2.66D, 14.2D, 20.2D, 2.64D, 16.0D);
                case EAST -> Block.box(0.0D, -2.66D, 16.9D, 1.8D, 2.64D, 20.2D);
                case SOUTH -> Block.box(-4.2D, -2.66D, 0.0D, -0.9D, 2.64D, 1.8D);
                case WEST -> Block.box(14.2D, -2.66D, -4.2D, 16.0D, 2.64D, -0.9D);
                default -> Shapes.empty();
            };""")
    replace_once(
        "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
"""            Direction facing = state.getValue(FACING);
            BlockPos pairPos = pos.relative(facing.getOpposite(), 2);
            BlockState pairState = level.getBlockState(pairPos);
            if (!isDoorButton(pairState.getBlock()) && pairState.canBeReplaced()) {
                level.setBlock(pairPos, buttonFor(buttonState).get().defaultBlockState()
                        .setValue(FACING, facing.getOpposite()), Block.UPDATE_ALL);
            }
            if (buttonState == ButtonState.OPENING || buttonState == ButtonState.CLOSING) {""",
"""            if (buttonState == ButtonState.OPENING || buttonState == ButtonState.CLOSING) {""")
    replace_once(
        "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
"""            if (!level.isClientSide && level instanceof ServerLevel server) {
                setButtonPair(server, pos,
                        buttonState == ButtonState.CLOSED
                                ? ButtonState.OPENING : ButtonState.CLOSING);
            }""",
"""            if (!level.isClientSide && level instanceof ServerLevel server) {
                DoorButtonIndependentInteractionEvents.activateButton(server, pos);
            }""")
    replace_once(
        "src/main/java/net/mcreator/scpadditions/facility/DoorButtonIndependentInteractionEvents.java",
"""        BlockPos legacyCounterpartPos = pos.relative(facing.getOpposite(), 2);
        BlockState counterpartBefore = level.getBlockState(legacyCounterpartPos);

        Block target = blockFor(targetPhase, leftGeometry);
        level.setBlock(pos, target.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing), Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, target);

        // Base right-side Unity states still contain the old auto-pair hook.
        BlockState counterpartAfter = level.getBlockState(legacyCounterpartPos);
        if (isAnyButton(counterpartBefore.getBlock())) {
            if (!counterpartAfter.equals(counterpartBefore)) {
                level.setBlock(legacyCounterpartPos, counterpartBefore, Block.UPDATE_ALL);
            }
        } else if (isAnyButton(counterpartAfter.getBlock())) {
            level.removeBlock(legacyCounterpartPos, false);
        }""",
"""        Block target = blockFor(targetPhase, leftGeometry);
        level.setBlock(pos, target.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing), Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, target);""")

def patch_keys():
    replace_once(
        "src/main/java/com/bl4ues/scpinventory/item/ScpPickupRouter.java",
"""    private static int acceptKey(IScpInventory inventory, ServerPlayer player, ItemStack stack) {
        if (player == null) {
            return 0;
        }

        int acceptedLimit = Math.min(stack.getCount(), inventory.getFreeKeySlots());
        acceptedLimit = Math.min(acceptedLimit, ScpKeyringMirror.getFreeMirrorSlots(player));

        int accepted = 0;
        for (int i = 0; i < acceptedLimit; i++) {
            ItemStack singleKey = stack.copy();
            singleKey.setCount(1);
            stripNoMergeMarker(singleKey);

            if (!ScpKeyringMirror.addMirroredKey(player, singleKey)) {
                break;
            }

            if (!inventory.addKeyItem(singleKey)) {
                ScpKeyringMirror.removeMirroredKey(player, singleKey);
                break;
            }

            accepted++;
        }

        return accepted;
    }""",
"""    private static int acceptKey(IScpInventory inventory, ServerPlayer player, ItemStack stack) {
        int acceptedLimit = Math.min(stack.getCount(), inventory.getFreeKeySlots());
        int accepted = 0;
        for (int i = 0; i < acceptedLimit; i++) {
            ItemStack singleKey = stack.copy();
            singleKey.setCount(1);
            stripNoMergeMarker(singleKey);
            if (!inventory.addKeyItem(singleKey)) {
                break;
            }
            accepted++;
        }
        return accepted;
    }""")
    mirror = "src/main/java/com/bl4ues/scpinventory/events/VanillaMirrorSyncHandler.java"
    remove_once(mirror, "            changed |= syncKeys(player, inventory);\n")
    replace_once(
        mirror,
"""            ScpItemType type = ScpItemClassifier.getType(stack);
            if (type == ScpItemType.KEY) {
                continue;
            }

            ScpEquipmentSlot preservedSlot = getPreservedMirrorSlot(type);""",
"""            ScpItemType type = ScpItemClassifier.getType(stack);
            ScpEquipmentSlot preservedSlot = getPreservedMirrorSlot(type);""")
    text = read(mirror)
    pattern = re.compile(
        r"\n    private static boolean syncKeys\(ServerPlayer player, IScpInventory inventory\) \{.*?"
        r"\n    private static void showInventoryFullThrottled", re.S)
    text, count = pattern.subn("\n    private static void showInventoryFullThrottled", text)
    if count != 1:
        raise RuntimeError(f"Expected to remove key sync block once, got {count}")
    text = text.replace("import java.util.ArrayList;\n", "")
    text = text.replace("import java.util.List;\n", "")
    write(mirror, text)

    key_packet = "src/main/java/com/bl4ues/scpinventory/network/KeyActionPacket.java"
    remove_once(key_packet, "import com.bl4ues.scpinventory.item.ScpKeyringMirror;\n")
    remove_once(key_packet, "                        ScpKeyringMirror.removeMirroredKey(player, key);\n")

    commands = "src/main/java/com/bl4ues/scpinventory/commands/ScpInventoryCommands.java"
    remove_once(commands, "import com.bl4ues.scpinventory.item.ScpKeyringMirror;\n")
    remove_once(commands, "            ScpKeyringMirror.removeMirroredKeys(player, inventory.getKeys());\n")

if __name__ == "__main__":
    patch_facility_buttons()
    patch_keys()
    print("Facility buttons and key ownership fixed")
