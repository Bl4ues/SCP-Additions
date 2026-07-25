from pathlib import Path


def replace(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if new in text:
        return
    if old not in text:
        raise SystemExit(f"Expected block not found in {path}: {old[:140]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


chase = "src/main/java/net/mcreator/scpadditions/client/Scp106ChaseSound.java"
replace(
    chase,
    "    private static final int FADE_OUT_TICKS = 32;\n"
    "    private static final int STOP_CUE_LEAD_TICKS = 14;\n"
    "    private static final float STOP_CUE_VOLUME = 0.34F;\n\n"
    "    private int fadeTicksRemaining = -1;",
    "    private static final int FADE_IN_TICKS = 36;\n"
    "    private static final int FADE_OUT_TICKS = 32;\n"
    "    private static final int STOP_CUE_LEAD_TICKS = 12;\n"
    "    private static final float STOP_CUE_VOLUME = 0.14F;\n\n"
    "    private int fadeInTicksElapsed;\n"
    "    private int fadeTicksRemaining = -1;",
)
replace(chase, "        this.volume = 1.0F;", "        this.volume = 0.0F;")
replace(
    chase,
    "        if (fadeTicksRemaining < 0) return;\n"
    "        if (playStopCue && !stopCuePlayed",
    "        float fadeInVolume = Mth.clamp(fadeInTicksElapsed\n"
    "                / (float) FADE_IN_TICKS, 0.0F, 1.0F);\n"
    "        if (fadeTicksRemaining < 0) {\n"
    "            if (fadeInTicksElapsed < FADE_IN_TICKS) fadeInTicksElapsed++;\n"
    "            volume = Mth.clamp(fadeInTicksElapsed\n"
    "                    / (float) FADE_IN_TICKS, 0.0F, 1.0F);\n"
    "            return;\n"
    "        }\n"
    "        if (playStopCue && !stopCuePlayed",
)
replace(
    chase,
    "        volume = Mth.clamp(fadeTicksRemaining / (float) FADE_OUT_TICKS,\n"
    "                0.0F, 1.0F);",
    "        volume = fadeInVolume * Mth.clamp(\n"
    "                fadeTicksRemaining / (float) FADE_OUT_TICKS,\n"
    "                0.0F, 1.0F);",
)

portal = "src/main/java/net/mcreator/scpadditions/client/Scp106PortalParticle.java"
replace(
    portal,
    "    private final SpriteSet sprites;\n"
    "    private final Vec3 normal;",
    "    private final SpriteSet sprites;\n"
    "    private final Vec3 normal;\n"
    "    private final float baseQuadSize;\n"
    "    private final int fadeInTicks;",
)
replace(
    portal,
    "        this.quadSize = transientSurface\n"
    "                ? 0.58F + this.random.nextFloat() * 0.18F\n"
    "                : 0.95F + this.random.nextFloat() * 0.25F;",
    "        this.baseQuadSize = transientSurface\n"
    "                ? 0.58F + this.random.nextFloat() * 0.18F\n"
    "                : 0.95F + this.random.nextFloat() * 0.25F;\n"
    "        this.fadeInTicks = transientSurface ? 6 : 10;\n"
    "        this.quadSize = baseQuadSize * 0.18F;",
)
replace(portal, "        this.setAlpha(MAX_ALPHA);", "        this.setAlpha(0.0F);")
replace(
    portal,
    "        float remaining = 1.0F - Mth.clamp(\n"
    "                this.age / (float) this.lifetime, 0.0F, 1.0F);\n"
    "        float fade = Mth.clamp(remaining / 0.28F, 0.0F, 1.0F);\n"
    "        this.setAlpha(MAX_ALPHA * fade);\n"
    "        this.quadSize += 0.0009F;",
    "        float remaining = 1.0F - Mth.clamp(\n"
    "                this.age / (float) this.lifetime, 0.0F, 1.0F);\n"
    "        float fadeOut = Mth.clamp(remaining / 0.28F, 0.0F, 1.0F);\n"
    "        float appear = Mth.clamp(this.age / (float) fadeInTicks,\n"
    "                0.0F, 1.0F);\n"
    "        float smoothAppear = appear * appear * (3.0F - 2.0F * appear);\n"
    "        this.setAlpha(MAX_ALPHA * smoothAppear * fadeOut);\n"
    "        this.quadSize = baseQuadSize\n"
    "                * (0.18F + 0.82F * smoothAppear)\n"
    "                + this.age * 0.0009F;",
)

entity = "src/main/java/net/mcreator/scpadditions/entity/Scp106Entity.java"
replace(
    entity,
    "    private static final int RANGED_HAND_PARTICLE_START_TICK = 26;",
    "    private static final int RANGED_HAND_PARTICLE_START_TICK = 33;",
)
replace(
    entity,
    "        serverLevel.sendParticles(ParticleTypes.SQUID_INK,\n"
    "                hand.x, hand.y, hand.z, 2,\n"
    "                0.055D, 0.055D, 0.055D, 0.008D);\n"
    "        serverLevel.sendParticles(ParticleTypes.SMOKE,\n"
    "                hand.x, hand.y, hand.z, 1,\n"
    "                0.035D, 0.035D, 0.035D, 0.004D);",
    "        if ((rangedAttackTicks & 1) == 0) {\n"
    "            serverLevel.sendParticles(ParticleTypes.SMOKE,\n"
    "                    hand.x, hand.y, hand.z, 1,\n"
    "                    0.014D, 0.014D, 0.014D, 0.002D);\n"
    "        }\n"
    "        if (rangedAttackTicks % 4 == 0) {\n"
    "            serverLevel.sendParticles(ParticleTypes.ASH,\n"
    "                    hand.x, hand.y, hand.z, 1,\n"
    "                    0.010D, 0.010D, 0.010D, 0.001D);\n"
    "        }",
)

changelog = Path("CHANGELOG.md")
text = changelog.read_text(encoding="utf-8")
marker = "- Adjusted the chase ending so its final cue begins during the fade at a lower volume, blending both sounds more naturally;\n"
addition = (
    marker
    + "- Added a gentle fade-in to the SCP-106 chase music and further lowered its ending cue so transitions are less noticeable;\n"
    + "- Reduced the ranged attack's hand particles and made SCP-106 portals grow and fade in smoothly instead of appearing all at once;\n"
)
if "gentle fade-in to the SCP-106 chase music" not in text:
    if marker not in text:
        raise SystemExit("Could not find SCP-106 audio changelog marker")
    text = text.replace(marker, addition, 1)
changelog.write_text(text, encoding="utf-8")
