package net.mcreator.scpadditions.facility;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared validation and compaction rules for editable facility signs. */
public final class FacilitySignData {
    public static final int ENTRY_COUNT = 3;
    public static final int MAX_NUMBER_LENGTH = 8;
    public static final Entry EMPTY_ENTRY = new Entry("", "");

    private FacilitySignData() {
    }

    public static List<Entry> normalize(FacilitySignBlock.SignType type,
            List<Entry> source) {
        List<Entry> populated = new ArrayList<>(ENTRY_COUNT);
        if (source != null) {
            for (Entry entry : source) {
                Entry clean = sanitize(type, entry);
                if (!clean.isEmpty()) populated.add(clean);
                if (populated.size() == ENTRY_COUNT) break;
            }
        }
        while (populated.size() < ENTRY_COUNT) populated.add(EMPTY_ENTRY);
        return List.copyOf(populated);
    }

    public static Entry sanitize(FacilitySignBlock.SignType type, Entry entry) {
        if (type == null) type = FacilitySignBlock.SignType.CORE_ROOM;
        if (entry == null) entry = EMPTY_ENTRY;

        String text = clean(entry.text(), type.maxTextLength());
        if (type.forceUppercase()) {
            text = clean(text.toUpperCase(Locale.ROOT), type.maxTextLength());
        }
        String number = type.hasNumbers()
                ? cleanDigits(entry.number(), MAX_NUMBER_LENGTH) : "";
        return new Entry(number, text);
    }

    public static String cleanText(FacilitySignBlock.SignType type, String text) {
        return sanitize(type, new Entry("", text)).text();
    }

    public static String cleanNumber(String number) {
        return cleanDigits(number, MAX_NUMBER_LENGTH);
    }

    private static String clean(String value, int maxCodePoints) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        value.codePoints()
                .filter(codePoint -> !Character.isISOControl(codePoint))
                .limit(maxCodePoints)
                .forEach(result::appendCodePoint);
        return result.toString().strip();
    }

    private static String cleanDigits(String value, int maxCodePoints) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        value.codePoints()
                .filter(Character::isDigit)
                .limit(maxCodePoints)
                .forEach(result::appendCodePoint);
        return result.toString();
    }

    public record Entry(String number, String text) {
        public Entry {
            number = number == null ? "" : number;
            text = text == null ? "" : text;
        }

        public boolean isEmpty() {
            return number.isBlank() && text.isBlank();
        }
    }
}
