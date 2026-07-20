from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
JAVA_ROOT = ROOT / "src/main/java"

GET_CAPABILITY = re.compile(
    r"(?P<expr>[A-Za-z_][A-Za-z0-9_]*"
    r"(?:\.[A-Za-z_][A-Za-z0-9_]*(?:\([^()\n]*\))?)*)"
    r"\.getCapability\(ScpInventoryCapability\.INSTANCE(?:,\s*null)?\)"
)

changed = 0
for path in JAVA_ROOT.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    updated = text.replace(
        "import net.neoforged.neoforge.common.util.LazyOptional;",
        "import com.bl4ues.scpadditions.compat.LazyOptional;",
    )
    updated = updated.replace(
        "import net.minecraftforge.common.util.LazyOptional;",
        "import com.bl4ues.scpadditions.compat.LazyOptional;",
    )
    updated = GET_CAPABILITY.sub(
        lambda match: f"ScpInventoryCapability.get({match.group('expr')})",
        updated,
    )
    if updated != text:
        path.write_text(updated, encoding="utf-8")
        changed += 1

main = JAVA_ROOT / "net/mcreator/scpadditions/ScpAdditionsMod.java"
text = main.read_text(encoding="utf-8")
updated = text.replace(
    "import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;\n",
    "",
)
if "import com.bl4ues.scpinventory.capability.ScpInventoryCapability;" not in updated:
    updated = updated.replace(
        "import com.bl4ues.scpinventory.config.ScpInventoryConfig;\n",
        "import com.bl4ues.scpinventory.config.ScpInventoryConfig;\n"
        "import com.bl4ues.scpinventory.capability.ScpInventoryCapability;\n",
    )
if "ScpInventoryCapability.REGISTRY.register(bus);" not in updated:
    updated = updated.replace(
        "        ScpAdditionsModItems.REGISTRY.register(bus);\n",
        "        ScpAdditionsModItems.REGISTRY.register(bus);\n"
        "        ScpInventoryCapability.REGISTRY.register(bus);\n",
    )
if updated != text:
    main.write_text(updated, encoding="utf-8")
    changed += 1

remaining = []
for path in JAVA_ROOT.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    if "getCapability(ScpInventoryCapability.INSTANCE" in text:
        remaining.append(str(path.relative_to(ROOT)))

print(f"Updated {changed} files for the inventory attachment")
if remaining:
    print("Unconverted inventory capability calls:")
    print("\n".join(remaining))
    raise SystemExit(1)
