from pathlib import Path

path = Path("src/main/java/com/bl4ues/scpinventory/client/ContextPromptClient.java")
text = path.read_text(encoding="utf-8")

old_score = """    if (preciseAim && (alongRay < 0.0D || alongRay > reach)) {
        return Double.MAX_VALUE;
    }
"""
new_score = """    if (alongRay <= 0.0D || alongRay > reach) {
        return Double.MAX_VALUE;
    }
"""
if old_score not in text:
    raise SystemExit("Expected contextual target direction check was not found")
text = text.replace(old_score, new_score, 1)

old_projection = """        double depth = -transformed.z();
        if (depth <= 0.05D) return null;
        double fov = minecraft.options.fov().get();
        double scale = screenHeight
                / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));
        int x = (int) Math.round(screenWidth / 2.0D
                + transformed.x() * scale / depth);
"""
new_projection = """        double depth = transformed.z();
        if (depth <= 0.05D) return null;
        double fov = minecraft.options.fov().get();
        double scale = screenHeight
                / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));
        int x = (int) Math.round(screenWidth / 2.0D
                - transformed.x() * scale / depth);
"""
if old_projection not in text:
    raise SystemExit("Expected inverted contextual projection was not found")
text = text.replace(old_projection, new_projection, 1)

path.write_text(text, encoding="utf-8")
