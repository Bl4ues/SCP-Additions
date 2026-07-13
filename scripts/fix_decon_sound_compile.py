from pathlib import Path

path = Path('src/main/java/net/mcreator/scpadditions/procedures/DecontaminationCheckpointController.java')
text = path.read_text(encoding='utf-8')
replacements = {
    'level.playSound(null, chamberCenter(pos, current), ScpAdditionsModSounds.DOORCLOSING.get(),':
        'level.playSound(null, BlockPos.containing(chamberCenter(pos, current)), ScpAdditionsModSounds.DOORCLOSING.get(),',
    'level.playSound(null, chamberCenter(pos, state), ScpAdditionsModSounds.DECONTAMINATION.get(),':
        'level.playSound(null, BlockPos.containing(chamberCenter(pos, state)), ScpAdditionsModSounds.DECONTAMINATION.get(),',
    'level.playSound(null, chamberCenter(pos, current), ScpAdditionsModSounds.DOOROPEN.get(),':
        'level.playSound(null, BlockPos.containing(chamberCenter(pos, current)), ScpAdditionsModSounds.DOOROPEN.get(),',
}
for old, new in replacements.items():
    if old not in text:
        raise RuntimeError(f'Expected generated sound call not found: {old}')
    text = text.replace(old, new)
path.write_text(text, encoding='utf-8')
print('Corrected decontamination sound call overloads')
