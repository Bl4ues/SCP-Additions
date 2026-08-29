from pathlib import Path
import json

# ContextPromptClient: Unity-like control prompts and tight aim radius.
path = Path('src/main/java/com/bl4ues/scpclassifieddirective/inventory/client/ContextPromptClient.java')
text = path.read_text()
marker = '''    private static final double ELEVATOR_BUTTON_AIM_RADIUS_SQR =
            0.20D * 0.20D;
'''
addition = marker + '''    private static final double SCP_914_CONTROL_AIM_RADIUS_SQR =
            0.18D * 0.18D;
'''
if 'SCP_914_CONTROL_AIM_RADIUS_SQR' not in text:
    if marker not in text:
        raise SystemExit('Context prompt aim marker missing')
    text = text.replace(marker, addition, 1)

marker = '''        ScreenPoint point = projectToScreen(minecraft, target.anchor(),
                screenWidth, screenHeight, target.allowOffscreen());
        if (point == null) return;

'''
replacement = marker + '''        if (renderScp914ControlPrompt(graphics, minecraft, target, point,
                screenWidth, screenHeight)) {
            return;
        }

'''
if 'renderScp914ControlPrompt(graphics' not in text:
    if marker not in text:
        raise SystemExit('Context prompt render marker missing')
    text = text.replace(marker, replacement, 1)

marker = '''    private static double preciseAimRadiusSqr(String interactionKey) {
        return isElevatorButton(interactionKey)
                ? ELEVATOR_BUTTON_AIM_RADIUS_SQR
                : PRECISE_AIM_RADIUS_SQR;
    }
'''
replacement = '''    private static double preciseAimRadiusSqr(String interactionKey) {
        if (interactionKey != null
                && interactionKey.startsWith("scp_914_")) {
            return SCP_914_CONTROL_AIM_RADIUS_SQR;
        }
        return isElevatorButton(interactionKey)
                ? ELEVATOR_BUTTON_AIM_RADIUS_SQR
                : PRECISE_AIM_RADIUS_SQR;
    }
'''
if marker in text:
    text = text.replace(marker, replacement, 1)
elif 'return SCP_914_CONTROL_AIM_RADIUS_SQR;' not in text:
    raise SystemExit('Context prompt precise aim marker missing')

marker = '''    private static void drawIcon(GuiGraphics graphics,
            ResourceLocation icon, int x, int y, int size) {
'''
helper = '''    private static boolean renderScp914ControlPrompt(
            GuiGraphics graphics, Minecraft minecraft, ContextTarget target,
            ScreenPoint point, int screenWidth, int screenHeight) {
        String key = target.interactionKey();
        boolean dial = "scp_914_dial".equals(key);
        boolean start = "scp_914_start".equals(key);
        if (!dial && !start) return false;

        float promptScale = target.promptScale();
        int iconSize = Math.max(24,
                Math.round(BASE_ICON_SIZE * promptScale));
        int screenX = Mth.clamp(point.x(), iconSize / 2 + 6,
                screenWidth - iconSize / 2 - 6);
        int screenY = Mth.clamp(point.y(), iconSize / 2 + 6,
                screenHeight - iconSize / 2 - 6);
        int iconX = screenX - iconSize / 2;
        int iconY = screenY - iconSize / 2;
        drawIcon(graphics, target.icon(), iconX, iconY, iconSize);

        if (start) {
            String action = target.action() == null
                    || target.action().isBlank() ? "Start" : target.action();
            float textScale = 1.35F * promptScale;
            int textWidth = Math.round(minecraft.font.width(
                    ScpFonts.roboto(action)) * textScale);
            int textX = Mth.clamp(screenX - textWidth / 2, 6,
                    Math.max(6, screenWidth - textWidth - 6));
            int textY = Mth.clamp(iconY + iconSize
                            - Math.round(8.0F * promptScale),
                    6, screenHeight - Math.round(12.0F * textScale));
            drawScaledString(graphics, minecraft, action, textX, textY,
                    textScale, TEXT_WHITE);
        }
        return true;
    }

'''
if 'private static boolean renderScp914ControlPrompt(' not in text:
    if marker not in text:
        raise SystemExit('Context prompt draw marker missing')
    text = text.replace(marker, helper + marker, 1)
path.write_text(text)

# ContextInteractionRegistry: controls disappear while refining and need deliberate aim.
path = Path('src/main/java/com/bl4ues/scpclassifieddirective/inventory/context/ContextInteractionRegistry.java')
text = path.read_text()
import_marker = 'import com.bl4ues.scpclassifieddirective.integration.PlayerItemAccess;\n'
imports = import_marker + 'import com.bl4ues.scpclassifieddirective.block.entity.Scp914BlockEntity;\nimport com.bl4ues.scpclassifieddirective.scp914.Scp914Module;\n'
if 'import com.bl4ues.scpclassifieddirective.scp914.Scp914Module;' not in text:
    if import_marker not in text:
        raise SystemExit('Context registry import marker missing')
    text = text.replace(import_marker, imports, 1)

marker = '''        public boolean requiresPreciseAim() {
            return "close_object_containment_unit".equals(interactionKey)
                    || interactionKey.startsWith("elevator_station_")
                    || interactionKey.startsWith("elevator_carriage_");
        }
'''
replacement = '''        public boolean requiresPreciseAim() {
            return "close_object_containment_unit".equals(interactionKey)
                    || interactionKey.startsWith("elevator_station_")
                    || interactionKey.startsWith("elevator_carriage_")
                    || interactionKey.startsWith("scp_914_");
        }
'''
if marker in text:
    text = text.replace(marker, replacement, 1)
elif '|| interactionKey.startsWith("scp_914_");' not in text:
    raise SystemExit('Context registry precise aim marker missing')

marker = '''        public boolean isAvailable(Level level, BlockPos pos,
                BlockState state, Player player) {
            if (ObjectContainmentUnitModule.isProtectedContent(level, pos)) {
'''
replacement = '''        public boolean isAvailable(Level level, BlockPos pos,
                BlockState state, Player player) {
            if (block == Scp914Module.SCP_914.get()) {
                return level.getBlockEntity(pos) instanceof Scp914BlockEntity machine
                        && !machine.isRefining();
            }
            if (ObjectContainmentUnitModule.isProtectedContent(level, pos)) {
'''
if 'block == Scp914Module.SCP_914.get()' not in text:
    if marker not in text:
        raise SystemExit('Context registry availability marker missing')
    text = text.replace(marker, replacement, 1)
path.write_text(text)

# Runtime defaults: retire legacy piece prompts and add the two new controls.
path = Path('src/main/java/com/bl4ues/scpclassifieddirective/inventory/context/DefaultContextInteractions.java')
text = path.read_text()
marker = '    private static final String SCP1576_TAKE_RULE = """\n'
rules = '''    private static final String SCP914_DIAL_RULE = """
            {
              "type": "block",
              "id": "scp_classified_directive:scp_914",
              "interactionId": "scp_914_dial",
              "range": 2.25,
              "priority": 65,
              "useItem": "hand",
              "icon": "hand",
              "text": {"action": "", "nameMode": "manual", "name": "", "showAction": false, "showName": false},
              "anchor": {"position": [0.5, 1.2525, -0.015625], "rotateWith": "auto"},
              "input": {"allowE": false, "allowRightClick": false},
              "click": {"face": "front"},
              "visual": {"allowOffscreen": false, "scale": 0.72}
            }
            """;
    private static final String SCP914_START_RULE = """
            {
              "type": "block",
              "id": "scp_classified_directive:scp_914",
              "interactionId": "scp_914_start",
              "range": 2.25,
              "priority": 70,
              "useItem": "hand",
              "icon": "hand",
              "text": {"action": "Start", "nameMode": "manual", "name": "", "showAction": true, "showName": false},
              "anchor": {"position": [0.5, 0.90625, -0.0671875], "rotateWith": "auto"},
              "input": {"allowE": true, "allowRightClick": true},
              "click": {"face": "front"},
              "visual": {"allowOffscreen": false, "scale": 0.72}
            }
            """;

'''
if 'private static final String SCP914_DIAL_RULE' not in text:
    if marker not in text:
        raise SystemExit('Default context constant marker missing')
    text = text.replace(marker, rules + marker, 1)

marker = '''                if ("block".equalsIgnoreCase(type)
                        && "scp_classified_directive:scp_426".equals(id)) {
                    interactions.remove(i);
                    continue;
                }
'''
replacement = marker + '''                if ("block".equalsIgnoreCase(type)
                        && isLegacyScp914Id(id)) {
                    interactions.remove(i);
                    continue;
                }
'''
if '&& isLegacyScp914Id(id)' not in text:
    if marker not in text:
        raise SystemExit('Default context removal marker missing')
    text = text.replace(marker, replacement, 1)

marker = '''            boolean corpseExists = false;
            boolean scp714Exists = false;
            boolean scp1576Exists = false;
'''
replacement = '''            boolean corpseExists = false;
            boolean scp714Exists = false;
            boolean scp1576Exists = false;
            boolean scp914DialExists = false;
            boolean scp914StartExists = false;
'''
if 'boolean scp914DialExists' not in text:
    if marker not in text:
        raise SystemExit('Default context flags marker missing')
    text = text.replace(marker, replacement, 1)

marker = '''                if ("block".equalsIgnoreCase(type)
                        && "scp_classified_directive:scp_1576_placed".equals(id)
                        && "take_scp_1576".equals(interactionId)) {
                    scp1576Exists = true;
                }
'''
replacement = marker + '''                if ("block".equalsIgnoreCase(type)
                        && "scp_classified_directive:scp_914".equals(id)) {
                    if ("scp_914_dial".equals(interactionId)) {
                        scp914DialExists = true;
                    } else if ("scp_914_start".equals(interactionId)) {
                        scp914StartExists = true;
                    }
                }
'''
if 'scp914DialExists = true;' not in text:
    if marker not in text:
        raise SystemExit('Default context detection marker missing')
    text = text.replace(marker, replacement, 1)

marker = '''            if (!scp1576Exists) {
                interactions.add(JsonParser.parseString(SCP1576_TAKE_RULE)
                        .getAsJsonObject());
            }
'''
replacement = marker + '''            if (!scp914DialExists) {
                interactions.add(JsonParser.parseString(SCP914_DIAL_RULE)
                        .getAsJsonObject());
            }
            if (!scp914StartExists) {
                interactions.add(JsonParser.parseString(SCP914_START_RULE)
                        .getAsJsonObject());
            }
'''
if 'parseString(SCP914_DIAL_RULE)' not in text:
    if marker not in text:
        raise SystemExit('Default context append marker missing')
    text = text.replace(marker, replacement, 1)

marker = '    private static void normalizeScp1176(JsonObject object) {\n'
helper = '''    private static boolean isLegacyScp914Id(String id) {
        return id != null && (id.equals("scp_classified_directive:scp_914_key_wind")
                || id.startsWith("scp_classified_directive:scp_914dial_")
                || id.equals("scp_classified_directive:scp_914block")
                || id.startsWith("scp_classified_directive:scp_914clockworks")
                || id.equals("scp_classified_directive:scp_914body")
                || id.equals("scp_classified_directive:scp_914_intake")
                || id.equals("scp_classified_directive:scp_914_output")
                || id.startsWith("scp_classified_directive:scp_914_intake_door")
                || id.startsWith("scp_classified_directive:scp_914_output_door"));
    }

'''
if 'private static boolean isLegacyScp914Id' not in text:
    if marker not in text:
        raise SystemExit('Default context helper marker missing')
    text = text.replace(marker, helper + marker, 1)
path.write_text(text)

# Bundled context config.
path = Path('config/scp_classified_directive/context_interactions.json')
root = json.loads(path.read_text())
interactions = root.get('interactions', [])
legacy_ids = {
    'scp_classified_directive:scp_914_key_wind',
    'scp_classified_directive:scp_914dial_1to_1',
    'scp_classified_directive:scp_914dial_coarse',
    'scp_classified_directive:scp_914dial_rough',
    'scp_classified_directive:scp_914dial_fine',
    'scp_classified_directive:scp_914dial_very_fine',
}
interactions = [entry for entry in interactions
                if entry.get('id') not in legacy_ids
                and not (entry.get('id') == 'scp_classified_directive:scp_914'
                         and entry.get('interactionId') in {'scp_914_dial', 'scp_914_start'})]
dial = {
    'type':'block','id':'scp_classified_directive:scp_914','interactionId':'scp_914_dial',
    'range':2.25,'priority':65,'useItem':'hand','icon':'hand',
    'text':{'action':'','nameMode':'manual','name':'','showAction':False,'showName':False},
    'anchor':{'position':[0.5,1.2525,-0.015625],'rotateWith':'auto'},
    'input':{'allowE':False,'allowRightClick':False},'click':{'face':'front'},
    'visual':{'allowOffscreen':False,'scale':0.72},
}
start = {
    'type':'block','id':'scp_classified_directive:scp_914','interactionId':'scp_914_start',
    'range':2.25,'priority':70,'useItem':'hand','icon':'hand',
    'text':{'action':'Start','nameMode':'manual','name':'','showAction':True,'showName':False},
    'anchor':{'position':[0.5,0.90625,-0.0671875],'rotateWith':'auto'},
    'input':{'allowE':True,'allowRightClick':True},'click':{'face':'front'},
    'visual':{'allowOffscreen':False,'scale':0.72},
}
insert_at = next((i for i,e in enumerate(interactions)
                  if e.get('id') == 'scp_classified_directive:scp_1176'), len(interactions))
interactions[insert_at:insert_at] = [dial, start]
root['interactions'] = interactions
path.write_text(json.dumps(root, indent=2, ensure_ascii=False) + '\n')

# Creative tab: only the new SCP-914 item between SCP-902 and SCP-939.
path = Path('src/main/java/com/bl4ues/scpclassifieddirective/init/ScpClassifiedDirectiveModTabs.java')
text = path.read_text()
import_marker = 'import com.bl4ues.scpclassifieddirective.scp1576.Scp1576Module;\n'
if 'import com.bl4ues.scpclassifieddirective.scp914.Scp914Module;' not in text:
    if import_marker not in text:
        raise SystemExit('Creative tab import marker missing')
    text = text.replace(import_marker, import_marker + 'import com.bl4ues.scpclassifieddirective.scp914.Scp914Module;\n', 1)
old = '''        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.SCP_914_ASSEMBLY_KIT.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914BLOCK.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914CLOCKWORKS.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914BODY.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914DIAL_1TO_1.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914_KEY_WIND.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914_INTAKE.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914_OUTPUT.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914_INTAKE_DOOR.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914_OUTPUT_DOOR.get()));
'''
new = '        stacks.add(new ItemStack(Scp914Module.SCP_914_ITEM.get()));\n'
if old in text:
    text = text.replace(old, new, 1)
elif new not in text:
    raise SystemExit('Legacy SCP-914 creative entries marker missing')
path.write_text(text)
