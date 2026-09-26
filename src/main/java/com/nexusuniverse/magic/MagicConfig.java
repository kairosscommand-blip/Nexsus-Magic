package com.nexusuniverse.magic;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/** Loads and holds config.yml. {@code reload()} rebuilds everything from scratch, so
 * {@code /nexusmagic reload} always reflects whatever is on disk right now. */
public final class MagicConfig {

    private final JavaPlugin plugin;

    private boolean enabled;
    private int cooldownTicks;
    private int defaultGroundRadius;
    private int defaultGroundDurationSeconds;

    private int maxLevel;
    private double xpBase;
    private double xpGrowthPerLevel;
    private double xpPerCastHit;
    private double xpPerKill;

    private double minBackfireChance;
    private double maxBackfireChance;
    private double minBackfireDamage;
    private double maxBackfireDamage;
    private double minDamageMultiplier;
    private double maxDamageMultiplier;
    private double minRewardMultiplier;
    private double maxRewardMultiplier;

    private Map<Element, ElementDefinition> elements = new EnumMap<>(Element.class);

    public MagicConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        var config = plugin.getConfig();

        this.enabled = config.getBoolean("enabled", true);

        this.cooldownTicks = config.getInt("casting.cooldown-ticks", 20);
        this.defaultGroundRadius = config.getInt("casting.ground-effect-radius", 2);
        this.defaultGroundDurationSeconds = config.getInt("casting.ground-effect-duration-seconds", 20);

        this.maxLevel = Math.max(1, config.getInt("leveling.max-level", 100));
        this.xpBase = config.getDouble("leveling.xp-base", 50.0);
        this.xpGrowthPerLevel = config.getDouble("leveling.xp-growth-per-level", 25.0);
        this.xpPerCastHit = config.getDouble("leveling.xp-per-cast-hit", 4.0);
        this.xpPerKill = config.getDouble("leveling.xp-per-kill", 2.0);

        this.minBackfireChance = config.getDouble("risk-reward.min-backfire-chance", 0.02);
        this.maxBackfireChance = config.getDouble("risk-reward.max-backfire-chance", 0.35);
        this.minBackfireDamage = config.getDouble("risk-reward.min-backfire-damage", 1.0);
        this.maxBackfireDamage = config.getDouble("risk-reward.max-backfire-damage", 6.0);
        this.minDamageMultiplier = config.getDouble("risk-reward.min-damage-multiplier", 0.6);
        this.maxDamageMultiplier = config.getDouble("risk-reward.max-damage-multiplier", 2.0);
        this.minRewardMultiplier = config.getDouble("risk-reward.min-reward-multiplier", 0.5);
        this.maxRewardMultiplier = config.getDouble("risk-reward.max-reward-multiplier", 2.5);

        Map<Element, ElementDefinition> loaded = new EnumMap<>(Element.class);
        ConfigurationSection elementsSection = config.getConfigurationSection("elements");
        if (elementsSection != null) {
            for (Element element : Element.values()) {
                ConfigurationSection section = elementsSection.getConfigurationSection(element.configKey());
                if (section == null) {
                    continue;
                }
                loaded.put(element, parseElement(element, section));
            }
        }
        this.elements = loaded;
    }

    private ElementDefinition parseElement(Element element, ConfigurationSection section) {
        boolean elementEnabled = section.getBoolean("enabled", true);
        String displayName = section.getString("display-name", capitalize(element.configKey()));
        String colorName = section.getString("color", "WHITE");

        List<Material> ingredients = section.getStringList("ingredients").stream()
                .map(name -> {
                    Material material = Material.matchMaterial(name);
                    if (material == null) {
                        plugin.getLogger().log(Level.WARNING, "[NexusMagic] Unknown material \"" + name
                                + "\" in elements." + element.configKey() + ".ingredients -- recipe will be incomplete.");
                    }
                    return material;
                })
                .filter(material -> material != null)
                .toList();

        double onHitDamage = section.getDouble("on-hit-damage", 3.0);

        List<ElementDefinition.EffectSpec> onHitEffects = section.getMapList("on-hit-effects").stream()
                .map(this::parseEffect)
                .filter(spec -> spec != null)
                .toList();

        String hitMessage = section.getString("hit-message", "");

        String groundBlockName = section.getString("ground-block", null);
        Material groundBlock = groundBlockName == null || groundBlockName.equalsIgnoreCase("null")
                ? null
                : Material.matchMaterial(groundBlockName);

        int groundDuration = section.contains("ground-effect-duration-seconds")
                ? section.getInt("ground-effect-duration-seconds", defaultGroundDurationSeconds)
                : defaultGroundDurationSeconds;

        ElementSpecial special;
        try {
            special = ElementSpecial.valueOf(section.getString("special", "NONE").toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING, "[NexusMagic] Unknown special \"" + section.getString("special", "")
                    + "\" for elements." + element.configKey() + " -- treating as NONE.");
            special = ElementSpecial.NONE;
        }
        int specialFireTicks = section.getInt("special-fire-ticks", 100);
        double specialKnockback = section.getDouble("special-knockback", 1.0);

        return new ElementDefinition(element, elementEnabled, displayName, colorName, ingredients, onHitDamage,
                onHitEffects, hitMessage, groundBlock, groundDuration, special, specialFireTicks, specialKnockback);
    }

    private ElementDefinition.EffectSpec parseEffect(Map<?, ?> map) {
        Object typeName = map.get("type");
        if (typeName == null) {
            return null;
        }
        PotionEffectType type = resolveEffectType(typeName.toString());
        if (type == null) {
            plugin.getLogger().log(Level.WARNING, "[NexusMagic] Unknown potion effect type \"" + typeName + "\" in config -- skipped it.");
            return null;
        }
        int seconds = map.get("seconds") instanceof Number number ? number.intValue() : 3;
        int amplifier = map.get("amplifier") instanceof Number number ? number.intValue() : 0;
        return new ElementDefinition.EffectSpec(type, seconds, amplifier);
    }

    private PotionEffectType resolveEffectType(String name) {
        String normalized = name.trim().toUpperCase();
        return switch (normalized) {
            case "SPEED" -> PotionEffectType.SPEED;
            case "SLOWNESS" -> PotionEffectType.SLOWNESS;
            case "JUMP_BOOST" -> PotionEffectType.JUMP_BOOST;
            case "NAUSEA" -> PotionEffectType.NAUSEA;
            case "BLINDNESS" -> PotionEffectType.BLINDNESS;
            case "WEAKNESS" -> PotionEffectType.WEAKNESS;
            case "LEVITATION" -> PotionEffectType.LEVITATION;
            case "GLOWING" -> PotionEffectType.GLOWING;
            case "REGENERATION" -> PotionEffectType.REGENERATION;
            case "HASTE" -> PotionEffectType.HASTE;
            case "STRENGTH" -> PotionEffectType.STRENGTH;
            case "SATURATION" -> PotionEffectType.SATURATION;
            case "POISON" -> PotionEffectType.POISON;
            case "WITHER" -> PotionEffectType.WITHER;
            default -> null;
        };
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // --- Accessors ---

    public boolean enabled() {
        return enabled;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public int defaultGroundRadius() {
        return defaultGroundRadius;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public double xpForNextLevel(int currentLevel) {
        return xpBase + (currentLevel - 1) * xpGrowthPerLevel;
    }

    public double xpPerCastHit() {
        return xpPerCastHit;
    }

    public double xpPerKill() {
        return xpPerKill;
    }

    /** t = 0 at level 1, 1 at max-level. */
    private double progress(int level) {
        if (maxLevel <= 1) {
            return 1.0;
        }
        double t = (level - 1) / (double) (maxLevel - 1);
        return Math.max(0.0, Math.min(1.0, t));
    }

    private double lerp(double atLevel1, double atMaxLevel, double t) {
        return atLevel1 + (atMaxLevel - atLevel1) * t;
    }

    public double backfireChance(int level) {
        return lerp(maxBackfireChance, minBackfireChance, progress(level));
    }

    public double backfireDamage(int level) {
        return lerp(maxBackfireDamage, minBackfireDamage, progress(level));
    }

    public double damageMultiplier(int level) {
        return lerp(minDamageMultiplier, maxDamageMultiplier, progress(level));
    }

    public double rewardMultiplier(int level) {
        return lerp(minRewardMultiplier, maxRewardMultiplier, progress(level));
    }

    public Map<Element, ElementDefinition> elements() {
        return elements;
    }

    public ElementDefinition element(Element element) {
        return elements.get(element);
    }
}
