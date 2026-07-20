import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
changed: list[str] = []

screen_path = ROOT / "src/main/java/com/bl4ues/scpinventory/client/gui/ScpInventoryScreen.java"
text = screen_path.read_text(encoding="utf-8")
needle = "    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {\n"
route = (
    needle
    + "        if (mode == ScreenMode.CRAFTING && craftingPanel != null\n"
    + "                && craftingPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;\n"
)
if "mode == ScreenMode.CRAFTING && craftingPanel != null" not in text:
    if needle not in text:
        raise RuntimeError("Could not locate SCP inventory drag handler")
    text = text.replace(needle, route, 1)
    screen_path.write_text(text, encoding="utf-8")
    changed.append(str(screen_path.relative_to(ROOT)))

mixins_path = ROOT / "src/main/resources/scp_additions.mixins.json"
metadata = json.loads(mixins_path.read_text(encoding="utf-8"))
client = list(metadata.get("client", []))
if "client.ScreenDragMixin" in client:
    client.remove("client.ScreenDragMixin")
    metadata["client"] = client
    mixins_path.write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    changed.append(str(mixins_path.relative_to(ROOT)))

print(f"Fabric API round 13 changed {len(changed)} files")
for item in changed:
    print(item)
