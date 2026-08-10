from pathlib import Path


def replace(path: str, old: str, new: str, count: int = 1) -> None:
    p = Path(path)
    text = p.read_text()
    actual = text.count(old)
    if actual != count:
        raise SystemExit(
            f"{path}: expected {count} occurrence(s), found {actual}: {old[:160]!r}"
        )
    p.write_text(text.replace(old, new, count))


# Centralize the two legacy advancement ids in the existing server-side helper.
awards = "src/main/java/net/mcreator/scpadditions/advancement/ScpAdvancementAwards.java"
replace(
    awards,
    '''    public static final ResourceLocation FROM_THE_TRENCHES = id("from_the_trenches");\n    public static final ResourceLocation EYES_ON_ME = id("eyes_on_me");\n''',
    '''    public static final ResourceLocation FROM_THE_TRENCHES = id("from_the_trenches");\n    public static final ResourceLocation TESLA = id("tesla");\n    public static final ResourceLocation SWEET_TOOTH = id("scp_330_achievement");\n    public static final ResourceLocation EYES_ON_ME = id("eyes_on_me");\n''',
)

# SCP-330: award at the second, still-safe candy instead of on hand loss.
scp330 = "src/main/java/net/mcreator/scpadditions/scp330/Scp330Hands.java"
replace(scp330, "import net.minecraft.advancements.Advancement;\n", "")
replace(scp330, "import net.minecraft.advancements.AdvancementProgress;\n", "")
replace(scp330, "import net.minecraft.resources.ResourceLocation;\n", "")
replace(
    scp330,
    "import net.mcreator.scpadditions.ScpAdditionsMod;\n",
    "import net.mcreator.scpadditions.ScpAdditionsMod;\nimport net.mcreator.scpadditions.advancement.ScpAdvancementAwards;\n",
)
replace(
    scp330,
    '''            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(candy));\n            player.getPersistentData().putInt(COUNT_TAG, taken + 1);\n            return true;\n''',
    '''            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(candy));\n            int nextTaken = taken + 1;\n            player.getPersistentData().putInt(COUNT_TAG, nextTaken);\n            if (nextTaken == 2 && player instanceof ServerPlayer serverPlayer) {\n                ScpAdvancementAwards.award(serverPlayer,\n                        ScpAdvancementAwards.SWEET_TOOTH);\n            }\n            return true;\n''',
)
replace(scp330, "        awardAdvancement(player);\n", "")
replace(
    scp330,
    '''    private static void awardAdvancement(Player player) {\n        if (!(player instanceof ServerPlayer serverPlayer)) return;\n        Advancement advancement = serverPlayer.server.getAdvancements()\n                .getAdvancement(new ResourceLocation(ScpAdditionsMod.MODID,\n                        "scp_330_achievement"));\n        if (advancement == null) return;\n        AdvancementProgress progress = serverPlayer.getAdvancements()\n                .getOrStartProgress(advancement);\n        for (String criterion : progress.getRemainingCriteria()) {\n            serverPlayer.getAdvancements().award(advancement, criterion);\n        }\n    }\n\n''',
    "",
)

# Tesla Gate: being electrocuted remains lethal, but no longer grants the achievement.
tesla_pulse = "src/main/java/net/mcreator/scpadditions/procedures/TeslaGatePulseHelper.java"
replace(tesla_pulse, "import net.minecraft.advancements.Advancement;\n", "")
replace(tesla_pulse, "import net.minecraft.advancements.AdvancementProgress;\n", "")
replace(tesla_pulse, "import net.minecraft.resources.ResourceLocation;\n", "")
replace(tesla_pulse, "import net.minecraft.server.level.ServerPlayer;\n", "")
replace(
    tesla_pulse,
    '''\n            if (living instanceof ServerPlayer player) {\n                Advancement advancement = player.server.getAdvancements()\n                        .getAdvancement(new ResourceLocation("scp_additions:tesla"));\n                if (advancement != null) {\n                    AdvancementProgress progress = player.getAdvancements()\n                            .getOrStartProgress(advancement);\n                    if (!progress.isDone()) {\n                        for (String criteria : progress.getRemainingCriteria()) {\n                            player.getAdvancements().award(advancement, criteria);\n                        }\n                    }\n                }\n            }\n''',
    "\n",
)

# A successful authenticated Tesla-terminal configuration is now the safe interaction goal.
tesla_controller = "src/main/java/net/mcreator/scpadditions/procedures/TeslaTerminalController.java"
replace(
    tesla_controller,
    "import net.minecraft.world.entity.player.Player;\n",
    "import net.minecraft.world.entity.player.Player;\nimport net.minecraft.server.level.ServerPlayer;\n",
)
replace(
    tesla_controller,
    "import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;\n",
    "import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;\nimport net.mcreator.scpadditions.advancement.ScpAdvancementAwards;\n",
)
replace(
    tesla_controller,
    '''\t\tScp079FacilityAccessManager.recordActivity(world,\n\t\t\t\tScp079FacilityAccessManager.Activity.TESLA_CONFIGURATION);\n''',
    '''\t\trecordConfiguration(world, player);\n''',
    3,
)
replace(
    tesla_controller,
    '''\tprivate static boolean requireAuxiliaryPower(LevelAccessor world, Player player) {\n''',
    '''\tprivate static void recordConfiguration(LevelAccessor world, Player player) {\n\t\tScp079FacilityAccessManager.recordActivity(world,\n\t\t\t\tScp079FacilityAccessManager.Activity.TESLA_CONFIGURATION);\n\t\tif (player instanceof ServerPlayer serverPlayer) {\n\t\t\tScpAdvancementAwards.award(serverPlayer, ScpAdvancementAwards.TESLA);\n\t\t}\n\t}\n\n\tprivate static boolean requireAuxiliaryPower(LevelAccessor world, Player player) {\n''',
)

# Visible English copy: serious, direct and non-lethal.
lang = "src/main/resources/assets/scp_additions/lang/en_us.json"
replace(lang, '"Fatal Discharge"', '"Tesla Grid Access"')
replace(
    lang,
    '"Be killed by a Tesla Gate discharge"',
    '"Configure the Tesla Gate network from a terminal"',
)
replace(
    lang,
    '"Took more than two candies from SCP-330. Need a hand?"',
    '"Take two candies from SCP-330"',
)

# Keep public-facing terminology consistent and document the design change.
changelog = "CHANGELOG.md"
replace(
    changelog,
    '''- Added the hidden **What?** achievement for having SCP-714 prevent SCP-012 from taking hold;\n''',
    '''- Added the hidden **What?** achievement for having SCP-714 prevent SCP-012 from taking hold;\n- Reworked the Tesla Gate and SCP-330 achievements into non-lethal interaction goals, keeping the full achievement set obtainable in Hardcore runs;\n''',
)
replace(
    changelog,
    "**Custom Advancement Toasts**",
    "**Custom Achievement Toasts**",
)
