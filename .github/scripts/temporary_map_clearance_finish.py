from pathlib import Path
r=Path('src/main/java/com/bl4ues/scpclassifieddirective')
p=r/'client/scp079/Scp079FacilityMapScreen.java'
s=p.read_text()
a='''        // Clearance is supplementary information: expose it only once the
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
b='''        // Supplementary clearance stays hidden until the operator zooms in.
        // Once visible it always uses the same maximum size, independent of
        // zoom or the physical door angle, so it cannot bury other markers.
        if (mapZoom < 1.55D) return;
        final int half = 9;
        int segmentColor = locked ? 0xFF89989D : color;
        // Solid badge and door segment share one visual material/palette.
        graphics.fill(x - half, y - half, x + half + 1, y + half + 1,
                segmentColor);
        drawCenteredMapLabel(graphics, value, x, y, 1.85F,
                0xFF07151C);'''
assert s.count(a)==1, 'badge block changed unexpectedly'
s=s.replace(a,b)
a='''        float y = -font.lineHeight * 0.5F + 1.8F;'''
b='''        float y = -font.lineHeight * 0.5F + 2.8F;'''
assert s.count(a)==1
s=s.replace(a,b)
p.write_text(s)
p=r/'client/scp079/Scp079UiTheme.java';s=p.read_text()
a='''    public static final int ACCENT = 0xFFBDEEFF;'''
b='''    public static final int ACCENT = 0xFFBDEEFF;
    /** Active abilities and map activity share this amber highlight. */
    public static final int ACTIVE_SKILL = 0xFFFFC68A;'''
assert s.count(a)==1;s=s.replace(a,b);p.write_text(s)
p=r/'mixin/client/Scp079PlayableVisualsV2CursorMixin.java';s=p.read_text()
a='''speakerActive ? 0xFFFFC68A : Scp079UiTheme.TEXT'''
b='''speakerActive ? Scp079UiTheme.ACTIVE_SKILL : Scp079UiTheme.TEXT'''
assert s.count(a)==1;s=s.replace(a,b);p.write_text(s)
p=r/'client/scp079/Scp079FacilityMapNetworkOverlay.java';s=p.read_text()
a='''        int color = (alpha << 24) | 0x00FFC68A;'''
b='''        int color = (alpha << 24)
                | (Scp079UiTheme.ACTIVE_SKILL & 0x00FFFFFF);'''
assert s.count(a)==1;s=s.replace(a,b);p.write_text(s)
