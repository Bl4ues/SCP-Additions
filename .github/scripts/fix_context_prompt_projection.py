from pathlib import Path

path = Path("src/main/java/com/bl4ues/scpinventory/client/ContextPromptClient.java")
text = path.read_text(encoding="utf-8")

old = """        double depth = Math.abs(transformed.z());
        if (depth < 0.05D) return null;
        double fov = minecraft.options.fov().get();
        double scale = screenHeight
                / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));
        int x = (int) Math.round(screenWidth / 2.0D
                - transformed.x() * scale / depth);
"""
new = """        double depth = -transformed.z();
        if (depth <= 0.05D) return null;
        double fov = minecraft.options.fov().get();
        double scale = screenHeight
                / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));
        int x = (int) Math.round(screenWidth / 2.0D
                + transformed.x() * scale / depth);
"""

if old not in text:
    raise SystemExit("Expected broken context projection was not found")

path.write_text(text.replace(old, new, 1), encoding="utf-8")
