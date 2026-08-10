from pathlib import Path

p = Path('src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterModernWidgetEvents.java')
s = p.read_text(encoding='utf-8')
s = s.replace(
    'import net.minecraft.client.gui.components.EditBox;\n',
    'import net.minecraft.client.gui.components.EditBox;\nimport net.minecraft.client.gui.components.AbstractWidget;\n')
s = s.replace(
    'private static final Map<AbstractButton, Integer> SUPPRESSED_X',
    'private static final Map<AbstractWidget, Integer> SUPPRESSED_X')
s = s.replace(
    '''            if (listener instanceof AbstractButton button
                    && shouldSuppressNative(button)) {
                suppressNative(button);
            }
            prepare(listener);''',
    '''            if (listener instanceof AbstractSliderButton slider) {
                suppressNative(slider);
            } else if (listener instanceof AbstractButton button
                    && shouldSuppressNativeButton(button)) {
                suppressNative(button);
            }
            prepare(listener);''')
s = s.replace(
    '''    private static boolean shouldSuppressNative(AbstractButton button) {
        return button instanceof AbstractSliderButton
                || isSelfRenderedButton(button)
                || !(button instanceof Button);
    }

    private static void suppressNative(AbstractButton button) {''',
    '''    private static boolean shouldSuppressNativeButton(AbstractButton button) {
        return isSelfRenderedButton(button) || !(button instanceof Button);
    }

    private static void suppressNative(AbstractWidget button) {''')
s = s.replace(
    '''            if (!(listener instanceof AbstractButton button)) continue;
            Integer x = SUPPRESSED_X.get(button);''',
    '''            if (!(listener instanceof AbstractWidget button)) continue;
            Integer x = SUPPRESSED_X.get(button);''')
p.write_text(s, encoding='utf-8')
