package com.nexusuniverse.magic;

import java.util.UUID;

/** Soft-integration surface for other Nexus plugins, registered on Bukkit's ServicesManager the
 * same way NexusGrowthAPI/NexusEnchantsAPI/etc. already are -- look it up via
 * {@code Bukkit.getServicesManager().getRegistration(WizardLevelAPI.class)} and stay gracefully
 * disconnected if NexusMagic isn't installed. Offline players are reported as level 1 (their real
 * level is stored in their own PersistentDataContainer, only readable while online). */
public interface WizardLevelAPI {

    boolean isEnabled();

    int maxLevel();

    /** 1 if the player isn't online (their real level can't be read while offline). */
    int level(UUID playerId);

    double backfireChance(int level);

    double damageMultiplier(int level);

    double rewardMultiplier(int level);
}
