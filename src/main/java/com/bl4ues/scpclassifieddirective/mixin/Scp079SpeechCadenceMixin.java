package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079SpeechSynthesizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives SCP-079 the slower, lower and more deliberate CB-style cadence without
 * coupling the core synthesizer to a specific gameplay presentation.
 */
@Mixin(value = Scp079SpeechSynthesizer.class, remap = false)
public abstract class Scp079SpeechCadenceMixin {
    private static final double TIME_STRETCH = 1.18D;

    @ModifyVariable(method = "synthesise", at = @At("HEAD"),
            argsOnly = true, ordinal = 0, remap = false)
    private static String scpclassifieddirective$deepenPhrasePauses(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            out.append(c);
            // Add a short secondary pause after authored phrase punctuation.
            // Existing repeated punctuation is left alone so ellipses do not
            // become absurdly long merely because a human used three dots.
            boolean repeated = i + 1 < text.length()
                    && text.charAt(i + 1) == c;
            if (!repeated && (c == '.' || c == '!' || c == '?' || c == ','
                    || c == ';' || c == ':')) {
                out.append(':');
            }
        }
        return out.toString();
    }

    @Inject(method = "synthesise", at = @At("RETURN"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$slowAndLower(String text,
            CallbackInfoReturnable<short[]> cir) {
        short[] input = cir.getReturnValue();
        if (input == null || input.length < 2) return;
        int length = Math.max(2, (int) Math.round(input.length * TIME_STRETCH));
        short[] output = new short[length];
        double step = 1.0D / TIME_STRETCH;
        for (int i = 0; i < length; i++) {
            double source = Math.min(input.length - 1.0D, i * step);
            int left = (int) source;
            int right = Math.min(input.length - 1, left + 1);
            double fraction = source - left;
            int sample = (int) Math.round(input[left] * (1.0D - fraction)
                    + input[right] * fraction);
            output[i] = (short) Math.max(Short.MIN_VALUE,
                    Math.min(Short.MAX_VALUE, sample));
        }
        cir.setReturnValue(output);
    }
}
