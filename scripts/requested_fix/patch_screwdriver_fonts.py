from patch_common import ensure_import, replace_once, robotoize

def patch_screwdriver():
    path = "src/main/java/net/mcreator/scpadditions/keycard/KeycardReaderInteractionEvents.java"
    ensure_import(path, "import net.minecraft.core.BlockPos;\n", "package net.mcreator.scpadditions.keycard;\n\n")
    replace_once(
        path,
"""    private KeycardReaderInteractionEvents() {
    }

    @SubscribeEvent""",
"""    private KeycardReaderInteractionEvents() {
    }

    public static boolean tryOpenConfiguration(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null || !player.isShiftKeyDown()) {
            return false;
        }

        boolean hasScrewdriver = player.getMainHandItem().is(UnifiedReaderItems.SCREWDRIVER.get())
                || player.getOffhandItem().is(UnifiedReaderItems.SCREWDRIVER.get());
        if (!hasScrewdriver) {
            return false;
        }

        KeycardReaderLevels.ReaderDescriptor descriptor = KeycardReaderLevels.describe(
                player.level().getBlockState(pos));
        if (descriptor == null) {
            return false;
        }

        ScpEntityNetwork.openKeycardReaderScreen(player, pos, descriptor.level());
        return true;
    }

    @SubscribeEvent""")

    packet = "src/main/java/com/bl4ues/scpinventory/network/ContextInteractPacket.java"
    ensure_import(
        packet,
        "import net.mcreator.scpadditions.keycard.KeycardReaderInteractionEvents;\n",
        "import net.mcreator.scpadditions.entity.AbstractScp131Entity;\n")
    replace_once(
        packet,
"""        boolean doorBefore = isDoorWithOpenState(state);
        boolean wasOpen = doorBefore && state.getValue(BlockStateProperties.OPEN);
        BlockHitResult hit = new BlockHitResult(anchor, rule.resolveClickFace(state, player), pos, false);""",
"""        if (KeycardReaderInteractionEvents.tryOpenConfiguration(player, pos)) {
            player.swing(InteractionHand.MAIN_HAND, true);
            return;
        }

        boolean doorBefore = isDoorWithOpenState(state);
        boolean wasOpen = doorBefore && state.getValue(BlockStateProperties.OPEN);
        BlockHitResult hit = new BlockHitResult(anchor, rule.resolveClickFace(state, player), pos, false);""")

def patch_fonts():
    robotoize("src/main/java/com/bl4ues/scpinventory/client/ContextPromptClient.java", False)
    robotoize("src/main/java/com/bl4ues/scpinventory/client/PickupPromptClient.java", False)
    for path in (
        "src/main/java/com/bl4ues/scpinventory/client/gui/ScpInventoryScreen.java",
        "src/main/java/com/bl4ues/scpinventory/client/gui/components/CodexPanel.java",
        "src/main/java/com/bl4ues/scpinventory/client/gui/components/ContextMenu.java",
        "src/main/java/com/bl4ues/scpinventory/client/gui/components/EquipmentPanel.java",
        "src/main/java/com/bl4ues/scpinventory/client/gui/components/ScrollableItemList.java",
        "src/main/java/com/bl4ues/scpinventory/client/gui/components/StatusPanel.java",
    ):
        robotoize(path, True)

if __name__ == "__main__":
    patch_screwdriver()
    patch_fonts()
    print("Screwdriver context action and Roboto UI fixed")
