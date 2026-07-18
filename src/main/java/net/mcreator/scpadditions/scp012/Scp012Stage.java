package net.mcreator.scpadditions.scp012;

/** Physical containment-box stages. Transitional stages are hidden internals. */
public enum Scp012Stage {
    CLOSED,
    OPENING_1,
    OPENING_2,
    OPENING_3,
    OPEN,
    CLOSING_3,
    CLOSING_2,
    CLOSING_1;

    public boolean isOpen() {
        return this == OPEN;
    }

    public boolean isTransitional() {
        return this != CLOSED && this != OPEN;
    }
}
