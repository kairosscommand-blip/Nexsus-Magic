package com.nexusuniverse.magic;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/** The "combat" half of wizard leveling (the other half is CastListener's landed-hit XP): any
 * kill, mob or player, with any weapon, earns the killer a flat trickle of wizard XP -- being a
 * capable fighter counts toward wizard growth on its own, not only casting spells. */
public final class CombatXpListener implements Listener {

    private final MagicConfig config;
    private final WizardLevelManager levels;

    public CombatXpListener(MagicConfig config, WizardLevelManager levels) {
        this.config = config;
        this.levels = levels;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!config.enabled()) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        int levelsGained = levels.addXp(killer, config.xpPerKill());
        if (levelsGained > 0) {
            killer.sendMessage("§d§lYour magic grows stronger -- you're now a level "
                    + levels.level(killer) + " wizard!");
        }
    }
}
