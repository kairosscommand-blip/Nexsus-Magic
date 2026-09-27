package com.nexusuniverse.magic;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Right-click with a staff/bow to cast it: rolls the wizard's backfire chance (real self-damage,
 * more likely and harsher the lower their level is), then always fires the spell regardless --
 * a backfire is a real risk of using magic, not a chance the magic fails outright. */
public final class CastListener implements Listener {

    private final MagicConfig config;
    private final MagicItemFactory itemFactory;
    private final WizardLevelManager levels;
    private final NamespacedKey elementKey;
    private final NamespacedKey casterKey;
    private final NamespacedKey damageMultiplierKey;
    private final NamespacedKey rewardMultiplierKey;
    private final Map<UUID, Long> lastCastTick = new ConcurrentHashMap<>();

    public CastListener(JavaPlugin plugin, MagicConfig config, MagicItemFactory itemFactory, WizardLevelManager levels) {
        this.config = config;
        this.itemFactory = itemFactory;
        this.levels = levels;
        this.elementKey = new NamespacedKey(plugin, "magic_projectile_element");
        this.casterKey = new NamespacedKey(plugin, "magic_projectile_caster");
        this.damageMultiplierKey = new NamespacedKey(plugin, "magic_projectile_damage_mult");
        this.rewardMultiplierKey = new NamespacedKey(plugin, "magic_projectile_reward_mult");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!config.enabled() || event.getHand() != EquipmentSlot.HAND) {
            return; // only handle the main-hand event once, not also the off-hand copy
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        Element element = itemFactory.elementOf(item);
        if (element == null) {
            return;
        }
        ElementDefinition definition = config.element(element);
        if (definition == null || !definition.enabled()) {
            return;
        }

        event.setCancelled(true); // stops a Bow from also charging a vanilla arrow shot

        Player player = event.getPlayer();
        if (isOnCooldown(player)) {
            return;
        }
        lastCastTick.put(player.getUniqueId(), System.currentTimeMillis());

        rollBackfire(player, definition);

        double damageMultiplier = levels.damageMultiplier(player);
        double rewardMultiplier = levels.rewardMultiplier(player);

        Snowball projectile = player.launchProjectile(Snowball.class);
        var pdc = projectile.getPersistentDataContainer();
        pdc.set(elementKey, PersistentDataType.STRING, element.name());
        pdc.set(casterKey, PersistentDataType.STRING, player.getUniqueId().toString());
        pdc.set(damageMultiplierKey, PersistentDataType.DOUBLE, damageMultiplier);
        pdc.set(rewardMultiplierKey, PersistentDataType.DOUBLE, rewardMultiplier);
    }

    private boolean isOnCooldown(Player player) {
        Long last = lastCastTick.get(player.getUniqueId());
        if (last == null) {
            return false;
        }
        long elapsedMillis = System.currentTimeMillis() - last;
        long cooldownMillis = config.cooldownTicks() * 50L; // 20 ticks/sec = 50ms/tick
        return elapsedMillis < cooldownMillis;
    }

    private void rollBackfire(Player caster, ElementDefinition definition) {
        double chance = levels.backfireChance(caster);
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        double damage = levels.backfireDamage(caster);
        caster.damage(damage);
        caster.sendMessage("§c§oYour " + definition.displayName().toLowerCase()
                + " magic surges back into you!");
    }

    public NamespacedKey elementKey() {
        return elementKey;
    }

    public NamespacedKey casterKey() {
        return casterKey;
    }

    public NamespacedKey damageMultiplierKey() {
        return damageMultiplierKey;
    }

    public NamespacedKey rewardMultiplierKey() {
        return rewardMultiplierKey;
    }
}
