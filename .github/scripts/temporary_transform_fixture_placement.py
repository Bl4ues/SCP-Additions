from pathlib import Path

root = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform')
client = root / 'client/TransformGroupPlacementClient.java'
s = client.read_text(encoding='utf-8')
old = '''            BlockState sourceState = group.cells().get(source);
            return sourceState == null || sourceState.isAir()
                    ? source
                    : source.offset(face.getStepX(), face.getStepY(),
                            face.getStepZ());'''
new = '''            BlockState sourceState = group.cells().get(source);
            if (sourceState == null || sourceState.isAir()) return source;
            // The fixture's saved controller cell may sit beside its rendered
            // panel. A click on its visible face must extend the VISIBLE grid
            // cell, never the hidden controller address.
            TransformGroup.GridPos visual =
                    TransformWallFixturePlacement.visualCell(source, sourceState);
            return visual.offset(face.getStepX(), face.getStepY(),
                    face.getStepZ());'''
if s.count(old) != 1:
    raise RuntimeError('Client shifted-fixture placement anchor changed')
client.write_text(s.replace(old, new, 1), encoding='utf-8')

manager = root / 'TransformConstructionManager.java'
s = manager.read_text(encoding='utf-8')
old = '''        BlockState source = group.cells().getOrDefault(sourceCell,
                Blocks.AIR.defaultBlockState());
        GridPos expected = source.isAir() ? sourceCell : sourceCell.offset(
                outwardLocal.getStepX(), outwardLocal.getStepY(),
                outwardLocal.getStepZ());'''
new = '''        BlockState source = group.cells().getOrDefault(sourceCell,
                Blocks.AIR.defaultBlockState());
        // The client sends the hidden controller address as source identity,
        // but adjacency is evaluated at the fixture's rendered local cell.
        GridPos visualSource = source.isAir() ? sourceCell
                : TransformWallFixturePlacement.visualCell(sourceCell, source);
        GridPos expected = source.isAir() ? sourceCell : visualSource.offset(
                outwardLocal.getStepX(), outwardLocal.getStepY(),
                outwardLocal.getStepZ());'''
if s.count(old) != 1:
    raise RuntimeError('Server shifted-fixture placement anchor changed')
s = s.replace(old, new, 1)
old = '''        Vec3 supportOffset = localHit.subtract(sourceCell.x(), sourceCell.y(),
                sourceCell.z());'''
new = '''        Vec3 supportOffset = localHit.subtract(visualSource.x(),
                visualSource.y(), visualSource.z());'''
if s.count(old) != 1:
    raise RuntimeError('Fixture placement offset anchor changed')
s = s.replace(old, new, 1)
manager.write_text(s, encoding='utf-8')
print('PASS: local visible fixture adjacency is identical client/server')
