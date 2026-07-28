from pathlib import Path

path = Path('src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterService.java')
text = path.read_text(encoding='utf-8')
old = '\\"audio\\":{\\"enter_sound_enabled\\":true,\\"replace_player_hurt_sounds\\":true,\\"mute_non_player_hit_sounds\\":false}'
new = '\\"audio\\":{\\"enter_sound_enabled\\":true,\\"replace_player_hurt_sounds\\":true,\\"mute_non_player_hit_sounds\\":false,\\"disable_vanilla_music\\":false}'
if old not in text:
    raise SystemExit('Configuration Center audio fallback was not found')
path.write_text(text.replace(old, new, 1), encoding='utf-8')
