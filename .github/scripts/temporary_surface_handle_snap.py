from pathlib import Path
r=Path('src/main/java/com/bl4ues/scpclassifieddirective')
def patch(name,old,new):
 p=r/name;s=p.read_text();assert s.count(old)==1,(name,old,s.count(old));p.write_text(s.replace(old,new))

name='facility/transform/client/TransformConstructionClientControls.java'
patch(name,'''        if (snap) {
            switch (handle) {''','''        if (snap && !Screen.hasControlDown()) {
            switch (handle) {''')
patch(name,'''        ConstructionSurface next = surface.withGeometry(bs, be, ts, te, curve,
                heightCurve);''', '''        // Nearby real blocks and other Surface boundaries are preferred even
        // without Shift; Ctrl opts out of all snapping for precision editing.
        // Compute the target from the already-moved handle, not from the
        // original plane or a vanilla proxy, so a curved join remains exact.
        if (!Screen.hasControlDown() && handle != SurfaceHandle.CENTER) {
            Vec3 handlePoint = switch (handle) {
                case BOTTOM_START -> bs;
                case BOTTOM_END -> be;
                case TOP_START -> ts;
                case TOP_END -> te;
                case BOTTOM_EDGE -> bs.add(be).scale(0.5D);
                case TOP_EDGE -> ts.add(te).scale(0.5D);
                case START_EDGE -> bs.add(ts).scale(0.5D);
                case END_EDGE -> be.add(te).scale(0.5D);
                case CENTER -> Vec3.ZERO;
            };
            Vec3 adjustment = TransformSurfaceSnapClient.snap(handlePoint,
                    surface.id()).subtract(handlePoint);
            if (adjustment.lengthSqr() > 1.0E-10D) {
                switch (handle) {
                    case BOTTOM_START -> bs = bs.add(adjustment);
                    case BOTTOM_END -> be = be.add(adjustment);
                    case TOP_START -> ts = ts.add(adjustment);
                    case TOP_END -> te = te.add(adjustment);
                    case BOTTOM_EDGE -> {
                        bs = bs.add(adjustment);
                        be = be.add(adjustment);
                    }
                    case TOP_EDGE -> {
                        ts = ts.add(adjustment);
                        te = te.add(adjustment);
                    }
                    case START_EDGE -> {
                        bs = bs.add(adjustment);
                        ts = ts.add(adjustment);
                    }
                    case END_EDGE -> {
                        be = be.add(adjustment);
                        te = te.add(adjustment);
                    }
                    case CENTER -> { }
                }
            }
        }

        ConstructionSurface next = surface.withGeometry(bs, be, ts, te, curve,
                heightCurve);''')

name='facility/transform/client/TransformAlarmClientRenderer.java'
patch(name,'import com.bl4ues.scpclassifieddirective.client.TransformedAlarmAudioClient;\n','')
patch(name,'''            TransformedAlarmAudioClient.group(
                    (net.minecraft.client.multiplayer.ClientLevel) minecraft.level,
                    group.id(), cell.x(), cell.y(), cell.z(), center,
                    state.getValue(AlarmModule.ACTIVE));
''','')
patch(name,'''        TransformedAlarmAudioClient.surface(
                (net.minecraft.client.multiplayer.ClientLevel) minecraft.level,
                surface.id(), slot.column(), slot.row(), side, overlay,
                center, state.getValue(AlarmModule.ACTIVE));
''','')
