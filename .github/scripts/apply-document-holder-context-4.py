from pathlib import Path
import json


def replace_once(path, old, new):
    p = Path(path)
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'Missing marker in {path}: {old!r}')
    p.write_text(text.replace(old, new, 1), encoding='utf-8')

registry = 'src/main/java/com/bl4ues/scpinventory/context/ContextInteractionRegistry.java'
replace_once(
    registry,
    'import net.mcreator.scpadditions.facility.FacilityLargePropStructure;\n',
    'import net.mcreator.scpadditions.facility.DocumentHolderBlockEntity;\n'
    'import net.mcreator.scpadditions.facility.FacilityLargePropStructure;\n')
replace_once(
    registry,
    '        public boolean isAvailable(Level level, BlockPos pos,\n'
    '                BlockState state) {\n',
    '        public boolean isAvailable(Level level, BlockPos pos,\n'
    '                BlockState state, Player player) {\n'
    '            if (block != null && "document_holder".equals(id.getPath())\n'
    '                    && ScpAdditionsMod.MODID.equals(id.getNamespace())) {\n'
    '                if (!(level.getBlockEntity(pos)\n'
    '                        instanceof DocumentHolderBlockEntity holder)) {\n'
    '                    return false;\n'
    '                }\n'
    '                return holder.canContextInteract(player);\n'
    '            }\n'
    '            return isAvailable(level, pos, state);\n'
    '        }\n\n'
    '        public boolean isAvailable(Level level, BlockPos pos,\n'
    '                BlockState state) {\n')

holder = 'src/main/java/net/mcreator/scpadditions/facility/DocumentHolderBlockEntity.java'
replace_once(
    holder,
    '    public boolean wouldHandle(Player player, InteractionHand hand) {\n',
    '    /** True only while a contextual prompt can perform a real holder action. */\n'
    '    public boolean canContextInteract(Player player) {\n'
    '        return player != null && !isTransitioning()\n'
    '                && wouldHandle(player, InteractionHand.MAIN_HAND);\n'
    '    }\n\n'
    '    public boolean wouldHandle(Player player, InteractionHand hand) {\n')

prompt = 'src/main/java/com/bl4ues/scpinventory/client/ContextPromptClient.java'
replace_once(
    prompt,
    '                if (!rule.isAvailable(player.level(), rulePos, ruleState)\n'
    '                        || !rule.isHeldItemSatisfied(player)) continue;\n',
    '                if (!rule.isAvailable(player.level(), rulePos, ruleState,\n'
    '                        player) || !rule.isHeldItemSatisfied(player)) continue;\n')

packet = 'src/main/java/com/bl4ues/scpinventory/network/ContextInteractPacket.java'
replace_once(
    packet,
    '        if (rule == null || !rule.isAvailable(level, pos, state)) return;\n',
    '        if (rule == null || !rule.isAvailable(level, pos, state, player)) return;\n')

config_path = Path('config/scpinventory/context_interactions.json')
text = config_path.read_text(encoding='utf-8')
data = json.loads(text)
entry_id = 'scp_additions:document_holder'
interaction_id = 'document_holder_use'
exists = any(
    isinstance(x, dict)
    and x.get('id') == entry_id
    and x.get('interactionId', '') == interaction_id
    for x in data.get('interactions', [])
)
if not exists:
    marker = '\n  ],\n  "examples"'
    if marker not in text:
        raise SystemExit('Could not locate end of interactions array')
    entry = (
        '    {"type": "block", "id": "scp_additions:document_holder", '
        '"interactionId": "document_holder_use", "range": 1.5, '
        '"priority": 30, "useItem": "hand", '
        '"text": {"action": "Use", "nameMode": "manual", '
        '"name": "Document Holder", "showAction": false, '
        '"showName": false}, '
        '"anchor": {"position": [0.5, 0.5, 0.9], "rotateWith": "auto"}, '
        '"input": {"allowE": true, "allowRightClick": true}, '
        '"click": {"face": "front"}, '
        '"visual": {"icon": "hand", "scale": 1.0, '
        '"allowOffscreen": false}}'
    )
    text = text.replace(marker, ',\n' + entry + marker, 1)
    config_path.write_text(text, encoding='utf-8')
    json.loads(text)

replace_once('build.gradle', "version = '3.1.0'", "version = '4.0.0'")
replace_once('CHANGELOG.md',
             '# SCP Additions 3.1.0 — In Development',
             '# SCP Additions 4.0.0 — In Development')
replace_once(
    'CHANGELOG.md',
    '- Added item-specific contextual interactions and inherited alternate variants, allowing one block or entity to expose different actions according to the item held without duplicating its complete configuration;\n',
    '- Added item-specific contextual interactions and inherited alternate variants, allowing one block or entity to expose different actions according to the item held without duplicating its complete configuration;\n'
    '- Added a state-aware integrated Document Holder prompt that shows only the hand icon, with no text, and appears only while the holder can currently accept, return, or close a document;\n')

print('Document Holder integrated interaction and 4.0.0 version applied.')
