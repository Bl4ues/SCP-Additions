from pathlib import Path
base=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client')
p=base/'TransformAlarmClientRenderer.java';s=p.read_text()
old='''            if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) continue;

            CellKey key = new CellKey(group.id(), cell);'''
new='''            if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) continue;
            // The render index is already restricted to Alarm controllers. A
            // visible active fixture also refreshes its audio key, covering
            // missed/late state deltas without scanning an entire facility.
            TransformAlarmAudioClient.groupCellChanged(group.id(), cell, state);

            CellKey key = new CellKey(group.id(), cell);'''
assert s.count(old)==1;s=s.replace(old,new)
old='''        if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) return;

        SurfaceKey key = new SurfaceKey(surface.id(), slot, side, overlay);'''
new='''        if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) return;
        TransformAlarmAudioClient.surfaceChanged(surface.id(), slot,
                overlay ? side : 0, state);

        SurfaceKey key = new SurfaceKey(surface.id(), slot, side, overlay);'''
assert s.count(old)==1;s=s.replace(old,new);p.write_text(s)
p=base/'TransformAlarmAudioClient.java';s=p.read_text()
old='''            AlarmLoop existing = LOOPS.get(key);
            if (existing != null && !existing.isFinished()) {
                existing.cancelPendingFinish();
                continue;
            }
            AlarmLoop loop = new AlarmLoop(level, key);'''
new='''            AlarmLoop existing = LOOPS.get(key);
            if (existing != null && !existing.isFinished()) {
                // SoundEngine may discard a loop while its Java object remains
                // in our address cache (e.g. engine reload or device reset).
                // Never mistake that stale object for audible playback.
                if (level.getGameTime() - existing.startedAtTick > 8L
                        && !minecraft.getSoundManager().isActive(existing)) {
                    existing.finish();
                    LOOPS.remove(key);
                } else {
                    existing.cancelPendingFinish();
                    continue;
                }
            }
            AlarmLoop loop = new AlarmLoop(level, key);'''
assert s.count(old)==1;s=s.replace(old,new)
# Immediate exit when silenced. A new enabled state can now start a new loop.
old='''            if (!active) finishCurrentCycle();
            else cancelPendingFinish();'''
new='''            if (state.getValue(AlarmModule.SILENT)) {
                finish();
                return;
            }
            if (!active) finishCurrentCycle();
            else cancelPendingFinish();'''
assert s.count(old)==1;s=s.replace(old,new);p.write_text(s)
print('Transformed Alarm audio refresh/recovery uses existing synchronized source and silent state.')
