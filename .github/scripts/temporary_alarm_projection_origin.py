from pathlib import Path
p=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformAlarmPhysicalProjection.java')
s=p.read_text()
def once(old,new):
 global s
 assert s.count(old)==1,(old[:100],s.count(old))
 s=s.replace(old,new,1)
once('''        Projector projector = new Projector(lamp, right, up,
                alarm.projectionPhase(partialTick));''','''        Projector projector = new Projector(lamp, outward, right, up,
                alarm.projectionPhase(partialTick));''')
once('''        Projector projector = new Projector(lamp, right, vertical,
                alarm.projectionPhase(partialTick));''','''        Projector projector = new Projector(lamp, normal, right, vertical,
                alarm.projectionPhase(partialTick));''')
start=s.index('    private static final class Projector {')
new='''    /** Matches the vanilla Alarm's physical fan instead of spinning a centered
     * texture rectangle. V=0 is the lamp tip; all rotor phases share that
     * world-space anchor. The receiver mesh is still clipped to real walls.
     */
    private static final class Projector {
        private static final double ORIGIN_OUTSET = 0.34D;
        private static final double INNER_RADIUS = 0.06D;
        private static final double OUTER_RADIUS = 3.58D;
        private final Vec3 lamp;
        private final Vec3 rayStart;
        private final Vec3 outward;
        private final Vec3 tangent;
        private final Vec3 fanSide;

        private Projector(Vec3 lamp, Vec3 outward, Vec3 right, Vec3 up,
                double phase) {
            this.lamp = lamp;
            this.outward = outward.normalize();
            this.rayStart = lamp.add(this.outward.scale(ORIGIN_OUTSET));
            double angle = -phase * Math.PI * 2.0D;
            this.tangent = right.scale(Math.sin(angle))
                    .subtract(up.scale(Math.cos(angle))).normalize();
            Vec3 side = this.outward.cross(this.tangent);
            this.fanSide = side.lengthSqr() < 1.0E-8D
                    ? right.normalize() : side.normalize();
        }

        private Sample sample(Vec3 world) {
            // A projected point must be measured where its ray intersects
            // the alarm mounting plane, as in the vanilla physical projector.
            Vec3 ray = world.subtract(rayStart);
            double denominator = ray.dot(outward);
            if (Math.abs(denominator) < 1.0E-8D) {
                return new Sample(world, -100.0F, -100.0F);
            }
            double t = lamp.subtract(rayStart).dot(outward) / denominator;
            if (!Double.isFinite(t) || t <= 0.0D) {
                return new Sample(world, -100.0F, -100.0F);
            }
            Vec3 relative = rayStart.add(ray.scale(t)).subtract(lamp);
            float v = (float) ((relative.dot(tangent) - INNER_RADIUS)
                    / (OUTER_RADIUS - INNER_RADIUS));
            double spread = 0.55D + 0.45D * Math.sqrt(
                    Math.max(0.0D, Math.min(1.0D, v)));
            double halfWidth = HALF_WIDTH * spread;
            float u = (float) ((relative.dot(fanSide) / halfWidth
                    + 1.0D) * 0.5D);
            return new Sample(world, u, v);
        }
    }
}
'''
assert s.endswith('    }\n}\n'), 'Projector was not at the end of the projection class'
s=s[:start]+new
p.write_text(s)
print('Replaced centered UV rotation with the vanilla radial fan anchored at the lamp tip.')
