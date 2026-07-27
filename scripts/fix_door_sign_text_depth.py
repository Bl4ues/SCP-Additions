from pathlib import Path

path = Path("src/main/java/net/mcreator/scpadditions/client/FacilitySignBlockEntityRenderer.java")
text = path.read_text(encoding="utf-8")
old = "private static final float DOOR_SURFACE_OFFSET = 0.30F * MODEL_PIXEL;"
new = "private static final float DOOR_SURFACE_OFFSET = 0.03125F * MODEL_PIXEL;"
if text.count(old) != 1:
    raise RuntimeError(f"Expected exactly one Door Sign surface offset, found {text.count(old)}")
path.write_text(text.replace(old, new, 1), encoding="utf-8")
