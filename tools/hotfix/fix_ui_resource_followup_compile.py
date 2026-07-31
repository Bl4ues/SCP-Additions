from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]

for relative in (
    "src/main/java/net/mcreator/scpadditions/client/gui/ElevatorArrivalEditorScreen.java",
    "src/main/java/com/bl4ues/scpinventory/client/gui/ContextAnchorEditorScreen.java",
):
    path = ROOT / relative
    text = path.read_text(encoding="utf-8")
    old = "        protected void renderWidget(GuiGraphics graphics, int mouseX,\n"
    if text.count(old) != 1:
        raise RuntimeError(f"Centered EditBox signature not found in {relative}")
    path.write_text(text.replace(old,
            "        public void renderWidget(GuiGraphics graphics, int mouseX,\n", 1),
            encoding="utf-8")

path = ROOT / "src/main/java/net/mcreator/scpadditions/client/Scp330Client.java"
text = path.read_text(encoding="utf-8")
text = text.replace("import com.mojang.blaze3d.vertex.PoseStack;\n", "")
text = text.replace("import com.mojang.math.Axis;\n", "")
text = text.replace("import net.minecraft.client.renderer.MultiBufferSource;\n", "")
pattern = re.compile(
    r"    private static final class Renderer extends GeoBlockRenderer<Scp330BlockEntity> \{.*?^    \}\n",
    re.MULTILINE | re.DOTALL,
)
replacement = """    private static final class Renderer extends GeoBlockRenderer<Scp330BlockEntity> {
        private Renderer() {
            super(new Model());
        }
    }
"""
text, count = pattern.subn(replacement, text, count=1)
if count != 1:
    raise RuntimeError("Generated SCP-330 renderer override was not found")
path.write_text(text, encoding="utf-8")
