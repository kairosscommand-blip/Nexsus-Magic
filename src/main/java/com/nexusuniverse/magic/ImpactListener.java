package com.nexusuniverse.magic;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.UUID;

/** Where a cast spell actually resolves: on-hit damage/effects to whatever it struck, the
 * element's "special" (ignite/lightning/knockback), a temporary ground effect at the impact spot
 * regardless of what was hit, and XP for the caster. */
public final class ImpactListener implements Listener {

    private final MagicConfig config;
    private final WizardLevelManager levels;
    private final GroundEffectManager groundEffects;
    private final CastListener castListener;

    public ImpactListener(MagicConfig config, WizardLevelManager levels, GroundEffectManager groundEffects,
                           CastListener castListener) {
        this.config = config;
        this.levels = levels;
        this.groundEffects = groundEffects;
        this.castListener = castListener;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Entity projectileEntity = event.getEntity();
        if (!(projectileEntity instanceof Projectile projectile)) {
            return; // defensive -- ProjectileHitEvent's entity is always a Projectile in practice
        }

        PersistentDataContainer pdc = projectile.getPersistentDataContainer();
        String elementRaw = pdc.get(castListener.elementKey(), PersistentDataType.STRING);
        if (elementRaw == null) {
            return; // not one of ours -- some other plugin's tagged Snowball
        }
        projectile.remove();

        Element element;
        try {
            element = Element.valueOf(elementRaw);
        } catch (IllegalArgumentException ex) {
            return;
        }
        ElementDefinition definition = config.element(element);
        if (definition == null) {
            return;
        }

        String casterRaw = pdc.get(castListener.casterKey(), PersistentDataType.STRING);
        Double storedDamageMultiplier = pdc.get(castListener.damageMultiplierKey(), PersistentDataType.DOUBLE);
        Double storedRewardMultiplier = pdc.get(castListener.rewardMultiplierKey(), PersistentDataType.DOUBLE);
        double damageMultiplier = storedDamageMultiplier == null ? 1.0 : storedDamageMultiplier;
        double rewardMultiplier = storedRewardMultiplier == null ? 1.0 : storedRewardMultiplier;

        Player caster = resolveCaster(casterRaw);
        Location impact = projectile.getLocation();

        LivingEntity target = event.getHitEntity() instanceof LivingEntity living ? living : null;
        if (target != null) {
            applyOnHit(target, definition, damageMultiplier);
            applySpecial(definition, target, caster);
            if (caster != null) {
                awardHitXp(caster, definition, rewardMultiplier);
            }
        } else if (definition.special() == ElementSpecial.LIGHTNING_STRIKE) {
            applySpecial(definition, null, caster);
        }

        if (definition.groundBlock() != null) {
            Location anchor = event.getHitBlock() != null
                    ? event.getHitBlock().getLocation()
                    : impact.clone().add(0, -1, 0);
            groundEffects.apply(anchor, definition.groundBlock(), config.defaultGroundRadius(),
                    definition.groundEffectDurationSeconds());
        }
    }

    private Player resolveCaster(String casterRaw) {
        if (casterRaw == null) {
            return null;
        }
        try {
            return Bukkit.getPlayer(UUID.fromString(casterRaw));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void applyOnHit(LivingEntity target, ElementDefinition definition, double damageMultiplier) {
        target.damage(definition.onHitDamage() * damageMultiplier);
        for (ElementDefinition.EffectSpec effect : definition.onHitEffects()) {
            target.addPotionEffect(new PotionEffect(effect.type(), effect.seconds() * 20, effect.amplifier()));
        }
        if (target instanceof Player targetPlayer && !definition.hitMessage().isEmpty()) {
            targetPlayer.sendMessage("§c" + definition.hitMessage());
        }
    }

    private void applySpecial(ElementDefinition definition, LivingEntity target, Player caster) {
        switch (definition.special()) {
            case IGNITE -> {
                if (target != null) {
                    target.setFireTicks(definition.specialFireTicks());
                }
            }
            case LIGHTNING_STRIKE -> {
                World world = target != null ? target.getWorld() : (caster != null ? caster.getWorld() : null);
                Location strikeAt = target != null ? target.getLocation() : (caster != null ? caster.getLocation() : null);
                if (world != null && strikeAt != null) {
                    world.strikeLightningEffect(strikeAt);
                }
            }
            case GUST_KNOCKBACK -> {
                if (target != null && caster != null) {
                    knockback(target, caster.getLocation(), definition.specialKnockback());
                }
            }
            case WATER_PUSH -> {
                if (target != null) {
                    target.setFireTicks(0);
                    if (caster != null) {
                        knockback(target, caster.getLocation(), definition.specialKnockback());
                    }
                }
            }
            case NONE -> {
                // nothing extra
            }
        }
    }

    private void knockback(LivingEntity target, Location from, double strength) {
        Location to = target.getLocation();
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        if (dx == 0 && dz == 0) {
            dx = 0.01; // avoid a zero-length vector when caster and target are stacked
        }
        Vector push = new Vector(dx, 0.2, dz).normalize().multiply(strength);
        target.setVelocity(target.getVelocity().add(push));
    }

    private void awardHitXp(Player caster, ElementDefinition definition, double rewardMultiplier) {
        double xp = config.xpPerCastHit() * rewardMultiplier;
        int levelsGained = levels.addXp(caster, xp);
        if (levelsGained > 0) {
            caster.sendMessage("§d§lYour magic grows stronger -- you're now a level "
                    + levels.level(caster) + " wizard!");
        }
    }
}
