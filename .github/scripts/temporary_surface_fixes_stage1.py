from pathlib import Path
import shutil
r = Path('src/main')
assets = r/'resources/assets/scp_classified_directive'
source = assets/'textures/item/facility_mapping_tool.png'
assert source.is_file(), source
for name in ('surface_construct.png', 'offgrid_construct.png'):
    destination = assets/'textures/item'/name
    if not destination.exists():
        shutil.copyfile(source, destination)

p = r/'java/com/bl4ues/scpclassifieddirective/facility/FacilityPipeModule.java'
s = p.read_text()
a = 'tooltip.add(Component.literal("Decorative only")'
b = 'tooltip.add(Component.literal("Decorative Only")'
assert s.count(a)==1
p.write_text(s.replace(a,b))

p = r/'java/com/bl4ues/scpclassifieddirective/client/scp079/Scp079FacilityMapScreen.java'
s = p.read_text()
a = '''        // Supplementary clearance stays hidden until the operator zooms in.
        // Once visible it always uses the same maximum size, independent of
        // zoom or the physical door angle, so it cannot bury other markers.
        if (mapZoom < 1.55D) return;
        final int half = 9;'''
b = '''        // Clearance is attached to the door's midpoint in screen space.
        // It is readable at every zoom, never scaled by the map/camera.
        final int half = 9;'''
assert s.count(a)==1
s = s.replace(a,b)
a='''        drawCenteredMapLabel(graphics, value, x, y, 1.85F,
                0xFF07151C);'''
b='''        drawCenteredMapLabel(graphics, value, x, y + 1, 1.85F,
                0xFF07151C);'''
assert s.count(a)==1
s=s.replace(a,b)
a='''        if (mapZoom < 1.55D) return;
        drawCenteredMapLabel(graphics,
                Integer.toString(marker.requiredLevel()),
                x, y, 1.60F, 0xFFFFFFFF);'''
b='''        drawCenteredMapLabel(graphics,
                Integer.toString(marker.requiredLevel()),
                x, y + 1, 1.60F, 0xFFFFFFFF);'''
assert s.count(a)==1
s=s.replace(a,b)
p.write_text(s)
