from pathlib import Path

root = Path('src/main/java/com/bl4ues/scpclassifieddirective')
alarm = root / 'client/AlarmClient.java'
s = alarm.read_text(encoding='utf-8')
old = '''            renderInternal(alarm, partialTick, poseStack, bufferSource,
                    packedLight, packedOverlay, false, cameraInBlock);
            if (alarm.getBlockState().getValue(AlarmModule.ACTIVE)) {
                renderTransformedLocalProjection(alarm, partialTick,
                        poseStack, bufferSource);
            }'''
new = '''            // The transformed host only owns the physical lamp. Its light is
            // now rendered against actual construction receiver faces by
            // TransformAlarmPhysicalProjection, not on a free-floating plane.
            renderInternal(alarm, partialTick, poseStack, bufferSource,
                    packedLight, packedOverlay, false, cameraInBlock);'''
if s.count(old) != 1:
    raise RuntimeError('Transformed Alarm projection invocation anchor mismatch')
s = s.replace(old, new, 1)
start = '''    /**
     * Cheap projection for transformed construction.'''
end = '''    private static void renderProjection(AlarmModule.AlarmBlockEntity alarm,'''
if s.count(start) != 1 or s.count(end) != 1:
    raise RuntimeError('Legacy floating Alarm cone method boundaries mismatch')
i, j = s.index(start), s.index(end)
if j <= i: raise RuntimeError('Legacy projection boundaries reversed')
s = s[:i] + s[j:]
alarm.write_text(s, encoding='utf-8')

renderer = root / 'facility/transform/client/TransformAlarmClientRenderer.java'
s = renderer.read_text(encoding='utf-8')
old = '''            pose.popPose();
        }
    }

    private static void renderSurface(Minecraft minecraft,'''
new = '''            pose.popPose();
            if (state.getValue(AlarmModule.ACTIVE)) {
                TransformAlarmPhysicalProjection.renderGroup(group, cell,
                        alarm, event.getPartialTick(), pose, buffers, camera);
            }
        }
    }

    private static void renderSurface(Minecraft minecraft,'''
if s.count(old) != 1:
    raise RuntimeError('Group Alarm post-render anchor mismatch')
s = s.replace(old, new, 1)
old = '''        pose.popPose();
    }

    private static AlarmModule.AlarmBlockEntity host(Minecraft minecraft,'''
new = '''        pose.popPose();
        if (state.getValue(AlarmModule.ACTIVE)) {
            TransformAlarmPhysicalProjection.renderSurface(surface, slot,
                    normalSign, overlay, alarm, event.getPartialTick(),
                    pose, buffers, camera);
        }
    }

    private static AlarmModule.AlarmBlockEntity host(Minecraft minecraft,'''
if s.count(old) != 1:
    raise RuntimeError('Surface Alarm post-render anchor mismatch')
s = s.replace(old, new, 1)
renderer.write_text(s, encoding='utf-8')
print('Removed unbounded transformed cone and attached face-clipped projection to Group and Surface render passes')
