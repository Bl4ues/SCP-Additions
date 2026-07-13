from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one {label} match, found {count}")
    return text.replace(old, new, 1)


spawn_path = Path("src/main/java/net/mcreator/scpadditions/event/Scp173SpawnEvents.java")
spawn = spawn_path.read_text(encoding="utf-8")
spawn = replace_once(
    spawn,
    "import net.minecraft.server.level.ServerPlayer;\n",
    "import net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.sounds.SoundSource;\n",
    "spawn SoundSource import",
)
spawn = replace_once(
    spawn,
    "import net.mcreator.scpadditions.entity.Scp173Entity;\n",
    "import net.mcreator.scpadditions.entity.Scp173Entity;\nimport net.mcreator.scpadditions.entity.Scp173Sounds;\n",
    "spawn Scp173Sounds import",
)
spawn = replace_once(
    spawn,
    "                spawn173(level, pos, player);",
    "                spawn173(level, pos, player, random);",
    "spawn call",
)
spawn = replace_once(
    spawn,
    "    private static void spawn173(ServerLevel level, BlockPos pos, ServerPlayer player) {",
    "    private static void spawn173(ServerLevel level, BlockPos pos, ServerPlayer player, RandomSource random) {",
    "spawn method signature",
)
spawn = replace_once(
    spawn,
    "        level.addFreshEntity(scp173);\n",
    "        level.addFreshEntity(scp173);\n"
    "        level.playSound(null, x, y + 0.6D, z, Scp173Sounds.RATTLE.get(), SoundSource.HOSTILE,\n"
    "                0.72F, 0.96F + random.nextFloat() * 0.08F);\n",
    "natural-spawn rattle",
)
spawn_path.write_text(spawn, encoding="utf-8")


entity_path = Path("src/main/java/net/mcreator/scpadditions/entity/Scp173Entity.java")
entity = entity_path.read_text(encoding="utf-8")
entity = replace_once(
    entity,
    "    public void markRoutineSpawn() {\n"
    "        entityData.set(ROUTINE_SPAWN, true);\n"
    "        setActivated(false);\n"
    "        setPersistenceRequired();\n"
    "        lastSeenOrCloseTick = tickCount;\n"
    "        setTarget(null);\n"
    "    }",
    "    public void markRoutineSpawn() {\n"
    "        entityData.set(ROUTINE_SPAWN, true);\n"
    "        setActivated(false);\n"
    "        setNoAi(true);\n"
    "        entityData.set(SCRAPING, false);\n"
    "        setPersistenceRequired();\n"
    "        lastSeenOrCloseTick = tickCount;\n"
    "        setTarget(null);\n"
    "        getNavigation().stop();\n"
    "    }",
    "routine-spawn initialization",
)
entity = replace_once(
    entity,
    "        if (!isActivated()) {\n"
    "            super.tick();\n"
    "            LivingEntity observer = findObservingEntity();\n"
    "            if (observer != null) {\n"
    "                setActivated(true);\n"
    "                setTarget(observer);\n"
    "                if (observer instanceof Player) lastSeenOrCloseTick = tickCount;\n"
    "            }\n"
    "            restorePose(preTickPose);\n"
    "            stopAndLock(preTickPose);\n"
    "            handleRoutineDespawn();\n"
    "            return;\n"
    "        }",
    "        if (!isActivated()) {\n"
    "            // Natural spawns remain inert until a non-creative player actually sees them.\n"
    "            // No-AI prevents target selection/navigation during super.tick(), while the\n"
    "            // explicit target and scraping reset also protects worlds saved before 3.0.1.\n"
    "            setNoAi(true);\n"
    "            setTarget(null);\n"
    "            getNavigation().stop();\n"
    "            entityData.set(SCRAPING, false);\n"
    "            super.tick();\n"
    "\n"
    "            Player observer = findObservingPlayer();\n"
    "            if (observer != null) {\n"
    "                setActivated(true);\n"
    "                setNoAi(false);\n"
    "                setTarget(observer);\n"
    "                lastSeenOrCloseTick = tickCount;\n"
    "            }\n"
    "            restorePose(preTickPose);\n"
    "            stopAndLock(preTickPose);\n"
    "            handleRoutineDespawn();\n"
    "            return;\n"
    "        }",
    "inactive tick branch",
)
entity = replace_once(
    entity,
    "        entityData.set(ROUTINE_SPAWN, tag.getBoolean(\"RoutineSpawn\"));\n"
    "        lastSeenOrCloseTick = tag.getInt(\"LastSeenOrCloseTick\");\n"
    "    }",
    "        entityData.set(ROUTINE_SPAWN, tag.getBoolean(\"RoutineSpawn\"));\n"
    "        lastSeenOrCloseTick = tag.getInt(\"LastSeenOrCloseTick\");\n"
    "        if (!isActivated()) {\n"
    "            setNoAi(true);\n"
    "            setTarget(null);\n"
    "            entityData.set(SCRAPING, false);\n"
    "        }\n"
    "    }",
    "inactive-load safeguard",
)
entity_path.write_text(entity, encoding="utf-8")


changelog_path = Path("CHANGELOG.md")
changelog = changelog_path.read_text(encoding="utf-8")
changelog = replace_once(
    changelog,
    "- Removed the immediate natural-spawn rattle so SCP-173 does not announce itself before the player actually sees it;\n"
    "- Kept the initial SCP-173 scare and blink HUD activation gated behind confirmed player vision and proximity;",
    "- Restored the intended rattle sound when SCP-173 naturally spawns;\n"
    "- Prevented naturally spawned SCP-173 from selecting targets, navigating, moving, or producing dragging sounds before a non-creative player actually sees it;\n"
    "- Restricted the initial activation to confirmed player observation, while other configured observers can still freeze SCP-173 normally after activation;\n"
    "- Kept the initial SCP-173 scare and blink HUD activation gated behind confirmed player vision and proximity;",
    "3.0.1 SCP-173 changelog correction",
)
changelog_path.write_text(changelog, encoding="utf-8")

print("SCP-173 rattle and visual-activation correction applied")
