from pathlib import Path
root=Path('src/main/java/com/bl4ues/scpclassifieddirective/client/scp079')
p=root/'Scp079FacilityMapScreen.java'
s=p.read_text()
a='''        // Credential badges are screen-space UI, not world geometry. Keep the
        // square and glyph readable at every map zoom and cover the closed-door
        // bar cleanly at its center.
        int half = 8;
        int badge = color;
        graphics.fill(x - half, y - half, x + half + 1, y + half + 1, badge);
        border(graphics, x - half, y - half, half * 2 + 1, half * 2 + 1,
                locked ? 0xFF89989D : 0xFFFFFFFF);
        drawCenteredMapLabel(graphics, value, x, y, 1.30F,
                locked ? 0xFF172126 : 0xFF071116);'''
b='''        // Clearance is supplementary information: expose it only once the
        // operator zooms into the doorway. Its badge stops growing at the
        // maximum readable camera zoom and never scales with the world line.
        if (mapZoom < 1.55D) return;
        int half = Math.min(9, 4 + (int) Math.round(
                (mapZoom - 1.55D) * 5.0D));
        int borderColor = locked ? 0xFF89989D : color;
        graphics.fill(x - half, y - half, x + half + 1, y + half + 1,
                0xF20A1D27);
        border(graphics, x - half, y - half, half * 2 + 1, half * 2 + 1,
                borderColor);
        drawCenteredMapLabel(graphics, value, x, y, 1.60F,
                locked ? 0xFF89989D : 0xFFF4FAFD);'''
assert s.count(a)==1,s.count(a)
s=s.replace(a,b)
a='''        drawCenteredMapLabel(graphics,
                Integer.toString(marker.requiredLevel()),
                x, y, 1.30F, 0xFFFFFFFF);'''
b='''        if (mapZoom < 1.55D) return;
        drawCenteredMapLabel(graphics,
                Integer.toString(marker.requiredLevel()),
                x, y, 1.60F, 0xFFFFFFFF);'''
assert s.count(a)==1
s=s.replace(a,b)
a='''        float y = -font.lineHeight * 0.5F + 0.5F;'''
b='''        // Font ascent sits above the line box center; compensate for it.
        float y = -font.lineHeight * 0.5F + 1.8F;'''
assert s.count(a)==1
s=s.replace(a,b)
p.write_text(s)
p=root/'Scp079FacilityMapNetworkOverlay.java'
s=p.read_text();assert s.count('0x00BDEEFF')==1
s=s.replace('0x00BDEEFF','0x00FFC68A')
p.write_text(s)
