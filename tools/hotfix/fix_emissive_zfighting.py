from pathlib import Path

root = Path(__file__).resolve().parents[2]


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


model_path = root / "src/main/java/net/mcreator/scpadditions/client/render/NativeEmissiveModelEvents.java"
model = model_path.read_text(encoding="utf-8")
model = replace_once(
    model,
    '''            if (renderType == BLOCK_OVERLAY_TYPE) {
                List<BakedQuad> source = originalModel.getQuads(state, side,
                        random, modelData, null);
                List<BakedQuad> emissive = overlays.forQuads(source);
''',
    '''            if (renderType == BLOCK_OVERLAY_TYPE) {
                // Shader bloom uses a separate eyes/spidereyes pass. Keeping
                // this coplanar full-bright overlay at the same time causes
                // depth flicker on bright masks, so the chunk pass supplies
                // only the model's original cutout geometry while shaders run.
                if (NativeEmissiveShaderBloomRenderer.isShaderPackInUse()) {
                    return originalModel.getQuads(state, side, random,
                            modelData, renderType);
                }

                List<BakedQuad> source = originalModel.getQuads(state, side,
                        random, modelData, null);
                List<BakedQuad> emissive = overlays.forQuads(source);
''',
    "block overlay shader guard",
)
model = replace_once(
    model,
    '''            ChunkRenderTypeSet original = originalModel.getRenderTypes(state,
                    random, modelData);
            if (!hasBlockEmission(state, modelData)) {
                return original;
            }
''',
    '''            ChunkRenderTypeSet original = originalModel.getRenderTypes(state,
                    random, modelData);
            if (NativeEmissiveShaderBloomRenderer.isShaderPackInUse()
                    || !hasBlockEmission(state, modelData)) {
                return original;
            }
''',
    "block overlay render type guard",
)
model_path.write_text(model, encoding="utf-8")

renderer_path = root / "src/main/java/net/mcreator/scpadditions/client/render/NativeEmissiveShaderBloomRenderer.java"
renderer = renderer_path.read_text(encoding="utf-8")
renderer = replace_once(
    renderer,
    '''    @Nullable
    private static ClientLevel cachedLevel;
    private static int rescanCursor;
''',
    '''    @Nullable
    private static ClientLevel cachedLevel;
    private static int rescanCursor;
    private static boolean previousShaderPackState;
    private static boolean shaderStateInitialized;
''',
    "shader state fields",
)
renderer = replace_once(
    renderer,
    '''        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            clearLevelCache();
            return;
        }

        ensureLevel(level);
''',
    '''        Minecraft minecraft = Minecraft.getInstance();
        boolean shaderPackState = isShaderPackInUse();
        if (!shaderStateInitialized) {
            previousShaderPackState = shaderPackState;
            shaderStateInitialized = true;
        } else if (previousShaderPackState != shaderPackState) {
            previousShaderPackState = shaderPackState;
            // Chunk meshes contain the native full-bright overlay only while
            // shaders are disabled. Rebuild once when the active shader state
            // changes so stale coplanar geometry cannot survive a mode switch.
            if (minecraft.level != null) {
                minecraft.levelRenderer.allChanged();
            }
        }

        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            clearLevelCache();
            return;
        }

        ensureLevel(level);
''',
    "shader state transition handling",
)
renderer = replace_once(
    renderer,
    '''    /** Optional Iris/Oculus bridge kept reflection-only for a clean runtime. */
    private static final class ShaderBridge {
''',
    '''    /** Shared shader-state query used by the baked-model fallback pass. */
    static boolean isShaderPackInUse() {
        return ShaderBridge.isShaderPackInUse();
    }

    /** Optional Iris/Oculus bridge kept reflection-only for a clean runtime. */
    private static final class ShaderBridge {
''',
    "shared shader state query",
)
renderer_path.write_text(renderer, encoding="utf-8")

for temporary in (
    root / "tools/hotfix/APPLY_EMISSIVE_ZFIGHT_FIX",
    root / "tools/hotfix/fix_emissive_zfighting.py",
):
    if temporary.exists():
        temporary.unlink()
