from pathlib import Path
p=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformAlarmPhysicalProjection.java')
s=p.read_text()
def change(old,new):
 global s
 assert s.count(old)==1,(old,s.count(old))
 s=s.replace(old,new)
change('import net.minecraft.world.level.EmptyBlockGetter;', '''import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Blocks;''')
change('''                for (int x = 0; x < 2; x++) {
                    for (int y = 0; y < 2; y++) {
                        double x0 = x * 0.5D, x1 = (x + 1) * 0.5D;
                        double y0 = y * 0.5D, y1 = (y + 1) * 0.5D;''', '''                // Match the payload mesh tessellation exactly. The previous
                // 2x2 receiver was a different polygonal approximation to the
                // same curved wall, so its triangles crossed behind the wall
                // and appeared as short luminous slits under depth testing.
                int horizontalSteps = deform ? 4 : 1;
                int verticalSteps = deform
                        && surface.heightCurveOffset().lengthSqr() > 1.0E-8D
                        ? 2 : 1;
                for (int x = 0; x < horizontalSteps; x++) {
                    for (int y = 0; y < verticalSteps; y++) {
                        double x0 = x / (double) horizontalSteps;
                        double x1 = (x + 1.0D) / horizontalSteps;
                        double y0 = y / (double) verticalSteps;
                        double y1 = (y + 1.0D) / verticalSteps;''')
change('''        if (state == null || state.isAir() || !state.canOcclude()
                || FacilityModule.isDoorPassable(state)) return false;''', '''        if (state == null || state.isAir()
                || FacilityModule.isDoorPassable(state)
                || state.getBlock() instanceof AbstractGlassBlock
                || state.is(Blocks.GLASS_PANE)) return false;
        // Facility walls commonly render as full opaque-looking cubes with
        // noOcclusion for custom textures. canOcclude() would silently reject
        // their physical faces even when they completely fill the cell.''')
change('''    private static final double FACE_OFFSET = 0.0025D;''', '''    private static final double FACE_OFFSET = 0.008D;''')
p.write_text(s)
