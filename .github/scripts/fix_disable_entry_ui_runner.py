from pathlib import Path

path = Path('.github/scripts/apply_disable_entry_ui.py')
text = path.read_text(encoding='utf-8')
start = text.index('# New item rules start enabled.')
end = text.index('# Item rule list quick toggle and dimming.', start)
replacement = '''# New item rules start enabled. The creation block exists in both init() and\n# rebuildWidgetsOnly(), so both copies must be updated.\nitem_rule_old = r\'''                rule.addProperty("id", id);\n                rule.addProperty("type", "MISCELLANEOUS");\'''\nitem_rule_new = r\'''                rule.addProperty("id", id);\n                rule.addProperty("type", "MISCELLANEOUS");\n                rule.addProperty("enabled", true);\'''\nif text.count(item_rule_old) != 2:\n    raise SystemExit(f"item rule enabled default: expected two matches, found {text.count(item_rule_old)}")\ntext = text.replace(item_rule_old, item_rule_new)\n\n'''
path.write_text(text[:start] + replacement + text[end:], encoding='utf-8')
