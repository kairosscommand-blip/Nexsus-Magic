package com.nexusuniverse.magic;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/** The fully-parsed config for one element -- everything CastListener/ImpactListener/
 * RecipeRegistrar need, read once by MagicConfig and handed around from then on. Nothing here is
 * mutable after load; a {@code /nexusmagic reload} simply builds a fresh set of these. */
public final class ElementDefinition {

    public record EffectSpec(PotionEffectType type, int seconds, int amplifier) {
    }

    private final Element element;
    private final boolean enabled;
    private final String displayName;
    private final String colorName;
    private final List<Material> ingredients;
    private final double onHitDamage;
    private final List<EffectSpec> onHitEffects;
    private final String hitMessage;
    private final Material groundBlock;
    private final int groundEffectDurationSeconds;
    private final ElementSpecial special;
    private final int specialFireTicks;
    private final double specialKnockback;

    public ElementDefinition(Element element, boolean enabled, String displayName, String colorName,
                              List<Material> ingredients, double onHitDamage, List<EffectSpec> onHitEffects,
                              String hitMessage, Material groundBlock, int groundEffectDurationSeconds,
                              ElementSpecial special, int specialFireTicks, double specialKnockback) {
        this.element = element;
        this.enabled = enabled;
        this.displayName = displayName;
        this.colorName = colorName;
        this.ingredients = ingredients;
        this.onHitDamage = onHitDamage;
        this.onHitEffects = onHitEffects;
        this.hitMessage = hitMessage;
        this.groundBlock = groundBlock;
        this.groundEffectDurationSeconds = groundEffectDurationSeconds;
        this.special = special;
        this.specialFireTicks = specialFireTicks;
        this.specialKnockback = specialKnockback;
    }

    public Element element() {
        return element;
    }

    public boolean enabled() {
        return enabled;
    }

    public String displayName() {
        return displayName;
    }

    public String colorName() {
        return colorName;
    }

    public List<Material> ingredients() {
        return ingredients;
    }

    public double onHitDamage() {
        return onHitDamage;
    }

    public List<EffectSpec> onHitEffects() {
        return onHitEffects;
    }

    public String hitMessage() {
        return hitMessage;
    }

    /** Null means this element leaves no temporary ground effect. */
    public Material groundBlock() {
        return groundBlock;
    }

    public int groundEffectDurationSeconds() {
        return groundEffectDurationSeconds;
    }

    public ElementSpecial special() {
        return special;
    }

    public int specialFireTicks() {
        return specialFireTicks;
    }

    public double specialKnockback() {
        return specialKnockback;
    }
}
