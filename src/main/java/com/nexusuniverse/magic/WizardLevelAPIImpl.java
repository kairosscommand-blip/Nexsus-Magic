package com.nexusuniverse.magic;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class WizardLevelAPIImpl implements WizardLevelAPI {

    private final MagicConfig config;
    private final WizardLevelManager levels;

    public WizardLevelAPIImpl(MagicConfig config, WizardLevelManager levels) {
        this.config = config;
        this.levels = levels;
    }

    @Override
    public boolean isEnabled() {
        return config.enabled();
    }

    @Override
    public int maxLevel() {
        return config.maxLevel();
    }

    @Override
    public int level(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        return player == null ? 1 : levels.level(player);
    }

    @Override
    public double backfireChance(int level) {
        return config.backfireChance(level);
    }

    @Override
    public double damageMultiplier(int level) {
        return config.damageMultiplier(level);
    }

    @Override
    public double rewardMultiplier(int level) {
        return config.rewardMultiplier(level);
    }
}
