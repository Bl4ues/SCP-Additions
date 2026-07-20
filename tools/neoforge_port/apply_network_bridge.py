from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA_ROOT = ROOT / "src/main/java"

REPLACEMENTS = {
    "import net.minecraftforge.network.simple.SimpleChannel;":
        "import com.bl4ues.scpadditions.compat.network.SimpleChannel;",
    "import net.minecraftforge.network.NetworkRegistry;":
        "import com.bl4ues.scpadditions.compat.network.NetworkRegistry;",
    "import net.minecraftforge.network.NetworkEvent;":
        "import com.bl4ues.scpadditions.compat.network.NetworkEvent;",
    "import net.minecraftforge.network.PacketDistributor;":
        "import com.bl4ues.scpadditions.compat.network.PacketDistributor;",
    "net.minecraftforge.network.simple.SimpleChannel":
        "com.bl4ues.scpadditions.compat.network.SimpleChannel",
    "net.minecraftforge.network.NetworkRegistry":
        "com.bl4ues.scpadditions.compat.network.NetworkRegistry",
    "net.minecraftforge.network.NetworkEvent":
        "com.bl4ues.scpadditions.compat.network.NetworkEvent",
    "net.minecraftforge.network.PacketDistributor":
        "com.bl4ues.scpadditions.compat.network.PacketDistributor",
    "import net.minecraftforge.fml.DistExecutor;":
        "import net.neoforged.fml.DistExecutor;",
    "net.minecraftforge.fml.DistExecutor":
        "net.neoforged.fml.DistExecutor",
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

print(f"Updated {changed} source files for the network compatibility bridge")
