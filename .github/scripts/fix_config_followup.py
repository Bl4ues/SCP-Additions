from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"Expected block not found in {path}: {old[:120]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


# This class was a temporary cover-up for an old centered panel layout. Its
# coordinates no longer match the modern Configuration Center and the fill is
# the large grey bar visible outside General & Modules.
obsolete = Path(
    "src/main/java/net/mcreator/scpadditions/config/ui/ModulesHeaderCleanup.java"
)
if obsolete.exists():
    obsolete.unlink()

modules = (
    "src/main/java/net/mcreator/scpadditions/config/ui/"
    "Scp079ModulesScreenExtension.java"
)
replace_once(
    modules,
    '''            int visible = visibleRows();
            if (rows.size() > visible) {
                graphics.drawString(font,
                        ScpFonts.roboto("Mouse wheel: scroll options"),
                        panelX + panelWidth - 160, panelY + 31, MUTED, false);
            }
            int contentY = panelY + (sectionTitle == null ? 44 : 57);
''',
    '''            int visible = visibleRows();
            int contentY = panelY + (sectionTitle == null ? 44 : 57);
''',
)

config = "src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterClient.java"
replace_once(
    config,
    '''            homeNotice = result.message();
''',
    '''            // The save already returns to the Configuration Center. A second
            // transient success line only competes with the header composition.
            homeNotice = "";
''',
)

chair = (
    "src/main/java/net/mcreator/scpadditions/facility/"
    "ArchivistsChairBlock.java"
)
replace_once(
    chair,
    ''' * GeckoLib/Blockbench geometry is authored around the chair root pivot at
 * X=6, Z=0, while Block.box uses the block's north-west corner as zero. The
 * collision therefore translates from that authored pivot before mirroring X
 * and then follows the same FACING rotation as the renderer. Four practical
''',
    ''' * GeckoLib/Blockbench geometry is authored relative to the Bedrock model
 * origin, which GeoBlockRenderer places at the horizontal centre of the block.
 * Block.box instead uses the block's north-west corner as zero, so collision
 * coordinates need the same +8px translation on both X and Z. Four practical
''',
)
replace_once(
    chair,
    '''        // The Blockbench geometry is centred on the chair root pivot X=6, Z=0.
        // GeckoLib mirrors Bedrock X, so first translate X around that authored
        // pivot and only then mirror it around Minecraft's 8px block centre.
        return box(3.5D + minX, minY, 8.0D + minZ,
                3.5D + maxX, maxY, 8.0D + maxZ);
''',
    '''        // GeoBlockRenderer places the Bedrock horizontal origin at the
        // centre of the Minecraft block. The chair itself is authored off-centre
        // toward +X, so X must use the same +8px translation already used by Z.
        return box(8.0D + minX, minY, 8.0D + minZ,
                8.0D + maxX, maxY, 8.0D + maxZ);
''',
)
