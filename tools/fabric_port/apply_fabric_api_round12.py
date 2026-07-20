from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
path = ROOT / "src/main/java/net/mcreator/scpadditions/fabric/mixin/client/ScreenDragMixin.java"
text = path.read_text(encoding="utf-8")
updated = text.replace(
    "import net.minecraft.client.gui.screens.Screen;",
    "import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;\n"
    "import net.minecraft.client.gui.screens.Screen;",
).replace(
    "@Mixin(Screen.class)",
    "@Mixin(AbstractContainerEventHandler.class)",
).replace(
    "        ScreenEvent.MouseDragged.Pre event = new ScreenEvent.MouseDragged.Pre(\n"
    "                (Screen) (Object) this,\n"
    "                mouseX, mouseY, button, dragX, dragY);",
    "        if (!((Object) this instanceof Screen screen)) return;\n"
    "        ScreenEvent.MouseDragged.Pre event = new ScreenEvent.MouseDragged.Pre(\n"
    "                screen, mouseX, mouseY, button, dragX, dragY);",
)
if updated == text:
    if "@Mixin(AbstractContainerEventHandler.class)" not in text:
        raise RuntimeError("Could not retarget ScreenDragMixin")
    print("Screen drag mixin is already targeted at its owning class")
else:
    path.write_text(updated, encoding="utf-8")
    print(f"Retargeted screen drag mixin in {path.relative_to(ROOT)}")
