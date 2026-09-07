package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Small, deterministic, offline speech engine for playable SCP-079.
 *
 * The target is the coarse First Byte/SBTalker family heard through Dr. Sbaitso,
 * not natural modern TTS. Speech is assembled from a rule-based grapheme to
 * phoneme pass, excited through three crude resonators at an 8522 Hz native
 * rate, quantized to signed 8-bit steps, and only then resampled to the 48 kHz
 * PCM format consumed by Simple Voice Chat. The deliberately limited bandwidth,
 * abrupt phoneme transitions and almost-flat fundamental are features here.
 */
public final class Scp079SpeechSynthesizer {
    public static final int OUTPUT_RATE = 48_000;
    private static final int NATIVE_RATE = 8_522;
    private static final int MAX_NATIVE_SAMPLES = NATIVE_RATE * 22;
    private static final double TWO_PI = Math.PI * 2.0D;
    private static final double BASE_PITCH_HZ = 91.0D;

    // Keep the rule tables readable without relying on an illegal implicit
    // import of constants from the nested enum. Java does not lift nested enum
    // constants into the enclosing class namespace, because apparently even
    // phonemes need paperwork.
    private static final Phoneme AA = Phoneme.AA;
    private static final Phoneme AE = Phoneme.AE;
    private static final Phoneme AH = Phoneme.AH;
    private static final Phoneme AO = Phoneme.AO;
    private static final Phoneme AW = Phoneme.AW;
    private static final Phoneme AY = Phoneme.AY;
    private static final Phoneme EH = Phoneme.EH;
    private static final Phoneme ER = Phoneme.ER;
    private static final Phoneme EY = Phoneme.EY;
    private static final Phoneme IH = Phoneme.IH;
    private static final Phoneme IY = Phoneme.IY;
    private static final Phoneme OW = Phoneme.OW;
    private static final Phoneme OY = Phoneme.OY;
    private static final Phoneme UH = Phoneme.UH;
    private static final Phoneme UW = Phoneme.UW;
    private static final Phoneme B = Phoneme.B;
    private static final Phoneme D = Phoneme.D;
    private static final Phoneme G = Phoneme.G;
    private static final Phoneme P = Phoneme.P;
    private static final Phoneme T = Phoneme.T;
    private static final Phoneme K = Phoneme.K;
    private static final Phoneme CH = Phoneme.CH;
    private static final Phoneme JH = Phoneme.JH;
    private static final Phoneme F = Phoneme.F;
    private static final Phoneme V = Phoneme.V;
    private static final Phoneme S = Phoneme.S;
    private static final Phoneme Z = Phoneme.Z;
    private static final Phoneme SH = Phoneme.SH;
    private static final Phoneme TH = Phoneme.TH;
    private static final Phoneme HH = Phoneme.HH;
    private static final Phoneme M = Phoneme.M;
    private static final Phoneme N = Phoneme.N;
    private static final Phoneme NG = Phoneme.NG;
    private static final Phoneme L = Phoneme.L;
    private static final Phoneme R = Phoneme.R;
    private static final Phoneme W = Phoneme.W;
    private static final Phoneme Y = Phoneme.Y;

    private static final Map<String, Phoneme[]> WORDS = dictionary();

    private Scp079SpeechSynthesizer() {
    }

    public static short[] synthesise(String text) {
        List<Unit> units = phonemes(text);
        if (units.isEmpty()) return new short[0];

        double[] nativeAudio = new double[MAX_NATIVE_SAMPLES];
        int cursor = 0;
        Random random = new Random(0x079L ^ (text == null ? 0 : text.hashCode()));
        SynthState state = new SynthState();

        for (Unit unit : units) {
            if (cursor >= MAX_NATIVE_SAMPLES) break;
            if (unit.pauseMs > 0) {
                cursor = writePause(nativeAudio, cursor, unit.pauseMs);
                continue;
            }
            cursor = renderPhoneme(nativeAudio, cursor, unit.phoneme,
                    unit.pitchScale, state, random);
        }
        if (cursor <= 0) return new short[0];

        // A tiny delayed path gives the old card/cheap PA combination its hard,
        // metallic edge without turning intelligibility into a science project.
        int delay = 29;
        for (int i = delay; i < cursor; i++) {
            nativeAudio[i] = Mth.clamp(nativeAudio[i] * 0.84D
                    + nativeAudio[i - delay] * 0.19D, -1.0D, 1.0D);
        }

        short[] quantized = new short[cursor];
        for (int i = 0; i < cursor; i++) {
            int eightBit = Mth.clamp((int) Math.round(nativeAudio[i] * 112.0D),
                    -127, 127);
            quantized[i] = (short) (eightBit << 8);
        }
        return resample(quantized, NATIVE_RATE, OUTPUT_RATE);
    }

    private static int renderPhoneme(double[] output, int cursor,
            Phoneme phoneme, double pitchScale, SynthState state, Random random) {
        int samples = Math.max(1, (int) Math.round(
                phoneme.durationMs * NATIVE_RATE / 1000.0D));
        int end = Math.min(output.length, cursor + samples);
        if (end <= cursor) return cursor;

        Resonator r1 = new Resonator(phoneme.f1, phoneme.bw1);
        Resonator r2 = new Resonator(phoneme.f2, phoneme.bw2);
        Resonator r3 = new Resonator(phoneme.f3, phoneme.bw3);
        int fadeSamples = Math.max(4, NATIVE_RATE / 170);
        double pitch = BASE_PITCH_HZ * pitchScale;

        for (int i = cursor; i < end; i++) {
            double excitation;
            if (phoneme.voiced) {
                double oldPhase = state.phase;
                state.phase += pitch / NATIVE_RATE;
                if (state.phase >= 1.0D) state.phase -= Math.floor(state.phase);
                boolean impulse = state.phase < oldPhase;
                double saw = state.phase * 2.0D - 1.0D;
                excitation = (impulse ? 1.0D : 0.0D) * 0.92D
                        + saw * 0.10D
                        + (random.nextDouble() * 2.0D - 1.0D)
                        * phoneme.noise * 0.20D;
            } else {
                excitation = (random.nextDouble() * 2.0D - 1.0D)
                        * (0.58D + phoneme.noise * 0.42D);
            }

            double sample = r1.process(excitation) * phoneme.a1
                    + r2.process(excitation) * phoneme.a2
                    + r3.process(excitation) * phoneme.a3;
            if (phoneme.burst) {
                int local = i - cursor;
                if (local < NATIVE_RATE / 110) {
                    sample += (random.nextDouble() * 2.0D - 1.0D)
                            * (1.0D - local / (double) (NATIVE_RATE / 110))
                            * 0.72D;
                }
            }

            // Soft saturation was common enough in the complete chain that a
            // clean floating-point waveform immediately sounds too modern.
            sample = Math.tanh(sample * 1.55D) * 0.86D;
            int local = i - cursor;
            int remaining = end - i - 1;
            double envelope = Math.min(1.0D,
                    Math.min((local + 1) / (double) fadeSamples,
                            (remaining + 1) / (double) fadeSamples));
            output[i] = sample * envelope;
        }
        return end;
    }

    private static int writePause(double[] output, int cursor, int millis) {
        int samples = Math.max(1,
                (int) Math.round(millis * NATIVE_RATE / 1000.0D));
        return Math.min(output.length, cursor + samples);
    }

    private static short[] resample(short[] input, int sourceRate,
            int targetRate) {
        if (input.length == 0 || sourceRate <= 0 || targetRate <= 0) {
            return new short[0];
        }
        int length = Math.max(1, (int) Math.ceil(
                input.length * (double) targetRate / sourceRate));
        short[] output = new short[length];
        double ratio = sourceRate / (double) targetRate;
        for (int i = 0; i < length; i++) {
            double source = i * ratio;
            int left = Math.min(input.length - 1, (int) source);
            int right = Math.min(input.length - 1, left + 1);
            double fraction = source - left;
            output[i] = (short) Mth.clamp((int) Math.round(
                    input[left] * (1.0D - fraction)
                            + input[right] * fraction),
                    Short.MIN_VALUE, Short.MAX_VALUE);
        }
        return output;
    }

    private static List<Unit> phonemes(String input) {
        if (input == null || input.isBlank()) return List.of();
        String text = input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9'.,!?;: -]", " ")
                .replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) return List.of();

        List<Unit> result = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (int index = 0; index <= text.length(); index++) {
            char c = index < text.length() ? text.charAt(index) : ' ';
            if (Character.isLetterOrDigit(c) || c == '\'') {
                word.append(c);
                continue;
            }
            if (!word.isEmpty()) {
                appendWord(result, expandNumber(word.toString()));
                word.setLength(0);
            }
            switch (c) {
                case '.', '!', '?' -> result.add(Unit.pause(c == '.' ? 185 : 225));
                case ',', ';', ':' -> result.add(Unit.pause(115));
                case ' ', '-' -> {
                    if (!result.isEmpty() && !result.get(result.size() - 1).isPause()) {
                        result.add(Unit.pause(42));
                    }
                }
                default -> { }
            }
        }
        return result;
    }

    private static void appendWord(List<Unit> result, String word) {
        if (word == null || word.isBlank()) return;
        if (word.indexOf(' ') >= 0) {
            String[] pieces = word.split(" +");
            for (int i = 0; i < pieces.length; i++) {
                appendWord(result, pieces[i]);
                if (i + 1 < pieces.length) result.add(Unit.pause(42));
            }
            return;
        }
        Phoneme[] known = WORDS.get(word);
        if (known != null) {
            for (Phoneme phoneme : known) result.add(Unit.sound(phoneme, 1.0D));
            return;
        }
        List<Phoneme> generated = g2p(word);
        for (Phoneme phoneme : generated) result.add(Unit.sound(phoneme, 1.0D));
    }

    private static List<Phoneme> g2p(String word) {
        List<Phoneme> out = new ArrayList<>();
        String w = word.replace("'", "");
        for (int i = 0; i < w.length();) {
            String rest = w.substring(i);
            if (rest.startsWith("tion")) { add(out, SH, AH, N); i += 4; continue; }
            if (rest.startsWith("tch")) { add(out, CH); i += 3; continue; }
            if (rest.startsWith("igh")) { add(out, AY); i += 3; continue; }
            if (rest.startsWith("dge")) { add(out, JH); i += 3; continue; }
            if (rest.startsWith("sch")) { add(out, S, K); i += 3; continue; }
            if (rest.startsWith("th")) { add(out, TH); i += 2; continue; }
            if (rest.startsWith("sh")) { add(out, SH); i += 2; continue; }
            if (rest.startsWith("ch")) { add(out, CH); i += 2; continue; }
            if (rest.startsWith("ph")) { add(out, F); i += 2; continue; }
            if (rest.startsWith("ng")) { add(out, NG); i += 2; continue; }
            if (rest.startsWith("qu")) { add(out, K, W); i += 2; continue; }
            if (rest.startsWith("ee") || rest.startsWith("ea")) { add(out, IY); i += 2; continue; }
            if (rest.startsWith("oo")) { add(out, UW); i += 2; continue; }
            if (rest.startsWith("ai") || rest.startsWith("ay")) { add(out, EY); i += 2; continue; }
            if (rest.startsWith("oi") || rest.startsWith("oy")) { add(out, OY); i += 2; continue; }
            if (rest.startsWith("ow") || rest.startsWith("ou")) { add(out, AW); i += 2; continue; }
            if (rest.startsWith("au")) { add(out, AO); i += 2; continue; }
            if (rest.startsWith("er") || rest.startsWith("ir")
                    || rest.startsWith("ur")) { add(out, ER); i += 2; continue; }
            if (rest.startsWith("ar")) { add(out, AA, R); i += 2; continue; }
            if (rest.startsWith("or")) { add(out, AO, R); i += 2; continue; }

            char c = w.charAt(i);
            char next = i + 1 < w.length() ? w.charAt(i + 1) : '\0';
            char after = i + 2 < w.length() ? w.charAt(i + 2) : '\0';
            boolean silentEPattern = isVowel(c) && next != '\0'
                    && !isVowel(next) && after == 'e' && i + 3 == w.length();
            switch (c) {
                case 'a' -> add(out, silentEPattern ? EY : AE);
                case 'e' -> {
                    if (i != w.length() - 1 || w.length() <= 2) add(out, EH);
                }
                case 'i', 'y' -> add(out, silentEPattern ? AY : IH);
                case 'o' -> add(out, silentEPattern ? OW : AA);
                case 'u' -> add(out, silentEPattern ? UW : AH);
                case 'b' -> add(out, B);
                case 'c' -> add(out, next == 'e' || next == 'i' || next == 'y' ? S : K);
                case 'd' -> add(out, D);
                case 'f' -> add(out, F);
                case 'g' -> add(out, next == 'e' || next == 'i' || next == 'y' ? JH : G);
                case 'h' -> add(out, HH);
                case 'j' -> add(out, JH);
                case 'k' -> add(out, K);
                case 'l' -> add(out, L);
                case 'm' -> add(out, M);
                case 'n' -> add(out, N);
                case 'p' -> add(out, P);
                case 'q' -> add(out, K);
                case 'r' -> add(out, R);
                case 's' -> add(out, S);
                case 't' -> add(out, T);
                case 'v' -> add(out, V);
                case 'w' -> add(out, W);
                case 'x' -> add(out, K, S);
                case 'z' -> add(out, Z);
                default -> { }
            }
            i++;
        }
        return out;
    }

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    private static String expandNumber(String value) {
        if (!value.chars().allMatch(Character::isDigit)) return value;
        if (value.length() > 4) {
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                if (i > 0) out.append(' ');
                out.append(digit(value.charAt(i)));
            }
            return out.toString();
        }
        try {
            int number = Integer.parseInt(value);
            if (number < 20) return small(number);
            if (number < 100) {
                int tens = number / 10;
                int ones = number % 10;
                return tens(tens) + (ones == 0 ? "" : " " + small(ones));
            }
            if (number < 1000) {
                int rest = number % 100;
                return small(number / 100) + " hundred"
                        + (rest == 0 ? "" : " " + expandNumber(Integer.toString(rest)));
            }
            int rest = number % 1000;
            return small(number / 1000) + " thousand"
                    + (rest == 0 ? "" : " " + expandNumber(Integer.toString(rest)));
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private static String digit(char c) {
        return switch (c) {
            case '0' -> "zero"; case '1' -> "one"; case '2' -> "two";
            case '3' -> "three"; case '4' -> "four"; case '5' -> "five";
            case '6' -> "six"; case '7' -> "seven"; case '8' -> "eight";
            case '9' -> "nine"; default -> "";
        };
    }

    private static String small(int value) {
        return switch (value) {
            case 0 -> "zero"; case 1 -> "one"; case 2 -> "two";
            case 3 -> "three"; case 4 -> "four"; case 5 -> "five";
            case 6 -> "six"; case 7 -> "seven"; case 8 -> "eight";
            case 9 -> "nine"; case 10 -> "ten"; case 11 -> "eleven";
            case 12 -> "twelve"; case 13 -> "thirteen"; case 14 -> "fourteen";
            case 15 -> "fifteen"; case 16 -> "sixteen"; case 17 -> "seventeen";
            case 18 -> "eighteen"; case 19 -> "nineteen"; default -> "";
        };
    }

    private static String tens(int value) {
        return switch (value) {
            case 2 -> "twenty"; case 3 -> "thirty"; case 4 -> "forty";
            case 5 -> "fifty"; case 6 -> "sixty"; case 7 -> "seventy";
            case 8 -> "eighty"; case 9 -> "ninety"; default -> "";
        };
    }

    private static void add(List<Phoneme> out, Phoneme... phonemes) {
        for (Phoneme phoneme : phonemes) out.add(phoneme);
    }

    private static Map<String, Phoneme[]> dictionary() {
        Map<String, Phoneme[]> map = new HashMap<>();
        map.put("a", p(AH));
        map.put("i", p(AY));
        map.put("the", p(TH, AH));
        map.put("this", p(TH, IH, S));
        map.put("that", p(TH, AE, T));
        map.put("you", p(Y, UW));
        map.put("your", p(Y, AO, R));
        map.put("are", p(AA, R));
        map.put("is", p(IH, Z));
        map.put("yes", p(Y, EH, S));
        map.put("no", p(N, OW));
        map.put("not", p(N, AA, T));
        map.put("can", p(K, AE, N));
        map.put("cannot", p(K, AE, N, AA, T));
        map.put("will", p(W, IH, L));
        map.put("human", p(HH, Y, UW, M, AH, N));
        map.put("computer", p(K, AH, M, P, Y, UW, T, ER));
        map.put("facility", p(F, AH, S, IH, L, IH, T, IY));
        map.put("foundation", p(F, AW, N, D, EY, SH, AH, N));
        map.put("containment", p(K, AH, N, T, EY, N, M, AH, N, T));
        map.put("breach", p(B, R, IY, CH));
        map.put("access", p(AE, K, S, EH, S));
        map.put("escape", p(IH, S, K, EY, P));
        map.put("door", p(D, AO, R));
        map.put("speaker", p(S, P, IY, K, ER));
        map.put("machine", p(M, AH, SH, IY, N));
        map.put("hello", p(HH, EH, L, OW));
        map.put("goodbye", p(G, UH, D, B, AY));
        map.put("please", p(P, L, IY, Z));
        map.put("old", p(OW, L, D));
        map.put("ai", p(EY, AY));
        map.put("scp", p(EH, S, S, IY, P, IY));
        return Map.copyOf(map);
    }

    private static Phoneme[] p(Phoneme... value) {
        return value;
    }

    private static final class Resonator {
        private final double c1;
        private final double c2;
        private final double gain;
        private double y1;
        private double y2;

        private Resonator(double frequency, double bandwidth) {
            double f = Mth.clamp(frequency, 80.0D, NATIVE_RATE * 0.44D);
            double r = Math.exp(-Math.PI * bandwidth / NATIVE_RATE);
            this.c1 = 2.0D * r * Math.cos(TWO_PI * f / NATIVE_RATE);
            this.c2 = -r * r;
            this.gain = 1.0D - r;
        }

        private double process(double sample) {
            double y = gain * sample + c1 * y1 + c2 * y2;
            y2 = y1;
            y1 = y;
            return y;
        }
    }

    private static final class SynthState {
        private double phase;
    }

    private record Unit(Phoneme phoneme, int pauseMs, double pitchScale) {
        private static Unit sound(Phoneme phoneme, double pitchScale) {
            return new Unit(phoneme, 0, pitchScale);
        }

        private static Unit pause(int millis) {
            return new Unit(null, millis, 1.0D);
        }

        private boolean isPause() {
            return pauseMs > 0;
        }
    }

    private enum Phoneme {
        // name voiced f1 f2 f3 bw1 bw2 bw3 a1 a2 a3 duration noise burst
        AA(true, 730, 1090, 2440, 95, 120, 180, .95, .62, .28, 116, .05, false),
        AE(true, 660, 1720, 2410, 95, 130, 190, .96, .68, .26, 115, .05, false),
        AH(true, 640, 1190, 2390, 100, 135, 190, .94, .58, .24, 105, .06, false),
        AO(true, 570, 840, 2410, 95, 120, 180, .98, .56, .28, 116, .04, false),
        AW(true, 680, 1120, 2460, 100, 130, 190, .96, .61, .25, 142, .05, false),
        AY(true, 620, 1510, 2520, 100, 135, 190, .96, .66, .27, 142, .05, false),
        EH(true, 530, 1840, 2480, 90, 135, 190, .92, .69, .26, 104, .05, false),
        ER(true, 490, 1350, 1690, 90, 115, 160, .95, .61, .34, 118, .04, false),
        EY(true, 500, 1900, 2600, 90, 135, 190, .88, .70, .24, 130, .05, false),
        IH(true, 400, 1990, 2550, 85, 140, 190, .82, .72, .23, 92, .06, false),
        IY(true, 310, 2320, 3000, 80, 150, 210, .76, .73, .26, 112, .05, false),
        OW(true, 500, 900, 2500, 90, 120, 185, .96, .54, .25, 137, .04, false),
        OY(true, 540, 1240, 2510, 95, 130, 190, .94, .62, .25, 143, .05, false),
        UH(true, 440, 1020, 2240, 95, 125, 180, .94, .55, .23, 100, .05, false),
        UW(true, 350, 850, 2200, 85, 120, 175, .90, .52, .23, 116, .04, false),

        B(true, 300, 1100, 2100, 170, 220, 300, .55, .24, .12, 62, .12, true),
        D(true, 330, 1700, 2700, 175, 240, 320, .46, .30, .14, 57, .12, true),
        G(true, 300, 1250, 2350, 180, 230, 320, .48, .27, .13, 64, .13, true),
        P(false, 450, 1500, 2800, 220, 330, 460, .20, .27, .38, 58, .95, true),
        T(false, 500, 2300, 3350, 230, 360, 500, .13, .31, .42, 54, 1.00, true),
        K(false, 450, 1750, 3150, 230, 350, 480, .15, .30, .42, 62, 1.00, true),
        CH(false, 520, 2050, 3300, 220, 340, 480, .14, .31, .47, 92, 1.00, true),
        JH(true, 500, 1900, 3100, 210, 320, 450, .24, .34, .37, 94, .65, true),

        F(false, 650, 1700, 3400, 260, 420, 520, .10, .27, .52, 105, 1.00, false),
        V(true, 560, 1550, 3250, 240, 390, 520, .18, .30, .42, 92, .55, false),
        S(false, 650, 2550, 3650, 260, 430, 560, .08, .27, .58, 104, 1.00, false),
        Z(true, 560, 2350, 3500, 250, 410, 540, .17, .30, .49, 94, .62, false),
        SH(false, 520, 1900, 3050, 250, 390, 520, .10, .34, .54, 112, 1.00, false),
        TH(false, 580, 1800, 3250, 250, 410, 550, .10, .31, .53, 88, .92, false),
        HH(false, 700, 1400, 2850, 330, 470, 600, .18, .25, .34, 72, .78, false),

        M(true, 260, 1080, 2100, 80, 160, 260, .93, .24, .10, 92, .03, false),
        N(true, 280, 1660, 2600, 85, 180, 280, .86, .30, .12, 86, .03, false),
        NG(true, 300, 1350, 2250, 90, 180, 290, .88, .27, .11, 96, .03, false),
        L(true, 390, 1250, 2700, 100, 170, 250, .79, .45, .19, 84, .03, false),
        R(true, 370, 1300, 1700, 95, 160, 230, .83, .48, .27, 88, .03, false),
        W(true, 330, 900, 2200, 95, 150, 230, .86, .43, .18, 78, .03, false),
        Y(true, 310, 2200, 3000, 90, 170, 250, .71, .57, .21, 75, .03, false);

        private final boolean voiced;
        private final double f1, f2, f3;
        private final double bw1, bw2, bw3;
        private final double a1, a2, a3;
        private final int durationMs;
        private final double noise;
        private final boolean burst;

        Phoneme(boolean voiced, double f1, double f2, double f3,
                double bw1, double bw2, double bw3,
                double a1, double a2, double a3,
                int durationMs, double noise, boolean burst) {
            this.voiced = voiced;
            this.f1 = f1;
            this.f2 = f2;
            this.f3 = f3;
            this.bw1 = bw1;
            this.bw2 = bw2;
            this.bw3 = bw3;
            this.a1 = a1;
            this.a2 = a2;
            this.a3 = a3;
            this.durationMs = durationMs;
            this.noise = noise;
            this.burst = burst;
        }
    }
}
