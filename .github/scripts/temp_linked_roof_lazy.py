from pathlib import Path
p = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s = p.read_text()
old = '''            Vec3[] shell = PARENT_SHELLS.computeIfAbsent(key, ignored -> {
                Vec3[] points = new Vec3[segments + 1];
                for (int i = 0; i <= segments; i++)
                    points[i] = parentShellVertex(parent, edge,
                            i / (double) segments, layer);
                return points;
            });
            a = shell[lower];
            if (amount < 1.0E-9D) return a;
            b = shell[lower + 1];
'''
new = '''            // Allocate only an address table here. A long wall can have
            // thousands of border vertices, but a visible roof may touch just
            // a few of them; preparing all samples synchronously at first
            // contact caused a new selection/geometry stutter.
            Vec3[] shell = PARENT_SHELLS.computeIfAbsent(key,
                    ignored -> new Vec3[segments + 1]);
            a = shell[lower];
            if (a == null) {
                a = parentShellVertex(parent, edge,
                        lower / (double) segments, layer);
                shell[lower] = a;
            }
            if (amount < 1.0E-9D) return a;
            b = shell[lower + 1];
            if (b == null) {
                b = parentShellVertex(parent, edge,
                        (lower + 1.0D) / segments, layer);
                shell[lower + 1] = b;
            }
'''
assert s.count(old) == 1, 'Cannot find eager roof-shell cache'
p.write_text(s.replace(old, new))
print('Lazy, bounded parent shell cache enabled')
