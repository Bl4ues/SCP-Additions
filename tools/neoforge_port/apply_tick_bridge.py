from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA_ROOT = ROOT / "src/main/java"

changed = 0
for path in JAVA_ROOT.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    updated = text.replace(
        "import net.minecraftforge.event.TickEvent;",
        "import com.bl4ues.scpadditions.compat.TickEvent;",
    )
    if updated != text:
        path.write_text(updated, encoding="utf-8")
        changed += 1

# The bootstrap converted the generic type, but not the anonymous array type.
bleeding = JAVA_ROOT / "net/mcreator/scpadditions/scp012/Scp012BleedingEvents.java"
if bleeding.exists():
    text = bleeding.read_text(encoding="utf-8")
    updated = text.replace(
        "Supplier<SoundEvent>[] sounds = new RegistryObject[]{",
        "Supplier<SoundEvent>[] sounds = new Supplier[]{",
    )
    if updated != text:
        bleeding.write_text(updated, encoding="utf-8")
        changed += 1

print(f"Updated {changed} source files for the tick compatibility bridge")
