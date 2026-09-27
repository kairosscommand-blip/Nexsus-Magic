package com.nexusuniverse.magic;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/** Builds a staff or bow for a given element -- a plain vanilla Stick/Bow underneath, tagged via
 * PDC (so CastListener/ImpactListener can recognize it) and dressed up with a colored name, lore
 * describing what it does, and a cosmetic enchant glint (it's the shimmer that matters, not a
 * real vanilla enchantment). */
public final class MagicItemFactory {

    private final MagicConfig config;
    private final NamespacedKey markerKey;
    private final NamespacedKey elementKey;
    private final NamespacedKey kindKey;

    public MagicItemFactory(JavaPlugin plugin, MagicConfig config) {
        this.config = config;
        this.markerKey = new NamespacedKey(plugin, "magic_item");
        this.elementKey = new NamespacedKey(plugin, "magic_element");
        this.kindKey = new NamespacedKey(plugin, "magic_kind");
    }

    public NamespacedKey elementKey() {
        return elementKey;
    }

    public NamespacedKey kindKey() {
        return kindKey;
    }

    public ItemStack create(Element element, ItemKind kind) {
        ElementDefinition definition = config.element(element);
        String displayName = definition != null ? definition.displayName() : element.configKey();
        String colorPrefix = MagicColors.prefix(definition != null ? definition.colorName() : "WHITE");

        Material base = kind == ItemKind.STAFF ? Material.STICK : Material.BOW;
        ItemStack item = new ItemStack(base, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String kindLabel = kind == ItemKind.STAFF ? "Staff" : "Bow";
            meta.setDisplayName(colorPrefix + displayName + " " + kindLabel + MagicColors.RESET);
            meta.setLore(buildLore(definition, kind));
            // The clean, modern way to get the enchant shimmer without a real enchantment (which
            // Bukkit/Paper no longer lets plugin code construct at all -- Enchantment is an
            // abstract, registry-backed class as of 1.20.5+/1.21.x): Paper's own
            // ItemMeta#setEnchantmentGlintOverride exists for exactly this "I want the sparkle,
            // not a real enchant" case. No addEnchant call and no HIDE_ENCHANTS flag needed.
            meta.setEnchantmentGlintOverride(true);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(markerKey, PersistentDataType.BYTE, (byte) 1);
            pdc.set(elementKey, PersistentDataType.STRING, element.name());
            pdc.set(kindKey, PersistentDataType.STRING, kind.name());

            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> buildLore(ElementDefinition definition, ItemKind kind) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Right-click to cast.");
        if (definition != null && !definition.hitMessage().isEmpty()) {
            lore.add("§7Effect: §f" + definition.hitMessage());
        }
        if (definition != null && definition.groundBlock() != null) {
            lore.add("§7Leaves a temporary elemental patch where it lands.");
        }
        lore.add("§8A wizard's own risk and reward scale with their level.");
        return lore;
    }

    public boolean isMagicItem(ItemStack item) {
        return elementOf(item) != null;
    }

    public Element elementOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String raw = meta.getPersistentDataContainer().get(elementKey, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return Element.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public ItemKind kindOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String raw = meta.getPersistentDataContainer().get(kindKey, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return ItemKind.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
