from pathlib import Path

# Applied after the main integration patch so GUI interpolation uses pixels.
path = Path("src/main/java/net/mcreator/scpadditions/client/ElevatorArrivalOverlay.java")
text = path.read_text(encoding="utf-8")
old_sector = """        int sectorY = Mth.lerp((float) sectorProgress,
                sectorHiddenY, sectorShownY);
"""
new_sector = """        int sectorY = Math.round(Mth.lerp((float) sectorProgress,
                sectorHiddenY, sectorShownY));
"""
old_floor = """        int floorY = Mth.lerp((float) floorProgress,
                floorHiddenY, floorShownY);
"""
new_floor = """        int floorY = Math.round(Mth.lerp((float) floorProgress,
                floorHiddenY, floorShownY));
"""
if text.count(old_sector) != 1 or text.count(old_floor) != 1:
    raise RuntimeError("Expected overlay interpolation snippets were not found")
path.write_text(text.replace(old_sector, new_sector, 1)
        .replace(old_floor, new_floor, 1), encoding="utf-8")
