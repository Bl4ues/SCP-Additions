from pathlib import Path

screen = Path('src/main/java/net/mcreator/scpadditions/client/FacilityDiagnosticsScreen.java')
text = screen.read_text(encoding='utf-8')

old_status = '''        int midpoint = x + width / 2;
        drawBody(graphics, "AUXILIARY BUS", x + 10, y + 24, METAL_GRAY);
        drawBody(graphics, powerState, x + 92, y + 24, powerColor);
        drawBody(graphics, "SESSION CACHE", midpoint + 8, y + 24,
                METAL_GRAY);
        rightAligned(graphics, body(cacheState), x + width - 10, y + 24,
                cacheColor);

        int buttonWidth = Math.max(190, Math.min(232, width * 46 / 100));
        resetX = x + (width - buttonWidth) / 2;
        resetY = y + 38;
'''
new_status = '''        drawBody(graphics, "AUXILIARY BUS", x + 10, y + 24, METAL_GRAY);
        drawBody(graphics, powerState, x + 92, y + 24, powerColor);
        drawBody(graphics, "SESSION CACHE", x + 10, y + 38, METAL_GRAY);
        drawBody(graphics, cacheState, x + 92, y + 38, cacheColor);

        int buttonWidth = Math.max(136, Math.min(190, width * 38 / 100));
        resetX = x + width - buttonWidth - 8;
        resetY = y + 18;
'''
if old_status not in text:
    raise SystemExit('SCiPNET operations layout anchor not found')
text = text.replace(old_status, new_status, 1)

old_center = '''        graphics.drawString(font, component,
                x + Math.max(0, (width - font.width(component)) / 2),
                y + Math.max(0, (height - font.lineHeight) / 2) - 1,
                color, false);
'''
new_center = '''        graphics.drawString(font, component,
                x + Math.max(0, (width - font.width(component)) / 2),
                y + Math.max(0, (height - font.lineHeight) / 2),
                color, false);
'''
if old_center not in text:
    raise SystemExit('Centered body baseline anchor not found')
text = text.replace(old_center, new_center, 1)
screen.write_text(text, encoding='utf-8')

font = Path('src/main/resources/assets/scp_additions/font/scipnet_terminal.json')
font_text = font.read_text(encoding='utf-8')
old_shift = '"shift": [0, 0]'
new_shift = '"shift": [0, 1]'
if old_shift not in font_text:
    raise SystemExit('SCiPNET font shift anchor not found')
font.write_text(font_text.replace(old_shift, new_shift, 1), encoding='utf-8')
