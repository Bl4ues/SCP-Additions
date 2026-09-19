from pathlib import Path
root=Path('src/main/java/com/bl4ues/scpclassifieddirective')
p=root/'facility/transform/client/TransformAlarmClientRenderer.java'
s=p.read_text(encoding='utf-8')
a='import com.bl4ues.scpclassifieddirective.client.AlarmClient;'
assert s.count(a)==1
s=s.replace(a,a+'\nimport com.bl4ues.scpclassifieddirective.client.TransformedAlarmAudioClient;')
a='''            if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) continue;

            CellKey key = new CellKey(group.id(), cell);'''
b='''            if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) continue;
            TransformedAlarmAudioClient.group(
                    (net.minecraft.client.multiplayer.ClientLevel) minecraft.level,
                    group.id(), cell.x(), cell.y(), cell.z(), center,
                    state.getValue(AlarmModule.ACTIVE));

            CellKey key = new CellKey(group.id(), cell);'''
assert s.count(a)==1
s=s.replace(a,b)
a='''        if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) return;

        SurfaceKey key = new SurfaceKey(surface.id(), slot, side, overlay);'''
b='''        if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) return;
        TransformedAlarmAudioClient.surface(
                (net.minecraft.client.multiplayer.ClientLevel) minecraft.level,
                surface.id(), slot.column(), slot.row(), side, overlay,
                center, state.getValue(AlarmModule.ACTIVE));

        SurfaceKey key = new SurfaceKey(surface.id(), slot, side, overlay);'''
assert s.count(a)==1
s=s.replace(a,b)
p.write_text(s,encoding='utf-8')

p=root/'facility/transform/client/TransformAlarmPhysicalProjection.java'
s=p.read_text(encoding='utf-8')
a='''    private static final RenderType WASH = RenderType.entityTranslucentEmissive(
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                    "textures/effect/alarm_light_emissive.png"));'''
b='''    // The broad, feathered wash is the visible cone. Emissive is only a
    // secondary, low-alpha pass, matching the vanilla Alarm projector.
    private static final RenderType WASH = RenderType.entityTranslucent(
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                    "textures/effect/alarm_light_splash.png"), true);
    private static final RenderType GLOW = RenderType.entityTranslucentEmissive(
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                    "textures/effect/alarm_light_emissive.png"));'''
assert s.count(a)==1
s=s.replace(a,b)
s=s.replace('private static final double HALF_WIDTH = 0.78D;',
            'private static final double HALF_WIDTH = 1.72D;')
s=s.replace('private static final double HALF_HEIGHT = 1.55D;',
            'private static final double HALF_HEIGHT = 3.58D;')
a='''        VertexConsumer consumer = buffers.getBuffer(WASH);
        boolean emitted = false;
        for (int horizontal = -2; horizontal <= 2; horizontal++) {
            for (int vertical = -2; vertical <= 2; vertical++) {'''
b='''        for (int pass = 0; pass < 2; pass++) {
            RenderType layer = pass == 0 ? WASH : GLOW;
            VertexConsumer consumer = buffers.getBuffer(layer);
            boolean emitted = false;
            for (int horizontal = -3; horizontal <= 3; horizontal++) {
                for (int vertical = -4; vertical <= 4; vertical++) {'''
assert s.count(a)==1
s=s.replace(a,b)
a='''        VertexConsumer consumer = buffers.getBuffer(WASH);
        boolean emitted = false;
        for (int dc = -2; dc <= 2; dc++) {
            for (int dr = -2; dr <= 2; dr++) {'''
b='''        for (int pass = 0; pass < 2; pass++) {
            RenderType layer = pass == 0 ? WASH : GLOW;
            VertexConsumer consumer = buffers.getBuffer(layer);
            boolean emitted = false;
            for (int dc = -3; dc <= 3; dc++) {
                for (int dr = -4; dr <= 4; dr++) {'''
assert s.count(a)==1
s=s.replace(a,b)
a='''        if (emitted) buffers.endBatch(WASH);
        pose.popPose();'''
b='''            if (emitted) buffers.endBatch(layer);
        }
        pose.popPose();'''
assert s.count(a)==2,s.count(a)
s=s.replace(a,b)
a='emitted |= emit(consumer, pose, projector, outward, a, b, c, d);'
b='emitted |= emit(consumer, pose, projector, outward, a, b, c, d,\n                        pass == 0 ? 255 : 87);'
assert s.count(a)==1
s=s.replace(a,b)
a='''                        emitted |= emit(consumer, pose, projector, normal,
                                a, b, c, d);'''
b='''                        emitted |= emit(consumer, pose, projector, normal,
                                a, b, c, d, pass == 0 ? 255 : 87);'''
assert s.count(a)==1
s=s.replace(a,b)
a='''            Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
        // Match the vertex winding'''
b='''            Vec3 a, Vec3 b, Vec3 c, Vec3 d, int alpha) {
        // Match the vertex winding'''
assert s.count(a)==1
s=s.replace(a,b)
a='''            vertex(output, pose, first, normal);
            vertex(output, pose, previous, normal);
            vertex(output, pose, next, normal);
            vertex(output, pose, next, normal);'''
b='''            vertex(output, pose, first, normal, alpha);
            vertex(output, pose, previous, normal, alpha);
            vertex(output, pose, next, normal, alpha);
            vertex(output, pose, next, normal, alpha);'''
assert s.count(a)==1
s=s.replace(a,b)
a='''            Sample sample, Vec3 normal) {'''
b='''            Sample sample, Vec3 normal, int alpha) {'''
assert s.count(a)==1
s=s.replace(a,b)
a='''.color(255, 255, 255, 87)'''
b='''.color(255, 255, 255, alpha)'''
assert s.count(a)==1
s=s.replace(a,b)
p.write_text(s,encoding='utf-8')
