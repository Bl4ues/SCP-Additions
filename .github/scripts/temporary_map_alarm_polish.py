from pathlib import Path
root=Path('src/main/java/com/bl4ues/scpclassifieddirective')
p=root/'client/scp079/Scp079FacilityMapScreen.java'
s=p.read_text()
def replace_once(before, after):
    global s
    assert s.count(before)==1, (str(p), before, s.count(before))
    s=s.replace(before,after,1)
replace_once('''        // Clearance is attached to the door's midpoint in screen space.
        // It is readable at every zoom, never scaled by the map/camera.
        final int half = 9;''','''        // The badge belongs to the map, not the camera/HUD: zooming away
        // makes it proportionally small, while close zoom caps its screen size.
        // The door midpoint remains the anchor at every orientation.
        final int half = clearanceHalfSize(transform);''')
replace_once('''        drawCenteredMapLabel(graphics, value, x, y + 1, 1.85F,
                0xFF07151C);''','''        drawCenteredMapLabel(graphics, value, x, y,
                half * (1.67F / 9.0F), 0xFF07151C);''')
replace_once('''        drawCenteredMapLabel(graphics,
                Integer.toString(marker.requiredLevel()),
                x, y + 1, 1.60F, 0xFFFFFFFF);''','''        drawCenteredMapLabel(graphics,
                Integer.toString(marker.requiredLevel()),
                x, y, clearanceHalfSize(transform) * (1.52F / 9.0F),
                0xFFFFFFFF);''')
replace_once('''    private void drawFixedCenteredMapLabel(GuiGraphics graphics, String value,
''','''    private static int clearanceHalfSize(MapTransform transform) {
        // A map unit scales exactly like the door segment beneath the badge.
        return Mth.clamp((int) Math.round(transform.scale() * 0.38D), 2, 9);
    }

    private void drawFixedCenteredMapLabel(GuiGraphics graphics, String value,
''')
replace_once('''        // Font ascent sits above the line box center; compensate for it.
        float y = -font.lineHeight * 0.5F + 2.8F;''','''        // The old +2.8 baseline pushed the clearance digit below its badge.
        float y = -font.lineHeight * 0.5F + 0.2F;''')
p.write_text(s)
p=root/'facility/alarm/AlarmModule.java'; s=p.read_text(); old='Use a Screwdriver to toggle alarm sound without disabling its light.'; assert s.count(old)==1;s=s.replace(old,'Use a Screwdriver to toggle alarm sound.');p.write_text(s)
print('Map clearance scales with the map, digit re-centered, Alarm tooltip simplified.')
