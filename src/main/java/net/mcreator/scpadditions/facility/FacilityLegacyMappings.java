package net.mcreator.scpadditions.facility;

/** Registers standalone facility namespaces as aliases of scp_additions ids. */
public final class FacilityLegacyMappings {
    private FacilityLegacyMappings() {
    }

    public static void registerAliases() {
        FacilityModule.registerLegacyAliases();
        UBlocksModule.registerLegacyAliases();
    }
}
