from __future__ import annotations

import json
import re
from pathlib import Path


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    Path(path).write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one occurrence, found {count}")
    return text.replace(old, new, 1)


def replace_count(text: str, old: str, new: str, expected: int, label: str) -> str:
    count = text.count(old)
    if count != expected:
        raise SystemExit(f"{label}: expected {expected} occurrences, found {count}")
    return text.replace(old, new)


# ContextConfigOpenPacket ----------------------------------------------------
path = "src/main/java/com/bl4ues/scpinventory/network/ContextConfigOpenPacket.java"
text = read(path)
text = replace_once(text,
    "    private final boolean allowRightClick;\n    private final String useItem;",
    "    private final boolean allowRightClick;\n    private final boolean allowOffscreen;\n    private final boolean likelyRightClick;\n    private final String useItem;",
    "open packet fields")
text = replace_once(text,
    "boolean allowE, boolean allowRightClick, String useItem, String clickFace, String rotateWith,",
    "boolean allowE, boolean allowRightClick, boolean allowOffscreen, boolean likelyRightClick,\n                                   String useItem, String clickFace, String rotateWith,",
    "open packet constructor signature")
text = replace_once(text,
    "        this.allowRightClick = allowRightClick;\n        this.useItem = useItem == null ? \"hand\" : useItem;",
    "        this.allowRightClick = allowRightClick;\n        this.allowOffscreen = allowOffscreen;\n        this.likelyRightClick = likelyRightClick;\n        this.useItem = useItem == null ? \"hand\" : useItem;",
    "open packet assignments")
text = replace_once(text,
    "        buf.writeBoolean(msg.allowRightClick);\n        buf.writeUtf(msg.useItem);",
    "        buf.writeBoolean(msg.allowRightClick);\n        buf.writeBoolean(msg.allowOffscreen);\n        buf.writeBoolean(msg.likelyRightClick);\n        buf.writeUtf(msg.useItem);",
    "open packet encode")
text = replace_once(text,
    "                buf.readBoolean(),\n                buf.readBoolean(),\n                buf.readUtf(),",
    "                buf.readBoolean(),\n                buf.readBoolean(),\n                buf.readBoolean(),\n                buf.readBoolean(),\n                buf.readUtf(),",
    "open packet decode")
text = replace_once(text,
    "    public boolean allowRightClick() {\n        return allowRightClick;\n    }\n\n    public String useItem() {",
    "    public boolean allowRightClick() {\n        return allowRightClick;\n    }\n\n    public boolean allowOffscreen() {\n        return allowOffscreen;\n    }\n\n    public boolean likelyRightClick() {\n        return likelyRightClick;\n    }\n\n    public String useItem() {",
    "open packet getters")
write(path, text)


# ContextConfigSavePacket ----------------------------------------------------
path = "src/main/java/com/bl4ues/scpinventory/network/ContextConfigSavePacket.java"
text = read(path)
text = replace_once(text,
    "    private final boolean allowRightClick;\n    private final String useItem;",
    "    private final boolean allowRightClick;\n    private final boolean allowOffscreen;\n    private final String useItem;",
    "save packet field")
text = replace_once(text,
    "boolean allowE, boolean allowRightClick, String useItem, String clickFace, String rotateWith,",
    "boolean allowE, boolean allowRightClick, boolean allowOffscreen, String useItem,\n                                   String clickFace, String rotateWith,",
    "save packet constructor signature")
text = replace_once(text,
    "        this.allowRightClick = allowRightClick;\n        this.useItem = useItem == null ? \"hand\" : useItem;",
    "        this.allowRightClick = allowRightClick;\n        this.allowOffscreen = allowOffscreen;\n        this.useItem = useItem == null ? \"hand\" : useItem;",
    "save packet assignment")
text = replace_once(text,
    "        buf.writeBoolean(msg.allowRightClick);\n        buf.writeUtf(msg.useItem);",
    "        buf.writeBoolean(msg.allowRightClick);\n        buf.writeBoolean(msg.allowOffscreen);\n        buf.writeUtf(msg.useItem);",
    "save packet encode")
text = replace_once(text,
    "                buf.readBoolean(),\n                buf.readBoolean(),\n                buf.readUtf(),",
    "                buf.readBoolean(),\n                buf.readBoolean(),\n                buf.readBoolean(),\n                buf.readUtf(),",
    "save packet decode")
text = replace_once(text,
    "                    msg.allowE, msg.allowRightClick, msg.useItem, msg.clickFace, msg.rotateWith,",
    "                    msg.allowE, msg.allowRightClick, msg.allowOffscreen, msg.useItem,\n                    msg.clickFace, msg.rotateWith,",
    "save packet entity dispatch")
text = replace_once(text,
    "                    msg.allowE, msg.allowRightClick, msg.useItem, msg.clickFace, msg.rotateWith,",
    "                    msg.allowE, msg.allowRightClick, msg.allowOffscreen, msg.useItem,\n                    msg.clickFace, msg.rotateWith,",
    "save packet block dispatch")
write(path, text)


# ContextConfigScreen --------------------------------------------------------
path = "src/main/java/com/bl4ues/scpinventory/client/gui/ContextConfigScreen.java"
text = read(path)
text = replace_once(text, "    private static final int PANEL_H = 366;", "    private static final int PANEL_H = 420;", "screen height")
text = replace_once(text,
    "    private boolean allowRightClick;\n    private String useItem;",
    "    private boolean allowRightClick;\n    private boolean allowOffscreen;\n    private final boolean likelyRightClick;\n    private String useItem;",
    "screen fields")
text = replace_once(text,
    "        this.allowRightClick = packet.allowRightClick();\n        this.useItem = \"card\".equalsIgnoreCase(packet.useItem()) ? \"card\" : \"hand\";",
    "        this.allowRightClick = packet.allowRightClick();\n        this.allowOffscreen = packet.allowOffscreen();\n        this.likelyRightClick = packet.likelyRightClick();\n        this.useItem = \"card\".equalsIgnoreCase(packet.useItem()) ? \"card\" : \"hand\";",
    "screen constructor")
needle = '''        addRenderableWidget(Button.builder(Component.literal("Rotate: " + rotateWith), b -> {
            rotateWith = next(rotateWith, new String[]{"none", "auto", "facing", "horizontal_facing", "axis"});
            b.setMessage(Component.literal("Rotate: " + rotateWith));
        }).bounds(fieldX + 128, y, 118, 18).build());

        int bottomY = top + PANEL_H - 30;'''
replacement = '''        addRenderableWidget(Button.builder(Component.literal("Rotate: " + rotateWith), b -> {
            rotateWith = next(rotateWith, new String[]{"none", "auto", "facing", "horizontal_facing", "axis"});
            b.setMessage(Component.literal("Rotate: " + rotateWith));
        }).bounds(fieldX + 128, y, 118, 18).build());

        y += 28;
        addRenderableWidget(Button.builder(Component.literal(offscreenText()), b -> {
            allowOffscreen = !allowOffscreen;
            b.setMessage(Component.literal(offscreenText()));
        }).bounds(fieldX, y, fieldW, 18).build());

        int bottomY = top + PANEL_H - 30;'''
text = replace_once(text, needle, replacement, "screen offscreen button")
old_render = '''        g.drawString(font, "Anchor local", left + 12, top + 242, 0xFFE6E6B0, false);
        g.drawString(font, "X " + fmt(anchorX) + "  Y " + fmt(anchorY) + "  Z " + fmt(anchorZ), left + 12, top + 254, 0xFFE6E6B0, false);
        g.drawString(font, "Arrows X/Y, PgUp/PgDn Z, wheel Y", left + 12, top + 270, 0xFF999999, false);
        g.drawString(font, "Shift=0.10  Ctrl=0.01  normal=0.05", left + 12, top + 282, 0xFF999999, false);
        g.drawString(font, "Rotate mode now previews final in-world anchor.", left + 12, top + 296, 0xFF88DDEE, false);
        g.drawString(font, "Forget asks twice, then removes this rule.", left + 12, top + 310, 0xFFFF9D9D, false);'''
new_render = '''        g.drawString(font, "Off-screen prompts appear at the edge behind you.", left + 12, top + 242, 0xFF88DDEE, false);
        g.drawString(font, "Anchor local", left + 12, top + 270, 0xFFE6E6B0, false);
        g.drawString(font, "X " + fmt(anchorX) + "  Y " + fmt(anchorY) + "  Z " + fmt(anchorZ), left + 12, top + 282, 0xFFE6E6B0, false);
        g.drawString(font, "Arrows X/Y, PgUp/PgDn Z, wheel Y", left + 12, top + 298, 0xFF999999, false);
        g.drawString(font, "Shift=0.10  Ctrl=0.01  normal=0.05", left + 12, top + 310, 0xFF999999, false);
        g.drawString(font, "Rotate mode previews the final in-world anchor.", left + 12, top + 324, 0xFF88DDEE, false);
        if (!likelyRightClick) {
            g.drawString(font, "Warning: no right-click interaction was detected.", left + 12, top + 340, 0xFFFFB35C, false);
            g.drawString(font, "You can still save if this detection is wrong.", left + 12, top + 352, 0xFFFFB35C, false);
        }
        g.drawString(font, "Forget asks twice, then removes this rule.", left + 12, top + 370, 0xFFFF9D9D, false);'''
text = replace_once(text, old_render, new_render, "screen explanatory text")
text = replace_once(text,
    "new ContextConfigSavePacket(pos, blockId, actionBox.getValue(), nameBox.getValue(), showName, parseRange(), allowE, allowRightClick, useItem, clickFace, rotateWith, anchorX, anchorY, anchorZ)",
    "new ContextConfigSavePacket(pos, blockId, actionBox.getValue(), nameBox.getValue(), showName, parseRange(), allowE, allowRightClick, allowOffscreen, useItem, clickFace, rotateWith, anchorX, anchorY, anchorZ)",
    "screen save packet")
text = replace_once(text,
    "    private String itemText() {\n        return \"card\".equals(useItem) ? \"Item: Card\" : \"Item: Hand\";\n    }",
    "    private String offscreenText() {\n        return allowOffscreen ? \"Off-screen prompts: On\"\n                : \"Off-screen prompts: Off\";\n    }\n\n    private String itemText() {\n        return \"card\".equals(useItem) ? \"Item: Card\" : \"Item: Hand\";\n    }",
    "screen offscreen label")
write(path, text)


# ContextConfigManager -------------------------------------------------------
path = "src/main/java/com/bl4ues/scpinventory/context/ContextConfigManager.java"
text = read(path)
text = replace_once(text,
    "import net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.world.level.ClipContext;",
    "import net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.world.InteractionHand;\nimport net.minecraft.world.entity.player.Player;\nimport net.minecraft.world.level.ClipContext;\nimport net.minecraft.world.level.Level;\nimport net.minecraft.world.level.block.Block;",
    "manager interaction imports")
text = replace_once(text,
    "boolean showName, double range, boolean allowE, boolean allowRightClick, String useItem,",
    "boolean showName, double range, boolean allowE, boolean allowRightClick, boolean allowOffscreen, String useItem,",
    "manager save signature")
text = replace_once(text,
    "        input.addProperty(\"allowRightClick\", allowRightClick);\n\n        JsonObject click = object(rule, \"click\");",
    "        input.addProperty(\"allowRightClick\", allowRightClick);\n\n        object(rule, \"visual\").addProperty(\"allowOffscreen\", allowOffscreen);\n\n        JsonObject click = object(rule, \"click\");",
    "manager save visual")
text = replace_once(text,
    "        JsonObject click = object(rule, \"click\");\n        JsonObject anchor = object(rule, \"anchor\");",
    "        JsonObject click = object(rule, \"click\");\n        JsonObject anchor = object(rule, \"anchor\");\n        JsonObject visual = object(rule, \"visual\");",
    "manager packet visual")
text = replace_once(text,
    "                bool(input, \"allowRightClick\", bool(rule, \"allowRightClick\", true)),\n                cleanUseItem(string(rule, \"useItem\", \"hand\")),",
    "                bool(input, \"allowRightClick\", bool(rule, \"allowRightClick\", true)),\n                bool(visual, \"allowOffscreen\", bool(rule, \"allowOffscreen\", false)),\n                likelySupportsRightClick(state),\n                cleanUseItem(string(rule, \"useItem\", \"hand\")),",
    "manager packet booleans")
text = replace_once(text,
    "        JsonObject click = new JsonObject();\n        click.addProperty(\"face\", \"front\");\n        rule.add(\"click\", click);\n        return rule;",
    "        JsonObject click = new JsonObject();\n        click.addProperty(\"face\", \"front\");\n        rule.add(\"click\", click);\n\n        JsonObject visual = new JsonObject();\n        visual.addProperty(\"allowOffscreen\", false);\n        rule.add(\"visual\", visual);\n        return rule;",
    "manager default visual")
detector = '''
    private static boolean likelySupportsRightClick(BlockState state) {
        if (state == null || state.isAir()) return false;
        try {
            return state.getBlock().getClass().getMethod("use",
                    BlockState.class, Level.class, BlockPos.class,
                    Player.class, InteractionHand.class,
                    BlockHitResult.class).getDeclaringClass() != Block.class;
        } catch (ReflectiveOperationException ignored) {
            // Avoid blocking valid custom blocks when another mod changes the
            // implementation shape in a way reflection cannot inspect.
            return true;
        }
    }

'''
text = replace_once(text,
    "    private static void setSession(ServerPlayer player, BlockPos pos, ResourceLocation id) {",
    detector + "    private static void setSession(ServerPlayer player, BlockPos pos, ResourceLocation id) {",
    "manager detector")
write(path, text)


# ContextConfigSaveService ---------------------------------------------------
path = "src/main/java/com/bl4ues/scpinventory/context/ContextConfigSaveService.java"
text = read(path)
text = replace_once(text,
    "            boolean allowRightClick,\n            String useItem,",
    "            boolean allowRightClick,\n            boolean allowOffscreen,\n            String useItem,",
    "save service signature")
text = replace_once(text,
    "            input.addProperty(\"allowRightClick\", allowRightClick);\n\n            object(rule, \"click\").addProperty",
    "            input.addProperty(\"allowRightClick\", allowRightClick);\n\n            object(rule, \"visual\").addProperty(\"allowOffscreen\",\n                    allowOffscreen);\n\n            object(rule, \"click\").addProperty",
    "save service visual")
text = replace_once(text,
    "        JsonObject click = new JsonObject();\n        click.addProperty(\"face\", \"front\");\n        rule.add(\"click\", click);\n\n        JsonObject anchor = new JsonObject();",
    "        JsonObject click = new JsonObject();\n        click.addProperty(\"face\", \"front\");\n        rule.add(\"click\", click);\n\n        JsonObject visual = new JsonObject();\n        visual.addProperty(\"allowOffscreen\", false);\n        rule.add(\"visual\", visual);\n\n        JsonObject anchor = new JsonObject();",
    "save service default visual")
write(path, text)


# ContextEntityConfigManager ------------------------------------------------
path = "src/main/java/com/bl4ues/scpinventory/context/ContextEntityConfigManager.java"
text = read(path)
text = replace_once(text,
    "boolean showName, double range, boolean allowE, boolean allowRightClick, String useItem,",
    "boolean showName, double range, boolean allowE, boolean allowRightClick, boolean allowOffscreen, String useItem,",
    "entity save signature")
text = replace_once(text,
    "        input.addProperty(\"allowRightClick\", allowRightClick);\n\n        JsonObject click = object(rule, \"click\");",
    "        input.addProperty(\"allowRightClick\", allowRightClick);\n\n        object(rule, \"visual\").addProperty(\"allowOffscreen\", allowOffscreen);\n\n        JsonObject click = object(rule, \"click\");",
    "entity save visual")
text = replace_once(text,
    "        JsonObject click = object(rule, \"click\");\n        JsonObject anchor = object(rule, \"anchor\");",
    "        JsonObject click = object(rule, \"click\");\n        JsonObject anchor = object(rule, \"anchor\");\n        JsonObject visual = object(rule, \"visual\");",
    "entity packet visual")
text = replace_once(text,
    "                bool(input, \"allowRightClick\", bool(rule, \"allowRightClick\", true)),\n                cleanUseItem(string(rule, \"useItem\", \"hand\")),",
    "                bool(input, \"allowRightClick\", bool(rule, \"allowRightClick\", true)),\n                bool(visual, \"allowOffscreen\", bool(rule, \"allowOffscreen\", false)),\n                true,\n                cleanUseItem(string(rule, \"useItem\", \"hand\")),",
    "entity packet booleans")
text = replace_once(text,
    "        JsonObject click = new JsonObject();\n        click.addProperty(\"face\", \"front\");\n        rule.add(\"click\", click);\n        return rule;",
    "        JsonObject click = new JsonObject();\n        click.addProperty(\"face\", \"front\");\n        rule.add(\"click\", click);\n\n        JsonObject visual = new JsonObject();\n        visual.addProperty(\"allowOffscreen\", false);\n        rule.add(\"visual\", visual);\n        return rule;",
    "entity default visual")
write(path, text)


# ContextInteractionRegistry ------------------------------------------------
path = "src/main/java/com/bl4ues/scpinventory/context/ContextInteractionRegistry.java"
text = read(path)
text = replace_count(text, '"front", "hand", "hand", 0.38D));', '"front", "hand", "hand", 0.38D, false));', 4, "built-in elevator offscreen defaults")
text = replace_once(text,
    "        double promptScale = getDouble(visual, \"scale\",\n                getDouble(object, \"promptScale\", 1.0D));",
    "        double promptScale = getDouble(visual, \"scale\",\n                getDouble(object, \"promptScale\", 1.0D));\n        boolean allowOffscreen = getBoolean(visual, \"allowOffscreen\",\n                getBoolean(object, \"allowOffscreen\", false));",
    "registry parse offscreen")
text = replace_once(text,
    "                allowE, allowRightClick, clickFace, useItem, icon,\n                promptScale);",
    "                allowE, allowRightClick, clickFace, useItem, icon,\n                promptScale, allowOffscreen);",
    "registry parsed constructor")
text = replace_once(text,
    "        private final double promptScale;",
    "        private final double promptScale;\n        private final boolean allowOffscreen;",
    "registry field")
text = replace_once(text,
    "                boolean allowE, boolean allowRightClick, String clickFace,\n                String useItem, String icon, double promptScale) {",
    "                boolean allowE, boolean allowRightClick, String clickFace,\n                String useItem, String icon, double promptScale,\n                boolean allowOffscreen) {",
    "registry constructor signature")
text = replace_once(text,
    "            this.promptScale = Math.max(0.35D,\n                    Math.min(1.5D, promptScale));",
    "            this.promptScale = Math.max(0.35D,\n                    Math.min(1.5D, promptScale));\n            this.allowOffscreen = allowOffscreen;",
    "registry assignment")
text = replace_once(text,
    "        public double promptScale() { return promptScale; }\n        public boolean requiresPreciseAim() {",
    "        public double promptScale() { return promptScale; }\n        public boolean allowOffscreen() { return allowOffscreen; }\n        public boolean requiresPreciseAim() {",
    "registry getter")
write(path, text)


# ContextPromptClient --------------------------------------------------------
path = "src/main/java/com/bl4ues/scpinventory/client/ContextPromptClient.java"
text = read(path)
text = replace_once(text,
    "        ScreenPoint point = projectToScreen(minecraft, target.anchor(),\n                screenWidth, screenHeight);",
    "        ScreenPoint point = projectToScreen(minecraft, target.anchor(),\n                screenWidth, screenHeight, target.allowOffscreen());",
    "prompt render projection")
text = replace_count(text,
    "                        preciseAimRadiusSqr(rule.interactionKey()));",
    "                        preciseAimRadiusSqr(rule.interactionKey()),\n                        rule.allowOffscreen());",
    2, "prompt score calls")
text = replace_once(text,
    "            boolean preciseAim, double preciseAimRadiusSqr) {",
    "            boolean preciseAim, double preciseAimRadiusSqr,\n            boolean allowOffscreen) {",
    "prompt score signature")
text = replace_once(text,
    "    if (alongRay <= 0.0D || alongRay > reach) {",
    "    if ((!allowOffscreen && alongRay <= 0.0D) || alongRay > reach) {",
    "prompt behind selection")
text = replace_count(text,
    "                            (float) rule.promptScale(), score);",
    "                            (float) rule.promptScale(),\n                            rule.allowOffscreen(), score);",
    2, "prompt target offscreen field")
old_projection = '''    private static ScreenPoint projectToScreen(Minecraft minecraft,
            Vec3 worldPos, int screenWidth, int screenHeight) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 relative = worldPos.subtract(camera.getPosition());
        Quaternionf rotation = new Quaternionf(camera.rotation());
        rotation.conjugate();
        Vector3f transformed = new Vector3f((float) relative.x,
                (float) relative.y, (float) relative.z);
        transformed.rotate(rotation);
        double depth = transformed.z();
        if (depth <= 0.05D) return null;
        double fov = minecraft.options.fov().get();
        double scale = screenHeight
                / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));
        int x = (int) Math.round(screenWidth / 2.0D
                - transformed.x() * scale / depth);
        int y = (int) Math.round(screenHeight / 2.0D
                - transformed.y() * scale / depth);
        return new ScreenPoint(x, y);
    }'''
new_projection = '''    private static ScreenPoint projectToScreen(Minecraft minecraft,
            Vec3 worldPos, int screenWidth, int screenHeight,
            boolean allowOffscreen) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 relative = worldPos.subtract(camera.getPosition());
        Quaternionf rotation = new Quaternionf(camera.rotation());
        rotation.conjugate();
        Vector3f transformed = new Vector3f((float) relative.x,
                (float) relative.y, (float) relative.z);
        transformed.rotate(rotation);
        double rawDepth = transformed.z();
        if (rawDepth <= 0.05D) {
            if (!allowOffscreen) return null;
            double offsetX = -transformed.x();
            double offsetY = -transformed.y();
            double length = Math.hypot(offsetX, offsetY);
            if (length < 1.0E-4D) {
                offsetY = 1.0D;
                length = 1.0D;
            }
            double edgeDistance = Math.max(screenWidth, screenHeight) * 2.0D;
            return new ScreenPoint(
                    (int) Math.round(screenWidth / 2.0D
                            + offsetX / length * edgeDistance),
                    (int) Math.round(screenHeight / 2.0D
                            + offsetY / length * edgeDistance));
        }
        double fov = minecraft.options.fov().get();
        double scale = screenHeight
                / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));
        int x = (int) Math.round(screenWidth / 2.0D
                - transformed.x() * scale / rawDepth);
        int y = (int) Math.round(screenHeight / 2.0D
                - transformed.y() * scale / rawDepth);
        return new ScreenPoint(x, y);
    }'''
text = replace_once(text, old_projection, new_projection, "prompt projection")
text = replace_once(text,
    "            float promptScale, double score) {",
    "            float promptScale, boolean allowOffscreen, double score) {",
    "prompt record field")
write(path, text)


# Default context interaction files -----------------------------------------
for path in (
        "config/scpinventory/context_interactions.json",
        "src/main/resources/defaults/scpinventory/context_interactions.json"):
    data = json.loads(read(path))
    changed = 0
    for rule in data.get("interactions", []):
        if not isinstance(rule, dict) or rule.get("type") != "block":
            continue
        block_path = str(rule.get("id", "")).split(":", 1)[-1].lower()
        if "button" in block_path or "reader" in block_path:
            visual = rule.setdefault("visual", {})
            if visual.get("allowOffscreen") is not True:
                visual["allowOffscreen"] = True
                changed += 1
    if changed == 0:
        raise SystemExit(f"{path}: no button or reader defaults were updated")
    write(path, json.dumps(data, separators=(",", ":"), ensure_ascii=False) + "\n")


# Elevator cabin anchor and translucent rendering ---------------------------
path = "src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorCarriageEntity.java"
text = read(path)
text = replace_once(text,
    "        double modelZ = 11.00251D / 16.0D;",
    "        double modelZ = -11.00251D / 16.0D;",
    "carriage button horizontal mirror")
write(path, text)

path = "src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorClient.java"
text = read(path)
text = replace_count(text,
    "            return RenderType.entityTranslucentCull(texture);",
    "            return RenderType.entityTranslucent(texture, true);",
    2, "elevator translucent no-cull rendering")
write(path, text)

print("Applied configurable off-screen prompts, editor warning, elevator anchor, and translucent rendering fixes.")
