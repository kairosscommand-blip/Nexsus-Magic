package com.nexusuniverse.magic;

import java.util.Map;

/** Resolves a config color name (e.g. "AQUA", the standard Bukkit ChatColor constant names) to
 * its raw Minecraft color code. Uses the same raw-§-escape approach NexusGrowth's
 * GrowthCommand already established in this project, rather than depending on Bukkit's ChatColor
 * (which this stub environment models as a plain class with no name-based lookup) -- the actual
 * server resolves these identically either way. */
public final class MagicColors {

    private static final Map<String, Character> CODES = Map.ofEntries(
            Map.entry("BLACK", '0'), Map.entry("DARK_BLUE", '1'), Map.entry("DARK_GREEN", '2'),
            Map.entry("DARK_AQUA", '3'), Map.entry("DARK_RED", '4'), Map.entry("DARK_PURPLE", '5'),
            Map.entry("GOLD", '6'), Map.entry("GRAY", '7'), Map.entry("DARK_GRAY", '8'),
            Map.entry("BLUE", '9'), Map.entry("GREEN", 'a'), Map.entry("AQUA", 'b'),
            Map.entry("RED", 'c'), Map.entry("LIGHT_PURPLE", 'd'), Map.entry("YELLOW", 'e'),
            Map.entry("WHITE", 'f')
    );

    private MagicColors() {
    }

    /** Returns the "§<code>" prefix for a color name, or white if unrecognized. */
    public static String prefix(String colorName) {
        char code = CODES.getOrDefault(colorName == null ? "" : colorName.toUpperCase(), 'f');
        return "§" + code;
    }

    public static final String RESET = "§r";
}
