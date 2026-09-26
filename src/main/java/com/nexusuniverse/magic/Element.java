package com.nexusuniverse.magic;

/** The nine elements NexusMagic ships with. The config key (lowercase name()) is what
 * config.yml's {@code elements.<key>} sections are keyed by. */
public enum Element {
    ICE,
    FIRE,
    WATER,
    LIGHTNING,
    EARTH,
    WIND,
    POISON,
    LIGHT,
    DARK;

    public String configKey() {
        return name().toLowerCase();
    }
}
