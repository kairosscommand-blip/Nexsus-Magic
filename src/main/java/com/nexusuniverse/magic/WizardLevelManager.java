package com.nexusuniverse.magic;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Every player's wizard level (1..max-level) and progress toward the next one, stored directly
 * on their own PersistentDataContainer -- no separate data file, same approach NexusGrowth uses
 * for its own per-player state. */
public final class WizardLevelManager {

    private final MagicConfig config;
    private final NamespacedKey levelKey;
    private final NamespacedKey xpKey;

    public WizardLevelManager(JavaPlugin plugin, MagicConfig config) {
        this.config = config;
        this.levelKey = new NamespacedKey(plugin, "wizard_level");
        this.xpKey = new NamespacedKey(plugin, "wizard_xp");
    }

    public int level(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        Integer stored = pdc.get(levelKey, PersistentDataType.INTEGER);
        return stored == null ? 1 : Math.max(1, Math.min(config.maxLevel(), stored));
    }

    public double xpTowardNext(Player player) {
        Double stored = player.getPersistentDataContainer().get(xpKey, PersistentDataType.DOUBLE);
        return stored == null ? 0.0 : stored;
    }

    public double xpNeededForNext(Player player) {
        return config.xpForNextLevel(level(player));
    }

    public void setLevel(Player player, int level) {
        int clamped = Math.max(1, Math.min(config.maxLevel(), level));
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(levelKey, PersistentDataType.INTEGER, clamped);
        pdc.set(xpKey, PersistentDataType.DOUBLE, 0.0);
    }

    /** Adds XP, rolling levels up (possibly several at once) as thresholds are crossed. Returns
     * how many levels were gained (0 if none, e.g. already at max-level). */
    public int addXp(Player player, double amount) {
        if (amount <= 0) {
            return 0;
        }
        int level = level(player);
        double xp = xpTowardNext(player);
        int levelsGained = 0;

        xp += amount;
        while (level < config.maxLevel()) {
            double needed = config.xpForNextLevel(level);
            if (xp < needed) {
                break;
            }
            xp -= needed;
            level++;
            levelsGained++;
        }
        if (level >= config.maxLevel()) {
            xp = 0.0; // fully maxed -- nothing more to climb toward
        }

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(levelKey, PersistentDataType.INTEGER, level);
        pdc.set(xpKey, PersistentDataType.DOUBLE, xp);
        return levelsGained;
    }

    // --- Risk/reward curve, keyed off whatever level the player is currently at ---

    public double backfireChance(Player player) {
        return config.backfireChance(level(player));
    }

    public double backfireDamage(Player player) {
        return config.backfireDamage(level(player));
    }

    public double damageMultiplier(Player player) {
        return config.damageMultiplier(level(player));
    }

    public double rewardMultiplier(Player player) {
        return config.rewardMultiplier(level(player));
    }
}
