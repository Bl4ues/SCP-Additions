from pathlib import Path
p=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s=p.read_text()
old='''                && (slot.column() == 0 && Math.abs(point.x) < 1.0E-6D
                    || slot.column() == surface.columns() - 1
                        && Math.abs(point.x - 1.0D) < 1.0E-6D)
'''
new='''                && (slot.column() == 0
                    || slot.column() == surface.columns() - 1)
                && (Math.abs(point.x) < 1.0E-6D
                    || Math.abs(point.x - 1.0D) < 1.0E-6D)
'''
assert s.count(old)==1, s.count(old)
p.write_text(s.replace(old,new,1))
print('Shared edges follow the physical surface grid, including flipped planes.')
