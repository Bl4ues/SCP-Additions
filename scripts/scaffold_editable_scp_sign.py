from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file_path = Path(path)
    text = file_path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected one match in {path}, found {count}: {old[:80]!r}")
    file_path.write_text(text.replace(old, new, 1), encoding="utf-8")


# Replace the decorative controller with the new block-entity-capable controller.
replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    'public static final RegistryObject<Block> SIGN_SUPPORT = registerBlock("sign_support", SignSupportBlock::new, true);',
    'public static final RegistryObject<Block> SIGN_SUPPORT = registerBlock("sign_support", ScpSignSupportBlock::new, true);',
)

replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    '''    public static final RegistryObject<BlockEntityType<FacilitySignBlockEntity>>
            FACILITY_SIGN_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
                    "facility_sign", () -> BlockEntityType.Builder.of(
                            FacilitySignBlockEntity::new,
                            CORE_ROOM_SIGN.get(), DOOR_SIGN.get()).build(null));''',
    '''    public static final RegistryObject<BlockEntityType<FacilitySignBlockEntity>>
            FACILITY_SIGN_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
                    "facility_sign", () -> BlockEntityType.Builder.of(
                            FacilitySignBlockEntity::new,
                            CORE_ROOM_SIGN.get(), DOOR_SIGN.get()).build(null));
    public static final RegistryObject<BlockEntityType<ScpSignSupportBlockEntity>>
            SCP_SIGN_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
                    "scp_sign_support", () -> BlockEntityType.Builder.of(
                            ScpSignSupportBlockEntity::new,
                            SIGN_SUPPORT.get()).build(null));''',
)

# Let clicks on the invisible multiblock cells reach the controller editor.
replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityPropPartBlock.java",
    "import net.minecraft.server.level.ServerLevel;",
    "import net.minecraft.server.level.ServerLevel;\nimport net.minecraft.server.level.ServerPlayer;",
)
replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityPropPartBlock.java",
    "import net.minecraft.world.entity.player.Player;",
    "import net.minecraft.world.InteractionHand;\nimport net.minecraft.world.InteractionResult;\nimport net.minecraft.world.entity.player.Player;",
)
replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityPropPartBlock.java",
    "import net.minecraft.world.phys.HitResult;",
    "import net.minecraft.world.phys.BlockHitResult;\nimport net.minecraft.world.phys.HitResult;",
)
replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityPropPartBlock.java",
    "import net.minecraft.world.phys.shapes.VoxelShape;",
    "import net.minecraft.world.phys.shapes.VoxelShape;\nimport net.mcreator.scpadditions.keycard.KeycardReaderInteractionEvents;\nimport net.mcreator.scpadditions.network.ScpEntityNetwork;",
)
replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityPropPartBlock.java",
    '''    @Override
    public List<ItemStack> getDrops(BlockState state,
            LootParams.Builder builder) {''',
    '''    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        Part part = state.getValue(PART);
        if (part.kind() != FacilityLargePropStructure.Kind.SIGN_SUPPORT
                || KeycardReaderInteractionEvents.screwdriver(player).isEmpty()) {
            return InteractionResult.PASS;
        }
        BlockPos controller = FacilityLargePropStructure.controllerPosition(
                pos, state);
        if (!FacilityLargePropStructure.isValidPart(level, pos, state)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(controller)
                        instanceof ScpSignSupportBlockEntity sign) {
            ScpEntityNetwork.openScpSignScreen(serverPlayer, sign);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state,
            LootParams.Builder builder) {''',
)

# Register the renderer.
replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityClientRenderEvents.java",
    "import net.mcreator.scpadditions.client.FacilitySignBlockEntityRenderer;",
    "import net.mcreator.scpadditions.client.FacilitySignBlockEntityRenderer;\nimport net.mcreator.scpadditions.client.ScpSignSupportBlockEntityRenderer;",
)
replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityClientRenderEvents.java",
    '''        event.registerBlockEntityRenderer(
                FacilityModule.WET_FLOOR_BLOCK_ENTITY.get(),
                WetFloorBlockEntityRenderer::new);''',
    '''        event.registerBlockEntityRenderer(
                FacilityModule.WET_FLOOR_BLOCK_ENTITY.get(),
                WetFloorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                FacilityModule.SCP_SIGN_BLOCK_ENTITY.get(),
                ScpSignSupportBlockEntityRenderer::new);''',
)

# Append network packets so established packet IDs remain stable.
replace_once(
    "src/main/java/net/mcreator/scpadditions/network/ScpEntityNetwork.java",
    "import net.mcreator.scpadditions.facility.FacilitySignBlockEntity;",
    "import net.mcreator.scpadditions.facility.FacilitySignBlockEntity;\nimport net.mcreator.scpadditions.facility.ScpSignSupportBlockEntity;",
)
replace_once(
    "src/main/java/net/mcreator/scpadditions/network/ScpEntityNetwork.java",
    '''        ScpAdditionsMod.addNetworkMessage(FacilitySignClipboardPacket.class,
                FacilitySignClipboardPacket::encode,
                FacilitySignClipboardPacket::decode,
                FacilitySignClipboardPacket::handle);''',
    '''        ScpAdditionsMod.addNetworkMessage(FacilitySignClipboardPacket.class,
                FacilitySignClipboardPacket::encode,
                FacilitySignClipboardPacket::decode,
                FacilitySignClipboardPacket::handle);
        ScpAdditionsMod.addNetworkMessage(ScpSignOpenScreenPacket.class,
                ScpSignOpenScreenPacket::encode,
                ScpSignOpenScreenPacket::decode,
                ScpSignOpenScreenPacket::handle);
        ScpAdditionsMod.addNetworkMessage(ScpSignSavePacket.class,
                ScpSignSavePacket::encode,
                ScpSignSavePacket::decode,
                ScpSignSavePacket::handle);''',
)
replace_once(
    "src/main/java/net/mcreator/scpadditions/network/ScpEntityNetwork.java",
    '''    public static void openFacilitySignScreen(ServerPlayer player,
            FacilitySignBlockEntity sign) {
        if (player == null || sign == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new FacilitySignOpenScreenPacket(
                        sign.getBlockPos(), sign.type(), sign.entries()));
    }
}''',
    '''    public static void openFacilitySignScreen(ServerPlayer player,
            FacilitySignBlockEntity sign) {
        if (player == null || sign == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new FacilitySignOpenScreenPacket(
                        sign.getBlockPos(), sign.type(), sign.entries()));
    }

    public static void openScpSignScreen(ServerPlayer player,
            ScpSignSupportBlockEntity sign) {
        if (player == null || sign == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ScpSignOpenScreenPacket(sign.getBlockPos(), sign.data()));
    }
}''',
)

# Add the font attribution alongside the existing redistributed fonts.
replace_once(
    "LICENSE.md",
    "| Jura Variable 5.106 | Door Sign numbers | Copyright 2019 The Jura Project Authors; designed by Daniel Johnson, Alexei Vanyashin, and Mirko Velimirovic | [SIL Open Font License 1.1](https://github.com/ossobuffo/jura/blob/master/OFL.txt) |",
    "| Jura Variable 5.106 | Door Sign numbers | Copyright 2019 The Jura Project Authors; designed by Daniel Johnson, Alexei Vanyashin, and Mirko Velimirovic | [SIL Open Font License 1.1](https://github.com/ossobuffo/jura/blob/master/OFL.txt) |\n| Kokoro Regular | Editable SCP Sign Support text | Copyright 2016 The Kokoro Project Authors; designed by Yasushi Saikusa / typingart | [SIL Open Font License 1.1](https://github.com/google/fonts/blob/main/ofl/kokoro/OFL.txt) |",
)

# Add editor labels to the active English language file.
replace_once(
    "src/main/resources/assets/scp_additions/lang/en_us_3_0.json",
    '  "block.scp_additions.wet_floor": "Wet Floor Sign"\n}',
    '''  "block.scp_additions.wet_floor": "Wet Floor Sign",
  "tooltip.scp_additions.sign_support_primary": "Editable SCP containment information sign.",
  "tooltip.scp_additions.sign_support_secondary": "Use a Screwdriver",
  "screen.scp_additions.scp_sign_editor": "SCP SIGN EDITOR",
  "screen.scp_additions.scp_sign_number": "SCP Number",
  "screen.scp_additions.scp_sign_containment": "Containment Class",
  "screen.scp_additions.scp_sign_custom_containment": "Custom Containment Class",
  "screen.scp_additions.scp_sign_clearance": "Clearance Level",
  "screen.scp_additions.scp_sign_anomaly": "Anomaly Type",
  "screen.scp_additions.scp_sign_custom_anomaly": "Custom Anomaly Type",
  "screen.scp_additions.scp_sign_hazard": "Hazard Slot %s"
}''',
)
