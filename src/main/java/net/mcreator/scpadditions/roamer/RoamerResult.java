package net.mcreator.scpadditions.roamer;

/** Most recent scheduler event, kept separately from the current roamer state. */
public enum RoamerResult {
    NONE,
    TIMER_STARTED,
    SPAWNED,
    DESPAWNED_TIMER_RESET,
    CHANCE_FAILED,
    NO_VALID_POSITION,
    BLOCKED_BY_EXISTING,
    RULE_DISABLED,
    MODULE_DISABLED,
    NOT_IMPLEMENTED,
    PAUSED_CREATIVE,
    PAUSED_SPECTATOR,
    CHANCE_FAILED_OTHER_ROAMER
}
