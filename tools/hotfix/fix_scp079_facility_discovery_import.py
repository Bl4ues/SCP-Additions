from pathlib import Path

root = Path(__file__).resolve().parents[2]
target = root / "src/main/java/net/mcreator/scpadditions/block/Scp079onBlock.java"
text = target.read_text(encoding="utf-8")
required = "import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;"
if required not in text:
    anchor = "import net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;"
    if anchor not in text:
        raise RuntimeError("SCP-079 facility manager import anchor is missing")
    text = text.replace(anchor, anchor + "\n" + required, 1)
    target.write_text(text, encoding="utf-8")

Path(__file__).unlink()
