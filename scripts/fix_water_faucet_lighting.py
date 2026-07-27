from pathlib import Path

path = Path("src/main/resources/assets/scp_additions/models/block/water_faucet.json")
text = path.read_text(encoding="utf-8")

if '"ambientocclusion"' not in text:
    marker = '\t"credit": "Made with Blockbench",\n'
    text = text.replace(marker, marker + '\t"ambientocclusion": false,\n', 1)

needle = '\t\t{\n\t\t\t"from":'
replacement = '\t\t{\n\t\t\t"shade": false,\n\t\t\t"from":'
text = text.replace(needle, replacement)

path.write_text(text, encoding="utf-8")
