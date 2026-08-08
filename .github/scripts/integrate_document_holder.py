import json
from pathlib import Path


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


facility = Path("src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java")
text = facility.read_text(encoding="utf-8")
needle = "        addExternalCreativeItem(functional, ScpAdditionsModBlocks.SCP_079_SYSTEM_CONTROL.get().asItem());\n"
replacement = needle + "        addExternalCreativeItem(functional, DocumentHolderModule.item());\n"
text = replace_once(text, needle, replacement, "facility creative placement")
facility.write_text(text, encoding="utf-8")

lang = Path("src/main/resources/assets/scp_additions/lang/en_us.json")
data = json.loads(lang.read_text(encoding="utf-8"))
data["block.scp_additions.document_holder"] = "Document Holder"
lang.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n",
                encoding="utf-8")

changelog = Path("CHANGELOG.md")
text = changelog.read_text(encoding="utf-8")
needle = "- Added Roombas;\n"
replacement = needle + (
    "- Added a wall-mounted **Document Holder** that stores one dedicated Document item, preserves its full document data, and uses authored GeckoLib open, take, fill, and close animations;\n"
)
text = replace_once(text, needle, replacement, "changelog entry")
changelog.write_text(text, encoding="utf-8")
