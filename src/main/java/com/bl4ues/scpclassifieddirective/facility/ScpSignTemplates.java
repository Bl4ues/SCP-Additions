package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.util.List;
import java.util.Locale;

/** Built-in IDs, limits and shared sanitization for sign templates. */
public final class ScpSignTemplates {
    public static final String INFORMATION = "builtin:scp_information";
    public static final String SCP_914_NOTICE = "builtin:scp_914_notice";
    public static final String UNDER_CONSTRUCTION =
            "builtin:under_construction";
    public static final String CREATE_CUSTOM = "action:create_custom";
    public static final String CUSTOM_PREFIX = "custom:";

    public static final int TARGET_WIDTH = 1024;
    public static final int TARGET_HEIGHT = 640;
    public static final int MAX_NAME_LENGTH = 48;
    public static final int MAX_ID_LENGTH = 80;
    public static final int MAX_IMAGE_BYTES = 1_500_000;
    public static final int MAX_CUSTOM_TEMPLATES = 64;
    public static final int MAX_TOTAL_IMAGE_BYTES = 24_000_000;

    public static final ResourceLocation INFORMATION_TEXTURE =
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                    "textures/screens/scpsign/scp_sign_base.png");
    public static final ResourceLocation SCP_914_TEXTURE =
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                    "textures/screens/scpsign/914-notice.png");
    public static final ResourceLocation UNDER_CONSTRUCTION_TEXTURE =
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                    "textures/screens/scpsign/under_construction.png");

    private static final List<ScpSignTemplateSummary> BUILT_INS = List.of(
            new ScpSignTemplateSummary(INFORMATION,
                    "SCP Information Sign", false),
            new ScpSignTemplateSummary(SCP_914_NOTICE,
                    "SCP-914 Usage Notice", false),
            new ScpSignTemplateSummary(UNDER_CONSTRUCTION,
                    "Area Under Construction Sign", false),
            new ScpSignTemplateSummary(CREATE_CUSTOM,
                    "Create Custom Sign...", false));

    private ScpSignTemplates() {
    }

    public static List<ScpSignTemplateSummary> builtIns() {
        return BUILT_INS;
    }

    public static boolean isInformation(String id) {
        return INFORMATION.equals(cleanId(id));
    }

    public static boolean isBuiltIn(String id) {
        String clean = cleanId(id);
        return INFORMATION.equals(clean) || SCP_914_NOTICE.equals(clean)
                || UNDER_CONSTRUCTION.equals(clean);
    }

    public static boolean isCustom(String id) {
        String clean = cleanId(id);
        return clean.startsWith(CUSTOM_PREFIX)
                && clean.length() > CUSTOM_PREFIX.length();
    }

    public static ResourceLocation builtInTexture(String id) {
        String clean = cleanId(id);
        if (INFORMATION.equals(clean)) return INFORMATION_TEXTURE;
        if (SCP_914_NOTICE.equals(clean)) return SCP_914_TEXTURE;
        if (UNDER_CONSTRUCTION.equals(clean)) {
            return UNDER_CONSTRUCTION_TEXTURE;
        }
        return null;
    }

    public static String cleanId(String value) {
        if (value == null || value.isBlank()) return INFORMATION;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(
                Math.min(MAX_ID_LENGTH, normalized.length()));
        for (int index = 0; index < normalized.length()
                && result.length() < MAX_ID_LENGTH; index++) {
            char character = normalized.charAt(index);
            if (character >= 'a' && character <= 'z'
                    || character >= '0' && character <= '9'
                    || character == ':' || character == '_'
                    || character == '-') {
                result.append(character);
            }
        }
        return result.isEmpty() ? INFORMATION : result.toString();
    }

    public static String cleanName(String value) {
        if (value == null || value.isBlank()) return "Custom Sign";
        StringBuilder result = new StringBuilder(MAX_NAME_LENGTH);
        value.codePoints()
                .filter(codePoint -> !Character.isISOControl(codePoint))
                .limit(MAX_NAME_LENGTH)
                .forEach(result::appendCodePoint);
        String clean = result.toString().trim();
        return clean.isEmpty() ? "Custom Sign" : clean;
    }
}
