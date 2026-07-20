package com.bl4ues.scpinventory.events;

/**
 * NeoForge data attachments own inventory persistence and clone handling.
 * The registered attachment is serializable and copy-on-death, replacing the
 * old AttachCapabilitiesEvent and manual PlayerEvent.Clone implementation.
 */
public final class CapabilityEvents {
    private CapabilityEvents() {
    }
}
