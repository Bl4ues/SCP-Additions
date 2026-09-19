from pathlib import Path
r=Path('src/main/java/com/bl4ues/scpclassifieddirective')
controls=r/'facility/transform/client/TransformConstructionClientControls.java'
s=controls.read_text()
def once(old,new):
    global s
    assert s.count(old)==1, (controls,old[:80],s.count(old))
    s=s.replace(old,new)
once('    private static boolean attackLatch;\n','    private static boolean attackLatch;\n    // Editor preference only. Surface geometry remains server-authoritative.\n    private static boolean independentEdgeHandles;\n\n    public static boolean independentEdgeHandles() {\n        return independentEdgeHandles;\n    }\n\n    private static boolean edgeHandle(SurfaceHandle handle) {\n        return handle == SurfaceHandle.BOTTOM_EDGE\n                || handle == SurfaceHandle.TOP_EDGE\n                || handle == SurfaceHandle.START_EDGE\n                || handle == SurfaceHandle.END_EDGE;\n    }\n')
once('''            case GLFW.GLFW_KEY_F -> {
                finishDrag();
                if (selection.type() == SelectionType.SURFACE) {''','''            case GLFW.GLFW_KEY_H -> {
                if (selection.type() == SelectionType.SURFACE) {
                    finishDrag();
                    independentEdgeHandles = !independentEdgeHandles;
                    status(independentEdgeHandles
                            ? "Edge handles: bend without moving corners"
                            : "Edge handles: move both endpoints");
                }
            }
            case GLFW.GLFW_KEY_F -> {
                finishDrag();
                if (selection.type() == SelectionType.SURFACE) {''')
start=s.index('    private static void applySurfaceDelta(')
prefix,section=s[:start],s[start:]
old='''            case BOTTOM_EDGE -> {
                bs = bs.add(delta);
                be = be.add(delta);
            }
            case TOP_EDGE -> {
                ts = ts.add(delta);
                te = te.add(delta);
            }
            case START_EDGE -> {
                bs = bs.add(delta);
                ts = ts.add(delta);
            }
            case END_EDGE -> {
                be = be.add(delta);
                te = te.add(delta);
            }'''
new='''            case BOTTOM_EDGE -> {
                if (independentEdgeHandles) curve = curve.add(delta.scale(2.0D));
                else {
                    bs = bs.add(delta);
                    be = be.add(delta);
                }
            }
            case TOP_EDGE -> {
                if (independentEdgeHandles) curve = curve.add(delta.scale(2.0D));
                else {
                    ts = ts.add(delta);
                    te = te.add(delta);
                }
            }
            case START_EDGE -> {
                if (independentEdgeHandles) heightCurve = heightCurve.add(delta);
                else {
                    bs = bs.add(delta);
                    ts = ts.add(delta);
                }
            }
            case END_EDGE -> {
                if (independentEdgeHandles) heightCurve = heightCurve.add(delta);
                else {
                    be = be.add(delta);
                    te = te.add(delta);
                }
            }'''
assert section.count(old)==1
section=section.replace(old,new,1)
old='''        if (snap && !Screen.hasControlDown()) {
            switch (handle) {'''
new='''        // In independent mode snapping the endpoints would silently undo the
        // user's choice to bend the edge without moving its four vertices.
        if (snap && !Screen.hasControlDown()
                && !(independentEdgeHandles && edgeHandle(handle))) {
            switch (handle) {'''
assert section.count(old)==1
section=section.replace(old,new,1)
old='''        if (!Screen.hasControlDown() && handle != SurfaceHandle.CENTER) {
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
            };'''
new='''        if (!Screen.hasControlDown() && handle != SurfaceHandle.CENTER
                && !(independentEdgeHandles && edgeHandle(handle))) {
            ConstructionSurface moved = surface.withGeometry(bs, be, ts, te,
                    curve, heightCurve);
            Vec3 handlePoint = handlePosition(moved, handle);'''
assert section.count(old)==1
section=section.replace(old,new,1)
s=prefix+section
once('''            case BOTTOM_EDGE -> surface.bottomStart().add(surface.bottomEnd())
                    .scale(0.5D);
            case TOP_EDGE -> surface.topStart().add(surface.topEnd())
                    .scale(0.5D);
            case START_EDGE -> surface.bottomStart().add(surface.topStart())
                    .scale(0.5D);
            case END_EDGE -> surface.bottomEnd().add(surface.topEnd())
                    .scale(0.5D);
            case CENTER -> surface.gridPoint(0.5D, 0.5D);''','''            // Handles track the actual parametric edge, not its chord. This
            // matters when joining sharply curved walls and ceiling surfaces.
            case BOTTOM_EDGE -> surface.point(0.5D, 0.0D);
            case TOP_EDGE -> surface.point(0.5D, 1.0D);
            case START_EDGE -> surface.point(0.0D, 0.5D);
            case END_EDGE -> surface.point(1.0D, 0.5D);
            case CENTER -> surface.point(0.5D, 0.5D);''')
controls.write_text(s)
hud=r/'client/BuilderToolGuideHud.java';s=hud.read_text()
old='''import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState;'''
new='''import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientControls;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState;'''
assert s.count(old)==1;s=s.replace(old,new)
old='''                lines.add(new Line("F", "flip wall side"));'''
new='''                lines.add(new Line("H", TransformConstructionClientControls
                        .independentEdgeHandles()
                        ? "edge mode: bend (fixed corners)"
                        : "edge mode: move endpoints"));
                lines.add(new Line("Ctrl", "hold to disable auto-snap"));
                lines.add(new Line("F", "flip wall side"));'''
assert s.count(old)==1;s=s.replace(old,new)
hud.write_text(s)
print('Applied independent Surface edge handles and physical curved control anchors.')
