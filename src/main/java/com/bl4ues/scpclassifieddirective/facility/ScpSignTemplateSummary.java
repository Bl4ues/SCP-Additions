package com.bl4ues.scpclassifieddirective.facility;

/** Lightweight template metadata safe to synchronize to clients. */
public record ScpSignTemplateSummary(String id, String name,
        boolean custom) {
    public ScpSignTemplateSummary {
        id = ScpSignTemplates.cleanId(id);
        name = ScpSignTemplates.cleanName(name);
        custom = custom && ScpSignTemplates.isCustom(id);
    }
}
