package com.nexusuniverse.magic;

/** The handful of on-hit behaviors that are more than a plain damage+potion-effect combo. Every
 * element still gets its config-driven damage and potion effects on top of whichever of these
 * (if any) it's configured for. */
public enum ElementSpecial {
    NONE,
    /** Sets the target on fire for {@code special-fire-ticks}. */
    IGNITE,
    /** A visual-only lightning strike at the impact point (no incidental splash damage --
     * the element's own on-hit-damage already covers that). */
    LIGHTNING_STRIKE,
    /** Pushes the target away from the impact point, scaled by {@code special-knockback}. */
    GUST_KNOCKBACK,
    /** Pushes the target away (like GUST_KNOCKBACK, same {@code special-knockback}) and also
     * extinguishes them, since water putting out fire is the whole point. */
    WATER_PUSH
}
