from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA_ROOT = ROOT / "src/main/java"

REPLACEMENTS = {
    "import net.neoforged.fml.DistExecutor;":
        "import com.bl4ues.scpadditions.compat.DistExecutor;",
    "import net.minecraftforge.fml.DistExecutor;":
        "import com.bl4ues.scpadditions.compat.DistExecutor;",
    "import net.minecraftforge.client.settings.KeyConflictContext;":
        "import net.neoforged.neoforge.client.settings.KeyConflictContext;",
    "software.bernie.geckolib.core.":
        "software.bernie.geckolib.",
    "import net.minecraftforge.network.NetworkHooks;\n":
        "",
    "NetworkHooks.openScreen(player,":
        "player.openMenu(",
}

changed = 0
for path in JAVA_ROOT.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    updated = text
    for before, after in REPLACEMENTS.items():
        updated = updated.replace(before, after)
    if updated != text:
        path.write_text(updated, encoding="utf-8")
        changed += 1

print(f"Updated {changed} files for moved APIs")
