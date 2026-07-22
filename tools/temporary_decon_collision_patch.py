from pathlib import Path


def replace(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Expected block not found in {path}:\n{old}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


controller = "src/main/java/net/mcreator/scpadditions/procedures/DecontaminationCheckpointController.java"
replace(
    controller,
    "import net.mcreator.scpadditions.effect.EyeProtectionAccess;\n",
    "import net.mcreator.scpadditions.effect.EyeProtectionAccess;\n"
    "import net.mcreator.scpadditions.facility.FacilityStructureBreakGuard;\n",
)
replace(
    controller,
    "        if (!state.is(ScpAdditionsModBlocks.DECON_OPEN.get())) return;\n\n"
    "        CheckpointKey key = new CheckpointKey(level.dimension(), pos.immutable());\n",
    "        if (!state.is(ScpAdditionsModBlocks.DECON_OPEN.get())\n"
    "                || FacilityStructureBreakGuard.isBeingMined(level, pos)) {\n"
    "            return;\n"
    "        }\n\n"
    "        CheckpointKey key = new CheckpointKey(level.dimension(), pos.immutable());\n",
)
replace(
    controller,
    "            BlockState current = level.getBlockState(pos);\n"
    "            if (!current.is(ScpAdditionsModBlocks.DECON_OPEN.get())) return;\n",
    "            BlockState current = level.getBlockState(pos);\n"
    "            if (!current.is(ScpAdditionsModBlocks.DECON_OPEN.get())\n"
    "                    || FacilityStructureBreakGuard.isBeingMined(level, pos)) {\n"
    "                return;\n"
    "            }\n",
)
replace(
    controller,
    "    public static void finishClosed(ServerLevel level, BlockPos pos) {\n"
    "        CheckpointKey key = new CheckpointKey(level.dimension(), pos.immutable());\n"
    "        try {\n"
    "            BlockState current = level.getBlockState(pos);\n"
    "            if (!current.is(ScpAdditionsModBlocks.DECON_CLOSED.get())) return;\n\n"
    "            level.playSound(null, BlockPos.containing(chamberCenter(pos, current)), ScpAdditionsModSounds.DOOROPEN.get(),\n"
    "                    SoundSource.BLOCKS, 1.0F, 1.0F);\n"
    "            level.setBlock(pos, copyCommonState(\n"
    "                    ScpAdditionsModBlocks.DECON_OPEN_RELOAD.get().defaultBlockState(), current), 3);\n"
    "        } finally {\n"
    "            PROCESSING.remove(key);\n"
    "        }\n"
    "    }\n",
    "    public static void finishClosed(ServerLevel level, BlockPos pos) {\n"
    "        CheckpointKey key = new CheckpointKey(level.dimension(), pos.immutable());\n"
    "        BlockState current = level.getBlockState(pos);\n"
    "        if (!current.is(ScpAdditionsModBlocks.DECON_CLOSED.get())) {\n"
    "            PROCESSING.remove(key);\n"
    "            return;\n"
    "        }\n"
    "        if (FacilityStructureBreakGuard.isBeingMined(level, pos)) {\n"
    "            level.scheduleTick(pos, ScpAdditionsModBlocks.DECON_CLOSED.get(), 10);\n"
    "            return;\n"
    "        }\n\n"
    "        try {\n"
    "            level.playSound(null, BlockPos.containing(chamberCenter(pos, current)), ScpAdditionsModSounds.DOOROPEN.get(),\n"
    "                    SoundSource.BLOCKS, 1.0F, 1.0F);\n"
    "            level.setBlock(pos, copyCommonState(\n"
    "                    ScpAdditionsModBlocks.DECON_OPEN_RELOAD.get().defaultBlockState(), current), 3);\n"
    "        } finally {\n"
    "            PROCESSING.remove(key);\n"
    "        }\n"
    "    }\n",
)
replace(
    controller,
    "        BlockState state = level.getBlockState(pos);\n"
    "        if (!state.is(ScpAdditionsModBlocks.DECON_OPEN_RELOAD.get())) return;\n"
    "        level.setBlock(pos, copyCommonState(\n"
    "                ScpAdditionsModBlocks.DECON_OPEN.get().defaultBlockState(), state), 3);\n"
    "    }\n\n"
    "    private static void decontaminate(ServerLevel level, ServerPlayer player) {\n",
    "        BlockState state = level.getBlockState(pos);\n"
    "        if (!state.is(ScpAdditionsModBlocks.DECON_OPEN_RELOAD.get())) return;\n"
    "        if (FacilityStructureBreakGuard.isBeingMined(level, pos)) {\n"
    "            level.scheduleTick(pos, ScpAdditionsModBlocks.DECON_OPEN_RELOAD.get(), 10);\n"
    "            return;\n"
    "        }\n"
    "        level.setBlock(pos, copyCommonState(\n"
    "                ScpAdditionsModBlocks.DECON_OPEN.get().defaultBlockState(), state), 3);\n"
    "    }\n\n"
    "    public static void forget(Level level, BlockPos pos) {\n"
    "        if (level == null || pos == null) return;\n"
    "        CheckpointKey key = new CheckpointKey(level.dimension(), pos.immutable());\n"
    "        LATCHED_UNTIL_EXIT.remove(key);\n"
    "        PROCESSING.remove(key);\n"
    "    }\n\n"
    "    private static void decontaminate(ServerLevel level, ServerPlayer player) {\n",
)

structure = "src/main/java/net/mcreator/scpadditions/block/DecontaminationStructure.java"
replace(
    structure,
    "\n        forEachPart(facing, (offsetX, offsetY, offsetZ) -> {\n        });\n",
    "\n",
)
replace(
    structure,
    "\n    private static void forEachPart(Direction facing, PartConsumer consumer) {\n"
    "        for (int offsetX = MIN_OFFSET; offsetX <= MAX_OFFSET; offsetX++) {\n"
    "            for (int offsetY = MIN_OFFSET; offsetY <= MAX_OFFSET; offsetY++) {\n"
    "                for (int offsetZ = MIN_OFFSET; offsetZ <= MAX_OFFSET; offsetZ++) {\n"
    "                    if (!isControllerOffset(offsetX, offsetY, offsetZ)\n"
    "                            && DecontaminationShapeHelper.hasStructurePart(\n"
    "                            facing, offsetX, offsetY, offsetZ)) {\n"
    "                        consumer.accept(offsetX, offsetY, offsetZ);\n"
    "                    }\n"
    "                }\n"
    "            }\n"
    "        }\n"
    "    }\n\n"
    "    @FunctionalInterface\n"
    "    private interface PartConsumer {\n"
    "        void accept(int offsetX, int offsetY, int offsetZ);\n"
    "    }\n",
    "\n",
)
